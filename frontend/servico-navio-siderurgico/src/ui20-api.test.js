import assert from 'node:assert/strict';
import test, { afterEach, beforeEach } from 'node:test';
import { api, clearSession, saveSession, setRuntimeConfigForTests } from './api.js';
import { craneApi, setCraneBaseUrlForTests } from './ui20-crane-api.js';

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
  setCraneBaseUrlForTests('http://api.cloudport.test/');
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

test('consulta e salva o plano de guindastes da visita', async () => {
  await craneApi.obterPlanoGuindaste(42);
  await craneApi.salvarPlanoGuindaste(42, {
    berco: 'B01',
    status: 'PUBLICADO',
    observacao: 'Plano validado no Control Room',
    guindastes: [{
      codigoGuindaste: 'QC-01',
      porao: 1,
      workQueueId: 7,
      sequencia: 1,
      movimentosPlanejados: 25,
      produtividadePlanejadaMovimentosHora: 20,
      inicioPlanejado: '2026-07-16T18:00:00',
      fimPlanejado: '2026-07-16T20:00:00'
    }]
  });

  assert.equal(requests[0].url, 'http://api.cloudport.test/visitas-navio/42/crane-plan');
  assert.equal(requests[0].options.method, 'GET');
  assert.equal(requests[1].url, 'http://api.cloudport.test/visitas-navio/42/crane-plan');
  assert.equal(requests[1].options.method, 'POST');
  const body = JSON.parse(requests[1].options.body);
  assert.equal(body.usuario, 'Diogo');
  assert.equal(body.guindastes[0].workQueueId, 7);
});

test('cria, lista e inicia uma sequencia de guindaste', async () => {
  const allocation = {
    id: 15,
    codigoGuindaste: 'QC-01',
    workQueueId: 7,
    inicioPlanejado: '2026-07-16T18:00:00',
    observacao: 'Operação de teste'
  };

  await craneApi.listarSequencias(42);
  await craneApi.criarSequencia(42, allocation);
  await craneApi.transicionarSequencia(allocation, 'start');
  await craneApi.listarHistoricoSequencia(allocation);

  assert.equal(requests[0].url, 'http://api.cloudport.test/api/crane-sequences?vesselVisitId=42');
  assert.equal(requests[1].url, 'http://api.cloudport.test/api/crane-sequences');
  assert.deepEqual(JSON.parse(requests[1].options.body), {
    movementId: 'crane-plan-15',
    vesselVisitId: '42',
    craneId: 'QC-01',
    loadUnitId: '7',
    plannedStart: '2026-07-16T18:00:00',
    notes: 'Operação de teste'
  });
  assert.equal(requests[2].url, 'http://api.cloudport.test/api/crane-sequences/crane-plan-15/start');
  assert.deepEqual(JSON.parse(requests[2].options.body), { operatorId: 'Diogo', reason: null });
  assert.equal(requests[3].url, 'http://api.cloudport.test/api/crane-sequences/crane-plan-15/history');
});

test('consulta a matriz oficial e executa transição robusta', async () => {
  await api.obterMatrizEstadosWorkInstructionPatio();
  await api.concluirWorkInstructionPatio(99, 'Movimento confirmado pelo operador');

  assert.equal(requests[0].url, 'http://api.cloudport.test/yard/patio/work-instructions/matriz-estados');
  assert.equal(requests[1].url, 'http://api.cloudport.test/yard/patio/work-instructions/99/concluir');
  const body = JSON.parse(requests[1].options.body);
  assert.equal(body.motivo, 'Movimento confirmado pelo operador');
  assert.equal(body.usuario, 'Diogo');
});
