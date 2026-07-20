import { useEffect, useMemo, useRef, useState } from 'react';
import { Operational2DCommandCenter } from '../../components/Operational2DCommandCenter.jsx';
import { publishOperationalSelection, useOperationalSelection } from '../../operational-selection.js';
import {
  createOperationalViewport,
  drillOperationalViewport,
  operationalViewportBreadcrumbs,
  panOperationalViewport,
  resetOperationalViewport,
  returnOperationalViewport,
  selectViewportItems,
  zoomOperationalViewport
} from '../../operational-viewport.js';
import { OperationalSelectionPanel, OperationalWorkspaceManual } from '../OperationalSelectionPanel.jsx';
import { OperationalYardViews as OperationalYardViewsBase } from './OperationalYardViewsBase.jsx';

const TOOL_LABELS = {
  pointer: 'Ponteiro',
  pan: 'Mover',
  zoom: 'Lupa',
  info: 'Informação',
  multi: 'Seleção múltipla',
  area: 'Marcar área'
};

function flattenStacks(blocks) {
  return (Array.isArray(blocks) ? blocks : []).flatMap((block) => Array.isArray(block?.stacks) ? block.stacks : []);
}

function sameStack(left, right) {
  return Boolean(left && right)
    && String(left.bloco) === String(right.bloco)
    && String(left.linha) === String(right.linha)
    && String(left.coluna) === String(right.coluna);
}

function stackKey(stack) {
  return stack ? `${stack.bloco}:${stack.linha}:${stack.coluna}` : '';
}

function findRelatedMovement(stack, movements) {
  const codes = new Set((stack?.layers ?? []).map((layer) => String(layer.codigoConteiner ?? '')).filter(Boolean));
  return (Array.isArray(movements) ? movements : []).find((movement) => codes.has(String(movement.codigoConteiner ?? movement.containerCode ?? ''))) ?? null;
}

function selectionForStack(stack, properties) {
  if (!stack) return null;
  const layers = Array.isArray(stack.layers) ? stack.layers : [];
  const selectedLayer = layers.find((layer) => layer.ocupada) ?? layers.find((layer) => layer.plannedOrder) ?? layers[0] ?? {};
  const movement = findRelatedMovement(stack, properties.movements);
  const unitCode = selectedLayer.codigoConteiner ?? movement?.codigoConteiner ?? '';
  const workInstruction = selectedLayer.plannedOrder?.id
    ?? movement?.workInstructionId
    ?? movement?.ordemId
    ?? movement?.id
    ?? '';
  const equipmentItem = (Array.isArray(properties.telemetry) ? properties.telemetry : []).find((item) => {
    return String(item.bloco ?? '') === String(stack.bloco)
      && (!item.linha || String(item.linha) === String(stack.linha));
  });
  const equipment = equipmentItem?.equipamento ?? equipmentItem?.identificador ?? movement?.equipamento ?? '';
  const rail = movement?.vagaoId ?? movement?.codigoVagao ?? movement?.linhaFerroviaria ?? '';
  const related = [];
  if (equipment) {
    related.push({
      domain: 'equipment',
      id: `equipment:${equipment}`,
      label: equipment,
      unitCode,
      equipment,
      workInstruction,
      location: { bloco: stack.bloco, linha: stack.linha, coluna: stack.coluna },
      metadata: { status: equipmentItem?.statusOperacional ?? equipmentItem?.status },
      source: 'OperationalYardViews'
    });
  }
  if (rail) {
    related.push({
      domain: 'rail',
      id: `rail:${rail}`,
      label: `Ferrovia ${rail}`,
      unitCode,
      rail,
      workInstruction,
      location: { linha: movement?.linhaFerroviaria, slot: movement?.posicaoVagao },
      source: 'OperationalYardViews'
    });
  }
  return {
    domain: 'yard',
    id: `yard:${stackKey(stack)}`,
    label: unitCode || `${stack.bloco} · L${stack.linha}/C${stack.coluna}`,
    unitCode,
    stackKey: stackKey(stack),
    workInstruction,
    equipment,
    rail,
    location: {
      bloco: stack.bloco,
      linha: stack.linha,
      coluna: stack.coluna,
      tier: selectedLayer.camadaOperacional,
      slot: selectedLayer.id
    },
    origin: movement?.origem ?? {
      bloco: movement?.blocoOrigem,
      linha: movement?.linhaOrigem,
      coluna: movement?.colunaOrigem
    },
    destination: movement?.destino ?? {
      bloco: movement?.blocoDestino,
      linha: movement?.linhaDestino,
      coluna: movement?.colunaDestino
    },
    metadata: {
      ocupadas: stack.occupiedLayers,
      capacidade: stack.totalLayers,
      restrita: Boolean(stack.restricted)
    },
    source: 'OperationalYardViews',
    related
  };
}

function modeLabelForLevel(level) {
  if (level === 'overview' || level === 'block') return 'Bloco';
  if (level === 'line') return 'Seção lateral';
  if (level === 'stack') return 'Scan';
  return 'Microvisão';
}

export function OperationalYardViews(properties) {
  const sharedSelection = useOperationalSelection();
  const allStacks = useMemo(() => flattenStacks(properties.blocks), [properties.blocks]);
  const [viewport, setViewport] = useState(() => createOperationalViewport());
  const [tool, setTool] = useState('pointer');
  const [selectionRectangle, setSelectionRectangle] = useState(null);
  const [areaSelection, setAreaSelection] = useState([]);
  const pointerState = useRef(null);
  const stageRef = useRef(null);
  const baseRef = useRef(null);
  const externalStack = useMemo(() => {
    if (sharedSelection.active?.domain !== 'yard') return null;
    return allStacks.find((stack) => stackKey(stack) === sharedSelection.active.stackKey) ?? null;
  }, [allStacks, sharedSelection.active]);
  const effectiveSelectedStack = externalStack ?? properties.selectedStack;

  function selectStack(stack) {
    if (stack) publishOperationalSelection(selectionForStack(stack, properties));
    properties.onSelectStack?.(stack);
  }

  useEffect(() => {
    if (properties.selectedStack) publishOperationalSelection(selectionForStack(properties.selectedStack, properties));
  }, [properties.selectedStack, properties.movements, properties.telemetry]);

  useEffect(() => {
    if (!externalStack || sameStack(externalStack, properties.selectedStack)) return;
    properties.onSelectStack?.(externalStack);
  }, [externalStack]);

  function synchronizeBaseView(level) {
    globalThis.queueMicrotask?.(() => {
      const expected = modeLabelForLevel(level);
      const button = Array.from(baseRef.current?.querySelectorAll('.yard-operational-toolbar button') ?? [])
        .find((item) => item.textContent?.trim() === expected);
      button?.click();
    });
  }

  function drillDown() {
    const selected = effectiveSelectedStack;
    if (!selected) return;
    const layer = selected.layers?.find((item) => item.ocupada) ?? selected.layers?.[0] ?? {};
    setViewport((current) => {
      const next = drillOperationalViewport(current, {
        bloco: selected.bloco,
        linha: selected.linha,
        coluna: selected.coluna,
        tier: layer.camadaOperacional,
        slot: layer.codigoConteiner ?? layer.id
      });
      synchronizeBaseView(next.level);
      return next;
    });
  }

  function returnTo(level) {
    setViewport((current) => {
      const next = returnOperationalViewport(current, level);
      synchronizeBaseView(next.level);
      return next;
    });
  }

  function relativePoint(event) {
    const bounds = stageRef.current?.getBoundingClientRect();
    return { x: event.clientX - (bounds?.left ?? 0), y: event.clientY - (bounds?.top ?? 0) };
  }

  function pointerDown(event) {
    if (tool !== 'pan' && tool !== 'area') return;
    const point = relativePoint(event);
    event.currentTarget.setPointerCapture?.(event.pointerId);
    pointerState.current = { point, viewport };
    if (tool === 'area') setSelectionRectangle({ x1: point.x, y1: point.y, x2: point.x, y2: point.y });
  }

  function pointerMove(event) {
    if (!pointerState.current) return;
    const point = relativePoint(event);
    if (tool === 'pan') {
      const origin = pointerState.current;
      setViewport(panOperationalViewport(origin.viewport, point.x - origin.point.x, point.y - origin.point.y));
    } else if (tool === 'area') {
      setSelectionRectangle((current) => current ? { ...current, x2: point.x, y2: point.y } : null);
    }
  }

  function finishAreaSelection(rectangle) {
    if (!rectangle || !stageRef.current || !baseRef.current) return;
    const stageBounds = stageRef.current.getBoundingClientRect();
    const nodes = Array.from(baseRef.current.querySelectorAll('.yard-operational-stack'));
    const items = nodes.map((node) => {
      const label = allStacks.find((stack) => node.textContent?.includes(`${stack.bloco} · L${stack.linha}/C${stack.coluna}`));
      const bounds = node.getBoundingClientRect();
      return label ? {
        value: label,
        bounds: {
          left: bounds.left - stageBounds.left,
          right: bounds.right - stageBounds.left,
          top: bounds.top - stageBounds.top,
          bottom: bounds.bottom - stageBounds.top
        }
      } : null;
    }).filter(Boolean);
    setAreaSelection(selectViewportItems(items, rectangle).map((item) => item.value));
  }

  function pointerUp() {
    if (tool === 'area') finishAreaSelection(selectionRectangle);
    pointerState.current = null;
    if (tool === 'area') setSelectionRectangle(null);
  }

  function canvasClick(event) {
    if (event.target.closest('input, select, textarea, a')) return;
    if (tool === 'zoom') {
      const point = relativePoint(event);
      setViewport((current) => zoomOperationalViewport(current, 0.2, point));
    }
    if (tool === 'multi') {
      const node = event.target.closest('.yard-operational-stack');
      if (!node) return;
      const stack = allStacks.find((item) => node.textContent?.includes(`${item.bloco} · L${item.linha}/C${item.coluna}`));
      if (!stack) return;
      setAreaSelection((current) => current.some((item) => sameStack(item, stack))
        ? current.filter((item) => !sameStack(item, stack))
        : [...current, stack]);
    }
  }

  function keyDown(event) {
    if (event.key === '+' || event.key === '=') setViewport((current) => zoomOperationalViewport(current, 0.15));
    if (event.key === '-') setViewport((current) => zoomOperationalViewport(current, -0.15));
    if (event.key === '0') setViewport(resetOperationalViewport());
    if (event.key === 'Backspace') {
      event.preventDefault();
      returnTo();
    }
    if (event.key === 'Escape') {
      setSelectionRectangle(null);
      setAreaSelection([]);
    }
  }

  const breadcrumbs = operationalViewportBreadcrumbs(viewport);
  const rectangleStyle = selectionRectangle ? {
    left: Math.min(selectionRectangle.x1, selectionRectangle.x2),
    top: Math.min(selectionRectangle.y1, selectionRectangle.y2),
    width: Math.abs(selectionRectangle.x2 - selectionRectangle.x1),
    height: Math.abs(selectionRectangle.y2 - selectionRectangle.y1)
  } : null;

  return <div className="integrated-operational-workspace yard-integrated-workspace" tabIndex={0} onKeyDown={keyDown}>
    <OperationalWorkspaceManual scope="workspace hierárquico do pátio" />
    <OperationalSelectionPanel title="Composição operacional do pátio" />
    <Operational2DCommandCenter data={properties} scope="Pátio · Navio, Yard, Rail, work queues e equipamentos" onCommand={properties.onOperational2DCommand} />
    <section className="operational-viewport-controller" aria-label="Ferramentas do viewport 2D">
      <div className="operational-viewport-toolbar">
        <div>{Object.entries(TOOL_LABELS).map(([value, label]) => <button type="button" key={value} className={tool === value ? 'active' : 'secondary'} onClick={() => setTool(value)}>{label}</button>)}</div>
        <div><button type="button" className="secondary" onClick={() => setViewport((current) => zoomOperationalViewport(current, -0.15))}>−</button><span>{Math.round(viewport.zoom * 100)}%</span><button type="button" className="secondary" onClick={() => setViewport((current) => zoomOperationalViewport(current, 0.15))}>+</button><button type="button" className="secondary" onClick={() => setViewport(resetOperationalViewport())}>Restaurar</button></div>
      </div>
      <nav className="operational-viewport-breadcrumbs" aria-label="Navegação hierárquica">{breadcrumbs.map((item, index) => <button type="button" key={item.level} disabled={index === breadcrumbs.length - 1} onClick={() => returnTo(item.level)}>{item.label}</button>)}</nav>
      <div
        ref={stageRef}
        className={`operational-viewport-stage tool-${tool}`}
        onPointerDown={pointerDown}
        onPointerMove={pointerMove}
        onPointerUp={pointerUp}
        onPointerCancel={pointerUp}
        onClick={canvasClick}
        onDoubleClick={drillDown}
      >
        <div ref={baseRef} className="operational-viewport-transform" style={{ transform: `translate(${viewport.x}px, ${viewport.y}px) scale(${viewport.zoom})` }}>
          <OperationalYardViewsBase {...properties} selectedStack={effectiveSelectedStack} onSelectStack={selectStack} />
        </div>
        {rectangleStyle && <div className="operational-selection-rectangle" style={rectangleStyle} />}
      </div>
      <footer><span>Nível: <strong>{breadcrumbs.at(-1)?.label}</strong></span><span>Ferramenta: <strong>{TOOL_LABELS[tool]}</strong></span><span>Duplo clique aprofunda e Backspace retorna preservando o contexto.</span></footer>
    </section>
    {areaSelection.length > 0 && <section className="operational-area-selection"><header><strong>Elementos da área marcada</strong><span>{areaSelection.length} pilha(s)</span></header><div>{areaSelection.map((stack) => <button type="button" key={stackKey(stack)} onClick={() => selectStack(stack)}><strong>{stack.bloco} · L{stack.linha}/C{stack.coluna}</strong><small>{stack.occupiedLayers ?? stack.layers?.filter((item) => item.ocupada).length ?? 0}/{stack.totalLayers ?? stack.layers?.length ?? 0} ocupadas</small></button>)}</div></section>}
  </div>;
}
