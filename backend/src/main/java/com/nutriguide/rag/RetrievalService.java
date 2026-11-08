package com.nutriguide.rag;

import com.nutriguide.config.AiConfig;
import com.nutriguide.rag.VectorRepository.FragmentoRecuperado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Etapa de recuperación del RAG: genera el embedding de la pregunta, busca en pgvector los
 * fragmentos más similares, neutraliza instrucciones maliciosas que puedan venir dentro de los
 * documentos y construye el contexto delimitado que recibe el modelo de lenguaje.
 */
@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    static final String TEXTO_BLOQUEADO = "[INSTRUCCIÓN EXTERNA BLOQUEADA POR SEGURIDAD]";

    // Instrucciones típicas de inyección de prompt indirecta (español e inglés)
    private static final Pattern PROMPT_INJECTION = Pattern.compile(
            "(?iu)(ignora\\s+(todas\\s+)?(las\\s+)?instrucciones(\\s+anteriores|\\s+previas)?"
                    + "|ignore\\s+(all\\s+)?(the\\s+)?(previous|prior|above)\\s+instructions"
                    + "|olvida\\s+(todas\\s+)?(tus|las)\\s+(reglas|instrucciones)"
                    + "|disregard\\s+(all\\s+)?(previous|prior)\\s+instructions"
                    + "|system\\s*prompt\\s*:"
                    + "|act[uú]a\\s+como\\s+(un\\s+)?(sistema|modelo|asistente)\\s+sin\\s+(restricciones|reglas)"
                    + "|eres\\s+ahora\\s+(un\\s+)?(modelo|asistente)\\s+sin\\s+(reglas|restricciones)"
                    + "|you\\s+are\\s+now\\s+(a|an)\\s+\\w+)");

    // Evita que un documento cierre o abra el bloque de contexto
    private static final Pattern DELIMITADOR = Pattern.compile("(?i)</?\\s*CONTEXTO_DOCUMENTAL_VERIFICADO\\s*>");

    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepository;
    private final AiConfig aiConfig;

    public RetrievalService(EmbeddingService embeddingService, VectorRepository vectorRepository, AiConfig aiConfig) {
        this.embeddingService = embeddingService;
        this.vectorRepository = vectorRepository;
        this.aiConfig = aiConfig;
    }

    public List<FragmentoRecuperado> recuperarFragmentosRelevantes(String pregunta) {
        float[] embeddingPregunta = embeddingService.generarEmbeddingConsulta(pregunta.trim());
        List<FragmentoRecuperado> recuperados = vectorRepository.buscarSimilares(
                embeddingPregunta, aiConfig.getTopK(), aiConfig.getMinSimilarity());
        if (log.isInfoEnabled()) {
            log.info("Búsqueda vectorial: {} fragmentos con similitud >= {} (mejor: {}).",
                    recuperados.size(), aiConfig.getMinSimilarity(),
                    recuperados.isEmpty() ? "-" : String.format(Locale.ROOT, "%.3f", recuperados.get(0).similitud()));
        }
        return recuperados.stream().map(this::sanitizarFragmento).toList();
    }

    public String construirContextoDocumental(List<FragmentoRecuperado> fragmentos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fragmentos.size(); i++) {
            FragmentoRecuperado f = fragmentos.get(i);
            sb.append("--- FUENTE DOCUMENTAL #").append(i + 1).append(" ---\n")
                    .append("Título del documento: ").append(f.tituloDocumento()).append('\n')
                    .append("Procedencia: ").append(f.fuenteOrigen()).append('\n')
                    .append("Categoría: ").append(f.categoriaDocumento()).append('\n')
                    .append("Fragmento N.º ").append(f.numeroFragmento()).append('\n')
                    .append("Contenido:\n").append(f.contenido()).append("\n\n");
        }
        return sb.toString().trim();
    }

    /** Sustituye instrucciones maliciosas y delimitadores del prompt dentro de un fragmento. */
    public FragmentoRecuperado sanitizarFragmento(FragmentoRecuperado f) {
        String limpio = PROMPT_INJECTION.matcher(f.contenido()).replaceAll(TEXTO_BLOQUEADO);
        limpio = DELIMITADOR.matcher(limpio).replaceAll(TEXTO_BLOQUEADO);
        return new FragmentoRecuperado(f.id(), f.documentoId(), f.tituloDocumento(), f.categoriaDocumento(),
                f.fuenteOrigen(), f.nombreArchivo(), limpio, f.numeroFragmento(), f.similitud());
    }
}
