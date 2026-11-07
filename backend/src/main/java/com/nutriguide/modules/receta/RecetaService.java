package com.nutriguide.modules.receta;

import com.nutriguide.common.DuplicateDocumentException;
import com.nutriguide.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para la consulta y administración de recetas educativas.
 */
@Service
public class RecetaService {

    private final RecetaRepository recetaRepository;

    public RecetaService(RecetaRepository recetaRepository) {
        this.recetaRepository = recetaRepository;
    }

    @Transactional(readOnly = true)
    public List<RecetaResponse> listarTodas(String categoria, String nombre) {
        List<Receta> recetas;
        if (nombre != null && !nombre.isBlank()) {
            recetas = recetaRepository.findByNombreContainingIgnoreCaseOrderByNombreAsc(nombre.trim());
        } else if (categoria != null && !categoria.isBlank()) {
            recetas = recetaRepository.findByCategoriaIgnoreCaseOrderByNombreAsc(categoria.trim());
        } else {
            recetas = recetaRepository.findAllByOrderByNombreAsc();
        }
        return recetas.stream()
                .map(RecetaResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecetaResponse obtenerPorId(Long id) {
        Receta receta = recetaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la receta con ID: " + id));
        return RecetaResponse.fromEntity(receta);
    }

    @Transactional
    public RecetaResponse crear(RecetaRequest request) {
        recetaRepository.findByNombreIgnoreCase(request.getNombre().trim())
                .ifPresent(existing -> {
                    throw new DuplicateDocumentException(
                            "Ya existe una receta registrada con el nombre: " + request.getNombre());
                });

        Receta receta = new Receta();
        mapRequestToEntity(request, receta);
        Receta guardada = recetaRepository.save(receta);
        return RecetaResponse.fromEntity(guardada);
    }

    @Transactional
    public RecetaResponse actualizar(Long id, RecetaRequest request) {
        Receta receta = recetaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la receta con ID: " + id));

        recetaRepository.findByNombreIgnoreCase(request.getNombre().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateDocumentException(
                            "Ya existe otra receta registrada con el nombre: " + request.getNombre());
                });

        mapRequestToEntity(request, receta);
        Receta actualizada = recetaRepository.save(receta);
        return RecetaResponse.fromEntity(actualizada);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!recetaRepository.existsById(id)) {
            throw new ResourceNotFoundException("No se encontró la receta con ID: " + id);
        }
        recetaRepository.deleteById(id);
    }

    private void mapRequestToEntity(RecetaRequest request, Receta receta) {
        receta.setNombre(request.getNombre().trim());
        receta.setDescripcion(request.getDescripcion().trim());
        receta.setImagenUrl(request.getImagenUrl() != null ? request.getImagenUrl().trim() : null);
        receta.setIngredientes(request.getIngredientes().trim());
        receta.setPreparacion(request.getPreparacion().trim());
        receta.setTiempoPreparacion(request.getTiempoPreparacion());
        receta.setInformacionNutricional(request.getInformacionNutricional().trim());
        receta.setCategoria(request.getCategoria().trim());
    }
}

