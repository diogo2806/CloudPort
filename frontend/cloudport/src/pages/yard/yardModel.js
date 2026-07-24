import { sanitizeText } from '../../api.js';

export const FINAL_ORDER_STATUSES = new Set(['CONCLUIDA', 'CANCELADA']);

function coordinateValue(value) {
  if (value === undefined || value === null || value === '') return '';
  return String(value);
}

function compareCoordinate(left, right) {
  const leftNumber = Number(left);
  const rightNumber = Number(right);
  if (Number.isFinite(leftNumber) && Number.isFinite(rightNumber)) return leftNumber - rightNumber;
  return String(left).localeCompare(String(right), 'pt-BR', { numeric: true });
}

function warningCount(value) {
  const count = Number(value);
  return Number.isFinite(count) && count > 0 ? count : 0;
}

export function positionKey(position) {
  const line = coordinateValue(position?.linha);
  const column = coordinateValue(position?.coluna);
  const layer = coordinateValue(position?.camadaOperacional ?? position?.camadaDestino);
  return line && column && layer ? `${line}:${column}:${layer}` : '';
}

export function orderDestinationKey(order) {
  const line = coordinateValue(order?.linhaDestino);
  const column = coordinateValue(order?.colunaDestino);
  const layer = coordinateValue(order?.camadaDestino);
  return line && column && layer ? `${line}:${column}:${layer}` : '';
}

export function buildStacks(positions, orders) {
  const planned = new Map();
  (orders ?? [])
    .filter((order) => !FINAL_ORDER_STATUSES.has(order.statusOrdem))
    .forEach((order) => {
      const key = orderDestinationKey(order);
      if (key) planned.set(key, order);
    });

  const blocks = new Map();
  (positions ?? []).forEach((position) => {
    const blockName = sanitizeText(position.bloco) || 'SEM_BLOCO';
    if (!blocks.has(blockName)) blocks.set(blockName, new Map());
    const stackKey = `${coordinateValue(position.linha)}:${coordinateValue(position.coluna)}`;
    if (!blocks.get(blockName).has(stackKey)) {
      blocks.get(blockName).set(stackKey, {
        bloco: blockName,
        linha: position.linha,
        coluna: position.coluna,
        avisosEstivagem: 0,
        layers: []
      });
    }
    const stack = blocks.get(blockName).get(stackKey);
    const key = positionKey(position);
    stack.avisosEstivagem += warningCount(position.avisosEstivagem);
    stack.layers.push({
      ...position,
      avisosEstivagem: warningCount(position.avisosEstivagem),
      plannedOrder: key ? planned.get(key) ?? null : null
    });
  });

  return Array.from(blocks, ([bloco, stacks]) => {
    const orderedStacks = Array.from(stacks.values()).map((stack) => ({
      ...stack,
      layers: stack.layers.sort((left, right) => compareCoordinate(left.camadaOperacional, right.camadaOperacional))
    })).sort((left, right) => compareCoordinate(left.linha, right.linha) || compareCoordinate(left.coluna, right.coluna));
    return {
      bloco,
      avisosEstivagem: orderedStacks.reduce((total, stack) => total + stack.avisosEstivagem, 0),
      stacks: orderedStacks
    };
  }).sort((left, right) => left.bloco.localeCompare(right.bloco, 'pt-BR', { numeric: true }));
}

export function stackClass(stack) {
  if (stack.layers.some((layer) => layer.interditada)) return 'interdicted';
  if (stack.layers.some((layer) => layer.bloqueada || !layer.areaPermitida)) return 'blocked';
  if (stack.layers.some((layer) => layer.plannedOrder)) return 'reserved';
  if (stack.layers.every((layer) => layer.ocupada)) return 'full';
  if (stack.layers.some((layer) => layer.ocupada)) return 'occupied';
  return 'available';
}
