import assert from 'node:assert/strict';
import test from 'node:test';
import {
  buildEquipmentMapEntries,
  mergeYardEquipment,
  yardRestrictionSummary
} from './pages/yard/yardLiveMap.js';

test('mescla mapa e telemetria preservando a leitura mais recente do CHE', () => {
  const result = mergeYardEquipment(
    [{ identificador: 'RTG-01', status: 'PARADO', linha: 1, coluna: 1 }],
    [{ equipamento: 'RTG-01', statusOperacional: 'OPERANDO', linha: 2, coluna: 3 }]
  );

  assert.equal(result.length, 1);
  assert.equal(result[0].identificador, 'RTG-01');
  assert.equal(result[0].statusOperacional, 'OPERANDO');
  assert.equal(result[0].linha, 2);
  assert.equal(result[0].coluna, 3);
});

test('posiciona CHE por GPS ou pela pilha operacional mais próxima', () => {
  const layout = [
    { stack: { bloco: 'A', linha: 1, coluna: 2 }, center: { lat: -22.9, lng: -43.8 } }
  ];
  const result = buildEquipmentMapEntries([
    { identificador: 'RTG-01', bloco: 'A', linha: 1, coluna: 2, status: 'OPERANDO' },
    { identificador: 'RS-02', latitude: -22.91, longitude: -43.81, status: 'DESLOCANDO' }
  ], layout);

  assert.deepEqual(result[0].position, { lat: -22.9, lng: -43.8 });
  assert.equal(result[0].nearestStack.bloco, 'A');
  assert.deepEqual(result[1].position, { lat: -22.91, lng: -43.81 });
  assert.equal(result[1].nearestStack, null);
});

test('resume pilhas bloqueadas e interditadas sem duplicar contagens', () => {
  const summary = yardRestrictionSummary([
    { bloco: 'A', stacks: [
      { restricted: true, layers: [{ bloqueada: true }, { interditada: true }] },
      { restricted: false, layers: [{ areaPermitida: true }] },
      { restricted: false, layers: [{ areaPermitida: false }] }
    ] }
  ]);

  assert.deepEqual(summary, { blocked: 2, interdicted: 1 });
});
