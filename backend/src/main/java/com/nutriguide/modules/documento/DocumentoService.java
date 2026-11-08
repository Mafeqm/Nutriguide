package com.nutriguide.modules.documento;

import com.nutriguide.common.AiServiceException;
import com.nutriguide.common.DocumentProcessingException;
import com.nutriguide.common.DuplicateDocumentException;
import com.nutriguide.common.ResourceNotFoundException;
import com.nutriguide.ia.N8nClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Ciclo de vida de los documentos de la biblioteca:
 * validación → control de duplicados (SHA-256 y título) → almacenamiento del archivo y del texto en
 * PostgreSQL → indexación en Qdrant mediante el flujo n8n (fragmentos + embeddings) → estado.
 * <p>
 * Si la indexación no es posible (n8n detenido, sin clave de IA, cuota agotada) el documento queda
 * registrado como PENDIENTE o ERROR con el motivo y puede reprocesarse sin volver a subirlo.
 */
@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    private final DocumentoRepository documentoRepository;
    private final DocumentProcessor documentProcessor;
    private final N8nClient n8nClient;
    private final Path directorio;

    public DocumentoService(DocumentoRepository documentoRepository, DocumentProcessor documentProcessor,
                            N8nClient n8nClient,
                            @Value("${nutriguide.storage.upload-dir:./uploads/documentos}") String uploadDir) {
        this.documentoRepository = documentoRepository;
        this.documentProcessor = documentProcessor;
        this.n8nClient = n8nClient;
        this.directorio = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    // ------------------------------------------------------------------ consultas

    public List<DocumentoResponse> listar(String categoria, EstadoProcesamiento estado) {
        List<Documento> documentos;
        if (estado != null) {
            documentos = documentoRepository.findByEstadoProcesamientoOrderByFechaCreacionAsc(estado);
        } else if (categoria != null && !categoria.isBlank()) {
            documentos = documentoRepository.findByCategoriaIgnoreCaseOrderByFechaCreacionDesc(categoria.trim());
        } else {
            documentos = documentoRepository.findAllByOrderByFechaCreacionDesc();
        }
        return documentos.stream().map(d -> DocumentoResponse.fromEntity(d, urlDescarga(d))).toList();
    }

    public DocumentoResponse obtener(Long id) {
        Documento doc = buscar(id);
        return DocumentoResponse.fromEntity(doc, urlDescarga(doc));
    }

    /** Archivo original para descargar o abrir desde la biblioteca. */
    public ArchivoDocumento archivo(Long id) {
        Documento doc = buscar(id);
        Path ruta = Paths.get(doc.getRutaArchivo()).toAbsolutePath().normalize();
        if (!ruta.startsWith(directorio) || !Files.isRegularFile(ruta)) {
            throw new ResourceNotFoundException("El archivo original del documento " + id + " no está disponible.");
        }
        String tipo = "pdf".equals(documentProcessor.obtenerExtension(doc.getNombreArchivo()))
                ? "application/pdf" : "text/plain;charset=UTF-8";
        return new ArchivoDocumento(new FileSystemResource(ruta), doc.getNombreArchivo(), tipo);
    }

    // ------------------------------------------------------------------ carga

    public DocumentoResponse cargarArchivo(MultipartFile archivo, String titulo, String descripcion,
                                           String categoria, String fuenteOrigen) {
        documentProcessor.validarArchivo(archivo);
        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (IOException ex) {
            throw new DocumentProcessingException("No se pudo leer el archivo enviado.", ex);
        }
        String nombre = sanitizarNombre(archivo.getOriginalFilename());
        String extension = documentProcessor.obtenerExtension(nombre);
        String tituloFinal = textoONulo(titulo) != null ? titulo.trim() : nombre;
        return registrar(bytes, extension, nombre, tituloFinal, descripcion,
                textoONulo(categoria) != null ? categoria.trim() : "Nutrición general", fuenteOrigen);
    }

    public DocumentoResponse cargarTexto(DocumentoTextoRequest request) {
        byte[] bytes = request.getContenido().trim().getBytes(StandardCharsets.UTF_8);
        String nombre = sanitizarNombre(request.getTitulo().trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_") + ".txt");
        return registrar(bytes, "txt", nombre, request.getTitulo().trim(), request.getDescripcion(),
                request.getCategoria().trim(), request.getFuenteOrigen());
    }

    private DocumentoResponse registrar(byte[] bytes, String extension, String nombreArchivo, String titulo,
                                        String descripcion, String categoria, String fuente) {
        String hash = documentProcessor.calcularSha256(bytes);
        documentoRepository.findByHashSha256(hash).ifPresent(e -> {
            throw new DuplicateDocumentException("Este archivo ya fue cargado como '" + e.getTitulo()
                    + "' (ID=" + e.getId() + "). Si falló su indexación, use POST /api/documentos/" + e.getId()
                    + "/reprocesar.");
        });
        documentoRepository.findByTituloIgnoreCase(titulo).ifPresent(e -> {
            throw new DuplicateDocumentException("Ya existe un documento con el título '" + titulo + "'.");
        });

        // Si el texto no se puede extraer, no se registra nada
        String texto = documentProcessor.extraerYLimpiarTexto(bytes, extension);
        Path ruta = guardarEnDisco(hash, nombreArchivo, bytes);

        Documento doc = new Documento();
        doc.setTitulo(titulo);
        doc.setDescripcion(textoONulo(descripcion) != null ? descripcion.trim() : null);
        doc.setCategoria(categoria);
        doc.setNombreArchivo(nombreArchivo);
        doc.setRutaArchivo(ruta.toString());
        doc.setHashSha256(hash);
        doc.setFuenteOrigen(textoONulo(fuente) != null ? fuente.trim() : "Fuente educativa institucional");
        doc.setContenidoTexto(texto);
        doc.setEstadoProcesamiento(EstadoProcesamiento.PENDIENTE);
        doc = documentoRepository.save(doc);

        return indexar(doc, false);
    }

    // ------------------------------------------------------------------ indexación (n8n + Qdrant)

    public DocumentoResponse reprocesar(Long id) {
        return indexar(buscar(id), true);
    }

    /** Indexa los documentos PENDIENTES o con ERROR. Solo actúa si el servicio de IA está configurado. */
    public List<DocumentoResponse> reprocesarPendientes() {
        if (!n8nClient.estaConfigurado()) {
            return List.of();
        }
        return documentoRepository.findAll().stream()
                .filter(d -> d.getEstadoProcesamiento() != EstadoProcesamiento.PROCESADO)
                .map(d -> indexar(d, false))
                .toList();
    }

    /**
     * @param lanzarError si es true, un fallo se propaga como error HTTP; si es false, queda registrado
     *                    en el estado del documento y se devuelve el documento.
     */
    private DocumentoResponse indexar(Documento doc, boolean lanzarError) {
        if (!n8nClient.estaConfigurado()) {
            doc.setEstadoProcesamiento(EstadoProcesamiento.PENDIENTE);
            doc.setMensajeError("Pendiente de indexar: el servidor no tiene configurado el servicio de IA (N8N_WEBHOOK_TOKEN).");
            doc = documentoRepository.save(doc);
            if (lanzarError) {
                throw new AiServiceException(doc.getMensajeError(), 503);
            }
            return DocumentoResponse.fromEntity(doc, urlDescarga(doc));
        }

        doc.setEstadoProcesamiento(EstadoProcesamiento.PROCESANDO);
        doc.setMensajeError(null);
        doc = documentoRepository.save(doc);
        try {
            if (doc.getContenidoTexto() == null) {
                doc.setContenidoTexto(releerTexto(doc));
            }
            int fragmentos = n8nClient.indexar(doc);
            doc.setTotalFragmentos(fragmentos);
            doc.setEstadoProcesamiento(EstadoProcesamiento.PROCESADO);
            doc.setFechaProcesamiento(LocalDateTime.now());
            doc = documentoRepository.save(doc);
            log.info("Documento '{}' (ID={}) indexado en Qdrant con {} fragmentos.", doc.getTitulo(), doc.getId(), fragmentos);
            return DocumentoResponse.fromEntity(doc, urlDescarga(doc));
        } catch (RuntimeException ex) {
            log.error("No se pudo indexar el documento '{}' (ID={}): {}", doc.getTitulo(), doc.getId(), ex.getMessage());
            doc.setEstadoProcesamiento(EstadoProcesamiento.ERROR);
            doc.setMensajeError(ex.getMessage());
            doc = documentoRepository.save(doc);
            if (lanzarError) {
                throw ex;
            }
            return DocumentoResponse.fromEntity(doc, urlDescarga(doc));
        }
    }

    private String releerTexto(Documento doc) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(doc.getRutaArchivo()));
            return documentProcessor.extraerYLimpiarTexto(bytes, documentProcessor.obtenerExtension(doc.getNombreArchivo()));
        } catch (IOException ex) {
            throw new DocumentProcessingException("No se encontró el archivo original del documento para reindexarlo.", ex);
        }
    }

    // ------------------------------------------------------------------ borrado

    /**
     * Borra primero los fragmentos en Qdrant (para que el asistente no cite un documento eliminado)
     * y después el registro y el archivo. Si nunca se indexó, no hace falta llamar a n8n.
     */
    public void eliminar(Long id) {
        Documento doc = buscar(id);
        if (doc.getTotalFragmentos() > 0 || doc.getEstadoProcesamiento() == EstadoProcesamiento.PROCESADO) {
            n8nClient.eliminar(id);
        }
        documentoRepository.delete(doc);
        try {
            Path ruta = Paths.get(doc.getRutaArchivo()).toAbsolutePath().normalize();
            if (ruta.startsWith(directorio)) {
                Files.deleteIfExists(ruta);
            }
        } catch (IOException ex) {
            log.warn("No se pudo borrar el archivo del documento {}: {}", id, ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ utilidades

    private Documento buscar(Long id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el documento con ID: " + id));
    }

    private String urlDescarga(Documento doc) {
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/documentos/{id}/archivo").buildAndExpand(doc.getId()).toUriString();
        } catch (IllegalStateException sinPeticion) {
            return "/api/documentos/" + doc.getId() + "/archivo"; // fuera de una petición HTTP (arranque)
        }
    }

    private Path guardarEnDisco(String hash, String nombre, byte[] bytes) {
        try {
            Files.createDirectories(directorio);
            Path destino = directorio.resolve(hash.substring(0, 12) + "_" + nombre).normalize();
            if (!destino.startsWith(directorio)) {
                throw new DocumentProcessingException("Nombre de archivo no permitido.");
            }
            Files.write(destino, bytes);
            return destino;
        } catch (IOException ex) {
            throw new DocumentProcessingException("No fue posible guardar el archivo en el servidor.", ex);
        }
    }

    private static String sanitizarNombre(String original) {
        if (original == null || original.isBlank()) {
            return "documento.txt";
        }
        String nombre = Paths.get(original.replace('\\', '/')).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        return nombre.length() > 150 ? nombre.substring(nombre.length() - 150) : nombre;
    }

    private static String textoONulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    /** Archivo original listo para enviarse en la respuesta HTTP. */
    public record ArchivoDocumento(Resource recurso, String nombre, String tipoContenido) {
    }
}
