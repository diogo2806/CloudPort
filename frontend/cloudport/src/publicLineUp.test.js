import assert from 'node:assert/strict';
import test from 'node:test';
import { clearSession, loadRuntimeConfig } from './api.js';
import { filterLineUp, lineUpStatusLabel, normalizeLineUp } from './publicLineUp.js';
import { listarLineUpPublico } from './publicLineUpApi.js';

function storage() {
  const values = new Map();
  return { getItem: (key) => values.get(key) ?? null, setItem: (key, value) => values.set(key, String(value)), removeItem: (key) => values.delete(key) };
}

test.beforeEach(() => { globalThis.localStorage = storage(); clearSession(); });

test('normaliza, ordena e filtra escalas públicas', () => {
  const items = normalizeLineUp([
    { nomeNavio: 'B', codigoImo: '222', fase: 'OPERANDO', chegadaPrevista: '2026-07-20T10:00:00' },
    { nomeNavio: 'A', codigoImo: '111', fase: 'INBOUND', chegadaPrevista: '2026-07-18T08:00:00' }
  ]);
  assert.equal(items[0].nomeNavio, 'A');
  assert.equal(filterLineUp(items, '222', '').length, 1);
  assert.equal(filterLineUp(items, '', 'INBOUND')[0].codigoImo, '111');
  assert.equal(lineUpStatusLabel('OPERANDO'), 'Em operação');
});

test('consulta o line-up sem sessão nem JWT', async () => {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });
    if (url === '/assets/configuracao.json') return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    return new Response(JSON.stringify([{ nomeNavio: 'Cloud Port' }]), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };
  await loadRuntimeConfig();
  const result = await listarLineUpPublico(15);
  assert.equal(calls.at(-1).url, 'http://localhost:8080/public/line-up-navios?dias=15');
  assert.equal(calls.at(-1).options.headers.has('Authorization'), false);
  assert.equal(result[0].nomeNavio, 'Cloud Port');
});
