package com.nutriguide.rag;

import com.pgvector.PGvector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio vectorial sobre PostgreSQL + pgvector (consultas parametrizadas con JdbcTemplate).
 * Guarda los fragmentos con su embedding y busca por similitud coseno (operador <=>),
 * usando el índice HNSW de la columna embedding.
 */
@Repository
public class VectorRepository {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public VectorRepository(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Reemplaza en una sola transacción todos los fragmentos de un documento.
     * Si algo falla, los fragmentos anteriores se conservan.
     */
    public void reemplazarFragmentos(Long documentoId, List<String> contenidos, List<float[]> embeddings) {
        if (contenidos.size() != embeddings.size()) {
            throw new IllegalArgumentException("Cada fragmento debe tener exactamente un embedding.");
        }
        List<Object[]> filas = new ArrayList<>(contenidos.size());
        for (int i = 0; i < contenidos.size(); i++) {
            filas.add(new Object[]{documentoId, contenidos.get(i), i + 1, new PGvector(embeddings.get(i)).toString()});
        }
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update("DELETE FROM fragmentos_documentos WHERE documento_id = ?", documentoId);
            jdbcTemplate.batchUpdate("""
                    INSERT INTO fragmentos_documentos (documento_id, contenido, numero_fragmento, embedding)
                    VALUES (?, ?, ?, CAST(? AS vector))
                    """, filas);
        });
    }

    /**
     * Fragmentos más similares a la pregunta entre los documentos PROCESADOS.
     * La subconsulta ordena por distancia con LIMIT (aprovecha el índice HNSW) y después se
     * descartan los fragmentos por debajo de la similitud mínima.
     */
    public List<FragmentoRecuperado> buscarSimilares(float[] embeddingPregunta, int topK, double similitudMinima) {
        String vector = new PGvector(embeddingPregunta).toString();
        return jdbcTemplate.query("""
                        SELECT * FROM (
                            SELECT f.id, f.documento_id, d.titulo, d.categoria, d.fuente_origen, d.nombre_archivo,
                                   f.contenido, f.numero_fragmento,
                                   1 - (f.embedding <=> CAST(? AS vector)) AS similitud
                            FROM fragmentos_documentos f
                            JOIN documentos d ON d.id = f.documento_id
                            WHERE d.estado_procesamiento = 'PROCESADO'
                            ORDER BY f.embedding <=> CAST(? AS vector)
                            LIMIT ?
                        ) candidatos
                        WHERE similitud >= ?
                        ORDER BY similitud DESC
                        """,
                (rs, n) -> new FragmentoRecuperado(
                        rs.getLong("id"),
                        rs.getLong("documento_id"),
                        rs.getString("titulo"),
                        rs.getString("categoria"),
                        rs.getString("fuente_origen"),
                        rs.getString("nombre_archivo"),
                        rs.getString("contenido"),
                        rs.getInt("numero_fragmento"),
                        rs.getDouble("similitud")),
                vector, vector, topK, similitudMinima);
    }

    /** Fragmentos de un documento con la dimensión real del vector almacenado (para verificación). */
    public List<FragmentoAlmacenado> listarPorDocumento(Long documentoId) {
        return jdbcTemplate.query("""
                        SELECT numero_fragmento, contenido, vector_dims(embedding) AS dimension
                        FROM fragmentos_documentos
                        WHERE documento_id = ?
                        ORDER BY numero_fragmento
                        """,
                (rs, n) -> new FragmentoAlmacenado(
                        rs.getInt("numero_fragmento"), rs.getString("contenido"), rs.getInt("dimension")),
                documentoId);
    }

    /** Número de fragmentos por documento, en una sola consulta. */
    public Map<Long, Integer> contarPorDocumento() {
        Map<Long, Integer> conteos = new HashMap<>();
        jdbcTemplate.query("SELECT documento_id, COUNT(*) AS total FROM fragmentos_documentos GROUP BY documento_id",
                rs -> {
                    conteos.put(rs.getLong("documento_id"), rs.getInt("total"));
                });
        return conteos;
    }

    public int contarPorDocumento(Long documentoId) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fragmentos_documentos WHERE documento_id = ?", Integer.class, documentoId);
        return total == null ? 0 : total;
    }

    /** Fragmento recuperado por la búsqueda semántica junto con los datos de su documento. */
    public record FragmentoRecuperado(
            Long id,
            Long documentoId,
            String tituloDocumento,
            String categoriaDocumento,
            String fuenteOrigen,
            String nombreArchivo,
            String contenido,
            int numeroFragmento,
            double similitud) {
    }

    /** Fragmento almacenado en pgvector (sin el vector, con su dimensión). */
    public record FragmentoAlmacenado(int numeroFragmento, String contenido, int dimensionEmbedding) {
    }
}
