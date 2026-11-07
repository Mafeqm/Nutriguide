package com.nutriguide.modules.receta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Receta con consultas parametrizadas.
 */
@Repository
public interface RecetaRepository extends JpaRepository<Receta, Long> {

    List<Receta> findByNombreContainingIgnoreCaseOrderByNombreAsc(String nombre);

    List<Receta> findByCategoriaIgnoreCaseOrderByNombreAsc(String categoria);

    Optional<Receta> findByNombreIgnoreCase(String nombre);

    List<Receta> findAllByOrderByNombreAsc();
}

