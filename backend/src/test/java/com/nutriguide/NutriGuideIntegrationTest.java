package com.nutriguide;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriguide.support.PdfDePrueba;
import com.nutriguide.support.FakeN8nServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de integración del backend sobre PostgreSQL 16 REAL (Testcontainers).
 * El flujo n8n (Qdrant + Gemini) se sustituye por {@link FakeN8nServer}, que respeta el mismo
 * contrato JSON; el flujo real se prueba por separado (ver IA/README.md).
 * Requiere Docker; si no está disponible, la prueba se omite.
 */
@SpringBootTest(properties = {
        "nutriguide.ia.webhook-token=" + NutriGuideIntegrationTest.TOKEN_N8N,
        "nutriguide.admin.token=token-de-prueba",
        "nutriguide.chat.max-requests-per-minute=0"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NutriGuideIntegrationTest {

    static final String TOKEN_N8N = "token-n8n-de-prueba";
    private static final String ADMIN = "X-Admin-Token";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static final FakeN8nServer N8N = iniciarN8n();
    static final String UPLOADS = uploads();

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("nutriguide.ia.n8n-url", N8N::url);
        registry.add("nutriguide.storage.upload-dir", () -> UPLOADS);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    @AfterAll
    static void detener() {
        N8N.close();
    }

    @BeforeEach
    void limpiar() {
        N8N.limpiar();
    }

    @Test
    @Order(1)
    void elEsquemaEsRelacionalYSinTablasVectoriales() {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'fragmentos_documentos'", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'documentos' AND column_name = 'total_fragmentos'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    @Order(2)
    void losDocumentosInicialesSeIndexanAtravesDeN8n() throws Exception {
        mvc.perform(get("/api/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].estadoProcesamiento", everyItem(is("PROCESADO"))))
                .andExpect(jsonPath("$[*].totalFragmentos", everyItem(greaterThan(0))))
                .andExpect(jsonPath("$[0].contenido", not(emptyOrNullString())))
                .andExpect(jsonPath("$[0].url", containsString("/api/documentos/")))
                .andExpect(jsonPath("$[0].rutaArchivo").doesNotExist());
    }

    @Test
    @Order(2)
    void elBotonAbrirDocumentoFuncionaAunqueFalteElArchivoEnDisco() throws Exception {
        long id = jdbc.queryForObject("SELECT MIN(id) FROM documentos", Long.class);
        mvc.perform(get("/api/documentos/{id}/archivo", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));

        // Simula un proyecto movido o una carpeta de subidas distinta
        jdbc.update("UPDATE documentos SET ruta_archivo = '/ruta/que/no/existe/doc.txt' WHERE id = ?", id);
        String contenido = jdbc.queryForObject("SELECT contenido_texto FROM documentos WHERE id = ?", String.class, id);
        mvc.perform(get("/api/documentos/{id}/archivo", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(contenido));
    }

    @Test
    @Order(3)
    void alimentosYRecetasSalenDePostgresql() throws Exception {
        mvc.perform(get("/api/alimentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(14)))
                .andExpect(jsonPath("$[0].tamanoPorcion", is(100.0)))
                .andExpect(jsonPath("$[0].unidadPorcion", is("g")));
        mvc.perform(get("/api/alimentos/buscar").param("nombre", "brocoli"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre", is("Brócoli fresco crudo")));
        mvc.perform(get("/api/alimentos/{id}", 999_999)).andExpect(status().isNotFound());
        mvc.perform(get("/api/recetas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].pasos", not(empty())))
                .andExpect(jsonPath("$[0].informacionNutricional.calorias", notNullValue()));
    }

    @Test
    @Order(4)
    void elChatDelegaEnN8nYDevuelveElContratoDelFrontend() throws Exception {
        JsonNode r = preguntar("¿Qué nutrientes contiene la avena?");

        assertThat(r.path("respuesta").asText()).contains("¿Qué nutrientes contiene la avena?");
        assertThat(r.path("informacionSuficiente").asBoolean()).isTrue();
        assertThat(r.path("fuentes").path(0).path("titulo").asText()).isEqualTo("Guía de prueba");
        assertThat(r.path("fuentes").path(0).path("contenido").asText()).isEqualTo("Fragmento de prueba");

        FakeN8nServer.Peticion p = N8N.peticiones().get(0);
        assertThat(p.ruta()).isEqualTo("chat");
        assertThat(p.token()).isEqualTo(TOKEN_N8N);
    }

    @Test
    @Order(5)
    void losErroresDelServicioDeIaLleganConMensajeLegible() throws Exception {
        N8N.forzarCodigo(404);
        mvc.perform(post("/api/chat/preguntar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pregunta\":\"¿Qué aporta la avena?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje", containsString("no está publicado")));

        mvc.perform(post("/api/chat/preguntar").contentType(MediaType.APPLICATION_JSON).content("{\"pregunta\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    void cargarDocumentosLosIndexaEnN8nYBorrarLosElimina() throws Exception {
        MockMultipartFile txt = new MockMultipartFile("archivo", "zinc.txt", "text/plain",
                "Guía sobre el zinc. El zinc participa en la cicatrización y en el sistema inmunitario.".getBytes(StandardCharsets.UTF_8));

        String respuesta = mvc.perform(multipart("/api/documentos").file(txt)
                        .param("titulo", "Guía de prueba sobre el zinc").header(ADMIN, "token-de-prueba"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoProcesamiento", is("PROCESADO")))
                .andExpect(jsonPath("$.totalFragmentos", is(1)))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(respuesta).path("id").asLong();

        FakeN8nServer.Peticion ingesta = N8N.peticiones().get(0);
        assertThat(ingesta.ruta()).isEqualTo("ingesta");
        assertThat(ingesta.cuerpo().path("documentoId").asLong()).isEqualTo(id);
        assertThat(ingesta.cuerpo().path("contenido").asText()).contains("El zinc participa");

        mvc.perform(multipart("/api/documentos").file(txt).param("titulo", "Otro").header(ADMIN, "token-de-prueba"))
                .andExpect(status().isConflict());

        byte[] pdf = PdfDePrueba.crear("GUIA DEL CALCIO. El calcio participa en la mineralizacion osea.");
        mvc.perform(multipart("/api/documentos")
                        .file(new MockMultipartFile("archivo", "calcio.pdf", "application/pdf", pdf))
                        .param("titulo", "Guía de prueba sobre el calcio").header(ADMIN, "token-de-prueba"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contenido", containsString("mineralizacion osea")));

        N8N.limpiar();
        mvc.perform(delete("/api/documentos/{id}", id).header(ADMIN, "token-de-prueba")).andExpect(status().isOk());
        assertThat(N8N.peticiones()).singleElement().satisfies(p -> {
            assertThat(p.ruta()).isEqualTo("eliminar");
            assertThat(p.cuerpo().path("documentoId").asLong()).isEqualTo(id);
        });
        mvc.perform(get("/api/documentos/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    void siN8nFallaElDocumentoQuedaEnErrorYPuedeReprocesarse() throws Exception {
        N8N.forzarCodigo(502);
        String respuesta = mvc.perform(post("/api/documentos").contentType(MediaType.APPLICATION_JSON)
                        .header(ADMIN, "token-de-prueba")
                        .content(json.writeValueAsString(Map.of("titulo", "Guía de hidratación de prueba",
                                "categoria", "Hábitos", "contenido", "Beber agua regularmente ayuda a mantener la hidratación."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoProcesamiento", is("ERROR")))
                .andExpect(jsonPath("$.mensajeError", is("Error simulado del flujo")))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(respuesta).path("id").asLong();

        N8N.forzarCodigo(0);
        mvc.perform(post("/api/documentos/{id}/reprocesar", id).header(ADMIN, "token-de-prueba"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoProcesamiento", is("PROCESADO")));
    }

    @Test
    @Order(8)
    void lasOperacionesDeAdministracionExigenToken() throws Exception {
        mvc.perform(post("/api/alimentos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/documentos/1").header(ADMIN, "incorrecto"))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode preguntar(String pregunta) throws Exception {
        String cuerpo = mvc.perform(post("/api/chat/preguntar").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("pregunta", pregunta))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(cuerpo);
    }

    private static FakeN8nServer iniciarN8n() {
        try {
            return new FakeN8nServer(TOKEN_N8N);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String uploads() {
        try {
            return Files.createTempDirectory("nutriguide-uploads").toString();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
