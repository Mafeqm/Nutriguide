import test from 'node:test';
import assert from 'node:assert/strict';
import { toList, normAlimento, normReceta, normDocumento, normChat, toSteps, toIngredients, formatTiempo } from '../js/normalize.js';

test('toList acepta arreglo o respuesta paginada', () => {
  assert.deepEqual(toList([1, 2]), [1, 2]);
  assert.deepEqual(toList({ content: [3] }), [3]);
  assert.deepEqual(toList(null), []);
});

test('normAlimento mapea campos, categoría como objeto y números como texto', () => {
  const a = normAlimento({ id: 1, nombre: 'Avena', categoria: { nombre: 'Cereales' }, calorias: '389', proteinas: 16.9, unidadPorcion: 'g', tamanoPorcion: 100 });
  assert.equal(a.categoria, 'Cereales');
  assert.equal(a.calorias, 389);
  assert.equal(a.fibra, null);
  assert.equal(a.porcion, 100);
  assert.equal(a.unidad, 'g');
});

test('normAlimento lee valores anidados y otros nutrientes (objeto o arreglo)', () => {
  const a = normAlimento({ nombre: 'X', informacionNutricional: { calorias: 10, otrosNutrientes: { 'Vitamina C': 5 } } });
  assert.equal(a.calorias, 10);
  assert.deepEqual(a.otros, [{ nombre: 'Vitamina C', valor: 5, unidad: '' }]);
  const b = normAlimento({ nombre: 'Y', otrosNutrientes: [{ nombre: 'Hierro', valor: 2, unidad: 'mg' }, { valor: 1 }] });
  assert.equal(b.otros.length, 1);
});

test('toSteps / toIngredients aceptan texto con saltos de línea o arreglos', () => {
  assert.deepEqual(toSteps('1. Hervir\n2) Servir'), ['Hervir', 'Servir']);
  assert.deepEqual(toSteps([{ descripcion: 'A' }, 'B']), ['A', 'B']);
  assert.deepEqual(toIngredients([{ cantidad: 2, unidad: 'tazas', nombre: 'arroz' }, 'sal']), ['2 tazas arroz', 'sal']);
});

test('normReceta y normDocumento aplican variantes de nombres', () => {
  const r = normReceta({ id: 1, titulo: 'Guiso', tiempoPreparacion: '30 min', preparacion: 'a\nb' });
  assert.equal(r.nombre, 'Guiso');
  assert.equal(r.pasos.length, 2);
  const d = normDocumento({ id: 1, nombre: 'Guía', enlace: 'https://x.dev' });
  assert.equal(d.titulo, 'Guía');
  assert.equal(d.url, 'https://x.dev');
});

test('normChat exige respuesta y normaliza fuentes', () => {
  assert.throws(() => normChat({ respuesta: '  ' }));
  assert.throws(() => normChat(null));
  const c = normChat({ respuesta: 'Hola', fuentes: [{ titulo: 'T', contenido: 'C' }, {}] });
  assert.equal(c.fuentes[0].titulo, 'T');
  assert.equal(c.fuentes[1].titulo, 'Fuente sin título');
  assert.deepEqual(normChat({ respuesta: 'ok' }).fuentes, []);
});

test('el tiempo en minutos del backend se muestra con unidad', () => {
  assert.equal(formatTiempo(12), '12 min');
  assert.equal(formatTiempo('25'), '25 min');
  assert.equal(formatTiempo('10 min'), '10 min');
  assert.equal(formatTiempo(null), '');
});

test('normReceta acepta el formato del backend (listas y nutrición por porción)', () => {
  const r = normReceta({
    id: 2, nombre: 'Bowl', categoria: 'Almuerzos', tiempoPreparacion: 25,
    ingredientes: ['60 g de quinua', '80 g de garbanzos'], pasos: ['Lavar.', 'Cocinar.'],
    informacionNutricional: { calorias: 510, proteinas: 18.7, carbohidratos: null, tamanoPorcion: 435, unidadPorcion: 'g (1 porción servida)', fuente: 'Estimación USDA' },
  });
  assert.equal(r.tiempo, '25 min');
  assert.deepEqual(r.pasos, ['Lavar.', 'Cocinar.']);
  assert.equal(r.calorias, 510);
  assert.equal(r.carbohidratos, null);
  assert.equal(r.porcion, 435);
  assert.equal(r.fuente, 'Estimación USDA');
});
