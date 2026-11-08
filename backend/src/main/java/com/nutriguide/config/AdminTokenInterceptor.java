package com.nutriguide.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriguide.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protege las operaciones de administración (crear, modificar y borrar alimentos, recetas y
 * documentos, y la carga de documentos al RAG) con un token enviado en la cabecera X-Admin-Token.
 * El token se configura con la variable de entorno ADMIN_TOKEN; si está vacía, esas operaciones
 * quedan deshabilitadas. Las consultas (GET) y el chat son públicos.
 */
@Component
public class AdminTokenInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Admin-Token";

    private final String adminToken;
    private final ObjectMapper objectMapper;

    public AdminTokenInterceptor(@Value("${nutriguide.admin.token:}") String adminToken, ObjectMapper objectMapper) {
        this.adminToken = adminToken == null ? "" : adminToken.trim();
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        if (HttpMethod.GET.matches(method) || HttpMethod.OPTIONS.matches(method) || HttpMethod.HEAD.matches(method)) {
            return true;
        }
        if (adminToken.isEmpty()) {
            return rechazar(response, HttpStatus.FORBIDDEN,
                    "Las operaciones de administración están deshabilitadas. Configure la variable de entorno ADMIN_TOKEN.");
        }
        String enviado = request.getHeader(HEADER);
        if (enviado == null || !MessageDigest.isEqual(
                enviado.trim().getBytes(StandardCharsets.UTF_8), adminToken.getBytes(StandardCharsets.UTF_8))) {
            return rechazar(response, HttpStatus.UNAUTHORIZED,
                    "Token de administración ausente o inválido (cabecera " + HEADER + ").");
        }
        return true;
    }

    private boolean rechazar(HttpServletResponse response, HttpStatus status, String mensaje) throws Exception {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(mensaje));
        return false;
    }
}
