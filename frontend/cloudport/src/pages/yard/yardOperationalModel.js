import { sanitizeText } from '../../api.js';

export const YARD_VIEW_MODES = ['block', 'section', 'scan', 'micro'];
export const YARD_OVERLAYS = ['status', 'occupancy', 'dwell', 'reefer'];

export function yardStackKey(stack) {
  return `${stack?.bloco ?? 'SEM_BLOCO'}:${stack?.linha ?? ''}:${stack?.coluna ?? ''}`;
}

export function buildMovementAgeByContainer(movements, now = Date.now()) {
  const latest = new Map();
  (movements ?? []).forEach((movement) => {
    const code = sanitizeText(movement.codigoConteiner);
    const timestamp = Date.parse(movement.registradoEm);
    if (!code || !Number.isFinite(timestamp)) return;
    const current = latest.get(code);
    if (current === undefined || timestamp > current) latest.set(code, timestamp);
  });
  return new Map(Array.from(latest, ([code, timestamp]) => [code, Math.max(0, (now - timestamp) / 3600000)]));
}

export function enrichOperationalStacks(blocks, movements, now = Date.now()) {
  const ageByContainer = buildMovementAgeByContainer(movements, now);
  return (blocks ?? []).map((block) => ({
    ...block,
    stacks: block.stacks.map((stack) => {
      const occupiedLayers = stack.layers.filter((layer) => layer.ocupada);
      const maxDwellHours = occupiedLayers.reduce((maximum, layer) =>
        Math.max(maximum, ageByContainer.get(sanitizeText(layer.codigoConteiner)) ?? 0), 0);
      return {
        ...stack,
        occupiedLayers: occupiedLayers.length,
        totalLayers: stack.layers.length,
        occupancyPercent: stack.layers.length ? occupiedLayers.length / stack.layers.length * 100 : 0,
        maxDwellHours,
        reeferCount: occupiedLayers.filter((layer) => String(layer.tipoCarga ?? '').toUpperCase() === 'REFRIGERADO').length,
        restricted: stack.layers.some((layer) => layer.bloqueada || layer.interditada || !layer.areaPermitida),
        note: stack.layers.find((layer) => sanitizeText(layer.notaOperacional))?.notaOperacional ?? ''
      };
    })
  }));
}

export function validateMoveTarget(sourceLayer, targetLayer) {
  if (!sourceLayer?.ocupada || !sourceLayer?.codigoConteiner) return 'Selecione um contêiner ocupado como origem.';
  if (!targetLayer) return 'Selecione uma posição de destino.';
  if (sourceLayer.id === targetLayer.id) return 'A origem e o destino são a mesma posição.';
  if (targetLayer.ocupada) return 'A posição de destino já está ocupada.';
  if (targetLayer.interditada) return 'A posição de destino está interditada.';
  if (targetLayer.bloqueada || !targetLayer.areaPermitida) return 'A posição de destino está bloqueada.';
  if (targetLayer.plannedOrder) return 'A posição de destino está reservada por uma work instruction.';
  return '';
}

export function createMovePreview(sourceLayer, targetLayer) {
  const error = validateMoveTarget(sourceLayer, targetLayer);
  return {
    valid: !error,
    error,
    conteinerId: sourceLayer?.conteinerId ?? sourceLayer?.containerId ?? sourceLayer?.idConteiner ?? null,
    codigoConteiner: sourceLayer?.codigoConteiner ?? '',
    origem: sourceLayer ? {
      id: sourceLayer.id,
      linha: sourceLayer.linha,
      coluna: sourceLayer.coluna,
      camadaOperacional: sourceLayer.camadaOperacional,
      bloco: sourceLayer.bloco
    } : null,
    destino: targetLayer ? {
      id: targetLayer.id,
      linha: targetLayer.linha,
      coluna: targetLayer.coluna,
      camadaOperacional: targetLayer.camadaOperacional,
      bloco: targetLayer.bloco
    } : null
  };
}

export function normalizeYardWorkspace(workspace = {}) {
  return {
    name: sanitizeText(workspace.name).substring(0, 60),
    viewMode: YARD_VIEW_MODES.includes(workspace.viewMode) ? workspace.viewMode : 'block',
    overlay: YARD_OVERLAYS.includes(workspace.overlay) ? workspace.overlay : 'status',
    selectedBlock: sanitizeText(workspace.selectedBlock),
    selectedLine: workspace.selectedLine === '' || workspace.selectedLine === undefined ? '' : String(workspace.selectedLine),
    filters: Object.fromEntries(Object.entries(workspace.filters ?? {}).map(([key, value]) => [key,
      Array.isArray(value) ? value.map(sanitizeText).filter(Boolean) : sanitizeText(value)]))
  };
}

export function readYardWorkspaces(storage, key = 'cloudport-yard-workspaces') {
  try {
    const parsed = JSON.parse(storage?.getItem(key) ?? '[]');
    return Array.isArray(parsed) ? parsed.map(normalizeYardWorkspace).filter((item) => item.name) : [];
  } catch {
    return [];
  }
}

export function writeYardWorkspaces(storage, workspaces, key = 'cloudport-yard-workspaces') {
  const normalized = (workspaces ?? []).map(normalizeYardWorkspace).filter((item) => item.name).slice(0, 20);
  storage?.setItem(key, JSON.stringify(normalized));
  return normalized;
}
