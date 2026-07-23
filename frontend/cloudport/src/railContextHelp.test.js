import assert from 'node:assert/strict';
import test from 'node:test';
import {
  RAIL_COMMON_BLOCKERS,
  RAIL_HELP_PATHS,
  RAIL_INSPECTION_STATES,
  RAIL_MANEUVER_STATES,
  RAIL_ORDER_STATES,
  RAIL_PERMISSIONS,
  RAIL_VISIT_STATES,
  resolveRailContextHelp
} from './railContextHelp.js';
import {
  isContextHelpCloseShortcut,
  isContextHelpOpenShortcut,
  isContextHelpTypingTarget
} from './contextHelpKeyboard.js';

const REQUIRED_PATHS = [
  '/home/ferrovia/visitas',
  '/home/ferrovia/visitas/importar',
  '/home/ferrovia/line-up',
  '/home/ferrovia/lista-trabalho',
  '/home/ferrovia/manobras-inspecoes',
  '/home/ferrovia/locomotivas'
];

test('fornece manual completo para toda rota ferroviária operacional', () => {
  assert.deepEqual([...RAIL_HELP_PATHS].sort(), [...REQUIRED_PATHS].sort());
  for (const path of REQUIRED_PATHS) {
    const help = resolveRailContextHelp(path, { currentRoles: ['ROLE_PLANEJADOR'] });
    assert.equal(help.module, 'Ferrovia');
    assert.ok(help.title.length > 3);
    assert.ok(help.purpose.length > 20);
    assert.ok(help.flow.length >= 4);
    assert.ok(help.fields.length >= 4);
    assert.ok(help.permissions.length >= 3);
    assert.ok(help.states.length >= 3);
    assert.ok(help.blockers.length >= 4);
    assert.ok(help.example.length > 20);
    assert.ok(help.shortcuts.some((item) => item.includes('F1')));
    assert.match(help.documentationUrl, /github\.com\/diogo2806\/CloudPort\/blob\/main\/docs\/manuais\//);
    assert.deepEqual(help.currentRoles, ['ROLE_PLANEJADOR']);
  }
});

test('reutiliza catálogos canônicos de permissões, estados e bloqueios', () => {
  const visits = resolveRailContextHelp('/home/ferrovia/visitas');
  const lineUp = resolveRailContextHelp('/home/ferrovia/line-up');
  const workList = resolveRailContextHelp('/home/ferrovia/lista-trabalho');
  const operations = resolveRailContextHelp('/home/ferrovia/manobras-inspecoes');

  for (const permission of RAIL_PERMISSIONS) assert.ok(visits.permissions.includes(permission));
  for (const state of RAIL_VISIT_STATES) assert.ok(lineUp.states.includes(state));
  for (const state of RAIL_ORDER_STATES) assert.ok(workList.states.includes(state));
  for (const state of RAIL_MANEUVER_STATES) assert.ok(operations.states.includes(state));
  for (const state of RAIL_INSPECTION_STATES) assert.ok(operations.states.includes(state));
  for (const blocker of RAIL_COMMON_BLOCKERS) assert.ok(workList.blockers.includes(blocker));
});

test('normaliza query, hash e barra final ao resolver o manual', () => {
  const help = resolveRailContextHelp('/home/ferrovia/line-up/?visita=10#operacao');
  assert.equal(help.path, '/home/ferrovia/line-up');
});

test('atalhos abrem fora de campos e Escape fecha a ajuda aberta', () => {
  assert.equal(isContextHelpOpenShortcut({ key: 'F1', target: { tagName: 'BUTTON' } }), true);
  assert.equal(isContextHelpOpenShortcut({ key: '?', shiftKey: true, target: { tagName: 'DIV' } }), true);
  assert.equal(isContextHelpOpenShortcut({ key: 'F1', target: { tagName: 'INPUT' } }), false);
  assert.equal(isContextHelpOpenShortcut({ key: '?', shiftKey: true, target: { tagName: 'TEXTAREA' } }), false);
  assert.equal(isContextHelpTypingTarget({ tagName: 'SELECT' }), true);
  assert.equal(isContextHelpTypingTarget({ tagName: 'DIV', isContentEditable: true }), true);
  assert.equal(isContextHelpCloseShortcut({ key: 'Escape' }, true), true);
  assert.equal(isContextHelpCloseShortcut({ key: 'Escape' }, false), false);
});

test('retorna null para rota fora do domínio ferroviário', () => {
  assert.equal(resolveRailContextHelp('/home/patio/mapa'), null);
});
