package com.nutriguide.modules.documento;

import com.nutriguide.common.ApiResponse;
import com.nutriguide.rag.VectorRepository.FragmentoRecuperado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controlador REST para la biblioteca de documentos educativos y la administración del flujo de ingesta RAG.
 */
@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @GetMapping
    public ResponseEntity<List<DocumentoResponse>> listarDocumentos(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) EstadoProcesamiento estado) {
        return ResponseEntity.ok(documentoService.listarTodos(categoria, estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoResponse> obtenerDocumentoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(documentoService.obtenerPorId(id));
    }

    @GetMapping("/{id}/fragmentos")
    public ResponseEntity<List<FragmentoRecuperado>> listarFragmentosDeDocumento(@PathVariable Long id) {
        return ResponseEntity.ok(documentoService.listarFragmentosDeDocumento(id));
    }

    /**
     * Carga un archivo educativo (.pdf o .txt) mediante multipart/form-data para alimentar el sistema RAG.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> cargarDocumentoMultipart(
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "file", required = false) MultipartFile fileAlternativo,
            @RequestParam(value = "titulo", required = false) String titulo,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "categoria", defaultValue = "Nutrición Educativa") String categoria,
            @RequestParam(value = "fuenteOrigen", defaultValue = "Fuente educativa institucional") String fuenteOrigen) {

        MultipartFile archivoSeleccionado = (archivo != null && !archivo.isEmpty()) ? archivo : fileAlternativo;
        DocumentoResponse creado = documentoService.cargarDocumentoArchivo(
                archivoSeleccionado, titulo, descripcion, categoria, fuenteOrigen);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * Alternativa JSON para cargar e indexar un documento educativo directamente desde texto en POST /api/documentos.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentoResponse> cargarDocumentoJson(
            @Valid @RequestBody DocumentoTextoRequest request) {
        DocumentoResponse creado = documentoService.cargarDocumentoDesdeTexto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PostMapping("/{id}/reprocesar")
    public ResponseEntity<DocumentoResponse> reprocesarDocumento(@PathVariable Long id) {
        return ResponseEntity.ok(documentoService.reprocesarDocumento(id));
    }

    @PostMapping("/reprocesar-pendientes")
    public ResponseEntity<List<DocumentoResponse>> reprocesarPendientes() {
        return ResponseEntity.ok(documentoService.reprocesarPendientesYFallidos());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarDocumento(@PathVariable Long id) {
        documentoService.eliminarDocumento(id);
        return ResponseEntity.ok(ApiResponse.ok("Documento y vectores asociados eliminados correctamente", null));
    }
}

