import { useMemo } from 'react';
import {
  OVERLAY_RISK,
  VESSEL_OVERLAY_MODES,
  buildCraneLanes,
  buildRestowFlows,
  findSlotByPosition,
  overlaySummary
} from '../vessel-planner-phase3.js';
import { formatSlotPosition } from '../vessel-planner-model.js';

function riskLabel(risk) {
  if (risk === OVERLAY_RISK.HIGH) return 'Alto';
  if (risk === OVERLAY_RISK.MEDIUM) return 'Atenção';
  if (risk === OVERLAY_RISK.LOW) return 'Monitorado';
  return 'Sem risco';
}

function slotById(slots, id) {
  return slots.find((slot) => String(slot.id) === String(id)) ?? null;
}

export function VesselOverlayToolbar({ mode, setMode, overlayIndex }) {
  const summary = useMemo(() => overlaySummary(overlayIndex), [overlayIndex]);
  return <section className="vessel-overlay-toolbar" aria-label="Camadas técnicas do Vessel Planner">
    <div className="overlay-toolbar-heading">
      <div><span className="view-kicker">Technical overlays</span><h3>Camadas técnicas</h3></div>
      <div className="overlay-risk-summary" aria-label="Resumo de riscos do overlay">
        <span className="risk-high">Alto <strong>{summary.HIGH}</strong></span>
        <span className="risk-medium">Atenção <strong>{summary.MEDIUM}</strong></span>
        <span className="risk-low">Monitorado <strong>{summary.LOW}</strong></span>
      </div>
    </div>
    <div className="overlay-mode-tabs" role="tablist" aria-label="Selecionar camada técnica">
      {VESSEL_OVERLAY_MODES.map((item) => <button
        key={item.value}
        type="button"
        className={mode === item.value ? 'active' : ''}
        aria-pressed={mode === item.value}
        onClick={() => setMode(item.value)}
      >{item.label}</button>)}
    </div>
    <div className="overlay-legend"><span><i className="risk-low" />Monitorado</span><span><i className="risk-medium" />Atenção</span><span><i className="risk-high" />Alto/bloqueio técnico</span></div>
  </section>;
}

function ImdgSegregationPanel({ slots, imdgIndex, selectedSlotId, onSelectSlot }) {
  const entries = slots
    .map((slot) => ({ slot, overlay: imdgIndex[String(slot.id)] }))
    .filter((item) => item.overlay)
    .sort((left, right) => {
      const riskOrder = { HIGH: 0, MEDIUM: 1, LOW: 2, NONE: 3 };
      return riskOrder[left.overlay.risk] - riskOrder[right.overlay.risk]
        || left.slot.bay - right.slot.bay
        || left.slot.rowBay - right.slot.rowBay
        || left.slot.tier - right.slot.tier;
    });

  return <section className="phase3-panel imdg-panel" aria-label="Segregação IMDG gráfica">
    <header><div><span className="view-kicker">IMDG segregation</span><h3>Segregação IMDG gráfica</h3></div><small>Conflitos do backend em vermelho; proximidade é apenas zona gráfica de atenção.</small></header>
    {entries.length ? <div className="imdg-grid">{entries.map(({ slot, overlay }) => <button
      key={slot.id}
      type="button"
      className={`imdg-card risk-${overlay.risk.toLowerCase()}${String(slot.id) === String(selectedSlotId) ? ' selected' : ''}`}
      onClick={() => onSelectSlot(slot)}
    >
      <span className="imdg-position">{formatSlotPosition(slot)}</span>
      <strong>{slot.codigoContainer}</strong>
      <span>{overlay.label} · ONU {slot.numeroOnu || 'N/I'}</span>
      <small>{overlay.authoritativeConflict ? `${overlay.violations.length || 1} conflito(s) técnico(s)` : overlay.nearbySlotIds.length ? `${overlay.nearbySlotIds.length} carga(s) perigosa(s) próxima(s)` : 'Sem conflito retornado'}</small>
    </button>)}</div> : <div className="visual-empty">Nenhuma carga perigosa alocada no plano.</div>}
  </section>;
}

function CraneSequenceGraphic({ slots, sequencing, selectedSlotId, onSelectSlot }) {
  const lanes = useMemo(() => buildCraneLanes(sequencing), [sequencing]);
  return <section className="phase3-panel crane-sequence-graphic" aria-label="Sequência gráfica dos guindastes">
    <header><div><span className="view-kicker">Crane sequence</span><h3>Sequência gráfica dos guindastes</h3></div><small>Cada faixa representa um guindaste e a ordem calculada pelo backend.</small></header>
    {lanes.length ? <div className="crane-lanes">{lanes.map((lane) => <article key={lane.craneId} className="crane-lane">
      <div className="crane-lane-heading"><strong>Q{lane.craneId}</strong><span>{lane.operations.length} operação(ões)</span>{lane.blocked > 0 && <small>{lane.blocked} bloqueada(s)</small>}</div>
      <ol>{lane.operations.map((operation) => {
        const slot = findSlotByPosition(slots, { bay: operation.bay, rowBay: operation.rowBay, tier: operation.tier });
        return <li key={`${lane.craneId}:${operation.order}:${operation.codigoContainer ?? operation.position}`}>
          <button
            type="button"
            className={`${operation.blocked ? 'blocked' : ''}${slot && String(slot.id) === String(selectedSlotId) ? ' selected' : ''}`}
            disabled={!slot}
            onClick={() => slot && onSelectSlot(slot)}
            title={operation.motivoBloqueioTampa || ''}
          >
            <span>#{operation.order}</span>
            <strong>{operation.codigoContainer || operation.tipoOperacao || 'Operação'}</strong>
            <small>B{operation.bay} · R{operation.rowBay} · T{operation.tier}{operation.blocked ? ' · BLOQUEADA' : ''}</small>
          </button>
        </li>;
      })}</ol>
    </article>)}</div> : <div className="visual-empty">Execute “Sequenciar guindastes” para gerar as faixas gráficas.</div>}
  </section>;
}

function RestowGraphic({ slots, restow, selectedSlotId, onSelectSlot }) {
  const flows = useMemo(() => buildRestowFlows(restow), [restow]);
  return <section className="phase3-panel restow-graphic" aria-label="Mapa gráfico de restow">
    <header><div><span className="view-kicker">Restow flow</span><h3>Restow gráfico</h3></div><small>Origem, destino e motivo de cada reposicionamento necessário.</small></header>
    {flows.length ? <div className="restow-flow-list">{flows.map((flow) => {
      const sourceSlot = findSlotByPosition(slots, flow.source);
      const destinationSlot = findSlotByPosition(slots, flow.destination);
      const selected = [sourceSlot, destinationSlot].some((slot) => slot && String(slot.id) === String(selectedSlotId));
      return <article key={flow.id} className={selected ? 'selected' : ''}>
        <div className="restow-order"><span>Restow</span><strong>#{flow.order}</strong></div>
        <div className="restow-container"><strong>{flow.containerCode}</strong><small>{flow.status}</small></div>
        <div className="restow-route">
          <button type="button" disabled={!sourceSlot} onClick={() => sourceSlot && onSelectSlot(sourceSlot)}>B{flow.source.bay} R{flow.source.rowBay} T{flow.source.tier}</button>
          <span aria-hidden="true">→</span>
          <button type="button" disabled={!destinationSlot} onClick={() => destinationSlot && onSelectSlot(destinationSlot)}>B{flow.destination.bay} R{flow.destination.rowBay} T{flow.destination.tier}</button>
        </div>
        <p>{flow.reason}</p>
      </article>;
    })}</div> : <div className="visual-empty">Nenhum restow calculado para o plano atual.</div>}
  </section>;
}

function SelectedOverlayPanel({ selectedSlot, descriptor }) {
  if (!selectedSlot) return <section className="phase3-panel selected-overlay-panel"><header><div><span className="view-kicker">Overlay inspector</span><h3>Detalhe técnico</h3></div></header><div className="visual-empty">Selecione um slot para consultar a camada técnica ativa.</div></section>;
  return <section className={`phase3-panel selected-overlay-panel risk-${String(descriptor?.risk ?? 'none').toLowerCase()}`}>
    <header><div><span className="view-kicker">Overlay inspector</span><h3>{formatSlotPosition(selectedSlot)}</h3></div><strong>{riskLabel(descriptor?.risk)}</strong></header>
    <div className="selected-overlay-summary"><span>Camada</span><strong>{descriptor?.label || 'Sem overlay'}</strong><small>{selectedSlot.codigoContainer || 'Slot livre'}</small></div>
    {descriptor?.details?.length ? <ul>{descriptor.details.map((detail, index) => <li key={`${detail}-${index}`}>{detail}</li>)}</ul> : <p>Não há detalhe adicional para o slot na camada selecionada.</p>}
  </section>;
}

export function VesselPlannerPhase3Panels({ slots, imdgIndex, overlayIndex, selectedSlotId, sequencing, restow, onSelectSlot }) {
  const selectedSlot = slotById(slots, selectedSlotId);
  const descriptor = selectedSlot ? overlayIndex[String(selectedSlot.id)] : null;
  return <div className="vessel-phase3-panels">
    <SelectedOverlayPanel selectedSlot={selectedSlot} descriptor={descriptor} />
    <ImdgSegregationPanel slots={slots} imdgIndex={imdgIndex} selectedSlotId={selectedSlotId} onSelectSlot={onSelectSlot} />
    <CraneSequenceGraphic slots={slots} sequencing={sequencing} selectedSlotId={selectedSlotId} onSelectSlot={onSelectSlot} />
    <RestowGraphic slots={slots} restow={restow} selectedSlotId={selectedSlotId} onSelectSlot={onSelectSlot} />
  </div>;
}
