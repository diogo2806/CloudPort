import { useEffect, useMemo, useState } from 'react';
import {
  VESSEL_LEGEND_MODES,
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
import {
  DROP_VALIDATION_STATUS,
  dragSource,
  hatchCoverForSlot,
  projectedStackWeight,
  stackStatus,
  validateDropTarget,
  validationClass
} from '../vessel-planner-phase2.js';
import {
  VESSEL_OVERLAY_MODES,
  aggregateOverlayForSlots,
  buildImdgIndex,
  buildOverlayIndex
} from '../vessel-planner-phase3.js';
import {
  COMPLETE_VESSEL_VIEW_MODES,
  overlayClassName,
  overlayTooltip
} from '../vessel-planner-complete.js';
import '../vessel-planner-phase1.css';
import '../vessel-planner-phase2.css';
import '../vessel-planner-complete.css';
import { CraneExecutionTimeline } from './CraneExecutionTimeline.jsx';
import { VesselHatchCoverPanel } from './VesselHatchCoverPanel.jsx';
import { VesselPlannerScanView } from './VesselPlannerScanView.jsx';

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

function HatchStateBadge({ slot, covers }) {
  if (!slot?.codigoHatchCover) return null;
  const cover = hatchCoverForSlot(slot, covers);
  const state = String(cover?.estado ?? 'NÃO CARREGADA').toLowerCase();
  return <span className={`hatch-state-badge ${state}`}>HC {slot.codigoHatchCover} · {cover?.estado ?? 'N/I'}</span>;
}

function SlotGlyph({ slot, selected, context, drag, canEdit, onSelect, onDragStart, onDragEnd, onHoverTarget, onDropTarget }) {
  const occupied = Boolean(slot?.codigoContainer);
  const legendValue = legendValueForSlot(slot, context.legendMode, context.containerIndex);
  const warnings = buildSlotWarnings(slot, { violations: context.violationIndex }, context.stackSummaries[stackPositionKey(slot)]);
  const restow = context.restowIndex[slotPositionKey(slot)];
  const crane = context.craneIndex[slotPositionKey(slot)]?.[0];
  const validation = drag.payload ? context.validateTarget(slot, drag.payload) : null;
  const source = dragSource(drag.payload);
  const sourceSlot = source?.sourceSlotId && String(source.sourceSlotId) === String(slot.id);
  const hovered = String(drag.hoveredTargetId ?? '') === String(slot.id);
  const overlay = context.overlayIndex?.[String(slot.id)] ?? null;
  const tooltip = [
    (validation?.reasons?.length ? validation.reasons : warnings).map((warning) => warning.message).join('\n'),
    overlayTooltip(overlay)
  ].filter(Boolean).join('\n');
  const classes = [
    'vessel-visual-slot',
    occupied ? 'occupied' : 'empty',
    slot.restrito ? 'restricted' : '',
    selected ? 'selected' : '',
    occupied ? `legend-tone-${toneIndex(legendValue)}` : '',
    warnings.length ? 'has-warning' : '',
    validationClass(validation),
    overlayClassName(overlay),
    hovered ? 'drop-hover' : '',
    sourceSlot ? 'drag-source' : ''
  ].filter(Boolean).join(' ');

  return <button
    type="button"
    className={classes}
    draggable={canEdit && occupied}
    onDragStart={(event) => {
      if (!occupied) return;
      const payload = { kind: 'slot', slot };
      writeDragPayload(event, payload);
      onDragStart(payload);
    }}
    onDragEnd={onDragEnd}
    onDragEnter={() => drag.payload && onHoverTarget(slot)}
    onDragOver={(event) => {
      if (!drag.payload) return;
      event.preventDefault();
      event.dataTransfer.dropEffect = validation?.status === DROP_VALIDATION_STATUS.BLOCKED ? 'none' : 'move';
    }}
    onDrop={(event) => onDropTarget(event, slot)}
    onClick={() => onSelect(slot)}
    aria-pressed={selected}
    aria-label={`${formatSlotPosition(slot)}, ${occupied ? `contêiner ${slot.codigoContainer}` : 'livre'}, ${legendValue}`}
    title={tooltip || `${formatSlotPosition(slot)} · ${legendValue}`}
  >
    {overlay?.shortLabel && <i className="vessel-complete-overlay-badge">{overlay.shortLabel}</i>}
    <span className="slot-coordinate">B{slot.bay} · R{slot.rowBay} · T{slot.tier}</span>
    <strong>{occupied ? slot.codigoContainer : slot.restrito ? 'Restrito' : 'Livre'}</strong>
    <small>{occupied ? `${legendValue} · ${displayWeight(slot.pesoVgmKg ?? slot.pesoKg)}` : slot.tipoSlot || 'NORMAL'}</small>
    <HatchStateBadge slot={slot} covers={context.hatchCovers} />
    <span className="slot-badges" aria-hidden="true">
      {slot.reefer && <i>RF</i>}{slot.perigoso && <i>IMO</i>}{restow && <i>R</i>}{crane && <i>Q{crane.guindasteId}</i>}{warnings.length > 0 && <i className="warning-badge">!{warnings.length}</i>}
    </span>
  </button>;
}

function AggregateCell({ slots, selectedSlotId, active, context, title, children, canEdit, drag, onSelect, onDragStart, onDragEnd, onHoverTarget, onDropTarget, target }) {
  const occupied = slots.filter((slot) => slot.codigoContainer);
  const dominant = dominantLegendForSlots(occupied, context.legendMode, context.containerIndex);
  const representative = findSynchronizedSlot(slots, context.coordinates, { preferOccupied: true });
  const selected = slots.some((slot) => String(slot.id) === String(selectedSlotId));
  const restricted = slots.length > 0 && slots.every((slot) => slot.restrito);
  const validation = drag.payload ? context.validateTarget(target, drag.payload) : null;
  const hovered = target && String(drag.hoveredTargetId ?? '') === String(target.id);
  const draggableSlot = occupied.length === 1 ? occupied[0] : null;
  const overlay = aggregateOverlayForSlots(slots, context.overlayIndex);
  return <button
    type="button"
    className={[
      'vessel-phase1-aggregate',
      occupied.length ? 'occupied' : 'empty',
      restricted ? 'restricted' : '',
      selected ? 'selected' : '',
      active ? 'active-coordinate' : '',
      dominant ? `legend-tone-${dominant.tone}` : '',
      validationClass(validation),
      overlayClassName(overlay),
      hovered ? 'drop-hover' : ''
    ].filter(Boolean).join(' ')}
    draggable={Boolean(canEdit && draggableSlot)}
    onDragStart={(event) => {
      if (!draggableSlot) return;
      const payload = { kind: 'slot', slot: draggableSlot };
      writeDragPayload(event, payload);
      onDragStart(payload);
    }}
    onDragEnd={onDragEnd}
    onClick={() => representative && onSelect(representative)}
    onDragEnter={() => drag.payload && target && onHoverTarget(target)}
    onDragOver={(event) => {
      if (!drag.payload) return;
      event.preventDefault();
      event.dataTransfer.dropEffect = validation?.status === DROP_VALIDATION_STATUS.BLOCKED ? 'none' : 'move';
    }}
    onDrop={(event) => onDropTarget(event, target)}
    aria-pressed={selected}
    title={[title, dominant ? dominant.value : '', validation?.reasons?.map((reason) => reason.message).join('\n'), overlayTooltip(overlay)].filter(Boolean).join('\n')}
  >
    {overlay?.shortLabel && <i className="vessel-complete-overlay-badge">{overlay.shortLabel}</i>}
    {children}
    {dominant && <span className="aggregate-legend"><i className={`legend-tone-${dominant.tone}`} />{dominant.value}</span>}
  </button>;
}

function ProfileView({ slots, context, selectedSlotId, canEdit, drag, onSelect, onDragStart, onDragEnd, onHoverTarget, onDropTarget, onCoordinates }) {
  const bays = uniqueCoordinates(slots, 'bay');
  const tiers = uniqueCoordinates(slots, 'tier').sort((left, right) => right - left);
  const groups = useMemo(() => slots.reduce((index, slot) => {
    const key = `${slot.bay}:${slot.tier}`;
    if (!index[key]) index[key] = [];
    index[key].push(slot);
    return index;
  }, {}), [slots]);

  return <section className="vessel-view-card profile-view" aria-label="Perfil lateral do navio">
    <header><div><span className="view-kicker">Profile view</span><h3>Perfil lateral</h3></div><small>Bays, tiers e tampas sincronizados</small></header>
    <div className="profile-scroll"><div className="profile-grid" style={{ gridTemplateColumns: `52px repeat(${Math.max(1, bays.length)}, minmax(96px, 1fr))` }}>
      <div className="profile-axis-corner">Tier</div>
      {bays.map((bay) => <button key={bay} type="button" className={bay === context.coordinates.bay ? 'profile-bay-head active' : 'profile-bay-head'} onClick={() => onCoordinates({ bay })}>B{bay}</button>)}
      {tiers.map((tier) => <div className="profile-row" key={tier}>
        <div className="profile-tier-label">T{tier}</div>
        {bays.map((bay) => {
          const group = groups[`${bay}:${tier}`] ?? [];
          const target = group.filter((slot) => !slot.codigoContainer && !slot.restrito).sort((left, right) => left.rowBay - right.rowBay)[0] ?? null;
          return <AggregateCell key={`${bay}:${tier}`} slots={group} selectedSlotId={selectedSlotId} active={bay === context.coordinates.bay && tier === context.coordinates.tier} context={{ ...context, coordinates: { ...context.coordinates, bay, tier } }} title={`Bay ${bay}, tier ${tier}`} canEdit={canEdit} drag={drag} onSelect={onSelect} onDragStart={onDragStart} onDragEnd={onDragEnd} onHoverTarget={onHoverTarget} onDropTarget={onDropTarget} target={target}>
            <strong>{group.filter((slot) => slot.codigoContainer).length}/{group.length}</strong><small>{group.find((slot) => slot.codigoHatchCover)?.codigoHatchCover || 'slots'}</small>
          </AggregateCell>;
        })}
      </div>)}
      <div className="profile-hatch-label">Tampas</div>
      {bays.map((bay) => {
        const codes = Array.from(new Set(slots.filter((slot) => slot.bay === bay).map((slot) => slot.codigoHatchCover).filter(Boolean)));
        const selected = codes.includes(context.selectedCoverCode);
        return <div key={`hatch-${bay}`} className={`profile-hatch-cell${selected ? ' hatch-selected' : ''}`}>
          {codes.length ? codes.map((code) => {
            const cover = context.hatchCovers.find((item) => String(item.codigo).toUpperCase() === String(code).toUpperCase());
            return <span key={code} className={`hatch-state-badge ${String(cover?.estado ?? '').toLowerCase()}`}>{code} · {cover?.estado ?? 'N/I'}</span>;
          }) : 'Convés aberto'}
        </div>;
      })}
    </div></div>
    <div className="vessel-silhouette" aria-hidden="true"><span className="bow" /><span className="hull" /><span className="stern" /></div>
  </section>;
}

function TopView({ slots, context, selectedSlotId, canEdit, drag, onSelect, onDragStart, onDragEnd, onHoverTarget, onDropTarget, onCoordinates }) {
  const bays = uniqueCoordinates(slots, 'bay');
  const rows = uniqueCoordinates(slots, 'rowBay');
  const stacks = useMemo(() => slots.reduce((index, slot) => {
    const key = stackPositionKey(slot);
    if (!index[key]) index[key] = [];
    index[key].push(slot);
    return index;
  }, {}), [slots]);

  return <section className="vessel-view-card top-view" aria-label="Vista superior do navio">
    <header><div><span className="view-kicker">Top view</span><h3>Vista superior</h3></div><small>Ocupação e peso acumulado por stack</small></header>
    <div className="top-scroll"><div className="top-grid" style={{ gridTemplateColumns: `52px repeat(${Math.max(1, bays.length)}, minmax(96px, 1fr))` }}>
      <div className="top-axis-corner">Row</div>
      {bays.map((bay) => <button key={bay} type="button" className={bay === context.coordinates.bay ? 'top-bay-label active' : 'top-bay-label'} onClick={() => onCoordinates({ bay })}>B{bay}</button>)}
      {rows.map((row) => <div className="top-grid-row" key={row}>
        <button type="button" className={row === context.coordinates.rowBay ? 'top-row-label active' : 'top-row-label'} onClick={() => onCoordinates({ rowBay: row })}>R{row}</button>
        {bays.map((bay) => {
          const stackSlots = stacks[`${bay}:${row}`] ?? [];
          const summary = context.stackSummaries[`${bay}:${row}`];
          const target = chooseDropSlot(stackSlots, bay, row);
          const projection = drag.payload && target ? projectedStackWeight(target, drag.payload, context.stackSummaries) : null;
          const percent = projection?.percent ?? summary?.percent;
          return <AggregateCell key={`${bay}:${row}`} slots={stackSlots} selectedSlotId={selectedSlotId} active={bay === context.coordinates.bay && row === context.coordinates.rowBay} context={{ ...context, coordinates: { ...context.coordinates, bay, rowBay: row } }} title={`Bay ${bay}, row ${row}`} canEdit={canEdit} drag={drag} onSelect={onSelect} onDragStart={onDragStart} onDragEnd={onDragEnd} onHoverTarget={onHoverTarget} onDropTarget={onDropTarget} target={target}>
            <strong>{summary?.occupied ?? 0}/{summary?.capacity ?? 0}</strong><small>{displayWeight(projection?.projectedWeightKg ?? summary?.weightKg)}{projection ? ' projetado' : ''}</small>
            {summary?.maxWeightKg && <span className="stack-weight-meter"><span style={{ width: `${Math.min(100, Math.max(2, percent ?? 0))}%` }} /></span>}
          </AggregateCell>;
        })}
      </div>)}
    </div></div>
  </section>;
}

function ExactGrid({ columns, rows, slotAt, context, selectedSlotId, canEdit, drag, onSelect, onDragStart, onDragEnd, onHoverTarget, onDropTarget, columnLabel, rowLabel, ariaLabel }) {
  return <div className="exact-slot-scroll"><div className="exact-slot-grid" style={{ gridTemplateColumns: `64px repeat(${Math.max(1, columns.length)}, minmax(116px, 1fr))` }} aria-label={ariaLabel}>
    <div className="exact-axis-corner">Eixo</div>
    {columns.map((column) => <div key={column} className="exact-column-label">{columnLabel(column)}</div>)}
    {rows.map((row) => <div className="exact-grid-row" key={row}>
      <div className="exact-row-label">{rowLabel(row)}</div>
      {columns.map((column) => {
        const slot = slotAt(row, column);
        return slot ? <SlotGlyph key={slot.id} slot={slot} selected={String(slot.id) === String(selectedSlotId)} context={context} drag={drag} canEdit={canEdit} onSelect={onSelect} onDragStart={onDragStart} onDragEnd={onDragEnd} onHoverTarget={onHoverTarget} onDropTarget={onDropTarget} /> : <div key={`${row}:${column}`} className="vessel-visual-slot absent">—</div>;
      })}
    </div>)}
  </div></div>;
}

function SectionView(props) {
  const { slots, context, selectedSlotId, canEdit, drag, onSelect, onDragStart, onDragEnd, onHoverTarget, onDropTarget } = props;
  const section = slots.filter((slot) => slot.bay === context.coordinates.bay);
  const rows = uniqueCoordinates(section, 'rowBay');
  const tiers = uniqueCoordinates(section, 'tier').sort((left, right) => right - left);
  const index = Object.fromEntries(section.map((slot) => [`${slot.tier}:${slot.rowBay}`, slot]));
  return <section className="vessel-view-card"><header><div><span className="view-kicker">Section view</span><h3>Seção do bay {context.coordinates.bay ?? '—'}</h3></div><small>Restrições por slot e tampa</small></header>
    {section.length ? <ExactGrid columns={rows} rows={tiers} slotAt={(tier, row) => index[`${tier}:${row}`]} context={context} selectedSlotId={selectedSlotId} canEdit={canEdit} drag={drag} onSelect={onSelect} onDragStart={onDragStart} onDragEnd={onDragEnd} onHoverTarget={onHoverTarget} onDropTarget={onDropTarget} columnLabel={(row) => `R${row}`} rowLabel={(tier) => `T${tier}`} ariaLabel="Seção transversal" /> : <div className="visual-empty">Nenhum slot neste bay.</div>}
  </section>;
}

function TierView(props) {
  const { slots, context, selectedSlotId, canEdit, drag, onSelect, onDragStart, onDragEnd, onHoverTarget, onDropTarget } = props;
  const tierSlots = slots.filter((slot) => slot.tier === context.coordinates.tier);
  const bays = uniqueCoordinates(tierSlots, 'bay');
  const rows = uniqueCoordinates(tierSlots, 'rowBay');
  const index = Object.fromEntries(tierSlots.map((slot) => [`${slot.bay}:${slot.rowBay}`, slot]));
  return <section className="vessel-view-card"><header><div><span className="view-kicker">Tier view</span><h3>Tier {context.coordinates.tier ?? '—'}</h3></div><small>Validação longitudinal e transversal</small></header>
    {tierSlots.length ? <ExactGrid columns={rows} rows={bays} slotAt={(bay, row) => index[`${bay}:${row}`]} context={context} selectedSlotId={selectedSlotId} canEdit={canEdit} drag={drag} onSelect={onSelect} onDragStart={onDragStart} onDragEnd={onDragEnd} onHoverTarget={onHoverTarget} onDropTarget={onDropTarget} columnLabel={(row) => `R${row}`} rowLabel={(bay) => `B${bay}`} ariaLabel="Plano do tier" /> : <div className="visual-empty">Nenhum slot neste tier.</div>}
  </section>;
}

function ScanView(props) {
  const { slots, context, selectedSlotId, canEdit, drag, onSelect, onDragStart, onDragEnd, onHoverTarget, onDropTarget } = props;
  return <VesselPlannerScanView
    slots={slots}
    context={context}
    selectedSlotId={selectedSlotId}
    onSelect={onSelect}
    renderSlot={(slot) => <SlotGlyph key={slot.id} slot={slot} selected={String(slot.id) === String(selectedSlotId)} context={context} drag={drag} canEdit={canEdit} onSelect={onSelect} onDragStart={onDragStart} onDragEnd={onDragEnd} onHoverTarget={onHoverTarget} onDropTarget={onDropTarget} />}
  />;
}

function LegendPanel({ mode, setMode, legend }) {
  return <aside className="vessel-legend-panel"><label><span>Colorir por</span><select value={mode} onChange={(event) => setMode(event.target.value)}>{VESSEL_LEGEND_MODES.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
    <div className="vessel-legend-items">{legend.map((item) => <span key={item.value}><i className={`legend-tone-${item.tone}`} />{item.value}<small>{item.count}</small></span>)}{!legend.length && <span className="legend-empty">Nenhum contêiner alocado</span>}</div>
  </aside>;
}

function ValidationKey() {
  return <div className="vessel-phase2-key" aria-label="Legenda de validação de movimentação"><span><i className="valid" />Válido</span><span><i className="warning" />Atenção</span><span><i className="blocked" />Bloqueado</span><span><i className="restricted" />Restrito</span></div>;
}

function DragValidationBanner({ payload, target, validation }) {
  const source = dragSource(payload);
  if (!payload) return <div className="vessel-drag-validation"><span>Drag-and-drop</span><strong>Arraste um contêiner para validar todos os destinos em tempo real.</strong><small>O backend permanece como validação definitiva.</small></div>;
  const status = String(validation?.status ?? 'VALID').toLowerCase();
  const reason = validation?.reasons?.map((item) => item.message).join(' · ') || 'Destino compatível com as validações locais.';
  return <div className={`vessel-drag-validation ${status}`} role="status"><span>{validation?.status ?? 'VALID'}</span><strong>{source?.codigoContainer ?? 'Contêiner'} → {target ? formatSlotPosition(target) : 'selecione um destino'} · {reason}</strong><small>{validation?.maxWeightKg ? `Stack: ${displayWeight(validation.projectedWeightKg)} / ${displayWeight(validation.maxWeightKg)} (${validation.percent}%)` : 'Stack sem limite informado'}</small></div>;
}

function Inspector({ slot, context, dragPayload, onClear }) {
  if (!slot) return <aside className="vessel-slot-inspector empty"><span className="view-kicker">Seleção sincronizada</span><h3>Nenhum slot selecionado</h3><p>Selecione uma célula nas vistas profile, top, scan, section ou tier.</p></aside>;
  const metadata = context.containerIndex[String(slot.codigoContainer ?? '').toUpperCase()] ?? {};
  const summary = context.stackSummaries[stackPositionKey(slot)];
  const warnings = buildSlotWarnings(slot, { violations: context.violationIndex }, summary);
  const validation = dragPayload ? context.validateTarget(slot, dragPayload) : null;
  const restrictions = validation?.reasons?.length ? validation.reasons : warnings.map((item) => ({ ...item, status: String(item.severity).toUpperCase() === 'PERIGO' ? DROP_VALIDATION_STATUS.BLOCKED : DROP_VALIDATION_STATUS.WARNING }));
  const overlay = context.overlayIndex?.[String(slot.id)] ?? null;
  return <aside className="vessel-slot-inspector"><div className="inspector-heading"><div><span className="view-kicker">Slot selecionado</span><h3>{formatSlotPosition(slot)}</h3></div><button type="button" className="icon-button" onClick={onClear} aria-label="Limpar seleção">×</button></div>
    <div className="inspector-status-row"><span className={slot.codigoContainer ? 'status-pill occupied' : 'status-pill empty'}>{slot.codigoContainer ? 'Ocupado' : 'Livre'}</span>{slot.restrito && <span className="status-pill danger">Restrito</span>}{overlay?.risk && overlay.risk !== 'NONE' && <span className={`status-pill ${String(overlay.risk).toLowerCase()}`}>{overlay.label} · {overlay.risk}</span>}<HatchStateBadge slot={slot} covers={context.hatchCovers} /></div>
    <dl className="slot-inspector-grid"><div><dt>Contêiner</dt><dd>{slot.codigoContainer || '—'}</dd></div><div><dt>POD</dt><dd>{slot.portoDescarga || metadata.portoDescarga || '—'}</dd></div><div><dt>Peso</dt><dd>{displayWeight(slot.pesoVgmKg ?? slot.pesoKg ?? metadata.pesoVgmKg ?? metadata.pesoKg)}</dd></div><div><dt>Peso da stack</dt><dd>{displayWeight(summary?.weightKg)}{summary?.maxWeightKg ? ` / ${displayWeight(summary.maxWeightKg)} · ${summary.percent}%` : ''}</dd></div><div><dt>IMO</dt><dd>{slot.perigoso || metadata.perigoso ? slot.classeImo || metadata.classeImo || 'N/I' : 'Não perigoso'}</dd></div><div><dt>Reefer</dt><dd>{slot.reefer || metadata.reefer ? 'Sim' : 'Não'}</dd></div><div><dt>Operador</dt><dd>{legendValueForSlot(slot, 'OPERATOR', context.containerIndex)}</dd></div><div><dt>Limite do slot</dt><dd>{displayWeight(slot.maxPesoKg ?? slot.limitePesoKg)}</dd></div></dl>
    {restrictions.length > 0 && <div><span className="view-kicker">Restrições e validações</span><ul className="slot-restriction-list">{restrictions.map((item, index) => <li key={`${item.code ?? item.type}-${index}`} className={item.status === DROP_VALIDATION_STATUS.BLOCKED ? 'blocked' : ''}>{item.message}</li>)}</ul></div>}
    {overlay?.details?.length > 0 && <div className="vessel-overlay-details"><span className="view-kicker">Camada técnica · {overlay.label}</span><ul>{overlay.details.map((detail, index) => <li key={`${detail}-${index}`}>{detail}</li>)}</ul></div>}
  </aside>;
}

function UnallocatedTray({ containers, canEdit, onDragStart, onDragEnd }) {
  return <section className="unallocated-tray"><header><div><span className="view-kicker">Load list</span><h3>Não alocados</h3></div><small>Arraste para um slot livre</small></header><div>{containers.map((container) => <article key={container.codigoContainer} draggable={canEdit} onDragStart={(event) => { const payload = { kind: 'container', container }; writeDragPayload(event, payload); onDragStart(payload); }} onDragEnd={onDragEnd} className={canEdit ? 'draggable' : ''}><strong>{container.codigoContainer}</strong><span>{container.isoCode || 'ISO N/I'} · {displayWeight(container.pesoVgmKg ?? container.pesoKg)}</span><small>{container.portoDescarga || 'POD N/I'}{container.reefer ? ' · Reefer' : ''}{container.classeImo ? ` · IMO ${container.classeImo}` : ''}</small></article>)}{!containers.length && <div className="visual-empty">Todos os contêineres estão alocados.</div>}</div></section>;
}

function StackWeightPanel({ slots, summaries, coordinates, dragPayload, onSelect }) {
  const stacks = Object.values(summaries).sort((left, right) => left.bay - right.bay || left.rowBay - right.rowBay);
  return <section className="stack-weight-panel"><header><div><span className="view-kicker">Stack weight</span><h3>Peso acumulado por stack</h3></div><small>Verde &lt; 85% · âmbar 85–100% · vermelho &gt; 100%</small></header>
    <div className="stack-weight-grid">{stacks.map((summary) => {
      const stackSlots = slots.filter((slot) => stackPositionKey(slot) === summary.key);
      const target = chooseDropSlot(stackSlots, summary.bay, summary.rowBay);
      const projection = dragPayload && target ? projectedStackWeight(target, dragPayload, summaries) : null;
      const percent = projection?.percent ?? summary.percent;
      const status = stackStatus(summary, percent).toLowerCase();
      const representative = findSynchronizedSlot(stackSlots, { bay: summary.bay, rowBay: summary.rowBay, tier: coordinates.tier }, { preferOccupied: true });
      return <button key={summary.key} type="button" className={`stack-weight-card ${status}${summary.bay === coordinates.bay && summary.rowBay === coordinates.rowBay ? ' active' : ''}`} onClick={() => representative && onSelect(representative)}>
        <strong>B{summary.bay} · R{summary.rowBay} · {summary.occupied}/{summary.capacity}</strong>
        <small>{displayWeight(projection?.projectedWeightKg ?? summary.weightKg)}{summary.maxWeightKg ? ` / ${displayWeight(summary.maxWeightKg)}` : ' · sem limite'}{projection ? ' · projetado' : ''}</small>
        {summary.maxWeightKg && <span className="stack-weight-meter"><span style={{ width: `${Math.min(100, Math.max(2, percent ?? 0))}%` }} /></span>}
      </button>;
    })}</div>
  </section>;
}

function TechnicalSummary({ stability, restow, sequencing, onSelect, slots }) {
  const restows = Array.isArray(restow?.movimentos) ? restow.movimentos : [];
  const operations = Array.isArray(sequencing?.sequencia) ? sequencing.sequencia : [];
  const byPosition = Object.fromEntries(slots.map((slot) => [slotPositionKey(slot), slot]));
  const blocked = operations.filter((operation) => operation.bloqueadoPorTampa).length;
  return <section className="vessel-phase1-technical"><article><span>Estabilidade</span><strong>{stability ? stability.aprovado ? 'Aprovada' : 'Com restrições' : 'Não calculada'}</strong></article><article><span>Restows</span><strong>{restows.length}</strong></article><article><span>Operações de guindaste</span><strong>{operations.length}</strong></article><article><span>Bloqueadas por tampa</span><strong>{blocked}</strong></article>{operations.slice(0, 6).map((operation) => <button key={`${operation.guindasteId}:${operation.ordem}`} type="button" title={operation.motivoBloqueioTampa || ''} onClick={() => onSelect(byPosition[`${operation.bay}:${operation.rowBay}:${operation.tier}`])}>Q{operation.guindasteId} #{operation.ordem} · B{operation.bay} R{operation.rowBay} T{operation.tier}{operation.bloqueadoPorTampa ? ' · BLOQUEADA' : ''}</button>)}</section>;
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
  const [overlayMode, setOverlayMode] = useState('COMBINED');
  const [coordinates, setCoordinates] = useState(() => selectionCoordinates(slots[0]));
  const [hatchCovers, setHatchCovers] = useState([]);
  const [dragPayload, setDragPayload] = useState(null);
  const [hoveredTargetId, setHoveredTargetId] = useState('');
  const [lastValidation, setLastValidation] = useState(null);
  const imdgIndex = useMemo(() => buildImdgIndex(slots, containerIndex, violationIndex), [slots, containerIndex, violationIndex]);
  const overlayIndex = useMemo(() => buildOverlayIndex(slots, overlayMode, { stackSummaries, violationIndex, imdgIndex }), [slots, overlayMode, stackSummaries, violationIndex, imdgIndex]);
  const allocated = useMemo(() => new Set(slots.map((slot) => slot.codigoContainer).filter(Boolean)), [slots]);
  const unallocated = useMemo(() => (Array.isArray(bayPlan?.containers) ? bayPlan.containers : []).filter((container) => !allocated.has(container.codigoContainer)), [allocated, bayPlan]);
  const legend = useMemo(() => buildLegend(slots, legendMode, containerIndex), [slots, legendMode, containerIndex]);
  const hoveredTarget = useMemo(() => slots.find((slot) => String(slot.id) === String(hoveredTargetId)) ?? null, [hoveredTargetId, slots]);
  const selectedCoverCode = selectedSlot?.codigoHatchCover ?? '';

  function validateTarget(target, payload = dragPayload) {
    return validateDropTarget({ payload, target, slots, stackSummaries, hatchCovers });
  }

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

  function startDrag(payload) {
    setDragPayload(payload);
    setHoveredTargetId('');
    setLastValidation(null);
  }

  function endDrag() {
    setDragPayload(null);
    setHoveredTargetId('');
  }

  function hoverTarget(target) {
    setHoveredTargetId(target ? String(target.id) : '');
  }

  async function dropTarget(event, target) {
    event.preventDefault();
    event.stopPropagation();
    const payload = readDragPayload(event) ?? dragPayload;
    const validation = validateTarget(target, payload);
    setLastValidation({ target, validation, payload });
    if (validation.status === DROP_VALIDATION_STATUS.BLOCKED || !target) {
      setHoveredTargetId(target ? String(target.id) : '');
      return;
    }
    await onMoveContainer(payload, target);
    selectSlot(target);
    endDrag();
  }

  function selectCover(cover) {
    const candidates = slots.filter((slot) => String(slot.codigoHatchCover).toUpperCase() === String(cover?.codigo).toUpperCase());
    const slot = findSynchronizedSlot(candidates, coordinates, { preferOccupied: true }) ?? candidates[0];
    if (slot) selectSlot(slot);
  }

  useEffect(() => {
    if (selectedSlot) setCoordinates(selectionCoordinates(selectedSlot));
  }, [selectedSlot]);

  useEffect(() => {
    if (!slots.length) return;
    if (!selectedSlot) selectSlot(findSynchronizedSlot(slots, coordinates, { preferOccupied: true }));
  }, [slots]);

  const activeBanner = dragPayload && hoveredTarget
    ? { payload: dragPayload, target: hoveredTarget, validation: validateTarget(hoveredTarget, dragPayload) }
    : lastValidation ?? { payload: dragPayload, target: null, validation: null };
  const context = { legendMode, overlayMode, overlayIndex, imdgIndex, containerIndex, stackSummaries, violationIndex, restowIndex, craneIndex, coordinates, hatchCovers, selectedCoverCode, validateTarget };
  const drag = { payload: dragPayload, hoveredTargetId };

  function renderView(mode) {
    const shared = { slots, context, selectedSlotId, canEdit: canEdit && !busy, drag, onSelect: selectSlot, onDragStart: startDrag, onDragEnd: endDrag, onHoverTarget: hoverTarget, onDropTarget: dropTarget };
    if (mode === 'PROFILE') return <ProfileView {...shared} onCoordinates={selectCoordinates} />;
    if (mode === 'TOP') return <TopView {...shared} onCoordinates={selectCoordinates} />;
    if (mode === 'SCAN') return <ScanView {...shared} />;
    if (mode === 'SECTION') return <SectionView {...shared} />;
    return <TierView {...shared} />;
  }

  if (!slots.length) return <div className="visual-empty large">O plano não possui geometria de slots.</div>;

  return <div className="vessel-planner-workspace">
    <VesselHatchCoverPanel planId={plan?.id} canEdit={canEdit} busy={busy} selectedCoverCode={selectedCoverCode} onSelectCover={selectCover} onCoversChange={setHatchCovers} />
    <div className="vessel-workspace-toolbar">
      <div className="view-mode-tabs" role="tablist" aria-label="Vistas do Vessel Planner">{COMPLETE_VESSEL_VIEW_MODES.map((mode) => <button key={mode.value} type="button" className={viewMode === mode.value ? 'active' : ''} onClick={() => setViewMode(mode.value)}>{mode.label}</button>)}</div>
      <div className="coordinate-controls">
        <label>Bay<select value={coordinates.bay ?? ''} onChange={(event) => selectCoordinates({ bay: Number(event.target.value) })}>{bays.map((bay) => <option key={bay} value={bay}>{bay}</option>)}</select></label>
        <label>Row<select value={coordinates.rowBay ?? ''} onChange={(event) => selectCoordinates({ rowBay: Number(event.target.value) })}>{rows.map((row) => <option key={row} value={row}>{row}</option>)}</select></label>
        <label>Tier<select value={coordinates.tier ?? ''} onChange={(event) => selectCoordinates({ tier: Number(event.target.value) })}>{tiers.map((tier) => <option key={tier} value={tier}>{tier}</option>)}</select></label>
        <label className="vessel-complete-overlay-control"><span>Overlay</span><select value={overlayMode} onChange={(event) => setOverlayMode(event.target.value)}>{VESSEL_OVERLAY_MODES.map((mode) => <option key={mode.value} value={mode.value}>{mode.label}</option>)}</select></label>
      </div>
    </div>
    <div className="vessel-phase1-selection"><span>Seleção sincronizada</span><strong>B{coordinates.bay} · R{coordinates.rowBay} · T{coordinates.tier}</strong><small>{selectedSlot?.codigoContainer || 'Slot livre'} · {selectedSlot ? legendValueForSlot(selectedSlot, legendMode, containerIndex) : '—'} · {VESSEL_OVERLAY_MODES.find((mode) => mode.value === overlayMode)?.label}</small></div>
    <LegendPanel mode={legendMode} setMode={setLegendMode} legend={legend} />
    <ValidationKey />
    <DragValidationBanner payload={activeBanner.payload} target={activeBanner.target} validation={activeBanner.validation} />
    <div className="vessel-workspace-layout"><main className="vessel-main-canvas">{viewMode === 'MULTI' ? <div className="multi-view-grid"><div>{renderView('PROFILE')}</div><div>{renderView('TOP')}</div><div>{renderView('SECTION')}</div><div>{renderView('TIER')}</div><div className="multi-view-scan">{renderView('SCAN')}</div></div> : renderView(viewMode)}</main><Inspector slot={selectedSlot} context={context} dragPayload={dragPayload} onClear={() => onSelectSlot(null)} /></div>
    <UnallocatedTray containers={unallocated} canEdit={canEdit && !busy} onDragStart={startDrag} onDragEnd={endDrag} />
    <StackWeightPanel slots={slots} summaries={stackSummaries} coordinates={coordinates} dragPayload={dragPayload} onSelect={selectSlot} />
    <CraneExecutionTimeline plan={plan} sequencing={sequencing} disabled={busy} selectedSlotId={selectedSlotId} onSelectSlot={selectSlot} overlayMode={overlayMode} onOverlayModeChange={setOverlayMode} showOverlayToolbar={false} />
    <TechnicalSummary stability={stability} restow={restow} sequencing={sequencing} onSelect={selectSlot} slots={slots} />
  </div>;
}
