package com.nutriguide.common;

/**
 * Excepción lanzada cuando ocurre un error en la validación, lectura o procesamiento de un documento RAG.
 */
public class DocumentProcessingException extends RuntimeException {
    public DocumentProcessingException(String message) {
        super(message);
    }

    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

