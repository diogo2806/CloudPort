import assert from 'node:assert/strict';
import test from 'node:test';
import {
  OVERLAY_RISK,
  aggregateOverlayForSlots,
  buildCraneLanes,
  buildImdgIndex,
  buildOverlayIndex,
  buildRestowFlows,
  highestRisk,
  overlaySummary
} from './vessel-planner-phase3.js';

const slots = [
  { id: 1, bay: 1, rowBay: 1, tier: 82, codigoContainer: 'IMDG0000001', perigoso: true, classeImo: '3', numeroOnu: '1203', pesoKg: 22000, maxPesoKg: 30000 },
  { id: 2, bay: 1, rowBay: 2, tier: 82, codigoContainer: 'IMDG0000002', perigoso: true, classeImo: '8', numeroOnu: '1760', pesoKg: 18000, maxPesoKg: 30000 },
  { id: 3, bay: 3, rowBay: 5, tier: 2, codigoContainer: 'DRY00000001', pesoKg: 12000, maxPesoKg: 25000 }
];

const stackSummaries = {
  '1:1': { ratio: 0.9, percent: 90, weightKg: 22000, maxWeightKg: 25000 },
  '1:2': { ratio: 0.6, percent: 60, weightKg: 18000, maxWeightKg: 30000 },
  '3:5': { ratio: 1.1, percent: 110, weightKg: 12000, maxWeightKg: 11000 }
};

const violationIndex = {
  __global__: [],
  '1': [{ tipo: 'SEGREGACAO_IMDG', severidade: 'PERIGO', descricao: 'Separação mínima não atendida', slotRelacionadoId: 2 }],
  '3': [{ tipo: 'FORCA_ESTRUTURAL', severidade: 'PERIGO', descricao: 'Limite estrutural excedido' }]
};

test('marca conflito IMDG retornado pelo backend como risco alto', () => {
  const index = buildImdgIndex(slots, {}, violationIndex);
  assert.equal(index['1'].risk, OVERLAY_RISK.HIGH);
  assert.equal(index['1'].authoritativeConflict, true);
  assert.deepEqual(index['1'].relatedPositions, ['1:2:82']);
});

test('marca proximidade entre cargas perigosas apenas como atenção gráfica', () => {
  const index = buildImdgIndex(slots, {}, { __global__: [] });
  assert.equal(index['1'].risk, OVERLAY_RISK.MEDIUM);
  assert.equal(index['2'].risk, OVERLAY_RISK.MEDIUM);
  assert.equal(index['1'].authoritativeConflict, false);
});

test('overlay combinado escolhe o maior risco técnico', () => {
  const imdgIndex = buildImdgIndex(slots, {}, violationIndex);
  const overlays = buildOverlayIndex(slots, 'COMBINED', { stackSummaries, violationIndex, imdgIndex });
  assert.equal(overlays['1'].risk, OVERLAY_RISK.HIGH);
  assert.equal(overlays['3'].risk, OVERLAY_RISK.HIGH);
  assert.equal(highestRisk(['LOW', 'MEDIUM', 'HIGH']), OVERLAY_RISK.HIGH);
});

test('agrega o pior risco das células agrupadas e resume os níveis', () => {
  const overlays = {
    '1': { risk: 'LOW', shortLabel: 'EST', details: [] },
    '2': { risk: 'HIGH', shortLabel: 'IMDG', details: ['Conflito'] },
    '3': { risk: 'NONE', shortLabel: '', details: [] }
  };
  const aggregate = aggregateOverlayForSlots(slots.slice(0, 2), overlays);
  assert.equal(aggregate.risk, OVERLAY_RISK.HIGH);
  assert.equal(aggregate.shortLabel, 'IMDG');
  assert.deepEqual(overlaySummary(overlays), { NONE: 1, LOW: 1, MEDIUM: 0, HIGH: 1 });
});

test('agrupa e ordena a sequência por guindaste', () => {
  const lanes = buildCraneLanes({ sequencia: [
    { guindasteId: 2, ordem: 2, codigoContainer: 'B', bay: 3, rowBay: 1, tier: 82 },
    { guindasteId: 1, ordem: 1, codigoContainer: 'A', bay: 1, rowBay: 1, tier: 82 },
    { guindasteId: 2, ordem: 1, codigoContainer: 'C', bay: 2, rowBay: 1, tier: 82, bloqueadoPorTampa: true }
  ] });
  assert.deepEqual(lanes.map((lane) => lane.craneId), [1, 2]);
  assert.deepEqual(lanes[1].operations.map((operation) => operation.order), [1, 2]);
  assert.equal(lanes[1].blocked, 1);
});

test('normaliza o fluxo de restow com origem, destino e ordem', () => {
  const flows = buildRestowFlows({ movimentos: [{
    codigoContainer: 'REST0000001',
    bayAtual: 1,
    rowAtual: 2,
    tierAtual: 82,
    bayDestino: 3,
    rowDestino: 4,
    tierDestino: 84,
    motivo: 'Liberar descarga'
  }] });
  assert.equal(flows[0].order, 1);
  assert.deepEqual(flows[0].source, { bay: 1, rowBay: 2, tier: 82 });
  assert.deepEqual(flows[0].destination, { bay: 3, rowBay: 4, tier: 84 });
  assert.equal(flows[0].reason, 'Liberar descarga');
});
