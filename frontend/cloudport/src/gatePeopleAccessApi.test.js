import assert from 'node:assert/strict';
import test from 'node:test';
import { clearSession, loadRuntimeConfig, saveSession } from './api.js';
import { gatePeopleAccessApi } from './gatePeopleAccessApi.js';

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

test('consulta resumo, presentes e histórico de pessoas', async () => {
  saveSession({ token: jwt({ nome: 'Diogo', roles: ['OPERADOR_GATE'], exp: Math.floor(Date.now() / 1000) + 3600 }) });
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      });
    }
    return new Response(JSON.stringify([]), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    });
  };

  await loadRuntimeConfig();
  await gatePeopleAccessApi.obterResumo();
  assert.equal(calls.at(-1).url, 'http://localhost:8080/gate/pessoas/resumo');

  await gatePeopleAccessApi.listarPresentes();
  assert.equal(calls.at(-1).url, 'http://localhost:8080/gate/pessoas/presentes');

  await gatePeopleAccessApi.listarMovimentacoes('123.456.789-00', 50);
  assert.equal(calls.at(-1).url, 'http://localhost:8080/gate/pessoas/movimentacoes?documento=123.456.789-00&limite=50');
});

test('registra entrada e saída com os contratos corretos', async () => {
  saveSession({ token: jwt({ nome: 'Diogo', roles: ['OPERADOR_GATE'], exp: Math.floor(Date.now() / 1000) + 3600 }) });
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      });
    }
    return new Response(JSON.stringify({ id: 1 }), {
      status: 201,
      headers: { 'Content-Type': 'application/json' }
    });
  };

  await loadRuntimeConfig();
  await gatePeopleAccessApi.registrarEntrada({
    nome: ' Maria da Silva ',
    documento: '123.456.789-00',
    tipoPessoa: 'visitante',
    empresa: 'Empresa Exemplo',
    pontoAcesso: 'Portaria principal',
    motivo: 'Reunião'
  });

  const entrada = calls.at(-1);
  assert.equal(entrada.url, 'http://localhost:8080/gate/pessoas/entradas');
  assert.equal(entrada.options.method, 'POST');
  const entradaBody = JSON.parse(entrada.options.body);
  assert.equal(entradaBody.nome, 'Maria da Silva');
  assert.equal(entradaBody.tipoPessoa, 'VISITANTE');
  assert.equal(entradaBody.usuario, 'Diogo');

  await gatePeopleAccessApi.registrarSaida({
    documento: '123.456.789-00',
    pontoAcesso: 'Portaria principal',
    motivo: 'Saída normal'
  });

  const saida = calls.at(-1);
  assert.equal(saida.url, 'http://localhost:8080/gate/pessoas/saidas');
  assert.equal(saida.options.method, 'POST');
  assert.equal(JSON.parse(saida.options.body).documento, '123.456.789-00');
});
