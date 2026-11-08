package com.nutriguide.modules.documento;

import com.nutriguide.common.ApiResponse;
import com.nutriguide.modules.documento.DocumentoService.ArchivoDocumento;
import com.nutriguide.rag.VectorRepository.FragmentoAlmacenado;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Biblioteca de documentos educativos e ingesta del RAG.
 * Las consultas (GET) son públicas; la carga, el reprocesamiento y el borrado requieren
 * la cabecera X-Admin-Token (ver AdminTokenInterceptor).
 */
@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @GetMapping
    public List<DocumentoResponse> listar(@RequestParam(required = false) String categoria,
                                          @RequestParam(required = false) EstadoProcesamiento estado) {
        return documentoService.listar(categoria, estado);
    }

    @GetMapping("/{id}")
    public DocumentoResponse obtener(@PathVariable Long id) {
        return documentoService.obtener(id);
    }

    /** Fragmentos almacenados en pgvector (contenido y dimensión del vector), para verificación. */
    @GetMapping("/{id}/fragmentos")
    public List<FragmentoAlmacenado> fragmentos(@PathVariable Long id) {
        return documentoService.listarFragmentos(id);
    }

    /** Archivo original (.pdf o .txt). */
    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> archivo(@PathVariable Long id) {
        ArchivoDocumento archivo = documentoService.archivo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(archivo.recurso());
    }

    /** Carga de un archivo .pdf o .txt (multipart/form-data, campo "archivo"). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> cargarArchivo(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String fuenteOrigen) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentoService.cargarArchivo(archivo, titulo, descripcion, categoria, fuenteOrigen));
    }

    /** Alternativa JSON: registrar un documento a partir de su texto. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentoResponse> cargarTexto(@Valid @RequestBody DocumentoTextoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentoService.cargarTexto(request));
    }

    @PostMapping("/{id}/reprocesar")
    public DocumentoResponse reprocesar(@PathVariable Long id) {
        return documentoService.reprocesar(id);
    }

    @PostMapping("/reprocesar-pendientes")
    public List<DocumentoResponse> reprocesarPendientes() {
        return documentoService.reprocesarPendientes();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        documentoService.eliminar(id);
        return ApiResponse.ok("Documento y fragmentos vectoriales eliminados.", null);
    }
}
