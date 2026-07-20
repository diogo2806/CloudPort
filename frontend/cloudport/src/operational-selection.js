import { useSyncExternalStore } from 'react';

const STORAGE_KEY = 'cloudport.operational-selection.v1';

export const OPERATIONAL_DOMAINS = Object.freeze({
  vessel: 'Navio',
  yard: 'Pátio',
  rail: 'Ferrovia',
  equipment: 'Equipamento'
});

const DOMAIN_KEYS = Object.keys(OPERATIONAL_DOMAINS);
const listeners = new Set();

function cleanText(value, maximumLength = 180) {
  if (value === undefined || value === null) return '';
  return String(value).trim().slice(0, maximumLength);
}

function normalizeRecord(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return null;
  const entries = Object.entries(value)
    .slice(0, 24)
    .map(([key, item]) => {
      if (item === undefined || item === null || item === '') return null;
      if (typeof item === 'number' || typeof item === 'boolean') return [cleanText(key, 60), item];
      return [cleanText(key, 60), cleanText(item, 180)];
    })
    .filter(Boolean);
  return entries.length ? Object.fromEntries(entries) : null;
}

function emptySnapshot() {
  return {
    revision: 0,
    active: null,
    contexts: {
      vessel: null,
      yard: null,
      rail: null,
      equipment: null
    },
    simulation: null
  };
}

function readStoredSnapshot() {
  try {
    const raw = globalThis.sessionStorage?.getItem(STORAGE_KEY);
    if (!raw) return emptySnapshot();
    const parsed = JSON.parse(raw);
    return {
      ...emptySnapshot(),
      ...parsed,
      contexts: { ...emptySnapshot().contexts, ...(parsed?.contexts ?? {}) }
    };
  } catch {
    return emptySnapshot();
  }
}

let snapshot = readStoredSnapshot();

function persist() {
  try {
    globalThis.sessionStorage?.setItem(STORAGE_KEY, JSON.stringify(snapshot));
  } catch {
    // O workspace continua funcional mesmo quando o navegador bloqueia storage.
  }
}

function emit(nextSnapshot) {
  snapshot = nextSnapshot;
  persist();
  listeners.forEach((listener) => listener());
}

function contextIdentifier(context) {
  const location = context.location ?? {};
  return [
    context.domain,
    context.unitCode,
    context.slotId,
    context.stackKey,
    location.bay,
    location.bloco,
    location.linha,
    location.coluna,
    location.tier
  ].filter((value) => value !== undefined && value !== null && value !== '').join(':');
}

export function normalizeOperationalSelection(value) {
  if (!value || typeof value !== 'object') return null;
  const domain = DOMAIN_KEYS.includes(value.domain) ? value.domain : '';
  if (!domain) return null;
  const normalized = {
    domain,
    id: cleanText(value.id, 220),
    label: cleanText(value.label, 220),
    unitCode: cleanText(value.unitCode ?? value.codigoConteiner ?? value.containerCode, 80),
    slotId: cleanText(value.slotId, 100),
    stackKey: cleanText(value.stackKey, 120),
    workInstruction: cleanText(value.workInstruction ?? value.workInstructionId ?? value.ordemId, 120),
    equipment: cleanText(value.equipment ?? value.equipamento, 120),
    rail: cleanText(value.rail ?? value.vagao ?? value.linhaFerroviaria, 120),
    location: normalizeRecord(value.location),
    origin: normalizeRecord(value.origin),
    destination: normalizeRecord(value.destination),
    metadata: normalizeRecord(value.metadata),
    source: cleanText(value.source, 120),
    selectedAt: cleanText(value.selectedAt, 60) || new Date().toISOString()
  };
  normalized.id = normalized.id || contextIdentifier(normalized) || `${domain}:${normalized.selectedAt}`;
  normalized.label = normalized.label || normalized.unitCode || OPERATIONAL_DOMAINS[domain];
  return normalized;
}

export function subscribeOperationalSelection(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getOperationalSelectionSnapshot() {
  return snapshot;
}

export function useOperationalSelection() {
  return useSyncExternalStore(
    subscribeOperationalSelection,
    getOperationalSelectionSnapshot,
    getOperationalSelectionSnapshot
  );
}

export function publishOperationalSelection(value) {
  const primary = normalizeOperationalSelection(value);
  if (!primary) return null;
  const contexts = { ...snapshot.contexts, [primary.domain]: primary };
  const related = Array.isArray(value.related) ? value.related : [];
  related.forEach((item) => {
    const normalized = normalizeOperationalSelection({ ...item, unitCode: item.unitCode || primary.unitCode });
    if (normalized) contexts[normalized.domain] = normalized;
  });
  emit({
    ...snapshot,
    revision: snapshot.revision + 1,
    active: primary,
    contexts
  });
  return primary;
}

export function activateOperationalContext(domain) {
  const context = snapshot.contexts?.[domain];
  if (!context) return null;
  emit({ ...snapshot, revision: snapshot.revision + 1, active: context });
  return context;
}

export function clearOperationalSelection(domain) {
  if (!domain) {
    emit(emptySnapshot());
    return;
  }
  if (!DOMAIN_KEYS.includes(domain)) return;
  const contexts = { ...snapshot.contexts, [domain]: null };
  emit({
    ...snapshot,
    revision: snapshot.revision + 1,
    contexts,
    active: snapshot.active?.domain === domain ? null : snapshot.active,
    simulation: snapshot.simulation && [snapshot.simulation.source?.domain, snapshot.simulation.target?.domain].includes(domain)
      ? null
      : snapshot.simulation
  });
}

export function prepareOperationalSimulation(sourceDomain, targetDomain) {
  const source = snapshot.contexts?.[sourceDomain] ?? null;
  const target = snapshot.contexts?.[targetDomain] ?? null;
  const reasons = [];
  if (!source) reasons.push(`Selecione um contexto de ${OPERATIONAL_DOMAINS[sourceDomain] ?? sourceDomain}.`);
  if (!target) reasons.push(`Selecione um contexto de ${OPERATIONAL_DOMAINS[targetDomain] ?? targetDomain}.`);
  if (sourceDomain === targetDomain) reasons.push('Origem e destino devem pertencer a domínios diferentes.');
  if (source?.unitCode && target?.unitCode && source.unitCode !== target.unitCode) {
    reasons.push(`A origem contém ${source.unitCode}, mas o destino está associado a ${target.unitCode}.`);
  }
  if (source && !source.unitCode) reasons.push('A origem não identifica a unidade operacional.');
  if (target && !target.location && !target.destination) reasons.push('O destino não possui posição operacional identificável.');
  const simulation = {
    id: globalThis.crypto?.randomUUID?.() ?? `sim-${Date.now()}`,
    source,
    target,
    valid: reasons.length === 0,
    reasons,
    status: reasons.length ? 'BLOCKED' : 'SIMULATED',
    reason: '',
    createdAt: new Date().toISOString()
  };
  emit({ ...snapshot, revision: snapshot.revision + 1, simulation });
  return simulation;
}

export function markOperationalSimulationReady(reason) {
  const simulation = snapshot.simulation;
  if (!simulation?.valid) return { ok: false, error: 'A simulação possui bloqueios e não pode seguir para confirmação.' };
  const normalizedReason = cleanText(reason, 500);
  if (normalizedReason.length < 3) return { ok: false, error: 'Informe o motivo operacional da proposta.' };
  const next = {
    ...simulation,
    status: 'READY_FOR_TRANSACTION',
    reason: normalizedReason,
    readyAt: new Date().toISOString()
  };
  emit({ ...snapshot, revision: snapshot.revision + 1, simulation: next });
  return { ok: true, simulation: next };
}

export function cancelOperationalSimulation() {
  if (!snapshot.simulation) return;
  emit({ ...snapshot, revision: snapshot.revision + 1, simulation: null });
}

export function formatOperationalLocation(context) {
  const location = context?.location ?? {};
  const parts = [];
  if (location.navio) parts.push(location.navio);
  if (location.bay !== undefined) parts.push(`B${location.bay}`);
  if (location.row !== undefined) parts.push(`R${location.row}`);
  if (location.bloco !== undefined) parts.push(`Bloco ${location.bloco}`);
  if (location.linha !== undefined) parts.push(`L${location.linha}`);
  if (location.coluna !== undefined) parts.push(`C${location.coluna}`);
  if (location.tier !== undefined) parts.push(`T${location.tier}`);
  if (location.slot !== undefined) parts.push(`Slot ${location.slot}`);
  return parts.join(' · ') || 'Posição não informada';
}

export function resetOperationalSelectionForTests() {
  snapshot = emptySnapshot();
  listeners.forEach((listener) => listener());
}
