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
  normalizeSlots,
  slotPositionKey,
  stackPositionKey,
  toneIndex,
  uniqueCoordinates
} from '../vessel-planner-model.js';
import { dominantLegendForSlots, findSynchronizedSlot, selectionCoordinates } from '../vessel-planner-phase1.js';
import '../vessel-planner-phase1.css';
import { CraneExecutionTimeline } from './CraneExecutionTimeline.jsx';
import { HatchCoverOperationPanel } from './HatchCoverOperationPanel.jsx';

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

function SlotGlyph({ slot, selected, context, canEdit, onSelect, onMove }) {
  const occupied = Boolean(slot?.codigoContainer);
  const legendValue = legendValueForSlot(slot, context.legendMode, context.containerIndex);
  const warnings = buildSlotWarnings(slot, { violations: context.violationIndex }, context.stackSummaries[stackPositionKey(slot)]);
  const restow = context.restowIndex[slotPositionKey(slot)];
  const crane = context.craneIndex[slotPositionKey(slot)]?.[0];
  const canDrop = canEdit && !occupied && !slot.restrito;
  const classes = [
    'vessel-visual-slot', occupied ? 'occupied' : 'empty', slot.restrito ? 'restricted' : '', selected ? 'selected' : '',
    occupied ? `legend-tone-${toneIndex(legendValue)}` : '', warnings.length ? 'has-warning' : ''
  ].filter(Boolean).join(' ');

  return <button
    type="button"
    className={classes}
    draggable={canEdit && occupied}
    onDragStart={(event) => occupied && writeDragPayload(event, { kind: 'slot', slot })}
    onDragOver={(event) => { if (canDrop) event.preventDefault(); }}
    onDrop={(event) => {
      if (!canDrop) return;
      event.preventDefault();
      const payload = readDragPayload(event);
      if (payload) onMove(payload, slot);
    }}
    onClick={() => onSelect(slot)}
    aria-pressed={selected}
    aria-label={`${formatSlotPosition(slot)}, ${occupied ? `contêiner ${slot.codigoContainer}` : 'livre'}, ${legendValue}`}
    title={warnings.map((warning) => warning.message).join('\n') || `${formatSlotPosition(slot)} · ${legendValue}`}
  >
    <span className="slot-coordinate">B{slot.bay} · R{slot.rowBay} · T{slot.tier}</span>
    <strong>{occupied ? slot.codigoContainer : slot.restrito ? 'Restrito' : 'Livre'}</strong>
    <small>{occupied ? `${legendValue} · ${displayWeight(slot.pesoVgmKg ?? slot.pesoKg)}` : slot.tipoSlot || 'NORMAL'}</small>
    <span className="slot-badges" aria-hidden="true">
      {slot.reefer && <i>RF</i>}{slot.perigoso && <i>IMO</i>}{restow && <i>R</i>}{crane && <i>Q{crane.guindasteId}</i>}{slot.codigoHatchCover && <i>HC</i>}{warnings.length > 0 && <i className="warning-badge">!{warnings.length}</i>}
    </span>
  </button>;
}

function AggregateCell({ slots, selectedSlotId, active, context, title, children, canEdit, onSelect, onMove, target }) {
  const occupied = slots.filter((slot) => slot.codigoContainer);
  const dominant = dominantLegendForSlots(occupied, context.legendMode, context.containerIndex);
  const representative = findSynchronizedSlot(slots, context.coordinates, { preferOccupied: true });
  const selected = slots.some((slot) => String(slot.id) === String(selectedSlotId));
  const restricted = slots.length > 0 && slots.every((slot) => slot.restrito);
  return <button
    type="button"
    className={[
      'vessel-phase1-aggregate', occupied.length ? 'occupied' : 'empty', restricted ? 'restricted' : '', selected ? 'selected' : '', active ? 'active-coordinate' : '',
      dominant ? `legend-tone-${dominant.tone}` : ''
    ].filter(Boolean).join(' ')}
    onClick={() => representative && onSelect(representative)}
    onDragOver={(event) => { if (canEdit && target) event.preventDefault(); }}
    onDrop={(event) => {
      if (!canEdit || !target) return;
      event.preventDefault();
      const payload = readDragPayload(event);
      if (payload) onMove(payload, target);
    }}
    aria-pressed={selected}
    title={`${title}${dominant ? ` · ${dominant.value}` : ''}`}
  >
    {children}
    {dominant && <span className="aggregate-legend"><i className={`legend-tone-${dominant.tone}`} />{dominant.value}</span>}
  </button>;
}

function ProfileView({ slots, context, selectedSlotId, canEdit, onSelect, onMove, onCoordinates }) {
  const bays = uniqueCoordinates(slots, 'bay');
  const tiers = uniqueCoordinates(slots, 'tier').sort((left, right) => right - left);
  const groups = useMemo(() => slots.reduce((index, slot) => {
    const key = `${slot.bay}:${slot.tier}`;
    if (!index[key]) index[key] = [];
    index[key].push(slot);
    return index;
  }, {}), [slots]);

  return <section className="vessel-view-card profile-view" aria-label="Perfil lateral do navio">
    <header><div><span className="view-kicker">Profile view</span><h3>Perfil lateral</h3></div><small>Bays e tiers sincronizados</small></header>
    <div className="profile-scroll"><div className="profile-grid" style={{ gridTemplateColumns: `52px repeat(${Math.max(1, bays.length)}, minmax(86px, 1fr))` }}>
      <div className="profile-axis-corner">Tier</div>
      {bays.map((bay) => <button key={bay} type="button" className={bay === context.coordinates.bay ? 'profile-bay-head active' : 'profile-bay-head'} onClick={() => onCoordinates({ bay })}>B{bay}</button>)}
      {tiers.map((tier) => <div className="profile-row" key={tier}>
        <div className="profile-tier-label">T{tier}</div>
        {bays.map((bay) => {
          const group = groups[`${bay}:${tier}`] ?? [];
          const target = group.filter((slot) => !slot.codigoContainer && !slot.restrito).sort((a, b) => a.rowBay - b.rowBay)[0];
          return <AggregateCell key={`${bay}:${tier}`} slots={group} selectedSlotId={selectedSlotId} active={bay === context.coordinates.bay && tier === context.coordinates.tier} context={{ ...context, coordinates: { ...context.coordinates, bay, tier } }} title={`Bay ${bay}, tier ${tier}`} canEdit={canEdit} onSelect={onSelect} onMove={onMove} target={target}>
            <strong>{group.filter((slot) => slot.codigoContainer).length}/{group.length}</strong><small>{group.find((slot) => slot.codigoHatchCover)?.codigoHatchCover || 'slots'}</small>
          </AggregateCell>;
        })}
      </div>)}
      <div className="profile-hatch-label">Tampas</div>
      {bays.map((bay) => <div key={`hatch-${bay}`} className="profile-hatch-cell">{Array.from(new Set(slots.filter((slot) => slot.bay === bay).map((slot) => slot.codigoHatchCover).filter(Boolean))).join(', ') || 'Convés aberto'}</div>)}
    </div></div>
    <div className="vessel-silhouette" aria-hidden="true"><span className="bow" /><span className="hull" /><span className="stern" /></div>
  </section>;
}

function TopView({ slots, context, selectedSlotId, canEdit, onSelect, onMove, onCoordinates }) {
  const bays = uniqueCoordinates(slots, 'bay');
  const rows = uniqueCoordinates(slots, 'rowBay');
  const stacks = useMemo(() => slots.reduce((index, slot) => {
    const key = stackPositionKey(slot);
    if (!index[key]) index[key] = [];
    index[key].push(slot);
    return index;
  }, {}), [slots]);

  return <section className="vessel-view-card top-view" aria-label="Vista superior do navio">
    <header><div><span className="view-kicker">Top view</span><h3>Vista superior</h3></div><small>Bays e rows sincronizados</small></header>
    <div className="top-scroll"><div className="top-grid" style={{ gridTemplateColumns: `52px repeat(${Math.max(1, bays.length)}, minmax(86px, 1fr))` }}>
      <div className="top-axis-corner">Row</div>
      {bays.map((bay) => <button key={bay} type="button" className={bay === context.coordinates.bay ? 'top-bay-label active' : 'top-bay-label'} onClick={() => onCoordinates({ bay })}>B{bay}</button>)}
      {rows.map((row) => <div className="top-grid-row" key={row}>
        <button type="button" className={row === context.coordinates.rowBay ? 'top-row-label active' : 'top-row-label'} onClick={() => onCoordinates({ rowBay: row })}>R{row}</button>
        {bays.map((bay) => {
          const stackSlots = stacks[`${bay}:${row}`] ?? [];
          const summary = context.stackSummaries[`${bay}:${row}`];
          return <AggregateCell key={`${bay}:${row}`} slots={stackSlots} selectedSlotId={selectedSlotId} active={bay === context.coordinates.bay && row === context.coordinates.rowBay} context={{ ...context, coordinates: { ...context.coordinates, bay, rowBay: row } }} title={`Bay ${bay}, row ${row}`} canEdit={canEdit} onSelect={onSelect} onMove={onMove} target={chooseDropSlot(stackSlots, bay, row)}>
            <strong>{summary?.occupied ?? 0}/{summary?.capacity ?? 0}</strong><small>{displayWeight(summary?.weightKg)}</small>
          </AggregateCell>;
        })}
      </div>)}
    </div></div>
  </section>;
}

function ExactGrid({ columns, rows, slotAt, context, selectedSlotId, canEdit, onSelect, onMove, columnLabel, rowLabel, ariaLabel }) {
  return <div className="exact-slot-scroll"><div className="exact-slot-grid" style={{ gridTemplateColumns: `64px repeat(${Math.max(1, columns.length)}, minmax(104px, 1fr))` }} aria-label={ariaLabel}>
    <div className="exact-axis-corner">Eixo</div>
    {columns.map((column) => <div key={column} className="exact-column-label">{columnLabel(column)}</div>)}
    {rows.map((row) => <div className="exact-grid-row" key={row}>
      <div className="exact-row-label">{rowLabel(row)}</div>
      {columns.map((column) => {
        const slot = slotAt(row, column);
        return slot ? <SlotGlyph key={slot.id} slot={slot} selected={String(slot.id) === String(selectedSlotId)} context={context} canEdit={canEdit} onSelect={onSelect} onMove={onMove} /> : <div key={`${row}:${column}`} className="vessel-visual-slot absent">—</div>;
      })}
    </div>)}
  </div></div>;
}

function SectionView({ slots, context, selectedSlotId, canEdit, onSelect, onMove }) {
  const section = slots.filter((slot) => slot.bay === context.coordinates.bay);
  const rows = uniqueCoordinates(section, 'rowBay');
  const tiers = uniqueCoordinates(section, 'tier').sort((a, b) => b - a);
  const index = Object.fromEntries(section.map((slot) => [`${slot.tier}:${slot.rowBay}`, slot]));
  return <section className="vessel-view-card"><header><div><span className="view-kicker">Section view</span><h3>Seção do bay {context.coordinates.bay ?? '—'}</h3></div><small>Row × tier</small></header>
    {section.length ? <ExactGrid columns={rows} rows={tiers} slotAt={(tier, row) => index[`${tier}:${row}`]} context={context} selectedSlotId={selectedSlotId} canEdit={canEdit} onSelect={onSelect} onMove={onMove} columnLabel={(row) => `R${row}`} rowLabel={(tier) => `T${tier}`} ariaLabel="Seção transversal" /> : <div className="visual-empty">Nenhum slot neste bay.</div>}
  </section>;
}

function TierView({ slots, context, selectedSlotId, canEdit, onSelect, onMove }) {
  const tierSlots = slots.filter((slot) => slot.tier === context.coordinates.tier);
  const bays = uniqueCoordinates(tierSlots, 'bay');
  const rows = uniqueCoordinates(tierSlots, 'rowBay');
  const index = Object.fromEntries(tierSlots.map((slot) => [`${slot.bay}:${slot.rowBay}`, slot]));
  return <section className="vessel-view-card"><header><div><span className="view-kicker">Tier view</span><h3>Tier {context.coordinates.tier ?? '—'}</h3></div><small>Bay × row</small></header>
    {tierSlots.length ? <ExactGrid columns={rows} rows={bays} slotAt={(bay, row) => index[`${bay}:${row}`]} context={context} selectedSlotId={selectedSlotId} canEdit={canEdit} onSelect={onSelect} onMove={onMove} columnLabel={(row) => `R${row}`} rowLabel={(bay) => `B${bay}`} ariaLabel="Vista longitudinal do tier" /> : <div className="visual-empty">Nenhum slot neste tier.</div>}
  </section>;
}

function LegendPanel({ mode, setMode, legend }) {
  return <aside className="vessel-legend-panel"><label><span>Colorir por</span><select value={mode} onChange={(event) => setMode(event.target.value)}>{VESSEL_LEGEND_MODES.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
    <div className="vessel-legend-items">{legend.map((item) => <span key={item.value}><i className={`legend-tone-${item.tone}`} />{item.value}<small>{item.count}</small></span>)}{!legend.length && <span className="legend-empty">Nenhum contêiner alocado</span>}</div>
  </aside>;
}

function Inspector({ slot, context, onClear }) {
  if (!slot) return <aside className="vessel-slot-inspector empty"><span className="view-kicker">Seleção sincronizada</span><h3>Nenhum slot selecionado</h3><p>Selecione uma célula nas vistas profile, top, section ou tier.</p></aside>;
  const metadata = context.containerIndex[String(slot.codigoContainer ?? '').toUpperCase()] ?? {};
  return <aside className="vessel-slot-inspector"><div className="inspector-heading"><div><span className="view-kicker">Slot selecionado</span><h3>{formatSlotPosition(slot)}</h3></div><button type="button" className="icon-button" onClick={onClear}>×</button></div>
    <div className="inspector-status-row"><span className={slot.codigoContainer ? 'status-pill occupied' : 'status-pill empty'}>{slot.codigoContainer ? 'Ocupado' : 'Livre'}</span>{slot.restrito && <span className="status-pill danger">Restrito</span>}</div>
    <dl className="slot-inspector-grid"><div><dt>Contêiner</dt><dd>{slot.codigoContainer || '—'}</dd></div><div><dt>POD</dt><dd>{slot.portoDescarga || metadata.portoDescarga || '—'}</dd></div><div><dt>Peso</dt><dd>{displayWeight(slot.pesoVgmKg ?? slot.pesoKg ?? metadata.pesoVgmKg ?? metadata.pesoKg)}</dd></div><div><dt>IMO</dt><dd>{slot.perigoso || metadata.perigoso ? slot.classeImo || metadata.classeImo || 'N/I' : 'Não perigoso'}</dd></div><div><dt>Reefer</dt><dd>{slot.reefer || metadata.reefer ? 'Sim' : 'Não'}</dd></div><div><dt>Tampa</dt><dd>{slot.codigoHatchCover || 'Não aplicável'}</dd></div><div><dt>Operador</dt><dd>{legendValueForSlot(slot, 'OPERATOR', context.containerIndex)}</dd></div></dl>
  </aside>;
}

function UnallocatedTray({ containers, canEdit }) {
  return <section className="unallocated-tray"><header><div><span className="view-kicker">Load list</span><h3>Não alocados</h3></div><small>Arraste para um slot livre</small></header><div>{containers.map((container) => <article key={container.codigoContainer} draggable={canEdit} onDragStart={(event) => writeDragPayload(event, { kind: 'container', container })} className={canEdit ? 'draggable' : ''}><strong>{container.codigoContainer}</strong><span>{container.isoCode || 'ISO N/I'} · {displayWeight(container.pesoVgmKg ?? container.pesoKg)}</span><small>{container.portoDescarga || 'POD N/I'}{container.reefer ? ' · Reefer' : ''}</small></article>)}{!containers.length && <div className="visual-empty">Todos os contêineres estão alocados.</div>}</div></section>;
}

function TechnicalSummary({ stability, restow, sequencing, onSelect, slots }) {
  const restows = Array.isArray(restow?.movimentos) ? restow.movimentos : [];
  const operations = Array.isArray(sequencing?.sequencia) ? sequencing.sequencia : [];
  const byPosition = Object.fromEntries(slots.map((slot) => [slotPositionKey(slot), slot]));
  return <section className="vessel-phase1-technical"><article><span>Estabilidade</span><strong>{stability ? stability.aprovado ? 'Aprovada' : 'Com restrições' : 'Não calculada'}</strong></article><article><span>Restows</span><strong>{restows.length}</strong></article><article><span>Operações de guindaste</span><strong>{operations.length}</strong></article>{operations.slice(0, 6).map((operation) => <button key={`${operation.guindasteId}:${operation.ordem}`} type="button" onClick={() => onSelect(byPosition[`${operation.bay}:${operation.rowBay}:${operation.tier}`])}>Q{operation.guindasteId} #{operation.ordem} · B{operation.bay} R{operation.rowBay} T{operation.tier}</button>)}</section>;
}

export function VesselPlannerWorkspace({ plan, bayPlan, stability, restow, sequencing, selectedSlotId, onSelectSlot, onMoveContainer, canEdit, busy }) {
  const slots = useMemo(() => normalizeSlots(plan), [plan]);
  const bays = useMemo(() => uniqueCoordinates(slots, 'bay'), [slots]);
  const rows = useMemo(() => uniqueCoordinates(slots, 'rowBay'), [slots]);
  const tiers = useMemo(() => uniqueCoordinates(slots, 'tier'), [slots]);
  const containerIndex = useMemo(() => buildContainerIndex(bayPlan?.containers), [bayPlan]);
  const stackSummaries = useMemo(() => buildStackSummaries(slots), [slots]);
  const violationIndex = useMemo(() => buildViolationIndex(stability), [stability]);
  const restowIndex = useMemo(() => buildRestowIndex(restow), [restow]);
  const craneIndex = useMemo(() => buildCraneIndex(sequencing), [sequencing]);
  const selectedSlot = useMemo(() => slots.find((slot) => String(slot.id) === String(selectedSlotId)) ?? null, [selectedSlotId, slots]);
  const [viewMode, setViewMode] = useState('MULTI');
  const [legendMode, setLegendMode] = useState('POD');
  const [coordinates, setCoordinates] = useState(() => selectionCoordinates(slots[0]));
  const [hatchRefreshKey, setHatchRefreshKey] = useState(0);
  const allocated = useMemo(() => new Set(slots.map((slot) => slot.codigoContainer).filter(Boolean)), [slots]);
  const unallocated = useMemo(() => (Array.isArray(bayPlan?.containers) ? bayPlan.containers : []).filter((container) => !allocated.has(container.codigoContainer)), [allocated, bayPlan]);
  const legend = useMemo(() => buildLegend(slots, legendMode, containerIndex), [slots, legendMode, containerIndex]);

  function selectSlot(slot) {
    if (!slot) return;
    setCoordinates(selectionCoordinates(slot));
    onSelectSlot(slot);
  }

  function selectCoordinates(partial) {
    const next = { ...coordinates, ...partial };
    const slot = findSynchronizedSlot(slots, next, { preferOccupied: true });
    if (slot) selectSlot(slot);
    else setCoordinates(next);
  }

  useEffect(() => {
    if (selectedSlot) setCoordinates(selectionCoordinates(selectedSlot));
  }, [selectedSlot]);

  useEffect(() => {
    if (!slots.length) return;
    if (!selectedSlot) selectSlot(findSynchronizedSlot(slots, coordinates, { preferOccupied: true }));
  }, [slots]);

  const context = { legendMode, containerIndex, stackSummaries, violationIndex, restowIndex, craneIndex, coordinates };

  async function move(payload, target) {
    if (!canEdit || busy || !target || target.codigoContainer || target.restrito) return;
    await onMoveContainer(payload, target);
    selectSlot(target);
  }

  function renderView(mode) {
    const shared = { slots, context, selectedSlotId, canEdit: canEdit && !busy, onSelect: selectSlot, onMove: move };
    if (mode === 'PROFILE') return <ProfileView {...shared} onCoordinates={selectCoordinates} />;
    if (mode === 'TOP') return <TopView {...shared} onCoordinates={selectCoordinates} />;
    if (mode === 'SECTION') return <SectionView {...shared} />;
    return <TierView {...shared} />;
  }

  if (!slots.length) return <div className="visual-empty large">O plano não possui geometria de slots.</div>;

  return <div className="vessel-planner-workspace">
    <div className="vessel-workspace-toolbar"><div className="view-mode-tabs" role="tablist" aria-label="Vistas do Vessel Planner">{VESSEL_VIEW_MODES.map((mode) => <button key={mode.value} type="button" className={viewMode === mode.value ? 'active' : ''} onClick={() => setViewMode(mode.value)}>{mode.label}</button>)}</div><div className="coordinate-controls"><label>Bay<select value={coordinates.bay ?? ''} onChange={(event) => selectCoordinates({ bay: Number(event.target.value) })}>{bays.map((bay) => <option key={bay} value={bay}>{bay}</option>)}</select></label><label>Row<select value={coordinates.rowBay ?? ''} onChange={(event) => selectCoordinates({ rowBay: Number(event.target.value) })}>{rows.map((row) => <option key={row} value={row}>{row}</option>)}</select></label><label>Tier<select value={coordinates.tier ?? ''} onChange={(event) => selectCoordinates({ tier: Number(event.target.value) })}>{tiers.map((tier) => <option key={tier} value={tier}>{tier}</option>)}</select></label></div></div>
    <div className="vessel-phase1-selection"><span>Seleção sincronizada</span><strong>B{coordinates.bay} · R{coordinates.rowBay} · T{coordinates.tier}</strong><small>{selectedSlot?.codigoContainer || 'Slot livre'} · {selectedSlot ? legendValueForSlot(selectedSlot, legendMode, containerIndex) : '—'}</small></div>
    <LegendPanel mode={legendMode} setMode={setLegendMode} legend={legend} />
    <div className="vessel-workspace-layout"><main className="vessel-main-canvas">{viewMode === 'MULTI' ? <div className="multi-view-grid"><div>{renderView('PROFILE')}</div><div>{renderView('TOP')}</div><div>{renderView('SECTION')}</div><div>{renderView('TIER')}</div></div> : renderView(viewMode)}</main><Inspector slot={selectedSlot} context={context} onClear={() => onSelectSlot(null)} /></div>
    <UnallocatedTray containers={unallocated} canEdit={canEdit && !busy} />
    <HatchCoverOperationPanel plan={plan} canEdit={canEdit} disabled={busy} onChanged={() => setHatchRefreshKey((current) => current + 1)} />
    <CraneExecutionTimeline plan={plan} sequencing={sequencing} disabled={busy} refreshKey={hatchRefreshKey} />
    <TechnicalSummary stability={stability} restow={restow} sequencing={sequencing} onSelect={selectSlot} slots={slots} />
  </div>;
}
