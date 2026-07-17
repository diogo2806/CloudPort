import assert from 'node:assert/strict';
import test from 'node:test';
import { selectGateAppointments } from './gateDataset.js';

test('seleciona os agendamentos da visão consolidada do Gate', () => {
  const appointments = [{ id: 1, codigo: 'AG-001' }, { id: 2, codigo: 'AG-002' }];

  assert.deepEqual(selectGateAppointments({
    usuario: { nome: 'Operador' },
    situacaoPatio: { status: 'NORMAL' },
    agendamentos: appointments
  }), appointments);
});

test('retorna coleção vazia quando a visão consolidada não contém agendamentos válidos', () => {
  assert.deepEqual(selectGateAppointments(null), []);
  assert.deepEqual(selectGateAppointments({}), []);
  assert.deepEqual(selectGateAppointments({ agendamentos: null }), []);
  assert.deepEqual(selectGateAppointments({ agendamentos: { id: 1 } }), []);
});
