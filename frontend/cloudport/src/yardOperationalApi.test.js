import assert from 'node:assert/strict';
import test from 'node:test';
import { clearSession, loadRuntimeConfig, saveSession } from './api.js';
import { yardOperationalApi } from './pages/yard/yardOperationalApi.js';

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

test.beforeEach(() => {
  globalThis.localStorage = createStorage();
  clearSession();
  saveSession({ token: jwt({ nome: 'Diogo', roles: ['PLANEJADOR'], exp: Math.floor(Date.now() / 1000) + 3600 }) });
});

test('replaneja allocation com destino e motivo explícitos', async () => {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    return new Response(JSON.stringify({ id: 91 }), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };
  await loadRuntimeConfig();

  await yardOperationalApi.replanejarAllocation(91, { linha: 4, coluna: 7, camadaOperacional: 'T2' }, 'Balanceamento do bloco');
  const sent = calls.at(-1);
  const body = JSON.parse(sent.options.body);

  assert.equal(sent.url, 'http://localhost:8080/yard/patio/work-instructions/91/allocation');
  assert.equal(sent.options.method, 'PATCH');
  assert.equal(body.linhaDestino, 4);
  assert.equal(body.colunaDestino, 7);
  assert.equal(body.camadaDestino, 'T2');
  assert.equal(body.motivo, 'Balanceamento do bloco');
});

test('consulta telemetria reefer pelo endpoint operacional', async () => {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    return new Response(JSON.stringify([]), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };
  await loadRuntimeConfig();

  await yardOperationalApi.listarTelemetriaReefers();
  assert.equal(calls.at(-1).url, 'http://localhost:8080/yard/patio/reefers/telemetria');
  assert.equal(calls.at(-1).options.method, 'GET');
});
