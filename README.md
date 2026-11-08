# NutriGuide – Asistente inteligente de alimentación

Plataforma web educativa para consultar alimentos y sus propiedades nutricionales, explorar recetas, acceder a una biblioteca de recursos y hacer preguntas a un asistente de inteligencia artificial que responde con **RAG** (recuperación de información desde una base de datos vectorial) citando sus fuentes.

> **Aviso:** NutriGuide es una herramienta educativa. No diagnostica enfermedades, no prescribe dietas y no reemplaza la orientación de un profesional de la salud.

## Estado del proyecto

| Área | Estado |
|---|---|
| Frontend (HTML5 + CSS3 + JavaScript puro) | **Implementado y probado contra el backend real** (catálogo, detalle, recetas, biblioteca y chat). 12 pruebas unitarias. |
| Backend (Java 21 + Spring Boot 3.4) | **Implementado.** Compila y arranca contra PostgreSQL + pgvector. 29 pruebas (unitarias e integración). |
| Base de datos relacional (PostgreSQL 16) | **Implementada.** Esquema único con restricciones, índices y datos iniciales. |
| Base de datos vectorial (pgvector) | **Implementada y probada** con PostgreSQL + pgvector real (vectores de 768 dimensiones, índice HNSW, búsqueda coseno). |
| Sistema RAG (embeddings + modelo de lenguaje) | **Implementado.** El flujo completo está probado con un doble del proveedor de IA. **Pendiente: probarlo con una clave real de Google Gemini** y ajustar la similitud mínima. |

## Tecnologías

- **Frontend:** HTML5, CSS3 y JavaScript puro (Fetch API). Sin frameworks.
- **Backend:** Java 21, Spring Boot (Web, Data JPA, Validation), Maven (wrapper incluido).
- **Bases de datos:** PostgreSQL 16 con la extensión **pgvector**.
- **Inteligencia artificial:** Google Gemini: `gemini-embedding-001` (embeddings) y `gemini-2.5-flash` (respuestas), integrados con el `HttpClient` de Java. El RAG está implementado en Java.

## Puesta en marcha rápida

**Windows, un solo comando** (con Docker Desktop abierto):

```powershell
# La primera vez: copia backend\.env.example a backend\.env y pon tu GEMINI_API_KEY
powershell -ExecutionPolicy Bypass -File .\iniciar-nutriguide.ps1           # arrancar
powershell -ExecutionPolicy Bypass -File .\iniciar-nutriguide.ps1 -Detener  # detener
```

El script levanta la base de datos, compila y arranca el backend (registro en `backend\logs\`) y sirve el frontend en `http://localhost:5500`. Si ya usas Live Server de VS Code con la carpeta del proyecto abierta, el frontend está en `http://127.0.0.1:5500/frontend/index.html`. Tras cambiar código del backend, usa `-Compilar`.

**Paso a paso (cualquier sistema):**

```bash
# 1. Base de datos (PostgreSQL 16 + pgvector en el puerto 5434)
cd backend
docker compose up -d

# 2. Backend en http://localhost:8080 (ver backend/README.md para las variables)
#    PowerShell: $env:GEMINI_API_KEY="tu-clave"; $env:ADMIN_TOKEN="un-token-largo"
./mvnw spring-boot:run

# 3. Frontend en http://localhost:5500
cd ../frontend
python -m http.server 5500
```

Sin `GEMINI_API_KEY` funcionan el catálogo, las recetas y la biblioteca; el chat responde con un aviso de que la IA no está configurada.

## Estructura del repositorio

```
IA2/
├── README.md
├── frontend/                  Cliente web (ver frontend/README.md)
│   ├── index.html · pages/    Páginas
│   ├── css/ · js/             Estilos y módulos ES (api.js centraliza las peticiones)
│   ├── assets/                Imágenes e íconos
│   └── tests/                 Pruebas unitarias (node --test)
└── backend/                   API REST Spring Boot (ver backend/README.md)
    ├── docker-compose.yml     PostgreSQL 16 + pgvector
    └── src/main/java/com/nutriguide/
        ├── config/            CORS, IA, verificación de pgvector, seguridad, carga inicial
        ├── modules/           alimento · receta · documento · chat
        │                      (entidad, repositorio, servicio, controlador y DTO de cada uno)
        ├── rag/               DocumentProcessor → TextChunker → EmbeddingService →
        │                      VectorRepository → RetrievalService → LlmGenerationService → RagService
        └── common/            Errores centralizados y utilidades
```

El backend sigue la arquitectura **modular por responsabilidades** que define el prompt del Desarrollador 2: cada módulo agrupa su controlador, servicio, repositorio, entidad y DTO, y el RAG vive en su propio paquete.

## Contrato entre frontend y backend

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/alimentos` | Catálogo de alimentos |
| GET | `/api/alimentos/{id}` | Detalle de un alimento |
| GET | `/api/alimentos/buscar?nombre=` | Búsqueda por nombre (sin distinguir tildes) |
| GET | `/api/recetas` · `/api/recetas/{id}` | Recetas |
| GET | `/api/documentos` · `/api/documentos/{id}` | Biblioteca |
| POST | `/api/chat/preguntar` | Pregunta al asistente (RAG) |
| POST | `/api/documentos` | Cargar documentos al RAG (requiere `X-Admin-Token`) |

```json
// POST /api/chat/preguntar
{ "pregunta": "¿Qué nutrientes contiene la avena?" }

// Respuesta
{ "respuesta": "Según la guía de cereales…",
  "fuentes": [ { "titulo": "Guía educativa sobre cereales integrales…", "contenido": "Fragmento utilizado" } ],
  "informacionSuficiente": true,
  "advertenciaEducativa": "…" }
```

Los errores tienen la forma `{"exito": false, "mensaje": "…"}`, y el frontend muestra el `mensaje`. La lista completa de endpoints y códigos HTTP está en [backend/README.md](backend/README.md); los campos que interpreta el frontend, en [frontend/README.md](frontend/README.md).

## Equipo

- **Desarrollador 1:** frontend.
- **Desarrollador 2:** backend (Spring Boot), base de datos y sistema RAG.

## Pendiente

- Probar el RAG con una clave real de Gemini y ajustar `RAG_MIN_SIMILARITY` según los resultados.
- Sustituir o complementar los documentos iniciales (síntesis redactadas por el equipo) por documentos oficiales de FAO, OMS o ICBF cargados con `POST /api/documentos`.
- Revisar las fotografías de los alimentos que no tienen una imagen verificada (hoy muestran una ilustración).
