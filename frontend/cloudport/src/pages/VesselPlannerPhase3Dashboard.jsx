import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, request } from '../api.js';
import {
  buildContainerIndex,
  buildStackSummaries,
  buildViolationIndex,
  normalizeSlots,
  stackPositionKey,
  uniqueCoordinates
} from '../vessel-planner-model.js';
import {
  OVERLAY_RISK,
  aggregateOverlayForSlots,
  buildImdgIndex,
  buildOverlayIndex
} from '../vessel-planner-phase3.js';
import '../vessel-planner-phase3.css';
import '../vessel-planner-phase3-dashboard.css';
import { VesselOverlayToolbar, VesselPlannerPhase3Panels } from './VesselPlannerPhase3Panels.jsx';

function displayWeight(value) {
  const number = Number(value);
  if (!Number.isFinite(number) || number <= 0) return '—';
  return `${new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 }).format(number / 1000)} t`;
}

function riskClass(risk) {
  return `overlay-${String(risk ?? OVERLAY_RISK.NONE).toLowerCase()}`;
}

function representativeSlot(slots) {
  const occupied = slots.filter((slot) => slot.codigoContainer);
  return [...(occupied.length ? occupied : slots)].sort((left, right) => Number(right.tier) - Number(left.tier))[0] ?? null;
}

function OverlayMap({ slots, overlayIndex, stackSummaries, selectedSlotId, onSelectSlot }) {
  const bays = uniqueCoordinates(slots, 'bay');
  const rows = uniqueCoordinates(slots, 'rowBay');
  const stacks = useMemo(() => slots.reduce((index, slot) => {
    const key = stackPositionKey(slot);
    if (!index[key]) index[key] = [];
    index[key].push(slot);
    return index;
  }, {}), [slots]);

  return <section className="phase3-panel vessel-overlay-map" aria-label="Mapa gráfico das camadas técnicas">
    <header><div><span className="view-kicker">Overlay map</span><h3>Mapa técnico por stack</h3></div><small>O contorno representa o maior risco dos slots da stack na camada ativa.</small></header>
    <div className="phase3-map-scroll"><div className="phase3-map-grid" style={{ gridTemplateColumns: `58px repeat(${Math.max(1, bays.length)}, minmax(112px, 1fr))` }}>
      <div className="phase3-map-corner">Row</div>
      {bays.map((bay) => <div key={`bay-${bay}`} className="phase3-map-axis">B{bay}</div>)}
      {rows.map((row) => <div key={`row-${row}`} className="phase3-map-row">
        <div className="phase3-map-axis">R{row}</div>
        {bays.map((bay) => {
          const stackSlots = stacks[`${bay}:${row}`] ?? [];
          const representative = representativeSlot(stackSlots);
          const overlay = aggregateOverlayForSlots(stackSlots, overlayIndex);
          const summary = stackSummaries[`${bay}:${row}`];
          const selected = stackSlots.some((slot) => String(slot.id) === String(selectedSlotId));
          return stackSlots.length ? <button
            key={`${bay}:${row}`}
            type="button"
            className={`phase3-map-cell phase3-overlay ${riskClass(overlay.risk)}${selected ? ' selected' : ''}`}
            onClick={() => representative && onSelectSlot(representative)}
            title={overlay.details.join('\n') || `Bay ${bay}, row ${row}`}
          >
            {overlay.shortLabel && <span className="phase3-overlay-badge">{overlay.shortLabel}</span>}
            <strong>{summary?.occupied ?? 0}/{summary?.capacity ?? stackSlots.length}</strong>
            <span>{displayWeight(summary?.weightKg)}</span>
            <small>{overlay.risk === 'NONE' ? 'Sem risco' : overlay.risk}</small>
          </button> : <div key={`${bay}:${row}`} className="phase3-map-cell absent">—</div>;
        })}
      </div>)}
    </div></div>
  </section>;
}

function OverlaySection({ slots, overlayIndex, selectedSlotId, onSelectSlot }) {
  const selected = slots.find((slot) => String(slot.id) === String(selectedSlotId)) ?? slots[0];
  const bay = selected?.bay;
  const section = slots.filter((slot) => slot.bay === bay);
  const rows = uniqueCoordinates(section, 'rowBay');
  const tiers = uniqueCoordinates(section, 'tier').sort((left, right) => right - left);
  const index = Object.fromEntries(section.map((slot) => [`${slot.tier}:${slot.rowBay}`, slot]));

  if (!section.length) return null;
  return <section className="phase3-panel vessel-overlay-section" aria-label="Seção técnica do bay selecionado">
    <header><div><span className="view-kicker">Technical section</span><h3>Seção técnica do bay {bay}</h3></div><small>Detalhamento exato por row e tier.</small></header>
    <div className="phase3-section-scroll"><div className="phase3-section-grid" style={{ gridTemplateColumns: `58px repeat(${Math.max(1, rows.length)}, minmax(104px, 1fr))` }}>
      <div className="phase3-map-corner">Tier</div>
      {rows.map((row) => <div key={`row-${row}`} className="phase3-map-axis">R{row}</div>)}
      {tiers.map((tier) => <div key={`tier-${tier}`} className="phase3-section-row">
        <div className="phase3-map-axis">T{tier}</div>
        {rows.map((row) => {
          const slot = index[`${tier}:${row}`];
          if (!slot) return <div key={`${tier}:${row}`} className="phase3-section-cell absent">—</div>;
          const overlay = overlayIndex[String(slot.id)] ?? { risk: 'NONE', details: [], shortLabel: '' };
          return <button
            key={slot.id}
            type="button"
            className={`phase3-section-cell phase3-overlay ${riskClass(overlay.risk)}${String(slot.id) === String(selectedSlotId) ? ' selected' : ''}`}
            onClick={() => onSelectSlot(slot)}
            title={overlay.details.join('\n') || `${slot.codigoContainer || 'Slot livre'}`}
          >
            {overlay.shortLabel && <span className="phase3-overlay-badge">{overlay.shortLabel}</span>}
            <strong>{slot.codigoContainer || 'Livre'}</strong>
            <span>B{slot.bay} R{slot.rowBay} T{slot.tier}</span>
            <small>{overlay.risk === 'NONE' ? 'Sem risco' : overlay.risk}</small>
          </button>;
        })}
      </div>)}
    </div></div>
  </section>;
}

export function VesselPlannerPhase3Dashboard({ plan, sequencing, disabled }) {
  const [stability, setStability] = useState(plan?.estabilidade ?? null);
  const [restow, setRestow] = useState(null);
  const [overlayMode, setOverlayMode] = useState('COMBINED');
  const [selectedSlotId, setSelectedSlotId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const slots = useMemo(() => normalizeSlots(plan), [plan]);
  const containerIndex = useMemo(() => buildContainerIndex(plan?.containers), [plan]);
  const stackSummaries = useMemo(() => buildStackSummaries(slots), [slots]);
  const violationIndex = useMemo(() => buildViolationIndex(stability), [stability]);
  const imdgIndex = useMemo(() => buildImdgIndex(slots, containerIndex, violationIndex), [slots, containerIndex, violationIndex]);
  const overlayIndex = useMemo(() => buildOverlayIndex(slots, overlayMode, {
    stackSummaries,
    violationIndex,
    imdgIndex
  }), [slots, overlayMode, stackSummaries, violationIndex, imdgIndex]);

  const loadTechnicalData = useCallback(async () => {
    if (!plan?.id) return;
    setLoading(true);
    setError('');
    try {
      const [stabilityResponse, restowResponse] = await Promise.all([
        request(`/api/vessel-planner/planos/${plan.id}/estabilidade`),
        request(`/api/vessel-planner/planos/${plan.id}/restow`)
      ]);
      setStability(stabilityResponse);
      setRestow(restowResponse);
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }, [plan?.id]);

  useEffect(() => {
    setStability(plan?.estabilidade ?? null);
    setRestow(null);
    setSelectedSlotId('');
    loadTechnicalData();
  }, [plan?.id, loadTechnicalData]);

  useEffect(() => {
    if (!selectedSlotId && slots.length) setSelectedSlotId(String(slots.find((slot) => slot.codigoContainer)?.id ?? slots[0].id));
  }, [slots, selectedSlotId]);

  if (!plan?.id || !slots.length) return null;

  return <section className="vessel-phase3-dashboard" aria-label="Fase 3 do planejamento gráfico">
    <div className="phase3-dashboard-heading">
      <div><span className="view-kicker">Vessel Planner · Fase 3</span><h2>Segregação, sequência, restow e integridade técnica</h2><p>Camadas gráficas calculadas a partir do plano e dos resultados técnicos persistidos pelo backend.</p></div>
      <button type="button" className="secondary" onClick={loadTechnicalData} disabled={loading || disabled}>{loading ? 'Atualizando...' : 'Atualizar análises'}</button>
    </div>
    {error && <div className="phase3-dashboard-message error">{error}</div>}
    <VesselOverlayToolbar mode={overlayMode} setMode={setOverlayMode} overlayIndex={overlayIndex} />
    <div className="phase3-visual-grid">
      <OverlayMap slots={slots} overlayIndex={overlayIndex} stackSummaries={stackSummaries} selectedSlotId={selectedSlotId} onSelectSlot={(slot) => setSelectedSlotId(String(slot.id))} />
      <OverlaySection slots={slots} overlayIndex={overlayIndex} selectedSlotId={selectedSlotId} onSelectSlot={(slot) => setSelectedSlotId(String(slot.id))} />
    </div>
    <VesselPlannerPhase3Panels
      slots={slots}
      imdgIndex={imdgIndex}
      overlayIndex={overlayIndex}
      selectedSlotId={selectedSlotId}
      sequencing={sequencing}
      restow={restow}
      onSelectSlot={(slot) => setSelectedSlotId(String(slot.id))}
    />
  </section>;
}
