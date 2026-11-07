-- Esquema automático para inicialización en Spring Boot (PostgreSQL + pgvector)
CREATE EXTENSION IF NOT EXISTS vector;

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

CREATE TABLE IF NOT EXISTS fragmentos_documentos (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL REFERENCES documentos(id) ON DELETE CASCADE,
    contenido TEXT NOT NULL,
    numero_fragmento INTEGER NOT NULL CHECK (numero_fragmento >= 1),
    embedding vector(768) NOT NULL,
    CONSTRAINT uk_documento_numero_fragmento UNIQUE (documento_id, numero_fragmento)
);

CREATE INDEX IF NOT EXISTS idx_fragmentos_documento_id ON fragmentos_documentos (documento_id);

CREATE INDEX IF NOT EXISTS idx_fragmentos_embedding_hnsw
    ON fragmentos_documentos
    USING hnsw (embedding vector_cosine_ops);

