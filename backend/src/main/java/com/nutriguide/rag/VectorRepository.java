package com.nutriguide.rag;

import com.pgvector.PGvector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repositorio vectorial encargado de almacenar embeddings y ejecutar búsquedas por similitud
 * semántica (distancia coseno <=>) sobre PostgreSQL con la extensión pgvector.
 */
@Repository
public class VectorRepository {

    private static final Logger log = LoggerFactory.getLogger(VectorRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final boolean postgresBackend;

    // Almacenamiento en memoria auxiliar exclusivamente para entorno de pruebas unitarias sin PostgreSQL (H2)
    private final Map<Long, List<RegistroVectorMemoria>> memoriaPruebas = new ConcurrentHashMap<>();
    private final AtomicLong secuenciaMemoria = new AtomicLong(1L);

    public VectorRepository(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.postgresBackend = detectarPostgres(dataSource);
    }

    private boolean detectarPostgres(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            String productName = conn.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (Exception ex) {
            log.warn("No se pudo detectar el motor de base de datos al iniciar VectorRepository: {}", ex.getMessage());
            return true;
        }
    }

    /**
     * Guarda un fragmento de documento junto con su vector de embedding en pgvector.
     */
    public void guardarFragmento(
            Long documentoId,
            String tituloDocumento,
            String categoriaDocumento,
            String fuenteOrigen,
            String nombreArchivo,
            int numeroFragmento,
            String contenido,
            float[] embedding) {

        if (postgresBackend) {
            PGvector pgVector = new PGvector(embedding);
            String vectorLiteral = pgVector.toString();
            String sql = """
                    INSERT INTO fragmentos_documentos (documento_id, contenido, numero_fragmento, embedding)
                    VALUES (?, ?, ?, CAST(? AS vector))
                    ON CONFLICT (documento_id, numero_fragmento)
                    DO UPDATE SET contenido = EXCLUDED.contenido, embedding = EXCLUDED.embedding
                    """;
            jdbcTemplate.update(sql, documentoId, contenido, numeroFragmento, vectorLiteral);
        } else {
            memoriaPruebas.computeIfAbsent(documentoId, k -> new ArrayList<>())
                    .add(new RegistroVectorMemoria(
                            secuenciaMemoria.getAndIncrement(),
                            documentoId,
                            tituloDocumento,
                            categoriaDocumento,
                            fuenteOrigen,
                            nombreArchivo,
                            contenido,
                            numeroFragmento,
                            Arrays.copyOf(embedding, embedding.length)
                    ));
        }
    }

    /**
     * Busca los fragmentos más similares semánticamente al vector de la pregunta utilizando
     * el operador de distancia coseno (<=>) de pgvector sobre documentos en estado PROCESADO.
     */
    public List<FragmentoRecuperado> buscarSimilares(float[] queryEmbedding, int topK, double minSimilarity) {
        if (postgresBackend) {
            PGvector pgVector = new PGvector(queryEmbedding);
            String vectorLiteral = pgVector.toString();
            String sql = """
                    SELECT f.id,
                           f.documento_id,
                           d.titulo AS titulo_documento,
                           d.categoria AS categoria_documento,
                           d.fuente_origen,
                           d.nombre_archivo,
                           f.contenido,
                           f.numero_fragmento,
                           (1 - (f.embedding <=> CAST(? AS vector))) AS similitud
                    FROM fragmentos_documentos f
                    INNER JOIN documentos d ON d.id = f.documento_id
                    WHERE d.estado_procesamiento = 'PROCESADO'
                      AND (1 - (f.embedding <=> CAST(? AS vector))) >= ?
                    ORDER BY f.embedding <=> CAST(? AS vector) ASC
                    LIMIT ?
                    """;

            return jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> new FragmentoRecuperado(
                            rs.getLong("id"),
                            rs.getLong("documento_id"),
                            rs.getString("titulo_documento"),
                            rs.getString("categoria_documento"),
                            rs.getString("fuente_origen"),
                            rs.getString("nombre_archivo"),
                            rs.getString("contenido"),
                            rs.getInt("numero_fragmento"),
                            rs.getDouble("similitud")
                    ),
                    vectorLiteral,
                    vectorLiteral,
                    minSimilarity,
                    topK
            );
        } else {
            return memoriaPruebas.values().stream()
                    .flatMap(List::stream)
                    .map(reg -> new FragmentoRecuperado(
                            reg.id(),
                            reg.documentoId(),
                            reg.tituloDocumento(),
                            reg.categoriaDocumento(),
                            reg.fuenteOrigen(),
                            reg.nombreArchivo(),
                            reg.contenido(),
                            reg.numeroFragmento(),
                            calcularSimilitudCoseno(queryEmbedding, reg.embedding())
                    ))
                    .filter(f -> f.similitud() >= minSimilarity)
                    .sorted(Comparator.comparingDouble(FragmentoRecuperado::similitud).reversed())
                    .limit(topK)
                    .toList();
        }
    }

    /**
     * Elimina todos los fragmentos vectoriales asociados a un documento.
     */
    public void eliminarPorDocumentoId(Long documentoId) {
        if (postgresBackend) {
            jdbcTemplate.update("DELETE FROM fragmentos_documentos WHERE documento_id = ?", documentoId);
        } else {
            memoriaPruebas.remove(documentoId);
        }
    }

    /**
     * Cuenta la cantidad de fragmentos vectorizados almacenados para un documento.
     */
    public int contarPorDocumentoId(Long documentoId) {
        if (postgresBackend) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM fragmentos_documentos WHERE documento_id = ?",
                    Integer.class,
                    documentoId);
            return count != null ? count : 0;
        } else {
            return memoriaPruebas.getOrDefault(documentoId, List.of()).size();
        }
    }

    /**
     * Lista los fragmentos almacenados de un documento específico ordenados por su número de secuencia.
     */
    public List<FragmentoRecuperado> listarPorDocumentoId(Long documentoId) {
        if (postgresBackend) {
            String sql = """
                    SELECT f.id,
                           f.documento_id,
                           d.titulo AS titulo_documento,
                           d.categoria AS categoria_documento,
                           d.fuente_origen,
                           d.nombre_archivo,
                           f.contenido,
                           f.numero_fragmento
                    FROM fragmentos_documentos f
                    INNER JOIN documentos d ON d.id = f.documento_id
                    WHERE f.documento_id = ?
                    ORDER BY f.numero_fragmento ASC
                    """;
            return jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> new FragmentoRecuperado(
                            rs.getLong("id"),
                            rs.getLong("documento_id"),
                            rs.getString("titulo_documento"),
                            rs.getString("categoria_documento"),
                            rs.getString("fuente_origen"),
                            rs.getString("nombre_archivo"),
                            rs.getString("contenido"),
                            rs.getInt("numero_fragmento"),
                            1.0
                    ),
                    documentoId
            );
        } else {
            return memoriaPruebas.getOrDefault(documentoId, List.of()).stream()
                    .sorted(Comparator.comparingInt(RegistroVectorMemoria::numeroFragmento))
                    .map(reg -> new FragmentoRecuperado(
                            reg.id(),
                            reg.documentoId(),
                            reg.tituloDocumento(),
                            reg.categoriaDocumento(),
                            reg.fuenteOrigen(),
                            reg.nombreArchivo(),
                            reg.contenido(),
                            reg.numeroFragmento(),
                            1.0
                    ))
                    .toList();
        }
    }

    private double calcularSimilitudCoseno(float[] a, float[] b) {
        int len = Math.min(a.length, b.length);
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < len; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA <= 1e-12 || normB <= 1e-12) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Proyección inmutable de un fragmento recuperado desde pgvector junto con los metadatos de su documento.
     */
    public record FragmentoRecuperado(
            Long id,
            Long documentoId,
            String tituloDocumento,
            String categoriaDocumento,
            String fuenteOrigen,
            String nombreArchivo,
            String contenido,
            int numeroFragmento,
            double similitud
    ) {
    }

    private record RegistroVectorMemoria(
            Long id,
            Long documentoId,
            String tituloDocumento,
            String categoriaDocumento,
            String fuenteOrigen,
            String nombreArchivo,
            String contenido,
            int numeroFragmento,
            float[] embedding
    ) {
    }
}

