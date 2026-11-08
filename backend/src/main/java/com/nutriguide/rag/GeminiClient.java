package com.nutriguide.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriguide.common.AiServiceException;
import com.nutriguide.config.AiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/**
 * Cliente HTTP de bajo nivel para la API de Google Gemini (java.net.http.HttpClient).
 * Centraliza la autenticación por cabecera, los tiempos de espera y los reintentos con
 * espera creciente ante límites de solicitudes (HTTP 429) y errores transitorios (5xx).
 * La clave nunca se escribe en los registros ni en los mensajes de error.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final Set<Integer> REINTENTABLES = Set.of(429, 500, 502, 503, 504);

    private final AiConfig aiConfig;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(AiConfig aiConfig, HttpClient aiHttpClient, ObjectMapper objectMapper) {
        this.aiConfig = aiConfig;
        this.httpClient = aiHttpClient;
        this.objectMapper = objectMapper;
    }

    /** Lanza 503 si el proveedor de IA no está configurado. */
    public void exigirConfiguracion() {
        if (!aiConfig.hasValidApiKey()) {
            throw new AiServiceException(
                    "El servicio de inteligencia artificial no está configurado en el servidor (falta GEMINI_API_KEY).",
                    503);
        }
    }

    /**
     * Envía un POST a {baseUrl}/models/{modelo}:{accion} y devuelve el JSON de respuesta.
     */
    public JsonNode post(String modelo, String accion, Object cuerpo) {
        exigirConfiguracion();
        String modelPath = modelo.startsWith("models/") ? modelo : "models/" + modelo;
        URI uri = URI.create(aiConfig.getBaseUrl() + "/" + modelPath + ":" + accion);

        String json;
        try {
            json = objectMapper.writeValueAsString(cuerpo);
        } catch (IOException ex) {
            throw new AiServiceException("No se pudo serializar la petición al proveedor de IA.", ex);
        }

        int maxIntentos = aiConfig.getMaxRetries() + 1;
        for (int intento = 1; ; intento++) {
            HttpResponse<String> response;
            try {
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", aiConfig.getApiKey())
                        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                        .build();
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AiServiceException("La petición al proveedor de IA fue interrumpida.", ex);
            } catch (IOException ex) {
                if (intento < maxIntentos) {
                    esperar(intento, null, "error de red: " + ex.getMessage());
                    continue;
                }
                throw new AiServiceException("No fue posible conectar con el proveedor de IA: " + ex.getMessage(), ex);
            }

            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                try {
                    return objectMapper.readTree(response.body());
                } catch (IOException ex) {
                    throw new AiServiceException("El proveedor de IA devolvió un JSON inválido.", ex);
                }
            }
            if (REINTENTABLES.contains(status) && intento < maxIntentos) {
                esperar(intento, response.headers().firstValue("Retry-After").orElse(null), "HTTP " + status);
                continue;
            }
            throw new AiServiceException(describirError(status, response.body()), status == 429 ? 429 : 502);
        }
    }

    private void esperar(int intento, String retryAfter, String motivo) {
        long esperaMs = 1000L * (1L << (intento - 1)); // 1 s, 2 s, 4 s...
        if (retryAfter != null) {
            try {
                esperaMs = Math.max(esperaMs, Long.parseLong(retryAfter.trim()) * 1000L);
            } catch (NumberFormatException ignored) {
                // formato de fecha: se usa la espera exponencial
            }
        }
        esperaMs = Math.min(esperaMs, 30_000L);
        log.warn("Proveedor de IA no disponible ({}). Reintento {} en {} ms.", motivo, intento, esperaMs);
        try {
            Thread.sleep(esperaMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("La espera de reintento fue interrumpida.", ex);
        }
    }

    private String describirError(int status, String cuerpo) {
        String detalle = "";
        try {
            detalle = objectMapper.readTree(cuerpo).path("error").path("message").asText("");
        } catch (IOException ignored) {
            // cuerpo no JSON
        }
        if (detalle.length() > 300) {
            detalle = detalle.substring(0, 300) + "…";
        }
        String base = switch (status) {
            case 400 -> "El proveedor de IA rechazó la petición (HTTP 400)";
            case 401, 403 -> "La clave GEMINI_API_KEY no es válida o no tiene permisos (HTTP " + status + ")";
            case 404 -> "El modelo de IA configurado no existe (HTTP 404)";
            case 429 -> "Se alcanzó el límite de solicitudes del proveedor de IA (HTTP 429). Intenta más tarde";
            default -> "El proveedor de IA devolvió un error (HTTP " + status + ")";
        };
        return detalle.isBlank() ? base + "." : base + ": " + detalle;
    }
}
