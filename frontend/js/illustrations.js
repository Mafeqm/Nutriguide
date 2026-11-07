/**
 * Ilustraciones de alimentos (SVG propios, planos y coloridos).
 * Se inyectan como un sprite y se usan con <use>. Sirven como imagen de respaldo
 * cuando un alimento/receta no trae foto desde el backend, y como decoración.
 * Todo el contenido es estático y de confianza.
 */
export const Art = (() => {
  const S = {
    apple: `<path d="M32 21C26 14 14 17 14 31c0 13 8 25 15 25 2 0 2-1 3-1s1 1 3 1c7 0 15-12 15-25 0-14-12-17-18-10z" fill="#e5483f"/><path d="M20 30c0-6 3-10 7-11" stroke="#fff" stroke-opacity=".45" stroke-width="3" stroke-linecap="round" fill="none"/><path d="M32 21c0-6 2-10 6-13" stroke="#6b4423" stroke-width="3" stroke-linecap="round" fill="none"/><path d="M37 13c4-6 11-5 13-3-2 6-9 8-13 3z" fill="#5fb447"/>`,
    orange: `<circle cx="32" cy="36" r="21" fill="#f59a23"/><path d="M19 30c2-6 6-9 11-10" stroke="#fff" stroke-opacity=".5" stroke-width="3" stroke-linecap="round" fill="none"/><path d="M32 15c1-4 4-7 8-7 0 5-3 8-8 7z" fill="#4fa83d"/><circle cx="32" cy="16" r="2" fill="#c9701a"/>`,
    carrot: `<path d="M43 21c6 6-22 36-34 33-3-12 28-39 34-33z" fill="#f2812b"/><path d="M24 36l5 3M30 28l5 3M18 44l4 3" stroke="#d2661a" stroke-width="2.5" stroke-linecap="round"/><path d="M43 21c-1-8 4-13 9-14M43 21c5-6 12-6 15-3M43 21c8 0 13 5 14 9" stroke="#4fa83d" stroke-width="4" stroke-linecap="round" fill="none"/>`,
    broccoli: `<path d="M26 38l-3 18h18l-3-18z" fill="#9bd47a"/><circle cx="19" cy="31" r="10" fill="#3e9a42"/><circle cx="45" cy="31" r="10" fill="#3e9a42"/><circle cx="32" cy="22" r="13" fill="#4cae4d"/><circle cx="32" cy="35" r="11" fill="#3e9a42"/><circle cx="27" cy="19" r="3" fill="#7bd06f" opacity=".7"/><circle cx="38" cy="25" r="2.5" fill="#7bd06f" opacity=".7"/>`,
    avocado: `<path d="M32 5c10 0 14 15 18 27 4 13-4 26-18 26S10 45 14 32C18 20 22 5 32 5z" fill="#4a8c37"/><path d="M32 12c7 0 10 12 13 22 3 10-3 19-13 19S16 44 19 34c3-10 6-22 13-22z" fill="#c4e381"/><circle cx="32" cy="42" r="9" fill="#8a5a34"/><circle cx="29" cy="39" r="2.5" fill="#fff" opacity=".35"/>`,
    tomato: `<circle cx="32" cy="37" r="21" fill="#e8392f"/><path d="M20 30c2-5 5-8 10-9" stroke="#fff" stroke-opacity=".45" stroke-width="3" stroke-linecap="round" fill="none"/><path d="M32 22l-5-9 4 5 1-8 3 8 6-5-4 8 8 1-9 3z" fill="#3e9a42"/>`,
    egg: `<path d="M11 34C9 20 23 11 34 14c14-3 22 8 18 20 6 10-6 21-18 18C22 58 7 48 11 34z" fill="#fff" stroke="#e8e2d0" stroke-width="2"/><circle cx="32" cy="34" r="11" fill="#ffb92e"/><circle cx="28" cy="30" r="3.5" fill="#fff" opacity=".5"/>`,
    wheat: `<path d="M32 60V12" stroke="#c49a1f" stroke-width="3" stroke-linecap="round"/><g fill="#e8bd3c"><ellipse cx="22" cy="46" rx="5" ry="9" transform="rotate(-35 22 46)"/><ellipse cx="42" cy="46" rx="5" ry="9" transform="rotate(35 42 46)"/><ellipse cx="22" cy="33" rx="5" ry="9" transform="rotate(-35 22 33)"/><ellipse cx="42" cy="33" rx="5" ry="9" transform="rotate(35 42 33)"/><ellipse cx="23" cy="20" rx="4.5" ry="8" transform="rotate(-35 23 20)"/><ellipse cx="41" cy="20" rx="4.5" ry="8" transform="rotate(35 41 20)"/><ellipse cx="32" cy="10" rx="4.5" ry="8"/></g>`,
    drop: `<path d="M32 5C27 14 13 28 13 41a19 19 0 0 0 38 0C51 28 37 14 32 5z" fill="#3aa0e8"/><path d="M22 40c0 6 3 10 8 12" stroke="#fff" stroke-opacity=".55" stroke-width="3.5" stroke-linecap="round" fill="none"/>`,
    bean: `<g transform="rotate(-25 32 32)"><path d="M12 28c0-10 14-14 26-10 12 4 16 14 10 22-5 7-16 8-26 4-6-3-10-8-10-16z" fill="#a4482f"/><path d="M20 26c4-4 11-5 17-2" stroke="#fff" stroke-opacity=".4" stroke-width="3" stroke-linecap="round" fill="none"/></g><ellipse cx="44" cy="48" rx="10" ry="7" fill="#c9653f" transform="rotate(20 44 48)"/><ellipse cx="18" cy="52" rx="8" ry="5.5" fill="#d9754a" transform="rotate(-10 18 52)"/>`,
    lemon: `<path d="M8 36c0-3 3-4 5-4 3-10 14-15 25-13 9 2 14 10 12 18 2 1 4 2 4 4s-3 3-5 3c-4 8-14 12-24 9-9-3-14-8-14-17z" fill="#f7d94c"/><path d="M20 28c3-5 8-7 13-7" stroke="#fff" stroke-opacity=".55" stroke-width="3" stroke-linecap="round" fill="none"/><path d="M46 20c3-6 9-8 12-6-2 5-6 8-12 6z" fill="#5fb447"/>`,
    fish: `<path d="M6 32c8-12 24-16 34-8l12-9v34l-12-9c-10 8-26 4-34-8z" fill="#5ab2d6"/><path d="M40 24l12-9v34l-12-9" fill="#3b8fb5"/><circle cx="18" cy="29" r="3" fill="#fff"/><circle cx="18" cy="29" r="1.4" fill="#123"/><path d="M26 22c3 6 3 14 0 20" stroke="#fff" stroke-opacity=".45" stroke-width="2.5" stroke-linecap="round" fill="none"/>`,
    milk: `<path d="M20 12l8-6h8l8 6v46H20z" fill="#fff" stroke="#d7e3ec" stroke-width="2"/><path d="M20 12h24" stroke="#d7e3ec" stroke-width="2"/><rect x="20" y="28" width="24" height="16" fill="#4aa3e0"/><path d="M27 34h10M27 38h7" stroke="#fff" stroke-width="2.5" stroke-linecap="round"/>`,
    drumstick: `<path d="M44 44l10 10M50 40a4 4 0 1 1 4 4M40 50a4 4 0 1 1 4 4" stroke="#f1e6d0" stroke-width="5" stroke-linecap="round" fill="none"/><path d="M12 30c-2-12 8-22 20-21 11 1 18 10 14 21-4 10-15 16-24 11-5-3-9-6-10-11z" fill="#c8743a"/><path d="M20 24c3-5 8-8 14-8" stroke="#fff" stroke-opacity=".4" stroke-width="3" stroke-linecap="round" fill="none"/>`,
    leaf: `<path d="M10 54C8 26 26 8 56 8c0 28-16 46-42 46" fill="#4fae5a"/><path d="M10 54c10-18 22-28 36-34" stroke="#e5f7d3" stroke-width="3" stroke-linecap="round" fill="none"/>`,
  };

  // Fondo suave de cada ilustración (para tarjetas sin foto)
  const TINT = {
    apple: '#fde4e1', orange: '#fdeacb', carrot: '#fde6d2', broccoli: '#dff1d6', avocado: '#e9f5cf', tomato: '#fde0dc',
    egg: '#fff3d1', wheat: '#fbf0cd', drop: '#d9eefb', bean: '#f6dfd3', lemon: '#fcf5cc', fish: '#d9eef6',
    milk: '#e3f0f9', drumstick: '#f8e3d3', leaf: '#e3f2e7',
  };

  // palabra clave (sin tildes) -> ilustración
  const KEYS = [
    [/manzana|pera\b|fruta|uva|fresa|mora/, 'apple'], [/naranja|mandarina|citric/, 'orange'], [/zanahoria/, 'carrot'],
    [/brocoli|coliflor|espinaca|verdura|lechuga|ensalada|hortaliza|vegetal/, 'broccoli'], [/aguacate|palta/, 'avocado'],
    [/tomate/, 'tomato'], [/huevo/, 'egg'], [/avena|arroz|trigo|cereal|pan\b|pasta|quinua|quinoa|maiz|grano/, 'wheat'],
    [/agua|hidrat|jugo|bebida/, 'drop'], [/lenteja|frijol|garbanzo|legumbre|soya|haba|alubia/, 'bean'], [/limon/, 'lemon'],
    [/pescado|salmon|atun|trucha|mariscos?/, 'fish'], [/leche|yogur|queso|lacteo/, 'milk'], [/pollo|carne|res\b|cerdo|pavo|proteina/, 'drumstick'],
  ];
  const strip = (s) => String(s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

  /** Elige una ilustración a partir de textos (nombre, categoría...). */
  function pick(...texts) {
    for (const t of texts) {
      const s = strip(t);
      const hit = KEYS.find(([re]) => re.test(s));
      if (hit) return hit[1];
    }
    return 'leaf';
  }

  function mountSprite() {
    if (document.getElementById('art-sprite')) return;
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.id = 'art-sprite';
    svg.setAttribute('aria-hidden', 'true');
    svg.setAttribute('width', '0');
    svg.setAttribute('height', '0');
    svg.style.position = 'absolute';
    svg.innerHTML = Object.entries(S).map(([k, v]) => `<symbol id="art-${k}" viewBox="0 0 64 64">${v}</symbol>`).join('');
    document.body.prepend(svg);
  }

  /** Devuelve un <svg> con la ilustración indicada. */
  function svg(name, size = 64, cls = '') {
    const n = S[name] ? name : 'leaf';
    const s = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    s.setAttribute('viewBox', '0 0 64 64');
    s.setAttribute('width', size);
    s.setAttribute('height', size);
    s.setAttribute('aria-hidden', 'true');
    if (cls) s.setAttribute('class', cls);
    const u = document.createElementNS('http://www.w3.org/2000/svg', 'use');
    u.setAttribute('href', `#art-${n}`);
    s.append(u);
    return s;
  }

  const tint = (name) => TINT[name] || TINT.leaf;
  mountSprite(); // los módulos se ejecutan con el DOM ya analizado
  return { svg, pick, tint, names: Object.keys(S) };
})();
