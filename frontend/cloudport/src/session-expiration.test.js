import assert from 'node:assert/strict';
import test from 'node:test';
import {
  clearSession,
  loadRuntimeConfig,
  request,
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

test.beforeEach(() => {
  globalThis.localStorage = createStorage();
  clearSession();
});

test('qualquer resposta 401 publica a expiração mesmo sem token persistido', async () => {
  globalThis.fetch = async (url) => {
    if (url === '/assets/configuracao.json') {
      return new Response(JSON.stringify({ baseApiUrl: 'http://localhost:8080' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      });
    }
    return new Response(JSON.stringify({ mensagem: 'Sessão expirada' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json' }
    });
  };

  await loadRuntimeConfig();
  let expirations = 0;
  const unsubscribe = subscribeSessionExpired(() => { expirations += 1; });

  try {
    await assert.rejects(() => request('/api/protegida'), /Sessão expirada/);
    assert.equal(expirations, 1);
  } finally {
    unsubscribe();
  }
});
