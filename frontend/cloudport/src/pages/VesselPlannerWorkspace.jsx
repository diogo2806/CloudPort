import { useEffect, useMemo, useState } from 'react';
import {
  VESSEL_LEGEND_MODES,
  VESSEL_VIEW_MODES,
  buildContainerIndex,
  buildCraneIndex,
  buildLegend,
  buildRestowIndex,
  buildSlotWarnings,
  buildStackSummaries,
  buildViolationIndex,
  chooseDropSlot,
  formatSlotPosition,
  legendValueForSlot,
  lashingRiskForSlot,
  normalizeSlots,
  slotPositionKey,
  stabilityRiskForSlot,
  stackPositionKey,
  structuralRiskForSlot,
  toneIndex,
  uniqueCoordinates
} from '../vessel-planner-model.js';

const DRAG_TYPE = 'application/x-cloudport-vessel-container';

function displayNumber(value, maximumFractionDigits = 1) {
  const number = Number(value);
  if (!Number.isFinite(number)) return '—';
  return new Intl.NumberFormat('pt-BR', { maximumFractionDigits }).format(number);
}

function displayWeight(value) {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 ? `${displayNumber(number / 1000)} t` : '—';
}

function readDragPayload(event) {
  try {
    const raw = event.dataTransfer.getData(DRAG_TYPE) || event.dataTransfer.getData('text/plain');
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function writeDragPayload(event, payload) {
  const serialized = JSON.stringify(payload);
  event.dataTransfer.effectAllowed = 'move';
  event.dataTransfer.setData(DRAG_TYPE, serialized);
  event.dataTransfer.setData('text/plain', serialized);
}

function overlayClass(slot, overlays, violationIndex, stackSummary) {
  const classes = [];
  if (overlays.stability) classes.push(`risk-stability-${stabilityRiskForSlot(slot, violationIndex).toLowerCase()}`);
  if (overlays.lashing) classes.push(`risk-lashing-${lashingRiskForSlot(slot, stackSummary).toLowerCase()}`);
  if (overlays.structural) classes.push(`risk-structural-${structuralRiskForSlot(slot, stackSummary).toLowerCase()}`);
  if (overlays.imdg && slot?.perigoso) classes.push('imdg-segregation-active');
  return classes.join(' ');
}

function SlotGlyph({
  slot,
  selected,
  compact = false,
  legendMode,
  containerIndex,
  stackSummary,
  warnings,
  restowMarker,
  craneOperations,
  overlays,
  violationIndex,
  canEdit,
  onSelect,
  onMove
}) {
  const occupied = Boolean(slot?.codigoContainer);
  const legendValue = legendValueForSlot(slot, legendMode, containerIndex);
  const tone = occupied ? toneIndex(legendValue) : null;
  const canDrop = canEdit && !occupied && !slot?.restrito;
  const firstCrane = craneOperations?.[0];
  const classes = [
    'vessel-visual-slot',
    compact ? 'compact' : '',
    occupied ? 'occupied' : 'empty',
    slot?.restrito ? 'restricted' : '',
    selected ? 'selected' : '',
    occupied ? `legend-tone-${tone}` : '',
    warnings.length ? 'has-warning' : '',
    restowMarker ? `restow-${String(restowMarker.role).toLowerCase()}` : '',
    overlayClass(slot, overlays, violationIndex, stackSummary)
  ].filter(Boolean).join(' ');

  function drop(event) {
    if (!canDrop) return;
    event.preventDefault();
    event.stopPropagation();
    const payload = readDragPayload(event);
    if (payload) onMove(payload, slot);
  }

  return <button
    type="button"
    className={classes}
    draggable={canEdit && occupied}
    onDragStart={(event) => {
      if (!occupied) return;
      writeDragPayload(event, { kind: 'slot', slot });
    }}
    onDragOver={(event) => {
      if (canDrop) {
        event.preventDefault();
        event.dataTransfer.dropEffect = 'move';
      }
    }}
    onDrop={drop}
    onClick={() => onSelect(slot)}
    aria-label={`${formatSlotPosition(slot)}${occupied ? `, contêiner ${slot.codigoContainer}` : ', livre'}${warnings.length ? `, ${warnings.length} alerta(s)` : ''}`}
    title={warnings.map((warning) => warning.message).join('\n') || `${formatSlotPosition(slot)} · ${legendValue}`}
  >
    <span className="slot-coordinate">R{slot.rowBay} · T{slot.tier}</span>
    <strong>{occupied ? slot.codigoContainer : slot.restrito ? 'Restrito' : 'Livre'}</strong>
    {!compact && <small>{occupied ? `${legendValue} · ${displayWeight(slot.pesoVgmKg ?? slot.pesoKg)}` : slot.tipoSlot || 'NORMAL'}</small>}
    <span className="slot-badges" aria-hidden="true">
      {slot.codigoHatchCover && <i title={`Tampa ${slot.codigoHatchCover}`}>HC</i>}
      {slot.reefer && <i title="Reefer">RF</i>}
      {slot.perigoso && <i title={`IMO ${slot.classeImo || 'N/I'}`}>IMO</i>}
      {slot.oog && <i title="Out of gauge">OOG</i>}
      {restowMarker && <i title={`Restow ${restowMarker.role}`}>R</i>}
      {firstCrane && <i title={`Guindaste ${firstCrane.guindasteId}, ordem ${firstCrane.ordem}`}>Q{firstCrane.guindasteId}</i>}
      {warnings.length > 0 && <i className="warning-badge" title={warnings.map((warning) => warning.message).join('\n')}>!{warnings.length}</i>}
    </span>
    {!compact && stackSummary?.maxWeightKg && <span className={`slot-stack-meter ${String(stackSummary.status).toLowerCase()}`}>
      <span style={{ width: `${Math.min(100, Math.max(2, stackSummary.percent ?? 0))}%` }} />
    </span>}
  </button>;
}

function ProfileView({ slots, activeBay, onSetActiveBay, selectedSlotId, onSelect, onMove, canEdit }) {
  const bays = uniqueCoordinates(slots, 'bay');
  const tiers = uniqueCoordinates(slots, 'tier').sort((left, right) => right - left);
  const groups = useMemo(() => {
    const result = {};
    slots.forEach((slot) => {
      const key = `${slot.bay}:${slot.tier}`;
      if (!result[key]) result[key] = [];
      result[key].push(slot);
    });
    return result;
  }, [slots]);

  return <div className="vessel-view-card profile-view" aria-label="Perfil lateral completo do navio">
    <header><div><span className="view-kicker">Profile view</span><h3>Perfil lateral</h3></div><small>Bays, tiers, tampas e ocupação longitudinal</small></header>
    <div className="profile-scroll">
      <div className="profile-grid" style={{ gridTemplateColumns: `52px repeat(${Math.max(1, bays.length)}, minmax(72px, 1fr))` }}>
        <div className="profile-axis-corner">Tier</div>
        {bays.map((bay) => <button key={`head-${bay}`} type="button" className={bay === activeBay ? 'profile-bay-head active' : 'profile-bay-head'} onClick={() => onSetActiveBay(bay)}>B{bay}</button>)}
        {tiers.map((tier) => <div className="profile-row" key={`tier-${tier}`}>
          <div className="profile-tier-label">T{tier}</div>
          {bays.map((bay) => {
            const bayTierSlots = groups[`${bay}:${tier}`] ?? [];
            const occupied = bayTierSlots.filter((slot) => slot.codigoContainer);
            const representative = occupied[0] ?? bayTierSlots[0];
            const selected = bayTierSlots.some((slot) => String(slot.id) === String(selectedSlotId));
            const restricted = bayTierSlots.length > 0 && bayTierSlots.every((slot) => slot.restrito);
            const hatch = bayTierSlots.find((slot) => slot.codigoHatchCover)?.codigoHatchCover;
            const dropSlot = bayTierSlots
              .filter((slot) => !slot.codigoContainer && !slot.restrito)
              .sort((left, right) => left.rowBay - right.rowBay)[0];
            return <button
              key={`${bay}-${tier}`}
              type="button"
              className={['profile-cell', occupied.length ? 'occupied' : 'empty', restricted ? 'restricted' : '', selected ? 'selected' : '', bay === activeBay ? 'active-bay' : ''].filter(Boolean).join(' ')}
              onClick={() => {
                onSetActiveBay(bay);
                if (representative) onSelect(representative);
              }}
              onDragOver={(event) => { if (canEdit && dropSlot) event.preventDefault(); }}
              onDrop={(event) => {
                if (!canEdit || !dropSlot) return;
                event.preventDefault();
                const payload = readDragPayload(event);
                if (payload) onMove(payload, dropSlot);
              }}
              title={`Bay ${bay}, tier ${tier}: ${occupied.length}/${bayTierSlots.length} ocupado(s)${hatch ? ` · tampa ${hatch}` : ''}`}
            >
              <strong>{occupied.length || '·'}</strong>
              <small>{hatch || `${bayTierSlots.length} slots`}</small>
            </button>;
          })}
        </div>)}
        <div className="profile-hatch-label">Tampas</div>
        {bays.map((bay) => {
          const covers = Array.from(new Set(slots.filter((slot) => slot.bay === bay && slot.codigoHatchCover).map((slot) => slot.codigoHatchCover)));
          return <div key={`hatch-${bay}`} className="profile-hatch-cell">{covers.join(', ') || 'Convés aberto'}</div>;
        })}
      </div>
    </div>
    <div className="vessel-silhouette" aria-hidden="true"><span className="bow" /><span className="hull" /><span className="stern" /></div>
  </div>;
}

function TopView({ slots, context, activeBay, activeRow, onSetActiveBay, onSetActiveRow, selectedSlotId, onSelect, onMove, canEdit }) {
  const bays = uniqueCoordinates(slots, 'bay');
  const rows = uniqueCoordinates(slots, 'rowBay');
  const stackSummaries = context.stackSummaries;
  const stacks = useMemo(() => {
    const result = {};
    slots.forEach((slot) => {
      const key = stackPositionKey(slot);
      if (!result[key]) result[key] = [];
      result[key].push(slot);
    });
    return result;
  }, [slots]);

  return <div className="vessel-view-card top-view" aria-label="Vista superior do navio">
    <header><div><span className="view-kicker">Top view</span><h3>Vista superior</h3></div><small>Stacks por bay e row com peso acumulado</small></header>
    <div className="top-scroll">
      <div className="top-grid" style={{ gridTemplateColumns: `52px repeat(${Math.max(1, bays.length)}, minmax(74px, 1fr))` }}>
        <div className="top-axis-corner">Row</div>
        {bays.map((bay) => <button key={`bay-${bay}`} type="button" className={bay === activeBay ? 'top-bay-label active' : 'top-bay-label'} onClick={() => onSetActiveBay(bay)}>B{bay}</button>)}
        {rows.map((row) => <div className="top-grid-row" key={`row-${row}`}>
          <button type="button" className={row === activeRow ? 'top-row-label active' : 'top-row-label'} onClick={() => onSetActiveRow(row)}>R{row}</button>
          {bays.map((bay) => {
            const stackSlots = stacks[`${bay}:${row}`] ?? [];
            const summary = stackSummaries[`${bay}:${row}`];
            const topSlot = stackSlots.filter((slot) => slot.codigoContainer).sort((left, right) => right.tier - left.tier)[0] ?? stackSlots[0];
            const selected = stackSlots.some((slot) => String(slot.id) === String(selectedSlotId));
            const target = chooseDropSlot(stackSlots, bay, row);
            return <button
              type="button"
              key={`${bay}-${row}`}
              className={['top-stack-cell', summary?.occupied ? 'occupied' : 'empty', summary?.status ? `stack-${String(summary.status).toLowerCase()}` : '', selected ? 'selected' : '', bay === activeBay ? 'active-bay' : '', row === activeRow ? 'active-row' : ''].filter(Boolean).join(' ')}
              onClick={() => {
                onSetActiveBay(bay);
                onSetActiveRow(row);
                if (topSlot) onSelect(topSlot);
              }}
              onDragOver={(event) => { if (canEdit && target) event.preventDefault(); }}
              onDrop={(event) => {
                if (!canEdit || !target) return;
                event.preventDefault();
                const payload = readDragPayload(event);
                if (payload) onMove(payload, target);
              }}
              title={`Bay ${bay}, row ${row}: ${summary?.occupied ?? 0}/${summary?.capacity ?? 0}, ${displayWeight(summary?.weightKg)}${summary?.percent !== null && summary?.percent !== undefined ? ` (${summary.percent}% do limite)` : ''}`}
            >
              <strong>{summary?.occupied ?? 0}/{summary?.capacity ?? 0}</strong>
              <small>{displayWeight(summary?.weightKg)}</small>
              {summary?.maxWeightKg && <span className="stack-progress"><span style={{ width: `${Math.min(100, summary.percent ?? 0)}%` }} /></span>}
            </button>;
          })}
        </div>)}
      </div>
    </div>
  </div>;
}

function ExactSlotGrid({ columns, rows, slotAt, context, selectedSlotId, onSelect, onMove, canEdit, ariaLabel }) {
  return <div className="exact-slot-scroll">
    <div className="exact-slot-grid" style={{ gridTemplateColumns: `64px repeat(${Math.max(1, columns.length)}, minmax(92px, 1fr))` }} aria-label={ariaLabel}>
      <div className="exact-axis-corner">Tier/Row</div>
      {columns.map((column) => <div className="exact-column-label" key={`column-${column}`}>{context.columnLabel(column)}</div>)}
      {rows.map((row) => <div className="exact-grid-row" key={`row-${row}`}>
        <div className="exact-row-label">{context.rowLabel(row)}</div>
        {columns.map((column) => {
          const slot = slotAt(row, column);
          if (!slot) return <div className="vessel-visual-slot absent" key={`${row}-${column}`} aria-hidden="true">—</div>;
          const stackSummary = context.stackSummaries[stackPositionKey(slot)];
          const warnings = buildSlotWarnings(slot, { violations: context.violationIndex }, stackSummary);
          return <SlotGlyph
            key={slot.id}
            slot={slot}
            selected={String(slot.id) === String(selectedSlotId)}
            legendMode={context.legendMode}
            containerIndex={context.containerIndex}
            stackSummary={stackSummary}
            warnings={warnings}
            restowMarker={context.restowIndex[slotPositionKey(slot)]}
            craneOperations={context.craneIndex[slotPositionKey(slot)]}
            overlays={context.overlays}
            violationIndex={context.violationIndex}
            canEdit={canEdit}
            onSelect={onSelect}
            onMove={onMove}
          />;
        })}
      </div>)}
    </div>
  </div>;
}

function SectionView({ slots, context, activeBay, selectedSlotId, onSelect, onMove, canEdit }) {
  const sectionSlots = slots.filter((slot) => slot.bay === activeBay);
  const rows = uniqueCoordinates(sectionSlots, 'rowBay');
  const tiers = uniqueCoordinates(sectionSlots, 'tier').sort((left, right) => right - left);
  const byPosition = Object.fromEntries(sectionSlots.map((slot) => [`${slot.tier}:${slot.rowBay}`, slot]));
  return <div className="vessel-view-card section-view" aria-label={`Seção do bay ${activeBay}`}>
    <header><div><span className="view-kicker">Section view</span><h3>Seção do bay {activeBay ?? '—'}</h3></div><small>Rows, tiers, tampas e restrições por slot</small></header>
    {sectionSlots.length ? <ExactSlotGrid
      columns={rows}
      rows={tiers}
      slotAt={(tier, row) => byPosition[`${tier}:${row}`]}
      context={{
        ...context,
        columnLabel: (row) => `R${row}`,
        rowLabel: (tier) => `T${tier}`
      }}
      selectedSlotId={selectedSlotId}
      onSelect={onSelect}
      onMove={onMove}
      canEdit={canEdit}
      ariaLabel={`Slots da seção do bay ${activeBay}`}
    /> : <div className="visual-empty">Nenhum slot disponível neste bay.</div>}
    <div className="hatch-cover-strip">
      {Array.from(new Set(sectionSlots.map((slot) => slot.codigoHatchCover).filter(Boolean))).map((cover) => <span key={cover}>Tampa {cover}</span>)}
      {!sectionSlots.some((slot) => slot.codigoHatchCover) && <span>Sem tampa de porão cadastrada</span>}
    </div>
  </div>;
}

function TierView({ slots, context, activeTier, selectedSlotId, onSelect, onMove, canEdit }) {
  const tierSlots = slots.filter((slot) => slot.tier === activeTier);
  const bays = uniqueCoordinates(tierSlots, 'bay');
  const rows = uniqueCoordinates(tierSlots, 'rowBay');
  const byPosition = Object.fromEntries(tierSlots.map((slot) => [`${slot.bay}:${slot.rowBay}`, slot]));
  return <div className="vessel-view-card tier-view" aria-label={`Vista do tier ${activeTier}`}>
    <header><div><span className="view-kicker">Tier view</span><h3>Tier {activeTier ?? '—'}</h3></div><small>Distribuição longitudinal e transversal</small></header>
    {tierSlots.length ? <ExactSlotGrid
      columns={rows}
      rows={bays}
      slotAt={(bay, row) => byPosition[`${bay}:${row}`]}
      context={{
        ...context,
        columnLabel: (row) => `R${row}`,
        rowLabel: (bay) => `B${bay}`
      }}
      selectedSlotId={selectedSlotId}
      onSelect={onSelect}
      onMove={onMove}
      canEdit={canEdit}
      ariaLabel={`Slots do tier ${activeTier}`}
    /> : <div className="visual-empty">Nenhum slot disponível neste tier.</div>}
  </div>;
}

function LegendPanel({ legendMode, setLegendMode, legend }) {
  return <aside className="vessel-legend-panel">
    <label><span>Colorir contêineres por</span><select value={legendMode} onChange={(event) => setLegendMode(event.target.value)}>{VESSEL_LEGEND_MODES.map((mode) => <option key={mode.value} value={mode.value}>{mode.label}</option>)}</select></label>
    <div className="vessel-legend-items">
      {legend.map((item) => <span key={item.value}><i className={`legend-tone-${item.tone}`} />{item.value}<small>{item.count}</small></span>)}
      {!legend.length && <span className="legend-empty">Nenhum contêiner alocado</span>}
    </div>
  </aside>;
}

function OverlayControls({ overlays, setOverlays }) {
  const options = [
    ['stability', 'Estabilidade'],
    ['lashing', 'Lashing'],
    ['structural', 'Força estrutural'],
    ['imdg', 'Segregação IMDG'],
    ['restow', 'Restow'],
    ['cranes', 'Guindastes']
  ];
  return <div className="overlay-controls" aria-label="Sobreposições operacionais">
    {options.map(([key, label]) => <label key={key}><input type="checkbox" checked={Boolean(overlays[key])} onChange={(event) => setOverlays((current) => ({ ...current, [key]: event.target.checked }))} /><span>{label}</span></label>)}
  </div>;
}

function SelectedSlotInspector({ slot, context, onSelect }) {
  if (!slot) return <aside className="vessel-slot-inspector empty"><span className="view-kicker">Inspector</span><h3>Nenhum slot selecionado</h3><p>Clique em qualquer vista para sincronizar bay, row, tier e detalhes.</p></aside>;
  const stackSummary = context.stackSummaries[stackPositionKey(slot)];
  const warnings = buildSlotWarnings(slot, { violations: context.violationIndex }, stackSummary);
  const restow = context.restowIndex[slotPositionKey(slot)];
  const operations = context.craneIndex[slotPositionKey(slot)] ?? [];
  const metadata = context.containerIndex[String(slot.codigoContainer ?? '').toUpperCase()] ?? {};
  return <aside className="vessel-slot-inspector">
    <div className="inspector-heading"><div><span className="view-kicker">Inspector sincronizado</span><h3>{formatSlotPosition(slot)}</h3></div><button type="button" className="icon-button" onClick={() => onSelect(null)} aria-label="Fechar inspector">×</button></div>
    <div className="inspector-status-row"><span className={slot.codigoContainer ? 'status-pill occupied' : 'status-pill empty'}>{slot.codigoContainer ? 'Ocupado' : 'Livre'}</span>{slot.restrito && <span className="status-pill danger">Restrito</span>}{slot.statusAlertas && <span className="status-pill warning">{slot.statusAlertas}</span>}</div>
    <dl className="slot-inspector-grid">
      <div><dt>Contêiner</dt><dd>{slot.codigoContainer || '—'}</dd></div>
      <div><dt>ISO</dt><dd>{slot.isoCode || '—'}</dd></div>
      <div><dt>POD</dt><dd>{slot.portoDescarga || metadata.portoDescarga || '—'}</dd></div>
      <div><dt>Operador</dt><dd>{legendValueForSlot(slot, 'OPERATOR', context.containerIndex)}</dd></div>
      <div><dt>Peso</dt><dd>{displayWeight(slot.pesoVgmKg ?? slot.pesoKg)}</dd></div>
      <div><dt>Peso da pilha</dt><dd>{displayWeight(stackSummary?.weightKg)}{stackSummary?.percent !== null && stackSummary?.percent !== undefined ? ` · ${stackSummary.percent}%` : ''}</dd></div>
      <div><dt>IMO/ONU</dt><dd>{slot.perigoso ? `${slot.classeImo || 'N/I'} / ${slot.numeroOnu || 'N/I'}` : 'Não perigoso'}</dd></div>
      <div><dt>Reefer</dt><dd>{slot.reefer ? `${displayNumber(slot.temperaturaRequeridaC)} °C` : 'Não'}</dd></div>
      <div><dt>Tampa</dt><dd>{slot.codigoHatchCover || '—'}</dd></div>
      <div><dt>Limite do slot</dt><dd>{displayWeight(slot.maxPesoKg)}</dd></div>
    </dl>
    {warnings.length > 0 && <div className="inspector-alerts"><h4>Restrições e erros</h4>{warnings.map((warning, index) => <article key={`${warning.type}-${index}`} className={String(warning.severity).toLowerCase()}><strong>{warning.type}</strong><p>{warning.message}</p></article>)}</div>}
    {restow && <div className="inspector-operation"><strong>Restow {restow.role.toLowerCase()}</strong><span>{restow.codigoContainer} · {restow.motivoRestow}</span></div>}
    {operations.length > 0 && <div className="inspector-operation"><strong>Sequência de guindaste</strong>{operations.map((operation) => <span key={`${operation.guindasteId}-${operation.ordem}`}>Q{operation.guindasteId} · ordem {operation.ordem} · {operation.tipoOperacao}</span>)}</div>}
  </aside>;
}

function StabilityOverlay({ stability, slots, stackSummaries }) {
  const highStacks = Object.values(stackSummaries).filter((summary) => summary.status !== 'OK').length;
  const dangerous = slots.filter((slot) => slot.perigoso).length;
  return <section className="vessel-analysis-overlay">
    <header><div><span className="view-kicker">Overlays técnicos</span><h3>Estabilidade, lashing e estrutura</h3></div><span className={stability?.aprovado ? 'analysis-result approved' : 'analysis-result pending'}>{stability ? stability.aprovado ? 'Aprovado' : 'Com restrições' : 'Não calculado'}</span></header>
    <div className="analysis-metric-grid">
      <article><span>Trim</span><strong>{displayNumber(stability?.trimMetros)} m</strong></article>
      <article><span>Banda</span><strong>{displayNumber(stability?.listGraus)}°</strong></article>
      <article><span>GM</span><strong>{displayNumber(stability?.gmMetros)} m</strong></article>
      <article><span>Calado médio</span><strong>{displayNumber(stability?.caladoMedioMetros)} m</strong></article>
      <article><span>Shear force</span><strong>{displayNumber(stability?.sfMaxKn)} kN</strong></article>
      <article><span>Bending moment</span><strong>{displayNumber(stability?.bmMaxKnm)} kNm</strong></article>
      <article><span>Pilhas críticas</span><strong>{highStacks}</strong></article>
      <article><span>Cargas IMDG</span><strong>{dangerous}</strong></article>
    </div>
    <p className="analysis-disclaimer">Estabilidade e força estrutural usam os resultados persistidos do backend. O destaque de lashing é um indicador visual de risco por tier, OOG e utilização do limite da pilha; não substitui o cálculo certificado de amarração.</p>
  </section>;
}

function CraneTimeline({ sequencing, slots, onSelect }) {
  const operations = Array.isArray(sequencing?.sequencia) ? sequencing.sequencia : [];
  const cranes = Array.from(new Set(operations.map((operation) => operation.guindasteId))).sort((left, right) => left - right);
  const slotByPosition = Object.fromEntries(slots.map((slot) => [slotPositionKey(slot), slot]));
  return <section className="crane-timeline">
    <header><div><span className="view-kicker">Crane sequence</span><h3>Sequência visual dos guindastes</h3></div><small>{operations.length} operação(ões)</small></header>
    {cranes.length ? <div className="crane-lanes">{cranes.map((crane) => <div className="crane-lane" key={crane}><strong>Q{crane}</strong><div>{operations.filter((operation) => operation.guindasteId === crane).sort((left, right) => left.ordem - right.ordem).map((operation) => {
      const slot = slotByPosition[`${operation.bay}:${operation.rowBay}:${operation.tier}`];
      return <button type="button" key={`${crane}-${operation.ordem}-${operation.codigoContainer}`} onClick={() => slot && onSelect(slot)}><span>#{operation.ordem}</span><strong>{operation.codigoContainer}</strong><small>B{operation.bay} R{operation.rowBay} T{operation.tier} · {operation.tipoOperacao}</small></button>;
    })}</div></div>)}</div> : <div className="visual-empty">Execute o sequenciamento para exibir as lanes dos guindastes.</div>}
  </section>;
}

function RestowVisualization({ restow, slots, onSelect }) {
  const movements = Array.isArray(restow?.movimentos) ? restow.movimentos : [];
  const slotByPosition = Object.fromEntries(slots.map((slot) => [slotPositionKey(slot), slot]));
  return <section className="restow-visualization">
    <header><div><span className="view-kicker">Restow map</span><h3>Movimentos de restow</h3></div><small>{movements.length} movimento(s)</small></header>
    {movements.length ? <div className="restow-flow-list">{movements.map((movement, index) => {
      const source = slotByPosition[`${movement.bayAtual}:${movement.rowAtual}:${movement.tierAtual}`];
      const target = slotByPosition[`${movement.bayDestino}:${movement.rowDestino}:${movement.tierDestino}`];
      return <article key={`${movement.codigoContainer}-${index}`}><button type="button" onClick={() => source && onSelect(source)}><span>Origem</span><strong>B{movement.bayAtual} R{movement.rowAtual} T{movement.tierAtual}</strong></button><div className="restow-arrow"><strong>{movement.codigoContainer}</strong><span>→</span><small>{movement.motivoRestow}</small></div><button type="button" onClick={() => target && onSelect(target)}><span>Destino</span><strong>B{movement.bayDestino} R{movement.rowDestino} T{movement.tierDestino}</strong></button></article>;
    })}</div> : <div className="visual-empty">Nenhum restow calculado para o plano atual.</div>}
  </section>;
}

function UnallocatedTray({ containers, canEdit }) {
  return <section className="unallocated-tray">
    <header><div><span className="view-kicker">Load list</span><h3>Contêineres não alocados</h3></div><small>Arraste para um slot vazio</small></header>
    <div>{containers.map((container) => <article
      key={container.codigoContainer}
      draggable={canEdit}
      onDragStart={(event) => writeDragPayload(event, { kind: 'container', container })}
      className={canEdit ? 'draggable' : ''}
    ><strong>{container.codigoContainer}</strong><span>{container.isoCode || 'ISO N/I'} · {displayWeight(container.pesoVgmKg ?? container.pesoKg)}</span><small>{container.portoDescarga || 'POD N/I'}{container.classeImo ? ` · IMO ${container.classeImo}` : ''}{container.reefer ? ' · Reefer' : ''}</small></article>)}{!containers.length && <div className="visual-empty">Todos os contêineres estão alocados.</div>}</div>
  </section>;
}

export function VesselPlannerWorkspace({
  plan,
  bayPlan,
  stability,
  restow,
  sequencing,
  selectedSlotId,
  onSelectSlot,
  onMoveContainer,
  canEdit,
  busy
}) {
  const slots = useMemo(() => normalizeSlots(plan), [plan]);
  const bays = useMemo(() => uniqueCoordinates(slots, 'bay'), [slots]);
  const rows = useMemo(() => uniqueCoordinates(slots, 'rowBay'), [slots]);
  const tiers = useMemo(() => uniqueCoordinates(slots, 'tier'), [slots]);
  const containerIndex = useMemo(() => buildContainerIndex(bayPlan?.containers), [bayPlan]);
  const stackSummaries = useMemo(() => buildStackSummaries(slots), [slots]);
  const violationIndex = useMemo(() => buildViolationIndex(stability), [stability]);
  const restowIndex = useMemo(() => buildRestowIndex(restow), [restow]);
  const craneIndex = useMemo(() => buildCraneIndex(sequencing), [sequencing]);
  const [viewMode, setViewMode] = useState('MULTI');
  const [legendMode, setLegendMode] = useState('POD');
  const [activeBay, setActiveBay] = useState(bays[0] ?? null);
  const [activeRow, setActiveRow] = useState(rows[0] ?? null);
  const [activeTier, setActiveTier] = useState(tiers[0] ?? null);
  const [overlays, setOverlays] = useState({ stability: true, lashing: false, structural: false, imdg: true, restow: true, cranes: true });

  const selectedSlot = useMemo(() => slots.find((slot) => String(slot.id) === String(selectedSlotId)) ?? null, [selectedSlotId, slots]);
  const allocatedCodes = useMemo(() => new Set(slots.map((slot) => slot.codigoContainer).filter(Boolean)), [slots]);
  const unallocated = useMemo(() => (Array.isArray(bayPlan?.containers) ? bayPlan.containers : []).filter((container) => !allocatedCodes.has(container.codigoContainer)), [allocatedCodes, bayPlan]);
  const legend = useMemo(() => buildLegend(slots, legendMode, containerIndex), [containerIndex, legendMode, slots]);

  useEffect(() => {
    if (!bays.includes(activeBay)) setActiveBay(bays[0] ?? null);
  }, [activeBay, bays]);

  useEffect(() => {
    if (!rows.includes(activeRow)) setActiveRow(rows[0] ?? null);
  }, [activeRow, rows]);

  useEffect(() => {
    if (!tiers.includes(activeTier)) setActiveTier(tiers[0] ?? null);
  }, [activeTier, tiers]);

  useEffect(() => {
    if (!selectedSlot) return;
    setActiveBay(selectedSlot.bay);
    setActiveRow(selectedSlot.rowBay);
    setActiveTier(selectedSlot.tier);
  }, [selectedSlot]);

  const visualContext = {
    legendMode,
    containerIndex,
    stackSummaries,
    violationIndex,
    restowIndex,
    craneIndex,
    overlays
  };

  async function move(payload, targetSlot) {
    if (!canEdit || busy || !targetSlot || targetSlot.codigoContainer || targetSlot.restrito) return;
    await onMoveContainer(payload, targetSlot);
  }

  function renderView(mode) {
    if (mode === 'PROFILE') return <ProfileView slots={slots} activeBay={activeBay} onSetActiveBay={setActiveBay} selectedSlotId={selectedSlotId} onSelect={onSelectSlot} onMove={move} canEdit={canEdit} />;
    if (mode === 'TOP') return <TopView slots={slots} context={visualContext} activeBay={activeBay} activeRow={activeRow} onSetActiveBay={setActiveBay} onSetActiveRow={setActiveRow} selectedSlotId={selectedSlotId} onSelect={onSelectSlot} onMove={move} canEdit={canEdit} />;
    if (mode === 'SECTION') return <SectionView slots={slots} context={visualContext} activeBay={activeBay} selectedSlotId={selectedSlotId} onSelect={onSelectSlot} onMove={move} canEdit={canEdit} />;
    return <TierView slots={slots} context={visualContext} activeTier={activeTier} selectedSlotId={selectedSlotId} onSelect={onSelectSlot} onMove={move} canEdit={canEdit} />;
  }

  if (!slots.length) return <div className="visual-empty large">O plano não possui geometria de slots.</div>;

  return <div className="vessel-planner-workspace">
    <div className="vessel-workspace-toolbar">
      <div className="view-mode-tabs" role="tablist" aria-label="Vistas do Vessel Planner">{VESSEL_VIEW_MODES.map((mode) => <button key={mode.value} type="button" className={viewMode === mode.value ? 'active' : ''} onClick={() => setViewMode(mode.value)}>{mode.label}</button>)}</div>
      <div className="coordinate-controls">
        <label>Bay<select value={activeBay ?? ''} onChange={(event) => setActiveBay(Number(event.target.value))}>{bays.map((bay) => <option key={bay} value={bay}>{bay}</option>)}</select></label>
        <label>Row<select value={activeRow ?? ''} onChange={(event) => setActiveRow(Number(event.target.value))}>{rows.map((row) => <option key={row} value={row}>{row}</option>)}</select></label>
        <label>Tier<select value={activeTier ?? ''} onChange={(event) => setActiveTier(Number(event.target.value))}>{tiers.map((tier) => <option key={tier} value={tier}>{tier}</option>)}</select></label>
      </div>
    </div>

    <LegendPanel legendMode={legendMode} setLegendMode={setLegendMode} legend={legend} />
    <OverlayControls overlays={overlays} setOverlays={setOverlays} />

    <div className="vessel-workspace-layout">
      <main className="vessel-main-canvas">
        {viewMode === 'MULTI' ? <div className="multi-view-grid">
          <div>{renderView('PROFILE')}</div>
          <div>{renderView('TOP')}</div>
          <div>{renderView('SECTION')}</div>
          <div>{renderView('TIER')}</div>
        </div> : renderView(viewMode)}
      </main>
      <SelectedSlotInspector slot={selectedSlot} context={visualContext} onSelect={onSelectSlot} />
    </div>

    <UnallocatedTray containers={unallocated} canEdit={canEdit && !busy} />
    <StabilityOverlay stability={stability} slots={slots} stackSummaries={stackSummaries} />
    {overlays.restow && <RestowVisualization restow={restow} slots={slots} onSelect={onSelectSlot} />}
    {overlays.cranes && <CraneTimeline sequencing={sequencing} slots={slots} onSelect={onSelectSlot} />}
  </div>;
}
