import assert from 'node:assert/strict';
import test from 'node:test';
import {
  EDI_MESSAGE_TYPES,
  EDI_PROCESSING_STATUSES,
  buildEdiListQuery,
  normalizeEdiFilters,
  normalizeEdiPage,
  normalizeEdiProcessing,
  summarizeEdiProcessings,
  validateReprocessReason
} from './ediMonitor.js';

test('mantém os contratos de tipos e status alinhados ao backend', () => {
  assert.deepEqual(EDI_MESSAGE_TYPES, ['BAPLIE', 'COPRAR', 'COARRI', 'VERMAS']);
  assert.deepEqual(EDI_PROCESSING_STATUSES, [
    'RECEBIDO',
    'PROCESSANDO',
    'AGUARDANDO_REPROCESSAMENTO',
    'CONCLUIDO',
    'REJEITADO',
    'QUARENTENA'
  ]);
});

test('normaliza filtros inválidos para valores seguros', () => {
  assert.deepEqual(normalizeEdiFilters({ tipo: 'INVALIDO', status: 'X', pagina: -3, tamanho: 9999 }), {
    tipo: '',
    status: '',
    pagina: 0,
    tamanho: 50
  });
  assert.deepEqual(normalizeEdiFilters({ tipo: 'COARRI', status: 'QUARENTENA', pagina: 2, tamanho: 25 }), {
    tipo: 'COARRI',
    status: 'QUARENTENA',
    pagina: 2,
    tamanho: 25
  });
});

test('monta a query de listagem apenas com filtros preenchidos', () => {
  assert.deepEqual(buildEdiListQuery({}), { pagina: 0, tamanho: 50 });
  assert.deepEqual(buildEdiListQuery({ tipo: 'BAPLIE', status: 'REJEITADO', pagina: 1, tamanho: 25 }), {
    tipo: 'BAPLIE',
    status: 'REJEITADO',
    pagina: 1,
    tamanho: 25
  });
});

test('normaliza um processamento EDI com rótulo de status e flag de reprocessamento', () => {
  const row = normalizeEdiProcessing({
    id: 7,
    tipoMensagem: 'baplie',
    status: 'quarentena',
    codigoNavio: 'MSC LORETO',
    correlationId: 'corr-1',
    tentativa: 3,
    bayPlanId: 12
  });
  assert.equal(row.id, 7);
  assert.equal(row.tipoMensagem, 'BAPLIE');
  assert.equal(row.status, 'QUARENTENA');
  assert.equal(row.statusDescricao, 'Quarentena');
  assert.equal(row.reprocessavel, true);
  assert.equal(row.bayPlanId, 12);
  assert.equal(normalizeEdiProcessing({ id: 5, status: 'CONCLUIDO' }).reprocessavel, false);
  assert.equal(normalizeEdiProcessing({ id: 'abc' }), null);
  assert.equal(normalizeEdiProcessing(null), null);
});

test('normaliza a página de processamentos preservando a paginação do backend', () => {
  const page = normalizeEdiPage({
    conteudo: [{ id: 1, status: 'CONCLUIDO' }, { id: null }],
    pagina: 2,
    tamanho: 25,
    totalElementos: 51,
    totalPaginas: 3,
    primeira: false,
    ultima: true
  });
  assert.equal(page.rows.length, 1);
  assert.equal(page.pagina, 2);
  assert.equal(page.totalElementos, 51);
  assert.equal(page.totalPaginas, 3);
  assert.equal(page.primeira, false);
  assert.equal(page.ultima, true);
  const empty = normalizeEdiPage(null);
  assert.deepEqual(empty.rows, []);
  assert.equal(empty.totalPaginas, 1);
});

test('resume os processamentos por situação operacional', () => {
  const summary = summarizeEdiProcessings([
    { status: 'CONCLUIDO' },
    { status: 'CONCLUIDO' },
    { status: 'REJEITADO' },
    { status: 'QUARENTENA' },
    { status: 'AGUARDANDO_REPROCESSAMENTO' },
    { status: 'PROCESSANDO' },
    null
  ]);
  assert.deepEqual(summary, {
    total: 6,
    concluidos: 2,
    rejeitados: 1,
    quarentena: 1,
    emProcessamento: 1,
    aguardandoReprocessamento: 1
  });
});

test('exige motivo operacional mínimo para reprocessar', () => {
  assert.throws(() => validateReprocessReason('abc'), /pelo menos 5 caracteres/);
  assert.equal(validateReprocessReason('  Reprocessar após correção do manifesto  '), 'Reprocessar após correção do manifesto');
});
