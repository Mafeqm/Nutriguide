/** Biblioteca nutricional: listado de documentos educativos con búsqueda y filtro. */
import { Api } from './api.js';
import { UI } from './main.js';

const { el } = UI;

function openDoc(d) {
  const url = UI.safeUrl(d.url);
  UI.openModal(d.titulo, el('div', { class: 'doc' },
    d.categoria ? el('span', { class: 'badge' }, d.categoria) : null,
    d.descripcion ? el('p', { class: 'lead' }, d.descripcion) : null,
    d.contenido ? el('div', { class: 'doc__text' }, d.contenido) : null,
    url ? el('a', { class: 'btn btn--primary', href: url, target: '_blank', rel: 'noopener noreferrer' }, UI.icon('link', 16), 'Abrir documento') : null,
    !d.contenido && !url ? el('p', { class: 'muted' }, 'Este recurso no incluye contenido ni enlace.') : null));
}

function docCard(d) {
  const canOpen = d.contenido || UI.safeUrl(d.url);
  return el('article', { class: 'card card--doc' },
    el('div', { class: 'card__body' },
      el('span', { class: 'doc-icon' }, UI.icon('book', 24)),
      d.categoria ? el('span', { class: 'badge' }, d.categoria) : null,
      el('h3', { class: 'card__title' }, d.titulo),
      el('p', { class: 'muted clamp' }, d.descripcion || 'Sin descripción.'),
      el('button', { class: 'btn btn--ghost btn--block', type: 'button', disabled: canOpen ? null : true,
        'aria-label': `Consultar ${d.titulo}`, onclick: () => openDoc(d) }, canOpen ? 'Consultar' : 'No disponible')));
}

async function init(grid) {
  const $ = (id) => document.getElementById(id);
  const input = $('doc-search'); const form = $('doc-form'); const chips = $('doc-categories'); const count = $('doc-count');
  const qs = new URLSearchParams(location.search);
  let category = qs.get('cat') || '';
  input.value = qs.get('q') || '';
  let all = [];

  function render() {
    const q = UI.norm(input.value.trim());
    const shown = all.filter((d) => (!category || d.categoria === category) && (!q || UI.norm(`${d.titulo} ${d.descripcion}`).includes(q)));
    count.textContent = `${shown.length} ${shown.length === 1 ? 'recurso' : 'recursos'}`;
    UI.syncUrl({ q: input.value.trim(), cat: category });
    if (!shown.length) return UI.empty(grid, 'Sin resultados', 'Prueba con otras palabras o cambia la categoría.');
    grid.replaceChildren(...shown.map(docCard));
  }

  UI.skeletons(grid, 6);
  try {
    all = await Api.getDocumentos();
    UI.categoryChips(chips, UI.uniqueCategories(all), (c) => { category = c; render(); }, category);
    render();
  } catch (e) { return UI.error(grid, e, () => init(grid)); }

  input.addEventListener('input', render);
  form.addEventListener('submit', (e) => { e.preventDefault(); render(); });
}

const grid = document.getElementById('doc-grid');
if (grid) init(grid);
