export const JOURNEY_STATES = Object.freeze({
  COMPLETED: 'COMPLETED',
  CURRENT: 'CURRENT',
  FUTURE: 'FUTURE',
  BLOCKED: 'BLOCKED',
  CANCELLED: 'CANCELLED'
});

export function normalizeRoles(session) {
  return [...(Array.isArray(session?.roles) ? session.roles : []), session?.perfil]
    .filter(Boolean)
    .map((role) => String(role).replace(/^ROLE_/, '').toUpperCase());
}

export function canAccessStep(step, roles) {
  const required = Array.isArray(step?.requiredRoles) ? step.requiredRoles : [];
  return required.length === 0 || required.some((role) => roles.includes(String(role).replace(/^ROLE_/, '').toUpperCase()));
}

export function buildOperationalJourney({ steps = [], currentStepId, roles = [], blockers = [], cancelled = false }) {
  const currentIndex = steps.findIndex((step) => String(step.id) === String(currentStepId));
  return steps.map((step, index) => {
    const permitted = canAccessStep(step, roles);
    const stepBlockers = blockers.filter((blocker) => !blocker.stepId || String(blocker.stepId) === String(step.id));
    let state = JOURNEY_STATES.FUTURE;
    if (cancelled) state = JOURNEY_STATES.CANCELLED;
    else if (!permitted || stepBlockers.length) state = JOURNEY_STATES.BLOCKED;
    else if (currentIndex >= 0 && index < currentIndex) state = JOURNEY_STATES.COMPLETED;
    else if (currentIndex === index) state = JOURNEY_STATES.CURRENT;

    return {
      ...step,
      state,
      permitted,
      blockers: stepBlockers,
      blockingReason: !permitted
        ? `Permissão necessária: ${(step.requiredRoles ?? []).join(', ')}`
        : stepBlockers.map((blocker) => blocker.reason).filter(Boolean).join(' · ')
    };
  });
}

export function nextJourneyAction(journey) {
  const current = journey.find((step) => step.state === JOURNEY_STATES.CURRENT);
  if (!current) {
    const blocked = journey.find((step) => step.state === JOURNEY_STATES.BLOCKED);
    return blocked ? { label: 'Resolver bloqueio', step: blocked, disabled: true } : null;
  }
  return {
    label: current.nextAction || `Concluir ${current.label}`,
    step: current,
    disabled: false
  };
}
