import assert from 'node:assert/strict';
import test from 'node:test';
import { ALERT_CENTER_REFRESH_INTERVAL_MS, ALERT_CENTER_ROUTE } from './alertCenter.contract.js';

test('mantém a rota e o intervalo operacional da central', () => {
  assert.equal(ALERT_CENTER_ROUTE, '/home/alertas');
  assert.equal(ALERT_CENTER_REFRESH_INTERVAL_MS, 30000);
});
