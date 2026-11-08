package com.nutriguide.rag;

import com.nutriguide.common.DocumentProcessingException;
import com.nutriguide.modules.chat.ChatResponse;
import com.nutriguide.modules.chat.ChatResponse.FuenteResponse;
import com.nutriguide.rag.VectorRepository.FragmentoRecuperado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquesta el flujo RAG completo:
 * <ol>
 *   <li>Ingesta: texto limpio → fragmentos → embeddings → pgvector.</li>
 *   <li>Consulta: pregunta → embedding → búsqueda en pgvector → contexto → modelo de lenguaje
 *       → respuesta con las fuentes realmente utilizadas.</li>
 * </ol>
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    static final String ADVERTENCIA_EDUCATIVA = "Información con fines educativos basada en los documentos de la "
            + "biblioteca de NutriGuide. No constituye diagnóstico, tratamiento ni dieta clínica personalizada.";

    static final String SIN_INFORMACION = "No encontré información suficiente en los documentos educativos de "
            + "NutriGuide para responder tu pregunta de forma verificada. Prueba a reformularla o consulta sobre "
            + "alimentos, nutrientes, hidratación o hábitos alimentarios.";

    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepository;
    private final RetrievalService retrievalService;
    private final LlmGenerationService llmGenerationService;
    private final GeminiClient geminiClient;

    public RagService(TextChunker textChunker, EmbeddingService embeddingService, VectorRepository vectorRepository,
                      RetrievalService retrievalService, LlmGenerationService llmGenerationService,
                      GeminiClient geminiClient) {
        this.textChunker = textChunker;
        this.embeddingService = embeddingService;
        this.vectorRepository = vectorRepository;
        this.retrievalService = retrievalService;
        this.llmGenerationService = llmGenerationService;
        this.geminiClient = geminiClient;
    }

    /**
     * Fragmenta el texto, genera los embeddings y reemplaza los fragmentos del documento en pgvector.
     * Primero se calculan todos los vectores y luego se escriben en una sola transacción.
     *
     * @return número de fragmentos almacenados
     */
    public int indexarDocumento(Long documentoId, String titulo, String textoLimpio) {
        geminiClient.exigirConfiguracion();
        List<String> fragmentos = textChunker.dividirEnFragmentos(textoLimpio);
        if (fragmentos.isEmpty()) {
            throw new DocumentProcessingException("El documento no produjo fragmentos de texto válidos.");
        }
        List<float[]> embeddings = embeddingService.generarEmbeddingsDocumento(fragmentos, titulo);
        vectorRepository.reemplazarFragmentos(documentoId, fragmentos, embeddings);
        log.info("Documento '{}' (ID={}) indexado en pgvector con {} fragmentos.", titulo, documentoId, fragmentos.size());
        return fragmentos.size();
    }

    public ChatResponse responderPregunta(String pregunta) {
        geminiClient.exigirConfiguracion();
        List<FragmentoRecuperado> fragmentos = retrievalService.recuperarFragmentosRelevantes(pregunta);
        if (fragmentos.isEmpty()) {
            return new ChatResponse(SIN_INFORMACION, List.of(), false, ADVERTENCIA_EDUCATIVA);
        }

        String contexto = retrievalService.construirContextoDocumental(fragmentos);
        String respuesta = llmGenerationService.generarRespuestaConContexto(pregunta, contexto);

        if (respuesta.startsWith(LlmGenerationService.MARCA_SIN_INFORMACION)) {
            String explicacion = respuesta.substring(LlmGenerationService.MARCA_SIN_INFORMACION.length()).trim();
            return new ChatResponse(explicacion.isEmpty() ? SIN_INFORMACION : explicacion,
                    List.of(), false, ADVERTENCIA_EDUCATIVA);
        }

        List<FuenteResponse> fuentes = fragmentos.stream()
                .map(f -> new FuenteResponse(f.tituloDocumento(), f.contenido(), f.documentoId(),
                        f.numeroFragmento(), f.categoriaDocumento(), f.fuenteOrigen(),
                        Math.round(f.similitud() * 1000.0) / 1000.0))
                .toList();
        return new ChatResponse(respuesta, fuentes, true, ADVERTENCIA_EDUCATIVA);
    }
}
