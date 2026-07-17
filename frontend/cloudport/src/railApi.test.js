import assert from 'node:assert/strict';
import test from 'node:test';
import { clearSession, loadRuntimeConfig, saveSession } from './api.js';
import { railApi, railOrderTransitions } from './railApi.js';

function createStorage() {
  const values = new Map();
  return {
    getItem: (key) => values.has(key) ? values.get(key) : null,
    setItem: (key, value) => values.set(key, String(value)),
    removeItem: (key) => values.delete(key)
  };
}

function jwt(payload) {
  const encoded = Buffer.from(JSON.stringify(payload)).toString('base64url');
  return `header.${encoded}.signature`;
}

test.beforeEach(() => {
  globalThis.localStorage = createStorage();
  clearSession();
  saveSession({ token: jwt({ nome: 'Diogo', roles: ['OPERADOR_PATIO'], exp: Math.floor(Date.now() / 1000) + 3600 }) });
});

test('consulta ordens da visita pelo contrato real da lista de trabalho', async () => {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    return new Response(JSON.stringify([{ id: 9, statusMovimentacao: 'EM_EXECUCAO' }]), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };
  await loadRuntimeConfig();

  const orders = await railApi.listarOrdens(17, 'EM_EXECUCAO');

  assert.equal(calls.at(-1).url, 'http://localhost:8080/rail/ferrovia/lista-trabalho/visitas/17/ordens?status=EM_EXECUCAO');
  assert.equal(calls.at(-1).options.method, 'GET');
  assert.equal(orders[0].id, 9);
});

test('atualiza o status da ordem ferroviária com contexto operacional', async () => {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    return new Response(JSON.stringify({ id: 22, statusMovimentacao: 'CONCLUIDA' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };
  await loadRuntimeConfig();

  await railApi.atualizarStatusOrdem(17, 22, 'CONCLUIDA');

  const sent = calls.at(-1);
  const body = JSON.parse(sent.options.body);
  assert.equal(sent.url, 'http://localhost:8080/rail/ferrovia/lista-trabalho/visitas/17/ordens/22/status');
  assert.equal(sent.options.method, 'PATCH');
  assert.equal(body.statusMovimentacao, 'CONCLUIDA');
  assert.equal(body.usuario, 'Diogo');
  assert.equal(body.origemAcao, 'PORTAL_CLOUDPORT_REACT');
  assert.ok(body.correlationId);
});

test('registra a partida pelo contrato ferroviário dedicado', async () => {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    return new Response(JSON.stringify({ id: 17, statusVisita: 'PARTIU' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };
  await loadRuntimeConfig();

  await railApi.registrarPartida(17);

  const sent = calls.at(-1);
  assert.equal(sent.url, 'http://localhost:8080/rail/ferrovia/visitas/17/partida');
  assert.equal(sent.options.method, 'PATCH');
});

test('expõe somente as transições aceitas pelo domínio ferroviário', () => {
  assert.deepEqual(railOrderTransitions('PENDENTE'), ['EM_EXECUCAO', 'CONCLUIDA']);
  assert.deepEqual(railOrderTransitions('EM_EXECUCAO'), ['CONCLUIDA']);
  assert.deepEqual(railOrderTransitions('CONCLUIDA'), []);
});

test('rejeita identificadores e status inválidos antes da requisição', () => {
  assert.throws(() => railApi.listarOrdens(0), /inteiro positivo/i);
  assert.throws(() => railApi.atualizarStatusOrdem(1, 2, 'CANCELADA'), /status ferroviário inválido/i);
  assert.throws(() => railApi.registrarPartida(0), /inteiro positivo/i);
});
