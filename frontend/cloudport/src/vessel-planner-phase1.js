import { legendValueForSlot, toneIndex } from './vessel-planner-model.js';

function finite(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function coordinateDistance(slot, coordinates) {
  const bay = finite(coordinates?.bay);
  const rowBay = finite(coordinates?.rowBay ?? coordinates?.row);
  const tier = finite(coordinates?.tier);
  return (bay === null ? 0 : Math.abs(Number(slot?.bay) - bay) * 10000)
    + (rowBay === null ? 0 : Math.abs(Number(slot?.rowBay ?? slot?.row) - rowBay) * 100)
    + (tier === null ? 0 : Math.abs(Number(slot?.tier) - tier));
}

export function findSynchronizedSlot(slots, coordinates = {}, options = {}) {
  const available = (Array.isArray(slots) ? slots : []).filter(Boolean);
  if (!available.length) return null;
  const bay = finite(coordinates.bay);
  const rowBay = finite(coordinates.rowBay ?? coordinates.row);
  const tier = finite(coordinates.tier);
  const exact = available.find((slot) => (bay === null || Number(slot.bay) === bay)
    && (rowBay === null || Number(slot.rowBay ?? slot.row) === rowBay)
    && (tier === null || Number(slot.tier) === tier));
  if (exact) return exact;

  return [...available].sort((left, right) => {
    const leftDistance = coordinateDistance(left, { bay, rowBay, tier });
    const rightDistance = coordinateDistance(right, { bay, rowBay, tier });
    if (leftDistance !== rightDistance) return leftDistance - rightDistance;
    if (Boolean(options.preferOccupied)) {
      const occupiedDifference = Number(Boolean(right.codigoContainer)) - Number(Boolean(left.codigoContainer));
      if (occupiedDifference) return occupiedDifference;
    }
    return Number(left.id ?? 0) - Number(right.id ?? 0);
  })[0];
}

export function dominantLegendForSlots(slots, mode, containerIndex = {}) {
  const counts = new Map();
  (Array.isArray(slots) ? slots : []).forEach((slot) => {
    if (!slot?.codigoContainer) return;
    const value = legendValueForSlot(slot, mode, containerIndex);
    counts.set(value, (counts.get(value) ?? 0) + 1);
  });
  const ordered = Array.from(counts.entries())
    .sort((left, right) => right[1] - left[1] || left[0].localeCompare(right[0], 'pt-BR'));
  if (!ordered.length) return null;
  const [value, count] = ordered[0];
  return { value, count, total: Array.from(counts.values()).reduce((total, item) => total + item, 0), tone: toneIndex(value) };
}

export function selectionCoordinates(slot) {
  if (!slot) return { bay: null, rowBay: null, tier: null };
  return {
    bay: finite(slot.bay),
    rowBay: finite(slot.rowBay ?? slot.row),
    tier: finite(slot.tier)
  };
}
