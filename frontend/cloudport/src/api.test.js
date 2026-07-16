import assert from 'node:assert/strict';
import test from 'node:test';
import {
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
  sanitizeText
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
