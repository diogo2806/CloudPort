import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const appSource = readFileSync(new URL('./App.jsx', import.meta.url), 'utf8');
const styleSource = readFileSync(new URL('./navigation.css', import.meta.url), 'utf8');
const helpSource = readFileSync(new URL('./ContextHelp.jsx', import.meta.url), 'utf8');

test('grupos e menu móvel expõem estado expandido', () => {
  assert.match(appSource, /className="nav-group-toggle" aria-expanded=\{expanded\} aria-controls=\{groupId\}/);
  assert.match(appSource, /aria-label=\{mobileMenu \? 'Fechar menu' : 'Abrir menu'\} aria-expanded=\{mobileMenu\}/);
});

test('grupos do menu podem ser recolhidos mesmo quando contêm a tela ativa', () => {
  assert.match(appSource, /const expanded = Boolean\(searchQuery\) \|\| openGroups\.includes\(group\.group\);/);
  assert.doesNotMatch(appSource, /openGroups\.includes\(group\.group\) \|\| activeItem\?\.group === group\.group/);
  assert.match(appSource, /hidden=\{!expanded\}/);
});

test('itens e favoritos possuem nomes acessíveis e estado atual', () => {
  assert.match(appSource, /aria-current=\{active \? 'page' : undefined\}/);
  assert.match(appSource, /aria-label=\{`Abrir \$\{item\.label\}`\}/);
  assert.match(appSource, /aria-pressed=\{favorite\}/);
  assert.match(appSource, /nos favoritos/);
});

test('busca global possui rótulo, atalho e ignora campos em edição', () => {
  assert.match(appSource, /Buscar telas e comandos/);
  assert.match(appSource, /event\.ctrlKey \|\| event\.metaKey/);
  assert.match(appSource, /\['input', 'select', 'textarea'\]\.includes\(tag\)/);
  assert.match(appSource, /searchRef\.current\?\.focus\(\)/);
  assert.match(helpSource, /Ctrl \+ K ou Command \+ K/);
});

test('layout inclui tablet, celular e preferência de movimento reduzido', () => {
  assert.match(styleSource, /@media \(max-width: 1050px\)/);
  assert.match(styleSource, /@media \(max-width: 720px\)/);
  assert.match(styleSource, /@media \(prefers-reduced-motion: reduce\)/);
  assert.match(styleSource, /width: min\(340px, calc\(100vw - 42px\)\)/);
});

test('rodapé não expõe versões técnicas do portal', () => {
  assert.doesNotMatch(appSource, /React 19 · Vite 8/);
  assert.match(appSource, /Busca: Ctrl \+ K · Ajuda: F1/);
});
