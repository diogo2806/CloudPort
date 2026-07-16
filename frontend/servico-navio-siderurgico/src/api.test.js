import assert from 'node:assert/strict';
import test from 'node:test';
import { formatError, hasAnyRole } from './api.js';

test('normaliza roles recebidas no controle de acesso', () => {
  assert.equal(hasAnyRole({ roles: ['ROLE_PLANEJADOR'] }, 'PLANEJADOR'), true);
  assert.equal(hasAnyRole({ roles: ['ROLE_VISUALIZADOR'] }, 'OPERADOR_GATE'), false);
});

test('preserva código e correlationId nas mensagens da API', () => {
  const mensagem = formatError({
    payload: {
      mensagem: 'Operação recusada',
      codigo: 'YARD-409',
      correlationId: 'abc-123'
    }
  });
  assert.match(mensagem, /Operação recusada \[YARD-409\]/);
  assert.match(mensagem, /correlationId: abc-123/);
});
