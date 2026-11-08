# NutriGuide – Asistente inteligente de alimentación

Plataforma web educativa para consultar alimentos y sus propiedades nutricionales, explorar recetas, acceder a una biblioteca de recursos y hacer preguntas a un asistente de inteligencia artificial que responde con **RAG** (recuperación de información desde una base de datos vectorial) citando sus fuentes.

> **Aviso:** NutriGuide es una herramienta educativa. No diagnostica enfermedades, no prescribe dietas y no reemplaza la orientación de un profesional de la salud.

## Arquitectura

```
┌──────────────┐     ┌────────────────────────────┐     ┌───────────────────────────────┐
│   Frontend   │────►│  Backend (Spring Boot)     │────►│  IA: n8n (flujo RAG)          │
│ HTML/CSS/JS  │     │  /api/alimentos, /recetas, │     │   ├─ Qdrant (base vectorial)  │
└──────────────┘     │  /documentos, /chat        │     │   └─ Google Gemini            │
                     └─────────────┬──────────────┘     └───────────────────────────────┘
                                   ▼
                         PostgreSQL (datos relacionales)
```

- El **frontend** solo habla con el backend.
- El **backend** guarda alimentos, recetas y documentos en PostgreSQL. Para la IA llama a los webhooks de n8n con un token compartido.
- **n8n** ejecuta todo el asistente: fragmenta los documentos, genera embeddings, los guarda y busca en **Qdrant**, y redacta las respuestas con Gemini.

## Estado del proyecto

| Área | Estado |
|---|---|
| Frontend (HTML5 + CSS3 + JavaScript puro) | **Implementado y probado contra el backend real**, incluido el chat. 12 pruebas unitarias. |
| Backend (Java 21 + Spring Boot 3.4) | **Implementado.** Compila y arranca. 22 pruebas (unitarias e integración con PostgreSQL real). |
| Base de datos relacional (PostgreSQL 16) | **Implementada.** Esquema único con restricciones, índices y datos iniciales. |
| Asistente de IA (n8n + Qdrant) | **Implementado y probado con Google Gemini real**, n8n 2.46.6 y Qdrant 1.20.0: 5 documentos indexados (27 fragmentos), respuestas con fuentes, casos sin información y preguntas médicas remitidas a un profesional. |

## Tecnologías

- **Frontend:** HTML5, CSS3 y JavaScript puro (Fetch API). Sin frameworks.
- **Backend:** Java 21, Spring Boot (Web, Data JPA, Validation), Maven (wrapper incluido).
- **Base de datos relacional:** PostgreSQL 16.
- **Asistente de IA:** n8n (flujo RAG), Qdrant (base vectorial) y Google Gemini (`gemini-embedding-001` para embeddings y `gemini-3.5-flash-lite` para las respuestas, ambos configurables en `IA/.env`).

## Cómo ejecutarlo (Windows, Ubuntu o macOS)

**Requisitos:** JDK 21, Docker (Docker Desktop en Windows/macOS o Docker Engine con el plugin `compose` en Ubuntu) e IntelliJ IDEA. No hace falta instalar Maven ni ningún script: el proyecto trae el Maven Wrapper.

1. **Clona el repositorio** y ábrelo en IntelliJ (*File → Open* → la carpeta del proyecto o `backend/pom.xml`). Si IntelliJ pregunta, carga el proyecto Maven.
2. **Solo la primera vez, para el chat:** copia `IA/.env.example` como `IA/.env` y pon tu `GEMINI_API_KEY` (<https://aistudio.google.com/apikey>). Todo lo demás funciona sin configurar nada.
3. **Con Docker encendido, ejecuta `NutriGuideApplication`** (botón ▶ junto a la clase `backend/src/main/java/com/nutriguide/NutriGuideApplication.java`).
4. Abre **http://localhost:8080**.

Al pulsar Run, Spring Boot:

- levanta con Docker Compose **PostgreSQL, n8n y Qdrant** y espera a que estén listos (la primera vez descarga las imágenes; tarda unos minutos);
- n8n importa y publica solo el flujo `IA/nutriguide-ia.json`;
- lee `backend/.env` e `IA/.env` si existen (no hay que exportar variables);
- crea las tablas, carga los datos iniciales e indexa los documentos en Qdrant;
- sirve el frontend en el mismo puerto 8080.

Al detener la aplicación, los contenedores siguen encendidos para que el siguiente Run sea rápido. Para apagarlos: `docker compose stop` desde la carpeta `backend`.

Desde una terminal, el equivalente es:

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

| Servicio | URL |
|---|---|
| Aplicación (frontend) | http://localhost:8080 |
| API del backend | http://localhost:8080/api/alimentos |
| Editor de n8n | http://localhost:5680 |
| Panel de Qdrant | http://localhost:6333/dashboard |

Detalles: [backend/README.md](backend/README.md) e [IA/README.md](IA/README.md).

## Estructura del repositorio

```
IA2/
├── README.md
├── frontend/                  Cliente web (ver frontend/README.md)
│   ├── index.html · pages/    Páginas
│   ├── css/ · js/             Estilos y módulos ES (api.js centraliza las peticiones)
│   └── tests/                 Pruebas unitarias (node --test)
├── backend/                   API REST Spring Boot (ver backend/README.md)
│   ├── docker-compose.yml     PostgreSQL 16 + include de IA (lo levanta Spring al ejecutar)
│   └── src/main/java/com/nutriguide/
│       ├── config/            CORS, seguridad, carga inicial
│       ├── ia/                Cliente de los webhooks de n8n
│       ├── modules/           alimento · receta · documento · chat
│       └── common/            Errores centralizados y utilidades
└── IA/                        Asistente de IA (ver IA/README.md)
    ├── docker-compose.yml     n8n + Qdrant
    └── nutriguide-ia.json     Flujo de n8n completo (chat, ingesta, eliminación)
```

## Contrato entre frontend y backend

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/alimentos` | Catálogo de alimentos |
| GET | `/api/alimentos/{id}` | Detalle de un alimento |
| GET | `/api/alimentos/buscar?nombre=` | Búsqueda por nombre (sin distinguir tildes) |
| GET | `/api/recetas` · `/api/recetas/{id}` | Recetas |
| GET | `/api/documentos` · `/api/documentos/{id}` | Biblioteca |
| POST | `/api/chat/preguntar` | Pregunta al asistente (el backend la reenvía al flujo n8n) |
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

Los errores tienen la forma `{"exito": false, "mensaje": "…"}`, y el frontend muestra el `mensaje`. El contrato entre el backend y n8n está en [IA/README.md](IA/README.md).

## Equipo

- **Desarrollador 1:** frontend.
- **Desarrollador 2:** backend (Spring Boot), base de datos y sistema RAG.

## Pendiente

- Seguir afinando `RAG_MIN_SIMILARITY` (en `IA/.env`) con más preguntas reales a medida que crezca la biblioteca.
- Sustituir o complementar los documentos iniciales (síntesis redactadas por el equipo) por documentos oficiales de FAO, OMS o ICBF, cargados con `POST /api/documentos`.
- Revisar las fotografías de los alimentos que no tienen una imagen verificada (hoy muestran una ilustración).
