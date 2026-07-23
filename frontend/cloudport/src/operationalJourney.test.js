import assert from 'node:assert/strict';
import test from 'node:test';
import { buildOperationalJourney, JOURNEY_STATES, nextJourneyAction } from './operationalJourney.js';

const steps = [
  { id: 1, label: 'Check-in', requiredRoles: ['OPERADOR_GATE'] },
  { id: 2, label: 'Inspeção', requiredRoles: ['OPERADOR_GATE'], nextAction: 'Concluir inspeção' },
  { id: 3, label: 'Saída', requiredRoles: ['OPERADOR_GATE'] }
];

test('classifica etapas concluída, atual e futura', () => {
  const journey = buildOperationalJourney({ steps, currentStepId: 2, roles: ['OPERADOR_GATE'] });
  assert.deepEqual(journey.map((step) => step.state), [JOURNEY_STATES.COMPLETED, JOURNEY_STATES.CURRENT, JOURNEY_STATES.FUTURE]);
  assert.equal(nextJourneyAction(journey).label, 'Concluir inspeção');
});

test('bloqueia etapa por validação pendente', () => {
  const journey = buildOperationalJourney({ steps, currentStepId: 2, roles: ['OPERADOR_GATE'], blockers: [{ stepId: 2, reason: 'Motorista não verificado' }] });
  assert.equal(journey[1].state, JOURNEY_STATES.BLOCKED);
  assert.match(journey[1].blockingReason, /Motorista/);
});

test('bloqueia etapa sem permissão', () => {
  const journey = buildOperationalJourney({ steps, currentStepId: 1, roles: ['PLANEJADOR'] });
  assert.equal(journey[0].state, JOURNEY_STATES.BLOCKED);
  assert.match(journey[0].blockingReason, /OPERADOR_GATE/);
});

test('marca toda a jornada como cancelada', () => {
  const journey = buildOperationalJourney({ steps, currentStepId: 1, roles: ['OPERADOR_GATE'], cancelled: true });
  assert.ok(journey.every((step) => step.state === JOURNEY_STATES.CANCELLED));
});
