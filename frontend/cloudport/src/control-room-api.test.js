import test from 'node:test';
import assert from 'node:assert/strict';
import { buildControlRoomQuery, normalizeCommandPayload, parseSseBlock } from './controlRoomApi.js';

test('remove filtros vazios da consulta do Control Room', () => {
  assert.deepEqual(buildControlRoomQuery({ status: 'OPERACIONAL', tipo: '', conectividade: null, pagina: 0 }), {
    status: 'OPERACIONAL',
    pagina: 0
  });
});

test('normaliza o contrato de comando remoto', () => {
  const payload = normalizeCommandPayload({
    tipo: ' mover_para_posicao ',
    mensagem: ' Bloco A01 ',
    parametros: { posicao: 'A01-01-01' },
    correlationId: 'corr-123'
  });

  assert.deepEqual(payload, {
    tipo: 'MOVER_PARA_POSICAO',
    mensagem: 'Bloco A01',
    parametros: { posicao: 'A01-01-01' },
    correlationId: 'corr-123'
  });
});

test('interpreta o envelope SSE do Control Room', () => {
  const event = parseSseBlock([
    'id: evento-1',
    'event: cloudport.control-room.v1',
    'data: {"tipo":"TELEMETRIA_ATUALIZADA","dados":{"equipamento":"RTG-01"}}'
  ].join('\n'));

  assert.equal(event.id, 'evento-1');
  assert.equal(event.name, 'cloudport.control-room.v1');
  assert.equal(event.payload.tipo, 'TELEMETRIA_ATUALIZADA');
  assert.equal(event.payload.dados.equipamento, 'RTG-01');
});

test('ignora bloco SSE sem dados', () => {
  assert.equal(parseSseBlock('event: ping\nid: 2'), null);
});
