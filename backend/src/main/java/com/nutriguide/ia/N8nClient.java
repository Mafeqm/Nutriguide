package com.nutriguide.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriguide.common.AiServiceException;
import com.nutriguide.modules.chat.ChatResponse;
import com.nutriguide.modules.chat.ChatResponse.FuenteResponse;
import com.nutriguide.modules.documento.Documento;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente de los webhooks del flujo n8n "NutriGuide IA" (IA/nutriguide-ia.json):
 * <ul>
 *   <li>{@code POST /webhook/nutriguide/chat}: pregunta → RAG con Qdrant y Gemini → respuesta con fuentes.</li>
 *   <li>{@code POST /webhook/nutriguide/ingesta}: texto de un documento → fragmentos y embeddings en Qdrant.</li>
 *   <li>{@code POST /webhook/nutriguide/eliminar}: borra de Qdrant los fragmentos de un documento.</li>
 * </ul>
 * Todas las llamadas llevan la cabecera X-NutriGuide-Token.
 */
@Component
public class N8nClient {

    public static final String CABECERA_TOKEN = "X-NutriGuide-Token";

    private final IaConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public N8nClient(IaConfig config, HttpClient n8nHttpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = n8nHttpClient;
        this.objectMapper = objectMapper;
    }

    public boolean estaConfigurado() {
        return config.estaConfigurado();
    }

    /** Pregunta al asistente RAG. */
    public ChatResponse preguntar(String pregunta) {
        JsonNode r = post("chat", Map.of("pregunta", pregunta), config.getChatTimeout());
        String respuesta = r.path("respuesta").asText("").trim();
        if (respuesta.isEmpty()) {
            throw new AiServiceException("El servicio de IA devolvió una respuesta vacía.");
        }
        List<FuenteResponse> fuentes = new ArrayList<>();
        for (JsonNode f : r.path("fuentes")) {
            fuentes.add(new FuenteResponse(
                    f.path("titulo").asText("Documento"),
                    f.path("contenido").asText(""),
                    f.hasNonNull("documentoId") ? f.path("documentoId").asLong() : null,
                    null,
                    f.path("categoria").asText(null),
                    f.path("fuenteOrigen").asText(null),
                    f.hasNonNull("similitud") ? f.path("similitud").asDouble() : null));
        }
        return new ChatResponse(respuesta, fuentes,
                r.path("informacionSuficiente").asBoolean(!fuentes.isEmpty()),
                r.path("advertenciaEducativa").asText(null));
    }

    /**
     * Envía el texto del documento para fragmentarlo e indexarlo en Qdrant (reemplaza los fragmentos
     * anteriores del mismo documento).
     *
     * @return número de fragmentos guardados
     */
    public int indexar(Documento doc) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("documentoId", doc.getId());
        cuerpo.put("titulo", doc.getTitulo());
        cuerpo.put("categoria", doc.getCategoria());
        cuerpo.put("fuenteOrigen", doc.getFuenteOrigen());
        cuerpo.put("contenido", doc.getContenidoTexto());
        JsonNode r = post("ingesta", cuerpo, config.getIngestaTimeout());
        int fragmentos = r.path("fragmentos").asInt(0);
        if (fragmentos <= 0) {
            throw new AiServiceException("El servicio de IA no indexó ningún fragmento del documento.");
        }
        return fragmentos;
    }

    /** Borra de Qdrant los fragmentos de un documento. */
    public void eliminar(Long documentoId) {
        post("eliminar", Map.of("documentoId", documentoId), config.getChatTimeout());
    }

    private JsonNode post(String ruta, Object cuerpo, Duration timeout) {
        if (!config.estaConfigurado()) {
            throw new AiServiceException(
                    "El servicio de inteligencia artificial no está configurado en el servidor (falta N8N_WEBHOOK_TOKEN).",
                    503);
        }
        HttpResponse<String> response;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.getN8nUrl() + "/webhook/nutriguide/" + ruta))
                    .timeout(timeout)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header(CABECERA_TOKEN, config.getWebhookToken())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(cuerpo), StandardCharsets.UTF_8))
                    .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("La petición al servicio de IA fue interrumpida.", ex);
        } catch (java.net.http.HttpTimeoutException ex) {
            throw new AiServiceException("El servicio de IA tardó demasiado en responder.", 503);
        } catch (IOException ex) {
            throw new AiServiceException("No se pudo conectar con el servicio de IA (n8n en " + config.getN8nUrl()
                    + "). Verifique que los contenedores de la carpeta IA estén en ejecución.", 503);
        }

        JsonNode json = leer(response.body());
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return json;
        }
        String mensaje = json.path("mensaje").asText(json.path("message").asText(""));
        throw switch (status) {
            case 400 -> new IllegalArgumentException(mensaje.isBlank() ? "Petición no válida para el servicio de IA." : mensaje);
            case 401, 403 -> new AiServiceException(
                    "n8n rechazó el token de los webhooks: N8N_WEBHOOK_TOKEN del backend debe coincidir con "
                            + "NUTRIGUIDE_WEBHOOK_TOKEN de IA/.env.", 502);
            case 404 -> new AiServiceException(
                    "El flujo de n8n no está publicado (webhook /nutriguide/" + ruta + " no encontrado).", 503);
            case 429 -> new AiServiceException("Se alcanzó el límite de solicitudes del servicio de IA. Intenta más tarde.", 429);
            case 503 -> new AiServiceException(mensaje.isBlank()
                    ? "El servicio de IA no está disponible en este momento." : mensaje, 503);
            default -> new AiServiceException(mensaje.isBlank()
                    ? "El servicio de IA devolvió un error (HTTP " + status + ")." : mensaje, 502);
        };
    }

    private JsonNode leer(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(cuerpo);
        } catch (IOException ex) {
            return objectMapper.createObjectNode();
        }
    }
}
