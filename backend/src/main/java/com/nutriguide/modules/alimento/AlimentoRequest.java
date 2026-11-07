package com.nutriguide.modules.alimento;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO de entrada con validaciones para crear o actualizar un alimento.
 */
public class AlimentoRequest {

    @NotBlank(message = "El nombre del alimento es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    @NotBlank(message = "La descripción del alimento es obligatoria")
    private String descripcion;

    @NotBlank(message = "La categoría del alimento es obligatoria")
    @Size(max = 100, message = "La categoría no puede superar los 100 caracteres")
    private String categoria;

    @JsonAlias({"imagen_url"})
    @Size(max = 500, message = "La URL de imagen no puede superar los 500 caracteres")
    private String imagenUrl;

    @NotNull(message = "Las calorías son obligatorias")
    @DecimalMin(value = "0.0", inclusive = true, message = "Las calorías no pueden ser negativas")
    private BigDecimal calorias;

    @NotNull(message = "Las proteínas son obligatorias")
    @DecimalMin(value = "0.0", inclusive = true, message = "Las proteínas no pueden ser negativas")
    private BigDecimal proteinas;

    @NotNull(message = "Los carbohidratos son obligatorios")
    @DecimalMin(value = "0.0", inclusive = true, message = "Los carbohidratos no pueden ser negativos")
    private BigDecimal carbohidratos;

    @NotNull(message = "Las grasas son obligatorias")
    @DecimalMin(value = "0.0", inclusive = true, message = "Las grasas no pueden ser negativas")
    private BigDecimal grasas;

    @NotNull(message = "La fibra es obligatoria")
    @DecimalMin(value = "0.0", inclusive = true, message = "La fibra no puede ser negativa")
    private BigDecimal fibra;

    @JsonAlias({"porcion_referencia"})
    @NotNull(message = "La porción de referencia es obligatoria (ej. 100)")
    @DecimalMin(value = "0.01", inclusive = true, message = "La porción de referencia debe ser mayor que 0")
    private BigDecimal porcionReferencia;

    @NotBlank(message = "La unidad de la porción de referencia es obligatoria (ej. g o ml)")
    @Size(max = 50, message = "La unidad no puede superar los 50 caracteres")
    private String unidad;

    @JsonAlias({"fuente_informacion"})
    @NotBlank(message = "La fuente de información nutricional es obligatoria")
    @Size(max = 255, message = "La fuente de información no puede superar los 255 caracteres")
    private String fuenteInformacion;

    public AlimentoRequest() {
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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public BigDecimal getCalorias() {
        return calorias;
    }

    public void setCalorias(BigDecimal calorias) {
        this.calorias = calorias;
    }

    public BigDecimal getProteinas() {
        return proteinas;
    }

    public void setProteinas(BigDecimal proteinas) {
        this.proteinas = proteinas;
    }

    public BigDecimal getCarbohidratos() {
        return carbohidratos;
    }

    public void setCarbohidratos(BigDecimal carbohidratos) {
        this.carbohidratos = carbohidratos;
    }

    public BigDecimal getGrasas() {
        return grasas;
    }

    public void setGrasas(BigDecimal grasas) {
        this.grasas = grasas;
    }

    public BigDecimal getFibra() {
        return fibra;
    }

    public void setFibra(BigDecimal fibra) {
        this.fibra = fibra;
    }

    public BigDecimal getPorcionReferencia() {
        return porcionReferencia;
    }

    public void setPorcionReferencia(BigDecimal porcionReferencia) {
        this.porcionReferencia = porcionReferencia;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getFuenteInformacion() {
        return fuenteInformacion;
    }

    public void setFuenteInformacion(String fuenteInformacion) {
        this.fuenteInformacion = fuenteInformacion;
    }
}

