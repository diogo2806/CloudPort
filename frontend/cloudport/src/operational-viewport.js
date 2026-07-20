export const OPERATIONAL_VIEWPORT_LEVELS = Object.freeze([
  'overview',
  'block',
  'line',
  'stack',
  'tier',
  'slot'
]);

export const OPERATIONAL_VIEWPORT_LABELS = Object.freeze({
  overview: 'Visão geral',
  block: 'Bloco',
  line: 'Linha',
  stack: 'Pilha',
  tier: 'Tier',
  slot: 'Slot'
});

const LEVEL_ZOOM = Object.freeze({
  overview: 1,
  block: 1.12,
  line: 1.3,
  stack: 1.52,
  tier: 1.78,
  slot: 2.05
});

export function clampOperationalZoom(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) return 1;
  return Math.min(3, Math.max(0.55, Math.round(number * 100) / 100));
}

export function createOperationalViewport(value = {}) {
  const level = OPERATIONAL_VIEWPORT_LEVELS.includes(value.level) ? value.level : 'overview';
  return {
    level,
    zoom: clampOperationalZoom(value.zoom ?? LEVEL_ZOOM[level]),
    x: Number.isFinite(Number(value.x)) ? Number(value.x) : 0,
    y: Number.isFinite(Number(value.y)) ? Number(value.y) : 0,
    context: value.context && typeof value.context === 'object' ? { ...value.context } : {},
    history: Array.isArray(value.history) ? [...value.history] : []
  };
}

export function zoomOperationalViewport(viewport, delta, anchor = {}) {
  const current = createOperationalViewport(viewport);
  const nextZoom = clampOperationalZoom(current.zoom + Number(delta || 0));
  const anchorX = Number(anchor.x ?? 0);
  const anchorY = Number(anchor.y ?? 0);
  const ratio = nextZoom / current.zoom;
  return {
    ...current,
    zoom: nextZoom,
    x: anchorX - (anchorX - current.x) * ratio,
    y: anchorY - (anchorY - current.y) * ratio
  };
}

export function panOperationalViewport(viewport, deltaX, deltaY) {
  const current = createOperationalViewport(viewport);
  return {
    ...current,
    x: current.x + Number(deltaX || 0),
    y: current.y + Number(deltaY || 0)
  };
}

export function drillOperationalViewport(viewport, context = {}) {
  const current = createOperationalViewport(viewport);
  const index = OPERATIONAL_VIEWPORT_LEVELS.indexOf(current.level);
  const level = OPERATIONAL_VIEWPORT_LEVELS[Math.min(OPERATIONAL_VIEWPORT_LEVELS.length - 1, index + 1)];
  if (level === current.level) return current;
  return {
    level,
    zoom: LEVEL_ZOOM[level],
    x: current.x,
    y: current.y,
    context: { ...current.context, ...context },
    history: [...current.history, {
      level: current.level,
      zoom: current.zoom,
      x: current.x,
      y: current.y,
      context: current.context
    }]
  };
}

export function returnOperationalViewport(viewport, targetLevel) {
  const current = createOperationalViewport(viewport);
  const requested = OPERATIONAL_VIEWPORT_LEVELS.includes(targetLevel) ? targetLevel : null;
  let historyIndex = current.history.length - 1;
  if (requested) {
    historyIndex = current.history.map((item) => item.level).lastIndexOf(requested);
  }
  if (historyIndex >= 0) {
    const restored = current.history[historyIndex];
    return createOperationalViewport({
      ...restored,
      history: current.history.slice(0, historyIndex)
    });
  }
  const level = requested ?? OPERATIONAL_VIEWPORT_LEVELS[Math.max(0, OPERATIONAL_VIEWPORT_LEVELS.indexOf(current.level) - 1)];
  return createOperationalViewport({
    level,
    zoom: LEVEL_ZOOM[level],
    context: current.context,
    history: []
  });
}

export function resetOperationalViewport() {
  return createOperationalViewport();
}

function breadcrumbLabel(level, context) {
  if (level === 'block') return context.bloco ? `Bloco ${context.bloco}` : 'Bloco';
  if (level === 'line') return context.linha !== undefined ? `Linha ${context.linha}` : 'Linha';
  if (level === 'stack') return context.coluna !== undefined ? `Pilha ${context.coluna}` : 'Pilha';
  if (level === 'tier') return context.tier !== undefined ? `Tier ${context.tier}` : 'Tier';
  if (level === 'slot') return context.slot ? `Slot ${context.slot}` : 'Slot';
  return 'Visão geral';
}

export function operationalViewportBreadcrumbs(viewport) {
  const current = createOperationalViewport(viewport);
  const levelIndex = OPERATIONAL_VIEWPORT_LEVELS.indexOf(current.level);
  return OPERATIONAL_VIEWPORT_LEVELS
    .slice(0, levelIndex + 1)
    .map((level) => ({ level, label: breadcrumbLabel(level, current.context) }));
}

export function normalizeSelectionRectangle(rectangle) {
  const left = Math.min(Number(rectangle?.x1 ?? 0), Number(rectangle?.x2 ?? 0));
  const right = Math.max(Number(rectangle?.x1 ?? 0), Number(rectangle?.x2 ?? 0));
  const top = Math.min(Number(rectangle?.y1 ?? 0), Number(rectangle?.y2 ?? 0));
  const bottom = Math.max(Number(rectangle?.y1 ?? 0), Number(rectangle?.y2 ?? 0));
  return { left, right, top, bottom, width: right - left, height: bottom - top };
}

export function selectViewportItems(items, rectangle) {
  const selection = normalizeSelectionRectangle(rectangle);
  if (selection.width < 3 || selection.height < 3) return [];
  return (Array.isArray(items) ? items : []).filter((item) => {
    const bounds = item?.bounds ?? {};
    return Number(bounds.right) >= selection.left
      && Number(bounds.left) <= selection.right
      && Number(bounds.bottom) >= selection.top
      && Number(bounds.top) <= selection.bottom;
  });
}
