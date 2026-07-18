import assert from 'node:assert/strict';
import test from 'node:test';
import { clearSession, loadRuntimeConfig, saveSession } from './api.js';
import {
  buildGateReportQuery,
  buildInventoryQuery,
  inventoryReportsApi,
  selectGateReportRows,
  selectInventoryRows
} from './inventoryReportsApi.js';

function createStorage() {
  const values = new Map();
  return {
    getItem: (key) => values.has(key) ? values.get(key) : null,
    setItem: (key, value) => values.set(key, String(value)),
    removeItem: (key) => values.delete(key)
  };
}

function jwt(payload) {
  return `header.${Buffer.from(JSON.stringify(payload)).toString('base64url')}.signature`;
}

test.beforeEach(async () => {
  globalThis.localStorage = createStorage();
  clearSession();
  globalThis.fetch = async (url, options = {}) => {
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      });
    }
    return new Response(JSON.stringify({ url, method: options.method }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    });
  };
  await loadRuntimeConfig();
  saveSession({ token: jwt({ nome: 'Operador', roles: ['OPERADOR_GATE'], exp: Math.floor(Date.now() / 1000) + 3600 }) });
});

test('normaliza filtros do inventário', () => {
  assert.deepEqual(buildInventoryQuery({ codigo: ' abcd ', status: 'retido', tipoCarga: 'perigoso' }), {
    codigo: 'abcd',
    status: 'RETIDO',
    tipoCarga: 'PERIGOSO'
  });
});

test('consulta inventário usando o contrato consolidado', async () => {
  const response = await inventoryReportsApi.obterInventario({ codigo: 'ABCD', status: 'RETIDO' });
  assert.equal(response.url, 'http://localhost:8080/yard/inventario?codigo=ABCD&status=RETIDO');
  assert.equal(response.method, 'GET');
});

test('converte período do relatório do gate para limites completos do dia', async () => {
  assert.deepEqual(buildGateReportQuery({ inicio: '2026-07-01', fim: '2026-07-17', tipoOperacao: 'entrada' }), {
    inicio: '2026-07-01T00:00:00',
    fim: '2026-07-17T23:59:59',
    tipoOperacao: 'ENTRADA',
    transportadoraId: undefined
  });

  const response = await inventoryReportsApi.obterRelatorioGate({
    inicio: '2026-07-01',
    fim: '2026-07-17',
    tipoOperacao: 'ENTRADA'
  });
  assert.equal(response.url, 'http://localhost:8080/gate/relatorios?inicio=2026-07-01T00%3A00%3A00&fim=2026-07-17T23%3A59%3A59&tipoOperacao=ENTRADA');
});

test('seleciona somente coleções válidas das respostas', () => {
  const inventory = [{ identificador: 1 }];
  const appointments = [{ codigo: 'AG-1' }];
  assert.deepEqual(selectInventoryRows({ conteineres: inventory }), inventory);
  assert.deepEqual(selectInventoryRows({}), []);
  assert.deepEqual(selectGateReportRows({ agendamentos: appointments }), appointments);
  assert.deepEqual(selectGateReportRows(null), []);
});
