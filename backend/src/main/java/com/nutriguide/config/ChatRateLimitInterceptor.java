package com.nutriguide.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriguide.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite sencillo de preguntas al chat por dirección IP (ventana fija de un minuto).
 * Evita que un solo cliente agote la cuota del proveedor de IA.
 */
@Component
public class ChatRateLimitInterceptor implements HandlerInterceptor {

    private static final long VENTANA_MS = 60_000L;

    private final int maxPorMinuto;
    private final ObjectMapper objectMapper;
    private final Map<String, Ventana> ventanas = new ConcurrentHashMap<>();

    public ChatRateLimitInterceptor(
            @Value("${nutriguide.chat.max-requests-per-minute:20}") int maxPorMinuto,
            ObjectMapper objectMapper) {
        this.maxPorMinuto = maxPorMinuto;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (maxPorMinuto <= 0 || !"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        long ahora = System.currentTimeMillis();
        if (ventanas.size() > 10_000) {
            ventanas.values().removeIf(v -> ahora - v.inicio >= VENTANA_MS);
        }
        Ventana ventana = ventanas.compute(request.getRemoteAddr(), (ip, actual) ->
                (actual == null || ahora - actual.inicio >= VENTANA_MS) ? new Ventana(ahora) : actual);
        int usadas;
        synchronized (ventana) {
            usadas = ++ventana.contador;
        }
        if (usadas <= maxPorMinuto) {
            return true;
        }
        long esperaSeg = Math.max(1, (VENTANA_MS - (ahora - ventana.inicio)) / 1000);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(esperaSeg));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(
                "Has realizado demasiadas preguntas en poco tiempo. Intenta de nuevo en " + esperaSeg + " segundos."));
        return false;
    }

    private static final class Ventana {
        private final long inicio;
        private int contador;

        private Ventana(long inicio) {
            this.inicio = inicio;
        }
    }
}
