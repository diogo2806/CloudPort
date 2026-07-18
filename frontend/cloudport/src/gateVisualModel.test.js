import assert from 'node:assert/strict';
import test from 'node:test';
import {
  buildGateLanes,
  classifyGateSla,
  flattenGateVehicles,
  getGateJourney,
  groupGateWindows,
  isGateProblemVehicle
} from './gateVisualModel.js';

test('monta pistas, enriquece veículos e evita duplicidade no atendimento', () => {
  const appointments = [{
    id: 100,
    codigo: 'AG-100',
    veiculoId: 10,
    placaVeiculo: 'ABC1D23',
    motoristaNome: 'Maria',
    transportadoraNome: 'Transportes Sul',
    status: 'EM_ATENDIMENTO',
    documentos: [{ id: 1, statusValidacao: 'VALIDADO' }]
  }];
  const lanes = buildGateLanes({
    filasEntrada: [{ id: 'entrada-1', nome: 'Pista 1', veiculos: [{ id: 10, placa: 'ABC1D23', tempoFilaMinutos: 12 }] }],
    filasSaida: [],
    veiculosAtendimento: [{ id: 10, placa: 'ABC1D23', status: 'EM_ATENDIMENTO', tempoFilaMinutos: 15 }]
  }, appointments);

  const vehicles = flattenGateVehicles(lanes);
  assert.equal(vehicles.length, 1);
  assert.equal(vehicles[0].agendamento.codigo, 'AG-100');
  assert.equal(vehicles[0].documentos.length, 1);
  assert.equal(lanes.find((lane) => lane.id === 'atendimento').vehicles.length, 1);
});

test('adiciona agendamentos fora do painel na pista de pré-gate', () => {
  const lanes = buildGateLanes({}, [{ id: 1, veiculoId: 30, placaVeiculo: 'QWE2R34', status: 'CONFIRMADO' }]);
  assert.equal(lanes[0].id, 'pre-gate');
  assert.equal(lanes[0].vehicles[0].placa, 'QWE2R34');
});

test('classifica SLA e identifica transação problemática', () => {
  assert.equal(classifyGateSla(5).level, 'ok');
  assert.equal(classifyGateSla(25).level, 'warning');
  assert.equal(classifyGateSla(35).level, 'critical');
  assert.equal(isGateProblemVehicle({ tempoFilaMinutos: 35 }), true);
  assert.equal(isGateProblemVehicle({ tempoFilaMinutos: 5, excecoes: [{ descricao: 'OCR divergente' }] }), true);
  assert.equal(isGateProblemVehicle({ tempoFilaMinutos: 5, documentos: [{ statusValidacao: 'VALIDADO' }] }), false);
});

test('representa OCR, balança, inspeção e liberação na jornada', () => {
  const stages = getGateJourney({
    status: 'EM_EXECUCAO',
    podeImprimirComprovante: true,
    documentos: [{ statusValidacao: 'VALIDADO' }],
    excecoes: []
  });
  assert.deepEqual(stages.map((stage) => stage.key), ['appointment', 'ocr', 'scale', 'inspection', 'release']);
  assert.equal(stages.find((stage) => stage.key === 'ocr').state, 'complete');
  assert.equal(stages.find((stage) => stage.key === 'scale').state, 'complete');
  assert.equal(stages.find((stage) => stage.key === 'release').state, 'complete');
});

test('calcula ocupação e percentual das janelas', () => {
  const calendar = groupGateWindows([
    { id: 1, data: '2026-07-17', horaInicio: '08:00:00', horaFim: '09:00:00', capacidade: 2 },
    { id: 2, data: '2026-07-17', horaInicio: '09:00:00', horaFim: '10:00:00', capacidade: 4 }
  ], [
    { id: 11, janelaAtendimentoId: 1 },
    { id: 12, janelaAtendimentoId: 1 },
    { id: 13, janelaAtendimentoId: 2 }
  ]);

  assert.equal(calendar.length, 1);
  assert.equal(calendar[0].windows[0].occupied, 2);
  assert.equal(calendar[0].windows[0].percentage, 100);
  assert.equal(calendar[0].windows[1].percentage, 25);
});
