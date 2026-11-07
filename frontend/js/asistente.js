/**
 * NutriGuide AI: chat conectado al endpoint real POST /api/chat/preguntar.
 * El historial se guarda en sessionStorage (se pierde al cerrar la pestaña).
 */
import { Api } from './api.js';
import { UI } from './main.js';
import { Store } from './store.js';

const { el } = UI;
const MAX = window.NUTRIGUIDE_CONFIG.CHAT_MAX_LENGTH;
const HISTORY_KEY = 'chat';
const HISTORY_LIMIT = 40;

const $ = (id) => document.getElementById(id);
const log = $('chat-log');
const form = $('chat-form');
const input = $('chat-input');
const send = $('chat-send');
const stop = $('chat-stop');
const clear = $('chat-clear');
const hint = $('chat-hint');
const counter = $('chat-counter');
const suggestions = $('chat-suggestions');

let busy = false;
let controller = null;
let messages = Store.get(HISTORY_KEY, [], 'session'); // [{role:'user'|'bot', text, fuentes?}]

const scrollDown = () => { log.scrollTop = log.scrollHeight; };
const persist = () => Store.set(HISTORY_KEY, messages.slice(-HISTORY_LIMIT), 'session');
const avatar = (name = 'spark') => el('span', { class: 'msg__avatar' }, UI.icon(name, 18));

function sourcesBlock(fuentes) {
  if (!fuentes.length) return el('p', { class: 'msg__nosrc' }, 'Sin fuentes recuperadas para esta respuesta.');
  return el('details', { class: 'sources' },
    el('summary', {}, `Fuentes utilizadas (${fuentes.length})`),
    el('ul', {}, fuentes.map((f) => el('li', {}, el('strong', {}, f.titulo), f.contenido ? el('blockquote', {}, f.contenido) : null))));
}

function copyButton(text) {
  return el('button', {
    class: 'msg__action', type: 'button', 'aria-label': 'Copiar respuesta',
    onclick: async () => {
      try { await navigator.clipboard.writeText(text); UI.toast('Respuesta copiada'); } catch { UI.toast('No se pudo copiar'); }
    },
  }, UI.icon('copy', 15), 'Copiar');
}

function welcome() {
  log.replaceChildren(el('div', { class: 'msg msg--bot' }, avatar(),
    el('div', { class: 'msg__bubble' }, '¡Hola! Soy NutriGuide AI. Pregúntame sobre alimentos, nutrientes o ideas de recetas y responderé con base en los documentos de la plataforma.')));
}

function renderUser(text) {
  log.append(el('div', { class: 'msg msg--user' }, el('div', { class: 'msg__bubble' }, text)));
}
function renderBot(res) {
  log.append(el('div', { class: 'msg msg--bot' }, avatar(),
    el('div', { class: 'msg__bubble' }, UI.richText(res.respuesta), sourcesBlock(res.fuentes || []),
      el('div', { class: 'msg__actions' }, copyButton(res.respuesta)))));
}
function renderError(err, question) {
  const node = el('div', { class: 'msg msg--bot msg--error', role: 'alert' }, avatar('alert'),
    el('div', { class: 'msg__bubble' },
      el('p', {}, err.message || 'No se pudo obtener respuesta.'),
      el('button', { class: 'btn btn--ghost btn--sm', type: 'button', onclick: () => { node.remove(); ask(question, false); } }, 'Reintentar')));
  log.append(node);
}
function renderTyping() {
  const n = el('div', { class: 'msg msg--bot', role: 'status', 'aria-label': 'El asistente está escribiendo' }, avatar(),
    el('div', { class: 'msg__bubble' }, el('span', { class: 'dots' }, el('i'), el('i'), el('i'))));
  log.append(n);
  return n;
}

function restore() {
  welcome();
  messages.forEach((m) => (m.role === 'user' ? renderUser(m.text) : renderBot({ respuesta: m.text, fuentes: m.fuentes })));
  scrollDown();
}

function setBusy(v) {
  busy = v;
  send.hidden = v;
  stop.hidden = !v;
  input.disabled = v;
  log.setAttribute('aria-busy', String(v));
  if (v) stop.focus(); else input.focus();
}

async function ask(question, record = true) {
  if (busy) return;
  if (record) { renderUser(question); messages.push({ role: 'user', text: question }); persist(); }
  scrollDown();
  setBusy(true);
  controller = new AbortController();
  const typing = renderTyping();
  scrollDown();
  try {
    const res = await Api.preguntar(question, { signal: controller.signal });
    typing.remove();
    renderBot(res);
    messages.push({ role: 'bot', text: res.respuesta, fuentes: res.fuentes });
    persist();
  } catch (e) {
    typing.remove();
    if (e.aborted) UI.toast('Consulta cancelada');
    else renderError(e, question);
  } finally {
    controller = null;
    setBusy(false);
    scrollDown();
  }
}

function updateCounter() { counter.textContent = `${input.value.length}/${MAX}`; }
function autosize() { input.style.height = 'auto'; input.style.height = `${Math.min(input.scrollHeight, 160)}px`; }

input.maxLength = MAX;
send.append(UI.icon('send', 20));
stop.append(UI.icon('stop', 18));

form.addEventListener('submit', (e) => {
  e.preventDefault();
  const q = input.value.trim();
  if (q.length < 3) {
    hint.textContent = 'Escribe una pregunta de al menos 3 caracteres.';
    input.setAttribute('aria-invalid', 'true');
    input.focus();
    return;
  }
  hint.textContent = '';
  input.removeAttribute('aria-invalid');
  input.value = '';
  updateCounter();
  autosize();
  ask(q);
});
input.addEventListener('input', () => {
  updateCounter();
  autosize();
  if (hint.textContent) { hint.textContent = ''; input.removeAttribute('aria-invalid'); }
});
input.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) { e.preventDefault(); form.requestSubmit(); }
});
stop.addEventListener('click', () => controller?.abort());
clear.addEventListener('click', () => {
  if (busy) return;
  messages = [];
  Store.remove(HISTORY_KEY, 'session');
  welcome();
  input.focus();
});
suggestions.addEventListener('click', (e) => {
  const b = e.target.closest('button[data-q]');
  if (b && !busy) ask(b.dataset.q);
});

restore();
updateCounter();

// Permite llegar desde otras páginas con una pregunta: asistente.html?q=...
const preset = (new URLSearchParams(location.search).get('q') || '').trim().slice(0, MAX);
if (preset.length >= 3) {
  window.history.replaceState?.(null, '', location.pathname); // evita repetir la consulta al recargar
  ask(preset);
}
