package com.nutriguide.modules.receta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA de recetas (consultas parametrizadas).
 */
@Repository
public interface RecetaRepository extends JpaRepository<Receta, Long> {

    /** Búsqueda por nombre sin distinguir mayúsculas ni tildes (ver BusquedaTexto). */
    @Query(value = """
            SELECT * FROM recetas
            WHERE translate(lower(nombre), 'áàäâéèëêíìïîóòöôúùüûñ', 'aaaaeeeeiiiioooouuuun') LIKE :patron ESCAPE '\\'
            ORDER BY nombre
            """, nativeQuery = true)
    List<Receta> buscarPorNombre(@Param("patron") String patron);

    List<Receta> findByCategoriaIgnoreCaseOrderByNombreAsc(String categoria);

    Optional<Receta> findByNombreIgnoreCase(String nombre);

    List<Receta> findAllByOrderByNombreAsc();
}
