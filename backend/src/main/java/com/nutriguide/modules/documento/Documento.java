package com.nutriguide.modules.documento;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Documento educativo de la biblioteca del RAG. Sus fragmentos y embeddings se guardan en la
 * tabla fragmentos_documentos (pgvector), relacionada por documento_id.
 */
@Entity
@Table(name = "documentos")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, length = 100)
    private String categoria;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "hash_sha256", unique = true, length = 64)
    private String hashSha256;

    @Column(name = "fuente_origen", nullable = false, length = 255)
    private String fuenteOrigen;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_procesamiento", nullable = false, length = 30)
    private EstadoProcesamiento estadoProcesamiento;

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    /** Texto limpio extraído del archivo: se muestra en la biblioteca y permite reindexar. */
    @Column(name = "contenido_texto", columnDefinition = "TEXT")
    private String contenidoTexto;

    /** Modelo de embeddings con el que se indexó (para reindexar si cambia). */
    @Column(name = "modelo_embedding", length = 100)
    private String modeloEmbedding;

    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (estadoProcesamiento == null) {
            estadoProcesamiento = EstadoProcesamiento.PENDIENTE;
        }
        if (fuenteOrigen == null || fuenteOrigen.isBlank()) {
            fuenteOrigen = "Fuente educativa institucional";
        }
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

    public String getHashSha256() {
        return hashSha256;
    }

    public void setHashSha256(String hashSha256) {
        this.hashSha256 = hashSha256;
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

    public String getContenidoTexto() {
        return contenidoTexto;
    }

    public void setContenidoTexto(String contenidoTexto) {
        this.contenidoTexto = contenidoTexto;
    }

    public String getModeloEmbedding() {
        return modeloEmbedding;
    }

    public void setModeloEmbedding(String modeloEmbedding) {
        this.modeloEmbedding = modeloEmbedding;
    }

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }
}
