import assert from 'node:assert/strict';
import test from 'node:test';
import { buildStacks } from './pages/yard/yardModel.js';
import {
  createMovePreview,
  enrichOperationalStacks,
  readYardWorkspaces,
  validateMoveTarget,
  writeYardWorkspaces
} from './pages/yard/yardOperationalModel.js';

const positions = [
  { id: 1, bloco: 'A1', linha: 1, coluna: 1, camadaOperacional: 'T1', ocupada: true, codigoConteiner: 'RF001', tipoCarga: 'REFRIGERADO', areaPermitida: true },
  { id: 2, bloco: 'A1', linha: 1, coluna: 1, camadaOperacional: 'T2', ocupada: false, areaPermitida: true },
  { id: 3, bloco: 'A1', linha: 1, coluna: 2, camadaOperacional: 'T1', ocupada: false, areaPermitida: true }
];

test('calcula ocupação, dwell e reefers por pilha', () => {
  const blocks = buildStacks(positions, []);
  const now = Date.parse('2026-07-17T12:00:00Z');
  const enriched = enrichOperationalStacks(blocks, [{ codigoConteiner: 'RF001', registradoEm: '2026-07-15T12:00:00Z' }], now);
  const stack = enriched[0].stacks[0];

  assert.equal(stack.occupancyPercent, 50);
  assert.equal(stack.maxDwellHours, 48);
  assert.equal(stack.reeferCount, 1);
});

test('simula movimento somente para posição realmente disponível', () => {
  const source = { ...positions[0], conteinerId: 77 };
  const target = positions[2];
  const preview = createMovePreview(source, target);

  assert.equal(preview.valid, true);
  assert.equal(preview.conteinerId, 77);
  assert.equal(preview.destino.coluna, 2);
  assert.match(validateMoveTarget(source, { ...target, interditada: true }), /interditada/i);
  assert.match(validateMoveTarget(source, { ...target, plannedOrder: { id: 9 } }), /reservada/i);
});

test('persiste workspaces normalizados e ignora conteúdo inválido', () => {
  const values = new Map();
  const storage = {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value)
  };

  writeYardWorkspaces(storage, [{ name: ' Operação A ', viewMode: 'scan', overlay: 'dwell', selectedBlock: 'A1', filters: { status: 'LIBERADO' } }]);
  const workspaces = readYardWorkspaces(storage);

  assert.equal(workspaces.length, 1);
  assert.equal(workspaces[0].name, 'Operação A');
  assert.equal(workspaces[0].viewMode, 'scan');
  assert.equal(workspaces[0].overlay, 'dwell');
  assert.equal(workspaces[0].filters.status, 'LIBERADO');
});
