import test from 'node:test';
import assert from 'node:assert/strict';
import { parseBlocks, parseInline } from '../js/richtext.js';

test('parseInline separa negritas', () => {
  assert.deepEqual(parseInline('a **b** c'), [{ text: 'a ' }, { text: 'b', bold: true }, { text: ' c' }]);
});

test('parseBlocks agrupa párrafos y listas', () => {
  const b = parseBlocks('Intro\nlínea 2\n\n- uno\n- dos\n\n1. a\n2. b');
  assert.equal(b[0].type, 'p');
  assert.equal(b[0].lines.length, 2);
  assert.equal(b[1].type, 'ul');
  assert.equal(b[1].items.length, 2);
  assert.equal(b[2].type, 'ol');
});

test('el HTML se conserva como texto literal (no se interpreta)', () => {
  const [p] = parseBlocks('<img src=x onerror=alert(1)> **<b>x</b>**');
  const all = p.lines[0].map((s) => s.text).join('');
  assert.ok(all.includes('<img src=x onerror=alert(1)>'));
  assert.ok(p.lines[0].some((s) => s.bold && s.text === '<b>x</b>'));
});

test('entradas vacías no fallan', () => {
  assert.deepEqual(parseBlocks(''), []);
  assert.deepEqual(parseBlocks(null), []);
});
