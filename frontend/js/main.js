/**
 * Utilidades de interfaz compartidas: creación segura de DOM, layout común
 * (header/footer/tema), tarjetas, estados (carga/esqueleto/error/vacío), modal,
 * toasts y estado en la URL.
 * Nunca se usa innerHTML con datos externos: todo texto entra por textContent.
 */
import { Art } from './illustrations.js';
import { Favorites, Theme } from './store.js';
import { parseBlocks } from './richtext.js';

const ROOT = document.body.dataset.root || '';
const PAGES = `${ROOT}pages/`;
const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/** Crea un elemento. Los hijos de tipo texto se insertan como texto plano. */
function el(tag, props = {}, ...children) {
  const n = document.createElement(tag);
  for (const [k, v] of Object.entries(props || {})) {
    if (v === null || v === undefined || v === false) continue;
    if (k === 'class') n.className = v;
    else if (k === 'text') n.textContent = v;
    else if (k.startsWith('on') && typeof v === 'function') n.addEventListener(k.slice(2), v);
    else n.setAttribute(k, v === true ? '' : v);
  }
  for (const c of children.flat()) {
    if (c === null || c === undefined || c === false) continue;
    n.append(c.nodeType ? c : document.createTextNode(String(c)));
  }
  return n;
}

/** Solo permite URLs http(s) o rutas relativas (evita javascript:, data:, etc.). */
function safeUrl(u) {
  if (!u) return '';
  try {
    const url = new URL(u, window.location.href);
    return ['http:', 'https:'].includes(url.protocol) ? url.href : '';
  } catch { return ''; }
}

// ---------- Iconos (SVG propios, contenido estático y de confianza) ----------
const ICONS = {
  leaf: '<path d="M5 19c0-8 5-14 15-14 0 10-6 15-14 15"/><path d="M5 19c3-5 6-8 11-10"/>',
  apple: '<path d="M12 7c-2-2-6-1-7 2-1 4 1 10 4 11 1 .4 2-.4 3-.4s2 .8 3 .4c3-1 5-7 4-11-1-3-5-4-7-2z"/><path d="M12 7c0-2 1-3 3-4"/>',
  chef: '<path d="M6 14a4 4 0 1 1 1-7.9A4 4 0 0 1 12 3a4 4 0 0 1 5 3.1A4 4 0 1 1 18 14v6H6z"/><path d="M6 17h12"/>',
  book: '<path d="M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2z"/><path d="M4 21V5"/><path d="M9 8h6"/>',
  chat: '<path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z"/><path d="M9 11h6M9 14h4"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/>',
  send: '<path d="m4 12 16-8-6 16-3-7z"/>',
  trash: '<path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"/>',
  alert: '<path d="M12 3 2 20h20z"/><path d="M12 10v4M12 17v.5"/>',
  link: '<path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1"/><path d="M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"/>',
  spark: '<path d="M12 3v4M12 17v4M3 12h4M17 12h4M6 6l2.5 2.5M15.5 15.5 18 18M18 6l-2.5 2.5M8.5 15.5 6 18"/>',
  moon: '<path d="M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.5 1.5M17.5 17.5 19 19M19 5l-1.5 1.5M6.5 17.5 5 19"/>',
  heart: '<path d="M12 20s-7-4.6-9-9.3C1.7 7.3 3.6 4 7 4c2 0 3.5 1.1 5 3 1.5-1.9 3-3 5-3 3.4 0 5.3 3.3 4 6.7-2 4.7-9 9.3-9 9.3z"/>',
  copy: '<rect x="9" y="9" width="11" height="11" rx="2"/><path d="M5 15V6a2 2 0 0 1 2-2h9"/>',
  stop: '<rect x="6" y="6" width="12" height="12" rx="2"/>',
  check: '<path d="m5 12 5 5 9-10"/>',
};

function icon(name, size = 20) {
  const s = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  s.setAttribute('viewBox', '0 0 24 24');
  s.setAttribute('width', size);
  s.setAttribute('height', size);
  s.setAttribute('fill', 'none');
  s.setAttribute('stroke', 'currentColor');
  s.setAttribute('stroke-width', '1.8');
  s.setAttribute('stroke-linecap', 'round');
  s.setAttribute('stroke-linejoin', 'round');
  s.setAttribute('aria-hidden', 'true');
  s.innerHTML = ICONS[name] || '';
  return s;
}

// ---------- Layout común ----------
const NAV = [
  ['Inicio', `${ROOT}index.html`, 'home'],
  ['Alimentos', `${PAGES}alimentos.html`, 'alimentos'],
  ['Recetas', `${PAGES}recetas.html`, 'recetas'],
  ['Biblioteca', `${PAGES}biblioteca.html`, 'biblioteca'],
  ['Asistente IA', `${PAGES}asistente.html`, 'asistente'],
];

function logo() {
  return el('a', { class: 'brand', href: `${ROOT}index.html`, 'aria-label': 'NutriGuide, ir al inicio' },
    el('span', { class: 'brand__mark' }, icon('leaf', 22)),
    el('span', { class: 'brand__name' }, 'Nutri', el('b', {}, 'Guide')));
}

function themeToggle() {
  const btn = el('button', { class: 'icon-btn', type: 'button' });
  const paint = () => {
    const dark = Theme.current() === 'dark';
    btn.replaceChildren(icon(dark ? 'sun' : 'moon', 20));
    btn.setAttribute('aria-label', dark ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro');
  };
  btn.addEventListener('click', () => { Theme.toggle(); paint(); });
  paint();
  return btn;
}

function renderLayout() {
  const current = document.body.dataset.page;
  const header = document.getElementById('site-header');
  if (header) {
    const nav = el('nav', { id: 'main-nav', class: 'nav', 'aria-label': 'Principal' },
      NAV.map(([label, href, key]) => el('a', { href, class: 'nav__link', 'aria-current': key === current ? 'page' : null }, label)));
    const toggle = el('button', {
      class: 'nav-toggle', type: 'button', 'aria-label': 'Abrir menú', 'aria-expanded': 'false', 'aria-controls': 'main-nav',
      onclick: () => toggle.setAttribute('aria-expanded', String(nav.classList.toggle('is-open'))),
    }, el('span'), el('span'), el('span'));
    header.className = 'site-header';
    header.append(el('div', { class: 'container site-header__inner' }, logo(), el('div', { class: 'site-header__right' }, nav, themeToggle(), toggle)));
  }
  const footer = document.getElementById('site-footer');
  if (footer) {
    footer.className = 'site-footer';
    footer.append(el('div', { class: 'container site-footer__inner' },
      el('div', {}, logo(), el('p', { class: 'muted' }, 'Plataforma educativa de alimentación apoyada en inteligencia artificial con RAG.')),
      el('div', {}, el('h2', { class: 'footer-title' }, 'Explora'),
        el('ul', { class: 'footer-links' }, NAV.map(([l, h]) => el('li', {}, el('a', { href: h }, l))))),
      el('div', {}, el('h2', { class: 'footer-title' }, 'Aviso'),
        el('p', { class: 'muted' }, 'La información es de carácter educativo y no reemplaza la consulta con un profesional de la salud.'))),
      el('p', { class: 'container footer-copy' }, `© ${new Date().getFullYear()} NutriGuide · Proyecto académico`));
  }
  if (window.NUTRIGUIDE_CONFIG.USE_MOCK_DATA) {
    document.body.prepend(el('div', { class: 'mock-banner', role: 'status' },
      'Modo de prueba: alimentos, recetas y documentos son datos de ejemplo, no provienen del backend.'));
  }
  // Región viva para toasts
  document.body.append(el('div', { id: 'toasts', class: 'toasts', role: 'status', 'aria-live': 'polite' }));
}

// ---------- Formato ----------
function fmt(v, unit = '') {
  if (v === null || v === undefined) return '—';
  const n = Math.round(v * 10) / 10;
  return unit ? `${n} ${unit}` : String(n);
}

/** Quita tildes y pasa a minúsculas para búsquedas locales. */
const norm = (s) => String(s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

function debounce(fn, ms = 350) {
  let t;
  return (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms); };
}

// ---------- Estados ----------
function loading(container, text = 'Cargando…') {
  container.replaceChildren(el('div', { class: 'state', role: 'status' }, el('span', { class: 'spinner' }), el('p', {}, text)));
}

/** Tarjetas "esqueleto" mientras llegan los datos (evita saltos de layout). */
function skeletons(container, n = 4) {
  container.replaceChildren(
    el('p', { class: 'sr-only', role: 'status' }, 'Cargando…'),
    ...Array.from({ length: n }, () => el('div', { class: 'card skeleton', 'aria-hidden': 'true' },
      el('div', { class: 'sk sk--media' }),
      el('div', { class: 'card__body' }, el('div', { class: 'sk sk--line sk--short' }), el('div', { class: 'sk sk--line' }), el('div', { class: 'sk sk--line' })))));
}

function error(container, err, retry) {
  container.replaceChildren(el('div', { class: 'state state--error', role: 'alert' },
    icon('alert', 32), el('h3', {}, 'No pudimos cargar la información'), el('p', {}, err?.message || 'Ocurrió un error inesperado.'),
    retry ? el('button', { class: 'btn btn--primary', type: 'button', onclick: retry }, 'Reintentar') : null));
}

function empty(container, title, text) {
  container.replaceChildren(el('div', { class: 'state' }, icon('search', 32), el('h3', {}, title), text ? el('p', {}, text) : null));
}

function toast(message) {
  const box = document.getElementById('toasts');
  if (!box) return;
  const t = el('div', { class: 'toast' }, message);
  box.append(t);
  setTimeout(() => { t.classList.add('is-out'); setTimeout(() => t.remove(), 300); }, 2600);
}

// ---------- Imagen con respaldo ----------
function media(src, alt, fallbackIcon, cls = 'card__media', hint = '') {
  const wrap = el('div', { class: cls });
  const fallback = () => {
    // Sin foto: se usa una ilustración acorde al nombre/categoría
    const art = Art.pick(hint);
    wrap.style.background = Art.tint(art);
    wrap.replaceChildren(el('span', { class: 'media-fallback', role: 'img', 'aria-label': alt }, Art.svg(art, 96, 'media-art')));
  };
  const url = safeUrl(src);
  if (!url) { fallback(); return wrap; }
  const img = el('img', { src: url, alt, loading: 'lazy', decoding: 'async' });
  img.addEventListener('error', fallback, { once: true });
  wrap.append(img);
  return wrap;
}

// ---------- Favoritos ----------
function favBtn(kind, id, name) {
  const btn = el('button', {
    type: 'button', class: 'fav', 'aria-label': `Favorito: ${name}`,
    'aria-pressed': String(Favorites.has(kind, id)),
    onclick: () => {
      const on = Favorites.toggle(kind, id);
      btn.setAttribute('aria-pressed', String(on));
      toast(on ? `«${name}» añadido a favoritos` : `«${name}» quitado de favoritos`);
      document.dispatchEvent(new CustomEvent('favorites:change', { detail: { kind, id, on } }));
    },
  }, icon('heart', 20));
  return btn;
}

// ---------- Tarjetas ----------
function macroChips(item) {
  return el('ul', { class: 'macros', 'aria-label': 'Información nutricional resumida' },
    [['kcal', item.calorias, ''], ['Prot.', item.proteinas, 'g'], ['Carb.', item.carbohidratos, 'g'], ['Grasas', item.grasas, 'g']]
      .map(([l, v, u]) => el('li', {}, el('strong', {}, fmt(v, u)), el('span', {}, l))));
}

function foodCard(f) {
  return el('article', { class: 'card card--food' },
    media(f.imagen, `Imagen de ${f.nombre}`, 'apple', 'card__media', `${f.nombre} ${f.categoria}`),
    favBtn('alimento', f.id, f.nombre),
    el('div', { class: 'card__body' },
      f.categoria ? el('span', { class: 'badge' }, f.categoria) : null,
      el('h3', { class: 'card__title' }, f.nombre),
      macroChips(f),
      el('p', { class: 'muted small' }, f.porcion ? `Por ${f.porcion} ${f.unidad}`.trim() : 'Porción de referencia no indicada'),
      el('a', { class: 'btn btn--ghost btn--block', href: `${PAGES}detalle-alimento.html?id=${encodeURIComponent(f.id)}`, 'aria-label': `Ver detalle de ${f.nombre}` }, 'Ver detalle')));
}

function recipeCard(r, onOpen) {
  return el('article', { class: 'card card--recipe' },
    media(r.imagen, `Imagen de ${r.nombre}`, 'chef', 'card__media', `${r.nombre} ${r.categoria}`),
    favBtn('receta', r.id, r.nombre),
    el('div', { class: 'card__body' },
      r.categoria ? el('span', { class: 'badge badge--warm' }, r.categoria) : null,
      el('h3', { class: 'card__title' }, r.nombre),
      r.tiempo ? el('p', { class: 'meta' }, icon('clock', 16), r.tiempo) : null,
      r.descripcion ? el('p', { class: 'muted clamp' }, r.descripcion) : null,
      el('button', { class: 'btn btn--ghost btn--block', type: 'button', 'aria-label': `Ver receta de ${r.nombre}`, onclick: () => onOpen(r) }, 'Ver receta')));
}

/** Tabla de nutrientes reutilizada por detalle de alimento y receta. */
function nutritionTable(item) {
  const rows = [['Calorías', item.calorias, 'kcal'], ['Proteínas', item.proteinas, 'g'], ['Carbohidratos', item.carbohidratos, 'g'],
    ['Grasas', item.grasas, 'g'], ['Fibra', item.fibra, 'g']].filter(([, v]) => v !== null);
  item.otros.forEach((o) => rows.push([o.nombre, o.valor, o.unidad]));
  if (!rows.length) return el('p', { class: 'muted' }, 'No hay información nutricional disponible.');
  return el('table', { class: 'nutri-table' },
    el('caption', { class: 'sr-only' }, 'Información nutricional'),
    el('thead', {}, el('tr', {}, el('th', { scope: 'col' }, 'Nutriente'), el('th', { scope: 'col' }, 'Cantidad'))),
    el('tbody', {}, rows.map(([n, v, u]) => el('tr', {}, el('th', { scope: 'row' }, n), el('td', {}, fmt(v, u))))));
}

/** Barra apilada: de qué macronutriente vienen las calorías (factores de Atwater 4-4-9). */
function calorieSplit(item) {
  const { proteinas: p, carbohidratos: c, grasas: g } = item;
  if ([p, c, g].some((v) => v === null)) return null;
  const parts = [['Proteínas', p * 4, 'prot'], ['Carbohidratos', c * 4, 'carb'], ['Grasas', g * 9, 'fat']];
  const total = parts.reduce((s, [, k]) => s + k, 0);
  if (total <= 0) return null;
  const pct = parts.map(([n, k, cls]) => ({ n, cls, v: Math.round((k / total) * 100) }));
  return el('div', { class: 'split' },
    el('h3', {}, 'Origen de las calorías'),
    el('div', { class: 'split__bar', role: 'img', 'aria-label': pct.map((x) => `${x.n} ${x.v}%`).join(', ') },
      pct.filter((x) => x.v > 0).map((x) => el('span', { class: `split__seg split__seg--${x.cls}`, style: `width:${x.v}%` }, `${x.v}%`))),
    el('ul', { class: 'split__legend' }, pct.map((x) => el('li', {}, el('i', { class: `dot dot--${x.cls}` }), `${x.n} ${x.v}%`))),
    el('p', { class: 'muted small' }, 'Estimación a partir de 4 kcal/g (proteínas y carbohidratos) y 9 kcal/g (grasas).'));
}

/** Convierte texto del asistente en DOM seguro (párrafos, listas y negrita). */
function richText(text) {
  const inline = (segs) => segs.map((s) => (s.bold ? el('strong', {}, s.text) : s.text));
  return el('div', { class: 'rich' }, parseBlocks(text).map((b) => {
    if (b.type === 'p') return el('p', {}, b.lines.map((l, i) => [i ? el('br') : null, ...inline(l)]));
    return el(b.type, {}, b.items.map((it) => el('li', {}, inline(it))));
  }));
}

// ---------- Modal accesible (<dialog>) ----------
function openModal(title, content) {
  const opener = document.activeElement;
  const dlg = el('dialog', { class: 'modal', 'aria-labelledby': 'modal-title' },
    el('div', { class: 'modal__head' },
      el('h2', { id: 'modal-title' }, title),
      el('button', { class: 'modal__close', type: 'button', 'aria-label': 'Cerrar', onclick: () => dlg.close() }, '×')),
    el('div', { class: 'modal__body' }, content));
  dlg.addEventListener('close', () => { dlg.remove(); opener?.focus?.(); });
  dlg.addEventListener('click', (e) => { if (e.target === dlg) dlg.close(); });
  document.body.append(dlg);
  dlg.showModal();
}

// ---------- Filtros ----------
/** Chips de categoría (botones de filtro). onSelect recibe '' para "Todas". */
function categoryChips(container, categories, onSelect, selected = '') {
  const make = (label, value) => el('button', {
    type: 'button', class: 'chip', 'aria-pressed': String(value === selected),
    onclick: (e) => {
      container.querySelectorAll('.chip').forEach((c) => c.setAttribute('aria-pressed', 'false'));
      e.currentTarget.setAttribute('aria-pressed', 'true');
      onSelect(value);
    },
  }, label);
  const list = selected && !categories.includes(selected) ? [...categories, selected] : categories;
  container.replaceChildren(make('Todas', ''), ...list.map((c) => make(c, c)));
}
const uniqueCategories = (items) => [...new Set(items.map((i) => i.categoria).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'es'));

/** Refleja el estado de los filtros en la URL (enlaces compartibles, botón "atrás" coherente). */
function syncUrl(params) {
  const qs = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => { if (v) qs.set(k, v); });
  const q = qs.toString();
  history.replaceState(null, '', `${location.pathname}${q ? `?${q}` : ''}`);
}

// ---------- Decoración e interacciones globales ----------
function decorate() {
  document.querySelectorAll('[data-art]').forEach((n) => n.append(Art.svg(n.dataset.art, Number(n.dataset.size) || 64)));
  document.querySelectorAll('.page-head[data-arts]').forEach((h) => {
    h.append(el('div', { class: 'page-head__art', 'aria-hidden': 'true' },
      h.dataset.arts.split(',').map((a, i) => el('span', { class: `pfloat pfloat--${i + 1}` }, Art.svg(a.trim(), 72)))));
  });
}

function initReveal() {
  const items = document.querySelectorAll('.reveal');
  if (!items.length) return;
  if (reduceMotion || !('IntersectionObserver' in window)) { items.forEach((i) => i.classList.add('is-visible')); return; }
  const io = new IntersectionObserver((entries) => {
    entries.forEach((e) => { if (e.isIntersecting) { e.target.classList.add('is-visible'); io.unobserve(e.target); } });
  }, { threshold: 0.12 });
  items.forEach((i) => io.observe(i));
}

function initTilt() {
  if (reduceMotion || !window.matchMedia('(hover: hover) and (pointer: fine)').matches) return;
  const sel = '.card--food, .card--recipe';
  document.addEventListener('pointermove', (e) => {
    const card = e.target.closest?.(sel);
    if (!card) return;
    const r = card.getBoundingClientRect();
    card.style.setProperty('--rx', `${(-((e.clientY - r.top) / r.height - 0.5) * 6).toFixed(2)}deg`);
    card.style.setProperty('--ry', `${(((e.clientX - r.left) / r.width - 0.5) * 8).toFixed(2)}deg`);
  });
  document.addEventListener('pointerout', (e) => {
    const card = e.target.closest?.(sel);
    if (card && !card.contains(e.relatedTarget)) { card.style.removeProperty('--rx'); card.style.removeProperty('--ry'); }
  });
}

renderLayout();
decorate();
initReveal();
initTilt();

export const UI = {
  el, icon, safeUrl, fmt, norm, debounce,
  loading, skeletons, error, empty, toast,
  media, macroChips, foodCard, recipeCard, nutritionTable, calorieSplit, favBtn, richText,
  openModal, categoryChips, uniqueCategories, syncUrl,
};
