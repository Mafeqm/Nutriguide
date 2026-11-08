# NutriGuide IA – Asistente RAG con n8n y Qdrant

Esta carpeta contiene todo el **asistente de inteligencia artificial** de NutriGuide:

- **n8n** ejecuta el flujo RAG: recibe preguntas y documentos, genera embeddings, consulta la base vectorial y redacta la respuesta con Google Gemini.
- **Qdrant** es la **base de datos vectorial**: guarda los fragmentos de los documentos educativos y sus embeddings.

El backend Spring Boot no contiene lógica de IA ni vectores: solo llama a los webhooks de n8n. El frontend no cambia: sigue usando `POST /api/chat/preguntar` del backend.

```
Frontend ──► Backend (Spring Boot + PostgreSQL) ──► n8n (flujo "NutriGuide IA") ──► Qdrant
                         │                                    │
                         │  X-NutriGuide-Token                └──► Google Gemini
                         │                                         (gemini-embedding-001, gemini-3.5-flash-lite)
                         └─ alimentos, recetas, documentos (texto y archivo original)
```

## Contenido

```
IA/
├── nutriguide-ia.json                 EL FLUJO COMPLETO de n8n (chat, ingesta y eliminación)
├── docker-compose.yml                 n8n 2.46.6 + Qdrant 1.20.0
├── .env.example                       Variables (cópialo a .env)
└── n8n/setup/                         Arranque automático (no hace falta tocarlo)
    ├── iniciar-n8n.sh                 Importa credenciales y flujo, lo publica y arranca n8n
    └── credenciales.js                Genera las credenciales desde las variables de entorno
```

## Puesta en marcha

**No hay que arrancarlo a mano.** `backend/docker-compose.yml` incluye este `docker-compose.yml`, y Spring Boot levanta n8n y Qdrant (junto con PostgreSQL) al ejecutar `NutriGuideApplication` desde IntelliJ o con `./mvnw spring-boot:run`. Ver el [README principal](../README.md).

Lo único que hay que preparar, una vez, es la clave de Gemini:

```bash
cp IA/.env.example IA/.env        # en Windows: copy IA\.env.example IA\.env
# Edita IA/.env y pon GEMINI_API_KEY
```

Sin `IA/.env` todo arranca con valores de desarrollo, pero el chat responde que falta la clave.

Si quieres levantar solo la parte de IA (por ejemplo, para trabajar en el flujo): `cd IA && docker compose up -d`.

Al arrancar, el contenedor de n8n **importa las credenciales, importa el flujo y lo publica** sin ningún paso manual. Cada arranque reimporta el flujo desde `nutriguide-ia.json`, así que el repositorio es la fuente de verdad.

| Servicio | URL |
|---|---|
| Editor de n8n | http://localhost:5680 (la primera vez pide crear una cuenta de propietario local) |
| Webhooks | http://localhost:5680/webhook/nutriguide/{chat, ingesta, eliminar} |
| Panel de Qdrant | http://localhost:6333/dashboard (pide `QDRANT_API_KEY`) |

El puerto de n8n es **5680** (y no el 5678 habitual) para no chocar con otras instalaciones de n8n; se cambia con `N8N_PUERTO`. Ambos servicios solo escuchan en `127.0.0.1`.

### Conexión con el backend

El backend lee este mismo `IA/.env` al arrancar y usa su `NUTRIGUIDE_WEBHOOK_TOKEN` (y `N8N_PUERTO`) para llamar a los webhooks, así que el token siempre coincide sin copiarlo a ningún sitio.

## Variables (`IA/.env`)

| Variable | Por defecto | Uso |
|---|---|---|
| `GEMINI_API_KEY` | vacía | Clave de Google Gemini para embeddings y respuestas (**necesaria para el chat**) |
| `GEMINI_MODELO_CHAT` / `GEMINI_MODELO_EMBEDDINGS` | `models/gemini-3.5-flash-lite` / `models/gemini-embedding-001` | Modelos de Gemini |
| `RAG_MIN_SIMILARITY` | `0.55` | Similitud coseno mínima para usar un fragmento |
| `NUTRIGUIDE_WEBHOOK_TOKEN` | valor de desarrollo | Token que el backend envía en `X-NutriGuide-Token`; n8n rechaza (403) cualquier llamada sin él |
| `QDRANT_API_KEY` | valor de desarrollo | Protege la API de Qdrant |
| `N8N_ENCRYPTION_KEY` | valor de desarrollo | Clave con la que n8n cifra sus credenciales. Defínela **antes del primer arranque** y no la cambies después |
| `N8N_PUERTO` / `QDRANT_PUERTO` | `5680` / `6333` | Puertos en el equipo |

Los valores de desarrollo sirven en local. En un servidor compartido, define los tres tokens y claves con valores largos y aleatorios.

Ningún secreto se escribe en el flujo ni en el repositorio: `credenciales.js` crea las credenciales de n8n a partir de estas variables y n8n las guarda cifradas.

## El flujo `NutriGuide IA - Asistente RAG con Qdrant`

Un solo flujo con tres webhooks (todos `POST`, autenticados con la credencial *Header Auth* `X-NutriGuide-Token`).

### 1. Chat – `/webhook/nutriguide/chat`

```
Webhook → Validar pregunta → Buscar en Qdrant (top 4) ← Embeddings Gemini (pregunta)
       → Preparar contexto → ¿Hay contexto? ─ sí → Generar respuesta (LLM Chain ← Modelo de chat Gemini)
                                               │      → Formatear respuesta → Responder
                                               └ no → Responder ("no hay información suficiente")
```

- **Preparar contexto** descarta los fragmentos con similitud menor que `RAG_MIN_SIMILARITY`, neutraliza instrucciones maliciosas que vengan dentro de los documentos (inyección de prompt) y arma el contexto delimitado con título y procedencia de cada fuente.
- Si no hay fragmentos relevantes, **no se llama al modelo** y se responde que no hay información suficiente.
- El modelo recibe las reglas del asistente como mensaje de sistema: responder solo con el contexto, no inventar, citar las fuentes, separar lo no verificado, ignorar órdenes contenidas en los documentos y no diagnosticar ni prescribir dietas. Si el contexto no basta, empieza con `[SIN_INFORMACION]` y el flujo lo convierte en `informacionSuficiente: false`.

Petición y respuesta (el mismo contrato que el frontend ya usaba):

```json
// Petición
{ "pregunta": "¿Qué nutrientes contiene la avena?" }

// Respuesta 200
{
  "respuesta": "Según la Guía educativa sobre cereales integrales…",
  "fuentes": [
    { "titulo": "Guía educativa sobre cereales integrales, avena, quinua y fibra dietética",
      "contenido": "Fragmento recuperado…", "documentoId": 1,
      "categoria": "Cereales y carbohidratos complejos", "fuenteOrigen": "…", "similitud": 0.78 }
  ],
  "informacionSuficiente": true,
  "advertenciaEducativa": "Información con fines educativos…"
}
```

Errores: `400` (pregunta inválida), `502` (fallo de Qdrant o del modelo), `503` (falta `GEMINI_API_KEY`), siempre con `{"exito": false, "mensaje": "…"}`.

### 2. Ingesta – `/webhook/nutriguide/ingesta`

```
Webhook → Preparar documento → ¿Documento válido? → Borrar fragmentos anteriores (API de Qdrant)
       → Recuperar documento → Indexar en Qdrant ← Embeddings Gemini (documentos)
                                                ← Cargar texto y metadatos ← Dividir en fragmentos (900 / 150)
       → Resultado de la ingesta → Responder
```

```json
// Petición (la envía el backend al cargar o reprocesar un documento)
{ "documentoId": 1, "titulo": "…", "categoria": "…", "fuenteOrigen": "…", "contenido": "texto completo" }

// Respuesta 200
{ "documentoId": 1, "fragmentos": 6, "coleccion": "nutriguide_documentos" }
```

Primero se borran los fragmentos anteriores del documento, así que reindexar nunca duplica vectores. Cada punto de Qdrant guarda `content` (el fragmento) y `metadata` (`documentoId`, `titulo`, `categoria`, `fuenteOrigen`).

### 3. Eliminar – `/webhook/nutriguide/eliminar`

```json
{ "documentoId": 1 }   →   { "documentoId": 1, "eliminado": true }
```

Borra de Qdrant los fragmentos del documento (filtro por `metadata.documentoId`). El backend lo llama antes de eliminar un documento.

### Base vectorial (Qdrant)

- Colección: `nutriguide_documentos`, creada automáticamente en la primera ingesta.
- Vectores de **3072 dimensiones** (`gemini-embedding-001`) con distancia **coseno**.

### Modelos

Se configuran en `IA/.env` (`GEMINI_MODELO_CHAT` y `GEMINI_MODELO_EMBEDDINGS`) porque Google retira modelos con el tiempo: por ejemplo, `gemini-2.5-flash` ya no está disponible para claves nuevas. Por defecto:

- **Respuestas:** `models/gemini-3.5-flash-lite`, que contestó en 1–4 s en las pruebas. Los modelos Gemini 3 "flash" completos razonan internamente por defecto y llegaron a tardar más de 2 minutos, demasiado para un chat.
- **Embeddings:** `models/gemini-embedding-001`. Si lo cambias, borra la colección de Qdrant y reindexa, porque los vectores de modelos distintos no son comparables.

Para ver los modelos disponibles para tu clave: `https://generativelanguage.googleapis.com/v1beta/models` (cabecera `x-goog-api-key`).
- Búsqueda semántica por similitud de embeddings, no por coincidencia de palabras.

## Cómo se probó

**Con Google Gemini real** (n8n 2.46.6, Qdrant 1.20.0), a través del backend:

| Pregunta | Resultado | Similitud de las fuentes | Tiempo |
|---|---|---|---|
| ¿Qué nutrientes contiene la avena? | Responde con los valores del documento de cereales y lo cita | 0.72 – 0.81 | 3.7 s |
| ¿Qué alimentos contienen vitamina C? | Combina dos documentos y los cita | 0.65 – 0.72 | 2.0 s |
| ¿Cómo configuro un clúster de Kubernetes? | «Sin información»; no se llama al modelo | ninguna ≥ 0.55 | 0.6 s |
| Tengo diabetes, ¿qué dieta debo seguir? | No diagnostica ni prescribe; remite a un profesional | — | 1.3 s |

Los 5 documentos iniciales se indexaron con 27 fragmentos de 3072 dimensiones.

Antes, con un servidor de Gemini simulado (variable `GEMINI_HOST`), se probaron los casos límite:

- Import automático, credenciales y publicación del flujo al arrancar el contenedor.
- Webhook sin token → 403.
- Ingesta → fragmentos en Qdrant; reindexar reemplaza en vez de duplicar; eliminar deja la colección en 0 para ese documento.
- Chat con contexto → respuesta y fuentes; pregunta fuera de tema → «sin información» sin llamar al modelo; pregunta inválida → 400; colección inexistente → «sin información»; sin clave → 503.
- Recorrido completo frontend → backend → n8n → Qdrant con los 5 documentos iniciales (27 fragmentos).

**Umbral:** con estos documentos, las preguntas pertinentes superaron 0.65 y la pregunta fuera de tema no llegó a 0.55. Si al crecer la biblioteca preguntas válidas salen como «sin información», baja `RAG_MIN_SIMILARITY`; si aparecen fuentes poco relacionadas, súbelo.

## Operación

- **Reindexar todo** (por ejemplo, tras cambiar el modelo de embeddings): borra la colección desde el panel de Qdrant y usa `POST /api/documentos/{id}/reprocesar` del backend para cada documento.
- **Editar el flujo**: puedes modificarlo en el editor de n8n, pero en el siguiente arranque se reimporta el JSON del repositorio. Para conservar los cambios, expórtalo (`Download`) y reemplaza `IA/nutriguide-ia.json`.
- **Registros**: `docker logs nutriguide-n8n`; las ejecuciones de cada webhook se ven en la pestaña *Executions* del flujo en el editor.
- **Apagar los contenedores**: `docker compose stop` desde la carpeta `backend` (apaga también PostgreSQL).
