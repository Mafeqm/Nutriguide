package com.nutriguide.modules.documento;

import com.nutriguide.common.DocumentProcessingException;
import com.nutriguide.common.DuplicateDocumentException;
import com.nutriguide.common.ResourceNotFoundException;
import com.nutriguide.rag.DocumentProcessor;
import com.nutriguide.rag.RagService;
import com.nutriguide.rag.VectorRepository;
import com.nutriguide.rag.VectorRepository.FragmentoRecuperado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para la gestión del ciclo de vida de documentos educativos en la biblioteca RAG:
 * validación, control de duplicados, almacenamiento seguro, extracción de texto,
 * generación de embeddings y control de estado de procesamiento (PENDIENTE, PROCESANDO, PROCESADO, ERROR).
 */
@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    private final DocumentoRepository documentoRepository;
    private final DocumentProcessor documentProcessor;
    private final RagService ragService;
    private final VectorRepository vectorRepository;
    private final Path uploadDirectory;

    public DocumentoService(
            DocumentoRepository documentoRepository,
            DocumentProcessor documentProcessor,
            RagService ragService,
            VectorRepository vectorRepository,
            @Value("${nutriguide.storage.upload-dir:./uploads/documentos}") String uploadDir) {
        this.documentoRepository = documentoRepository;
        this.documentProcessor = documentProcessor;
        this.ragService = ragService;
        this.vectorRepository = vectorRepository;
        this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        inicializarDirectorioAlmacenamiento();
    }

    private void inicializarDirectorioAlmacenamiento() {
        try {
            Files.createDirectories(this.uploadDirectory);
        } catch (IOException ex) {
            log.warn("No se pudo crear el directorio de almacenamiento {}: {}",
                    this.uploadDirectory, ex.getMessage());
        }
    }

    public List<DocumentoResponse> listarTodos(String categoria, EstadoProcesamiento estado) {
        List<Documento> documentos;
        if (estado != null) {
            documentos = documentoRepository.findByEstadoProcesamientoOrderByFechaCreacionAsc(estado);
        } else if (categoria != null && !categoria.isBlank()) {
            documentos = documentoRepository.findByCategoriaIgnoreCaseOrderByFechaCreacionDesc(categoria.trim());
        } else {
            documentos = documentoRepository.findAllByOrderByFechaCreacionDesc();
        }
        return documentos.stream()
                .map(this::construirRespuestaDocumento)
                .toList();
    }

    public DocumentoResponse obtenerPorId(Long id) {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el documento con ID: " + id));
        return construirRespuestaDocumento(doc);
    }

    private DocumentoResponse construirRespuestaDocumento(Documento doc) {
        List<FragmentoRecuperado> fragmentos = vectorRepository.listarPorDocumentoId(doc.getId());
        String contenidoReconstruido = fragmentos.stream()
                .map(FragmentoRecuperado::contenido)
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("");
        return DocumentoResponse.fromEntity(doc, fragmentos.size(), contenidoReconstruido);
    }

    public List<FragmentoRecuperado> listarFragmentosDeDocumento(Long id) {
        if (!documentoRepository.existsById(id)) {
            throw new ResourceNotFoundException("No se encontró el documento con ID: " + id);
        }
        return vectorRepository.listarPorDocumentoId(id);
    }

    /**
     * Recibe un archivo Multipart (.pdf o .txt), valida su formato y tamaño, verifica que no esté duplicado,
     * extrae y limpia su texto, lo divide en fragmentos, genera los embeddings y los guarda en pgvector.
     */
    public DocumentoResponse cargarDocumentoArchivo(
            MultipartFile archivo,
            String titulo,
            String descripcion,
            String categoria,
            String fuenteOrigen) {

        documentProcessor.validarArchivo(archivo);

        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (IOException ex) {
            throw new DocumentProcessingException("Error leyendo el archivo subido: " + ex.getMessage(), ex);
        }

        String hashSha256 = documentProcessor.calcularSha256(bytes);
        verificarDuplicados(hashSha256, titulo);

        String nombreOriginal = sanitizarNombreArchivo(archivo.getOriginalFilename());
        String tituloFinal = (titulo != null && !titulo.isBlank()) ? titulo.trim() : nombreOriginal;
        String categoriaFinal = (categoria != null && !categoria.isBlank()) ? categoria.trim() : "Nutrición General";
        String fuenteFinal = (fuenteOrigen != null && !fuenteOrigen.isBlank())
                ? fuenteOrigen.trim()
                : "Documento educativo verificado";

        Path rutaDestino = guardarArchivoEnDisco(nombreOriginal, hashSha256, bytes);

        Documento doc = new Documento();
        doc.setTitulo(tituloFinal);
        doc.setDescripcion(descripcion != null ? descripcion.trim() : "Documento educativo sobre nutrición");
        doc.setCategoria(categoriaFinal);
        doc.setNombreArchivo(nombreOriginal);
        doc.setRutaArchivo(rutaDestino.toString());
        doc.setHashSha256(hashSha256);
        doc.setFuenteOrigen(fuenteFinal);
        doc.setEstadoProcesamiento(EstadoProcesamiento.PROCESANDO);
        doc = documentoRepository.save(doc);

        String extension = documentProcessor.obtenerExtension(nombreOriginal);
        return ejecutarProcesamientoYRegistroEstado(doc, bytes, extension);
    }

    /**
     * Permite registrar e indexar un documento educativo enviado en formato JSON (contenido de texto directo).
     */
    public DocumentoResponse cargarDocumentoDesdeTexto(DocumentoTextoRequest request) {
        byte[] bytes = request.getContenido().trim().getBytes(StandardCharsets.UTF_8);
        String hashSha256 = documentProcessor.calcularSha256(bytes);
        verificarDuplicados(hashSha256, request.getTitulo());

        String nombreArchivoGenerado = sanitizarNombreArchivo(
                request.getTitulo().trim().toLowerCase().replaceAll("[^a-z0-9]+", "_") + ".txt");
        Path rutaDestino = guardarArchivoEnDisco(nombreArchivoGenerado, hashSha256, bytes);

        Documento doc = new Documento();
        doc.setTitulo(request.getTitulo().trim());
        doc.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : "");
        doc.setCategoria(request.getCategoria().trim());
        doc.setNombreArchivo(nombreArchivoGenerado);
        doc.setRutaArchivo(rutaDestino.toString());
        doc.setHashSha256(hashSha256);
        doc.setFuenteOrigen(request.getFuenteOrigen() != null && !request.getFuenteOrigen().isBlank()
                ? request.getFuenteOrigen().trim()
                : "Documento educativo institucional");
        doc.setEstadoProcesamiento(EstadoProcesamiento.PROCESANDO);
        doc = documentoRepository.save(doc);

        return ejecutarProcesamientoYRegistroEstado(doc, bytes, "txt");
    }

    /**
     * Reprocesa un documento que haya quedado en estado PENDIENTE o ERROR.
     */
    public DocumentoResponse reprocesarDocumento(Long id) {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el documento con ID: " + id));

        Path ruta = Paths.get(doc.getRutaArchivo());
        if (!Files.exists(ruta)) {
            doc.setEstadoProcesamiento(EstadoProcesamiento.ERROR);
            doc.setMensajeError("No se encontró el archivo físico en la ruta registrada: " + doc.getRutaArchivo());
            documentoRepository.save(doc);
            throw new DocumentProcessingException(doc.getMensajeError());
        }

        try {
            byte[] bytes = Files.readAllBytes(ruta);
            doc.setEstadoProcesamiento(EstadoProcesamiento.PROCESANDO);
            doc.setMensajeError(null);
            doc = documentoRepository.save(doc);
            String extension = documentProcessor.obtenerExtension(doc.getNombreArchivo());
            return ejecutarProcesamientoYRegistroEstado(doc, bytes, extension);
        } catch (IOException ex) {
            doc.setEstadoProcesamiento(EstadoProcesamiento.ERROR);
            doc.setMensajeError("Error leyendo archivo físico para reprocesamiento: " + ex.getMessage());
            documentoRepository.save(doc);
            throw new DocumentProcessingException(doc.getMensajeError(), ex);
        }
    }

    /**
     * Reprocesa todos los documentos que se encuentren en estado PENDIENTE o ERROR.
     */
    public List<DocumentoResponse> reprocesarPendientesYFallidos() {
        List<Documento> porReprocesar = new ArrayList<>();
        porReprocesar.addAll(
                documentoRepository.findByEstadoProcesamientoOrderByFechaCreacionAsc(EstadoProcesamiento.PENDIENTE));
        porReprocesar.addAll(
                documentoRepository.findByEstadoProcesamientoOrderByFechaCreacionAsc(EstadoProcesamiento.ERROR));

        List<DocumentoResponse> resultados = new ArrayList<>();
        for (Documento doc : porReprocesar) {
            try {
                resultados.add(reprocesarDocumento(doc.getId()));
            } catch (Exception ex) {
                log.warn("El documento ID={} continuó con error al reprocesar: {}", doc.getId(), ex.getMessage());
                resultados.add(obtenerPorId(doc.getId()));
            }
        }
        return resultados;
    }

    public void eliminarDocumento(Long id) {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el documento con ID: " + id));
        vectorRepository.eliminarPorDocumentoId(id);
        documentoRepository.delete(doc);
    }

    private DocumentoResponse ejecutarProcesamientoYRegistroEstado(
            Documento doc,
            byte[] bytes,
            String extension) {
        try {
            String textoLimpio = documentProcessor.extraerYLimpiarTexto(bytes, extension);
            int totalFragmentos = ragService.indexarContenidoDocumento(doc, textoLimpio);

            doc.setEstadoProcesamiento(EstadoProcesamiento.PROCESADO);
            doc.setMensajeError(null);
            Documento actualizado = documentoRepository.save(doc);
            return DocumentoResponse.fromEntity(actualizado, totalFragmentos, textoLimpio);
        } catch (Exception ex) {
            log.error("Fallo al procesar el documento '{}' (ID={}): {}",
                    doc.getTitulo(), doc.getId(), ex.getMessage());
            doc.setEstadoProcesamiento(EstadoProcesamiento.ERROR);
            doc.setMensajeError(ex.getMessage());
            documentoRepository.save(doc);
            throw new DocumentProcessingException(
                    "El documento fue registrado con estado ERROR porque no pudo completarse su vectorización: "
                            + ex.getMessage(), ex);
        }
    }

    private void verificarDuplicados(String hashSha256, String titulo) {
        documentoRepository.findByHashSha256(hashSha256)
                .ifPresent(existing -> {
                    throw new DuplicateDocumentException(
                            "Este archivo ya fue cargado anteriormente bajo el título: '"
                                    + existing.getTitulo() + "' (ID=" + existing.getId() + ").");
                });

        if (titulo != null && !titulo.isBlank()) {
            documentoRepository.findByTituloIgnoreCase(titulo.trim())
                    .ifPresent(existing -> {
                        throw new DuplicateDocumentException(
                                "Ya existe un documento registrado con el título: '" + titulo.trim() + "'.");
                    });
        }
    }

    private Path guardarArchivoEnDisco(String nombreSanitizado, String hashSha256, byte[] bytes) {
        try {
            Files.createDirectories(this.uploadDirectory);
            String prefijoHash = hashSha256.substring(0, 8);
            Path destino = this.uploadDirectory.resolve(prefijoHash + "_" + nombreSanitizado).normalize();
            if (!destino.startsWith(this.uploadDirectory)) {
                throw new DocumentProcessingException("Nombre de archivo inválido por seguridad.");
            }
            Files.write(destino, bytes);
            return destino;
        } catch (IOException ex) {
            throw new DocumentProcessingException("No fue posible almacenar el archivo en el servidor: " + ex.getMessage(), ex);
        }
    }

    private String sanitizarNombreArchivo(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            return "documento.txt";
        }
        String soloNombre = Paths.get(nombreOriginal).getFileName().toString();
        return soloNombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

