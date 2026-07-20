import assert from 'node:assert/strict';
import test from 'node:test';
import {
  applyOperationalFilter,
  buildFlowPreview,
  normalizeCheMap,
  planRailYardAssignments,
  shortestOperationalRoute,
  transferQuayQueue,
  validateGeometryDraft,
  validateWorkspacePayload
} from './operational-2d-tools.js';

test('gera fluxo stack-wise numerado com destinos crescentes', () => {
  const preview = buildFlowPreview({
    units: [{ id: 'U1' }, { id: 'U2' }],
    destinations: [{ id: 'D2', bay: 2, row: 1, tier: 1 }, { id: 'D1', bay: 1, row: 1, tier: 1 }]
  });
  assert.equal(preview.valid, true);
  assert.deepEqual(preview.moves.map((move) => move.destinationId), ['D1', 'D2']);
  assert.deepEqual(preview.moves.map((move) => move.sequence), [1, 2]);
});

test('bloqueia paired 20 quando existe unidade incompatível', () => {
  const preview = buildFlowPreview({
    units: [{ id: 'U1', lengthFeet: 20 }, { id: 'U2', lengthFeet: 40 }],
    destinations: [{ id: 'D1' }, { id: 'D2' }],
    paired20: true
  });
  assert.equal(preview.valid, false);
  assert.match(preview.errors.join(' '), /20 pés/);
});

test('divide e transfere fila de cais preservando sequência', () => {
  const result = transferQuayQueue([{
    id: 'F1',
    crane: 'QC-01',
    bay: '12',
    operations: [{ id: 'A', sequence: 1 }, { id: 'B', sequence: 2 }, { id: 'C', sequence: 3 }]
  }], 'F1', 'QC-02', 1);
  assert.equal(result.valid, true);
  assert.equal(result.moved, 2);
  assert.deepEqual(result.queues.find((queue) => queue.crane === 'QC-02').operations.map((item) => item.sequence), [1, 2]);
});

test('distingue telemetria stale da posição atual', () => {
  const now = Date.parse('2026-07-20T12:00:00Z');
  const items = normalizeCheMap([
    { id: 'CHE-1', timestamp: '2026-07-20T11:59:30Z' },
    { id: 'CHE-2', timestamp: '2026-07-20T11:55:00Z' }
  ], { now, staleAfterSeconds: 90 });
  assert.equal(items[0].stale, false);
  assert.equal(items[1].stale, true);
});

test('filtro bidirecional mantém contexto acinzentado', () => {
  const result = applyOperationalFilter([
    { id: 'A', domain: 'yard', state: 'BLOQUEADO' },
    { id: 'B', domain: 'rail', state: 'CONCLUIDO' }
  ], { domain: 'yard' });
  assert.equal(result[0].highlighted, true);
  assert.equal(result[1].dimmed, true);
});

test('valida workspace por papel e versão', () => {
  const invalid = validateWorkspacePayload({ name: 'Teste', scope: 'PAPEL' });
  assert.equal(invalid.valid, false);
  const valid = validateWorkspacePayload({ name: 'Teste', scope: 'PAPEL', role: 'PLANEJADOR', version: 2 });
  assert.equal(valid.valid, true);
  assert.equal(valid.payload.version, 2);
});

test('detecta geometria com ligação inexistente', () => {
  const result = validateGeometryDraft({
    elements: [{ id: 'A', type: 'BLOCK', width: 1, height: 1 }],
    edges: [{ id: 'E1', from: 'A', to: 'B' }]
  });
  assert.equal(result.valid, false);
  assert.match(result.errors.join(' '), /inexistente/);
});

test('calcula rota alternativa ignorando interdição', () => {
  const nodes = [{ id: 'A' }, { id: 'B' }, { id: 'C' }];
  const edges = [
    { id: 'AB', from: 'A', to: 'B', distance: 1 },
    { id: 'BC', from: 'B', to: 'C', distance: 1 },
    { id: 'AC', from: 'A', to: 'C', distance: 5 }
  ];
  const normal = shortestOperationalRoute(nodes, edges, 'A', 'C');
  const blocked = shortestOperationalRoute(nodes, edges, 'A', 'C', ['AB']);
  assert.deepEqual(normal.path, ['A', 'B', 'C']);
  assert.deepEqual(blocked.path, ['A', 'C']);
});

test('planeja Rail × Yard e informa conflito sem vagão', () => {
  const valid = planRailYardAssignments([{ id: 'U1', type: 'DRY', weight: 10 }], [{ id: 'V1', capacity: 1, tipoPermitido: 'DRY', maxWeight: 20 }]);
  assert.equal(valid.valid, true);
  assert.equal(valid.assignments[0].wagonId, 'V1');
  const invalid = planRailYardAssignments([{ id: 'U2', type: 'REEFER' }], [{ id: 'V1', capacity: 1, tipoPermitido: 'DRY' }]);
  assert.equal(invalid.valid, false);
  assert.equal(invalid.conflicts.length, 1);
});
