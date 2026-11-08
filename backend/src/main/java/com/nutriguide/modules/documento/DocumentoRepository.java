package com.nutriguide.modules.documento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Documento.
 */
@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    Optional<Documento> findByHashSha256(String hashSha256);

    Optional<Documento> findByTituloIgnoreCase(String titulo);

    List<Documento> findByEstadoProcesamientoOrderByFechaCreacionAsc(EstadoProcesamiento estado);

    List<Documento> findByCategoriaIgnoreCaseOrderByFechaCreacionDesc(String categoria);

    List<Documento> findAllByOrderByFechaCreacionDesc();
}

