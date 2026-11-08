-- =============================================================================
-- NUTRIGUIDE - ESQUEMA DE BASE DE DATOS RELACIONAL (PostgreSQL 16)
-- Única fuente de verdad del esquema. Spring Boot lo ejecuta al arrancar
-- (spring.sql.init) y Hibernate solo VALIDA que las entidades coincidan.
-- Es idempotente: se puede ejecutar varias veces sin perder datos.
--
-- La base de datos VECTORIAL ya no está aquí: los fragmentos de los documentos y
-- sus embeddings los guarda el flujo n8n en Qdrant (carpeta IA del repositorio).
-- =============================================================================

-- 1. Limpieza de la versión anterior, que guardaba los vectores en PostgreSQL con pgvector.
--    Los fragmentos se regeneran en Qdrant al reindexar, así que no se pierde información original.
DROP TABLE IF EXISTS fragmentos_documentos;
DROP EXTENSION IF EXISTS vector;

-- 2. Alimentos. Todos los valores nutricionales están expresados por
--    porcion_referencia + unidad (en los datos iniciales: por 100 g).
CREATE TABLE IF NOT EXISTS alimentos (
    id                 BIGSERIAL PRIMARY KEY,
    nombre             VARCHAR(150)  NOT NULL UNIQUE,
    descripcion        TEXT          NOT NULL,
    categoria          VARCHAR(100)  NOT NULL,
    imagen_url         VARCHAR(500),
    calorias           NUMERIC(8, 2) NOT NULL CHECK (calorias >= 0),
    proteinas          NUMERIC(8, 2) NOT NULL CHECK (proteinas >= 0),
    carbohidratos      NUMERIC(8, 2) NOT NULL CHECK (carbohidratos >= 0),
    grasas             NUMERIC(8, 2) NOT NULL CHECK (grasas >= 0),
    fibra              NUMERIC(8, 2) NOT NULL CHECK (fibra >= 0),
    porcion_referencia NUMERIC(8, 2) NOT NULL CHECK (porcion_referencia > 0),
    unidad             VARCHAR(50)   NOT NULL,
    fuente_informacion VARCHAR(255)  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_alimentos_nombre    ON alimentos (LOWER(nombre));
CREATE INDEX IF NOT EXISTS idx_alimentos_categoria ON alimentos (LOWER(categoria));

-- 3. Recetas. Los valores nutricionales son POR PORCIÓN SERVIDA (porcion_gramos).
--    Son opcionales: si no se conocen quedan en NULL (nunca se inventan).
CREATE TABLE IF NOT EXISTS recetas (
    id                      BIGSERIAL PRIMARY KEY,
    nombre                  VARCHAR(200) NOT NULL UNIQUE,
    descripcion             TEXT         NOT NULL,
    imagen_url              VARCHAR(500),
    ingredientes            TEXT         NOT NULL,
    preparacion             TEXT         NOT NULL,
    tiempo_preparacion      INTEGER      NOT NULL CHECK (tiempo_preparacion > 0),
    informacion_nutricional TEXT         NOT NULL,
    categoria               VARCHAR(100) NOT NULL
);

-- Columnas añadidas con ALTER para que una base creada con la versión anterior se actualice sola
ALTER TABLE recetas ADD COLUMN IF NOT EXISTS calorias       NUMERIC(8, 2) CHECK (calorias >= 0);
ALTER TABLE recetas ADD COLUMN IF NOT EXISTS proteinas      NUMERIC(8, 2) CHECK (proteinas >= 0);
ALTER TABLE recetas ADD COLUMN IF NOT EXISTS carbohidratos  NUMERIC(8, 2) CHECK (carbohidratos >= 0);
ALTER TABLE recetas ADD COLUMN IF NOT EXISTS grasas         NUMERIC(8, 2) CHECK (grasas >= 0);
ALTER TABLE recetas ADD COLUMN IF NOT EXISTS fibra          NUMERIC(8, 2) CHECK (fibra >= 0);
ALTER TABLE recetas ADD COLUMN IF NOT EXISTS porcion_gramos NUMERIC(8, 2) CHECK (porcion_gramos > 0);

CREATE INDEX IF NOT EXISTS idx_recetas_nombre    ON recetas (LOWER(nombre));
CREATE INDEX IF NOT EXISTS idx_recetas_categoria ON recetas (LOWER(categoria));

-- 4. Documentos educativos (biblioteca del RAG)
CREATE TABLE IF NOT EXISTS documentos (
    id                   BIGSERIAL PRIMARY KEY,
    titulo               VARCHAR(255) NOT NULL,
    descripcion          TEXT,
    categoria            VARCHAR(100) NOT NULL,
    nombre_archivo       VARCHAR(255) NOT NULL,
    ruta_archivo         VARCHAR(500) NOT NULL,
    hash_sha256          VARCHAR(64) UNIQUE,
    fuente_origen        VARCHAR(255) NOT NULL DEFAULT 'Fuente educativa institucional',
    fecha_creacion       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado_procesamiento VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE'
        CHECK (estado_procesamiento IN ('PENDIENTE', 'PROCESANDO', 'PROCESADO', 'ERROR')),
    mensaje_error        TEXT
);

-- Texto limpio extraído (se muestra en la biblioteca y es lo que se envía a n8n para indexar)
-- y número de fragmentos guardados en Qdrant
ALTER TABLE documentos ADD COLUMN IF NOT EXISTS contenido_texto     TEXT;
ALTER TABLE documentos ADD COLUMN IF NOT EXISTS fecha_procesamiento TIMESTAMP;
ALTER TABLE documentos ADD COLUMN IF NOT EXISTS total_fragmentos    INTEGER NOT NULL DEFAULT 0 CHECK (total_fragmentos >= 0);
ALTER TABLE documentos DROP COLUMN IF EXISTS modelo_embedding;

CREATE INDEX IF NOT EXISTS idx_documentos_estado    ON documentos (estado_procesamiento);
CREATE INDEX IF NOT EXISTS idx_documentos_categoria ON documentos (LOWER(categoria));
CREATE UNIQUE INDEX IF NOT EXISTS uk_documentos_titulo ON documentos (LOWER(titulo));
