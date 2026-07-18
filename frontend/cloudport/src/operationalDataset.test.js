import assert from 'node:assert/strict';
import test from 'node:test';
import { collectDatasetKeys, humanizeDatasetKey } from './operationalDataset.js';

test('infere todas as colunas encontradas sem limitar a oito campos', () => {
  const rows = [
    { id: 1, campoA: 'A', campoB: 'B', campoC: 'C', campoD: 'D', campoE: 'E' },
    { id: 2, campoF: 'F', campoG: 'G', campoH: 'H', campoI: 'I', campoJ: 'J' }
  ];

  const keys = collectDatasetKeys(rows, ['id', 'campoA']);

  assert.equal(keys.length, 11);
  assert.deepEqual(keys.slice(0, 2), ['id', 'campoA']);
  assert.ok(keys.includes('campoJ'));
});

test('remove preferências duplicadas e humaniza nomes técnicos', () => {
  assert.deepEqual(collectDatasetKeys([{ codigoISO: 'BR' }], ['codigoISO', 'codigoISO']), ['codigoISO']);
  assert.equal(humanizeDatasetKey('horaChegada_prevista'), 'Hora Chegada prevista');
});
