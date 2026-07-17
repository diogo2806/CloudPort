import assert from 'node:assert/strict';
import test from 'node:test';
import { clearSession, loadRuntimeConfig, saveSession } from './api.js';
import { railApi } from './railApi.js';

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

test('consulta ordens ferroviárias pelo status publicado no backend', async () => {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    return new Response(JSON.stringify([]), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };
  await loadRuntimeConfig();

  await railApi.listarOrdens(19, 'EM_EXECUCAO');

  assert.equal(calls.at(-1).url, 'http://localhost:8080/rail/ferrovia/lista-trabalho/visitas/19/ordens?status=EM_EXECUCAO');
  assert.equal(calls.at(-1).options.method, 'GET');
});

test('avança a ordem ferroviária pelo contrato PATCH real', async () => {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    return new Response(JSON.stringify({ id: 8, statusMovimentacao: 'CONCLUIDA' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };
  await loadRuntimeConfig();

  await railApi.atualizarStatusOrdem(19, 8, 'CONCLUIDA');
  const sent = calls.at(-1);
  const body = JSON.parse(sent.options.body);

  assert.equal(sent.url, 'http://localhost:8080/rail/ferrovia/lista-trabalho/visitas/19/ordens/8/status');
  assert.equal(sent.options.method, 'PATCH');
  assert.equal(body.statusMovimentacao, 'CONCLUIDA');
  assert.equal(body.usuario, 'Diogo');
  assert.equal(body.origemAcao, 'PORTAL_CLOUDPORT_REACT');
  assert.ok(body.correlationId);
});
