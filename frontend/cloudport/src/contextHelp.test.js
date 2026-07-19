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

test('fornece manual operacional completo para unidades não localizadas', () => {
  const help = resolveContextHelp('/home/patio/lost-found', { perfil: 'ROLE_OPERADOR_PATIO' });
  assert.equal(help.module, 'Pátio');
  assert.equal(help.title, 'Unidades de carga não localizadas');
  assert.match(help.purpose, /unidades sem registro/i);
  assert.ok(help.flow.some((item) => item.includes('Associe')));
  assert.ok(help.fields.some((item) => item.includes('Tipo do caso')));
  assert.ok(help.permissions.some((item) => item.includes('OPERADOR_PATIO')));
  assert.ok(help.states.some((item) => item.startsWith('ENCERRADO')));
  assert.ok(help.blockers.some((item) => item.includes('Regularização sem unidade associada')));
  assert.match(help.example, /SEM_REGISTRO/);
  assert.equal(help.processPath, '/home/patio/inventario');
  assert.deepEqual(help.currentRoles, ['ROLE_OPERADOR_PATIO']);
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
