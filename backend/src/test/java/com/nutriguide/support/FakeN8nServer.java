package com.nutriguide.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

/**
 * DOBLE DE PRUEBA de los webhooks del flujo n8n "NutriGuide IA" (solo para tests).
 * Emula /webhook/nutriguide/chat, /ingesta y /eliminar con el mismo contrato JSON que el flujo real,
 * exige la cabecera X-NutriGuide-Token y registra las peticiones recibidas.
 */
public class FakeN8nServer implements AutoCloseable {

    private final HttpServer server;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String token;
    private final ConcurrentLinkedQueue<Peticion> peticiones = new ConcurrentLinkedQueue<>();
    private volatile int codigoForzado;
    private static final Function<String, Map<String, Object>> CHAT_POR_DEFECTO = pregunta -> Map.of(
            "respuesta", "Respuesta de prueba para: " + pregunta,
            "fuentes", List.of(Map.of("titulo", "Guía de prueba", "contenido", "Fragmento de prueba",
                    "documentoId", 1, "categoria", "Pruebas", "fuenteOrigen", "Prueba", "similitud", 0.81)),
            "informacionSuficiente", true,
            "advertenciaEducativa", "Información educativa.");
    private volatile Function<String, Map<String, Object>> chat = CHAT_POR_DEFECTO;

    public record Peticion(String ruta, String token, JsonNode cuerpo) {
    }

    public FakeN8nServer(String token) throws IOException {
        this.token = token;
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/webhook/nutriguide/", this::atender);
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public List<Peticion> peticiones() {
        return Collections.unmodifiableList(new ArrayList<>(peticiones));
    }

    public void limpiar() {
        peticiones.clear();
        codigoForzado = 0;
        chat = CHAT_POR_DEFECTO;
    }

    /** Todas las respuestas siguientes tendrán este código (0 = comportamiento normal). */
    public void forzarCodigo(int codigo) {
        codigoForzado = codigo;
    }

    public void respuestaChat(Function<String, Map<String, Object>> funcion) {
        chat = funcion;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void atender(HttpExchange ex) throws IOException {
        String ruta = ex.getRequestURI().getPath().replace("/webhook/nutriguide/", "");
        String recibido = ex.getRequestHeaders().getFirst("X-NutriGuide-Token");
        JsonNode cuerpo = mapper.readTree(ex.getRequestBody().readAllBytes());
        peticiones.add(new Peticion(ruta, recibido, cuerpo));

        if (!token.equals(recibido)) {
            responder(ex, 403, Map.of("message", "Authorization data is wrong!"));
            return;
        }
        if (codigoForzado != 0) {
            responder(ex, codigoForzado, Map.of("exito", false, "mensaje", "Error simulado del flujo"));
            return;
        }
        switch (ruta) {
            case "chat" -> responder(ex, 200, chat.apply(cuerpo.path("pregunta").asText()));
            case "ingesta" -> {
                int fragmentos = Math.max(1, (int) Math.ceil(cuerpo.path("contenido").asText().length() / 900.0));
                responder(ex, 200, Map.of("documentoId", cuerpo.path("documentoId").asLong(),
                        "fragmentos", fragmentos, "coleccion", "nutriguide_documentos"));
            }
            case "eliminar" -> responder(ex, 200, Map.of("documentoId", cuerpo.path("documentoId").asLong(), "eliminado", true));
            default -> responder(ex, 404, Map.of("message", "webhook no registrado"));
        }
    }

    private void responder(HttpExchange ex, int codigo, Object cuerpo) throws IOException {
        byte[] bytes = mapper.writeValueAsString(cuerpo).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }
}
