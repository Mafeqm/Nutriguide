package com.nutriguide.modules.documento;

import java.time.LocalDateTime;

/**
 * DTO de salida para representar un documento educativo y el estado de su vectorización.
 * Incluye el campo 'contenido' requerido por normalize.js y biblioteca.js del frontend.
 */
public class DocumentoResponse {

    private Long id;
    private String titulo;
    private String descripcion;
    private String categoria;
    private String contenido;
    private String nombreArchivo;
    private String rutaArchivo;
    private String fuenteOrigen;
    private LocalDateTime fechaCreacion;
    private EstadoProcesamiento estadoProcesamiento;
    private String mensajeError;
    private int totalFragmentos;

    public DocumentoResponse() {
    }

    public static DocumentoResponse fromEntity(Documento doc, int totalFragmentos, String contenido) {
        DocumentoResponse response = new DocumentoResponse();
        response.setId(doc.getId());
        response.setTitulo(doc.getTitulo());
        response.setDescripcion(doc.getDescripcion());
        response.setCategoria(doc.getCategoria());
        response.setContenido(contenido);
        response.setNombreArchivo(doc.getNombreArchivo());
        response.setRutaArchivo(doc.getRutaArchivo());
        response.setFuenteOrigen(doc.getFuenteOrigen());
        response.setFechaCreacion(doc.getFechaCreacion());
        response.setEstadoProcesamiento(doc.getEstadoProcesamiento());
        response.setMensajeError(doc.getMensajeError());
        response.setTotalFragmentos(totalFragmentos);
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public String getFuenteOrigen() {
        return fuenteOrigen;
    }

    public void setFuenteOrigen(String fuenteOrigen) {
        this.fuenteOrigen = fuenteOrigen;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public EstadoProcesamiento getEstadoProcesamiento() {
        return estadoProcesamiento;
    }

    public void setEstadoProcesamiento(EstadoProcesamiento estadoProcesamiento) {
        this.estadoProcesamiento = estadoProcesamiento;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public void setMensajeError(String mensajeError) {
        this.mensajeError = mensajeError;
    }

    public int getTotalFragmentos() {
        return totalFragmentos;
    }

    public void setTotalFragmentos(int totalFragmentos) {
        this.totalFragmentos = totalFragmentos;
    }
}
