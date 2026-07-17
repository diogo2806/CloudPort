import assert from 'node:assert/strict';
import test from 'node:test';
import {
  api,
  clearSession,
  formatError,
  hasAnyRole,
  loadRuntimeConfig,
  mapSession,
  normalizePage,
  normalizeRole,
  readSession,
  request,
  saveSession,
  sanitizeText,
  subscribeSessionExpired
} from './api.js';

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
});

test('normaliza papéis e identifica autorização', () => {
  assert.equal(normalizeRole('planejador'), 'ROLE_PLANEJADOR');
  assert.equal(hasAnyRole({ roles: ['ROLE_ADMIN_PORTO'] }, 'ADMIN_PORTO'), true);
  assert.equal(hasAnyRole({ roles: ['ROLE_VISUALIZADOR'] }, 'OPERADOR_GATE'), false);
});

test('mapeia e persiste sessão compatível com o portal legado', () => {
  const session = saveSession({ token: jwt({ userId: 7, nome: 'Diogo', roles: ['PLANEJADOR'], exp: Math.floor(Date.now() / 1000) + 3600 }) });
  assert.equal(session.nome, 'Diogo');
  assert.deepEqual(session.roles, ['ROLE_PLANEJADOR']);
  assert.equal(readSession().id, 7);
});

test('combina papéis do token e da resposta sem duplicação', () => {
  const session = mapSession({ token: jwt({ role: 'ADMIN_PORTO' }), roles: ['ROLE_ADMIN_PORTO', 'OPERADOR_GATE'] });
  assert.deepEqual(session.roles.sort(), ['ROLE_ADMIN_PORTO', 'ROLE_OPERADOR_GATE']);
});

test('sanitiza textos e normaliza coleções paginadas', () => {
  assert.equal(sanitizeText(' <b>Porto</b> '), 'bPorto/b');
  assert.deepEqual(normalizePage({ content: [{ id: 1 }] }), [{ id: 1 }]);
  assert.deepEqual(normalizePage([{ id: 2 }]), [{ id: 2 }]);
});

test('preserva código e correlationId nas mensagens de erro', () => {
  const message = formatError({ payload: { mensagem: 'Operação recusada', codigo: 'YARD-409', correlationId: 'abc-123' } });
  assert.match(message, /Operação recusada \[YARD-409\]/);
  assert.match(message, /correlationId: abc-123/);
});

test('cliente HTTP adiciona JWT, correlationId e contexto operacional', async () => {
  saveSession({ token: jwt({ nome: 'Diogo', roles: ['PLANEJADOR'], exp: Math.floor(Date.now() / 1000) + 3600 }) });
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    return new Response(JSON.stringify({ ok: true }), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };
  await loadRuntimeConfig();
  await request('/yard/patio/work-queues/1/dispatch', { method: 'POST', body: { limiteOrdens: 3 } });
  const sent = calls.at(-1);
  const body = JSON.parse(sent.options.body);
  assert.match(sent.options.headers.get('Authorization'), /^Bearer /);
  assert.ok(sent.options.headers.get('X-Correlation-Id'));
  assert.equal(body.usuario, 'Diogo');
  assert.equal(body.operador, 'Diogo');
  assert.equal(body.origemAcao, 'PORTAL_CLOUDPORT_REACT');
});

test('resposta 401 limpa a sessão e publica uma única expiração', async () => {
  saveSession({ token: jwt({ nome: 'Diogo', roles: ['PLANEJADOR'], exp: Math.floor(Date.now() / 1000) + 3600 }) });
  let expirationCount = 0;
  const unsubscribe = subscribeSessionExpired(() => { expirationCount += 1; });
  globalThis.fetch = async (url) => {
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    return new Response(JSON.stringify({ mensagem: 'Sessão expirada' }), { status: 401, headers: { 'Content-Type': 'application/json' } });
  };

  await loadRuntimeConfig();
  await assert.rejects(() => request('/api/roles'), (error) => error.status === 401);
  await assert.rejects(() => request('/api/roles'), (error) => error.status === 401);

  assert.equal(readSession(), null);
  assert.equal(expirationCount, 1);
  unsubscribe();
});

test('login envia a senha sem sanitização destrutiva', async () => {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    return new Response(JSON.stringify({ token: jwt({ roles: ['ADMIN_PORTO'] }) }), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };
  await loadRuntimeConfig();
  await request('/auth/login', { method: 'POST', body: { login: 'diogo', senha: 'A#<1>!' }, public: true });
  assert.equal(JSON.parse(calls.at(-1).options.body).senha, 'A#<1>!');
});

test('cliente do planejador bulk usa contratos persistidos e preserva correlationId', async () => {
  saveSession({ token: jwt({ nome: 'Diogo', roles: ['PLANEJADOR'], exp: Math.floor(Date.now() / 1000) + 3600 }) });
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    return new Response(JSON.stringify({ id: 41 }), { status: 200, headers: { 'Content-Type': 'application/json', 'X-Correlation-Id': 'bulk-41' } });
  };
  await loadRuntimeConfig();

  await api.listarPlanosEstivagemBulk(7, 'V001');
  assert.equal(calls.at(-1).url, 'http://localhost:8080/api/estivagem-bulk/planos?navioId=7&codigoViagem=V001');

  await api.criarPlanoEstivagemBulk({ navioId: 7, codigoViagem: 'V001', portoCarga: 'BRITG', portoDescarga: 'NLRTM' });
  const createCall = calls.at(-1);
  assert.equal(createCall.url, 'http://localhost:8080/api/estivagem-bulk/planos');
  assert.equal(createCall.options.method, 'POST');
  assert.deepEqual(JSON.parse(createCall.options.body), { navioId: 7, codigoViagem: 'V001', portoCarga: 'BRITG', portoDescarga: 'NLRTM' });
  assert.ok(createCall.options.headers.get('X-Correlation-Id'));
});

test('contratos operacionais do pátio usam rotas reais e motivo explícito', async () => {
  saveSession({ token: jwt({ nome: 'Diogo', roles: ['PLANEJADOR'], exp: Math.floor(Date.now() / 1000) + 3600 }) });
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    return new Response(JSON.stringify({ id: 15, status: 'ATIVA' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };
  await loadRuntimeConfig();
  await api.ativarWorkQueuePatio(15, 'Abertura da janela operacional');
  const sent = calls.at(-1);
  assert.equal(sent.url, 'http://localhost:8080/yard/patio/work-queues/15/ativar');
  assert.equal(sent.options.method, 'PATCH');
  const body = JSON.parse(sent.options.body);
  assert.equal(body.motivo, 'Abertura da janela operacional');
  assert.equal(body.usuario, 'Diogo');
  assert.equal(body.origemAcao, 'PORTAL_CLOUDPORT_REACT');
  assert.ok(body.correlationId);
});

test('comandos motivados do pátio rejeitam motivo vazio antes da requisição', () => {
  assert.throws(() => api.cancelarWorkInstructionPatio(9, '   '), /motivo operacional é obrigatório/i);
});

test('consulta de posições operacionais usa contrato de reservas e restrições reais', async () => {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    return new Response(JSON.stringify([{ id: 1, bloco: 'A1', bloqueada: false, interditada: false }]), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };
  await loadRuntimeConfig();
  const positions = await api.listarPosicoesReservaveisPatio();
  assert.equal(calls.at(-1).url, 'http://localhost:8080/yard/patio/reservas/posicoes');
  assert.equal(positions[0].bloco, 'A1');
});
