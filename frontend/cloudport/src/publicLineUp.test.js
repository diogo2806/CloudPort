import assert from 'node:assert/strict';
import test from 'node:test';
import { clearSession, loadRuntimeConfig } from './api.js';
import { filterLineUp, lineUpStatusLabel, normalizeLineUp, scheduleMoment } from './publicLineUp.js';
import { listarLineUpPublico } from './publicLineUpApi.js';

function createStorage() {
  const values = new Map();
  return {
    getItem: (key) => values.has(key) ? values.get(key) : null,
    setItem: (key, value) => values.set(key, String(value)),
    removeItem: (key) => values.delete(key)
  };
}

test.beforeEach(() => {
  globalThis.localStorage = createStorage();
  clearSession();
});

test('normaliza, ordena e filtra o line-up público', () => {
  const items = normalizeLineUp([
    { nomeNavio: 'Navio B', codigoImo: '222', fase: 'OPERANDO', chegadaPrevista: '2026-07-20T10:00:00', berco: 'B2' },
    { nomeNavio: 'Navio A', codigoImo: '111', fase: 'INBOUND', chegadaPrevista: '2026-07-18T08:00:00', berco: 'B1' }
  ]);

  assert.equal(items[0].nomeNavio, 'Navio A');
  assert.equal(filterLineUp(items, '222', '').length, 1);
  assert.equal(filterLineUp(items, '', 'INBOUND')[0].codigoImo, '111');
  assert.equal(lineUpStatusLabel('OPERANDO'), 'Em operação');
  assert.deepEqual(scheduleMoment('2026-07-18T09:00:00', '2026-07-18T08:00:00'), {
    value: '2026-07-18T09:00:00',
    actual: true
  });
});

test('consulta o line-up sem exigir sessão ou enviar JWT', async () => {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      });
    }
    return new Response(JSON.stringify([{ nomeNavio: 'Cloud Port' }]), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    });
  };

  await loadRuntimeConfig();
  const result = await listarLineUpPublico(15);
  const request = calls.at(-1);

  assert.equal(request.url, 'http://localhost:8080/public/line-up-navios?dias=15');
  assert.equal(request.options.headers.has('Authorization'), false);
  assert.equal(result[0].nomeNavio, 'Cloud Port');
});
