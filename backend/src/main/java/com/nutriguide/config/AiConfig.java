package com.nutriguide.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configuración centralizada del proveedor de IA (Google Gemini API) y de los parámetros del RAG.
 * La clave se obtiene de la variable de entorno GEMINI_API_KEY; no existe ningún modo simulado.
 */
@Configuration
public class AiConfig {

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    @Value("${ai.embedding.model:gemini-embedding-001}")
    private String embeddingModel;

    @Value("${ai.embedding.dimension:768}")
    private int embeddingDimension;

    @Value("${ai.embedding.batch-size:50}")
    private int embeddingBatchSize;

    @Value("${ai.llm.model:gemini-2.5-flash}")
    private String llmModel;

    @Value("${ai.llm.temperature:0.2}")
    private double temperature;

    @Value("${ai.llm.max-output-tokens:2048}")
    private int maxOutputTokens;

    @Value("${ai.http.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${ai.http.max-retries:3}")
    private int maxRetries;

    @Value("${rag.chunk.max-chars:900}")
    private int chunkMaxChars;

    @Value("${rag.chunk.overlap-chars:150}")
    private int chunkOverlapChars;

    @Value("${rag.retrieval.top-k:4}")
    private int topK;

    @Value("${rag.retrieval.min-similarity:0.55}")
    private double minSimilarity;

    @Bean
    public HttpClient aiHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 20)))
                .build();
    }

    /** true si hay una clave configurada (y no es el texto de ejemplo de .env.example). */
    public boolean hasValidApiKey() {
        return apiKey != null
                && !apiKey.isBlank()
                && !apiKey.startsWith("TU_")
                && !apiKey.startsWith("YOUR_");
    }

    public String getApiKey() {
        return apiKey == null ? "" : apiKey.trim();
    }

    public String getBaseUrl() {
        return baseUrl.replaceAll("/+$", "");
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    public int getEmbeddingBatchSize() {
        return Math.max(1, Math.min(embeddingBatchSize, 100));
    }

    public String getLlmModel() {
        return llmModel;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getMaxRetries() {
        return Math.max(0, maxRetries);
    }

    public int getChunkMaxChars() {
        return chunkMaxChars;
    }

    public int getChunkOverlapChars() {
        return chunkOverlapChars;
    }

    public int getTopK() {
        return topK;
    }

    public double getMinSimilarity() {
        return minSimilarity;
    }
}
