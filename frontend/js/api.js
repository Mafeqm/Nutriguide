/**
 * Capa de acceso al backend. TODAS las peticiones HTTP pasan por aquí.
 *  - Timeout y cancelación con AbortController.
 *  - Caché en memoria (TTL corto) y deduplicación de GET en vuelo.
 *  - Un reintento automático ante fallos transitorios (red o 502/503/504) en GET.
 *  - Normalización de respuestas (ver normalize.js).
 */
import { toList, normAlimento, normReceta, normDocumento, normChat } from './normalize.js';

const cfg = window.NUTRIGUIDE_CONFIG;
const CACHE_TTL_MS = 60_000;

export class ApiError extends Error {
  constructor(message, status = 0, { aborted = false } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.aborted = aborted;
  }
}

const HTTP_MESSAGES = {
  400: 'La solicitud no es válida.',
  404: 'No se encontró el recurso solicitado.',
  500: 'Error interno del servidor. Inténtalo más tarde.',
  503: 'El servicio no está disponible en este momento.',
};
const TRANSIENT = new Set([0, 502, 503, 504]);
const wait = (ms) => new Promise((r) => setTimeout(r, ms));

async function send(path, { method = 'GET', body, timeout = cfg.REQUEST_TIMEOUT_MS, signal } = {}) {
  if (signal?.aborted) throw new ApiError('Solicitud cancelada.', 0, { aborted: true });
  const controller = new AbortController();
  const onAbort = () => controller.abort();
  signal?.addEventListener('abort', onAbort, { once: true });
  const timer = setTimeout(() => controller.abort(), timeout);
  let res;
  try {
    res = await fetch(cfg.API_BASE_URL.replace(/\/+$/, '') + path, {
      method,
      headers: { Accept: 'application/json', ...(body ? { 'Content-Type': 'application/json' } : {}) },
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });
  } catch (err) {
    if (signal?.aborted) throw new ApiError('Solicitud cancelada.', 0, { aborted: true });
    if (err.name === 'AbortError') throw new ApiError('El servidor tardó demasiado en responder.');
    throw new ApiError('No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.');
  } finally {
    clearTimeout(timer);
    signal?.removeEventListener('abort', onAbort);
  }

  if (!res.ok) {
    let detail = '';
    try {
      const j = await res.json();
      detail = j.mensaje || j.message || j.error || '';
    } catch { /* sin cuerpo JSON */ }
    throw new ApiError(detail || HTTP_MESSAGES[res.status] || `Error del servidor (${res.status}).`, res.status);
  }
  if (res.status === 204) return null;
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    throw new ApiError('El servidor devolvió una respuesta con formato inválido.', res.status);
  }
}

async function withRetry(fn) {
  try {
    return await fn();
  } catch (err) {
    if (err instanceof ApiError && !err.aborted && TRANSIENT.has(err.status)) {
      await wait(400);
      return fn();
    }
    throw err;
  }
}

const cache = new Map();
const inflight = new Map();

/** GET con caché + deduplicación. Las peticiones con `signal` propio no se comparten. */
function get(path, opts = {}) {
  const hit = cache.get(path);
  if (hit && Date.now() - hit.t < CACHE_TTL_MS) return Promise.resolve(hit.data);
  const shared = !opts.signal;
  if (shared && inflight.has(path)) return inflight.get(path);
  const p = withRetry(() => send(path, opts))
    .then((data) => { cache.set(path, { t: Date.now(), data }); return data; })
    .finally(() => { if (shared) inflight.delete(path); });
  if (shared) inflight.set(path, p);
  return p;
}

const notFound = () => new ApiError(HTTP_MESSAGES[404], 404);

export const Api = {
  async getAlimentos(opts) {
    return toList(await get('/api/alimentos', opts)).map(normAlimento);
  },
  async getAlimento(id, opts) {
    const data = await get(`/api/alimentos/${encodeURIComponent(id)}`, opts);
    if (!data) throw notFound();
    return normAlimento(data);
  },
  async buscarAlimentos(nombre, opts) {
    return toList(await get(`/api/alimentos/buscar?nombre=${encodeURIComponent(nombre)}`, opts)).map(normAlimento);
  },

  async getRecetas(opts) {
    return toList(await get('/api/recetas', opts)).map(normReceta);
  },
  async getReceta(id, opts) {
    const data = await get(`/api/recetas/${encodeURIComponent(id)}`, opts);
    if (!data) throw notFound();
    return normReceta(data);
  },

  async getDocumentos(opts) {
    return toList(await get('/api/documentos', opts)).map(normDocumento);
  },

  /** Chat RAG real. Nunca usa datos simulados ni caché, y no se reintenta (no es idempotente). */
  async preguntar(pregunta, { signal } = {}) {
    const data = await send('/api/chat/preguntar', {
      method: 'POST', body: { pregunta }, timeout: cfg.CHAT_TIMEOUT_MS, signal,
    });
    try {
      return normChat(data);
    } catch {
      throw new ApiError('El asistente no devolvió una respuesta. Intenta reformular tu pregunta.');
    }
  },
};
