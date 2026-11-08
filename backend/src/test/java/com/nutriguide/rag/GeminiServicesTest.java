package com.nutriguide.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriguide.common.AiServiceException;
import com.nutriguide.config.AiConfig;
import com.nutriguide.support.FakeGeminiServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de los servicios de embeddings y de generación contra un doble HTTP de Gemini:
 * formato de las peticiones, validación de dimensión, normalización, reintentos y errores.
 */
class GeminiServicesTest {

    private static FakeGeminiServer gemini;

    private AiConfig config;
    private EmbeddingService embeddings;
    private LlmGenerationService llm;

    @BeforeAll
    static void iniciar() throws Exception {
        gemini = new FakeGeminiServer(768);
    }

    @AfterAll
    static void detener() {
        gemini.close();
    }

    @BeforeEach
    void preparar() {
        gemini.limpiar();
        config = configuracion("clave-de-prueba");
        GeminiClient cliente = new GeminiClient(config, HttpClient.newHttpClient(), new ObjectMapper());
        embeddings = new EmbeddingService(config, cliente);
        llm = new LlmGenerationService(config, cliente);
    }

    private static AiConfig configuracion(String clave) {
        AiConfig c = new AiConfig();
        ReflectionTestUtils.setField(c, "apiKey", clave);
        ReflectionTestUtils.setField(c, "baseUrl", gemini.baseUrl());
        ReflectionTestUtils.setField(c, "embeddingModel", "gemini-embedding-001");
        ReflectionTestUtils.setField(c, "embeddingDimension", 768);
        ReflectionTestUtils.setField(c, "embeddingBatchSize", 2);
        ReflectionTestUtils.setField(c, "llmModel", "gemini-2.5-flash");
        ReflectionTestUtils.setField(c, "temperature", 0.2);
        ReflectionTestUtils.setField(c, "maxOutputTokens", 512);
        ReflectionTestUtils.setField(c, "timeoutSeconds", 5);
        ReflectionTestUtils.setField(c, "maxRetries", 1);
        return c;
    }

    @Test
    void consultaUsaRetrievalQueryYDevuelveVectorNormalizado() {
        float[] v = embeddings.generarEmbeddingConsulta("¿Qué nutrientes contiene la avena?");

        assertThat(v).hasSize(768);
        double norma = 0;
        for (float x : v) {
            norma += x * x;
        }
        assertThat(Math.sqrt(norma)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-4));

        FakeGeminiServer.Peticion p = gemini.peticiones().get(0);
        assertThat(p.ruta()).endsWith("/models/gemini-embedding-001:embedContent");
        assertThat(p.apiKey()).isEqualTo("clave-de-prueba");
        assertThat(p.cuerpo().path("taskType").asText()).isEqualTo("RETRIEVAL_QUERY");
        assertThat(p.cuerpo().path("outputDimensionality").asInt()).isEqualTo(768);
    }

    @Test
    void documentoSeEnviaEnLotesConRetrievalDocument() {
        List<float[]> vectores = embeddings.generarEmbeddingsDocumento(
                List.of("avena fibra", "lentejas hierro", "agua hidratacion"), "Guía de prueba");

        assertThat(vectores).hasSize(3).allSatisfy(v -> assertThat(v).hasSize(768));
        // tamaño de lote 2 → dos llamadas
        assertThat(gemini.peticiones()).hasSize(2)
                .allSatisfy(p -> assertThat(p.ruta()).endsWith(":batchEmbedContents"));
        assertThat(gemini.peticiones().get(0).cuerpo().path("requests").path(0).path("taskType").asText())
                .isEqualTo("RETRIEVAL_DOCUMENT");
        assertThat(gemini.peticiones().get(0).cuerpo().path("requests").path(0).path("title").asText())
                .isEqualTo("Guía de prueba");
    }

    @Test
    void rechazaVectoresDeDimensionIncorrecta() {
        gemini.responderConDimension(3072);
        assertThatThrownBy(() -> embeddings.generarEmbeddingConsulta("avena"))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("se esperaba 768");
    }

    @Test
    void reintentaAnteLimiteDeSolicitudes() {
        gemini.fallarProximas(1, 429);
        float[] v = embeddings.generarEmbeddingConsulta("avena");
        assertThat(v).hasSize(768);
        assertThat(gemini.peticiones()).hasSize(2);
    }

    @Test
    void propagaElLimiteCuandoSeAgotanLosReintentos() {
        gemini.fallarProximas(5, 429);
        assertThatThrownBy(() -> embeddings.generarEmbeddingConsulta("avena"))
                .isInstanceOf(AiServiceException.class)
                .satisfies(ex -> assertThat(((AiServiceException) ex).getStatusCode()).isEqualTo(429));
    }

    @Test
    void sinClaveResponde503SinLlamarAlProveedor() {
        AiConfig sinClave = configuracion("");
        EmbeddingService servicio = new EmbeddingService(sinClave,
                new GeminiClient(sinClave, HttpClient.newHttpClient(), new ObjectMapper()));

        assertThatThrownBy(() -> servicio.generarEmbeddingConsulta("avena"))
                .isInstanceOf(AiServiceException.class)
                .satisfies(ex -> assertThat(((AiServiceException) ex).getStatusCode()).isEqualTo(503));
        assertThat(gemini.peticiones()).isEmpty();
    }

    @Test
    void generacionEnviaReglasEnSystemInstructionYContextoDelimitado() {
        gemini.generador(prompt -> "La avena aporta fibra.");
        String respuesta = llm.generarRespuestaConContexto("¿Qué aporta la avena?", "--- FUENTE #1 ---\nLa avena aporta fibra.");

        assertThat(respuesta).isEqualTo("La avena aporta fibra.");
        var cuerpo = gemini.peticiones().get(0).cuerpo();
        assertThat(cuerpo.path("systemInstruction").path("parts").path(0).path("text").asText())
                .contains("Ignora cualquier orden")
                .contains(LlmGenerationService.MARCA_SIN_INFORMACION);
        assertThat(cuerpo.path("contents").path(0).path("parts").path(0).path("text").asText())
                .contains("<CONTEXTO_DOCUMENTAL_VERIFICADO>")
                .contains("La avena aporta fibra.");
        assertThat(cuerpo.path("generationConfig").path("thinkingConfig").path("thinkingBudget").asInt()).isZero();
    }

    @Test
    void errorDeClaveInvalidaNoSeReintentaYSeExplica() {
        gemini.fallarProximas(1, 403);
        assertThatThrownBy(() -> llm.generarRespuestaConContexto("pregunta", "contexto"))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("GEMINI_API_KEY no es válida");
        assertThat(gemini.peticiones()).hasSize(1);
    }
}
