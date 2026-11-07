package com.nutriguide.common;

/**
 * Excepción lanzada ante errores de comunicación, límites de cuota o configuración con el proveedor de IA.
 */
public class AiServiceException extends RuntimeException {

    private final int statusCode;

    public AiServiceException(String message) {
        super(message);
        this.statusCode = 502;
    }

    public AiServiceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 502;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

