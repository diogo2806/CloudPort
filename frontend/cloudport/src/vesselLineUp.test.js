import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calcularConflitosBerco,
  calcularFaseSimulada,
  construirSimulacao,
  normalizarEscalasLineUp
} from './vesselLineUp.js';

test('normaliza a janela operacional usando ETA, ETB e ETD', () => {
  const [escala] = normalizarEscalasLineUp([{
    id: 1,
    nomeNavio: 'CloudPort One',
    chegadaPrevista: '2026-07-18T06:00:00',
    atracacaoPrevista: '2026-07-18T08:00:00',
    partidaPrevista: '2026-07-19T02:00:00',
    bercoPrevisto: 'B01'
  }]);

  assert.equal(escala.berco, 'B01');
  assert.equal(escala.etb.getHours(), 8);
  assert.equal(escala.etd.getDate(), 19);
});

test('calcula a fase simulada conforme a posição no cronograma', () => {
  const [escala] = normalizarEscalasLineUp([{
    id: 1,
    chegadaPrevista: '2026-07-18T06:00:00',
    atracacaoPrevista: '2026-07-18T08:00:00',
    partidaPrevista: '2026-07-18T20:00:00'
  }]);

  assert.equal(calcularFaseSimulada(escala, '2026-07-18T05:00:00'), 'PREVISTA');
  assert.equal(calcularFaseSimulada(escala, '2026-07-18T07:00:00'), 'INBOUND');
  assert.equal(calcularFaseSimulada(escala, '2026-07-18T12:00:00'), 'OPERANDO');
  assert.equal(calcularFaseSimulada(escala, '2026-07-18T21:00:00'), 'PARTIU');
});

test('identifica sobreposição de navios no mesmo berço', () => {
  const escalas = normalizarEscalasLineUp([
    { id: 1, chegadaPrevista: '2026-07-18T06:00:00', atracacaoPrevista: '2026-07-18T08:00:00', partidaPrevista: '2026-07-18T20:00:00', bercoPrevisto: 'B01' },
    { id: 2, chegadaPrevista: '2026-07-18T14:00:00', atracacaoPrevista: '2026-07-18T18:00:00', partidaPrevista: '2026-07-19T04:00:00', bercoPrevisto: 'B01' },
    { id: 3, chegadaPrevista: '2026-07-18T14:00:00', atracacaoPrevista: '2026-07-18T18:00:00', partidaPrevista: '2026-07-19T04:00:00', bercoPrevisto: 'B02' }
  ]);

  const conflitos = calcularConflitosBerco(escalas);
  assert.deepEqual([...conflitos].sort(), ['1', '2']);
});

test('constrói indicadores de progresso para a operação simulada', () => {
  const escalas = normalizarEscalasLineUp([{
    id: 1,
    chegadaPrevista: '2026-07-18T06:00:00',
    atracacaoPrevista: '2026-07-18T08:00:00',
    partidaPrevista: '2026-07-18T20:00:00'
  }]);

  const [simulada] = construirSimulacao(escalas, '2026-07-18T14:00:00');
  assert.equal(simulada.faseSimulada, 'OPERANDO');
  assert.equal(simulada.progressoOperacao, 50);
});
