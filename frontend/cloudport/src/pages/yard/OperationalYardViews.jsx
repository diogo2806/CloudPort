import { useEffect, useMemo, useState } from 'react';
import { formatError, sanitizeText } from '../../api.js';
import { EmptyState, Message, StatusBadge } from '../../components.jsx';
import { stackClass } from './yardModel.js';
import {
  createMovePreview,
  enrichOperationalStacks,
  normalizeYardWorkspace,
  readYardWorkspaces,
  validateMoveTarget,
  writeYardWorkspaces
} from './yardOperationalModel.js';
import { yardOperationalApi } from './yardOperationalApi.js';

const VIEW_LABELS = {
  block: 'Bloco',
  section: 'Seção lateral',
  scan: 'Scan',
  micro: 'Microvisão'
};

const OVERLAY_LABELS = {
  status: 'Situação',
  occupancy: 'Ocupação',
  dwell: 'Dwell time',
  reefer: 'Reefers'
};

function layerLabel(layer) {
  if (layer.codigoConteiner) return layer.codigoConteiner;
  if (layer.plannedOrder) return `WI #${layer.plannedOrder.id}`;
  return 'Livre';
}

function overlayLevel(stack, overlay) {
  if (overlay === 'occupancy') {
    if (stack.occupancyPercent >= 80) return 'critical';
    if (stack.occupancyPercent >= 50) return 'warning';
    return 'normal';
  }
  if (overlay === 'dwell') {
    if (stack.maxDwellHours >= 168) return 'critical';
    if (stack.maxDwellHours >= 72) return 'warning';
    return 'normal';
  }
  if (overlay === 'reefer') return stack.reeferCount ? 'info' : 'normal';
  return stackClass(stack);
}

function sameStack(left, right) {
  return Boolean(left && right)
    && String(left.bloco) === String(right.bloco)
    && String(left.linha) === String(right.linha)
    && String(left.coluna) === String(right.coluna);
}

function StackButton({ stack, overlay, selected, onSelect, onDragStart, onDrop }) {
  const level = overlayLevel(stack, overlay);
  const metric = overlay === 'occupancy'
    ? `${stack.occupancyPercent.toFixed(0)}%`
    : overlay === 'dwell'
      ? `${stack.maxDwellHours.toFixed(0)}h`
      : overlay === 'reefer'
        ? `${stack.reeferCount} RF`
        : `${stack.occupiedLayers}/${stack.totalLayers}`;
  return <button
    type="button"
    className={`yard-operational-stack ${level} ${selected ? 'selected' : ''}`}
    onClick={() => onSelect(stack)}
    onDragOver={(event) => event.preventDefault()}
    onDrop={(event) => onDrop(event, stack.layers.find((layer) => !layer.ocupada) ?? stack.layers.at(-1))}
    title={stack.note || `Pilha ${stack.linha}/${stack.coluna}`}
  >
    <span>{stack.bloco} · L{stack.linha}/C{stack.coluna}</span>
    <strong>{metric}</strong>
    <div className="yard-operational-layers">
      {stack.layers.map((layer) => <i
        key={layer.id ?? `${layer.linha}-${layer.coluna}-${layer.camadaOperacional}`}
        className={layer.ocupada ? 'occupied' : layer.plannedOrder ? 'reserved' : 'available'}
        draggable={Boolean(layer.ocupada)}
        onDragStart={(event) => onDragStart(event, layer)}
        title={`${layer.camadaOperacional}: ${layerLabel(layer)}`}
      >{layer.camadaOperacional}</i>)}
    </div>
    {stack.restricted && <small>Área restrita</small>}
    {stack.note && <small>{stack.note}</small>}
  </button>;
}

function BlockView(props) {
  return <div className="yard-operational-blocks">{props.blocks.map((block) => <section key={block.bloco}>
    <header><strong>{block.bloco}</strong><span>{block.stacks.length} pilha(s)</span></header>
    <div className="yard-operational-grid">{block.stacks.map((stack) => <StackButton key={`${stack.linha}-${stack.coluna}`} stack={stack} {...props} selected={props.selectedStackKey === `${stack.bloco}:${stack.linha}:${stack.coluna}`} />)}</div>
  </section>)}</div>;
}

function SectionView({ block, line, ...props }) {
  const stacks = block?.stacks.filter((stack) => line === '' || String(stack.linha) === String(line)) ?? [];
  if (!stacks.length) return <EmptyState title="Nenhuma pilha na seção selecionada" />;
  return <div className="yard-section-view">{stacks.map((stack) => <article key={`${stack.linha}-${stack.coluna}`}>
    <strong>C{stack.coluna}</strong>
    <div className="yard-section-layers">{[...stack.layers].reverse().map((layer) => <button
      type="button"
      key={layer.id}
      className={layer.ocupada ? 'occupied' : layer.plannedOrder ? 'reserved' : 'available'}
      draggable={Boolean(layer.ocupada)}
      onDragStart={(event) => props.onDragStart(event, layer)}
      onDragOver={(event) => event.preventDefault()}
      onDrop={(event) => props.onDrop(event, layer)}
      onClick={() => props.onSelect(stack)}
    ><span>{layer.camadaOperacional}</span><strong>{layerLabel(layer)}</strong></button>)}</div>
  </article>)}</div>;
}

function ScanView({ block, ...props }) {
  const lines = useMemo(() => Array.from(new Set((block?.stacks ?? []).map((stack) => stack.linha))).sort((a, b) => Number(a) - Number(b)), [block]);
  if (!block) return <EmptyState title="Selecione um bloco" />;
  return <div className="yard-scan-view">{lines.map((line) => <section key={line}>
    <header>Linha {line}</header>
    <div>{block.stacks.filter((stack) => stack.linha === line).map((stack) => <StackButton key={`${stack.linha}-${stack.coluna}`} stack={stack} {...props} selected={props.selectedStackKey === `${stack.bloco}:${stack.linha}:${stack.coluna}`} />)}</div>
  </section>)}</div>;
}

function MicroView({ stack, onDragStart, onDrop }) {
  if (!stack) return <EmptyState title="Selecione uma pilha para abrir a microvisão" />;
  return <div className="yard-micro-view">
    <header><strong>{stack.bloco} · L{stack.linha}/C{stack.coluna}</strong><span>{stack.occupiedLayers}/{stack.totalLayers} ocupadas</span></header>
    {[...stack.layers].reverse().map((layer) => <button
      type="button"
      key={layer.id}
      className={layer.ocupada ? 'occupied' : layer.plannedOrder ? 'reserved' : 'available'}
      draggable={Boolean(layer.ocupada)}
      onDragStart={(event) => onDragStart(event, layer)}
      onDragOver={(event) => event.preventDefault()}
      onDrop={(event) => onDrop(event, layer)}
    >
      <span>{layer.camadaOperacional}</span>
      <strong>{layerLabel(layer)}</strong>
      <small>{layer.statusConteiner ?? layer.plannedOrder?.statusOrdem ?? 'DISPONÍVEL'}</small>
    </button>)}
  </div>;
}

export function OperationalYardViews({ blocks, movements, telemetry, alerts, filters, selectedStack: controlledSelectedStack, onSelectStack, onApplyFilters, canOperate, onReload }) {
  const operationalBlocks = useMemo(() => enrichOperationalStacks(blocks, movements), [blocks, movements]);
  const blockNames = operationalBlocks.map((block) => block.bloco);
  const [viewMode, setViewMode] = useState('block');
  const [overlay, setOverlay] = useState('status');
  const [selectedBlock, setSelectedBlock] = useState(blockNames[0] ?? '');
  const [selectedLine, setSelectedLine] = useState('');
  const [localSelectedStack, setLocalSelectedStack] = useState(null);
  const [dragSource, setDragSource] = useState(null);
  const [preview, setPreview] = useState(null);
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [workspaceName, setWorkspaceName] = useState('');
  const [workspaces, setWorkspaces] = useState(() => readYardWorkspaces(globalThis.localStorage));
  const [restriction, setRestriction] = useState({ bloqueada: false, interditada: false, areaPermitida: true, notaOperacional: '' });
  const selectedSource = controlledSelectedStack ?? localSelectedStack;
  const selectedStack = operationalBlocks.flatMap((block) => block.stacks).find((stack) => sameStack(stack, selectedSource)) ?? selectedSource;

  useEffect(() => {
    if (!selectedBlock && blockNames.length) setSelectedBlock(blockNames[0]);
    if (selectedBlock && !blockNames.includes(selectedBlock)) setSelectedBlock(blockNames[0] ?? '');
  }, [blockNames.join('|'), selectedBlock]);

  useEffect(() => {
    if (!selectedStack) return;
    setSelectedBlock(selectedStack.bloco);
    setRestriction({
      bloqueada: selectedStack.layers.some((layer) => layer.bloqueada),
      interditada: selectedStack.layers.some((layer) => layer.interditada),
      areaPermitida: selectedStack.layers.every((layer) => layer.areaPermitida),
      notaOperacional: selectedStack.note ?? ''
    });
  }, [selectedStack]);

  const activeBlock = operationalBlocks.find((block) => block.bloco === selectedBlock) ?? operationalBlocks[0] ?? null;
  const selectedStackKey = selectedStack ? `${selectedStack.bloco}:${selectedStack.linha}:${selectedStack.coluna}` : '';
  const lines = Array.from(new Set((activeBlock?.stacks ?? []).map((stack) => stack.linha))).sort((a, b) => Number(a) - Number(b));
  const reefers = operationalBlocks.flatMap((block) => block.stacks).reduce((total, stack) => total + stack.reeferCount, 0);
  const restricted = operationalBlocks.flatMap((block) => block.stacks).filter((stack) => stack.restricted).length;

  function selectStack(stack) {
    setLocalSelectedStack(stack);
    setSelectedBlock(stack.bloco);
    onSelectStack?.(stack);
  }

  function startDrag(event, layer) {
    if (!canOperate || !layer.ocupada) {
      event.preventDefault();
      return;
    }
    setDragSource(layer);
    event.dataTransfer.effectAllowed = 'move';
    event.dataTransfer.setData('text/plain', String(layer.codigoConteiner ?? ''));
  }

  function drop(event, targetLayer) {
    event.preventDefault();
    if (!canOperate) return;
    const validation = validateMoveTarget(dragSource, targetLayer);
    const nextPreview = createMovePreview(dragSource, targetLayer);
    setPreview(nextPreview);
    setError(validation);
    setSuccess('');
  }

  async function confirmMove() {
    if (!preview?.valid || !preview.conteinerId) {
      setError(preview?.error || 'O contêiner não possui identificador operacional para movimentação.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      await yardOperationalApi.movimentarConteiner(preview.conteinerId, preview.destino, reason);
      setSuccess(`Movimentação de ${preview.codigoConteiner} confirmada.`);
      setPreview(null);
      setReason('');
      setDragSource(null);
      await onReload();
    } catch (operationError) {
      setError(formatError(operationError));
    } finally {
      setBusy(false);
    }
  }

  async function saveRestriction() {
    const positionId = selectedStack?.layers?.[0]?.id;
    if (!positionId) return;
    setBusy(true);
    setError('');
    try {
      await yardOperationalApi.atualizarRestricaoPilha(positionId, restriction, reason);
      setSuccess('Restrições e nota da pilha atualizadas.');
      setReason('');
      await onReload();
    } catch (operationError) {
      setError(formatError(operationError));
    } finally {
      setBusy(false);
    }
  }

  function saveWorkspace() {
    const normalized = normalizeYardWorkspace({ name: workspaceName, viewMode, overlay, selectedBlock, selectedLine, filters });
    if (!normalized.name) {
      setError('Informe um nome para o workspace.');
      return;
    }
    const next = writeYardWorkspaces(globalThis.localStorage, [...workspaces.filter((item) => item.name !== normalized.name), normalized]);
    setWorkspaces(next);
    setWorkspaceName('');
    setSuccess('Workspace salvo neste navegador.');
  }

  function loadWorkspace(workspace) {
    setViewMode(workspace.viewMode);
    setOverlay(workspace.overlay);
    setSelectedBlock(workspace.selectedBlock);
    setSelectedLine(workspace.selectedLine);
    onApplyFilters(workspace.filters);
  }

  function deleteWorkspace(name) {
    const next = writeYardWorkspaces(globalThis.localStorage, workspaces.filter((item) => item.name !== name));
    setWorkspaces(next);
  }

  const viewProps = { overlay, selectedStackKey, onSelect: selectStack, onDragStart: startDrag, onDrop: drop };

  return <div className="yard-operational-shell">
    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>
    <div className="yard-operational-metrics">
      <span><strong>{operationalBlocks.length}</strong> blocos</span>
      <span><strong>{restricted}</strong> pilhas restritas</span>
      <span><strong>{reefers}</strong> reefers</span>
      <span><strong>{telemetry?.length ?? 0}</strong> CHEs com telemetria</span>
      <span><strong>{alerts?.length ?? 0}</strong> alertas</span>
    </div>

    <div className="yard-operational-toolbar">
      <div><label>Vista</label>{Object.entries(VIEW_LABELS).map(([value, label]) => <button type="button" key={value} className={viewMode === value ? 'active' : 'secondary'} onClick={() => setViewMode(value)}>{label}</button>)}</div>
      <div><label>Camada visual</label>{Object.entries(OVERLAY_LABELS).map(([value, label]) => <button type="button" key={value} className={overlay === value ? 'active' : 'secondary'} onClick={() => setOverlay(value)}>{label}</button>)}</div>
      <div className="yard-operational-selectors">
        <label>Bloco<select value={selectedBlock} onChange={(event) => setSelectedBlock(event.target.value)}>{blockNames.map((name) => <option key={name}>{name}</option>)}</select></label>
        {viewMode === 'section' && <label>Linha<select value={selectedLine} onChange={(event) => setSelectedLine(event.target.value)}><option value="">Todas</option>{lines.map((line) => <option key={line}>{line}</option>)}</select></label>}
      </div>
    </div>

    <div className="yard-workspace-bar">
      <input value={workspaceName} onChange={(event) => setWorkspaceName(event.target.value)} placeholder="Nome do workspace" maxLength={60} />
      <button type="button" onClick={saveWorkspace}>Salvar vista</button>
      {workspaces.map((workspace) => <span key={workspace.name}><button type="button" className="secondary" onClick={() => loadWorkspace(workspace)}>{workspace.name}</button><button type="button" className="danger" aria-label={`Excluir ${workspace.name}`} onClick={() => deleteWorkspace(workspace.name)}>×</button></span>)}
    </div>

    <div className="yard-live-equipment">{(telemetry ?? []).map((item, index) => <article key={item.equipamento ?? item.identificador ?? index}>
      <strong>{item.equipamento ?? item.identificador ?? 'CHE'}</strong>
      <StatusBadge value={item.statusOperacional ?? item.status} />
      <small>{item.posicaoMaisProxima ?? `${item.bloco ? `${item.bloco} · ` : ''}${item.linha ?? '—'}/${item.coluna ?? '—'}`}</small>
    </article>)}</div>

    {viewMode === 'block' && <BlockView blocks={operationalBlocks} {...viewProps} />}
    {viewMode === 'section' && <SectionView block={activeBlock} line={selectedLine} {...viewProps} />}
    {viewMode === 'scan' && <ScanView block={activeBlock} {...viewProps} />}
    {viewMode === 'micro' && <MicroView stack={selectedStack} onDragStart={startDrag} onDrop={drop} />}

    {preview && <section className={`yard-simulation-panel ${preview.valid ? 'valid' : 'invalid'}`}>
      <header><strong>Simulação da movimentação</strong><span>Nenhuma alteração foi persistida</span></header>
      <p><strong>{preview.codigoConteiner}</strong>: {preview.origem?.bloco} L{preview.origem?.linha}/C{preview.origem?.coluna}/{preview.origem?.camadaOperacional} → {preview.destino?.bloco} L{preview.destino?.linha}/C{preview.destino?.coluna}/{preview.destino?.camadaOperacional}</p>
      {preview.error && <p>{preview.error}</p>}
      <label>Motivo operacional<textarea value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Justifique a movimentação manual" /></label>
      <div><button type="button" disabled={busy || !preview.valid} onClick={confirmMove}>Confirmar plano</button><button type="button" className="secondary" onClick={() => setPreview(null)}>Cancelar simulação</button></div>
    </section>}

    {selectedStack && <section className="yard-restriction-editor">
      <header><strong>Notas e restrições · {selectedStack.bloco} L{selectedStack.linha}/C{selectedStack.coluna}</strong></header>
      <div>
        <label><input type="checkbox" checked={restriction.bloqueada} onChange={(event) => setRestriction((current) => ({ ...current, bloqueada: event.target.checked }))} /> Bloqueada</label>
        <label><input type="checkbox" checked={restriction.interditada} onChange={(event) => setRestriction((current) => ({ ...current, interditada: event.target.checked }))} /> Interditada</label>
        <label><input type="checkbox" checked={restriction.areaPermitida} onChange={(event) => setRestriction((current) => ({ ...current, areaPermitida: event.target.checked }))} /> Aceita alocação</label>
      </div>
      <label>Nota da pilha<textarea value={restriction.notaOperacional} onChange={(event) => setRestriction((current) => ({ ...current, notaOperacional: sanitizeText(event.target.value) }))} maxLength={500} /></label>
      <label>Motivo operacional<textarea value={reason} onChange={(event) => setReason(event.target.value)} maxLength={255} /></label>
      <button type="button" disabled={!canOperate || busy} onClick={saveRestriction}>Salvar restrições</button>
    </section>}
  </div>;
}
