package com.nutriguide.modules.alimento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Alimento con consultas parametrizadas.
 */
@Repository
public interface AlimentoRepository extends JpaRepository<Alimento, Long> {

    List<Alimento> findByNombreContainingIgnoreCaseOrderByNombreAsc(String nombre);

    List<Alimento> findByCategoriaIgnoreCaseOrderByNombreAsc(String categoria);

    Optional<Alimento> findByNombreIgnoreCase(String nombre);

    List<Alimento> findAllByOrderByNombreAsc();
}

