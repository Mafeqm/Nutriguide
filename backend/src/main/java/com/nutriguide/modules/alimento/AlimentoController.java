package com.nutriguide.modules.alimento;

import com.nutriguide.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para consultar y administrar información nutricional de alimentos.
 */
@RestController
@RequestMapping("/api/alimentos")
public class AlimentoController {

    private final AlimentoService alimentoService;

    public AlimentoController(AlimentoService alimentoService) {
        this.alimentoService = alimentoService;
    }

    @GetMapping
    public ResponseEntity<List<AlimentoResponse>> listarAlimentos(
            @RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(alimentoService.listarTodos(categoria));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlimentoResponse> obtenerAlimentoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(alimentoService.obtenerPorId(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<AlimentoResponse>> buscarAlimentosPorNombre(
            @RequestParam(defaultValue = "") String nombre) {
        return ResponseEntity.ok(alimentoService.buscarPorNombre(nombre));
    }

    @PostMapping
    public ResponseEntity<AlimentoResponse> crearAlimento(
            @Valid @RequestBody AlimentoRequest request) {
        AlimentoResponse creado = alimentoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlimentoResponse> actualizarAlimento(
            @PathVariable Long id,
            @Valid @RequestBody AlimentoRequest request) {
        return ResponseEntity.ok(alimentoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarAlimento(@PathVariable Long id) {
        alimentoService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Alimento eliminado correctamente", null));
    }
}

