import assert from 'node:assert/strict';
import test from 'node:test';
import {
  COCKPIT_DEFINITIONS,
  applyCockpitPreferences,
  buildCockpitBlock,
  cockpitStorageKey,
  createCockpitSnapshot,
  defaultCockpitPreferences,
  isCockpitStale,
  moveCockpitBlock,
  permittedCockpitDefinitions,
  readCockpitStorage,
  sanitizeCockpitPreferences,
  toggleCockpitBlock,
  trendFrom,
  writeCockpitStorage
} from './operationalCockpit.js';

test('seleciona blocos conforme os papéis sem consultar módulos indevidos', () => {
  const yardOperator = permittedCockpitDefinitions({ roles: ['ROLE_OPERADOR_PATIO'] }).map((item) => item.key);
  assert.deepEqual(yardOperator, ['alerts', 'yard', 'rail', 'equipment']);

  const gateOperator = permittedCockpitDefinitions({ perfil: 'OPERADOR_GATE' }).map((item) => item.key);
  assert.deepEqual(gateOperator, ['alerts', 'gate', 'vessel', 'equipment']);

  const planner = permittedCockpitDefinitions({ roles: ['PLANEJADOR'] }).map((item) => item.key);
  assert.deepEqual(planner, ['alerts', 'gate', 'yard', 'rail', 'vessel', 'equipment', 'edi']);
});

test('calcula filas e atrasos do Gate sem duplicar registros', () => {
  const definition = COCKPIT_DEFINITIONS.find((item) => item.key === 'gate');
  const block = buildCockpitBlock(definition, { payload: { agendamentos: [
    { id: 1, status: 'EM_FILA', tempoFilaMinutos: 45 },
    { id: 2, status: 'AGENDADO', tempoFilaMinutos: 5 },
    { id: 3, status: 'FINALIZADO', tempoFilaMinutos: 60 }
  ] } }, undefined, '2026-07-23T10:00:00Z');
  assert.equal(block.value, 3);
  assert.equal(block.total, 3);
  assert.equal(block.state, 'attention');
  assert.match(block.detail, /2 aguardando/);
  assert.match(block.detail, /2 com atraso ou retenção/);
});

test('calcula ordens do pátio bloqueadas e pendentes', () => {
  const definition = COCKPIT_DEFINITIONS.find((item) => item.key === 'yard');
  const block = buildCockpitBlock(definition, { payload: [
    { status: 'BLOQUEADA' },
    { status: 'SUSPENSA' },
    { status: 'PENDENTE' },
    { status: 'CONCLUIDA' }
  ] }, 5);
  assert.equal(block.value, 3);
  assert.equal(block.attention, 2);
  assert.equal(block.state, 'attention');
  assert.equal(block.trend.label, '-2 desde a leitura anterior');
  assert.equal(block.trend.favorable, true);
});

test('isola erro parcial em um único bloco', () => {
  const definition = COCKPIT_DEFINITIONS.find((item) => item.key === 'edi');
  const block = buildCockpitBlock(definition, { status: 'rejected', error: new Error('EDI offline') });
  assert.equal(block.state, 'error');
  assert.equal(block.value, null);
  assert.match(block.error, /EDI offline/);
});

test('tendência informa direção e favorabilidade conforme o tipo da métrica', () => {
  assert.deepEqual(trendFrom(10, undefined, true), { direction: 'new', label: 'Primeira leitura', favorable: null });
  assert.deepEqual(trendFrom(10, 10, true), { direction: 'stable', label: 'Sem alteração', favorable: true });
  assert.deepEqual(trendFrom(12, 10, true), { direction: 'up', label: '+2 desde a leitura anterior', favorable: false });
  assert.deepEqual(trendFrom(8, 10, true), { direction: 'down', label: '-2 desde a leitura anterior', favorable: true });
  assert.deepEqual(trendFrom(12, 10, false), { direction: 'up', label: '+2 desde a leitura anterior', favorable: true });
});

test('sanitiza, move, oculta e restaura preferências permitidas', () => {
  const definitions = COCKPIT_DEFINITIONS.slice(0, 3);
  const defaults = defaultCockpitPreferences(definitions);
  assert.deepEqual(defaults.order, ['alerts', 'gate', 'yard']);

  const sanitized = sanitizeCockpitPreferences({ order: ['yard', 'segredo'], hidden: ['gate', 'segredo'], refreshSeconds: 999 }, definitions);
  assert.deepEqual(sanitized.order, ['yard', 'alerts', 'gate']);
  assert.deepEqual(sanitized.hidden, ['gate']);
  assert.equal(sanitized.refreshSeconds, 60);

  const moved = moveCockpitBlock(sanitized, 'gate', -1, definitions);
  assert.deepEqual(moved.order, ['yard', 'gate', 'alerts']);

  const visibleAgain = toggleCockpitBlock(moved, 'gate', definitions);
  assert.deepEqual(visibleAgain.hidden, []);

  const blocks = definitions.map((definition) => ({ ...definition, value: 0 }));
  assert.deepEqual(applyCockpitPreferences(blocks, moved).map((block) => block.key), ['yard', 'gate', 'alerts']);
});

test('snapshot guarda valores comparáveis e horário', () => {
  const snapshot = createCockpitSnapshot([
    { key: 'alerts', value: 4 },
    { key: 'gate', value: 2 }
  ], '2026-07-23T10:00:00Z');
  assert.deepEqual(snapshot, { updatedAt: '2026-07-23T10:00:00Z', values: { alerts: 4, gate: 2 } });
});

test('persistência é isolada por usuário e tolerante a armazenamento inválido', () => {
  assert.notEqual(cockpitStorageKey({ id: 1 }), cockpitStorageKey({ id: 2 }));
  const values = new Map();
  const storage = {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value)
  };
  assert.equal(writeCockpitStorage(storage, 'prefs', { hidden: ['gate'] }), true);
  assert.deepEqual(readCockpitStorage(storage, 'prefs'), { hidden: ['gate'] });
  assert.equal(readCockpitStorage({ getItem: () => '{inválido' }, 'prefs', 'fallback'), 'fallback');
  assert.equal(writeCockpitStorage({ setItem: () => { throw new Error('quota'); } }, 'prefs', {}), false);
});

test('detecta dado desatualizado usando pelo menos sessenta segundos', () => {
  const now = new Date('2026-07-23T10:02:01Z').getTime();
  assert.equal(isCockpitStale('2026-07-23T10:02:00Z', 30, now), false);
  assert.equal(isCockpitStale('2026-07-23T10:00:00Z', 30, now), true);
  assert.equal(isCockpitStale(null, 60, now), true);
});
