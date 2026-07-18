import test from 'node:test';
import assert from 'node:assert/strict';
import { dominantLegendForSlots, findSynchronizedSlot, selectionCoordinates } from './vessel-planner-phase1.js';

const slots = [
  { id: 1, bay: 1, rowBay: 1, tier: 82, codigoContainer: 'A', portoDescarga: 'BRSSZ' },
  { id: 2, bay: 1, rowBay: 2, tier: 82, codigoContainer: 'B', portoDescarga: 'NLRTM' },
  { id: 3, bay: 3, rowBay: 1, tier: 84, codigoContainer: 'C', portoDescarga: 'BRSSZ' },
  { id: 4, bay: 3, rowBay: 2, tier: 84 }
];

test('seleciona o slot exato ao sincronizar coordenadas', () => {
  assert.equal(findSynchronizedSlot(slots, { bay: 3, rowBay: 2, tier: 84 }).id, 4);
});

test('usa o slot mais próximo preservando prioridade de bay, row e tier', () => {
  assert.equal(findSynchronizedSlot(slots, { bay: 3, rowBay: 1, tier: 82 }).id, 3);
});

test('calcula a legenda dominante para células agregadas', () => {
  const dominant = dominantLegendForSlots(slots, 'POD');
  assert.equal(dominant.value, 'BRSSZ');
  assert.equal(dominant.count, 2);
  assert.equal(dominant.total, 3);
});

test('extrai coordenadas normalizadas da seleção', () => {
  assert.deepEqual(selectionCoordinates({ bay: '3', row: '2', tier: '84' }), { bay: 3, rowBay: 2, tier: 84 });
});
