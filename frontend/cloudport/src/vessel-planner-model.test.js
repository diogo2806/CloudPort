import test from 'node:test';
import assert from 'node:assert/strict';
import {
  buildContainerIndex,
  buildCraneIndex,
  buildLegend,
  buildRestowIndex,
  buildSlotWarnings,
  buildStackSummaries,
  buildViolationIndex,
  chooseDropSlot,
  legendValueForSlot,
  normalizeSlots,
  slotPositionKey,
  toAllocationPayload,
  weightBand
} from './vessel-planner-model.js';

const slots = [
  {
    id: 3,
    bay: 3,
    rowBay: 2,
    tier: 84,
    codigoContainer: 'ABCD1234567',
    pesoKg: 28000,
    maxPesoKg: 30000,
    maxPesoPilhaKg: 50000,
    portoDescarga: 'BRSSZ',
    classeImo: '3',
    perigoso: true,
    reefer: false
  },
  {
    id: 1,
    bay: 1,
    rowBay: 1,
    tier: 82,
    codigoContainer: 'EFGH1234567',
    pesoVgmKg: 22000,
    maxPesoPilhaKg: 40000,
    portoDescarga: 'NLRTM',
    reefer: true,
    tomadaReefer: true
  },
  {
    id: 2,
    bay: 1,
    rowBay: 1,
    tier: 84,
    codigoContainer: 'IJKL1234567',
    pesoKg: 16000,
    maxPesoPilhaKg: 40000,
    portoDescarga: 'NLRTM'
  },
  {
    id: 4,
    bay: 1,
    rowBay: 2,
    tier: 82,
    restrito: true,
    motivoRestricao: 'Área bloqueada'
  },
  {
    id: 5,
    bay: 1,
    rowBay: 2,
    tier: 84
  }
];

test('normaliza e ordena slots por bay, row e tier', () => {
  const normalized = normalizeSlots({ slots });
  assert.deepEqual(normalized.map((slot) => slot.id), [1, 2, 4, 5, 3]);
});

test('calcula peso acumulado e utilização da pilha', () => {
  const summaries = buildStackSummaries(slots);
  assert.equal(summaries['1:1'].weightKg, 38000);
  assert.equal(summaries['1:1'].maxWeightKg, 40000);
  assert.equal(summaries['1:1'].percent, 95);
  assert.equal(summaries['1:1'].status, 'ATENCAO');
  assert.equal(summaries['3:2'].status, 'OK');
});

test('gera legendas por POD, peso, IMO, reefer e operador', () => {
  const containerIndex = buildContainerIndex([
    { codigoContainer: 'ABCD1234567', operador: 'LINE-A' },
    { codigoContainer: 'EFGH1234567', operadorMaritimo: 'LINE-B' }
  ]);
  assert.equal(legendValueForSlot(slots[0], 'POD', containerIndex), 'BRSSZ');
  assert.equal(legendValueForSlot(slots[0], 'WEIGHT', containerIndex), '20–30 t');
  assert.equal(legendValueForSlot(slots[0], 'IMO', containerIndex), 'IMO 3');
  assert.equal(legendValueForSlot(slots[1], 'REEFER', containerIndex), 'Reefer');
  assert.equal(legendValueForSlot(slots[0], 'OPERATOR', containerIndex), 'LINE-A');
  assert.equal(buildLegend(slots, 'POD', containerIndex).find((item) => item.value === 'NLRTM').count, 2);
});

test('seleciona o primeiro slot livre e não restrito para drop na pilha', () => {
  const target = chooseDropSlot(slots, 1, 2);
  assert.equal(target.id, 5);
  assert.equal(chooseDropSlot(slots, 1, 1), null);
});

test('indexa violações, restow e sequência de guindastes pela posição', () => {
  const violations = buildViolationIndex({ violacoes: [{ slotId: 3, tipo: 'IMDG', severidade: 'PERIGO', descricao: 'Segregação inválida' }] });
  assert.equal(violations['3'].length, 1);

  const restow = buildRestowIndex({ movimentos: [{ codigoContainer: 'ABCD1234567', bayAtual: 3, rowAtual: 2, tierAtual: 84, bayDestino: 1, rowDestino: 2, tierDestino: 84 }] });
  assert.equal(restow['3:2:84'].role, 'ORIGEM');
  assert.equal(restow['1:2:84'].role, 'DESTINO');

  const cranes = buildCraneIndex({ sequencia: [
    { ordem: 2, bay: 3, rowBay: 2, tier: 84, guindasteId: 1 },
    { ordem: 1, bay: 3, rowBay: 2, tier: 84, guindasteId: 1 }
  ] });
  assert.deepEqual(cranes['3:2:84'].map((item) => item.ordem), [1, 2]);
});

test('projeta restrições e excesso de peso diretamente no slot', () => {
  const violationIndex = buildViolationIndex({ violacoes: [{ slotId: 4, tipo: 'GEOMETRIA', severidade: 'PERIGO', descricao: 'Slot incompatível' }] });
  const summaries = buildStackSummaries(slots);
  const warnings = buildSlotWarnings(slots[3], { violations: violationIndex }, summaries['1:2']);
  assert.equal(warnings.some((warning) => warning.type === 'GEOMETRIA'), true);
  assert.equal(warnings.some((warning) => warning.type === 'RESTRICAO'), true);
});

test('mantém todos os atributos operacionais ao mover contêiner entre slots', () => {
  const payload = toAllocationPayload({
    slot: {
      codigoContainer: 'ABCD1234567',
      isoCode: '45G1',
      pesoKg: 28000,
      pesoVgmKg: 28200,
      portoCarga: 'CNSHA',
      portoDescarga: 'BRSSZ',
      classeImo: '3',
      numeroOnu: '1203',
      perigoso: true,
      reefer: false,
      oog: true,
      excessoAlturaCm: 25
    }
  });
  assert.equal(payload.codigoContainer, 'ABCD1234567');
  assert.equal(payload.pesoVgmKg, 28200);
  assert.equal(payload.numeroOnu, '1203');
  assert.equal(payload.oog, true);
  assert.equal(payload.excessoAlturaCm, 25);
});

test('classifica faixas de peso e chaves de posição', () => {
  assert.equal(weightBand(9999), '< 10 t');
  assert.equal(weightBand(10000), '10–20 t');
  assert.equal(weightBand(30000), '≥ 30 t');
  assert.equal(slotPositionKey(slots[0]), '3:2:84');
});
