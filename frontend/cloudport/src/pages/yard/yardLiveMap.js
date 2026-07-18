import { sanitizeText } from '../../api.js';

function finite(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function equipmentId(item, index = 0) {
  return sanitizeText(item?.equipamento ?? item?.identificador ?? item?.codigo ?? item?.id ?? `CHE-${index + 1}`);
}

function stackKey(source) {
  const block = sanitizeText(source?.bloco ?? source?.block);
  const line = source?.linha ?? source?.line;
  const column = source?.coluna ?? source?.column;
  if (line === undefined || line === null || column === undefined || column === null) return '';
  return `${block}:${line}:${column}`;
}

export function mergeYardEquipment(...sources) {
  const merged = new Map();
  sources.flatMap((source) => Array.isArray(source) ? source : []).forEach((item, index) => {
    if (!item) return;
    const id = equipmentId(item, index);
    const current = merged.get(id) ?? {};
    merged.set(id, { ...current, ...item, identificador: id });
  });
  return Array.from(merged.values());
}

export function buildEquipmentMapEntries(equipment = [], layout = []) {
  const byExactStack = new Map(layout.map((entry) => [stackKey(entry.stack), entry]));
  const byPosition = new Map(layout.map((entry) => [`${entry.stack?.linha ?? ''}:${entry.stack?.coluna ?? ''}`, entry]));

  return (Array.isArray(equipment) ? equipment : []).map((item, index) => {
    const latitude = finite(item?.latitude ?? item?.lat);
    const longitude = finite(item?.longitude ?? item?.lng ?? item?.lon);
    const exact = byExactStack.get(stackKey(item));
    const position = byPosition.get(`${item?.linha ?? item?.line ?? ''}:${item?.coluna ?? item?.column ?? ''}`);
    const entry = exact ?? position ?? null;
    const mapPosition = latitude !== null && longitude !== null
      ? { lat: latitude, lng: longitude }
      : entry?.center ?? null;
    if (!mapPosition) return null;
    return {
      id: equipmentId(item, index),
      position: mapPosition,
      status: sanitizeText(item?.statusOperacional ?? item?.status ?? 'SEM STATUS').toUpperCase(),
      type: sanitizeText(item?.tipoEquipamento ?? item?.tipo ?? 'CHE').toUpperCase(),
      operator: sanitizeText(item?.operador ?? item?.usuario ?? ''),
      updatedAt: item?.atualizadoEm ?? item?.ultimaAtualizacao ?? item?.timestamp ?? null,
      nearestStack: entry?.stack ?? null,
      source: item
    };
  }).filter(Boolean);
}

export function yardRestrictionSummary(blocks = []) {
  const stacks = (Array.isArray(blocks) ? blocks : []).flatMap((block) => block?.stacks ?? []);
  return stacks.reduce((summary, stack) => {
    const layers = stack?.layers ?? [];
    const blocked = stack?.restricted || layers.some((layer) => layer?.bloqueada || layer?.areaPermitida === false);
    const interdicted = layers.some((layer) => layer?.interditada);
    if (blocked) summary.blocked += 1;
    if (interdicted) summary.interdicted += 1;
    return summary;
  }, { blocked: 0, interdicted: 0 });
}
