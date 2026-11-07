package com.nutriguide.modules.receta;

import com.nutriguide.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para consultar y administrar recetas educativas.
 */
@RestController
@RequestMapping("/api/recetas")
public class RecetaController {

    private final RecetaService recetaService;

    public RecetaController(RecetaService recetaService) {
        this.recetaService = recetaService;
    }

    @GetMapping
    public ResponseEntity<List<RecetaResponse>> listarRecetas(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(recetaService.listarTodas(categoria, nombre));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecetaResponse> obtenerRecetaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(recetaService.obtenerPorId(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<RecetaResponse>> buscarRecetas(
            @RequestParam(defaultValue = "") String nombre) {
        return ResponseEntity.ok(recetaService.listarTodas(null, nombre));
    }

    @PostMapping
    public ResponseEntity<RecetaResponse> crearReceta(@Valid @RequestBody RecetaRequest request) {
        RecetaResponse creada = recetaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecetaResponse> actualizarReceta(
            @PathVariable Long id,
            @Valid @RequestBody RecetaRequest request) {
        return ResponseEntity.ok(recetaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarReceta(@PathVariable Long id) {
        recetaService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Receta eliminada correctamente", null));
    }
}

