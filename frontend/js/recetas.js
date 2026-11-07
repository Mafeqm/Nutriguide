/** Recetas: destacadas (inicio) y listado con búsqueda, filtro, favoritos y detalle en modal. */
import { Api } from './api.js';
import { UI } from './main.js';
import { Favorites } from './store.js';

const { el } = UI;

/** Ingredientes como lista de verificación (se tachan al marcarlos). */
function ingredientList(items) {
  if (!items.length) return el('p', { class: 'muted' }, 'Ingredientes no disponibles.');
  return el('ul', { class: 'checklist' }, items.map((txt, i) => {
    const id = `ing-${i}`;
    return el('li', {}, el('input', { type: 'checkbox', id }), el('label', { for: id }, txt));
  }));
}

function recipeDetail(r) {
  return el('div', { class: 'recipe' },
    UI.media(r.imagen, `Imagen de ${r.nombre}`, 'chef', 'recipe__media', `${r.nombre} ${r.categoria}`),
    el('div', { class: 'recipe__meta' },
      r.tiempo ? el('p', { class: 'meta' }, UI.icon('clock', 16), `Tiempo de preparación: ${r.tiempo}`) : null,
      el('span', { class: 'actions__fav' }, UI.favBtn('receta', r.id, r.nombre), el('span', { class: 'small muted' }, 'Favorita'))),
    r.descripcion ? el('p', {}, r.descripcion) : null,
    el('h3', {}, 'Ingredientes'),
    ingredientList(r.ingredientes),
    el('h3', {}, 'Preparación'),
    r.pasos.length ? el('ol', { class: 'steps' }, r.pasos.map((p) => el('li', {}, p))) : el('p', { class: 'muted' }, 'Pasos de preparación no disponibles.'),
    el('h3', {}, 'Información nutricional'),
    UI.nutritionTable(r));
}

// Intenta obtener la receta completa (/api/recetas/{id}); si falla usa los datos del listado.
async function openRecipe(r) {
  let full = r;
  try { full = await Api.getReceta(r.id); } catch { /* se usa la versión del listado */ }
  UI.openModal(full.nombre, recipeDetail(full));
}

async function initFeatured(box) {
  UI.skeletons(box, 3);
  try {
    const list = (await Api.getRecetas()).slice(0, 3);
    if (!list.length) return UI.empty(box, 'Aún no hay recetas', 'Cuando existan recetas aparecerán aquí.');
    box.replaceChildren(...list.map((r) => UI.recipeCard(r, openRecipe)));
  } catch (e) { UI.error(box, e, () => initFeatured(box)); }
}

async function initList(grid) {
  const $ = (id) => document.getElementById(id);
  const input = $('recipe-search'); const form = $('recipe-form'); const chips = $('recipe-categories');
  const count = $('recipe-count'); const favChk = $('recipe-fav');

  const qs = new URLSearchParams(location.search);
  let category = qs.get('cat') || '';
  let onlyFav = qs.get('fav') === '1';
  input.value = qs.get('q') || '';
  favChk.checked = onlyFav;
  let all = [];

  function render() {
    const q = UI.norm(input.value.trim());
    const shown = all.filter((r) => (!category || r.categoria === category)
      && (!onlyFav || Favorites.has('receta', r.id)) && (!q || UI.norm(r.nombre).includes(q)));
    count.textContent = `${shown.length} ${shown.length === 1 ? 'receta' : 'recetas'}`;
    UI.syncUrl({ q: input.value.trim(), cat: category, fav: onlyFav ? '1' : '' });
    if (!shown.length) {
      return UI.empty(grid, onlyFav ? 'Aún no tienes recetas favoritas' : 'Sin resultados',
        onlyFav ? 'Marca recetas con el corazón para verlas aquí.' : 'Prueba con otro nombre o cambia la categoría.');
    }
    grid.replaceChildren(...shown.map((r) => UI.recipeCard(r, openRecipe)));
  }

  UI.skeletons(grid, 6);
  try {
    all = await Api.getRecetas();
    UI.categoryChips(chips, UI.uniqueCategories(all), (c) => { category = c; render(); }, category);
    render();
  } catch (e) { return UI.error(grid, e, () => initList(grid)); }

  input.addEventListener('input', render);
  form.addEventListener('submit', (e) => { e.preventDefault(); render(); });
  favChk.addEventListener('change', () => { onlyFav = favChk.checked; render(); });
  document.addEventListener('favorites:change', () => { if (onlyFav) render(); });
}

const featured = document.getElementById('featured-recipes');
const grid = document.getElementById('recipe-grid');
if (featured) initFeatured(featured);
if (grid) initList(grid);
