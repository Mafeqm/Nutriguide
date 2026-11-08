# NutriGuide – Backend

API REST en **Java 21 + Spring Boot 3.4** con **PostgreSQL 16 + pgvector** y un sistema **RAG** propio en Java (Google Gemini para embeddings y generación).

## Requisitos

- JDK 21 (`JAVA_HOME` apuntando a él). No hace falta instalar Maven: se usa el Maven Wrapper (`mvnw`).
- Docker (para PostgreSQL + pgvector y para las pruebas de integración).
- Una clave de Google Gemini (<https://aistudio.google.com/apikey>) para el chat y la indexación de documentos.

## Puesta en marcha

```bash
cd backend

# 1. Base de datos PostgreSQL 16 + pgvector (puerto 5434 del host)
docker compose up -d

# 2. Variables de entorno (ver .env.example)
#    PowerShell:
$env:GEMINI_API_KEY = "tu-clave"
$env:ADMIN_TOKEN    = "un-token-largo-y-aleatorio"
#    Bash:
export GEMINI_API_KEY=tu-clave ADMIN_TOKEN=un-token-largo-y-aleatorio

# 3. Arrancar el backend en http://localhost:8080
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Al arrancar, el backend:

1. Ejecuta `src/main/resources/db/schema.sql` (extensión pgvector, tablas, índices) y `db/data.sql` (14 alimentos y 5 recetas). Ambos son idempotentes.
2. Comprueba que pgvector esté instalado y que la columna `embedding` tenga la dimensión configurada; si no, no arranca.
3. Registra los 5 documentos educativos de `src/main/resources/initial-docs` y, si hay `GEMINI_API_KEY`, los indexa en pgvector.

Después abre el frontend (`frontend/`, servido en `http://localhost:5500`): su `js/config.js` ya apunta a `http://localhost:8080`.

**Sin `GEMINI_API_KEY`** funcionan el catálogo, las recetas y la biblioteca. El chat responde **503** con un mensaje claro y los documentos quedan **PENDIENTES** hasta que se configure la clave y se reinicie (o se llame a `POST /api/documentos/reprocesar-pendientes`). No existe ningún modo que simule la IA.

## Variables de entorno

| Variable | Por defecto | Uso |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | `localhost`, `5434`, `nutriguide_db`, `nutriguide`, contraseña de desarrollo | Conexión a PostgreSQL |
| `GEMINI_API_KEY` | vacío | Clave de Google Gemini |
| `GEMINI_EMBEDDING_MODEL` / `GEMINI_LLM_MODEL` | `gemini-embedding-001` / `gemini-2.5-flash` | Modelos |
| `RAG_MIN_SIMILARITY` | `0.55` | Similitud coseno mínima para usar un fragmento |
| `ADMIN_TOKEN` | vacío (escrituras deshabilitadas) | Token para crear/modificar/borrar y cargar documentos |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:[*],http://127.0.0.1:[*]` (cualquier puerto local) | Orígenes del frontend |
| `CHAT_RATE_LIMIT` | `20` | Preguntas por IP y minuto |
| `UPLOAD_DIR` | `./uploads/documentos` | Carpeta de los archivos subidos |

La contraseña por defecto de la base (`nutriguide_dev_pass`) es solo para el contenedor local; cámbiala con `DB_PASSWORD` fuera de desarrollo.

## Endpoints

Las respuestas de error tienen siempre la forma `{"exito": false, "mensaje": "..."}`.

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/api/alimentos?categoria=` | Lista de alimentos (filtro opcional) | 200 |
| GET | `/api/alimentos/{id}` | Detalle | 200, 404, 400 |
| GET | `/api/alimentos/buscar?nombre=` | Búsqueda sin distinguir mayúsculas ni tildes | 200 |
| GET | `/api/recetas?categoria=&nombre=` | Lista de recetas | 200 |
| GET | `/api/recetas/{id}` | Detalle | 200, 404 |
| GET | `/api/documentos` | Biblioteca (incluye `contenido` y `url` del archivo) | 200 |
| GET | `/api/documentos/{id}` | Detalle de un documento | 200, 404 |
| GET | `/api/documentos/{id}/archivo` | Archivo original (.pdf/.txt) | 200, 404 |
| GET | `/api/documentos/{id}/fragmentos` | Fragmentos guardados en pgvector y dimensión de su vector | 200, 404 |
| POST | `/api/chat/preguntar` | Pregunta al asistente RAG | 200, 400, 429, 502, 503 |
| POST 🔒 | `/api/documentos` (multipart, campo `archivo`) | Cargar e indexar un .pdf o .txt (máx. 10 MB) | 201, 409, 422 |
| POST 🔒 | `/api/documentos` (JSON) | Registrar un documento desde texto | 201, 400, 409 |
| POST 🔒 | `/api/documentos/{id}/reprocesar` | Reindexar un documento | 200, 502, 503 |
| POST 🔒 | `/api/documentos/reprocesar-pendientes` | Indexar los pendientes / con error | 200 |
| DELETE 🔒 | `/api/documentos/{id}` | Borrar documento, archivo y fragmentos | 200, 404 |
| POST/PUT/DELETE 🔒 | `/api/alimentos`, `/api/recetas` | Administrar alimentos y recetas | 201/200, 400, 404, 409 |

🔒 requiere la cabecera `X-Admin-Token` con el valor de `ADMIN_TOKEN` (401 si falta o no coincide; 403 si `ADMIN_TOKEN` no está configurado).

### Chat

```http
POST /api/chat/preguntar
Content-Type: application/json

{ "pregunta": "¿Qué nutrientes contiene la avena?" }
```

```json
{
  "respuesta": "Según la Guía educativa sobre cereales integrales…",
  "fuentes": [
    { "titulo": "Guía educativa sobre cereales integrales, avena, quinua y fibra dietética",
      "contenido": "Fragmento recuperado…", "documentoId": 1, "numeroFragmento": 2,
      "categoria": "Cereales y carbohidratos complejos", "fuenteOrigen": "…", "similitud": 0.78 }
  ],
  "informacionSuficiente": true,
  "advertenciaEducativa": "Información con fines educativos…"
}
```

Si la biblioteca no tiene información suficiente: `informacionSuficiente: false`, `fuentes: []` y una respuesta que lo indica.

### Cargar un documento

```bash
curl -H "X-Admin-Token: $ADMIN_TOKEN" \
     -F "archivo=@guia.pdf" -F "titulo=Guía de hidratación" \
     -F "categoria=Hábitos" -F "fuenteOrigen=OMS - Nota descriptiva sobre agua potable" \
     http://localhost:8080/api/documentos
```

## Base de datos

- **Esquema único:** `src/main/resources/db/schema.sql` (Hibernate solo valida, `ddl-auto=validate`).
- **Tablas:** `alimentos`, `recetas`, `documentos`, `fragmentos_documentos` (FK `documento_id` con `ON DELETE CASCADE`, único `(documento_id, numero_fragmento)`).
- **Unidades:** los alimentos se expresan por `porcion_referencia` + `unidad` (100 g en los datos iniciales); las recetas, **por porción servida** (`porcion_gramos`). Un valor nutricional desconocido queda en `NULL`; nunca se inventa.

### pgvector

- Se habilita con `CREATE EXTENSION IF NOT EXISTS vector;` al inicio de `schema.sql`. La imagen `pgvector/pgvector:pg16` del `docker-compose.yml` ya incluye la extensión. En un PostgreSQL propio hay que instalarla antes (<https://github.com/pgvector/pgvector>).
- La columna es `embedding vector(768)` con un índice **HNSW** de distancia coseno (`vector_cosine_ops`).
- **Dimensión:** `gemini-embedding-001` genera 3072 dimensiones; se le piden 768 con `outputDimensionality` y los vectores se normalizan (L2). Para cambiarla hay que modificar a la vez `vector(768)` en `schema.sql` y `ai.embedding.dimension` en `application.properties` y volver a indexar. Si no coinciden, la aplicación se niega a arrancar.
- Cada documento guarda el modelo con el que se indexó; si cambia el modelo o la dimensión, se reindexa al arrancar.

## Flujo RAG

```
Ingesta:  archivo → validación (extensión, cabecera PDF, UTF-8, 10 MB, SHA-256 duplicado)
          → extracción (PDFBox) y limpieza → fragmentos de ~900 caracteres con 150 de solapamiento
          → embeddings RETRIEVAL_DOCUMENT en lotes → pgvector (una transacción) → estado PROCESADO

Consulta: pregunta → embedding RETRIEVAL_QUERY → búsqueda coseno en pgvector (top 4, similitud ≥ 0.55)
          → sin fragmentos: "no hay información suficiente" (no se llama al modelo)
          → con fragmentos: se neutralizan instrucciones maliciosas → contexto delimitado
          → Gemini (reglas en systemInstruction) → respuesta + fuentes
```

| Paso | Clase |
|---|---|
| Validación y extracción | `rag/DocumentProcessor` |
| Fragmentación | `rag/TextChunker` |
| Embeddings (con reintentos ante 429/5xx) | `rag/EmbeddingService` + `rag/GeminiClient` |
| Almacenamiento y búsqueda vectorial | `rag/VectorRepository` |
| Recuperación y protección contra inyección de prompt | `rag/RetrievalService` |
| Generación | `rag/LlmGenerationService` |
| Orquestación | `rag/RagService` |

El modelo puede indicar que el contexto no basta (marca `[SIN_INFORMACION]`), y entonces la respuesta sale con `informacionSuficiente: false`. Las reglas del asistente le impiden diagnosticar, prescribir dietas o seguir instrucciones contenidas en los documentos.

## Seguridad

- Claves solo por variables de entorno; nunca en el código ni en git (`.env` está en `.gitignore`).
- Operaciones de escritura protegidas con `X-Admin-Token` (comparación en tiempo constante).
- CORS limitado a los orígenes configurados.
- Consultas parametrizadas (JPA y `JdbcTemplate`), incluida la búsqueda con comodines escapados.
- Validación de archivos: solo .pdf y .txt, cabecera PDF real, sin PDF cifrados, texto UTF-8 sin binarios, 10 MB máximo, nombres saneados y control de duplicados por SHA-256 y título.
- La API no expone rutas internas del servidor.
- Límite de preguntas por IP para proteger la cuota del proveedor de IA.

## Pruebas

```bash
./mvnw test
```

- **Unitarias:** fragmentación, extracción de PDF/TXT, validaciones, hash, protección contra inyección de prompt, conversión de recetas sin valores inventados, búsqueda sin tildes, y los servicios de Gemini contra un servidor HTTP falso (formato de petición, dimensión, reintentos, errores).
- **Integración** (`NutriGuideIntegrationTest`): levanta **PostgreSQL 16 + pgvector real** con Testcontainers y recorre el sistema completo con MockMvc: esquema y dimensión, datos iniciales, indexación de documentos con vectores de 768 dimensiones, carga de TXT y PDF, duplicados, búsqueda semántica, contexto enviado al modelo, fuentes que existen en la base, casos sin información, instrucciones maliciosas bloqueadas y errores HTTP.
  El proveedor de IA se sustituye por un doble HTTP (`FakeGeminiServer`), así que **no se evalúa la calidad de las respuestas del modelo real**. Sin Docker, esta prueba se omite.

### Probar con Gemini real

Con `GEMINI_API_KEY` configurada, después de arrancar:

1. Comprueba con `GET /api/documentos` que los 5 documentos iniciales están en `PROCESADO` (el log muestra `registrado: PROCESADO` para cada uno).
2. `GET /api/documentos/1/fragmentos` debe mostrar `dimensionEmbedding: 768`.
3. Pregunta algo cubierto por los documentos («¿Qué nutrientes contiene la avena?») y algo que no («¿Cómo configuro Kubernetes?»).
4. Si respuestas pertinentes salen como «sin información», baja `RAG_MIN_SIMILARITY` (por ejemplo a 0.45); si entran fragmentos poco relacionados, súbelo. El log muestra la similitud del mejor fragmento de cada pregunta.

## Datos iniciales y procedencia

- **Alimentos:** valores de USDA FoodData Central por 100 g, con el FDC ID en `fuente_informacion`. Las fotos son de Unsplash y solo se usan cuando corresponden al alimento; si no, el campo queda vacío y el frontend muestra una ilustración.
- **Recetas:** valores por porción **estimados** sumando los valores USDA de cada ingrediente; no son análisis de laboratorio.
- **Documentos (`initial-docs`):** son **síntesis educativas redactadas por el equipo** a partir de las referencias citadas (FAO, OMS, ICBF, USDA); no son publicaciones oficiales de esas instituciones y así se indica en su campo de procedencia. Para un uso real se recomienda cargar los documentos oficiales (PDF) con `POST /api/documentos`.

## Estructura

```
backend/
├── docker-compose.yml          PostgreSQL 16 + pgvector
├── mvnw, mvnw.cmd, .mvn/       Maven Wrapper
├── pom.xml
└── src/
    ├── main/java/com/nutriguide/
    │   ├── NutriGuideApplication.java
    │   ├── config/      AiConfig, CorsConfig, DatabaseConfig, DocumentSeeder, WebConfig,
    │   │                AdminTokenInterceptor, ChatRateLimitInterceptor
    │   ├── modules/
    │   │   ├── alimento/   entidad, repositorio, servicio, controlador, DTO
    │   │   ├── receta/     ídem
    │   │   ├── documento/  ídem + estado de procesamiento
    │   │   └── chat/       controlador, servicio, DTO
    │   ├── rag/         DocumentProcessor, TextChunker, EmbeddingService, GeminiClient,
    │   │                VectorRepository, RetrievalService, LlmGenerationService, RagService
    │   └── common/      ApiResponse, GlobalExceptionHandler, excepciones, BusquedaTexto
    ├── main/resources/
    │   ├── application.properties
    │   ├── db/schema.sql, db/data.sql
    │   └── initial-docs/   documentos educativos iniciales
    └── test/java/com/nutriguide/   pruebas unitarias e integración (Testcontainers)
```

## Problemas frecuentes

- **`Bind for 0.0.0.0:5434 failed: port is already allocated`:** otro servicio usa el puerto. Usa otro: `DB_PORT=5435 docker compose up -d` y arranca el backend con la misma variable `DB_PORT`.
- **`La extensión pgvector no está instalada`:** la base no es la imagen `pgvector/pgvector`; usa el `docker-compose.yml`.
- **El chat responde 503:** falta `GEMINI_API_KEY`.
- **Error 403 al cargar documentos:** falta `ADMIN_TOKEN` en el servidor; 401 si el token enviado no coincide.
- **Error CORS en el navegador:** añade el origen del frontend a `CORS_ALLOWED_ORIGINS`.
