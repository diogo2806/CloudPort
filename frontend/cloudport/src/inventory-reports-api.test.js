import assert from 'node:assert/strict';
import test from 'node:test';
import { clearSession, loadRuntimeConfig, saveSession } from './api.js';
import {
  buildCanonicalInventoryQuery,
  buildGateReportQuery,
  buildInventoryQuery,
  inventoryReportsApi,
  selectCanonicalInventoryRows,
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

test('normaliza filtros do inventário legado', () => {
  assert.deepEqual(buildInventoryQuery({ codigo: ' abcd ', status: 'retido', tipoCarga: 'perigoso' }), {
    codigo: 'abcd',
    status: 'RETIDO',
    tipoCarga: 'PERIGOSO'
  });
});

test('normaliza filtros do inventário canônico', () => {
  assert.deepEqual(buildCanonicalInventoryQuery({
    identificacao: ' abcd ',
    categoria: 'conteiner',
    estado: 'no_patio',
    condicao: 'operacional',
    proprietario: ' Armador ',
    operador: '',
    somenteComHold: true,
    somenteReefer: false
  }), {
    identificacao: 'abcd',
    categoria: 'CONTEINER',
    estado: 'NO_PATIO',
    condicao: 'OPERACIONAL',
    proprietario: 'Armador',
    operador: undefined,
    somenteComHold: true,
    somenteReefer: undefined
  });
});

test('consulta inventário canônico usando o contrato unificado', async () => {
  const response = await inventoryReportsApi.obterInventarioCanonico({
    identificacao: 'ABCD',
    categoria: 'conteiner',
    somenteComHold: true
  });
  assert.equal(response.url, 'http://localhost:8080/yard/inventario/canonico/unidades?identificacao=ABCD&categoria=CONTEINER&somenteComHold=true');
  assert.equal(response.method, 'GET');
});

test('consulta detalhe da unidade canônica', async () => {
  const response = await inventoryReportsApi.obterUnidadeInventario(19);
  assert.equal(response.url, 'http://localhost:8080/yard/inventario/canonico/unidades/19');
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
  const legacyInventory = [{ identificador: 1 }];
  const canonicalInventory = [{ id: 2, identificacao: 'ABCD1234567' }];
  const appointments = [{ codigo: 'AG-1' }];
  assert.deepEqual(selectInventoryRows({ conteineres: legacyInventory }), legacyInventory);
  assert.deepEqual(selectInventoryRows({}), []);
  assert.deepEqual(selectCanonicalInventoryRows({ unidades: canonicalInventory }), canonicalInventory);
  assert.deepEqual(selectCanonicalInventoryRows(null), []);
  assert.deepEqual(selectGateReportRows({ agendamentos: appointments }), appointments);
  assert.deepEqual(selectGateReportRows(null), []);
});
