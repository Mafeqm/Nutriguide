package com.nutriguide.modules.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para realizar una pregunta al asistente inteligente basado en RAG.
 */
public class ChatRequest {

    @NotBlank(message = "La pregunta no puede estar vacía")
    @Size(min = 3, max = 1000, message = "La pregunta debe tener entre 3 y 1000 caracteres")
    private String pregunta;

    public ChatRequest() {
    }

    public ChatRequest(String pregunta) {
        this.pregunta = pregunta;
    }

    public String getPregunta() {
        return pregunta;
    }

    public void setPregunta(String pregunta) {
        this.pregunta = pregunta;
    }
}

