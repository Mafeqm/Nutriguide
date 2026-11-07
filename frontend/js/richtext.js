/**
 * Mini-parser de texto enriquecido para las respuestas del asistente.
 * Soporta párrafos, listas (- * • / 1.) y **negrita**. Devuelve una estructura
 * de datos (no HTML), de modo que el DOM se construye siempre con nodos de texto
 * y es imposible inyectar HTML.
 */
export function parseInline(text) {
  const out = [];
  const re = /\*\*(.+?)\*\*/g;
  let last = 0;
  let m;
  while ((m = re.exec(text))) {
    if (m.index > last) out.push({ text: text.slice(last, m.index) });
    out.push({ text: m[1], bold: true });
    last = m.index + m[0].length;
  }
  if (last < text.length) out.push({ text: text.slice(last) });
  return out;
}

export function parseBlocks(text) {
  const blocks = [];
  let cur = null;
  for (const raw of String(text ?? '').replace(/\r/g, '').split('\n')) {
    if (!raw.trim()) { cur = null; continue; }
    const ul = raw.match(/^\s*[-*•]\s+(.*)/);
    const ol = raw.match(/^\s*\d+[.)]\s+(.*)/);
    if (ul || ol) {
      const type = ul ? 'ul' : 'ol';
      if (!cur || cur.type !== type) { cur = { type, items: [] }; blocks.push(cur); }
      cur.items.push(parseInline((ul || ol)[1]));
    } else if (cur && cur.type === 'p') {
      cur.lines.push(parseInline(raw.trim()));
    } else {
      cur = { type: 'p', lines: [parseInline(raw.trim())] };
      blocks.push(cur);
    }
  }
  return blocks;
}
