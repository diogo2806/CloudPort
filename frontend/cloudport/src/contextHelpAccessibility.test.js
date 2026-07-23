import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const source = readFileSync(new URL('./ContextHelp.jsx', import.meta.url), 'utf8');

test('botão de ajuda expõe estado e semântica de diálogo', () => {
  assert.match(source, /aria-expanded=\{open\}/);
  assert.match(source, /aria-haspopup="dialog"/);
  assert.match(source, /role="dialog"/);
  assert.match(source, /aria-modal="true"/);
  assert.match(source, /aria-labelledby="context-help-title"/);
});

test('abertura move foco para fechar e fechamento devolve foco ao gatilho', () => {
  assert.match(source, /if \(open\) closeRef\.current\?\.focus\(\)/);
  const returnFocusOccurrences = source.match(/triggerRef\.current\?\.focus\(\)/g) ?? [];
  assert.ok(returnFocusOccurrences.length >= 2);
});

test('drawer oferece fechamento por Escape e backdrop acessível', () => {
  assert.match(source, /isContextHelpCloseShortcut\(event, open\)/);
  assert.match(source, /aria-label="Fechar ajuda contextual"/);
  assert.match(source, /aria-label="Fechar ajuda"/);
});
