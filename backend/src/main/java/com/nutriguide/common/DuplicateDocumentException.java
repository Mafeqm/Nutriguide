package com.nutriguide.common;

/**
 * Excepción lanzada cuando se intenta registrar un documento o recurso duplicado.
 */
public class DuplicateDocumentException extends RuntimeException {
    public DuplicateDocumentException(String message) {
        super(message);
    }
}

