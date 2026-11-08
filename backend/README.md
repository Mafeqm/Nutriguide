# NutriGuide – Backend

API REST en **Java 21 + Spring Boot 3.4** con **PostgreSQL 16**. Gestiona alimentos, recetas y la biblioteca de documentos, y hace de puente con el **asistente de IA**, que vive en la carpeta [`IA/`](../IA/README.md): un flujo de **n8n** con **Qdrant** como base vectorial y Google Gemini.

```
Frontend ──► Backend (este proyecto) ──► n8n (webhooks) ──► Qdrant + Gemini
                    │
                    └─► PostgreSQL: alimentos, recetas, documentos (texto y archivo original)
```

## Requisitos

- JDK 21. No hace falta instalar Maven: el proyecto trae el Maven Wrapper (`mvnw`).
- Docker: Docker Desktop en Windows y macOS, o Docker Engine con el plugin `compose` en Ubuntu (`sudo apt install docker-compose-plugin`).
- IntelliJ IDEA (o cualquier IDE con soporte de Spring Boot / Maven).

## Puesta en marcha (cualquier sistema operativo)

1. Abre el proyecto en IntelliJ y carga el proyecto Maven (`backend/pom.xml`).
2. Para el chat: copia `IA/.env.example` como `IA/.env` y pon tu `GEMINI_API_KEY`.
3. Con Docker encendido, ejecuta **`NutriGuideApplication`**.
4. Abre **http://localhost:8080** (el backend también sirve el frontend).

Desde una terminal: `./mvnw spring-boot:run` (en Windows, `mvnw.cmd spring-boot:run`) dentro de la carpeta `backend`.

No hay scripts de arranque: lo hace Spring Boot.

- **Contenedores:** gracias a `spring-boot-docker-compose`, al ejecutar la aplicación se lanza `docker compose up` sobre `docker-compose.yml`. Ese archivo define PostgreSQL e incluye `../IA/docker-compose.yml` (n8n + Qdrant). Spring espera a que los tres estén sanos antes de continuar. Los contenedores quedan encendidos al detener la aplicación, para que el siguiente arranque sea rápido; se apagan con `docker compose stop`. En las pruebas automáticas esto se desactiva.
- **Configuración:** `spring.config.import` lee `backend/.env` e `IA/.env` si existen. Así el token de los webhooks sale del mismo `IA/.env` que usa n8n y siempre coincide. Sin ningún `.env`, backend y n8n usan los mismos valores de desarrollo.
- **Frontend:** se sirve desde `../frontend` en el mismo puerto.

El directorio de trabajo debe ser la carpeta `backend`, que es el valor por defecto de IntelliJ para este módulo y el de `./mvnw`.

Al arrancar, el backend también:

1. Ejecuta `src/main/resources/db/schema.sql` (tablas, restricciones e índices) y `db/data.sql` (14 alimentos y 5 recetas). Ambos son idempotentes.
2. Registra los 5 documentos educativos de `src/main/resources/initial-docs`.
3. Envía a n8n los documentos pendientes o con error para indexarlos en Qdrant.

**Sin n8n o sin `GEMINI_API_KEY` en `IA/.env`** funcionan el catálogo, las recetas y la biblioteca. El chat responde **503** con un mensaje claro y los documentos quedan **PENDIENTES** o en **ERROR** con el motivo. Se indexan solos en el siguiente arranque, o con `POST /api/documentos/reprocesar-pendientes`. No existe ningún modo que simule la IA.

## Configuración opcional (`backend/.env`)

Copia `backend/.env.example` como `backend/.env` solo si necesitas cambiar algo:

| Variable | Por defecto | Uso |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | `localhost`, `5434`, `nutriguide_db`, `nutriguide`, contraseña de desarrollo | Conexión a PostgreSQL (docker-compose usa los mismos valores) |
| `ADMIN_TOKEN` | sin definir (escrituras deshabilitadas) | Token para crear, modificar o borrar datos y cargar documentos |
| `N8N_URL` | `http://localhost:5680` | URL de n8n, si no es la local |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:[*],http://127.0.0.1:[*]` | Orígenes permitidos si el frontend se sirve aparte (Live Server, etc.) |
| `CHAT_RATE_LIMIT` | `20` | Preguntas por IP y minuto |
| `UPLOAD_DIR` | `./uploads/documentos` | Carpeta de los archivos subidos |

La clave de Gemini y el token de los webhooks se configuran en `IA/.env` (ver [IA/README.md](../IA/README.md)). La contraseña por defecto de la base (`nutriguide_dev_pass`) es solo para el contenedor local.

## Endpoints

Las respuestas de error tienen siempre la forma `{"exito": false, "mensaje": "..."}`.

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/api/alimentos?categoria=` | Lista de alimentos | 200 |
| GET | `/api/alimentos/{id}` | Detalle | 200, 404, 400 |
| GET | `/api/alimentos/buscar?nombre=` | Búsqueda sin distinguir mayúsculas ni tildes | 200 |
| GET | `/api/recetas?categoria=&nombre=` | Lista de recetas | 200 |
| GET | `/api/recetas/{id}` | Detalle | 200, 404 |
| GET | `/api/documentos` | Biblioteca (incluye `contenido`, `url` del archivo, estado y fragmentos indexados) | 200 |
| GET | `/api/documentos/{id}` | Detalle de un documento | 200, 404 |
| GET | `/api/documentos/{id}/archivo` | Archivo original (.pdf/.txt) | 200, 404 |
| POST | `/api/chat/preguntar` | Pregunta al asistente (se reenvía al flujo n8n) | 200, 400, 429, 502, 503 |
| POST 🔒 | `/api/documentos` (multipart, campo `archivo`) | Cargar un .pdf o .txt (máx. 10 MB) e indexarlo en Qdrant | 201, 409, 422 |
| POST 🔒 | `/api/documentos` (JSON) | Registrar un documento desde texto | 201, 400, 409 |
| POST 🔒 | `/api/documentos/{id}/reprocesar` | Volver a indexar un documento | 200, 502, 503 |
| POST 🔒 | `/api/documentos/reprocesar-pendientes` | Indexar los pendientes o con error | 200 |
| DELETE 🔒 | `/api/documentos/{id}` | Borrar documento, archivo y sus fragmentos en Qdrant | 200, 404, 503 |
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
  "fuentes": [ { "titulo": "…", "contenido": "Fragmento recuperado…", "documentoId": 1,
                 "categoria": "…", "fuenteOrigen": "…", "similitud": 0.78 } ],
  "informacionSuficiente": true,
  "advertenciaEducativa": "Información con fines educativos…"
}
```

### Cargar un documento

```bash
curl -H "X-Admin-Token: $ADMIN_TOKEN" \
     -F "archivo=@guia.pdf" -F "titulo=Guía de hidratación" \
     -F "categoria=Hábitos" -F "fuenteOrigen=OMS - Nota descriptiva sobre agua potable" \
     http://localhost:8080/api/documentos
```

El backend valida el archivo, extrae y limpia el texto (PDFBox), lo guarda en PostgreSQL y lo envía al webhook de ingesta de n8n, que lo fragmenta, genera los embeddings y los guarda en Qdrant. La respuesta incluye `estadoProcesamiento` y `totalFragmentos`.

## Base de datos

- **Esquema único:** `src/main/resources/db/schema.sql` (Hibernate solo valida, `ddl-auto=validate`).
- **Tablas:** `alimentos`, `recetas` y `documentos` (texto extraído, hash SHA-256, estado de procesamiento, número de fragmentos indexados).
- **Unidades:** los alimentos se expresan por `porcion_referencia` + `unidad` (100 g en los datos iniciales); las recetas, **por porción servida** (`porcion_gramos`). Un valor nutricional desconocido queda en `NULL`; nunca se inventa.
- **Vectores:** no están en PostgreSQL, sino en Qdrant (ver `IA/README.md`). El esquema elimina la tabla y la extensión de la versión anterior, que usaba pgvector. Por eso el `docker-compose.yml` mantiene la imagen `pgvector/pgvector:pg16` (PostgreSQL 16 oficial con esa librería): permite limpiar sin errores las bases creadas con la versión anterior.

## Seguridad

- Ninguna clave en el código ni en git (`.env` está en `.gitignore`).
- Webhooks de n8n protegidos con un token compartido (`X-NutriGuide-Token`).
- Operaciones de escritura protegidas con `X-Admin-Token` (comparación en tiempo constante).
- CORS limitado a los orígenes configurados.
- Consultas parametrizadas, incluida la búsqueda con comodines escapados.
- Validación de archivos: solo .pdf y .txt, cabecera PDF real, sin PDF cifrados, texto UTF-8 sin binarios, 10 MB máximo, nombres saneados y control de duplicados por SHA-256 y título.
- La API no expone rutas internas del servidor.
- Límite de preguntas por IP para proteger la cuota del proveedor de IA.

## Pruebas

```bash
./mvnw test
```

- **Unitarias:** cliente de n8n (formato de las peticiones, cabecera de token, conversión de respuestas, traducción de errores 400/404/429/502/503 y n8n caído), extracción y validación de documentos, recetas sin valores inventados y búsqueda sin tildes.
- **Integración** (`NutriGuideIntegrationTest`): levanta **PostgreSQL 16 real** con Testcontainers y recorre la API con MockMvc: esquema, datos iniciales, indexación de los documentos a través de n8n, chat, carga de TXT y PDF, duplicados, borrado (que también borra en Qdrant), recuperación tras un fallo de n8n y token de administración.
  n8n se sustituye por un doble HTTP (`FakeN8nServer`) que respeta el mismo contrato. El flujo real de n8n con Qdrant se probó por separado (ver `IA/README.md`). Sin Docker, esta prueba se omite.

## Datos iniciales y procedencia

- **Alimentos:** valores de USDA FoodData Central por 100 g, con el FDC ID en `fuente_informacion`. Las fotos son de Unsplash y solo se usan cuando corresponden al alimento; si no, el campo queda vacío y el frontend muestra una ilustración.
- **Recetas:** valores por porción **estimados** sumando los valores USDA de cada ingrediente; no son análisis de laboratorio.
- **Documentos (`initial-docs`):** son **síntesis educativas redactadas por el equipo** a partir de las referencias citadas (FAO, OMS, ICBF, USDA). No son publicaciones oficiales de esas instituciones, y así se indica en su campo de procedencia.

## Estructura

```
backend/
├── docker-compose.yml          PostgreSQL 16 + include de ../IA (lo levanta Spring al ejecutar)
├── mvnw, mvnw.cmd, .mvn/       Maven Wrapper
├── pom.xml
└── src/
    ├── main/java/com/nutriguide/
    │   ├── NutriGuideApplication.java
    │   ├── config/      CORS, seguridad (token de administración, límite del chat), carga inicial
    │   ├── ia/          IaConfig y N8nClient (webhooks de chat, ingesta y eliminación)
    │   ├── modules/
    │   │   ├── alimento/   entidad, repositorio, servicio, controlador, DTO
    │   │   ├── receta/     ídem
    │   │   ├── documento/  ídem + DocumentProcessor (validación y extracción de PDF/TXT)
    │   │   └── chat/       controlador, servicio, DTO
    │   └── common/      ApiResponse, GlobalExceptionHandler, excepciones, BusquedaTexto
    ├── main/resources/
    │   ├── application.properties
    │   ├── db/schema.sql, db/data.sql
    │   └── initial-docs/   documentos educativos iniciales
    └── test/java/com/nutriguide/   pruebas unitarias e integración (Testcontainers)
```

## Problemas frecuentes

- **`Cannot run program "docker"` o `No Docker Compose file found`:** Docker no está encendido, o el directorio de trabajo no es la carpeta `backend`.
- **503 «no se pudo conectar con el servicio de IA»:** n8n no está corriendo; vuelve a ejecutar la aplicación con Docker encendido.
- **503 «no tiene configurada la clave de Google Gemini»:** completa `GEMINI_API_KEY` en `IA/.env` y vuelve a ejecutar la aplicación (n8n se recrea con la nueva clave).
- **502 «n8n rechazó el token»:** se definió `N8N_WEBHOOK_TOKEN` a mano con un valor distinto de `NUTRIGUIDE_WEBHOOK_TOKEN` de `IA/.env`; quítalo para que se use el de `IA/.env`.
- **`port is already allocated`:** cambia `DB_PORT` (en `backend/.env`) o `N8N_PUERTO` / `QDRANT_PUERTO` (en `IA/.env`).
- **`./mvnw: Permission denied` en Ubuntu/macOS:** `chmod +x mvnw` (el repositorio ya lo marca como ejecutable).
- **Error 403 al cargar documentos:** falta `ADMIN_TOKEN` en el servidor; 401 si el token enviado no coincide.
