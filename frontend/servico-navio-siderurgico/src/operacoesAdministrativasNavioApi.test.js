import assert from 'node:assert/strict';
import test, { afterEach, beforeEach } from 'node:test';
import { clearSession, saveSession } from './api.js';
import {
  navioAdministrativeApi,
  setAdministrativeBaseUrlForTests
} from './operacoesAdministrativasNavioApi.js';

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
  setAdministrativeBaseUrlForTests('http://api.cloudport.test/');
  globalThis.fetch = async (url, options = {}) => {
    requests.push({ url: String(url), options });
    return new Response(JSON.stringify({ id: 1, status: 'CANCELADO' }), {
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

test('publica plano com motivo, identidade e correlação', async () => {
  await navioAdministrativeApi.publicarPlano(12, 8, 'Plano aprovado pela operação');

  assert.equal(requests.length, 1);
  assert.equal(requests[0].url, 'http://api.cloudport.test/visitas-navio/12/plano-estiva/8/publicar');
  assert.equal(requests[0].options.method, 'POST');
  assert.match(requests[0].options.headers.Authorization, /^Bearer /);
  assert.ok(requests[0].options.headers['X-Correlation-Id']);
  const body = JSON.parse(requests[0].options.body);
  assert.equal(body.motivo, 'Plano aprovado pela operação');
  assert.equal(body.usuario, 'Planejador');
  assert.equal(body.origemAcao, 'CONTROL_ROOM_NAVIO_ADMIN');
  assert.equal(body.correlationId, requests[0].options.headers['X-Correlation-Id']);
});

test('cancela visita, item, plano e ordem pelos contratos diferenciados', async () => {
  await navioAdministrativeApi.cancelarVisita(12, 'Visita removida da escala');
  await navioAdministrativeApi.cancelarItem(12, 33, 'Carga retirada');
  await navioAdministrativeApi.cancelarPlano(12, 8, 'Plano substituído');
  await navioAdministrativeApi.cancelarOrdem(12, 91, 'Ordem sem efeito');

  assert.deepEqual(requests.map((request) => request.url), [
    'http://api.cloudport.test/visitas-navio/12/cancelar',
    'http://api.cloudport.test/visitas-navio/12/itens/33/cancelar',
    'http://api.cloudport.test/visitas-navio/12/plano-estiva/8/cancelar',
    'http://api.cloudport.test/visitas-navio/12/integracao-patio/ordens/91/cancelar'
  ]);
  assert.ok(requests.every((request) => request.options.method === 'PATCH'));
  assert.ok(requests.every((request) => JSON.parse(request.options.body).motivo));
});

test('invalida plano e cria nova versão copiando posições', async () => {
  await navioAdministrativeApi.invalidarPlano(12, 8, 'Mudança de carga');
  await navioAdministrativeApi.criarNovaVersaoPlano(12, [{ itemOperacaoId: 3, porao: 1 }]);

  assert.equal(requests[0].url, 'http://api.cloudport.test/visitas-navio/12/plano-estiva/8/invalidar');
  assert.equal(requests[0].options.method, 'PATCH');
  assert.equal(requests[1].url, 'http://api.cloudport.test/visitas-navio/12/plano-estiva');
  assert.equal(requests[1].options.method, 'POST');
  assert.deepEqual(JSON.parse(requests[1].options.body).posicoes, [{ itemOperacaoId: 3, porao: 1 }]);
});

test('rejeita motivo vazio e identificadores inválidos antes da rede', () => {
  assert.throws(
    () => navioAdministrativeApi.cancelarVisita(12, '   '),
    /motivo da operação administrativa/
  );
  assert.throws(
    () => navioAdministrativeApi.cancelarItem(0, 2, 'Motivo'),
    /visitaId/
  );
  assert.equal(requests.length, 0);
});
