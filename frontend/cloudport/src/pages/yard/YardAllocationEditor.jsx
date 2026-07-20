import { useMemo, useState } from 'react';
import { formatError, sanitizeText } from '../../api.js';
import { YardOperationalTimeframes } from '../../components/OperationalTimeframes.jsx';
import { EmptyState, Message, StatusBadge } from '../../components.jsx';
import { yardOperationalApi } from './yardOperationalApi.js';

function allocationList(blocks) {
  const unique = new Map();
  (blocks ?? []).forEach((block) => block.stacks.forEach((stack) => stack.layers.forEach((layer) => {
    if (!layer.plannedOrder?.id || unique.has(layer.plannedOrder.id)) return;
    unique.set(layer.plannedOrder.id, {
      order: layer.plannedOrder,
      position: layer,
      block: block.bloco
    });
  })));
  return Array.from(unique.values()).sort((left, right) => Number(left.order.id) - Number(right.order.id));
}

function availableLayers(blocks, selectedOrderId) {
  return (blocks ?? []).flatMap((block) => block.stacks.flatMap((stack) => stack.layers
    .filter((layer) => !layer.ocupada)
    .filter((layer) => !layer.bloqueada && !layer.interditada && layer.areaPermitida)
    .filter((layer) => !layer.plannedOrder || layer.plannedOrder.id === selectedOrderId)
    .map((layer) => ({ ...layer, bloco: block.bloco }))));
}

export function YardAllocationEditor({ blocks, canOperate, onReload }) {
  const allocations = useMemo(() => allocationList(blocks), [blocks]);
  const [selectedId, setSelectedId] = useState('');
  const [target, setTarget] = useState(null);
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const selected = allocations.find((item) => String(item.order.id) === String(selectedId)) ?? null;
  const positions = useMemo(() => availableLayers(blocks, selected?.order.id), [blocks, selected?.order.id]);
  const blocksAvailable = useMemo(() => Array.from(new Set(positions.map((position) => position.bloco))), [positions]);

  async function confirm() {
    if (!selected || !target) {
      setError('Selecione uma work instruction e uma posição de destino.');
      return;
    }
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      await yardOperationalApi.replanejarAllocation(selected.order.id, target, reason);
      setSuccess(`Allocation da WI #${selected.order.id} alterada para ${target.bloco} L${target.linha}/C${target.coluna}/${target.camadaOperacional}.`);
      setTarget(null);
      setReason('');
      await onReload();
    } catch (operationError) {
      setError(formatError(operationError));
    } finally {
      setBusy(false);
    }
  }

  return <>
    <YardOperationalTimeframes blocks={blocks} />
    {!allocations.length ? <EmptyState title="Nenhuma allocation ativa" description="Work instructions pendentes aparecerão aqui para replanejamento gráfico." /> : <div className="yard-allocation-editor">
      <Message type="error">{error}</Message>
      <Message type="success">{success}</Message>
      <div className="yard-allocation-selector">
        <label>Work instruction<select value={selectedId} onChange={(event) => { setSelectedId(event.target.value); setTarget(null); }}>
          <option value="">Selecione</option>
          {allocations.map((item) => <option key={item.order.id} value={item.order.id}>WI #{item.order.id} · {sanitizeText(item.order.codigoConteiner)} · {item.block} L{item.position.linha}/C{item.position.coluna}/{item.position.camadaOperacional}</option>)}
        </select></label>
        {selected && <div className="yard-allocation-current"><StatusBadge value={selected.order.statusOrdem} /><span>Destino atual: {selected.block} L{selected.position.linha}/C{selected.position.coluna}/{selected.position.camadaOperacional}</span></div>}
      </div>

      {selected && <div className="yard-allocation-blocks">{blocksAvailable.map((blockName) => <section key={blockName}>
        <header><strong>{blockName}</strong><span>{positions.filter((position) => position.bloco === blockName).length} posição(ões) elegíveis</span></header>
        <div>{positions.filter((position) => position.bloco === blockName).map((position) => <button
          type="button"
          key={position.id}
          className={target?.id === position.id ? 'selected' : 'secondary'}
          onClick={() => setTarget(position)}
        >L{position.linha} · C{position.coluna} · {position.camadaOperacional}</button>)}</div>
      </section>)}</div>}

      {selected && target && <section className="yard-simulation-panel valid">
        <header><strong>Simulação da nova allocation</strong><span>Nenhuma alteração foi persistida</span></header>
        <p>WI <strong>#{selected.order.id}</strong>: {selected.block} L{selected.position.linha}/C{selected.position.coluna}/{selected.position.camadaOperacional} → {target.bloco} L{target.linha}/C{target.coluna}/{target.camadaOperacional}</p>
        <label>Motivo operacional<textarea value={reason} onChange={(event) => setReason(event.target.value)} maxLength={255} /></label>
        <div><button type="button" disabled={!canOperate || busy} onClick={confirm}>Confirmar allocation</button><button type="button" className="secondary" onClick={() => setTarget(null)}>Cancelar</button></div>
      </section>}
    </div>}
  </>;
}
