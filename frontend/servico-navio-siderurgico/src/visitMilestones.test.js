import test from 'node:test';
import assert from 'node:assert/strict';
import {
  VISIT_DATE_FIELDS,
  VISIT_MILESTONE_GROUPS,
  isAutomaticVisitMilestone,
  validateVisitMilestones
} from './visitMilestones.js';

test('expõe todos os marcos persistidos pela visita', () => {
  const fields = VISIT_MILESTONE_GROUPS.flatMap((group) => group.fields.map((field) => field.name));
  assert.deepEqual([...fields].sort(), [...VISIT_DATE_FIELDS].sort());
});

test('identifica os marcos preenchidos por transição operacional', () => {
  assert.equal(isAutomaticVisitMilestone('atb'), true);
  assert.equal(isAutomaticVisitMilestone('inicioOperacao'), true);
  assert.equal(isAutomaticVisitMilestone('fimOperacao'), true);
  assert.equal(isAutomaticVisitMilestone('atd'), true);
  assert.equal(isAutomaticVisitMilestone('ata'), false);
  assert.equal(isAutomaticVisitMilestone('eta'), false);
});

test('aceita uma cronologia válida', () => {
  const errors = validateVisitMilestones({
    eta: '2026-07-23T08:00',
    ata: '2026-07-23T09:00',
    etb: '2026-07-23T10:00',
    atb: '2026-07-23T10:30',
    inicioOperacao: '2026-07-23T11:00',
    fimOperacao: '2026-07-23T18:00',
    etd: '2026-07-23T19:00',
    atd: '2026-07-23T19:30',
    janelaRecebimentoInicio: '2026-07-22T08:00',
    janelaRecebimentoFim: '2026-07-23T07:00'
  });
  assert.deepEqual(errors, []);
});

test('informa todos os bloqueios cronológicos antes do envio', () => {
  const errors = validateVisitMilestones({
    eta: '2026-07-23T10:00',
    ata: '2026-07-23T09:00',
    etb: '2026-07-23T08:00',
    etd: '2026-07-23T07:00',
    janelaRecebimentoInicio: '2026-07-23T12:00',
    janelaRecebimentoFim: '2026-07-23T11:00'
  });
  assert.deepEqual(errors, [
    'ATA não pode ser anterior ao ETA.',
    'ETB não pode ser anterior ao ETA.',
    'ETD não pode ser anterior ao ETA.',
    'Fim da janela de recebimento não pode ser anterior ao início.'
  ]);
});