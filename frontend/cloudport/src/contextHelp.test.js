import assert from 'node:assert/strict';
import test from 'node:test';
import {
  buildHelpSections,
  filterHelpSections,
  normalizeHelpPath,
  resolveContextHelp
} from './contextHelp.js';

test('normaliza rota e remove query, hash e barra final', () => {
  assert.equal(normalizeHelpPath('/home/patio/mapa/?bloco=A#topo'), '/home/patio/mapa');
  assert.equal(normalizeHelpPath('/home'), '/home/dashboard');
});

test('combina ajuda do módulo com conteúdo específico da página', () => {
  const help = resolveContextHelp('/home/ferrovia/line-up', { roles: ['ROLE_PLANEJADOR'] });
  assert.equal(help.module, 'Ferrovia');
  assert.equal(help.title, 'Line-up ferroviário');
  assert.match(help.purpose, /sequência dos trens/i);
  assert.ok(help.permissions.some((item) => item.includes('PLANEJADOR')));
  assert.deepEqual(help.currentRoles, ['ROLE_PLANEJADOR']);
});

test('fornece ajuda padrão para uma rota dinâmica desconhecida', () => {
  const help = resolveContextHelp('/home/patio/processo/123');
  assert.equal(help.module, 'Pátio');
  assert.ok(help.flow.length > 0);
  assert.equal(help.processPath, '/home/patio/mapa');
});

test('monta e filtra seções sem diferenciar acentos', () => {
  const sections = buildHelpSections(resolveContextHelp('/home/alertas'));
  const filtered = filterHelpSections(sections, 'permissões');
  assert.ok(filtered.some((section) => section.id === 'permissions'));
  assert.ok(filtered.every((section) => section.items.length > 0));

  const states = filterHelpSections(sections, 'estados');
  assert.ok(states.some((section) => section.id === 'states'));
});
