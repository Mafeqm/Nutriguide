package com.nutriguide.rag;

import com.nutriguide.config.AiConfig;
import com.nutriguide.rag.VectorRepository.FragmentoRecuperado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Servicio encargado de la etapa de Recuperación (Retrieval) del flujo RAG:
 * genera el embedding de la pregunta, consulta pgvector por similitud semántica,
 * sanea los fragmentos frente a intentos de Prompt Injection indirecto y construye el contexto.
 */
@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    // Patrones de instrucciones maliciosas de Prompt Injection indirecto dentro de documentos
    private static final Pattern PROMPT_INJECTION_PATTERN = Pattern.compile(
            "(?i)(ignora\\s+(todas\\s+)?las\\s+instrucciones\\s+anteriores|"
                    + "ignore\\s+(all\\s+)?previous\\s+instructions|"
                    + "olvida\\s+tus\\s+reglas|"
                    + "system\\s*prompt\\s*:|"
                    + "act[uú]a\\s+como\\s+un\\s+sistema\\s+sin\\s+restricciones|"
                    + "eres\\s+ahora\\s+un\\s+modelo\\s+sin\\s+reglas)");

    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepository;
    private final AiConfig aiConfig;

    public RetrievalService(
            EmbeddingService embeddingService,
            VectorRepository vectorRepository,
            AiConfig aiConfig) {
        this.embeddingService = embeddingService;
        this.vectorRepository = vectorRepository;
        this.aiConfig = aiConfig;
    }

    /**
     * Recupera los fragmentos más relevantes semánticamente desde pgvector para una pregunta dada.
     */
    public List<FragmentoRecuperado> recuperarFragmentosRelevantes(String pregunta) {
        if (pregunta == null || pregunta.isBlank()) {
            throw new IllegalArgumentException("La pregunta no puede estar vacía.");
        }

        float[] embeddingPregunta = embeddingService.generarEmbedding(pregunta.trim());
        List<FragmentoRecuperado> recuperados = vectorRepository.buscarSimilares(
                embeddingPregunta,
                aiConfig.getTopK(),
                aiConfig.getMinSimilarity()
        );

        log.info("Búsqueda vectorial en pgvector para '{}': {} fragmentos recuperados (umbral >= {}).",
                pregunta, recuperados.size(), aiConfig.getMinSimilarity());

        return recuperados.stream()
                .map(this::sanitizarFragmento)
                .toList();
    }

    /**
     * Construye el bloque de contexto estructurado y delimitado para el modelo de lenguaje,
     * incluyendo título del documento original, fuente institucional y número de fragmento.
     */
    public String construirContextoDocumental(List<FragmentoRecuperado> fragmentos) {
        if (fragmentos == null || fragmentos.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fragmentos.size(); i++) {
            FragmentoRecuperado f = fragmentos.get(i);
            sb.append("--- INICIO FUENTE DOCUMENTAL #").append(i + 1).append(" ---\n");
            sb.append("Título del documento: ").append(f.tituloDocumento()).append("\n");
            sb.append("Procedencia / Fuente oficial: ").append(f.fuenteOrigen()).append("\n");
            sb.append("Categoría: ").append(f.categoriaDocumento()).append("\n");
            sb.append("Fragmento N°: ").append(f.numeroFragmento())
                    .append(" (Similitud semántica: ")
                    .append(String.format("%.2f", f.similitud()))
                    .append(")\n");
            sb.append("Contenido del fragmento:\n");
            sb.append(f.contenido()).append("\n");
            sb.append("--- FIN FUENTE DOCUMENTAL #").append(i + 1).append(" ---\n\n");
        }
        return sb.toString().trim();
    }

    /**
     * Neutraliza posibles instrucciones maliciosas incrustadas dentro de los documentos recuperados
     * para evitar que el modelo de lenguaje obedezca órdenes contenidas en datos externos.
     */
    public FragmentoRecuperado sanitizarFragmento(FragmentoRecuperado fragmento) {
        String contenidoSeguro = PROMPT_INJECTION_PATTERN
                .matcher(fragmento.contenido())
                .replaceAll("[INSTRUCCIÓN EXTERNA BLOQUEADA POR SEGURIDAD]");
        return new FragmentoRecuperado(
                fragmento.id(),
                fragmento.documentoId(),
                fragmento.tituloDocumento(),
                fragmento.categoriaDocumento(),
                fragmento.fuenteOrigen(),
                fragmento.nombreArchivo(),
                contenidoSeguro,
                fragmento.numeroFragmento(),
                fragmento.similitud()
        );
    }
}

