package com.nutriguide.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.nutriguide.common.AiServiceException;
import com.nutriguide.config.AiConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de embeddings (separado del servicio de generación de respuestas).
 * Usa el modelo de embeddings de Gemini con tipos de tarea distintos para documentos
 * (RETRIEVAL_DOCUMENT) y preguntas (RETRIEVAL_QUERY), valida la dimensión de cada vector
 * y lo normaliza (L2) para usar distancia coseno en pgvector.
 */
@Service
public class EmbeddingService {

    private final AiConfig aiConfig;
    private final GeminiClient geminiClient;

    public EmbeddingService(AiConfig aiConfig, GeminiClient geminiClient) {
        this.aiConfig = aiConfig;
        this.geminiClient = geminiClient;
    }

    /** Embedding de la pregunta de un usuario. */
    public float[] generarEmbeddingConsulta(String pregunta) {
        if (pregunta == null || pregunta.isBlank()) {
            throw new IllegalArgumentException("La pregunta no puede estar vacía.");
        }
        Map<String, Object> cuerpo = peticion(pregunta, "RETRIEVAL_QUERY", null);
        JsonNode respuesta = geminiClient.post(aiConfig.getEmbeddingModel(), "embedContent", cuerpo);
        return aVector(respuesta.path("embedding").path("values"));
    }

    /**
     * Embeddings de los fragmentos de un documento, en lotes (batchEmbedContents) para reducir
     * el número de llamadas al proveedor. Devuelve un vector por fragmento, en el mismo orden.
     */
    public List<float[]> generarEmbeddingsDocumento(List<String> fragmentos, String tituloDocumento) {
        List<float[]> vectores = new ArrayList<>(fragmentos.size());
        int lote = aiConfig.getEmbeddingBatchSize();
        for (int i = 0; i < fragmentos.size(); i += lote) {
            List<String> parte = fragmentos.subList(i, Math.min(i + lote, fragmentos.size()));
            List<Map<String, Object>> requests = parte.stream()
                    .map(texto -> peticion(texto, "RETRIEVAL_DOCUMENT", tituloDocumento))
                    .toList();
            JsonNode respuesta = geminiClient.post(
                    aiConfig.getEmbeddingModel(), "batchEmbedContents", Map.of("requests", requests));
            JsonNode embeddings = respuesta.path("embeddings");
            if (!embeddings.isArray() || embeddings.size() != parte.size()) {
                throw new AiServiceException("El proveedor de embeddings devolvió " + embeddings.size()
                        + " vectores para " + parte.size() + " fragmentos.");
            }
            for (JsonNode embedding : embeddings) {
                vectores.add(aVector(embedding.path("values")));
            }
        }
        return vectores;
    }

    private Map<String, Object> peticion(String texto, String taskType, String titulo) {
        String modelo = aiConfig.getEmbeddingModel();
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("model", modelo.startsWith("models/") ? modelo : "models/" + modelo);
        cuerpo.put("content", Map.of("parts", List.of(Map.of("text", texto))));
        cuerpo.put("taskType", taskType);
        if (titulo != null && !titulo.isBlank()) {
            cuerpo.put("title", titulo);
        }
        cuerpo.put("outputDimensionality", aiConfig.getEmbeddingDimension());
        return cuerpo;
    }

    private float[] aVector(JsonNode values) {
        if (!values.isArray() || values.isEmpty()) {
            throw new AiServiceException("La respuesta del proveedor de embeddings no contiene un vector.");
        }
        float[] vector = new float[values.size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) values.get(i).asDouble();
        }
        validarDimension(vector);
        return normalizarL2(vector);
    }

    /** El vector debe tener exactamente la dimensión de la columna vector(N) de pgvector. */
    public void validarDimension(float[] vector) {
        int esperada = aiConfig.getEmbeddingDimension();
        if (vector == null || vector.length != esperada) {
            throw new AiServiceException("Dimensión de embedding inválida: se esperaba " + esperada
                    + " y se obtuvo " + (vector == null ? 0 : vector.length) + ".");
        }
    }

    /**
     * Normalización L2. Necesaria porque gemini-embedding-001 solo devuelve vectores
     * normalizados en su dimensión completa (3072); al reducirla hay que normalizar.
     */
    public static float[] normalizarL2(float[] vector) {
        double suma = 0.0;
        for (float v : vector) {
            suma += (double) v * v;
        }
        if (suma <= 1e-12) {
            throw new AiServiceException("El proveedor de embeddings devolvió un vector nulo.");
        }
        double norma = Math.sqrt(suma);
        float[] resultado = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            resultado[i] = (float) (vector[i] / norma);
        }
        return resultado;
    }
}
