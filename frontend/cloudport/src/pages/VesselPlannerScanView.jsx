import { useMemo } from 'react';
import { buildVesselScanBays } from '../vessel-planner-complete.js';
import { findSynchronizedSlot } from '../vessel-planner-phase1.js';

function displayWeight(value) {
  const number = Number(value);
  if (!Number.isFinite(number) || number <= 0) return '—';
  return `${new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 }).format(number / 1000)} t`;
}

export function VesselPlannerScanView({ slots, context, selectedSlotId, onSelect, renderSlot }) {
  const bays = useMemo(() => buildVesselScanBays(slots, context), [slots, context]);

  return <section className="vessel-view-card vessel-scan-view" aria-label="Scan operacional do navio">
    <header>
      <div><span className="view-kicker">Scan view</span><h3>Scan operacional</h3></div>
      <small>Resumo navegável de ocupação, riscos, tampas, restow e sequência por bay</small>
    </header>
    <div className="vessel-scan-bays">
      {bays.map((bay) => {
        const representative = findSynchronizedSlot(bay.slots, context.coordinates, { preferOccupied: true });
        const active = bay.slots.some((slot) => String(slot.id) === String(selectedSlotId));
        return <article key={bay.bay} className={`vessel-scan-bay${active ? ' active' : ''}`}>
          <button type="button" className="vessel-scan-bay-heading" onClick={() => representative && onSelect(representative)}>
            <span>Bay</span><strong>{bay.bay}</strong><small>{bay.occupied}/{bay.capacity} ocupado(s)</small>
          </button>
          <div className="vessel-scan-metrics">
            <span><small>Peso</small><strong>{displayWeight(bay.weightKg)}</strong></span>
            <span><small>Livres</small><strong>{bay.free}</strong></span>
            <span><small>Restrições</small><strong>{bay.restricted}</strong></span>
            <span><small>Alertas</small><strong>{bay.warnings}</strong></span>
            <span><small>Restows</small><strong>{bay.restows}</strong></span>
            <span><small>Guindastes</small><strong>{bay.craneOperations}</strong></span>
          </div>
          <div className="vessel-scan-hatches">
            {bay.hatchCovers.length ? bay.hatchCovers.map((cover) => <span key={cover}>HC {cover}</span>) : <span>Convés aberto</span>}
          </div>
          <div className="vessel-scan-slots">
            {bay.slots.map((slot) => renderSlot(slot))}
          </div>
        </article>;
      })}
      {!bays.length && <div className="visual-empty">Não há geometria para o scan operacional.</div>}
    </div>
  </section>;
}
