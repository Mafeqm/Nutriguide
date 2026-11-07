package com.nutriguide.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Configuración de la base de datos relacional y vectorial (PostgreSQL + pgvector).
 * Verifica la disponibilidad de la extensión pgvector y garantiza la estructura vectorial.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @Order(1)
    public CommandLineRunner initializePgVectorExtension(
            DataSource dataSource,
            JdbcTemplate jdbcTemplate,
            AiConfig aiConfig) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                String dbProductName = metaData.getDatabaseProductName();
                log.info("Conectado a base de datos: {} ({})", dbProductName, metaData.getDatabaseProductVersion());

                if (dbProductName != null && dbProductName.toLowerCase().contains("postgresql")) {
                    jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
                    int dimension = aiConfig.getEmbeddingDimension();
                    jdbcTemplate.execute("""
                            CREATE TABLE IF NOT EXISTS fragmentos_documentos (
                                id BIGSERIAL PRIMARY KEY,
                                documento_id BIGINT NOT NULL REFERENCES documentos(id) ON DELETE CASCADE,
                                contenido TEXT NOT NULL,
                                numero_fragmento INTEGER NOT NULL CHECK (numero_fragmento >= 1),
                                embedding vector(%d) NOT NULL,
                                CONSTRAINT uk_documento_numero_fragmento UNIQUE (documento_id, numero_fragmento)
                            )
                            """.formatted(dimension));
                    jdbcTemplate.execute(
                            "CREATE INDEX IF NOT EXISTS idx_fragmentos_documento_id ON fragmentos_documentos (documento_id)");
                    jdbcTemplate.execute("""
                            CREATE INDEX IF NOT EXISTS idx_fragmentos_embedding_hnsw
                            ON fragmentos_documentos USING hnsw (embedding vector_cosine_ops)
                            """);
                    log.info("Extensión pgvector verificada y tabla vectorial 'fragmentos_documentos' (dimensión {}) lista.",
                            dimension);
                } else {
                    log.info("Ejecutando en motor '{}' (modo de pruebas sin extensión nativa pgvector).", dbProductName);
                }
            } catch (Exception ex) {
                log.error("Error al inicializar la extensión pgvector en PostgreSQL. " +
                        "Verifique que la imagen o instancia PostgreSQL tenga instalada la extensión 'vector': {}",
                        ex.getMessage());
            }
        };
    }
}

