package com.nutriguide.modules.alimento;

import java.math.BigDecimal;

/**
 * DTO de salida para representar un alimento y su información nutricional estandarizada.
 * Incluye los alias esperados por normalize.js del frontend (tamanoPorcion, unidadPorcion, fuente).
 */
public class AlimentoResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private String categoria;
    private String imagenUrl;
    private BigDecimal calorias;
    private BigDecimal proteinas;
    private BigDecimal carbohidratos;
    private BigDecimal grasas;
    private BigDecimal fibra;
    private BigDecimal porcionReferencia;
    private BigDecimal tamanoPorcion;
    private String unidad;
    private String unidadPorcion;
    private String porcionFormateada;
    private String fuenteInformacion;
    private String fuente;

    public AlimentoResponse() {
    }

    public static AlimentoResponse fromEntity(Alimento alimento) {
        AlimentoResponse response = new AlimentoResponse();
        response.setId(alimento.getId());
        response.setNombre(alimento.getNombre());
        response.setDescripcion(alimento.getDescripcion());
        response.setCategoria(alimento.getCategoria());
        response.setImagenUrl(alimento.getImagenUrl());
        response.setCalorias(alimento.getCalorias());
        response.setProteinas(alimento.getProteinas());
        response.setCarbohidratos(alimento.getCarbohidratos());
        response.setGrasas(alimento.getGrasas());
        response.setFibra(alimento.getFibra());
        response.setPorcionReferencia(alimento.getPorcionReferencia());
        response.setTamanoPorcion(alimento.getPorcionReferencia());
        response.setUnidad(alimento.getUnidad());
        response.setUnidadPorcion(alimento.getUnidad());
        response.setPorcionFormateada("Por cada " + alimento.getPorcionReferencia().stripTrailingZeros().toPlainString()
                + " " + alimento.getUnidad());
        response.setFuenteInformacion(alimento.getFuenteInformacion());
        response.setFuente(alimento.getFuenteInformacion());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getTamanoPorcion() {
        return tamanoPorcion;
    }

    public void setTamanoPorcion(BigDecimal tamanoPorcion) {
        this.tamanoPorcion = tamanoPorcion;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getUnidadPorcion() {
        return unidadPorcion;
    }

    public void setUnidadPorcion(String unidadPorcion) {
        this.unidadPorcion = unidadPorcion;
    }

    public String getPorcionFormateada() {
        return porcionFormateada;
    }

    public void setPorcionFormateada(String porcionFormateada) {
        this.porcionFormateada = porcionFormateada;
    }

    public String getFuenteInformacion() {
        return fuenteInformacion;
    }

    public void setFuenteInformacion(String fuenteInformacion) {
        this.fuenteInformacion = fuenteInformacion;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }
}
