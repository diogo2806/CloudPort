import assert from 'node:assert/strict';
import test from 'node:test';
import {
  DROP_VALIDATION_STATUS,
  containerWeightKg,
  dragSource,
  hatchRestriction,
  projectedStackWeight,
  stackStatus,
  validateDropTarget
} from './vessel-planner-phase2.js';

const baseSlots = [
  { id: 1, bay: 1, rowBay: 1, tier: 2, codigoContainer: 'BASE0000001', pesoKg: 18000, maxPesoPilhaKg: 50000 },
  { id: 2, bay: 1, rowBay: 1, tier: 4, maxPesoPilhaKg: 50000 },
  { id: 3, bay: 1, rowBay: 2, tier: 2, maxPesoPilhaKg: 30000, tomadaReefer: true },
  { id: 4, bay: 1, rowBay: 2, tier: 4, maxPesoPilhaKg: 30000 },
  { id: 5, bay: 2, rowBay: 1, tier: 2, restrito: true, motivoRestricao: 'Estrutura indisponível' }
];

const summaries = {
  '1:1': { key: '1:1', bay: 1, rowBay: 1, weightKg: 18000, maxWeightKg: 50000, percent: 36 },
  '1:2': { key: '1:2', bay: 1, rowBay: 2, weightKg: 0, maxWeightKg: 30000, percent: 0 },
  '2:1': { key: '2:1', bay: 2, rowBay: 1, weightKg: 0, maxWeightKg: 40000, percent: 0 }
};

test('normaliza a origem e prioriza o peso VGM', () => {
  const payload = { kind: 'container', container: { codigoContainer: 'TEST0000001', pesoKg: 20000, pesoVgmKg: 21500 } };
  assert.equal(dragSource(payload).codigoContainer, 'TEST0000001');
  assert.equal(containerWeightKg(payload), 21500);
});

test('bloqueia slot ocupado, restrito e destino sem suporte', () => {
  const payload = { kind: 'container', container: { codigoContainer: 'TEST0000001', pesoKg: 10000 } };
  const occupied = validateDropTarget({ payload, target: baseSlots[0], slots: baseSlots, stackSummaries: summaries });
  assert.equal(occupied.status, DROP_VALIDATION_STATUS.BLOCKED);
  assert.ok(occupied.reasons.some((reason) => reason.code === 'OCCUPIED'));

  const restricted = validateDropTarget({ payload, target: baseSlots[4], slots: baseSlots, stackSummaries: summaries });
  assert.ok(restricted.reasons.some((reason) => reason.code === 'RESTRICTED'));

  const unsupported = validateDropTarget({ payload, target: baseSlots[3], slots: baseSlots, stackSummaries: summaries });
  assert.ok(unsupported.reasons.some((reason) => reason.code === 'UNSUPPORTED'));
});

test('valida reefer e limite individual do slot', () => {
  const payload = { kind: 'container', container: { codigoContainer: 'REEF0000001', pesoKg: 32000, reefer: true } };
  const result = validateDropTarget({ payload, target: { ...baseSlots[3], maxPesoKg: 30000 }, slots: baseSlots, stackSummaries: summaries });
  assert.equal(result.status, DROP_VALIDATION_STATUS.BLOCKED);
  assert.ok(result.reasons.some((reason) => reason.code === 'REEFER'));
  assert.ok(result.reasons.some((reason) => reason.code === 'SLOT_WEIGHT'));
});

test('projeta o peso da stack e sinaliza atenção ou excesso', () => {
  const warningPayload = { kind: 'container', container: { codigoContainer: 'WARN0000001', pesoKg: 25000 } };
  const warningProjection = projectedStackWeight(baseSlots[1], warningPayload, summaries);
  assert.equal(warningProjection.projectedWeightKg, 43000);
  assert.equal(warningProjection.percent, 86);
  const warning = validateDropTarget({ payload: warningPayload, target: baseSlots[1], slots: baseSlots, stackSummaries: summaries });
  assert.equal(warning.status, DROP_VALIDATION_STATUS.WARNING);
  assert.equal(stackStatus(summaries['1:1'], warning.percent), 'WARNING');

  const blockedPayload = { kind: 'container', container: { codigoContainer: 'HEAV0000001', pesoKg: 34000 } };
  const blocked = validateDropTarget({ payload: blockedPayload, target: baseSlots[1], slots: baseSlots, stackSummaries: summaries });
  assert.equal(blocked.status, DROP_VALIDATION_STATUS.BLOCKED);
  assert.ok(blocked.reasons.some((reason) => reason.code === 'STACK_WEIGHT'));
});

test('aplica o estado persistido da tampa conforme porão e convés', () => {
  const covers = [{ codigo: 'HC01', estado: 'FECHADA', tarefas: [] }];
  const hold = hatchRestriction({ codigoHatchCover: 'HC01', tier: 6 }, covers);
  assert.equal(hold.code, 'HATCH_CLOSED');
  assert.equal(hold.status, DROP_VALIDATION_STATUS.BLOCKED);
  assert.equal(hatchRestriction({ codigoHatchCover: 'HC01', tier: 82 }, covers), null);

  const opened = [{ codigo: 'HC01', estado: 'ABERTA', tarefas: [] }];
  const deck = hatchRestriction({ codigoHatchCover: 'HC01', tier: 82 }, opened);
  assert.equal(deck.code, 'HATCH_OPEN');
});

test('aceita destino compatível e preserva aviso de tarefa de tampa planejada', () => {
  const payload = { kind: 'container', container: { codigoContainer: 'OKAY0000001', pesoKg: 9000 } };
  const valid = validateDropTarget({ payload, target: baseSlots[2], slots: baseSlots, stackSummaries: summaries });
  assert.equal(valid.status, DROP_VALIDATION_STATUS.VALID);

  const hatchTarget = { ...baseSlots[2], codigoHatchCover: 'HC02', zonaNavio: 'CONVES' };
  const covers = [{ codigo: 'HC02', estado: 'FECHADA', tarefas: [{ id: 1, tipo: 'ABRIR', status: 'PLANEJADA' }] }];
  const warning = validateDropTarget({ payload, target: hatchTarget, slots: baseSlots, stackSummaries: summaries, hatchCovers: covers });
  assert.equal(warning.status, DROP_VALIDATION_STATUS.WARNING);
  assert.ok(warning.reasons.some((reason) => reason.code === 'HATCH_TASK_PLANNED'));
});
