/**
 * Interacciones educativas del inicio: parallax del héroe, plato equilibrado,
 * explorador de nutrientes, contadores y cinta de alimentos.
 * El contenido es orientativo y de carácter educativo (no es información del backend).
 */
import { UI } from './main.js';
import { Art } from './illustrations.js';

(() => {
  const { el } = UI;
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const NS = 'http://www.w3.org/2000/svg';
  const askLink = (q) => `pages/asistente.html?q=${encodeURIComponent(q)}`;

  // ---------- Héroe: parallax + datos curiosos al tocar un alimento ----------
  function initHero() {
    const scene = document.getElementById('hero-scene');
    if (!scene) return;
    const floats = [...scene.querySelectorAll('.float')];
    const fact = document.getElementById('hero-fact');
    let timer;

    if (!reduce) {
      const hero = scene.closest('.hero');
      hero.addEventListener('pointermove', (e) => {
        const r = hero.getBoundingClientRect();
        const dx = (e.clientX - r.left) / r.width - 0.5;
        const dy = (e.clientY - r.top) / r.height - 0.5;
        floats.forEach((f) => {
          const d = Number(f.dataset.depth) || 20;
          f.style.setProperty('--px', `${(dx * d).toFixed(1)}px`);
          f.style.setProperty('--py', `${(dy * d).toFixed(1)}px`);
        });
      });
    }

    floats.forEach((f) => f.addEventListener('click', () => {
      f.classList.remove('pop'); void f.offsetWidth; f.classList.add('pop');
      fact.textContent = f.dataset.fact;
      fact.classList.add('is-on');
      clearTimeout(timer);
      timer = setTimeout(() => fact.classList.remove('is-on'), 4500);
    }));
  }

  // ---------- Plato equilibrado interactivo ----------
  const SLICES = [
    { id: 'verduras', label: 'Verduras', pct: 35, color: '#2f9e55', foods: [['broccoli', 'Brócoli'], ['carrot', 'Zanahoria'], ['tomato', 'Tomate']],
      text: 'Aportan fibra, vitaminas y minerales con pocas calorías. Variar los colores amplía la variedad de nutrientes.',
      tip: 'Procura que ocupen cerca de un tercio de tu plato.', q: '¿Qué verduras aportan más fibra y vitaminas?' },
    { id: 'frutas', label: 'Frutas', pct: 15, color: '#ee8a3a', foods: [['apple', 'Manzana'], ['orange', 'Naranja'], ['lemon', 'Limón']],
      text: 'Fuente de vitamina C, potasio y fibra. Enteras suelen ser mejor opción que en jugo porque conservan la fibra.',
      tip: 'Elige frutas de temporada y de distintos colores.', q: '¿Qué alimentos contienen vitamina C?' },
    { id: 'cereales', label: 'Cereales integrales', pct: 25, color: '#e2b23a', foods: [['wheat', 'Avena y trigo'], ['bean', 'Legumbres']],
      text: 'Proveen energía sostenida gracias a sus carbohidratos complejos y a su fibra.',
      tip: 'Prefiere granos enteros frente a harinas refinadas.', q: '¿Qué nutrientes contiene la avena?' },
    { id: 'proteinas', label: 'Proteínas', pct: 25, color: '#d4553f', foods: [['egg', 'Huevo'], ['fish', 'Pescado'], ['drumstick', 'Pollo'], ['bean', 'Lentejas']],
      text: 'Ayudan a construir y reparar tejidos. Conviene alternar fuentes animales y vegetales.',
      tip: 'Combina legumbres, huevo, pescado y carnes magras.', q: '¿Qué alimentos son fuentes de proteínas?' },
  ];

  function initPlate() {
    const chart = document.getElementById('plate-chart');
    if (!chart) return;
    const legend = document.getElementById('plate-legend');
    const panel = document.getElementById('plate-panel');
    const centerPct = document.getElementById('plate-center-pct');
    const centerLabel = document.getElementById('plate-center-label');

    const svg = document.createElementNS(NS, 'svg');
    svg.setAttribute('viewBox', '0 0 42 42');
    svg.setAttribute('class', 'donut');
    svg.setAttribute('aria-hidden', 'true');
    let acc = 0;
    const arcs = SLICES.map((s) => {
      const c = document.createElementNS(NS, 'circle');
      c.setAttribute('cx', 21); c.setAttribute('cy', 21); c.setAttribute('r', '15.9155');
      c.setAttribute('fill', 'none'); c.setAttribute('stroke', s.color); c.setAttribute('stroke-width', '7');
      c.setAttribute('stroke-dasharray', `${s.pct - 0.8} ${100 - s.pct + 0.8}`);
      c.setAttribute('stroke-dashoffset', String(25 - acc));
      c.setAttribute('class', 'donut__arc');
      c.addEventListener('click', () => select(s.id));
      c.addEventListener('pointerenter', () => select(s.id));
      acc += s.pct;
      svg.append(c);
      return c;
    });
    chart.prepend(svg);

    const buttons = SLICES.map((s) => el('button', { type: 'button', class: 'legend__btn', 'aria-pressed': 'false', onclick: () => select(s.id) },
      el('span', { class: 'legend__dot', style: `background:${s.color}` }), el('span', {}, s.label), el('strong', {}, `${s.pct}%`)));
    legend.replaceChildren(...buttons);

    function select(id) {
      const i = SLICES.findIndex((s) => s.id === id);
      const s = SLICES[i];
      arcs.forEach((a, k) => a.classList.toggle('is-active', k === i));
      buttons.forEach((b, k) => b.setAttribute('aria-pressed', String(k === i)));
      centerPct.textContent = `${s.pct}%`;
      centerLabel.textContent = s.label;
      panel.style.setProperty('--accent', s.color);
      panel.replaceChildren(
        el('h3', {}, s.label),
        el('p', {}, s.text),
        el('div', { class: 'art-row' }, s.foods.map(([a, n]) => el('figure', { class: 'art-tile', style: `background:${Art.tint(a)}` }, Art.svg(a, 54), el('figcaption', {}, n)))),
        el('p', { class: 'tip' }, el('b', {}, 'Consejo: '), s.tip),
        el('a', { class: 'btn btn--ghost btn--sm', href: askLink(s.q) }, UI.icon('chat', 16), 'Preguntar a la IA'));
    }
    select('verduras');
  }

  // ---------- Explorador de nutrientes (pestañas) ----------
  const NUTRIENTS = [
    { id: 'carbs', label: 'Carbohidratos', kcal: 4, color: '#e2b23a', foods: [['wheat', 'Avena'], ['apple', 'Frutas'], ['bean', 'Legumbres']],
      text: 'Son la principal fuente de energía del cuerpo, sobre todo para el cerebro y los músculos.', q: '¿Qué alimentos son ricos en carbohidratos?' },
    { id: 'prot', label: 'Proteínas', kcal: 4, color: '#d4553f', foods: [['egg', 'Huevo'], ['fish', 'Pescado'], ['milk', 'Lácteos']],
      text: 'Forman y reparan músculos, piel y defensas. También intervienen en enzimas y hormonas.', q: '¿Cuál es la diferencia entre carbohidratos y proteínas?' },
    { id: 'fat', label: 'Grasas', kcal: 9, color: '#9cae35', foods: [['avocado', 'Aguacate'], ['fish', 'Pescado azul'], ['egg', 'Huevo']],
      text: 'Son una reserva de energía concentrada y ayudan a absorber las vitaminas A, D, E y K.', q: '¿Qué grasas son saludables?' },
    { id: 'vit', label: 'Vitaminas y minerales', kcal: 0, color: '#2f9e55', foods: [['orange', 'Naranja'], ['broccoli', 'Brócoli'], ['carrot', 'Zanahoria']],
      text: 'No aportan energía, pero regulan funciones esenciales: defensas, huesos, visión y coagulación.', q: '¿Qué alimentos contienen vitamina C?' },
  ];

  function initNutrients() {
    const tabs = document.getElementById('nutri-tabs');
    if (!tabs) return;
    const panel = document.getElementById('nutri-panel');
    const btns = NUTRIENTS.map((n) => el('button', { type: 'button', role: 'tab', id: `tab-${n.id}`, 'aria-controls': 'nutri-panel', 'aria-selected': 'false', tabindex: '-1', class: 'tab' }, n.label));
    tabs.replaceChildren(...btns);

    function show(i, focus) {
      const n = NUTRIENTS[i];
      btns.forEach((b, k) => { b.setAttribute('aria-selected', String(k === i)); b.tabIndex = k === i ? 0 : -1; });
      if (focus) btns[i].focus();
      panel.setAttribute('aria-labelledby', `tab-${n.id}`);
      panel.style.setProperty('--accent', n.color);
      panel.replaceChildren(
        el('div', { class: 'nutri__main' },
          el('h3', {}, n.label), el('p', {}, n.text),
          el('div', { class: 'meter', role: 'img', 'aria-label': `${n.kcal} kilocalorías por gramo` },
            el('div', { class: 'meter__label' }, el('span', {}, 'Energía por gramo'), el('strong', {}, `${n.kcal} kcal`)),
            el('div', { class: 'meter__track' }, el('div', { class: 'meter__fill', style: '--w:0%' }))),
          el('a', { class: 'btn btn--primary btn--sm', href: askLink(n.q) }, UI.icon('chat', 16), 'Preguntar a la IA')),
        el('div', { class: 'art-row art-row--big' }, n.foods.map(([a, l]) => el('figure', { class: 'art-tile', style: `background:${Art.tint(a)}` }, Art.svg(a, 72), el('figcaption', {}, l)))));
      requestAnimationFrame(() => requestAnimationFrame(() => panel.querySelector('.meter__fill').style.setProperty('--w', `${(n.kcal / 9) * 100}%`)));
    }
    btns.forEach((b, i) => b.addEventListener('click', () => show(i)));
    tabs.addEventListener('keydown', (e) => {
      const cur = btns.findIndex((b) => b.getAttribute('aria-selected') === 'true');
      const map = { ArrowRight: cur + 1, ArrowLeft: cur - 1, Home: 0, End: btns.length - 1 };
      if (e.key in map) { e.preventDefault(); show((map[e.key] + btns.length) % btns.length, true); }
    });
    show(0);
  }

  // ---------- Contadores animados ----------
  function initCounters() {
    const nodes = document.querySelectorAll('[data-count]');
    if (!nodes.length) return;
    const run = (n) => {
      const end = Number(n.dataset.count);
      if (reduce) { n.textContent = end; return; }
      const t0 = performance.now();
      const step = (t) => {
        const p = Math.min((t - t0) / 1200, 1);
        n.textContent = Math.round(end * (1 - Math.pow(1 - p, 3)));
        if (p < 1) requestAnimationFrame(step);
      };
      requestAnimationFrame(step);
    };
    const io = new IntersectionObserver((es) => es.forEach((e) => { if (e.isIntersecting) { run(e.target); io.unobserve(e.target); } }), { threshold: 0.6 });
    nodes.forEach((n) => io.observe(n));
  }

  // ---------- Cinta de alimentos ----------
  function initMarquee() {
    const track = document.getElementById('marquee-track');
    if (!track) return;
    const items = [['apple', 'Frutas'], ['broccoli', 'Verduras'], ['wheat', 'Cereales'], ['bean', 'Legumbres'], ['egg', 'Huevos'], ['fish', 'Pescados'], ['milk', 'Lácteos'], ['avocado', 'Grasas saludables'], ['drop', 'Agua']];
    const make = () => items.map(([a, l]) => el('li', { class: 'marquee__item' }, Art.svg(a, 40), el('span', {}, l)));
    track.append(...make(), ...make().map((n) => { n.setAttribute('aria-hidden', 'true'); return n; }));
  }

  initHero(); initPlate(); initNutrients(); initCounters(); initMarquee();
})();
