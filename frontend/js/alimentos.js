/** Alimentos: destacados (inicio), catálogo con filtros/orden y detalle. */
import { Api } from './api.js';
import { UI } from './main.js';
import { Favorites } from './store.js';

const { el } = UI;

const SORTS = {
  nombre: (a, b) => a.nombre.localeCompare(b.nombre, 'es'),
  'calorias-asc': (a, b) => (a.calorias ?? Infinity) - (b.calorias ?? Infinity),
  'proteinas-desc': (a, b) => (b.proteinas ?? -1) - (a.proteinas ?? -1),
};

// ---------- Destacados en el inicio ----------
async function initFeatured(box) {
  UI.skeletons(box, 4);
  try {
    const list = (await Api.getAlimentos()).slice(0, 4);
    if (!list.length) return UI.empty(box, 'Aún no hay alimentos', 'Cuando el catálogo tenga datos aparecerán aquí.');
    box.replaceChildren(...list.map(UI.foodCard));
  } catch (e) { UI.error(box, e, () => initFeatured(box)); }
}

// ---------- Catálogo ----------
async function initCatalog(grid) {
  const $ = (id) => document.getElementById(id);
  const input = $('food-search'); const form = $('food-form'); const chips = $('food-categories');
  const count = $('food-count'); const hint = $('food-search-error'); const sortSel = $('food-sort'); const favChk = $('food-fav');

  // Estado inicial desde la URL
  const qs = new URLSearchParams(location.search);
  let category = qs.get('cat') || '';
  let sort = SORTS[qs.get('orden')] ? qs.get('orden') : 'nombre';
  let onlyFav = qs.get('fav') === '1';
  input.value = qs.get('q') || '';
  sortSel.value = sort;
  favChk.checked = onlyFav;

  let all = [];
  let current = []; // resultado de la búsqueda, antes de filtrar
  let controller = null;

  function render() {
    let shown = current;
    if (category) shown = shown.filter((f) => f.categoria === category);
    if (onlyFav) shown = shown.filter((f) => Favorites.has('alimento', f.id));
    shown = [...shown].sort(SORTS[sort]);
    count.textContent = `${shown.length} ${shown.length === 1 ? 'alimento' : 'alimentos'}`;
    UI.syncUrl({ q: input.value.trim(), cat: category, orden: sort === 'nombre' ? '' : sort, fav: onlyFav ? '1' : '' });
    if (!shown.length) {
      return UI.empty(grid, onlyFav ? 'Aún no tienes favoritos aquí' : 'Sin resultados',
        onlyFav ? 'Marca alimentos con el corazón para verlos en esta lista.' : 'Prueba con otro nombre o cambia la categoría.');
    }
    grid.replaceChildren(...shown.map(UI.foodCard));
  }

  async function search() {
    const q = input.value.trim();
    hint.textContent = q.length === 1 ? 'Escribe al menos 2 letras para buscar.' : '';
    if (q.length === 1) return;
    controller?.abort();
    if (!q) { current = all; return render(); }
    controller = new AbortController();
    UI.skeletons(grid, 8);
    try {
      current = await Api.buscarAlimentos(q, { signal: controller.signal });
      render();
    } catch (e) {
      if (!e.aborted) UI.error(grid, e, search);
    }
  }

  UI.skeletons(grid, 8);
  try {
    all = await Api.getAlimentos();
    current = all;
    const categorias = UI.uniqueCategories(all);
    // La categoría de la URL se busca sin distinguir mayúsculas ni tildes
    category = categorias.find((c) => UI.norm(c) === UI.norm(category)) || category;
    UI.categoryChips(chips, categorias, (c) => { category = c; render(); }, category);
  } catch (e) { return UI.error(grid, e, () => initCatalog(grid)); }
  await search(); // aplica la búsqueda inicial de la URL (o renderiza todo)

  input.addEventListener('input', UI.debounce(search));
  form.addEventListener('submit', (e) => { e.preventDefault(); search(); });
  sortSel.addEventListener('change', () => { sort = sortSel.value; render(); });
  favChk.addEventListener('change', () => { onlyFav = favChk.checked; render(); });
  // Si se quita un favorito con el filtro activo, la lista se actualiza
  document.addEventListener('favorites:change', () => { if (onlyFav) render(); });
}

// ---------- Detalle ----------
async function initDetail(box) {
  const id = new URLSearchParams(location.search).get('id');
  if (!id) return UI.empty(box, 'Alimento no especificado', 'Vuelve al catálogo y selecciona un alimento.');
  UI.loading(box, 'Cargando alimento…');
  try {
    const f = await Api.getAlimento(id);
    document.title = `${f.nombre} · NutriGuide`;
    const portion = f.porcion ? `${f.porcion} ${f.unidad}`.trim() : 'No indicada';
    const split = UI.calorieSplit(f);
    box.replaceChildren(
      el('div', { class: 'detail' },
        UI.media(f.imagen, `Imagen de ${f.nombre}`, 'apple', 'detail__media', `${f.nombre} ${f.categoria}`),
        el('div', { class: 'detail__info' },
          f.categoria ? el('span', { class: 'badge' }, f.categoria) : null,
          el('h1', {}, f.nombre),
          el('p', { class: 'lead' }, f.descripcion || 'Sin descripción disponible.'),
          el('dl', { class: 'facts' },
            el('div', {}, el('dt', {}, 'Porción de referencia'), el('dd', {}, portion)),
            el('div', {}, el('dt', {}, 'Fuente de la información'), el('dd', {}, f.fuente || 'No disponible'))),
          el('div', { class: 'actions' },
            el('span', { class: 'actions__fav' }, UI.favBtn('alimento', f.id, f.nombre), el('span', { class: 'small muted' }, 'Favorito')),
            el('button', {
              class: 'btn btn--ghost btn--sm', type: 'button',
              onclick: async () => {
                try { await navigator.clipboard.writeText(location.href); UI.toast('Enlace copiado'); } catch { UI.toast('No se pudo copiar el enlace'); }
              },
            }, UI.icon('link', 16), 'Copiar enlace'),
            el('a', { class: 'btn btn--ghost btn--sm', href: `asistente.html?q=${encodeURIComponent(`¿Qué nutrientes contiene ${f.nombre.toLowerCase()}?`)}` }, UI.icon('chat', 16), 'Preguntar a la IA')))),
      el('section', { class: 'panel', 'aria-labelledby': 'nutri-title' },
        el('h2', { id: 'nutri-title' }, 'Información nutricional'),
        UI.macroChips(f),
        split,
        UI.nutritionTable(f)),
      el('section', { id: 'related', class: 'related', hidden: true, 'aria-labelledby': 'related-title' },
        el('h2', { id: 'related-title' }, f.categoria ? `Más de ${f.categoria}` : 'Más alimentos'), el('div', { class: 'grid grid--4' })));
    loadRelated(f);
  } catch (e) {
    if (e.status === 404) UI.empty(box, 'Alimento no encontrado', 'Es posible que ya no exista en el catálogo.');
    else UI.error(box, e, () => initDetail(box));
  }
}

/** Sugerencias de la misma categoría (usa la caché de la lista; si falla, se omite). */
async function loadRelated(food) {
  try {
    const related = (await Api.getAlimentos()).filter((a) => String(a.id) !== String(food.id) && (!food.categoria || a.categoria === food.categoria)).slice(0, 4);
    const sec = document.getElementById('related');
    if (!related.length || !sec) return;
    sec.querySelector('.grid').replaceChildren(...related.map(UI.foodCard));
    sec.hidden = false;
  } catch { /* opcional */ }
}

const featured = document.getElementById('featured-foods');
const grid = document.getElementById('food-grid');
const detail = document.getElementById('food-detail');
if (featured) initFeatured(featured);
if (grid) initCatalog(grid);
if (detail) initDetail(detail);
