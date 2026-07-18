import assert from 'node:assert/strict';
import test from 'node:test';

const API_BASE = '/api/v1/visibilidade/alertas';

test('mantém os contratos públicos esperados pela central de alertas', () => {
  assert.equal(`${API_BASE}/filtrados`, '/api/v1/visibilidade/alertas/filtrados');
  assert.equal(`${API_BASE}/resumo`, '/api/v1/visibilidade/alertas/resumo');
  assert.equal(`${API_BASE}/15/reconhecer`, '/api/v1/visibilidade/alertas/15/reconhecer');
  assert.equal(`${API_BASE}/15/resolver`, '/api/v1/visibilidade/alertas/15/resolver');
});
