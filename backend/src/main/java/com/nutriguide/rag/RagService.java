package com.nutriguide.rag;

import com.nutriguide.common.DocumentProcessingException;
import com.nutriguide.modules.chat.ChatResponse;
import com.nutriguide.modules.chat.ChatResponse.FuenteResponse;
import com.nutriguide.modules.documento.Documento;
import com.nutriguide.rag.VectorRepository.FragmentoRecuperado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;

/**
 * Servicio central que orquesta el flujo completo de Retrieval-Augmented Generation (RAG):
 * 1. Ingesta, fragmentación (chunking), generación de embeddings y almacenamiento en pgvector.
 * 2. Búsqueda vectorial de fragmentos relevantes, construcción de contexto seguro y generación de respuesta con fuentes.
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private static final String ADVERTENCIA_EDUCATIVA =
            "Información con fines estrictamente educativos basada en documentos de referencia. "
                    + "No constituye diagnóstico médico, tratamiento ni dieta clínica personalizada.";

    private static final Set<String> PALABRAS_GENERICAS = Set.of(
            "que", "cual", "cuales", "como", "donde", "cuando", "quien", "por", "para",
            "con", "sin", "sobre", "entre", "tiene", "tienen", "contiene", "contienen",
            "aporta", "aportan", "son", "es", "del", "las", "los", "una", "uno", "unos",
            "unas", "beneficios", "propiedades", "informacion", "hablame", "dime", "explica",
            "puedes", "decir", "saber", "acerca", "este", "esta", "estos", "estas"
    );

    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepository;
    private final RetrievalService retrievalService;
    private final LlmGenerationService llmGenerationService;

    public RagService(
            TextChunker textChunker,
            EmbeddingService embeddingService,
            VectorRepository vectorRepository,
            RetrievalService retrievalService,
            LlmGenerationService llmGenerationService) {
        this.textChunker = textChunker;
        this.embeddingService = embeddingService;
        this.vectorRepository = vectorRepository;
        this.retrievalService = retrievalService;
        this.llmGenerationService = llmGenerationService;
    }

    /**
     * Divide el texto limpio de un documento en fragmentos, genera el embedding de cada uno
     * y los almacena en la tabla fragmentos_documentos con pgvector.
     *
     * @return Número total de fragmentos vectorizados y almacenados.
     */
    public int indexarContenidoDocumento(Documento documento, String textoLimpio) {
        List<String> fragmentos = textChunker.dividirEnFragmentos(textoLimpio);
        if (fragmentos.isEmpty()) {
            throw new DocumentProcessingException(
                    "No se pudieron generar fragmentos de texto válidos para el documento: " + documento.getTitulo());
        }

        vectorRepository.eliminarPorDocumentoId(documento.getId());

        int numeroFragmento = 1;
        for (String contenidoFragmento : fragmentos) {
            float[] embedding = embeddingService.generarEmbedding(contenidoFragmento);
            vectorRepository.guardarFragmento(
                    documento.getId(),
                    documento.getTitulo(),
                    documento.getCategoria(),
                    documento.getFuenteOrigen(),
                    documento.getNombreArchivo(),
                    numeroFragmento,
                    contenidoFragmento,
                    embedding
            );
            numeroFragmento++;
        }

        log.info("Documento '{}' (ID={}) indexado en pgvector con {} fragmentos.",
                documento.getTitulo(), documento.getId(), fragmentos.size());
        return fragmentos.size();
    }

    /**
     * Ejecuta el flujo RAG completo ante la pregunta de un usuario:
     * Pregunta -> Embedding -> Búsqueda en pgvector -> Recuperación de fragmentos ->
     * Construcción de contexto -> Modelo de IA -> Respuesta con fuentes verificadas.
     */
    public ChatResponse responderPregunta(String pregunta) {
        List<FragmentoRecuperado> candidatos = retrievalService.recuperarFragmentosRelevantes(pregunta);

        // Filtrar fragmentos que realmente tengan respaldos semánticos y conceptuales con la consulta
        List<FragmentoRecuperado> relevantes = filtrarPorRespaldoDocumental(pregunta, candidatos);

        if (relevantes.isEmpty()) {
            String mensajeSinInfo = "No se encontró información suficiente en los documentos educativos "
                    + "almacenados en la biblioteca de NutriGuide para responder de manera verificada a tu consulta: \""
                    + pregunta.trim() + "\". "
                    + "Para garantizar el rigor educativo y no inventar datos nutricionales, el asistente solo "
                    + "responde con información respaldada por los documentos cargados en la base de datos vectorial.";
            return new ChatResponse(mensajeSinInfo, List.of(), false, ADVERTENCIA_EDUCATIVA);
        }

        String contextoDocumental = retrievalService.construirContextoDocumental(relevantes);
        String respuestaGenerada = llmGenerationService.generarRespuestaConContexto(
                pregunta, contextoDocumental, relevantes);

        List<FuenteResponse> fuentes = relevantes.stream()
                .map(f -> new FuenteResponse(
                        f.tituloDocumento(),
                        f.contenido(),
                        f.documentoId(),
                        f.numeroFragmento(),
                        f.categoriaDocumento(),
                        f.fuenteOrigen(),
                        Math.round(f.similitud() * 1000.0) / 1000.0
                ))
                .toList();

        return new ChatResponse(respuestaGenerada, fuentes, true, ADVERTENCIA_EDUCATIVA);
    }

    private List<FragmentoRecuperado> filtrarPorRespaldoDocumental(
            String pregunta,
            List<FragmentoRecuperado> candidatos) {
        if (candidatos == null || candidatos.isEmpty()) {
            return List.of();
        }
        List<String> raicesPregunta = extraerRaicesSignificativas(pregunta);
        if (raicesPregunta.isEmpty()) {
            return candidatos;
        }

        List<FragmentoRecuperado> respaldados = new ArrayList<>();
        for (FragmentoRecuperado f : candidatos) {
            String textoDocNorm = normalizar(f.tituloDocumento() + " " + f.categoriaDocumento() + " " + f.contenido());
            boolean coincideTerminoClave = raicesPregunta.stream().anyMatch(textoDocNorm::contains);
            if (coincideTerminoClave || f.similitud() >= 0.72) {
                respaldados.add(f);
            }
        }
        return respaldados;
    }

    private List<String> extraerRaicesSignificativas(String texto) {
        String norm = normalizar(texto);
        String[] palabras = norm.split("\\s+");
        List<String> raices = new ArrayList<>();
        for (String p : palabras) {
            if (p.length() >= 4 && !PALABRAS_GENERICAS.contains(p)) {
                String raiz = p.length() >= 6 ? p.substring(0, p.length() - 2) : p;
                raices.add(raiz);
            }
        }
        return raices;
    }

    private String normalizar(String input) {
        if (input == null) {
            return "";
        }
        return Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}

