package com.nutriguide.modules.documento;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO alternativo para registrar e indexar un documento educativo directamente mediante texto JSON.
 */
public class DocumentoTextoRequest {

    @NotBlank(message = "El título del documento es obligatorio")
    @Size(max = 255, message = "El título no puede superar los 255 caracteres")
    private String titulo;

    private String descripcion;

    @NotBlank(message = "La categoría del documento es obligatoria")
    @Size(max = 100, message = "La categoría no puede superar los 100 caracteres")
    private String categoria;

    @JsonAlias({"fuente_origen"})
    private String fuenteOrigen;

    @NotBlank(message = "El contenido del documento educativo es obligatorio")
    @Size(min = 30, message = "El contenido del documento debe tener al menos 30 caracteres")
    private String contenido;

    public DocumentoTextoRequest() {
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getFuenteOrigen() {
        return fuenteOrigen;
    }

    public void setFuenteOrigen(String fuenteOrigen) {
        this.fuenteOrigen = fuenteOrigen;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }
}

