# NutriGuide – Frontend

HTML5 + CSS3 + JavaScript puro (sin frameworks). Consume el backend Spring Boot mediante Fetch API.

## Ejecutar

**Lo normal:** no hace falta servirlo aparte. Al ejecutar el backend (`NutriGuideApplication` en IntelliJ o `./mvnw spring-boot:run`), Spring Boot sirve esta carpeta y la aplicación queda en **http://localhost:8080**.

También puedes servirlo con cualquier servidor estático mientras desarrollas el frontend (Live Server, `python -m http.server 5500`, el servidor integrado de IntelliJ…). El backend acepta por CORS cualquier puerto de `localhost`.

1. La URL del backend está en [js/config.js](js/config.js) (`API_BASE_URL`, por defecto `http://localhost:8080`). Es el único lugar donde aparece.
2. Si el frontend se publica en otro dominio, añádelo a `CORS_ALLOWED_ORIGINS` del backend.

## Estructura

```
frontend/
├── index.html                 Dashboard
├── pages/                     alimentos, detalle-alimento, recetas, biblioteca, asistente
├── css/                       global (tokens + tema oscuro), layout, components, responsive
├── js/                        módulos ES (sin variables globales, salvo config.js)
│   ├── config.js              (script clásico) URL base y ajustes
│   ├── api.js                 TODAS las peticiones HTTP: timeout, cancelación, caché, reintento
│   ├── normalize.js           funciones puras que adaptan el contrato del backend (testeadas)
│   ├── richtext.js            parser seguro de texto del asistente (párrafos, listas, negrita)
│   ├── store.js               favoritos, tema e historial del chat (con respaldo si no hay storage)
│   ├── illustrations.js       ilustraciones SVG de alimentos (imagen de respaldo y decoración)
│   ├── main.js                utilidades UI (DOM seguro, layout, tarjetas, estados, modal, toasts)
│   └── alimentos.js · recetas.js · biblioteca.js · asistente.js · home.js   (un módulo por página)
├── tests/                     pruebas unitarias con node:test
└── assets/                    images (fotografías), icons
```

## Endpoints utilizados

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/alimentos` | Catálogo, destacados y categorías |
| GET | `/api/alimentos/{id}` | Detalle de alimento |
| GET | `/api/alimentos/buscar?nombre=` | Búsqueda por nombre |
| GET | `/api/recetas` | Listado y destacados (búsqueda/filtro en el cliente) |
| GET | `/api/recetas/{id}` | Detalle de receta (al abrir el modal; si falla se usa el dato del listado) |
| GET | `/api/documentos` | Biblioteca (búsqueda/filtro en el cliente) |
| POST | `/api/chat/preguntar` | Chat RAG |

Las listas pueden venir como arreglo o paginadas (`{ "content": [...] }`).

### Chat

Petición: `{ "pregunta": "¿Qué nutrientes contiene la avena?" }`

Respuesta esperada:
```json
{ "respuesta": "La avena contiene…",
  "fuentes": [ { "titulo": "Documento educativo sobre cereales", "contenido": "Fragmento utilizado" } ] }
```
Si `respuesta` viene vacía se muestra un error; si `fuentes` está vacío se indica "Sin fuentes".

### Campos esperados (ajustar con el Desarrollador 2)

`api.js` normaliza estos nombres (acepta también algunas variantes, ver `normNutricion`, `normReceta`, `normDocumento`):

- **Alimento**: `id, nombre, categoria (texto u objeto {nombre}), descripcion, imagenUrl, calorias, proteinas, carbohidratos, grasas, fibra, tamanoPorcion, unidadPorcion, fuente, otrosNutrientes` (arreglo `[{nombre, valor, unidad}]` u objeto `{nombre: valor}`). Los valores nutricionales pueden venir anidados en `informacionNutricional`.
- **Receta**: `id, nombre, categoria, descripcion, imagenUrl, tiempoPreparacion` (minutos; se muestra como «N min»), `ingredientes` (arreglo de textos u objetos `{nombre, cantidad, unidad}`), `preparacion` o `pasos` (arreglo o texto con saltos de línea) y los campos nutricionales, que el backend envía agrupados en `informacionNutricional` (valores por porción servida; `null` si no se conocen).
- **Documento**: `id, titulo, descripcion, categoria, contenido, url`.

Cualquier cambio de contrato se concentra en las funciones de normalización de `api.js`.

## Calidad y arquitectura

- **Módulos ES** (`<script type="module">`): dependencias explícitas con `import`, sin globales. Requieren servir por HTTP (no abrir con `file://`).
- **Capa de API**: caché en memoria de 60 s, deduplicación de peticiones GET en vuelo, un reintento ante fallos transitorios (red, 502/503/504), timeout y cancelación (`AbortController`). El chat no usa caché ni reintentos.
- **Estado en la URL** (`?q=&cat=&orden=&fav=`): búsquedas y filtros compartibles y coherentes con el botón "atrás".
- **Esqueletos de carga** (sin saltos de layout), toasts y estados de error con reintento.
- **Fondo animado**: degradado en malla (blanco, menta, cielo, durazno y lila) con un arcoíris que aparece y se desvanece cada ~20 s, solo con CSS (`body::before` y `body::after` en `global.css`). Con `prefers-reduced-motion` queda estático.
- **Tema claro/oscuro**: sigue al sistema, se puede cambiar desde el encabezado y se recuerda (sin parpadeo al cargar).
- **Favoritos** de alimentos y recetas (guardados en `localStorage`).
- **Pruebas**: `node --test` (Node 18+; sin dependencias) valida la normalización del contrato y el parser de texto.

## Elementos interactivos

- Detalle de alimento: origen de las calorías (estimación 4-4-9), favoritos, copiar enlace y alimentos relacionados.
- Recetas: ingredientes como lista de verificación.
- Chat: respuestas con formato, copiar, detener la consulta e historial de la sesión.
- Héroe con alimentos ilustrados: parallax con el cursor y dato nutricional al tocarlos.
- Plato equilibrado y explorador de nutrientes: contenido educativo orientativo (no viene del backend); enlazan al chat con una pregunta precargada (`asistente.html?q=...`).
- Si un alimento/receta no trae imagen desde la API, se muestra una ilustración acorde a su nombre/categoría.
- Las animaciones respetan `prefers-reduced-motion`.

## Seguridad y accesibilidad

- Los datos externos se insertan con `textContent` (nunca `innerHTML`); las URLs de imagen/enlace solo admiten `http(s)`.
- Enlaces externos con `rel="noopener noreferrer"`, `<dialog>` nativo para modales, `aria-live` en listas y chat, enlace "Saltar al contenido", foco visible y soporte de `prefers-reduced-motion`.

## Créditos de imágenes

Las fotografías de `assets/images/` provienen de [Unsplash](https://unsplash.com) y se usan bajo la [Licencia de Unsplash](https://unsplash.com/license) (uso libre, atribución no obligatoria). Son imágenes decorativas del sitio; las fotos de cada alimento o receta siguen viniendo del backend.
