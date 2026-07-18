import assert from 'node:assert/strict';
import test, { afterEach, beforeEach } from 'node:test';
import { clearSession, saveSession } from './api.js';
import { replanejamentoRealApi, setReplanningBaseUrlForTests } from './replanejamentoRealApi.js';

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
  saveSession({
    token: jwt({ nome: 'Planejador', roles: ['ROLE_PLANEJADOR'] }),
    nome: 'Planejador',
    roles: ['ROLE_PLANEJADOR']
  });
  setReplanningBaseUrlForTests('http://api.cloudport.test/');
  globalThis.fetch = async (url, options = {}) => {
    requests.push({ url: String(url), options });
    return new Response(JSON.stringify({
      planoOtimizacaoId: 'replan-001',
      atribuicoesPropostas: [],
      alertasImpeditivos: []
    }), {
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

test('simula sem aplicar e envia identidade, motivo e correlação', async () => {
  await replanejamentoRealApi.simular(42, 'Simulação operacional', {
    limiteRehandleAceitavel: 3,
    pesosCriterios: { DISTANCIA: 2 }
  });

  assert.equal(requests.length, 1);
  assert.equal(requests[0].url, 'http://api.cloudport.test/visitas-navio/42/integracao-patio/replanejar');
  assert.equal(requests[0].options.method, 'POST');
  assert.match(requests[0].options.headers.Authorization, /^Bearer /);
  assert.ok(requests[0].options.headers['X-Correlation-Id']);
  const body = JSON.parse(requests[0].options.body);
  assert.equal(body.aplicar, false);
  assert.equal(body.usuario, 'Planejador');
  assert.equal(body.motivo, 'Simulação operacional');
  assert.equal(body.limiteRehandleAceitavel, 3);
  assert.deepEqual(body.pesosCriterios, { DISTANCIA: 2 });
  assert.equal(body.correlationId, requests[0].options.headers['X-Correlation-Id']);
});

test('aplica pelo mesmo contrato após confirmação da interface', async () => {
  await replanejamentoRealApi.aplicar(42, 'Aplicação do plano aprovado');

  const body = JSON.parse(requests[0].options.body);
  assert.equal(body.aplicar, true);
  assert.equal(body.motivo, 'Aplicação do plano aprovado');
});

test('rejeita motivo vazio e visita inválida antes da rede', async () => {
  await assert.rejects(
    replanejamentoRealApi.simular(42, '   '),
    /motivo do replanejamento/
  );
  assert.throws(
    () => replanejamentoRealApi.aplicar(0, 'Aplicar'),
    /visitaId/
  );
  assert.equal(requests.length, 0);
});
