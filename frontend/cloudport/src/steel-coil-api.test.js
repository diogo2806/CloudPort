import assert from 'node:assert/strict';
import test from 'node:test';
import { clearSession, loadRuntimeConfig, saveSession } from './api.js';
import { steelCoilApi } from './steelCoilApi.js';

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
  saveSession({
    token: jwt({ nome: 'Planejador', roles: ['PLANEJADOR'], exp: Math.floor(Date.now() / 1000) + 3600 })
  });
});

test('lista planos bulk pelo identificador canônico da visita', async () => {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      });
    }
    return new Response(JSON.stringify([]), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };

  await loadRuntimeConfig();
  await steelCoilApi.listarPlanos(7, 91);

  const sent = calls.at(-1);
  assert.equal(sent.url, 'http://localhost:8080/api/estivagem-bulk/planos?navioId=7&visitaNavioId=91');
  assert.equal(sent.options.method, 'GET');
});

test('cria plano bulk enviando visitaNavioId obrigatório', async () => {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      });
    }
    return new Response(JSON.stringify({ id: 41 }), { status: 201, headers: { 'Content-Type': 'application/json' } });
  };

  await loadRuntimeConfig();
  await steelCoilApi.criarPlano({
    navioId: 7,
    visitaNavioId: 91,
    codigoViagem: 'V001',
    portoCarga: 'BRITG',
    portoDescarga: 'NLRTM'
  });

  const sent = calls.at(-1);
  assert.equal(sent.options.method, 'POST');
  assert.deepEqual(JSON.parse(sent.options.body), {
    navioId: 7,
    visitaNavioId: 91,
    codigoViagem: 'V001',
    portoCarga: 'BRITG',
    portoDescarga: 'NLRTM'
  });
});

test('consulta tacktop por GET sem corpo', async () => {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      });
    }
    return new Response(JSON.stringify({ numeroBobinasTopLayer: 2 }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    });
  };

  await loadRuntimeConfig();
  await steelCoilApi.calcularSecuring(41);

  const sent = calls.at(-1);
  assert.equal(sent.url, 'http://localhost:8080/api/estivagem-bulk/planos/41/tacktop');
  assert.equal(sent.options.method, 'GET');
  assert.equal(sent.options.body, undefined);
});
