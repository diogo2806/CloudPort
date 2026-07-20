import assert from 'node:assert/strict';
import test from 'node:test';
import {
  buildVesselTimeframeScene,
  buildYardTimeframeScene,
  normalizeOperationalState,
  operationalSceneSummary,
  yardPositionKey
} from './operational-timeframes.js';

test('normaliza estados operacionais heterogêneos', () => {
  assert.equal(normalizeOperationalState('em execução'), 'EM_EXECUCAO');
  assert.equal(normalizeOperationalState('IMINENTE'), 'DESPACHADO');
  assert.equal(normalizeOperationalState('interditada'), 'BLOQUEADO');
  assert.equal(normalizeOperationalState('completed'), 'CONCLUIDO');
});

test('projeta planos tentativos no Preplan do pátio', () => {
  const blocks = [{ bloco: 'A', stacks: [{ linha: 1, coluna: 2, layers: [{ id: 10, linha: 1, coluna: 2, camadaOperacional: '1', ocupada: false, areaPermitida: true }] }] }];
  const plans = [{ id: 7, bloco: 'A', linha: 1, coluna: 2, camada: '1', codigoContainer: 'CONT1', estado: 'TENTATIVO', origem: 'OTIMIZADOR', atualizadoEm: '2026-07-20T12:00:00Z' }];
  const scene = buildYardTimeframeScene(blocks, plans, 'PREPLAN');
  const layer = scene.blocks[0].stacks[0].layers[0];
  assert.equal(layer.key, yardPositionKey({ bloco: 'A', linha: 1, coluna: 2, camada: '1' }));
  assert.equal(layer.state, 'TENTATIVO');
  assert.equal(layer.containerCode, 'CONT1');
  assert.match(layer.origin, /OTIMIZADOR/);
});

test('mantém inventário físico no Current e bloqueios acima da ocupação', () => {
  const blocks = [{ bloco: 'B', stacks: [{ linha: 2, coluna: 3, layers: [
    { id: 1, linha: 2, coluna: 3, camadaOperacional: '1', ocupada: true, codigoConteiner: 'ABC', areaPermitida: true },
    { id: 2, linha: 2, coluna: 3, camadaOperacional: '2', ocupada: false, bloqueada: true, areaPermitida: false }
  ] }] }];
  const scene = buildYardTimeframeScene(blocks, [], 'CURRENT');
  assert.equal(scene.blocks[0].stacks[0].layers[0].state, 'CONCLUIDO');
  assert.equal(scene.blocks[0].stacks[0].layers[1].state, 'BLOQUEADO');
});

test('representa sequenciamento iminente e destino de restow no navio', () => {
  const plan = { id: 9, status: 'RASCUNHO', slots: [
    { id: 1, bay: 1, rowBay: 1, tier: 80, codigoContainer: 'AAA' },
    { id: 2, bay: 1, rowBay: 2, tier: 80 }
  ] };
  const sequencing = { sequencia: [{ bay: 1, rowBay: 1, tier: 80, guindasteId: 2, ordem: 1, status: 'EM_EXECUCAO' }] };
  const restow = { movimentos: [{ codigoContainer: 'AAA', bayAtual: 1, rowAtual: 1, tierAtual: 80, bayDestino: 1, rowDestino: 2, tierDestino: 80, status: 'PLANEJADO' }] };
  const imminent = buildVesselTimeframeScene(plan, restow, sequencing, 'IMMINENT');
  assert.equal(imminent.slots.find((slot) => slot.id === 1).state, 'EM_EXECUCAO');
  const future = buildVesselTimeframeScene(plan, restow, sequencing, 'FUTURE');
  assert.equal(future.slots.find((slot) => slot.id === 2).state, 'RESERVADO');
  assert.equal(future.slots.find((slot) => slot.id === 2).containerCode, 'AAA');
  assert.equal(operationalSceneSummary(future).RESERVADO, 1);
});
