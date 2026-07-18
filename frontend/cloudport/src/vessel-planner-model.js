export const VESSEL_VIEW_MODES = Object.freeze([
  { value: 'MULTI', label: 'Multivisão' },
  { value: 'PROFILE', label: 'Perfil' },
  { value: 'TOP', label: 'Superior' },
  { value: 'SECTION', label: 'Seção' },
  { value: 'TIER', label: 'Tier' }
]);

export const VESSEL_LEGEND_MODES = Object.freeze([
  { value: 'POD', label: 'POD' },
  { value: 'WEIGHT', label: 'Peso' },
  { value: 'IMO', label: 'IMO' },
  { value: 'REEFER', label: 'Reefer' },
  { value: 'OPERATOR', label: 'Operador' }
]);

function asNumber(value, fallback = 0) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

function normalizedText(value, fallback = '') {
  const text = String(value ?? '').trim();
  return text || fallback;
}

export function slotPositionKey(slot) {
  if (!slot) return '';
  return `${asNumber(slot.bay)}:${asNumber(slot.rowBay ?? slot.row)}:${asNumber(slot.tier)}`;
}

export function stackPositionKey(slot) {
  if (!slot) return '';
  return `${asNumber(slot.bay)}:${asNumber(slot.rowBay ?? slot.row)}`;
}

export function normalizeSlots(plan) {
  const slots = Array.isArray(plan?.slots) ? plan.slots : [];
  return slots
    .filter(Boolean)
    .map((slot) => ({
      ...slot,
      bay: asNumber(slot.bay),
      rowBay: asNumber(slot.rowBay ?? slot.row),
      tier: asNumber(slot.tier)
    }))
    .sort((left, right) => left.bay - right.bay || left.rowBay - right.rowBay || left.tier - right.tier);
}

export function uniqueCoordinates(slots, field) {
  return Array.from(new Set((Array.isArray(slots) ? slots : [])
    .map((slot) => asNumber(slot?.[field]))))
    .sort((left, right) => left - right);
}

export function buildContainerIndex(containers) {
  return (Array.isArray(containers) ? containers : []).reduce((index, container) => {
    const code = normalizedText(container?.codigoContainer).toUpperCase();
    if (code) index[code] = container;
    return index;
  }, {});
}

export function resolveContainerMetadata(slot, containerIndex = {}) {
  const code = normalizedText(slot?.codigoContainer).toUpperCase();
  return code ? containerIndex[code] ?? {} : {};
}

export function resolveOperator(slot, containerIndex = {}) {
  const metadata = resolveContainerMetadata(slot, containerIndex);
  const candidates = [
    metadata.operador,
    metadata.operadorMaritimo,
    metadata.lineOperator,
    metadata.armador,
    metadata.codigoArmador,
    metadata.codigoOperador,
    metadata.codigoLinha,
    slot?.operador,
    slot?.operadorMaritimo,
    slot?.lineOperator,
    slot?.armador
  ];
  return normalizedText(candidates.find((value) => normalizedText(value)), 'Não informado');
}

export function weightBand(weightKg) {
  const weight = asNumber(weightKg);
  if (weight <= 0) return 'Sem peso';
  if (weight < 10000) return '< 10 t';
  if (weight < 20000) return '10–20 t';
  if (weight < 30000) return '20–30 t';
  return '≥ 30 t';
}

export function legendValueForSlot(slot, mode, containerIndex = {}) {
  if (!slot?.codigoContainer) return 'Livre';
  const metadata = resolveContainerMetadata(slot, containerIndex);
  switch (mode) {
    case 'WEIGHT':
      return weightBand(slot.pesoVgmKg ?? slot.pesoKg ?? metadata.pesoVgmKg ?? metadata.pesoKg);
    case 'IMO':
      return slot.perigoso || metadata.perigoso
        ? `IMO ${normalizedText(slot.classeImo ?? metadata.classeImo, 'N/I')}`
        : 'Não perigoso';
    case 'REEFER':
      return slot.reefer || metadata.reefer ? 'Reefer' : 'Dry';
    case 'OPERATOR':
      return resolveOperator(slot, containerIndex);
    case 'POD':
    default:
      return normalizedText(slot.portoDescarga ?? metadata.portoDescarga, 'POD não informado');
  }
}

export function toneIndex(value, total = 12) {
  const text = normalizedText(value, 'N/I');
  let hash = 0;
  for (let index = 0; index < text.length; index += 1) {
    hash = ((hash << 5) - hash + text.charCodeAt(index)) | 0;
  }
  return Math.abs(hash) % total;
}

export function buildLegend(slots, mode, containerIndex = {}) {
  const counts = new Map();
  (Array.isArray(slots) ? slots : []).forEach((slot) => {
    if (!slot?.codigoContainer) return;
    const value = legendValueForSlot(slot, mode, containerIndex);
    counts.set(value, (counts.get(value) ?? 0) + 1);
  });
  return Array.from(counts.entries())
    .map(([value, count]) => ({ value, count, tone: toneIndex(value) }))
    .sort((left, right) => right.count - left.count || left.value.localeCompare(right.value, 'pt-BR'));
}

export function buildStackSummaries(slots) {
  const summaries = {};
  (Array.isArray(slots) ? slots : []).forEach((slot) => {
    const key = stackPositionKey(slot);
    const summary = summaries[key] ?? {
      key,
      bay: asNumber(slot?.bay),
      rowBay: asNumber(slot?.rowBay ?? slot?.row),
      occupied: 0,
      capacity: 0,
      weightKg: 0,
      maxWeightKg: null,
      maxTier: 0,
      occupiedTiers: []
    };
    summary.capacity += 1;
    summary.maxTier = Math.max(summary.maxTier, asNumber(slot?.tier));
    const limit = Number(slot?.maxPesoPilhaKg);
    if (Number.isFinite(limit) && limit > 0) {
      summary.maxWeightKg = summary.maxWeightKg === null ? limit : Math.min(summary.maxWeightKg, limit);
    }
    if (slot?.codigoContainer) {
      summary.occupied += 1;
      summary.weightKg += asNumber(slot.pesoVgmKg ?? slot.pesoKg);
      summary.occupiedTiers.push(asNumber(slot.tier));
    }
    summaries[key] = summary;
  });
  Object.values(summaries).forEach((summary) => {
    summary.ratio = summary.maxWeightKg ? summary.weightKg / summary.maxWeightKg : 0;
    summary.percent = summary.maxWeightKg ? Math.round(summary.ratio * 100) : null;
    summary.status = summary.ratio > 1 ? 'EXCEDIDO' : summary.ratio >= 0.85 ? 'ATENCAO' : 'OK';
    summary.occupiedTiers.sort((left, right) => left - right);
  });
  return summaries;
}

export function buildViolationIndex(stability) {
  const index = { __global__: [] };
  const violations = Array.isArray(stability?.violacoes) ? stability.violacoes : [];
  violations.forEach((violation) => {
    const key = violation?.slotId === undefined || violation?.slotId === null
      ? '__global__'
      : String(violation.slotId);
    if (!index[key]) index[key] = [];
    index[key].push(violation);
  });
  return index;
}

export function buildRestowIndex(restow) {
  const index = {};
  const movements = Array.isArray(restow?.movimentos) ? restow.movimentos : [];
  movements.forEach((movement) => {
    const sourceKey = `${asNumber(movement.bayAtual)}:${asNumber(movement.rowAtual)}:${asNumber(movement.tierAtual)}`;
    const destinationKey = `${asNumber(movement.bayDestino)}:${asNumber(movement.rowDestino)}:${asNumber(movement.tierDestino)}`;
    index[sourceKey] = { ...movement, role: 'ORIGEM' };
    index[destinationKey] = { ...movement, role: 'DESTINO' };
  });
  return index;
}

export function buildCraneIndex(sequencing) {
  const index = {};
  const operations = Array.isArray(sequencing?.sequencia) ? sequencing.sequencia : [];
  operations.forEach((operation) => {
    const key = `${asNumber(operation.bay)}:${asNumber(operation.rowBay)}:${asNumber(operation.tier)}`;
    if (!index[key]) index[key] = [];
    index[key].push(operation);
  });
  Object.values(index).forEach((items) => items.sort((left, right) => asNumber(left.ordem) - asNumber(right.ordem)));
  return index;
}

export function chooseDropSlot(slots, bay, rowBay) {
  return (Array.isArray(slots) ? slots : [])
    .filter((slot) => asNumber(slot?.bay) === asNumber(bay)
      && asNumber(slot?.rowBay ?? slot?.row) === asNumber(rowBay)
      && !slot?.codigoContainer
      && !slot?.restrito)
    .sort((left, right) => asNumber(left.tier) - asNumber(right.tier))[0] ?? null;
}

export function buildSlotWarnings(slot, indexes = {}, stackSummary = null) {
  if (!slot) return [];
  const warnings = [];
  const violations = indexes.violations?.[String(slot.id)] ?? [];
  violations.forEach((violation) => warnings.push({
    type: normalizedText(violation.tipo, 'VALIDACAO'),
    severity: normalizedText(violation.severidade, 'AVISO'),
    message: normalizedText(violation.descricao ?? violation.mensagem, 'Violação operacional')
  }));
  if (slot.restrito) warnings.push({
    type: 'RESTRICAO',
    severity: 'PERIGO',
    message: normalizedText(slot.motivoRestricao, 'Slot restrito')
  });
  if (slot.reefer && !slot.tomadaReefer) warnings.push({
    type: 'REEFER',
    severity: 'PERIGO',
    message: 'Contêiner reefer em slot sem tomada reefer.'
  });
  if (slot.perigoso && !normalizedText(slot.classeImo)) warnings.push({
    type: 'IMDG',
    severity: 'AVISO',
    message: 'Carga perigosa sem classe IMO informada.'
  });
  if (stackSummary?.status === 'EXCEDIDO') warnings.push({
    type: 'PESO_PILHA',
    severity: 'PERIGO',
    message: `Peso acumulado da pilha excede o limite em ${Math.max(0, (stackSummary.percent ?? 100) - 100)}%.`
  });
  if (stackSummary?.status === 'ATENCAO') warnings.push({
    type: 'PESO_PILHA',
    severity: 'AVISO',
    message: `Peso acumulado da pilha em ${stackSummary.percent}% do limite.`
  });
  if (normalizedText(slot.statusAlertas).toUpperCase() === 'AVISO' && warnings.length === 0) warnings.push({
    type: 'SLOT',
    severity: 'AVISO',
    message: 'O backend sinalizou aviso para este slot.'
  });
  return warnings;
}

export function lashingRiskForSlot(slot, stackSummary) {
  if (!slot?.codigoContainer) return 'NONE';
  const highTier = asNumber(slot.tier) >= 82;
  const ratio = asNumber(stackSummary?.ratio);
  if (slot.oog || ratio > 1 || (highTier && asNumber(slot.pesoVgmKg ?? slot.pesoKg) >= 28000)) return 'HIGH';
  if (ratio >= 0.85 || highTier || slot.perigoso) return 'MEDIUM';
  return 'LOW';
}

export function structuralRiskForSlot(slot, stackSummary) {
  if (!slot?.codigoContainer) return 'NONE';
  const ratio = asNumber(stackSummary?.ratio);
  const slotLimit = asNumber(slot.maxPesoKg);
  const weight = asNumber(slot.pesoVgmKg ?? slot.pesoKg);
  if ((slotLimit > 0 && weight > slotLimit) || ratio > 1) return 'HIGH';
  if ((slotLimit > 0 && weight / slotLimit >= 0.85) || ratio >= 0.85) return 'MEDIUM';
  return 'LOW';
}

export function stabilityRiskForSlot(slot, violationIndex = {}) {
  const violations = violationIndex[String(slot?.id)] ?? [];
  if (violations.some((item) => normalizedText(item.severidade).toUpperCase() === 'PERIGO')) return 'HIGH';
  if (violations.length || normalizedText(slot?.statusAlertas).toUpperCase() === 'AVISO') return 'MEDIUM';
  return slot?.codigoContainer ? 'LOW' : 'NONE';
}

export function toAllocationPayload(source) {
  const item = source?.container ?? source?.slot ?? source ?? {};
  return {
    codigoContainer: item.codigoContainer,
    isoCode: item.isoCode,
    pesoKg: item.pesoKg,
    pesoVgmKg: item.pesoVgmKg,
    estadoCarga: item.estadoCarga,
    portoCarga: item.portoCarga,
    portoDescarga: item.portoDescarga,
    classeImo: item.classeImo ?? '',
    numeroOnu: item.numeroOnu ?? '',
    grupoSegregacao: item.grupoSegregacao ?? '',
    perigoso: Boolean(item.perigoso || item.classeImo),
    reefer: Boolean(item.reefer),
    temperaturaRequeridaC: item.temperaturaRequeridaC,
    temperaturaMinimaC: item.temperaturaMinimaC,
    temperaturaMaximaC: item.temperaturaMaximaC,
    oog: Boolean(item.oog),
    excessoFrontalCm: item.excessoFrontalCm,
    excessoTraseiroCm: item.excessoTraseiroCm,
    excessoEsquerdoCm: item.excessoEsquerdoCm,
    excessoDireitoCm: item.excessoDireitoCm,
    excessoAlturaCm: item.excessoAlturaCm
  };
}

export function formatSlotPosition(slot) {
  if (!slot) return '—';
  return `B${String(asNumber(slot.bay)).padStart(3, '0')} · R${String(asNumber(slot.rowBay ?? slot.row)).padStart(2, '0')} · T${String(asNumber(slot.tier)).padStart(2, '0')}`;
}
