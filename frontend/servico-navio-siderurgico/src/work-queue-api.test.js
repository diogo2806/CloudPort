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
  requests = [];
  storage.clear();
  globalThis.sessionStorage = {
    getItem: (key) => storage.get(key) ?? null,
    setItem: (key, value) => storage.set(key, value),
    removeItem: (key) => storage.delete(key)
  };
  setRuntimeConfigForTests({ baseApiUrl: 'http://api.cloudport.test/' });
  saveSession({ token: jwt({ nome: 'Diogo', roles: ['ROLE_PLANEJADOR'] }) });
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

test('associa porão, guindaste, recurso de cais e CHE real', async () => {
  await api.atualizarRecursosWorkQueuePatio(7, {
    porao: 2,
    planoGuindasteId: 31,
    recursoCaisId: 44,
    equipamentoPatioId: 9,
    motivo: 'Ajuste de cobertura'
  });

  assert.equal(requests[0].url, 'http://api.cloudport.test/yard/patio/work-queues/7/recursos-operacionais');
  assert.equal(requests[0].options.method, 'PATCH');
  const body = JSON.parse(requests[0].options.body);
  assert.equal(body.equipamentoPatioId, 9);
  assert.equal(body.usuario, 'Diogo');
  assert.equal(body.origemAcao, 'CONTROL_ROOM_NAVIO_PATIO');
});

test('separa prioridade operacional da prioridade de fetch', async () => {
  await api.atualizarPrioridadesWorkInstructionPatio(88, {
    prioridadeOperacional: 4,
    prioridadeBusca: true,
    motivo: 'Movimento crítico'
  });

  assert.equal(requests[0].url, 'http://api.cloudport.test/yard/patio/work-instructions/88/prioridades');
  const body = JSON.parse(requests[0].options.body);
  assert.equal(body.prioridadeOperacional, 4);
  assert.equal(body.prioridadeBusca, true);
});

test('consulta job list por equipamento e drill-down', async () => {
  await api.listarJobListsEquipamentoPatio(42);
  await api.obterDrillDownWorkInstructionPatio(99);

  assert.equal(requests[0].url, 'http://api.cloudport.test/yard/patio/equipamentos/job-lists?visitaNavioId=42');
  assert.equal(requests[1].url, 'http://api.cloudport.test/yard/patio/work-instructions/99/drill-down');
});
