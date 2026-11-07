package com.nutriguide.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configuración centralizada para el proveedor de Inteligencia Artificial (Google Gemini API)
 * y parámetros del sistema RAG. Las credenciales se obtienen mediante variables de entorno.
 */
@Configuration
public class AiConfig {

    @Value("${ai.provider:gemini}")
    private String provider;

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    @Value("${ai.embedding.model:gemini-embedding-001}")
    private String embeddingModel;

    @Value("${ai.embedding.dimension:768}")
    private int embeddingDimension;

    @Value("${ai.embedding.fallback-enabled:true}")
    private boolean embeddingFallbackEnabled;

    @Value("${ai.llm.model:gemini-2.5-flash}")
    private String llmModel;

    @Value("${ai.llm.temperature:0.2}")
    private double temperature;

    @Value("${ai.llm.max-output-tokens:1024}")
    private int maxOutputTokens;

    @Value("${ai.http.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${ai.http.max-retries:2}")
    private int maxRetries;

    @Value("${rag.chunk.max-chars:750}")
    private int chunkMaxChars;

    @Value("${rag.chunk.overlap-chars:120}")
    private int chunkOverlapChars;

    @Value("${rag.retrieval.top-k:4}")
    private int topK;

    @Value("${rag.retrieval.min-similarity:0.25}")
    private double minSimilarity;

    @Bean
    public HttpClient aiHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    public boolean hasValidApiKey() {
        return apiKey != null
                && !apiKey.isBlank()
                && !apiKey.startsWith("TU_")
                && !apiKey.startsWith("YOUR_");
    }

    public String getProvider() {
        return provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    public boolean isEmbeddingFallbackEnabled() {
        return embeddingFallbackEnabled;
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
        return maxRetries;
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

