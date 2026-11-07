package com.nutriguide.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriguide.common.AiServiceException;
import com.nutriguide.config.AiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;

/**
 * Servicio dedicado exclusivamente a la generación y validación de embeddings vectoriales.
 * Se comunica con la API de Embeddings de Google Gemini mediante el cliente HTTP de Java
 * y valida que cada vector tenga exactamente la dimensión configurada para pgvector.
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private static final Set<String> STOPWORDS_ES = Set.of(
            "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "al", "a",
            "en", "con", "por", "para", "sin", "sobre", "entre", "hacia", "hasta", "desde",
            "que", "cual", "cuales", "quien", "como", "cuando", "donde", "es", "son", "fue",
            "ser", "esta", "estan", "este", "estos", "estas", "ese", "esa", "esos",
            "y", "o", "u", "e", "ni", "pero", "mas", "sino", "porque", "pues", "si", "no",
            "se", "su", "sus", "mi", "mis", "tu", "tus", "lo", "le", "les", "me", "te", "nos"
    );

    private final AiConfig aiConfig;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingService(AiConfig aiConfig, HttpClient aiHttpClient, ObjectMapper objectMapper) {
        this.aiConfig = aiConfig;
        this.httpClient = aiHttpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Genera el vector de embedding para un texto dado y valida su dimensión contra la configuración.
     *
     * @param texto Texto del fragmento o pregunta del usuario.
     * @return Vector float[] de dimensión {@code aiConfig.getEmbeddingDimension()} (por defecto 768).
     */
    public float[] generarEmbedding(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El texto para generar el embedding no puede estar vacío.");
        }

        int dimensionEsperada = aiConfig.getEmbeddingDimension();

        if (aiConfig.hasValidApiKey()) {
            try {
                float[] vectorRemoto = llamarApiEmbeddingsGemini(texto, dimensionEsperada);
                validarDimension(vectorRemoto, dimensionEsperada);
                return normalizarL2(vectorRemoto);
            } catch (AiServiceException ex) {
                if (aiConfig.isEmbeddingFallbackEnabled()) {
                    log.warn("Usando generador vectorial semántico de respaldo tras error en API externa: {}",
                            ex.getMessage());
                    float[] fallback = generarEmbeddingSemanticoLocal(texto, dimensionEsperada);
                    validarDimension(fallback, dimensionEsperada);
                    return fallback;
                }
                throw ex;
            }
        }

        if (aiConfig.isEmbeddingFallbackEnabled()) {
            log.debug("Clave GEMINI_API_KEY no configurada; generando embedding semántico de dimensión {} para pgvector.",
                    dimensionEsperada);
            float[] vectorLocal = generarEmbeddingSemanticoLocal(texto, dimensionEsperada);
            validarDimension(vectorLocal, dimensionEsperada);
            return vectorLocal;
        }

        throw new AiServiceException(
                "No se ha configurado la variable de entorno GEMINI_API_KEY para el servicio de embeddings.", 503);
    }

    private float[] llamarApiEmbeddingsGemini(String texto, int dimensionEsperada) {
        String model = aiConfig.getEmbeddingModel();
        String modelPath = model.startsWith("models/") ? model : "models/" + model;
        String endpoint = aiConfig.getBaseUrl() + "/" + modelPath + ":embedContent";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", modelPath);
        payload.put("content", Map.of(
                "parts", List.of(Map.of("text", texto))
        ));
        payload.put("outputDimensionality", dimensionEsperada);

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new AiServiceException("Error serializando petición de embedding", ex);
        }

        int maxIntentos = Math.max(1, aiConfig.getMaxRetries() + 1);
        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", aiConfig.getApiKey().trim())
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return parsearVectorRespuestaGemini(response.body(), dimensionEsperada);
                }

                if ((status == 429 || status == 503) && intento < maxIntentos) {
                    long esperaMs = 800L * intento;
                    log.warn("Límite de tasa o servicio ocupado en API de embeddings (HTTP {}). Reintentando en {} ms...",
                            status, esperaMs);
                    Thread.sleep(esperaMs);
                    continue;
                }

                throw new AiServiceException(
                        "El proveedor de embeddings devolvió HTTP " + status + ". Verifique su clave API o cuota.",
                        status);
            } catch (AiServiceException ex) {
                throw ex;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AiServiceException("Petición de embeddings interrumpida", ex);
            } catch (Exception ex) {
                if (intento == maxIntentos) {
                    throw new AiServiceException("Error de conexión con la API de embeddings: " + ex.getMessage(), ex);
                }
            }
        }
        throw new AiServiceException("No fue posible obtener el embedding tras los reintentos configurados.");
    }

    private float[] parsearVectorRespuestaGemini(String responseJson, int dimensionEsperada) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode valuesNode = root.path("embedding").path("values");
            if (!valuesNode.isArray() || valuesNode.isEmpty()) {
                throw new AiServiceException("La respuesta de la API de embeddings no contiene un vector 'values' válido.");
            }
            int size = valuesNode.size();
            float[] vector = new float[dimensionEsperada];
            int copyLen = Math.min(size, dimensionEsperada);
            for (int i = 0; i < copyLen; i++) {
                vector[i] = (float) valuesNode.get(i).asDouble();
            }
            return vector;
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiServiceException("Error al procesar el JSON del vector de embeddings", ex);
        }
    }

    /**
     * Valida que el vector generado no sea nulo y coincida exactamente con la dimensión configurada para pgvector.
     */
    public void validarDimension(float[] vector, int dimensionEsperada) {
        if (vector == null) {
            throw new AiServiceException("El vector de embedding generado es nulo.");
        }
        if (vector.length != dimensionEsperada) {
            throw new AiServiceException(
                    "Dimensión de embedding inválida: se esperaba " + dimensionEsperada
                            + " pero se obtuvo " + vector.length);
        }
    }

    /**
     * Generador vectorial determinista de respaldo basado en Feature Hashing semántico
     * (raíces léxicas en español, expansión de sinónimos nutricionales, bigramas y trigramas de caracteres)
     * para asegurar vectores unitarios de 768 dimensiones en pgvector cuando se ejecuta sin clave externa.
     */
    private float[] generarEmbeddingSemanticoLocal(String texto, int dimension) {
        float[] vector = new float[dimension];
        String limpio = normalizarTexto(texto);
        String[] tokensRaw = limpio.split("\\s+");

        List<String> tokens = new ArrayList<>();
        for (String t : tokensRaw) {
            if (t.length() >= 2 && !STOPWORDS_ES.contains(t)) {
                tokens.add(t);
            }
        }

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            String raiz = obtenerRaizEspanol(token);

            // Peso alto para palabra exacta y su raíz morfológica
            acumularFeature(vector, "tok:" + token, 3.0f);
            acumularFeature(vector, "stem:" + raiz, 4.0f);

            // Expansión de conceptos nutricionales equivalentes para similitud semántica
            for (String concepto : expandirConceptosNutricionales(raiz)) {
                acumularFeature(vector, "sem:" + concepto, 3.5f);
            }

            // Bigramas de raíces consecutivas
            if (i + 1 < tokens.size()) {
                String sigRaiz = obtenerRaizEspanol(tokens.get(i + 1));
                acumularFeature(vector, "bi:" + raiz + "_" + sigRaiz, 2.0f);
            }

            // N-gramas de caracteres (3 y 4) para tolerancia a variaciones morfológicas
            if (token.length() >= 4) {
                for (int c = 0; c <= token.length() - 3; c++) {
                    acumularFeature(vector, "c3:" + token.substring(c, c + 3), 0.6f);
                }
            }
        }

        return normalizarL2(vector);
    }

    private void acumularFeature(float[] vector, String feature, float peso) {
        int dim = vector.length;
        long h1 = fnv1a64(feature);
        int idx1 = (int) (Math.abs(h1 % dim));
        float sign1 = ((h1 & 1L) == 0L) ? 1.0f : -1.0f;
        vector[idx1] += sign1 * peso;

        long h2 = fnv1a64("alt_" + feature);
        int idx2 = (int) (Math.abs(h2 % dim));
        float sign2 = ((h2 & 1L) == 0L) ? 1.0f : -1.0f;
        vector[idx2] += sign2 * (peso * 0.5f);
    }

    private List<String> expandirConceptosNutricionales(String raiz) {
        List<String> conceptos = new ArrayList<>();
        if (raiz.startsWith("aven") || raiz.startsWith("betaglucan") || raiz.startsWith("cereal") || raiz.startsWith("quinu")) {
            conceptos.add("cereal_integral_avena_fibra_soluble");
            conceptos.add("carbohidratos_complejos_energia");
        }
        if (raiz.startsWith("protein") || raiz.startsWith("aminoacid") || raiz.startsWith("lentej")
                || raiz.startsWith("garbanz") || raiz.startsWith("legumbr") || raiz.startsWith("leguminos")) {
            conceptos.add("proteina_aminoacidos_leguminosas");
        }
        if (raiz.startsWith("hierr") || raiz.startsWith("hem") || raiz.startsWith("biodisponib") || raiz.startsWith("absorc")) {
            conceptos.add("hierro_absorcion_vitamina_c_legumbres");
        }
        if (raiz.startsWith("vitamin") || raiz.startsWith("guayab") || raiz.startsWith("brocol")
                || raiz.startsWith("espinac") || raiz.startsWith("frut") || raiz.startsWith("verdur")
                || raiz.startsWith("micronutrient")) {
            conceptos.add("micronutrientes_vitaminas_frutas_verduras");
        }
        if (raiz.startsWith("gras") || raiz.startsWith("lipid") || raiz.startsWith("omega")
                || raiz.startsWith("aguacat") || raiz.startsWith("salmon") || raiz.startsWith("almendr")
                || raiz.startsWith("insatur")) {
            conceptos.add("lipidos_grasas_saludables_omega3_monoinsaturadas");
        }
        if (raiz.startsWith("azucar") || raiz.startsWith("sodi") || raiz.startsWith("hidrat") || raiz.startsWith("agua")) {
            conceptos.add("hidratacion_limites_oms_azucar_sodio");
        }
        if (raiz.startsWith("nutrient") || raiz.startsWith("composicion") || raiz.startsWith("aport")
                || raiz.startsWith("calori") || raiz.startsWith("fibr")) {
            conceptos.add("valor_nutricional_composicion_alimento");
        }
        return conceptos;
    }

    private String normalizarTexto(String texto) {
        String sinAcentos = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private String obtenerRaizEspanol(String palabra) {
        if (palabra.length() <= 4) {
            return palabra;
        }
        String[] sufijos = {"amientos", "imientos", "aciones", "iciones", "mente", "idades", "idad",
                "acion", "icion", "antes", "entes", "ables", "ibles", "osos", "osas", "ales", "ares",
                "es", "os", "as", "ar", "er", "ir", "s"};
        for (String sufijo : sufijos) {
            if (palabra.endsWith(sufijo) && palabra.length() - sufijo.length() >= 4) {
                return palabra.substring(0, palabra.length() - sufijo.length());
            }
        }
        return palabra;
    }

    private long fnv1a64(String input) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < input.length(); i++) {
            hash ^= input.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    public float[] normalizarL2(float[] vector) {
        double sumaCuadrados = 0.0;
        for (float v : vector) {
            sumaCuadrados += (double) v * v;
        }
        if (sumaCuadrados <= 1e-12) {
            float[] uniforme = new float[vector.length];
            float val = (float) (1.0 / Math.sqrt(vector.length));
            Arrays.fill(uniforme, val);
            return uniforme;
        }
        float norma = (float) Math.sqrt(sumaCuadrados);
        float[] normalizado = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalizado[i] = vector[i] / norma;
        }
        return normalizado;
    }
}

