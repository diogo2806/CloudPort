import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calcularFaseTrem,
  calcularJanelaFerroviaria,
  calcularPosicaoVertical,
  construirOcupacoesFerroviarias,
  normalizarVisitasFerrovia
} from './railLineUp.js';

function criarVisita(id, chegada, partida, extras = {}) {
  return {
    id,
    identificadorTrem: `TREM-${id}`,
    operadoraFerroviaria: 'MRS',
    horaChegadaPrevista: chegada,
    horaPartidaPrevista: partida,
    listaVagoes: [{ codigo: `V-${id}` }],
    listaCarga: [],
    listaDescarga: [],
    ...extras
  };
}

test('normaliza visita e aplica duração padrão quando a partida é inválida', () => {
  const [visita] = normalizarVisitasFerrovia([
    criarVisita(1, '2026-07-18T08:00:00', '2026-07-18T07:00:00')
  ]);

  assert.equal(visita.identificadorTrem, 'TREM-1');
  assert.equal(visita.quantidadeVagoes, 1);
  assert.equal(visita.partida.getTime() - visita.chegada.getTime(), 12 * 60 * 60 * 1000);
});

test('cria sequência de recepção, operação e expedição para cada trem', () => {
  const visitas = normalizarVisitasFerrovia([
    criarVisita(1, '2026-07-18T08:00:00', '2026-07-18T20:00:00')
  ]);
  const segmentos = construirOcupacoesFerroviarias(visitas);

  assert.deepEqual(segmentos.map((segmento) => segmento.etapa), ['RECEPCAO', 'OPERACAO', 'EXPEDICAO']);
  assert.equal(segmentos[0].linha, 'Recepção');
  assert.equal(segmentos[1].linha, 'Linha 01');
  assert.equal(segmentos[2].linha, 'Expedição');
});

test('envia locomotiva isolada para a linha de apoio', () => {
  const visitas = normalizarVisitasFerrovia([
    criarVisita(1, '2026-07-18T08:00:00', '2026-07-18T14:00:00', { tipoVisita: 'LOCOMOTIVA_ISOLADA' })
  ]);
  const operacao = construirOcupacoesFerroviarias(visitas).find((segmento) => segmento.etapa === 'OPERACAO');

  assert.equal(operacao.linha, 'Linha de apoio');
  assert.equal(operacao.descricao, 'Apoio de locomotiva');
});

test('marca conflito e distribui faixas para ocupações sobrepostas na mesma linha', () => {
  const visitas = normalizarVisitasFerrovia([
    criarVisita(1, '2026-07-18T08:00:00', '2026-07-18T20:00:00'),
    criarVisita(2, '2026-07-18T09:00:00', '2026-07-18T19:00:00')
  ]);
  const operacoes = construirOcupacoesFerroviarias(visitas, { linhasOperacionais: ['Linha 01'] })
    .filter((segmento) => segmento.etapa === 'OPERACAO');

  assert.equal(operacoes.every((segmento) => segmento.conflito), true);
  assert.deepEqual(operacoes.map((segmento) => segmento.faixa).sort(), [0, 1]);
  assert.equal(operacoes.every((segmento) => segmento.totalFaixas === 2), true);
});

test('calcula posição vertical do segmento na janela', () => {
  const visitas = normalizarVisitasFerrovia([
    criarVisita(1, '2026-07-18T08:00:00', '2026-07-18T20:00:00')
  ]);
  const janela = calcularJanelaFerroviaria(visitas);
  const operacao = construirOcupacoesFerroviarias(visitas).find((segmento) => segmento.etapa === 'OPERACAO');
  const posicao = calcularPosicaoVertical(operacao, janela);

  assert.match(posicao.top, /%$/);
  assert.match(posicao.height, /%$/);
  assert.ok(Number.parseFloat(posicao.height) > 0);
});

test('calcula fase simulada conforme a etapa ocupada', () => {
  const visitas = normalizarVisitasFerrovia([
    criarVisita(1, '2026-07-18T08:00:00', '2026-07-18T20:00:00')
  ]);
  const segmentos = construirOcupacoesFerroviarias(visitas);

  assert.equal(calcularFaseTrem(visitas[0], segmentos, '2026-07-18T07:00:00'), 'PREVISTO');
  assert.equal(calcularFaseTrem(visitas[0], segmentos, '2026-07-18T08:30:00'), 'RECEBENDO');
  assert.equal(calcularFaseTrem(visitas[0], segmentos, '2026-07-18T12:00:00'), 'OPERANDO');
  assert.equal(calcularFaseTrem(visitas[0], segmentos, '2026-07-18T21:00:00'), 'PARTIU');
});
