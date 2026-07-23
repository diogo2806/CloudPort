import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const source = readFileSync(new URL('./OperationalCockpit.jsx', import.meta.url), 'utf8');
const style = readFileSync(new URL('./operational-cockpit.css', import.meta.url), 'utf8');
const help = readFileSync(new URL('./cockpitContextHelp.js', import.meta.url), 'utf8');

test('carrega somente fontes permitidas e mantém falhas isoladas', () => {
  assert.match(source, /definitions\.map\(\(definition\) => definition\.loader\)/);
  assert.match(source, /Promise\.allSettled/);
  assert.match(source, /settled\.every\(\(result\) => result\.status === 'rejected'\)/);
  assert.match(source, /Fonte temporariamente indisponível/);
  assert.match(source, /Tentar novamente/);
});

test('oferece drill-down, atualização manual e automática', () => {
  assert.match(source, /Abrir lista correspondente/);
  assert.match(source, /onOpen\(block\.route\)/);
  assert.match(source, /Atualizar agora/);
  assert.match(source, /setInterval\(load, preferences\.refreshSeconds \* 1000\)/);
  assert.match(source, /Dados desatualizados/);
});

test('personalização informa estado e ações nomeadas', () => {
  assert.match(source, /aria-pressed=\{personalizing\}/);
  assert.match(source, /Mover \$\{block\.title\} para cima/);
  assert.match(source, /Mover \$\{block\.title\} para baixo/);
  assert.match(source, /Restaurar padrão do perfil/);
  assert.match(source, /Blocos ocultos/);
});

test('gráficos possuem descrição e tabela equivalente', () => {
  assert.match(source, /role="img" aria-label=\{`Distribuição de/);
  assert.match(source, /cockpit-distribution-table/);
  assert.match(source, /Dados equivalentes do gráfico/);
  assert.match(source, /Estado/);
  assert.match(source, /Quantidade/);
});

test('layout cobre desktop tablet celular e movimento reduzido', () => {
  assert.match(style, /grid-template-columns: repeat\(2, minmax\(0, 1fr\)\)/);
  assert.match(style, /@media \(max-width: 1050px\)/);
  assert.match(style, /@media \(max-width: 780px\)/);
  assert.match(style, /@media \(max-width: 520px\)/);
  assert.match(style, /@media \(prefers-reduced-motion: reduce\)/);
});

test('ajuda contextual possui todas as seções obrigatórias', () => {
  for (const field of ['purpose', 'flow', 'fields', 'permissions', 'states', 'blockers', 'example', 'shortcuts', 'documentationUrl']) {
    assert.match(help, new RegExp(`${field}:`));
  }
});
