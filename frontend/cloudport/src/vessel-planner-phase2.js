export const DROP_VALIDATION_STATUS = Object.freeze({
  VALID: 'VALID',
  WARNING: 'WARNING',
  BLOCKED: 'BLOCKED'
});

function asNumber(value, fallback = 0) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

function normalizedText(value) {
  return String(value ?? '').trim();
}

function sourceItem(payload) {
  return payload?.slot ?? payload?.container ?? payload ?? null;
}

export function dragSource(payload) {
  const item = sourceItem(payload);
  if (!item?.codigoContainer) return null;
  return {
    ...item,
    kind: payload?.kind ?? (payload?.slot ? 'slot' : 'container'),
    sourceSlotId: payload?.slot?.id ?? null,
    bay: payload?.slot?.bay ?? item.bay,
    rowBay: payload?.slot?.rowBay ?? payload?.slot?.row ?? item.rowBay ?? item.row,
    tier: payload?.slot?.tier ?? item.tier
  };
}

export function containerWeightKg(payload) {
  const item = sourceItem(payload);
  return Math.max(0, asNumber(item?.pesoVgmKg ?? item?.pesoKg));
}

export function stackKey(slot) {
  if (!slot) return '';
  return `${asNumber(slot.bay)}:${asNumber(slot.rowBay ?? slot.row)}`;
}

export function currentHatchTask(cover) {
  const tasks = Array.isArray(cover?.tarefas) ? cover.tarefas : [];
  return [...tasks].reverse().find((task) => ['PLANEJADA', 'EM_EXECUCAO'].includes(String(task?.status ?? '').toUpperCase())) ?? null;
}

export function hatchCoverForSlot(slot, covers = []) {
  const code = normalizedText(slot?.codigoHatchCover).toUpperCase();
  if (!code) return null;
  return (Array.isArray(covers) ? covers : []).find((cover) => normalizedText(cover?.codigo).toUpperCase() === code) ?? null;
}

export function slotDeckZone(slot) {
  const explicit = normalizedText(slot?.zonaNavio ?? slot?.areaNavio ?? slot?.tipoArea ?? slot?.localizacaoVertical).toUpperCase();
  if (/PORAO|HOLD|BELOW/.test(explicit) || slot?.sobConves === true || slot?.porao === true) return 'HOLD';
  if (/CONVES|DECK|ABOVE/.test(explicit) || slot?.sobreConves === true) return 'DECK';
  return asNumber(slot?.tier) >= 80 ? 'DECK' : 'HOLD';
}

export function hatchRestriction(slot, covers = []) {
  if (!slot?.codigoHatchCover) return null;
  const cover = hatchCoverForSlot(slot, covers);
  if (!cover) return {
    status: DROP_VALIDATION_STATUS.WARNING,
    code: 'HATCH_NOT_LOADED',
    message: `Estado da tampa ${slot.codigoHatchCover} não carregado.`
  };

  const state = normalizedText(cover.estado).toUpperCase();
  const zone = slotDeckZone(slot);
  const task = currentHatchTask(cover);
  if (task?.status === 'EM_EXECUCAO') return {
    status: DROP_VALIDATION_STATUS.BLOCKED,
    code: 'HATCH_OPERATION',
    message: `Tampa ${cover.codigo} em operação ${task.tipo}.`
  };
  if (state === 'POSICIONADA') return {
    status: DROP_VALIDATION_STATUS.BLOCKED,
    code: 'HATCH_POSITIONED',
    message: `Tampa ${cover.codigo} posicionada, mas ainda não confirmada como fechada.`
  };
  if (zone === 'HOLD' && state === 'FECHADA') return {
    status: DROP_VALIDATION_STATUS.BLOCKED,
    code: 'HATCH_CLOSED',
    message: `Tampa ${cover.codigo} fechada impede acesso ao porão.`
  };
  if (zone === 'DECK' && ['ABERTA', 'REMOVIDA'].includes(state)) return {
    status: DROP_VALIDATION_STATUS.BLOCKED,
    code: 'HATCH_OPEN',
    message: `Tampa ${cover.codigo} ${state.toLowerCase()} impede ocupação sobre a abertura.`
  };
  if (task?.status === 'PLANEJADA') return {
    status: DROP_VALIDATION_STATUS.WARNING,
    code: 'HATCH_TASK_PLANNED',
    message: `Tampa ${cover.codigo} possui tarefa ${task.tipo} planejada.`
  };
  return null;
}

function allowedIsoCodes(target) {
  const raw = target?.isoCodesPermitidos ?? target?.tiposIsoPermitidos ?? target?.isoCodePermitido;
  if (Array.isArray(raw)) return raw.map((value) => normalizedText(value).toUpperCase()).filter(Boolean);
  return normalizedText(raw).split(/[,;\s]+/).map((value) => value.toUpperCase()).filter(Boolean);
}

function weightLimitForStack(target, summary) {
  const candidates = [summary?.maxWeightKg, target?.maxPesoPilhaKg, target?.limitePesoPilhaKg]
    .map((value) => Number(value))
    .filter((value) => Number.isFinite(value) && value > 0);
  return candidates.length ? Math.min(...candidates) : null;
}

function sourceHasLoadAbove(source, slots) {
  if (!source?.sourceSlotId) return false;
  return slots.some((slot) => asNumber(slot.bay) === asNumber(source.bay)
    && asNumber(slot.rowBay ?? slot.row) === asNumber(source.rowBay)
    && asNumber(slot.tier) > asNumber(source.tier)
    && Boolean(slot.codigoContainer));
}

function targetHasGapBelow(target, slots, source) {
  const lower = slots
    .filter((slot) => asNumber(slot.bay) === asNumber(target.bay)
      && asNumber(slot.rowBay ?? slot.row) === asNumber(target.rowBay ?? target.row)
      && asNumber(slot.tier) < asNumber(target.tier)
      && !slot.restrito)
    .sort((left, right) => asNumber(right.tier) - asNumber(left.tier));
  if (!lower.length) return false;
  const immediate = lower[0];
  if (String(immediate.id) === String(source?.sourceSlotId)) return true;
  return !immediate.codigoContainer;
}

export function projectedStackWeight(target, payload, stackSummaries = {}) {
  const source = dragSource(payload);
  const targetKey = stackKey(target);
  const sourceKey = source?.sourceSlotId ? stackKey(source) : '';
  const currentWeightKg = asNumber(stackSummaries?.[targetKey]?.weightKg);
  const weightKg = containerWeightKg(payload);
  const projectedWeightKg = Math.max(0, currentWeightKg + weightKg - (sourceKey === targetKey ? weightKg : 0));
  const maxWeightKg = weightLimitForStack(target, stackSummaries?.[targetKey]);
  const percent = maxWeightKg ? Math.round((projectedWeightKg / maxWeightKg) * 100) : null;
  return { currentWeightKg, projectedWeightKg, maxWeightKg, percent };
}

export function validateDropTarget({ payload, target, slots = [], stackSummaries = {}, hatchCovers = [] }) {
  const source = dragSource(payload);
  const reasons = [];
  const add = (status, code, message) => reasons.push({ status, code, message });
  const projection = projectedStackWeight(target, payload, stackSummaries);

  if (!source) add(DROP_VALIDATION_STATUS.BLOCKED, 'NO_SOURCE', 'A origem não possui contêiner identificável.');
  if (!target) add(DROP_VALIDATION_STATUS.BLOCKED, 'NO_TARGET', 'O slot de destino não foi identificado.');
  if (!target) return { status: DROP_VALIDATION_STATUS.BLOCKED, reasons, ...projection };
  if (target.codigoContainer) add(DROP_VALIDATION_STATUS.BLOCKED, 'OCCUPIED', `Slot ocupado por ${target.codigoContainer}.`);
  if (target.restrito) add(DROP_VALIDATION_STATUS.BLOCKED, 'RESTRICTED', normalizedText(target.motivoRestricao) || 'Slot marcado como restrito.');
  if (source?.sourceSlotId && String(source.sourceSlotId) === String(target.id)) add(DROP_VALIDATION_STATUS.BLOCKED, 'SAME_SLOT', 'Origem e destino são o mesmo slot.');
  if (sourceHasLoadAbove(source, slots)) add(DROP_VALIDATION_STATUS.BLOCKED, 'LOAD_ABOVE', 'O contêiner de origem possui carga acima na mesma pilha.');
  if (source && targetHasGapBelow(target, slots, source)) add(DROP_VALIDATION_STATUS.BLOCKED, 'UNSUPPORTED', 'A movimentação deixaria um vão livre abaixo do destino.');

  const weightKg = containerWeightKg(payload);
  const slotLimit = asNumber(target.maxPesoKg ?? target.limitePesoKg);
  if (slotLimit > 0 && weightKg > slotLimit) add(DROP_VALIDATION_STATUS.BLOCKED, 'SLOT_WEIGHT', `Peso de ${weightKg} kg excede o limite do slot de ${slotLimit} kg.`);
  if (projection.maxWeightKg && projection.projectedWeightKg > projection.maxWeightKg) add(DROP_VALIDATION_STATUS.BLOCKED, 'STACK_WEIGHT', `Peso projetado da pilha em ${projection.percent}% do limite.`);
  else if (projection.percent !== null && projection.percent >= 85) add(DROP_VALIDATION_STATUS.WARNING, 'STACK_WEIGHT_WARNING', `Peso projetado da pilha em ${projection.percent}% do limite.`);

  const sourceIso = normalizedText(source?.isoCode).toUpperCase();
  const allowedIso = allowedIsoCodes(target);
  if (sourceIso && allowedIso.length && !allowedIso.includes(sourceIso)) add(DROP_VALIDATION_STATUS.BLOCKED, 'ISO', `ISO ${sourceIso} incompatível com o slot.`);
  if (source?.reefer && !target.tomadaReefer) add(DROP_VALIDATION_STATUS.BLOCKED, 'REEFER', 'Contêiner reefer exige slot com tomada.');
  if ((source?.perigoso || source?.classeImo) && (target.permitePerigoso === false || target.proibidoImdg === true)) add(DROP_VALIDATION_STATUS.BLOCKED, 'IMDG', 'O slot não permite carga perigosa.');
  if (source?.oog && target.permiteOog === false) add(DROP_VALIDATION_STATUS.BLOCKED, 'OOG', 'O slot não permite carga fora de gabarito.');

  const hatch = hatchRestriction(target, hatchCovers);
  if (hatch) add(hatch.status, hatch.code, hatch.message);
  if (normalizedText(target.statusAlertas).toUpperCase() === 'AVISO') add(DROP_VALIDATION_STATUS.WARNING, 'BACKEND_WARNING', 'O backend sinalizou aviso para o slot.');

  const blocked = reasons.some((reason) => reason.status === DROP_VALIDATION_STATUS.BLOCKED);
  const warning = reasons.some((reason) => reason.status === DROP_VALIDATION_STATUS.WARNING);
  return {
    status: blocked ? DROP_VALIDATION_STATUS.BLOCKED : warning ? DROP_VALIDATION_STATUS.WARNING : DROP_VALIDATION_STATUS.VALID,
    reasons,
    hatchCover: hatchCoverForSlot(target, hatchCovers),
    ...projection
  };
}

export function validationClass(validation) {
  if (!validation) return '';
  return `drop-${String(validation.status).toLowerCase()}`;
}

export function stackStatus(summary, projectedPercent = null) {
  const percent = projectedPercent ?? summary?.percent;
  if (percent === null || percent === undefined) return 'UNLIMITED';
  if (percent > 100) return 'EXCEEDED';
  if (percent >= 85) return 'WARNING';
  return 'OK';
}
