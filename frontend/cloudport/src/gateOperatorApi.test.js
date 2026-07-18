import assert from 'node:assert/strict';
import test from 'node:test';
import { clearSession, loadRuntimeConfig, saveSession } from './api.js';
import { gateOperatorApi, selectGateOperatorVehicles } from './gateOperatorApi.js';

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

test('consolida veículos das filas e prioriza o atendimento atual', () => {
  const vehicles = selectGateOperatorVehicles({
    filasEntrada: [{ id: 'ENTRADA-1', nome: 'Entrada principal', veiculos: [{ id: 10, placa: 'ABC1D23', status: 'AGUARDANDO' }] }],
    filasSaida: [{ id: 'SAIDA-1', nome: 'Saída principal', veiculos: [{ id: 20, placa: 'XYZ9K87', status: 'AGUARDANDO' }] }],
    veiculosAtendimento: [{ id: 10, placa: 'ABC1D23', status: 'EM_ATENDIMENTO' }]
  });

  assert.equal(vehicles.length, 2);
  assert.deepEqual(vehicles.find((vehicle) => vehicle.id === 10), {
    id: 10,
    placa: 'ABC1D23',
    status: 'EM_ATENDIMENTO',
    filaOperacional: 'Atendimento',
    fluxoOperacional: 'ATENDIMENTO'
  });
  assert.equal(vehicles.find((vehicle) => vehicle.id === 20)?.filaOperacional, 'Saída principal');
  assert.equal(vehicles.find((vehicle) => vehicle.id === 20)?.fluxoOperacional, 'SAÍDA');
});

test('retorna coleção vazia para painel ausente ou malformado', () => {
  assert.deepEqual(selectGateOperatorVehicles(null), []);
  assert.deepEqual(selectGateOperatorVehicles({}), []);
  assert.deepEqual(selectGateOperatorVehicles({ filasEntrada: {}, filasSaida: null, veiculosAtendimento: {} }), []);
});

test('consulta os contratos dedicados do operador do Gate', async () => {
  saveSession({ token: jwt({ nome: 'Diogo', roles: ['OPERADOR_GATE'], exp: Math.floor(Date.now() / 1000) + 3600 }) });
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }
    return new Response(JSON.stringify([]), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };

  await loadRuntimeConfig();
  await gateOperatorApi.obterPainel();
  assert.equal(calls.at(-1).url, 'http://localhost:8080/gate/operador/painel');
  await gateOperatorApi.listarEventos();
  assert.equal(calls.at(-1).url, 'http://localhost:8080/gate/operador/eventos');
  await gateOperatorApi.obterComprovante(10);
  assert.equal(calls.at(-1).url, 'http://localhost:8080/gate/operador/veiculos/10/comprovante');
});

test('recusa impressão sem identificador válido do veículo', async () => {
  await assert.rejects(() => gateOperatorApi.obterComprovante(null), /veículo é obrigatório/i);
});
