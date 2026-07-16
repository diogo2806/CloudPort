import assert from 'node:assert/strict';
import test, { afterEach, beforeEach } from 'node:test';
import { api, clearSession, saveSession, setRuntimeConfigForTests } from './api.js';

const originalFetch = globalThis.fetch;
const storage = new Map();
let requests = [];

function jwt(payload) {
  return `header.${Buffer.from(JSON.stringify(payload)).toString('base64url')}.signature`;
}

beforeEach(() => {
  storage.clear();
  requests = [];
  globalThis.sessionStorage = {
    getItem: (key) => storage.get(key) ?? null,
    setItem: (key, value) => storage.set(key, value),
    removeItem: (key) => storage.delete(key)
  };
  setRuntimeConfigForTests({ baseApiUrl: 'http://api.cloudport.test/' });
  globalThis.fetch = async (url, options = {}) => {
    requests.push({ url: String(url), options });
    return new Response(JSON.stringify({ sucesso: true }), {
      status: 200,
      headers: { 'content-type': 'application/json' }
    });
  };
  saveSession({
    token: jwt({ nome: 'Diogo', roles: ['ROLE_PLANEJADOR'] }),
    nome: 'Diogo',
    roles: ['ROLE_PLANEJADOR']
  });
});

afterEach(() => {
  clearSession();
  globalThis.fetch = originalFetch;
  delete globalThis.sessionStorage;
});

test('consulta painel operacional avançado', async () => {
  await api.obterControlRoom(77);
  assert.equal(requests[0].url, 'http://api.cloudport.test/visitas-navio/77/control-room');
  assert.equal(requests[0].options.method, 'GET');
  assert.match(requests[0].options.headers.get('Authorization'), /^Bearer /);
});

test('envia validação estrutural com rastreabilidade', async () => {
  await api.validarRestricoesEstruturais(81, {
    limitePesoPorPoraoToneladas: 2000,
    poroesInterditados: [4],
    regrasSegregacao: []
  });
  const request = requests[0];
  const body = JSON.parse(request.options.body);
  assert.equal(request.url, 'http://api.cloudport.test/visitas-navio/81/validacoes-estruturais');
  assert.equal(request.options.method, 'POST');
  assert.equal(body.usuario, 'Diogo');
  assert.equal(body.origemAcao, 'CONTROL_ROOM_NAVIO_PATIO');
  assert.equal(body.correlationId, request.options.headers.get('X-Correlation-Id'));
  assert.ok(request.options.headers.get('X-Trace-Id'));
  assert.match(request.options.headers.get('traceparent'), /^00-[a-f0-9]{32}-[a-f0-9]{16}-01$/);
});

test('aciona otimização global da visita', async () => {
  await api.otimizarOperacaoGlobal(91);
  assert.equal(requests[0].url, 'http://api.cloudport.test/visitas-navio/91/otimizacao-global');
  assert.equal(requests[0].options.method, 'POST');
});

test('consulta a última telemetria dos equipamentos', async () => {
  await api.listarTelemetriaEquipamentos();
  assert.equal(requests[0].url, 'http://api.cloudport.test/yard/patio/equipamentos/telemetria');
  assert.equal(requests[0].options.method, 'GET');
});
