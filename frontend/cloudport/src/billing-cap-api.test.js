import assert from 'node:assert/strict';
import test from 'node:test';
import { clearSession, loadRuntimeConfig, saveSession } from './api.js';
import {
  billingCapApi,
  buildBillingQuery,
  normalizeInvoicePayload,
  normalizeTariffPayload
} from './billingCapApi.js';

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
    return new Response(JSON.stringify({
      url,
      method: options.method,
      body: options.body ? JSON.parse(options.body) : null
    }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    });
  };
  await loadRuntimeConfig();
  saveSession({
    token: jwt({ nome: 'Administrador', roles: ['ADMIN_PORTO'], exp: Math.floor(Date.now() / 1000) + 3600 })
  });
});

test('normaliza filtros e contratos financeiros', () => {
  assert.deepEqual(buildBillingQuery({ transportadoraId: '12', status: ' aberta ' }), {
    transportadoraId: 12,
    status: 'ABERTA'
  });
  assert.deepEqual(normalizeTariffPayload({
    codigo: ' gate entrada ',
    descricao: ' Atendimento de entrada ',
    tipoOperacao: 'entrada',
    valor: '150.50',
    inicioVigencia: '2026-07-18',
    fimVigencia: '',
    ativa: true
  }), {
    codigo: 'GATE ENTRADA',
    descricao: 'Atendimento de entrada',
    tipoOperacao: 'ENTRADA',
    valor: 150.5,
    inicioVigencia: '2026-07-18',
    fimVigencia: null,
    ativa: true
  });
  assert.deepEqual(normalizeInvoicePayload({
    transportadoraId: '9',
    vencimento: '2026-07-30',
    cobrancaIds: ['1', 2, 0, null]
  }), {
    transportadoraId: 9,
    vencimento: '2026-07-30',
    cobrancaIds: [1, 2]
  });
});

test('usa os endpoints canônicos de Billing e CAP', async () => {
  const tariff = await billingCapApi.salvarTarifa({
    codigo: 'GATE_ENTRADA',
    descricao: 'Entrada',
    tipoOperacao: 'ENTRADA',
    valor: 100,
    inicioVigencia: '2026-07-18'
  });
  assert.equal(tariff.url, 'http://localhost:8080/billing/tarifas');
  assert.equal(tariff.method, 'POST');
  assert.equal(tariff.body.tipoOperacao, 'ENTRADA');

  const charges = await billingCapApi.listarCobrancas({ transportadoraId: 4, status: 'PENDENTE' });
  assert.equal(charges.url, 'http://localhost:8080/billing/cobrancas?transportadoraId=4&status=PENDENTE');

  const cap = await billingCapApi.consultarCap();
  assert.equal(cap.url, 'http://localhost:8080/cap/resumo');
  assert.equal(cap.method, 'GET');
});
