package com.nutriguide.ia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Conexión con el servicio de IA de NutriGuide, implementado como un flujo de n8n
 * (carpeta IA/ del repositorio) que usa Qdrant como base vectorial y Google Gemini.
 * El backend solo conoce la URL de n8n y el token compartido de sus webhooks.
 */
@Configuration
public class IaConfig {

    private static final Logger log = LoggerFactory.getLogger(IaConfig.class);

    @Value("${nutriguide.ia.n8n-url:http://localhost:5680}")
    private String n8nUrl;

    @Value("${nutriguide.ia.webhook-token:}")
    private String webhookToken;

    @Value("${nutriguide.ia.chat-timeout-seconds:90}")
    private int chatTimeoutSeconds;

    @Value("${nutriguide.ia.ingesta-timeout-seconds:300}")
    private int ingestaTimeoutSeconds;

    /**
     * HTTP/1.1 explícito: por defecto el cliente de Java intenta pasar a HTTP/2 con la cabecera
     * "Upgrade: h2c", y el servidor de n8n (Node.js) deja esas peticiones sin responder.
     */
    @Bean
    public HttpClient n8nHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void avisarConfiguracion() {
        if (!estaConfigurado()) {
            log.warn("N8N_WEBHOOK_TOKEN no está configurado: el chat responderá 503 y los documentos quedarán "
                    + "PENDIENTES de indexar en Qdrant.");
        } else {
            log.info("Servicio de IA: flujo n8n en {}", getN8nUrl());
        }
    }

    public boolean estaConfigurado() {
        return webhookToken != null && !webhookToken.isBlank() && !webhookToken.startsWith("CAMBIA_");
    }

    public String getN8nUrl() {
        return n8nUrl.replaceAll("/+$", "");
    }

    public String getWebhookToken() {
        return webhookToken == null ? "" : webhookToken.trim();
    }

    public Duration getChatTimeout() {
        return Duration.ofSeconds(chatTimeoutSeconds);
    }

    public Duration getIngestaTimeout() {
        return Duration.ofSeconds(ingestaTimeoutSeconds);
    }
}
