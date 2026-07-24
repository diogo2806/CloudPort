import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';

const apiSource = await readFile(new URL('./yardStowageWarningApi.js', import.meta.url), 'utf8');
const pageSource = await readFile(new URL('./pages/yard/YardStowageWarningsPage.jsx', import.meta.url), 'utf8');
const mapPageSource = await readFile(new URL('./pages/yard/YardMapPages.jsx', import.meta.url), 'utf8');
const mapComponentSource = await readFile(new URL('./pages/yard/GoogleYardMap.jsx', import.meta.url), 'utf8');
const yardModelSource = await readFile(new URL('./pages/yard/yardModel.js', import.meta.url), 'utf8');

function expectSource(source, pattern, message) {
  assert.match(source, pattern, message);
}

test('API cobre varredura, atribuição, correção, histórico e revalidação', () => {
  expectSource(apiSource, /const BASE = '\/api\/yard\/stowage-warnings'/, 'deve usar o endpoint canônico');
  expectSource(apiSource, /\/scan/, 'deve varrer o inventário');
  expectSource(apiSource, /\/assign/, 'deve atribuir responsável e prazo');
  expectSource(apiSource, /\/start-correction/, 'deve iniciar a correção');
  expectSource(apiSource, /\/submit-revalidation/, 'deve enviar para revalidação');
  expectSource(apiSource, /\/history/, 'deve consultar o histórico imutável');
});

test('fila operacional permite priorização e tratamento auditável', () => {
  expectSource(pageSource, /Avisos de estivagem/, 'deve identificar a fila');
  expectSource(pageSource, /Badges por bloco/, 'deve exibir badges por bloco');
  expectSource(pageSource, /Badges por pilha/, 'deve exibir badges por pilha');
  expectSource(pageSource, /Badges por posição/, 'deve exibir badges por posição');
  expectSource(pageSource, /Badges por unidade/, 'deve exibir badges por unidade');
  expectSource(pageSource, /Responsável/, 'deve filtrar e atribuir responsável');
  expectSource(pageSource, /Atrasados/, 'deve priorizar prazo vencido');
  expectSource(pageSource, /Revalidar condição/, 'deve exigir revalidação física');
  expectSource(pageSource, /Histórico auditável/, 'deve mostrar todas as transições');
  expectSource(pageSource, /Manual contextual/, 'deve oferecer instrução operacional completa');
});

test('mapa mostra volume crítico e badges sobre as pilhas', () => {
  expectSource(mapPageSource, /yardStowageWarningApi\.resumo\(\)/, 'deve carregar o resumo persistido');
  expectSource(mapPageSource, /avisosEstivagem/, 'deve associar badges às posições');
  expectSource(mapPageSource, /Avisos de estivagem \(/, 'deve abrir a fila a partir do mapa');
  expectSource(mapPageSource, /Planejamentos e dispatches incompatíveis estão bloqueados/, 'deve sinalizar bloqueio crítico');
  expectSource(yardModelSource, /stack\.avisosEstivagem \+=/, 'deve agregar avisos por pilha');
  expectSource(yardModelSource, /orderedStacks\.reduce/, 'deve agregar avisos por bloco');
  expectSource(mapComponentSource, /yard-google-stowage-badge/, 'deve renderizar o badge sobre a pilha');
  expectSource(mapComponentSource, /aviso\(s\) de estivagem/, 'deve detalhar os avisos no popup e na legenda');
});
