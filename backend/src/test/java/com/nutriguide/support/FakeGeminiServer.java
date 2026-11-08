package com.nutriguide.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * DOBLE DE PRUEBA de la API de Gemini (solo para tests; nunca se usa en la aplicación).
 * Emula los endpoints embedContent, batchEmbedContents y generateContent:
 * <ul>
 *   <li>Embeddings: bolsa de palabras con hashing, determinista. Textos que comparten palabras
 *       quedan cerca en el espacio vectorial, lo que permite comprobar la búsqueda en pgvector.</li>
 *   <li>Generación: respuesta configurable a partir del prompt recibido.</li>
 * </ul>
 * Registra las peticiones para verificar qué se envió al proveedor (clave, taskType, contexto).
 */
public class FakeGeminiServer implements AutoCloseable {

    private final HttpServer server;
    private final ObjectMapper mapper = new ObjectMapper();
    private final int dimension;
    private final ConcurrentLinkedQueue<Peticion> peticiones = new ConcurrentLinkedQueue<>();
    private final AtomicInteger erroresPendientes = new AtomicInteger();
    private volatile int codigoError = 429;
    private volatile int dimensionRespuesta;
    private volatile Function<String, String> generador = prompt -> "Respuesta de prueba basada en el contexto.";

    public record Peticion(String ruta, String apiKey, JsonNode cuerpo) {
    }

    public FakeGeminiServer(int dimension) throws IOException {
        this.dimension = dimension;
        this.dimensionRespuesta = dimension;
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::atender);
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta";
    }

    public List<Peticion> peticiones() {
        return Collections.unmodifiableList(new ArrayList<>(peticiones));
    }

    public void limpiar() {
        peticiones.clear();
        erroresPendientes.set(0);
        dimensionRespuesta = dimension;
    }

    /** Las próximas {@code n} peticiones responderán con el código indicado. */
    public void fallarProximas(int n, int codigo) {
        codigoError = codigo;
        erroresPendientes.set(n);
    }

    public void responderConDimension(int dim) {
        dimensionRespuesta = dim;
    }

    public void generador(Function<String, String> funcion) {
        generador = funcion;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void atender(HttpExchange ex) throws IOException {
        String ruta = ex.getRequestURI().getPath();
        JsonNode cuerpo = mapper.readTree(ex.getRequestBody().readAllBytes());
        peticiones.add(new Peticion(ruta, ex.getRequestHeaders().getFirst("x-goog-api-key"), cuerpo));

        if (erroresPendientes.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            responder(ex, codigoError, Map.of("error", Map.of("message", "error simulado")));
            return;
        }
        if (ruta.endsWith(":embedContent")) {
            responder(ex, 200, Map.of("embedding", Map.of("values", vector(texto(cuerpo)))));
        } else if (ruta.endsWith(":batchEmbedContents")) {
            List<Map<String, Object>> embeddings = new ArrayList<>();
            for (JsonNode req : cuerpo.path("requests")) {
                embeddings.add(Map.of("values", vector(texto(req))));
            }
            responder(ex, 200, Map.of("embeddings", embeddings));
        } else if (ruta.endsWith(":generateContent")) {
            String prompt = cuerpo.path("contents").path(0).path("parts").path(0).path("text").asText();
            String texto = generador.apply(prompt);
            responder(ex, 200, Map.of("candidates", List.of(Map.of(
                    "content", Map.of("parts", List.of(Map.of("text", texto))),
                    "finishReason", "STOP"))));
        } else {
            responder(ex, 404, Map.of("error", Map.of("message", "ruta desconocida")));
        }
    }

    private static String texto(JsonNode req) {
        return req.path("content").path("parts").path(0).path("text").asText();
    }

    /** Bolsa de palabras (≥ 4 letras, sin tildes) proyectada con hashing. */
    private List<Double> vector(String texto) {
        double[] v = new double[dimensionRespuesta];
        String limpio = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9 ]", " ");
        for (String palabra : limpio.split("\\s+")) {
            if (palabra.length() >= 4) {
                v[Math.floorMod(palabra.hashCode(), dimensionRespuesta)] += 1.0;
            }
        }
        v[0] += 0.01; // nunca un vector nulo
        List<Double> lista = new ArrayList<>(v.length);
        for (double d : v) {
            lista.add(d);
        }
        return lista;
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
