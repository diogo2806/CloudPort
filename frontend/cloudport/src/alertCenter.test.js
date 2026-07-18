import assert from 'node:assert/strict';
import test from 'node:test';
import {
  moduleForAlert,
  normalizeAlertPage,
  replaceAlert,
  routeForAlert,
  severityLabel,
  summarizeAlerts
} from './alertCenter.js';

test('normaliza e prioriza alertas por severidade e data', () => {
  const page = normalizeAlertPage({
    content: [
      { id: 1, severidade: 'media', dataGerada: '2026-07-18T09:00:00', descricao: 'Média' },
      { id: 2, severidade: 'CRITICA', dataGerada: '2026-07-18T08:00:00', descricao: 'Crítica' },
      { id: 3, severidade: 'alta', dataGerada: '2026-07-18T10:00:00', descricao: 'Alta' }
    ],
    totalElements: 3
  });

  assert.deepEqual(page.alerts.map((alert) => alert.id), [2, 3, 1]);
  assert.equal(page.total, 3);
  assert.equal(severityLabel('critica'), 'Crítica');
});

test('resume ativos e alertas ainda não reconhecidos', () => {
  const summary = summarizeAlerts([
    { id: 1, status: 'ativo', severidade: 'critica' },
    { id: 2, status: 'ativo', severidade: 'alta', dataReconhecimento: '2026-07-18T10:00:00' },
    { id: 3, status: 'resolvido', severidade: 'media' }
  ]);

  assert.deepEqual(summary, { totalAtivos: 2, criticos: 1, altos: 1, medios: 0, baixos: 0, naoReconhecidos: 1 });
});

test('direciona o alerta para o módulo operacional correspondente', () => {
  assert.equal(routeForAlert({ tipo: 'GARGALO_YARD' }), '/home/patio/mapa');
  assert.equal(routeForAlert({ tipo: 'ATRASO_NAVIO' }), '/home/navio/line-up');
  assert.equal(routeForAlert({ tipo: 'FALHA_GATE' }), '/home/gate/dashboard');
  assert.equal(moduleForAlert({ tipo: 'VAGAO_BLOQUEADO' }), 'Ferrovia');
});

test('substitui o alerta atualizado sem alterar os demais', () => {
  const result = replaceAlert([{ id: 1, descricao: 'Antes' }, { id: 2, descricao: 'Outro' }], { id: 1, descricao: 'Depois', status: 'ativo' });
  assert.equal(result[0].descricao, 'Depois');
  assert.equal(result[1].descricao, 'Outro');
});
