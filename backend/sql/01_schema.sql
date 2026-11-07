-- =============================================================================
-- NUTRIGUIDE - SCRIPT DE ESQUEMA E INICIALIZACIÓN DE BASE DE DATOS Y PGVECTOR
-- Motor: PostgreSQL 15+ / 16+ con extensión pgvector
-- Dimensión de embeddings configurada: 768 (Google Gemini gemini-embedding-001 / text-embedding-004)
-- =============================================================================

-- 1. Habilitar la extensión vectorial pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Tabla de Alimentos
-- Nota: Todos los valores nutricionales están estandarizados según porcion_referencia y unidad
-- (por ejemplo, valores por cada 100 g de parte comestible) sin mezclar medidas caseras ambiguas.
CREATE TABLE IF NOT EXISTS alimentos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL UNIQUE,
    descripcion TEXT NOT NULL,
    categoria VARCHAR(100) NOT NULL,
    imagen_url VARCHAR(500),
    calorias NUMERIC(8, 2) NOT NULL CHECK (calorias >= 0),
    proteinas NUMERIC(8, 2) NOT NULL CHECK (proteinas >= 0),
    carbohidratos NUMERIC(8, 2) NOT NULL CHECK (carbohidratos >= 0),
    grasas NUMERIC(8, 2) NOT NULL CHECK (grasas >= 0),
    fibra NUMERIC(8, 2) NOT NULL CHECK (fibra >= 0),
    porcion_referencia NUMERIC(8, 2) NOT NULL CHECK (porcion_referencia > 0),
    unidad VARCHAR(50) NOT NULL,
    fuente_informacion VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_alimentos_nombre ON alimentos (LOWER(nombre));
CREATE INDEX IF NOT EXISTS idx_alimentos_categoria ON alimentos (LOWER(categoria));

-- 3. Tabla de Recetas
-- Nota: El campo informacion_nutricional especifica explícitamente el tamaño de la porción en gramos
-- y el aporte nutricional estimado por porción servida.
CREATE TABLE IF NOT EXISTS recetas (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL UNIQUE,
    descripcion TEXT NOT NULL,
    imagen_url VARCHAR(500),
    ingredientes TEXT NOT NULL,
    preparacion TEXT NOT NULL,
    tiempo_preparacion INTEGER NOT NULL CHECK (tiempo_preparacion > 0),
    informacion_nutricional TEXT NOT NULL,
    categoria VARCHAR(100) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_recetas_nombre ON recetas (LOWER(nombre));
CREATE INDEX IF NOT EXISTS idx_recetas_categoria ON recetas (LOWER(categoria));

-- 4. Tabla de Documentos Educativos (Biblioteca RAG)
CREATE TABLE IF NOT EXISTS documentos (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    categoria VARCHAR(100) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta_archivo VARCHAR(500) NOT NULL,
    hash_sha256 VARCHAR(64) UNIQUE,
    fuente_origen VARCHAR(255) NOT NULL DEFAULT 'Fuente educativa institucional',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado_procesamiento VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE'
        CHECK (estado_procesamiento IN ('PENDIENTE', 'PROCESANDO', 'PROCESADO', 'ERROR')),
    mensaje_error TEXT
);

CREATE INDEX IF NOT EXISTS idx_documentos_estado ON documentos (estado_procesamiento);
CREATE INDEX IF NOT EXISTS idx_documentos_categoria ON documentos (LOWER(categoria));

-- 5. Tabla de Fragmentos de Documentos y Vectores (Base de datos vectorial pgvector)
CREATE TABLE IF NOT EXISTS fragmentos_documentos (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL REFERENCES documentos(id) ON DELETE CASCADE,
    contenido TEXT NOT NULL,
    numero_fragmento INTEGER NOT NULL CHECK (numero_fragmento >= 1),
    embedding vector(768) NOT NULL,
    CONSTRAINT uk_documento_numero_fragmento UNIQUE (documento_id, numero_fragmento)
);

CREATE INDEX IF NOT EXISTS idx_fragmentos_documento_id ON fragmentos_documentos (documento_id);

-- Índice vectorial HNSW con distancia coseno (vector_cosine_ops) para búsqueda semántica eficiente
CREATE INDEX IF NOT EXISTS idx_fragmentos_embedding_hnsw
    ON fragmentos_documentos
    USING hnsw (embedding vector_cosine_ops);

