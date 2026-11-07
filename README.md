# NutriGuide – Asistente inteligente de alimentación

Plataforma web educativa para consultar alimentos y sus propiedades nutricionales, explorar recetas, acceder a una biblioteca de recursos y hacer preguntas a un asistente de inteligencia artificial que responde con **RAG** (recuperación de información desde una base de datos vectorial) citando sus fuentes.

> **Aviso:** NutriGuide es una herramienta educativa. No diagnostica enfermedades, no prescribe dietas y no reemplaza la orientación de un profesional de la salud.

## Estado actual del proyecto

| Área | Estado |
|---|---|
| Frontend (HTML5 + CSS3 + JavaScript puro) | **Implementado.** Probado visualmente y con datos de ejemplo; **sin probar contra el backend real** (aún no existe). |
| Backend (Java + Spring Boot) | **Pendiente.** Solo existe la estructura de carpetas. |
| Base de datos relacional (PostgreSQL) | **Pendiente.** Solo existe la estructura de carpetas. |
| Base de datos vectorial (Qdrant) | **Pendiente.** Solo existe la estructura de carpetas. |
| Sistema RAG (embeddings + modelo de lenguaje) | **Pendiente.** Solo existe la estructura de carpetas. |
| Auditoría e integración | **Pendiente.** No se han ejecutado pruebas de integración. |

Las carpetas vacías solo definen la estructura acordada; no representan funcionalidad terminada.

## Tecnologías obligatorias

- **Frontend:** HTML5, CSS3 y JavaScript puro (Fetch API). Sin React, Angular, Vue, Bootstrap, Tailwind ni jQuery.
- **Backend:** Java con Spring Boot.
- **Bases de datos:** PostgreSQL (datos relacionales) y Qdrant (base de datos vectorial).
- **Inteligencia artificial:** modelo de embeddings, modelo de lenguaje y arquitectura RAG.

## Arquitectura de carpetas

```
IA2/
├── README.md                     Este documento
│
├── frontend/                     ✅ Cliente web (ver frontend/README.md)
│   ├── index.html · pages/       Páginas
│   ├── css/ · js/                Estilos y módulos JavaScript
│   ├── assets/                   Imágenes e íconos
│   └── tests/                    Pruebas unitarias (node --test)
│
├── backend/                      ⏳ API REST en Spring Boot, organizada por capas
│   └── src/
│       ├── main/
│       │   ├── java/com/nutriguide/
│       │   │   ├── config/        Configuración (CORS, propiedades, beans)
│       │   │   ├── controller/    Capa web: endpoints REST
│       │   │   ├── dto/
│       │   │   │   ├── request/   Objetos de entrada
│       │   │   │   └── response/  Objetos de salida (contrato con el frontend)
│       │   │   ├── entity/        Entidades JPA
│       │   │   ├── exception/     Excepciones propias y manejo global de errores
│       │   │   ├── mapper/        Conversión entidad ↔ DTO
│       │   │   ├── repository/    Acceso a datos relacionales (PostgreSQL)
│       │   │   ├── service/       Lógica de negocio (interfaces)
│       │   │   │   └── impl/      Implementaciones
│       │   │   ├── rag/           Núcleo del sistema RAG
│       │   │   │   ├── ingestion/   Carga de documentos y extracción de texto
│       │   │   │   ├── chunking/    División en fragmentos
│       │   │   │   ├── embedding/   Generación de embeddings
│       │   │   │   ├── retrieval/   Búsqueda semántica en Qdrant
│       │   │   │   ├── prompt/      Construcción del contexto y las instrucciones
│       │   │   │   └── generation/  Llamada al modelo de lenguaje
│       │   │   ├── security/      Validación de archivos y entradas, sanitización
│       │   │   └── util/          Utilidades compartidas
│       │   └── resources/
│       │       ├── db/migration/  Scripts SQL versionados del esquema
│       │       └── prompts/       Plantillas de instrucciones del asistente
│       └── test/java/com/nutriguide/
│           ├── controller/ · service/ · repository/ · rag/
│
├── database/                     ⏳ Recursos de base de datos
│   ├── init/                     Inicialización de PostgreSQL (usuarios, esquema base, etc.)
│   └── seeds/                    Datos iniciales de ejemplo
│
├── data/
│   └── documentos/               ⏳ Documentos educativos fuente para el RAG
│
├── docs/                         ⏳ Documentación técnica
│   ├── arquitectura/ · api/ · base-de-datos/ · rag/
│   └── auditoria/                Informes de auditoría e integración
│
└── scripts/                      ⏳ Scripts de apoyo (arranque, carga de datos, etc.)
```

Leyenda: ✅ existe y tiene contenido · ⏳ solo estructura, sin contenido todavía.

### Responsabilidad de cada capa

1. **controller** recibe las peticiones HTTP, valida la entrada y delega; no contiene lógica de negocio.
2. **service** implementa la lógica de negocio y orquesta repositorios y el módulo RAG.
3. **repository** es la única capa que habla con PostgreSQL. El acceso a Qdrant (indexación y búsqueda de vectores) queda dentro del módulo **rag**.
4. **rag** encapsula el flujo: ingestión → fragmentación → embeddings → recuperación → prompt → generación.
5. **dto / mapper** separan el contrato público de las entidades internas.
6. **exception** y **security** centralizan los errores y las validaciones.

## Contrato acordado entre frontend y backend

El frontend consume estas rutas (aún no implementadas en el backend):

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/alimentos` | Catálogo de alimentos |
| GET | `/api/alimentos/{id}` | Detalle de un alimento |
| GET | `/api/alimentos/buscar?nombre=` | Búsqueda por nombre |
| GET | `/api/recetas` | Listado de recetas |
| GET | `/api/recetas/{id}` | Detalle de una receta |
| GET | `/api/documentos` | Biblioteca de documentos |
| POST | `/api/chat/preguntar` | Pregunta al asistente (RAG) |

Formato del chat:

```json
// Petición
{ "pregunta": "¿Qué nutrientes contiene la avena?" }

// Respuesta
{ "respuesta": "La avena contiene…",
  "fuentes": [ { "titulo": "Documento educativo sobre cereales", "contenido": "Fragmento utilizado" } ] }
```

Los nombres de campo de alimentos, recetas y documentos que el frontend espera están documentados en [frontend/README.md](frontend/README.md). Cualquier cambio de contrato debe coordinarse entre ambos desarrolladores.

## Cómo ejecutar lo que existe hoy

Solo el frontend es ejecutable por ahora:

```bash
cd frontend
python -m http.server 5500      # o: npm start
```

Abre http://localhost:5500. Mientras no exista el backend, puedes poner `USE_MOCK_DATA: true` en `frontend/js/config.js` para ver alimentos, recetas y documentos de ejemplo (el chat nunca se simula). Más detalles en [frontend/README.md](frontend/README.md).

## Equipo y responsabilidades

- **Desarrollador 1:** frontend.
- **Desarrollador 2:** backend (Spring Boot), base de datos y sistema RAG.

## Pendiente

- Implementar el backend, el esquema de PostgreSQL, la colección de Qdrant y el flujo RAG completo.
- Definir y documentar las herramientas de construcción y migraciones del backend.
- Probar la integración frontend ↔ backend y registrar los resultados en `docs/auditoria/`.
