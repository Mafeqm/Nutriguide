// Genera (por la salida estándar) las credenciales de n8n que usa el flujo NutriGuide IA,
// a partir de variables de entorno. n8n las cifra con N8N_ENCRYPTION_KEY al importarlas,
// así que ningún secreto queda escrito en el repositorio.
// Los IDs y nombres deben coincidir con los del flujo (n8n/workflows/nutriguide-ia.json).

const env = (nombre, porDefecto = '') => (process.env[nombre] || porDefecto).trim();

const credenciales = [
  {
    id: 'nutriWebhookTok1',
    name: 'NutriGuide - Token de webhooks',
    type: 'httpHeaderAuth',
    data: {
      name: 'X-NutriGuide-Token',
      value: env('NUTRIGUIDE_WEBHOOK_TOKEN'),
    },
  },
  {
    id: 'nutriGeminiKey01',
    name: 'NutriGuide - Google Gemini',
    type: 'googlePalmApi',
    data: {
      host: env('GEMINI_HOST', 'https://generativelanguage.googleapis.com'),
      // n8n no admite un valor vacío; si falta la clave, las llamadas a Gemini fallarán con 400/403
      apiKey: env('GEMINI_API_KEY', 'SIN_CONFIGURAR'),
    },
  },
  {
    id: 'nutriQdrantApi01',
    name: 'NutriGuide - Qdrant',
    type: 'qdrantApi',
    data: {
      qdrantUrl: env('QDRANT_URL', 'http://qdrant:6333'),
      apiKey: env('QDRANT_API_KEY'),
    },
  },
];

process.stdout.write(JSON.stringify(credenciales, null, 2));
