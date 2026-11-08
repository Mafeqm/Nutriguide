package com.nutriguide.modules.documento;

import java.time.LocalDateTime;

/**
 * Documento de la biblioteca tal como lo consume el frontend (biblioteca.js / normalize.js):
 * titulo, descripcion, categoria, contenido y url de descarga del archivo original.
 * No expone la ruta interna del archivo en el servidor.
 */
public record DocumentoResponse(
        Long id,
        String titulo,
        String descripcion,
        String categoria,
        String contenido,
        String url,
        String nombreArchivo,
        String fuenteOrigen,
        LocalDateTime fechaCreacion,
        EstadoProcesamiento estadoProcesamiento,
        String mensajeError,
        int totalFragmentos,
        LocalDateTime fechaProcesamiento) {

    public static DocumentoResponse fromEntity(Documento doc, String urlDescarga) {
        return new DocumentoResponse(
                doc.getId(),
                doc.getTitulo(),
                doc.getDescripcion(),
                doc.getCategoria(),
                doc.getContenidoTexto(),
                urlDescarga,
                doc.getNombreArchivo(),
                doc.getFuenteOrigen(),
                doc.getFechaCreacion(),
                doc.getEstadoProcesamiento(),
                doc.getMensajeError(),
                doc.getTotalFragmentos(),
                doc.getFechaProcesamiento());
    }
}
