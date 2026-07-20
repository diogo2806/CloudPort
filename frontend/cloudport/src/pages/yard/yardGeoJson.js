import { sanitizeText } from '../../api.js';
import { buildYardMapLayout } from './yardOpenStreetMap.js';
import { stackClass } from './yardModel.js';

export const YARD_GEOMETRY_TYPE_COLORS = Object.freeze({
  BLOCO: '#0f4c81',
  VIA: '#64748b',
  AREA_BLOQUEADA: '#dc2626',
  AREA_INTERDITADA: '#7c3aed',
  EQUIPAMENTO: '#0891b2'
});

function coordinateKey(value) {
  return value === undefined || value === null ? '' : String(value);
}

export function yardStackKey(value) {
  const bloco = sanitizeText(value?.bloco);
  const linha = coordinateKey(value?.linha);
  const coluna = coordinateKey(value?.coluna);
  return bloco && linha && coluna ? `${bloco}:${linha}:${coluna}` : '';
}

function geometryNode(entry) {
  const geoJson = entry?.geoJson;
  return geoJson?.type === 'Feature' ? geoJson.geometry : geoJson;
}

export function geometryPath(entry) {
  const ring = geometryNode(entry)?.coordinates?.[0];
  if (!Array.isArray(ring)) return [];
  const path = ring
    .filter((coordinate) => Array.isArray(coordinate) && coordinate.length >= 2)
    .map(([lng, lat]) => ({ lat: Number(lat), lng: Number(lng) }))
    .filter((point) => Number.isFinite(point.lat) && Number.isFinite(point.lng));
  if (path.length > 1) {
    const first = path[0];
    const last = path[path.length - 1];
    if (first.lat === last.lat && first.lng === last.lng) path.pop();
  }
  return path.length >= 3 ? path : [];
}

function pathCenter(path) {
  const total = path.reduce((current, point) => ({
    lat: current.lat + point.lat,
    lng: current.lng + point.lng
  }), { lat: 0, lng: 0 });
  return { lat: total.lat / path.length, lng: total.lng / path.length };
}

function geometryState(geometry, stack) {
  if (geometry.tipo === 'AREA_INTERDITADA') return 'interdicted';
  if (geometry.tipo === 'AREA_BLOQUEADA') return 'blocked';
  if (stack) return stackClass(stack);
  return 'available';
}

function stackIndex(blocks) {
  return new Map((blocks ?? []).flatMap((block) => (block.stacks ?? []).map((stack) => [yardStackKey(stack), stack])));
}

export function buildPersistedYardMapLayout(blocks, geometries) {
  const stacks = stackIndex(blocks);
  return (geometries ?? []).map((geometry) => {
    const path = geometryPath(geometry);
    if (!path.length) return null;
    const stackKey = geometry.tipo === 'PILHA' ? yardStackKey(geometry) : '';
    const stack = stackKey ? stacks.get(stackKey) ?? null : null;
    const occupiedLayers = (stack?.layers ?? []).filter((layer) => layer.ocupada).length;
    const totalLayers = stack?.layers?.length ?? 0;
    return {
      key: `geometry:${geometry.id ?? geometry.codigo}`,
      geometry,
      stack,
      stackKey,
      state: geometryState(geometry, stack),
      color: geometry.tipo === 'PILHA' ? null : YARD_GEOMETRY_TYPE_COLORS[geometry.tipo],
      center: pathCenter(path),
      path,
      occupiedLayers,
      totalLayers,
      label: stack
        ? `${sanitizeText(stack.bloco)} · L${stack.linha} C${stack.coluna} · ${occupiedLayers}/${totalLayers}`
        : `${sanitizeText(geometry.codigo)} · ${sanitizeText(geometry.tipo)}`
    };
  }).filter(Boolean);
}

export function buildResolvedYardMapLayout(blocks, config, geometries) {
  const persisted = buildPersistedYardMapLayout(blocks, geometries);
  const persistedStacks = new Set(persisted.map((entry) => entry.stackKey).filter(Boolean));
  const generated = buildYardMapLayout(blocks, config)
    .filter((entry) => !persistedStacks.has(yardStackKey(entry.stack)));
  return [...persisted, ...generated];
}
