package com.nutriguide.modules.alimento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA de alimentos (consultas parametrizadas).
 */
@Repository
public interface AlimentoRepository extends JpaRepository<Alimento, Long> {

    /** Búsqueda por nombre sin distinguir mayúsculas ni tildes (ver BusquedaTexto). */
    @Query(value = """
            SELECT * FROM alimentos
            WHERE translate(lower(nombre), 'áàäâéèëêíìïîóòöôúùüûñ', 'aaaaeeeeiiiioooouuuun') LIKE :patron ESCAPE '\\'
            ORDER BY nombre
            """, nativeQuery = true)
    List<Alimento> buscarPorNombre(@Param("patron") String patron);

    List<Alimento> findByCategoriaIgnoreCaseOrderByNombreAsc(String categoria);

    Optional<Alimento> findByNombreIgnoreCase(String nombre);

    List<Alimento> findAllByOrderByNombreAsc();
}
