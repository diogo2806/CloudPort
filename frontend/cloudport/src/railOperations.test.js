import assert from 'node:assert/strict';
import test from 'node:test';
import { nextRailOrderStatus, sortRailOrders, summarizeRailOrders } from './railOperations.js';

test('define somente as transições progressivas da ordem ferroviária', () => {
  assert.equal(nextRailOrderStatus('PENDENTE'), 'EM_EXECUCAO');
  assert.equal(nextRailOrderStatus('EM_EXECUCAO'), 'CONCLUIDA');
  assert.equal(nextRailOrderStatus('CONCLUIDA'), null);
  assert.equal(nextRailOrderStatus('DESCONHECIDO'), null);
});

test('resume ordens ferroviárias por status', () => {
  assert.deepEqual(summarizeRailOrders([
    { statusMovimentacao: 'PENDENTE' },
    { statusMovimentacao: 'EM_EXECUCAO' },
    { statusMovimentacao: 'CONCLUIDA' },
    { statusMovimentacao: 'CONCLUIDA' }
  ]), { total: 4, pendentes: 1, emExecucao: 1, concluidas: 2 });
});

test('ordena ordens por prioridade operacional e código do contêiner', () => {
  const sorted = sortRailOrders([
    { codigoConteiner: 'ZZZ1', statusMovimentacao: 'CONCLUIDA' },
    { codigoConteiner: 'BBB1', statusMovimentacao: 'PENDENTE' },
    { codigoConteiner: 'AAA1', statusMovimentacao: 'PENDENTE' },
    { codigoConteiner: 'CCC1', statusMovimentacao: 'EM_EXECUCAO' }
  ]);

  assert.deepEqual(sorted.map((order) => order.codigoConteiner), ['AAA1', 'BBB1', 'CCC1', 'ZZZ1']);
});
