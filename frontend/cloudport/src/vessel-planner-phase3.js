import {
  lashingRiskForSlot,
  resolveContainerMetadata,
  stabilityRiskForSlot,
  structuralRiskForSlot
} from './vessel-planner-model.js';

export const VESSEL_OVERLAY_MODES = Object.freeze([
  { value: 'NONE', label: 'Sem overlay', shortLabel: 'OFF' },
  { value: 'IMDG', label: 'Segregação IMDG', shortLabel: 'IMDG' },
  { value: 'STABILITY', label: 'Estabilidade', shortLabel: 'EST' },
  { value: 'LASHING', label: 'Lashing', shortLabel: 'LSH' },
  { value: 'STRUCTURAL', label: 'Força estrutural', shortLabel: 'STR' },
  { value: 'COMBINED', label: 'Risco combinado', shortLabel: 'ALL' }
]);

export const OVERLAY_RISK = Object.freeze({
  NONE: 'NONE',
  LOW: 'LOW',
  MEDIUM: 'MEDIUM',
  HIGH: 'HIGH'
});

const RISK_ORDER = Object.freeze({ NONE: 0, LOW: 1, MEDIUM: 2, HIGH: 3 });

function numberValue(value, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function textValue(value, fallback = '') {
  const text = String(value ?? '').trim();
  return text || fallback;
}

function normalized(value) {
  return textValue(value).toUpperCase();
}

function slotId(slot) {
  return String(slot?.id ?? '');
}

function positionKey(slot) {
  return `${numberValue(slot?.bay)}:${numberValue(slot?.rowBay ?? slot?.row)}:${numberValue(slot?.tier)}`;
}

function dangerousMetadata(slot, containerIndex = {}) {
  const metadata = resolveContainerMetadata(slot, containerIndex);
  const dangerous = Boolean(slot?.perigoso || slot?.classeImo || metadata?.perigoso || metadata?.classeImo);
  return {
    dangerous,
    classeImo: textValue(slot?.classeImo ?? metadata?.classeImo, 'N/I'),
    numeroOnu: textValue(slot?.numeroOnu ?? metadata?.numeroOnu, 'N/I'),
    grupoSegregacao: textValue(slot?.grupoSegregacao ?? metadata?.grupoSegregacao, 'N/I')
  };
}

function isImdgViolation(violation) {
  const haystack = [violation?.tipo, violation?.codigo, violation?.descricao, violation?.mensagem, violation?.regra]
    .map(normalized)
    .join(' ');
  return /IMDG|SEGREG|PERIGOS|DANGEROUS/.test(haystack);
}

function relatedSlotIds(violation) {
  const candidates = [
    violation?.slotRelacionadoId,
    violation?.outroSlotId,
    violation?.slotConflitanteId,
    violation?.alvoSlotId,
    violation?.origemSlotId,
    violation?.destinoSlotId
  ];
  return candidates.filter((value) => value !== undefined && value !== null).map(String);
}

function adjacentDangerousSlots(slot, dangerousSlots) {
  return dangerousSlots.filter((candidate) => {
    if (slotId(candidate) === slotId(slot)) return false;
    const bayDistance = Math.abs(numberValue(candidate.bay) - numberValue(slot.bay));
    const rowDistance = Math.abs(numberValue(candidate.rowBay ?? candidate.row) - numberValue(slot.rowBay ?? slot.row));
    const tierDistance = Math.abs(numberValue(candidate.tier) - numberValue(slot.tier));
    return bayDistance <= 2 && rowDistance <= 2 && tierDistance <= 4;
  });
}

export function buildImdgIndex(slots = [], containerIndex = {}, violationIndex = {}) {
  const occupied = (Array.isArray(slots) ? slots : []).filter((slot) => slot?.codigoContainer);
  const dangerousSlots = occupied.filter((slot) => dangerousMetadata(slot, containerIndex).dangerous);
  const index = {};

  dangerousSlots.forEach((slot) => {
    const metadata = dangerousMetadata(slot, containerIndex);
    const violations = (violationIndex[slotId(slot)] ?? []).filter(isImdgViolation);
    const explicitConflict = slot?.segregacaoImdgValida === false
      || slot?.conflitoImdg === true
      || ['CONFLITO', 'INVALIDO', 'REPROVADO'].includes(normalized(slot?.statusSegregacaoImdg ?? slot?.statusSegregacao));
    const nearby = adjacentDangerousSlots(slot, dangerousSlots);
    const related = new Set(violations.flatMap(relatedSlotIds));
    const relatedPositions = nearby.filter((candidate) => related.has(slotId(candidate))).map(positionKey);
    const risk = explicitConflict || violations.length
      ? OVERLAY_RISK.HIGH
      : nearby.length
        ? OVERLAY_RISK.MEDIUM
        : OVERLAY_RISK.LOW;

    index[slotId(slot)] = {
      risk,
      code: 'IMDG',
      label: `IMO ${metadata.classeImo}`,
      shortLabel: `IMO ${metadata.classeImo}`,
      details: [
        `ONU ${metadata.numeroOnu}`,
        `Grupo ${metadata.grupoSegregacao}`,
        violations.length ? `${violations.length} conflito(s) retornado(s) pelo backend` : '',
        nearby.length ? `${nearby.length} carga(s) perigosa(s) na zona gráfica de atenção` : ''
      ].filter(Boolean),
      violations,
      nearbySlotIds: nearby.map(slotId),
      relatedPositions,
      authoritativeConflict: explicitConflict || violations.length > 0
    };
  });

  return index;
}

export function highestRisk(risks = []) {
  return (Array.isArray(risks) ? risks : []).reduce((highest, risk) => {
    const normalizedRisk = normalized(risk) || OVERLAY_RISK.NONE;
    return (RISK_ORDER[normalizedRisk] ?? 0) > (RISK_ORDER[highest] ?? 0) ? normalizedRisk : highest;
  }, OVERLAY_RISK.NONE);
}

function riskDetails(mode, risk, slot, stackSummary, violationIndex, imdgEntry) {
  if (mode === 'IMDG') return imdgEntry?.details ?? [];
  if (mode === 'STABILITY') {
    const violations = violationIndex[slotId(slot)] ?? [];
    if (violations.length) return violations.map((item) => textValue(item.descricao ?? item.mensagem, 'Violação de estabilidade'));
    return risk === OVERLAY_RISK.NONE ? [] : ['Sem violação específica retornada para o slot.'];
  }
  if (mode === 'LASHING') {
    const details = [];
    if (slot?.oog) details.push('Carga fora de gabarito exige verificação de lashing.');
    if (numberValue(slot?.tier) >= 82) details.push('Carga localizada em tier de convés.');
    if (numberValue(stackSummary?.ratio) >= 0.85) details.push('Stack próxima ou acima do limite de peso.');
    return details.length ? details : risk === OVERLAY_RISK.NONE ? [] : ['Risco estimado a partir da posição e do peso da stack.'];
  }
  if (mode === 'STRUCTURAL') {
    const details = [];
    const slotLimit = numberValue(slot?.maxPesoKg ?? slot?.limitePesoKg);
    const weight = numberValue(slot?.pesoVgmKg ?? slot?.pesoKg);
    if (slotLimit > 0) details.push(`Slot utilizando ${Math.round((weight / slotLimit) * 100)}% do limite individual.`);
    if (stackSummary?.maxWeightKg) details.push(`Stack utilizando ${stackSummary.percent ?? 0}% do limite acumulado.`);
    return details.length ? details : risk === OVERLAY_RISK.NONE ? [] : ['Risco estimado a partir dos limites estruturais disponíveis.'];
  }
  return [];
}

export function overlayDescriptorForSlot(slot, mode, context = {}) {
  if (!slot || mode === 'NONE') return { risk: OVERLAY_RISK.NONE, mode, label: 'Sem overlay', shortLabel: '', details: [] };
  const stackSummary = context.stackSummaries?.[`${numberValue(slot.bay)}:${numberValue(slot.rowBay ?? slot.row)}`];
  const imdgEntry = context.imdgIndex?.[slotId(slot)];
  const riskByMode = {
    IMDG: imdgEntry?.risk ?? OVERLAY_RISK.NONE,
    STABILITY: stabilityRiskForSlot(slot, context.violationIndex),
    LASHING: lashingRiskForSlot(slot, stackSummary),
    STRUCTURAL: structuralRiskForSlot(slot, stackSummary)
  };

  if (mode === 'COMBINED') {
    const components = ['IMDG', 'STABILITY', 'LASHING', 'STRUCTURAL'].map((component) => ({
      mode: component,
      risk: riskByMode[component]
    })).filter((item) => item.risk !== OVERLAY_RISK.NONE);
    const risk = highestRisk(components.map((item) => item.risk));
    return {
      risk,
      mode,
      label: 'Risco combinado',
      shortLabel: risk === OVERLAY_RISK.NONE ? '' : 'ALL',
      details: components.map((item) => `${item.mode}: ${item.risk}`),
      components
    };
  }

  const risk = riskByMode[mode] ?? OVERLAY_RISK.NONE;
  const selectedMode = VESSEL_OVERLAY_MODES.find((item) => item.value === mode);
  return {
    risk,
    mode,
    label: selectedMode?.label ?? mode,
    shortLabel: risk === OVERLAY_RISK.NONE ? '' : selectedMode?.shortLabel ?? mode,
    details: riskDetails(mode, risk, slot, stackSummary, context.violationIndex ?? {}, imdgEntry),
    imdg: imdgEntry ?? null
  };
}

export function buildOverlayIndex(slots = [], mode, context = {}) {
  return (Array.isArray(slots) ? slots : []).reduce((index, slot) => {
    index[slotId(slot)] = overlayDescriptorForSlot(slot, mode, context);
    return index;
  }, {});
}

export function aggregateOverlayForSlots(slots = [], overlayIndex = {}) {
  const descriptors = (Array.isArray(slots) ? slots : [])
    .map((slot) => overlayIndex[slotId(slot)])
    .filter(Boolean);
  const risk = highestRisk(descriptors.map((item) => item.risk));
  const highest = descriptors.find((item) => item.risk === risk);
  return {
    risk,
    shortLabel: highest?.shortLabel ?? '',
    details: descriptors.flatMap((item) => item.details ?? []).slice(0, 4)
  };
}

export function overlaySummary(overlayIndex = {}) {
  return Object.values(overlayIndex).reduce((summary, descriptor) => {
    const risk = descriptor?.risk ?? OVERLAY_RISK.NONE;
    summary[risk] = (summary[risk] ?? 0) + 1;
    return summary;
  }, { NONE: 0, LOW: 0, MEDIUM: 0, HIGH: 0 });
}

export function buildCraneLanes(sequencing) {
  const operations = Array.isArray(sequencing?.sequencia) ? sequencing.sequencia : [];
  const lanes = new Map();
  operations.forEach((operation, index) => {
    const craneId = numberValue(operation?.guindasteId, 1);
    const lane = lanes.get(craneId) ?? { craneId, operations: [], blocked: 0 };
    const normalizedOperation = {
      ...operation,
      craneId,
      order: numberValue(operation?.ordem ?? operation?.ordemPlanejada, index + 1),
      position: `${numberValue(operation?.bay)}:${numberValue(operation?.rowBay ?? operation?.row)}:${numberValue(operation?.tier)}`,
      blocked: Boolean(operation?.bloqueadoPorTampa || operation?.bloqueado || normalized(operation?.status) === 'BLOQUEADO')
    };
    lane.operations.push(normalizedOperation);
    if (normalizedOperation.blocked) lane.blocked += 1;
    lanes.set(craneId, lane);
  });
  return Array.from(lanes.values())
    .map((lane) => ({ ...lane, operations: lane.operations.sort((left, right) => left.order - right.order) }))
    .sort((left, right) => left.craneId - right.craneId);
}

export function buildRestowFlows(restow) {
  const movements = Array.isArray(restow?.movimentos) ? restow.movimentos : [];
  return movements.map((movement, index) => ({
    ...movement,
    id: movement?.id ?? `${movement?.codigoContainer ?? 'RESTOW'}-${index}`,
    order: numberValue(movement?.ordem ?? movement?.sequencia, index + 1),
    containerCode: textValue(movement?.codigoContainer, 'Contêiner N/I'),
    source: {
      bay: numberValue(movement?.bayAtual ?? movement?.bayOrigem),
      rowBay: numberValue(movement?.rowAtual ?? movement?.rowOrigem),
      tier: numberValue(movement?.tierAtual ?? movement?.tierOrigem)
    },
    destination: {
      bay: numberValue(movement?.bayDestino),
      rowBay: numberValue(movement?.rowDestino),
      tier: numberValue(movement?.tierDestino)
    },
    reason: textValue(movement?.motivo ?? movement?.descricao, 'Reposicionamento necessário para acesso operacional.'),
    status: textValue(movement?.status, 'PLANEJADO')
  })).sort((left, right) => left.order - right.order);
}

export function findSlotByPosition(slots = [], position) {
  if (!position) return null;
  return (Array.isArray(slots) ? slots : []).find((slot) => numberValue(slot?.bay) === numberValue(position?.bay)
    && numberValue(slot?.rowBay ?? slot?.row) === numberValue(position?.rowBay ?? position?.row)
    && numberValue(slot?.tier) === numberValue(position?.tier)) ?? null;
}
