import assert from 'node:assert/strict';
import test from 'node:test';
import { api, clearSession, loadRuntimeConfig, normalizePage, saveSession } from './api.js';

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
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    return new Response(JSON.stringify({ url, method: options.method, body: options.body ?? null }), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };
  await loadRuntimeConfig();
  saveSession({ token: jwt({ nome: 'Diogo', roles: ['PLANEJADOR'], exp: Math.floor(Date.now() / 1000) + 3600 }) });
});

test('normaliza página EDI em português', () => {
  assert.deepEqual(normalizePage({ conteudo: [{ id: 1 }] }), [{ id: 1 }]);
});

test('cria plano sem poluir DTO com identidade operacional', async () => {
  const response = await api.criarPlanoVesselPlanner(12);
  assert.equal(response.url, 'http://localhost:8080/api/vessel-planner/planos');
  assert.deepEqual(JSON.parse(response.body), { bayPlanId: 12 });
});

test('reprocessamento EDI envia motivo e contexto operacional', async () => {
  const response = await api.reprocessarProcessamentoEdi(8, 'corrigir rejeição');
  const body = JSON.parse(response.body);
  assert.equal(body.motivo, 'corrigir rejeição');
  assert.equal(body.usuario, 'Diogo');
  assert.equal(body.origemAcao, 'PORTAL_CLOUDPORT_REACT');
  assert.ok(body.correlationId);
});

test('BAPLIE texto preserva corpo text/plain', async () => {
  const response = await api.enviarBaplieTexto("UNB+ABC'");
  assert.equal(response.body, "UNB+ABC'");
});

test('sequenciamento envia número de guindastes por query string', async () => {
  const response = await api.obterSequenciamentoGuindastes(5, 3);
  assert.equal(response.url, 'http://localhost:8080/api/vessel-planner/planos/5/sequenciamento-guindastes?numGuindastes=3');
});
