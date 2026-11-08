package com.nutriguide.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriguide.common.AiServiceException;
import com.nutriguide.modules.chat.ChatResponse;
import com.nutriguide.modules.documento.Documento;
import com.nutriguide.support.FakeN8nServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrato entre el backend y los webhooks del flujo n8n: formato de las peticiones, cabecera de
 * autenticación, conversión de la respuesta y traducción de errores a códigos HTTP del backend.
 */
class N8nClientTest {

    private static final String TOKEN = "token-de-prueba";
    private static FakeN8nServer n8n;

    private N8nClient cliente;

    @BeforeAll
    static void iniciar() throws Exception {
        n8n = new FakeN8nServer(TOKEN);
    }

    @AfterAll
    static void detener() {
        n8n.close();
    }

    @BeforeEach
    void preparar() {
        n8n.limpiar();
        cliente = cliente(n8n.url(), TOKEN);
    }

    private static N8nClient cliente(String url, String token) {
        IaConfig config = new IaConfig();
        ReflectionTestUtils.setField(config, "n8nUrl", url);
        ReflectionTestUtils.setField(config, "webhookToken", token);
        ReflectionTestUtils.setField(config, "chatTimeoutSeconds", 5);
        ReflectionTestUtils.setField(config, "ingestaTimeoutSeconds", 5);
        return new N8nClient(config, HttpClient.newHttpClient(), new ObjectMapper());
    }

    @Test
    void preguntarEnviaLaPreguntaConTokenYConvierteLaRespuesta() {
        ChatResponse r = cliente.preguntar("¿Qué aporta la avena?");

        assertThat(r.getRespuesta()).isEqualTo("Respuesta de prueba para: ¿Qué aporta la avena?");
        assertThat(r.isInformacionSuficiente()).isTrue();
        assertThat(r.getFuentes()).singleElement().satisfies(f -> {
            assertThat(f.getTitulo()).isEqualTo("Guía de prueba");
            assertThat(f.getContenido()).isEqualTo("Fragmento de prueba");
            assertThat(f.getDocumentoId()).isEqualTo(1L);
            assertThat(f.getSimilitud()).isEqualTo(0.81);
        });
        FakeN8nServer.Peticion p = n8n.peticiones().get(0);
        assertThat(p.ruta()).isEqualTo("chat");
        assertThat(p.token()).isEqualTo(TOKEN);
        assertThat(p.cuerpo().path("pregunta").asText()).isEqualTo("¿Qué aporta la avena?");
    }

    @Test
    void respetaCuandoElFlujoIndicaQueNoHayInformacion() {
        n8n.respuestaChat(q -> Map.of("respuesta", "No encontré información suficiente.",
                "fuentes", List.of(), "informacionSuficiente", false));
        ChatResponse r = cliente.preguntar("¿Kubernetes?");
        assertThat(r.isInformacionSuficiente()).isFalse();
        assertThat(r.getFuentes()).isEmpty();
    }

    @Test
    void indexarEnviaTextoYMetadatosYDevuelveLosFragmentos() {
        Documento doc = new Documento();
        doc.setId(7L);
        doc.setTitulo("Guía");
        doc.setCategoria("Cereales");
        doc.setFuenteOrigen("FAO");
        doc.setContenidoTexto("x".repeat(2000));

        assertThat(cliente.indexar(doc)).isEqualTo(3);
        var cuerpo = n8n.peticiones().get(0).cuerpo();
        assertThat(cuerpo.path("documentoId").asLong()).isEqualTo(7L);
        assertThat(cuerpo.path("titulo").asText()).isEqualTo("Guía");
        assertThat(cuerpo.path("fuenteOrigen").asText()).isEqualTo("FAO");
        assertThat(cuerpo.path("contenido").asText()).hasSize(2000);
    }

    @Test
    void eliminarLlamaAlWebhookDeEliminacion() {
        cliente.eliminar(9L);
        assertThat(n8n.peticiones().get(0).ruta()).isEqualTo("eliminar");
        assertThat(n8n.peticiones().get(0).cuerpo().path("documentoId").asLong()).isEqualTo(9L);
    }

    @Test
    void traduceLosErroresDelFlujo() {
        n8n.forzarCodigo(502);
        assertThatThrownBy(() -> cliente.preguntar("pregunta"))
                .isInstanceOf(AiServiceException.class).hasMessage("Error simulado del flujo");

        n8n.forzarCodigo(404);
        assertThatThrownBy(() -> cliente.preguntar("pregunta"))
                .isInstanceOf(AiServiceException.class).hasMessageContaining("no está publicado")
                .satisfies(e -> assertThat(((AiServiceException) e).getStatusCode()).isEqualTo(503));

        n8n.forzarCodigo(503);
        assertThatThrownBy(() -> cliente.preguntar("pregunta"))
                .isInstanceOf(AiServiceException.class).hasMessage("Error simulado del flujo")
                .satisfies(e -> assertThat(((AiServiceException) e).getStatusCode()).isEqualTo(503));

        n8n.forzarCodigo(400);
        assertThatThrownBy(() -> cliente.preguntar("pregunta"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Error simulado del flujo");
    }

    @Test
    void tokenIncorrectoSeExplica() {
        assertThatThrownBy(() -> cliente(n8n.url(), "otro-token").preguntar("pregunta"))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("NUTRIGUIDE_WEBHOOK_TOKEN");
    }

    @Test
    void sinTokenOSinN8nResponde503() {
        assertThatThrownBy(() -> cliente(n8n.url(), "").preguntar("pregunta"))
                .isInstanceOf(AiServiceException.class)
                .satisfies(e -> assertThat(((AiServiceException) e).getStatusCode()).isEqualTo(503));
        assertThat(n8n.peticiones()).isEmpty();

        assertThatThrownBy(() -> cliente("http://127.0.0.1:1", TOKEN).preguntar("pregunta"))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("No se pudo conectar con el servicio de IA")
                .satisfies(e -> assertThat(((AiServiceException) e).getStatusCode()).isEqualTo(503));
    }
}
