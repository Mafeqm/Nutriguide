package com.nutriguide.modules.alimento;

import com.nutriguide.common.BusquedaTexto;
import com.nutriguide.common.DuplicateDocumentException;
import com.nutriguide.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para la gestión y consulta de alimentos y su composición nutricional.
 */
@Service
public class AlimentoService {

    private final AlimentoRepository alimentoRepository;

    public AlimentoService(AlimentoRepository alimentoRepository) {
        this.alimentoRepository = alimentoRepository;
    }

    @Transactional(readOnly = true)
    public List<AlimentoResponse> listarTodos(String categoria) {
        List<Alimento> alimentos;
        if (categoria != null && !categoria.isBlank()) {
            alimentos = alimentoRepository.findByCategoriaIgnoreCaseOrderByNombreAsc(categoria.trim());
        } else {
            alimentos = alimentoRepository.findAllByOrderByNombreAsc();
        }
        return alimentos.stream()
                .map(AlimentoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlimentoResponse obtenerPorId(Long id) {
        Alimento alimento = alimentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el alimento con ID: " + id));
        return AlimentoResponse.fromEntity(alimento);
    }

    @Transactional(readOnly = true)
    public List<AlimentoResponse> buscarPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return listarTodos(null);
        }
        return alimentoRepository.buscarPorNombre(BusquedaTexto.patronLike(nombre))
                .stream()
                .map(AlimentoResponse::fromEntity)
                .toList();
    }

    @Transactional
    public AlimentoResponse crear(AlimentoRequest request) {
        alimentoRepository.findByNombreIgnoreCase(request.getNombre().trim())
                .ifPresent(existing -> {
                    throw new DuplicateDocumentException(
                            "Ya existe un alimento registrado con el nombre: " + request.getNombre());
                });

        Alimento alimento = new Alimento();
        mapRequestToEntity(request, alimento);
        Alimento guardado = alimentoRepository.save(alimento);
        return AlimentoResponse.fromEntity(guardado);
    }

    @Transactional
    public AlimentoResponse actualizar(Long id, AlimentoRequest request) {
        Alimento alimento = alimentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el alimento con ID: " + id));

        alimentoRepository.findByNombreIgnoreCase(request.getNombre().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateDocumentException(
                            "Ya existe otro alimento registrado con el nombre: " + request.getNombre());
                });

        mapRequestToEntity(request, alimento);
        Alimento actualizado = alimentoRepository.save(alimento);
        return AlimentoResponse.fromEntity(actualizado);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!alimentoRepository.existsById(id)) {
            throw new ResourceNotFoundException("No se encontró el alimento con ID: " + id);
        }
        alimentoRepository.deleteById(id);
    }

    private void mapRequestToEntity(AlimentoRequest request, Alimento alimento) {
        alimento.setNombre(request.getNombre().trim());
        alimento.setDescripcion(request.getDescripcion().trim());
        alimento.setCategoria(request.getCategoria().trim());
        alimento.setImagenUrl(request.getImagenUrl() != null ? request.getImagenUrl().trim() : null);
        alimento.setCalorias(request.getCalorias());
        alimento.setProteinas(request.getProteinas());
        alimento.setCarbohidratos(request.getCarbohidratos());
        alimento.setGrasas(request.getGrasas());
        alimento.setFibra(request.getFibra());
        alimento.setPorcionReferencia(request.getPorcionReferencia());
        alimento.setUnidad(request.getUnidad().trim());
        alimento.setFuenteInformacion(request.getFuenteInformacion().trim());
    }
}

