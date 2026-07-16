import assert from 'node:assert/strict';
import test, { afterEach, beforeEach } from 'node:test';
import { api, clearSession, formatError, hasAnyRole, saveSession, setRuntimeConfigForTests } from './api.js';

const originalFetch = globalThis.fetch;
const storage = new Map();
let requests = [];

function jwt(payload) {
  const encoded = Buffer.from(JSON.stringify(payload)).toString('base64url');
  return `header.${encoded}.signature`;
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
});

afterEach(() => {
  clearSession();
  globalThis.fetch = originalFetch;
  delete globalThis.sessionStorage;
});

test('normaliza roles recebidas no controle de acesso', () => {
  assert.equal(hasAnyRole({ roles: ['ROLE_PLANEJADOR'] }, 'PLANEJADOR'), true);
  assert.equal(hasAnyRole({ roles: ['ROLE_VISUALIZADOR'] }, 'OPERADOR_GATE'), false);
});

test('preserva código e correlationId nas mensagens da API', () => {
  const mensagem = formatError({
    payload: {
      mensagem: 'Operação recusada',
      codigo: 'YARD-409',
      correlationId: 'abc-123'
    }
  });
  assert.match(mensagem, /Operação recusada \[YARD-409\]/);
  assert.match(mensagem, /correlationId: abc-123/);
});

test('consulta work queues pelo contrato da visita e envia JWT', async () => {
  saveSession({
    token: jwt({ nome: 'Diogo', roles: ['PLANEJADOR'] }),
    nome: 'Diogo',
    roles: ['PLANEJADOR']
  });

  await api.listarWorkQueuesPatio(42);

  assert.equal(requests.length, 1);
  assert.equal(requests[0].url, 'http://api.cloudport.test/visitas-navio/42/integracao-patio/work-queues');
  assert.equal(requests[0].options.method, 'GET');
  assert.match(requests[0].options.headers.get('Authorization'), /^Bearer /);
  assert.ok(requests[0].options.headers.get('X-Correlation-Id'));
});

test('enriquece dispatch com usuário, origem, operador e correlationId', async () => {
  saveSession({
    token: jwt({ nome: 'Diogo', roles: ['ROLE_PLANEJADOR'] }),
    nome: 'Diogo',
    roles: ['ROLE_PLANEJADOR']
  });

  await api.despacharWorkQueuePatio(9, { limiteOrdens: 3, observacao: 'teste' });

  assert.equal(requests[0].url, 'http://api.cloudport.test/yard/patio/work-queues/9/dispatch');
  assert.equal(requests[0].options.method, 'POST');
  const body = JSON.parse(requests[0].options.body);
  assert.equal(body.limiteOrdens, 3);
  assert.equal(body.usuario, 'Diogo');
  assert.equal(body.operador, 'Diogo');
  assert.equal(body.origemAcao, 'CONTROL_ROOM_NAVIO_PATIO');
  assert.equal(body.correlationId, requests[0].options.headers.get('X-Correlation-Id'));
});

test('não envia sessão anterior nem metadados operacionais no login', async () => {
  saveSession({
    token: jwt({ nome: 'Anterior', roles: ['ROLE_PLANEJADOR'] }),
    nome: 'Anterior',
    roles: ['ROLE_PLANEJADOR']
  });

  await api.autenticar('diogo', 'senha-segura');

  assert.equal(requests[0].url, 'http://api.cloudport.test/auth/login');
  assert.equal(requests[0].options.headers.get('Authorization'), null);
  assert.equal(requests[0].options.headers.get('X-Correlation-Id'), null);
  assert.deepEqual(JSON.parse(requests[0].options.body), { login: 'diogo', senha: 'senha-segura' });
});

test('consulta o painel avançado e o Quay Monitor da visita', async () => {
  saveSession({
    token: jwt({ nome: 'Diogo', roles: ['ROLE_PLANEJADOR'] }),
    nome: 'Diogo',
    roles: ['ROLE_PLANEJADOR']
  });

  await api.obterControlRoom(77);
  await api.obterQuayMonitor(77);

  assert.equal(requests[0].url, 'http://api.cloudport.test/visitas-navio/77/control-room');
  assert.equal(requests[1].url, 'http://api.cloudport.test/visitas-navio/77/quay-monitor');
  assert.equal(requests[0].options.method, 'GET');
  assert.equal(requests[1].options.method, 'GET');
});

test('envia configuração estrutural e comando de otimização global', async () => {
  saveSession({
    token: jwt({ nome: 'Diogo', roles: ['ROLE_PLANEJADOR'] }),
    nome: 'Diogo',
    roles: ['ROLE_PLANEJADOR']
  });
  const configuracao = {
    limitePesoPorPoraoToneladas: 2000,
    desequilibrioBombordoBoresteMaximoPercentual: 8,
    poroesInterditados: [4]
  };

  await api.validarRestricoesEstruturais(81, configuracao);
  await api.otimizarOperacaoGlobal(81);

  assert.equal(requests[0].url, 'http://api.cloudport.test/visitas-navio/81/validacoes-estruturais');
  assert.deepEqual(JSON.parse(requests[0].options.body), {
    ...configuracao,
    usuario: 'Diogo',
    origemAcao: 'CONTROL_ROOM_NAVIO_PATIO',
    correlationId: requests[0].options.headers.get('X-Correlation-Id')
  });
  assert.equal(requests[1].url, 'http://api.cloudport.test/visitas-navio/81/otimizacao-global');
  assert.equal(requests[1].options.method, 'POST');
});
