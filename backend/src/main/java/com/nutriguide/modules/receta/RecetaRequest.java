package com.nutriguide.modules.receta;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;

/**
 * DTO de entrada con validaciones para crear o actualizar una receta educativa.
 */
public class RecetaRequest {

    @NotBlank(message = "El nombre de la receta es obligatorio")
    @Size(max = 200, message = "El nombre no puede superar los 200 caracteres")
    private String nombre;

    @NotBlank(message = "La descripción de la receta es obligatoria")
    private String descripcion;

    @JsonAlias({"imagen_url"})
    @Size(max = 500, message = "La URL de imagen no puede superar los 500 caracteres")
    private String imagenUrl;

    @NotBlank(message = "Los ingredientes son obligatorios")
    private String ingredientes;

    @NotBlank(message = "Las instrucciones de preparación son obligatorias")
    private String preparacion;

    @JsonAlias({"tiempo_preparacion"})
    @NotNull(message = "El tiempo de preparación en minutos es obligatorio")
    @Min(value = 1, message = "El tiempo de preparación debe ser de al menos 1 minuto")
    private Integer tiempoPreparacion;

    @JsonAlias({"informacion_nutricional"})
    @NotBlank(message = "La información nutricional por porción es obligatoria")
    private String informacionNutricional;

    @NotBlank(message = "La categoría de la receta es obligatoria")
    @Size(max = 100, message = "La categoría no puede superar los 100 caracteres")
    private String categoria;

    public RecetaRequest() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public String getIngredientes() {
        return ingredientes;
    }

    public void setIngredientes(String ingredientes) {
        this.ingredientes = ingredientes;
    }

    public String getPreparacion() {
        return preparacion;
    }

    public void setPreparacion(String preparacion) {
        this.preparacion = preparacion;
    }

    public Integer getTiempoPreparacion() {
        return tiempoPreparacion;
    }

    public void setTiempoPreparacion(Integer tiempoPreparacion) {
        this.tiempoPreparacion = tiempoPreparacion;
    }

    public String getInformacionNutricional() {
        return informacionNutricional;
    }

    public void setInformacionNutricional(String informacionNutricional) {
        this.informacionNutricional = informacionNutricional;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}

