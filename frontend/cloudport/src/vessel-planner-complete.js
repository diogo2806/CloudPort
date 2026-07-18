import { VESSEL_VIEW_MODES, slotPositionKey, stackPositionKey } from './vessel-planner-model.js';

export const COMPLETE_VESSEL_VIEW_MODES = Object.freeze([
  ...VESSEL_VIEW_MODES.slice(0, 3),
  { value: 'SCAN', label: 'Scan' },
  ...VESSEL_VIEW_MODES.slice(3)
]);

function asNumber(value, fallback = 0) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

function warningCount(slot, violationIndex = {}) {
  const direct = violationIndex[String(slot?.id)] ?? [];
  const global = violationIndex.__global__ ?? [];
  return direct.length + global.length;
}

export function buildVesselScanBays(slots = [], context = {}) {
  const grouped = new Map();
  (Array.isArray(slots) ? slots : []).filter(Boolean).forEach((slot) => {
    const bay = asNumber(slot.bay);
    const current = grouped.get(bay) ?? {
      bay,
      slots: [],
      occupied: 0,
      restricted: 0,
      warnings: 0,
      restows: 0,
      craneOperations: 0,
      weightKg: 0,
      maxWeightKg: 0,
      hatchCovers: new Set()
    };
    const position = slotPositionKey(slot);
    const stack = context.stackSummaries?.[stackPositionKey(slot)];
    current.slots.push(slot);
    current.occupied += slot.codigoContainer ? 1 : 0;
    current.restricted += slot.restrito ? 1 : 0;
    current.warnings += warningCount(slot, context.violationIndex);
    current.restows += context.restowIndex?.[position] ? 1 : 0;
    current.craneOperations += context.craneIndex?.[position]?.length ?? 0;
    current.weightKg += asNumber(slot.pesoVgmKg ?? slot.pesoKg);
    current.maxWeightKg = Math.max(current.maxWeightKg, asNumber(stack?.maxWeightKg));
    if (slot.codigoHatchCover) current.hatchCovers.add(String(slot.codigoHatchCover));
    grouped.set(bay, current);
  });

  return Array.from(grouped.values())
    .map((item) => ({
      ...item,
      capacity: item.slots.length,
      free: item.slots.length - item.occupied,
      hatchCovers: Array.from(item.hatchCovers).sort(),
      slots: [...item.slots].sort((left, right) => left.rowBay - right.rowBay || right.tier - left.tier)
    }))
    .sort((left, right) => left.bay - right.bay);
}

export function overlayClassName(descriptor) {
  const risk = String(descriptor?.risk ?? 'NONE').toLowerCase();
  return risk === 'none' ? '' : `phase3-overlay overlay-${risk}`;
}

export function overlayTooltip(descriptor) {
  const details = Array.isArray(descriptor?.details) ? descriptor.details.filter(Boolean) : [];
  if (!details.length) return '';
  return `${descriptor?.label ?? 'Overlay'}: ${details.join(' · ')}`;
}

export function resolveControlledValue(controlledValue, localValue) {
  return controlledValue === undefined || controlledValue === null ? localValue : controlledValue;
}
