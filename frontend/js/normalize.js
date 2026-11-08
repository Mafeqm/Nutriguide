/**
 * Funciones PURAS que convierten las respuestas del backend en el formato
 * interno del frontend. Sin dependencias del DOM → se pueden probar con Node.
 * Si el contrato del backend cambia, este es el único archivo a ajustar.
 */
const str = (v) => (v === null || v === undefined ? '' : String(v));
const num = (v) => (v === null || v === undefined || v === '' || Number.isNaN(Number(v)) ? null : Number(v));
const nameOf = (v) => (v && typeof v === 'object' ? str(v.nombre) : str(v));

/** Las listas pueden venir como arreglo o paginadas ({content|data|items}). */
export const toList = (d) => (Array.isArray(d) ? d : d?.content || d?.data || d?.items || []);

function normOtros(raw) {
  if (!raw) return [];
  if (Array.isArray(raw)) {
    return raw
      .map((o) => ({ nombre: str(o.nombre), valor: num(o.valor ?? o.cantidad), unidad: str(o.unidad) }))
      .filter((o) => o.nombre);
  }
  if (typeof raw === 'object') return Object.entries(raw).map(([nombre, valor]) => ({ nombre, valor: num(valor), unidad: '' }));
  return [];
}

export function normNutricion(a = {}) {
  const n = a.informacionNutricional || a.nutricion || a;
  return {
    calorias: num(n.calorias ?? n.energia),
    proteinas: num(n.proteinas ?? n.proteina),
    carbohidratos: num(n.carbohidratos),
    grasas: num(n.grasas ?? n.grasa),
    fibra: num(n.fibra),
    otros: normOtros(n.otrosNutrientes ?? n.nutrientes),
    porcion: num(n.tamanoPorcion ?? n.tamanioPorcion ?? n.porcion),
    unidad: str(n.unidadPorcion ?? n.unidadReferencia ?? n.unidad),
    fuente: str(n.fuente ?? n.fuenteInformacion ?? a.fuente),
  };
}

export const normAlimento = (a) => ({
  id: a.id,
  nombre: str(a.nombre),
  categoria: nameOf(a.categoria),
  descripcion: str(a.descripcion),
  imagen: str(a.imagenUrl ?? a.imagen ?? a.urlImagen),
  ...normNutricion(a),
});

/** El backend envía el tiempo de preparación en minutos (número); se muestra como "N min". */
export function formatTiempo(v) {
  const t = str(v).trim();
  return /^\d+$/.test(t) ? `${Number(t)} min` : t;
}

export function toSteps(v) {
  if (Array.isArray(v)) return v.map((p) => str(typeof p === 'object' && p ? p.descripcion ?? p.texto : p)).filter(Boolean);
  return str(v).split(/\r?\n/).map((s) => s.replace(/^\s*\d+[.)-]\s*/, '').trim()).filter(Boolean);
}

export function toIngredients(v) {
  if (Array.isArray(v)) {
    return v.map((i) => {
      if (typeof i !== 'object' || i === null) return str(i);
      const nombre = str(i.nombre ?? i.ingrediente);
      const cant = [i.cantidad, i.unidad].filter((x) => x !== undefined && x !== null && x !== '').join(' ');
      return [cant, nombre].filter(Boolean).join(' ');
    }).filter(Boolean);
  }
  return str(v).split(/\r?\n/).map((s) => s.trim()).filter(Boolean);
}

export const normReceta = (r) => ({
  id: r.id,
  nombre: str(r.nombre ?? r.titulo),
  categoria: nameOf(r.categoria),
  descripcion: str(r.descripcion),
  imagen: str(r.imagenUrl ?? r.imagen ?? r.urlImagen),
  tiempo: formatTiempo(r.tiempoPreparacion ?? r.tiempo),
  ingredientes: toIngredients(r.ingredientes),
  pasos: toSteps(r.preparacion ?? r.pasos ?? r.instrucciones),
  ...normNutricion(r),
});

export const normDocumento = (d) => ({
  id: d.id,
  titulo: str(d.titulo ?? d.nombre),
  descripcion: str(d.descripcion ?? d.resumen),
  categoria: nameOf(d.categoria ?? d.tema),
  contenido: str(d.contenido ?? d.texto),
  url: str(d.url ?? d.enlace),
});

/** Respuesta del chat → { respuesta, fuentes[] }; lanza Error si no hay respuesta. */
export function normChat(data) {
  const respuesta = str(data?.respuesta).trim();
  if (!respuesta) throw new Error('empty');
  const fuentes = (Array.isArray(data.fuentes) ? data.fuentes : []).map((f) => ({
    titulo: str(f?.titulo ?? f?.nombre) || 'Fuente sin título',
    contenido: str(f?.contenido ?? f?.fragmento),
  }));
  return { respuesta, fuentes };
}
