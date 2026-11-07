/**
 * Persistencia ligera en el navegador (favoritos, tema, historial del chat).
 * Todo acceso a Web Storage va protegido: puede fallar en modo privado o con
 * el almacenamiento bloqueado; en ese caso se degrada a memoria sin romper la app.
 */
const memory = new Map();
const PREFIX = 'ng:';

const area = (kind) => (kind === 'session' ? window.sessionStorage : window.localStorage);

export const Store = {
  get(key, fallback = null, kind = 'local') {
    try {
      const raw = area(kind).getItem(PREFIX + key);
      return raw === null ? fallback : JSON.parse(raw);
    } catch {
      return memory.has(key) ? memory.get(key) : fallback;
    }
  },
  set(key, value, kind = 'local') {
    memory.set(key, value);
    try { area(kind).setItem(PREFIX + key, JSON.stringify(value)); } catch { /* solo memoria */ }
  },
  remove(key, kind = 'local') {
    memory.delete(key);
    try { area(kind).removeItem(PREFIX + key); } catch { /* nada */ }
  },
};

export const Favorites = {
  list(kind) { return Store.get(`fav:${kind}`, []); },
  has(kind, id) { return this.list(kind).includes(String(id)); },
  /** Alterna y devuelve el nuevo estado (true = es favorito). */
  toggle(kind, id) {
    const list = this.list(kind);
    const key = String(id);
    const i = list.indexOf(key);
    if (i >= 0) list.splice(i, 1); else list.push(key);
    Store.set(`fav:${kind}`, list);
    return i < 0;
  },
};

export const Theme = {
  saved() { return Store.get('theme', null); },
  current() { return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light'; },
  apply(theme) { document.documentElement.dataset.theme = theme; },
  toggle() {
    const next = this.current() === 'dark' ? 'light' : 'dark';
    Store.set('theme', next);
    this.apply(next);
    return next;
  },
};
