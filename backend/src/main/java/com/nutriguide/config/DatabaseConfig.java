package com.nutriguide.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Verificación de la base vectorial al arrancar.
 * El esquema lo crea db/schema.sql; aquí se comprueba que pgvector esté instalado y que la
 * dimensión de la columna embedding coincida con ai.embedding.dimension. Si no coincide, la
 * aplicación no arranca: guardar vectores de otra dimensión fallaría en cada documento.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @Order(1)
    public ApplicationRunner verificarPgVector(JdbcTemplate jdbcTemplate, AiConfig aiConfig) {
        return args -> {
            List<String> version = jdbcTemplate.queryForList(
                    "SELECT extversion FROM pg_extension WHERE extname = 'vector'", String.class);
            if (version.isEmpty()) {
                throw new IllegalStateException(
                        "La extensión pgvector no está instalada en PostgreSQL. Use la imagen pgvector/pgvector:pg16 "
                                + "(docker-compose) o instale la extensión 'vector'.");
            }

            Integer dimension = jdbcTemplate.queryForObject("""
                    SELECT a.atttypmod
                    FROM pg_attribute a
                    WHERE a.attrelid = 'fragmentos_documentos'::regclass
                      AND a.attname = 'embedding'
                    """, Integer.class);
            if (dimension == null || dimension != aiConfig.getEmbeddingDimension()) {
                throw new IllegalStateException("La columna fragmentos_documentos.embedding tiene dimensión "
                        + dimension + " pero ai.embedding.dimension=" + aiConfig.getEmbeddingDimension()
                        + ". Ajuste ambos valores para que coincidan.");
            }
            log.info("pgvector {} activo. Columna embedding: vector({}).", version.get(0), dimension);

            if (!aiConfig.hasValidApiKey()) {
                log.warn("GEMINI_API_KEY no está configurada: el chat responderá 503 y los documentos "
                        + "quedarán PENDIENTES de indexar hasta que se configure la clave.");
            }
        };
    }
}
