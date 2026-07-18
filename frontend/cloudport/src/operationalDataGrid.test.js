import assert from 'node:assert/strict';
import test from 'node:test';
import {
  applyGridFilters,
  buildGridCsv,
  moveColumn,
  paginateGridRows,
  reconcileColumnLayout,
  resolveRowKey,
  sortGridRows
} from './operationalDataGrid.js';

const columns = [
  { key: 'codigo', label: 'Código' },
  { key: 'status', label: 'Status' },
  { key: 'peso', label: 'Peso' }
];
const rows = [
  { id: 1, codigo: 'CNT-Á01', status: 'PENDENTE', peso: 20 },
  { id: 2, codigo: 'CNT-B02', status: 'LIBERADO', peso: 5 },
  { id: 3, codigo: 'BOX-C03', status: 'LIBERADO', peso: 12 }
];

test('filtra por busca global sem diferenciar acentos e aplica filtros por coluna', () => {
  assert.deepEqual(applyGridFilters(rows, columns, 'cnt-a', []).map((row) => row.id), [1]);
  assert.deepEqual(applyGridFilters(rows, columns, '', [{ columnKey: 'status', operator: 'equals', value: 'liberado' }]).map((row) => row.id), [2, 3]);
});

test('ordena de forma estável e pagina com os tamanhos operacionais', () => {
  assert.deepEqual(sortGridRows(rows, columns, { columnKey: 'peso', direction: 'asc' }).map((row) => row.id), [2, 3, 1]);
  const page = paginateGridRows(rows, 1, 2);
  assert.equal(page.totalPages, 2);
  assert.deepEqual(page.rows.map((row) => row.id), [3]);
});

test('reconcilia, move e preserva somente colunas existentes no layout', () => {
  const layout = reconcileColumnLayout(columns, { order: ['status', 'inexistente'], hidden: ['peso', 'outra'], frozenFirst: true });
  assert.deepEqual(layout, { order: ['status', 'codigo', 'peso'], hidden: ['peso'], frozenFirst: true });
  assert.deepEqual(moveColumn(layout.order, 'codigo', 'left'), ['codigo', 'status', 'peso']);
});

test('gera CSV compatível com Excel e resolve chaves estáveis', () => {
  const csv = buildGridCsv(rows.slice(0, 1), columns);
  assert.match(csv, /^\uFEFF"Código";"Status";"Peso"/);
  assert.match(csv, /"CNT-Á01"/);
  assert.equal(resolveRowKey(rows[0], 0, 'id'), '1');
});
