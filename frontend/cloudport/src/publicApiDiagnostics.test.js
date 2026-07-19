import assert from 'node:assert/strict';
import test from 'node:test';
import {
  PUBLIC_API_CLIENT_ID_HEADER,
  PUBLIC_API_CLIENT_SECRET_HEADER,
  PUBLIC_API_CONTRACTS,
  buildDiagnosticsHeaders,
  buildPublicApiPath,
  buildPublicApiQuery,
  findPublicApiContract
} from './publicApiDiagnostics.js';

test('mantém o catálogo alinhado aos contratos GET de /api/public/v1', () => {
  const caminhos = PUBLIC_API_CONTRACTS.map((contract) => contract.caminho);
  assert.deepEqual(caminhos, [
    '/api/public/v1/vessel-visits',
    '/api/public/v1/vessel-visits/{id}',
    '/api/public/v1/vessel-visits/{id}/stow-plan',
    '/api/public/v1/vessel-visits/{id}/yard-orders',
    '/api/public/v1/vessel-visits/{id}/work-queues',
    '/api/public/v1/vessel-visits/{id}/work-queues/{workQueueId}',
    '/api/public/v1/vessel-visits/{id}/events',
    '/api/public/v1/yard/orders',
    '/api/public/v1/yard/reservations'
  ]);
  assert.ok(PUBLIC_API_CONTRACTS.every((contract) => contract.metodo === 'GET'));
});

test('encontra contratos pelo identificador', () => {
  assert.equal(findPublicApiContract('yard-orders')?.caminho, '/api/public/v1/yard/orders');
  assert.equal(findPublicApiContract('inexistente'), null);
});

test('resolve parâmetros de rota validando identificadores numéricos', () => {
  const contract = findPublicApiContract('vessel-visit-work-queue-detalhe');
  assert.equal(
    buildPublicApiPath(contract, { id: '4', workQueueId: '9' }),
    '/api/public/v1/vessel-visits/4/work-queues/9'
  );
  assert.throws(() => buildPublicApiPath(contract, { id: 'abc', workQueueId: '9' }), /identificador numérico/);
  assert.throws(() => buildPublicApiPath(null, {}), /Selecione um contrato/);
});

test('monta a query exigindo os parâmetros obrigatórios do contrato', () => {
  const contract = findPublicApiContract('yard-orders');
  assert.deepEqual(
    buildPublicApiQuery(contract, { visitaNavioId: '3', status: 'ATIVA', pagina: '', tamanho: '10' }),
    { visitaNavioId: '3', status: 'ATIVA', tamanho: '10' }
  );
  assert.throws(() => buildPublicApiQuery(contract, {}), /"visitaNavioId" é obrigatório/);
});

test('monta os headers de credenciais do cliente sem aceitar valores vazios', () => {
  assert.deepEqual(buildDiagnosticsHeaders(' portal-parceiro ', 'segredo'), {
    [PUBLIC_API_CLIENT_ID_HEADER]: 'portal-parceiro',
    [PUBLIC_API_CLIENT_SECRET_HEADER]: 'segredo'
  });
  assert.throws(() => buildDiagnosticsHeaders('', 'segredo'), /client id e o client secret/);
  assert.throws(() => buildDiagnosticsHeaders('cliente', '   '), /client id e o client secret/);
});
