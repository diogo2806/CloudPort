import { useEffect, useMemo } from 'react';
import { Operational2DCommandCenter } from '../components/Operational2DCommandCenter.jsx';
import { clearOperationalSelection, publishOperationalSelection, useOperationalSelection } from '../operational-selection.js';
import { OperationalSelectionPanel, OperationalWorkspaceManual } from './OperationalSelectionPanel.jsx';
import { VesselPlannerWorkspace as VesselPlannerWorkspaceBase } from './VesselPlannerWorkspaceBase.jsx';

function planSlots(plan) {
  if (Array.isArray(plan?.slots)) return plan.slots;
  if (Array.isArray(plan?.posicoes)) return plan.posicoes;
  return [];
}

function sameCoordinates(left, right) {
  return String(left?.bay ?? '') === String(right?.bay ?? '')
    && String(left?.rowBay ?? left?.row ?? '') === String(right?.rowBay ?? right?.row ?? '')
    && String(left?.tier ?? '') === String(right?.tier ?? '');
}

function selectionForSlot(slot, properties) {
  if (!slot) return null;
  const operations = Array.isArray(properties.sequencing?.sequencia) ? properties.sequencing.sequencia : [];
  const operation = operations.find((item) => sameCoordinates(item, slot));
  const unitCode = slot.codigoContainer ?? slot.containerCode ?? '';
  const equipment = operation?.guindasteId
    ? `Guindaste ${operation.guindasteId}`
    : slot.equipamento ?? slot.che ?? '';
  const workInstruction = operation?.workInstructionId
    ?? operation?.ordemId
    ?? operation?.id
    ?? slot.workInstructionId
    ?? slot.ordemId
    ?? '';
  const related = [];
  if (equipment) {
    related.push({
      domain: 'equipment',
      id: `equipment:${equipment}`,
      label: equipment,
      unitCode,
      equipment,
      workInstruction,
      location: { bay: slot.bay, row: slot.rowBay, tier: slot.tier },
      source: 'VesselPlannerWorkspace'
    });
  }
  const rail = slot.vagaoId ?? slot.codigoVagao ?? slot.linhaFerroviaria ?? operation?.vagaoId ?? '';
  if (rail) {
    related.push({
      domain: 'rail',
      id: `rail:${rail}`,
      label: `Ferrovia ${rail}`,
      unitCode,
      rail,
      workInstruction,
      location: { linha: slot.linhaFerroviaria ?? operation?.linhaFerroviaria, slot: slot.posicaoVagao },
      source: 'VesselPlannerWorkspace'
    });
  }
  return {
    domain: 'vessel',
    id: `vessel:${properties.plan?.id ?? 'plan'}:${slot.id ?? `${slot.bay}-${slot.rowBay}-${slot.tier}`}`,
    label: unitCode || `B${slot.bay} · R${slot.rowBay} · T${slot.tier}`,
    unitCode,
    slotId: slot.id,
    workInstruction,
    equipment,
    location: {
      navio: properties.bayPlan?.codigoNavio ?? properties.plan?.codigoNavio,
      bay: slot.bay,
      row: slot.rowBay,
      tier: slot.tier,
      slot: slot.id
    },
    origin: slot.origem ?? { porto: slot.portoEmbarque ?? slot.portoOrigem },
    destination: slot.destino ?? { porto: slot.portoDescarga },
    metadata: {
      planId: properties.plan?.id,
      bayPlanId: properties.bayPlan?.id,
      status: slot.status,
      reefer: Boolean(slot.reefer),
      perigoso: Boolean(slot.perigoso)
    },
    source: 'VesselPlannerWorkspace',
    related
  };
}

export function VesselPlannerWorkspace(properties) {
  const sharedSelection = useOperationalSelection();
  const slots = useMemo(() => planSlots(properties.plan), [properties.plan]);
  const externalSlotId = sharedSelection.active?.domain === 'vessel' ? sharedSelection.active.slotId : '';
  const externalSlot = useMemo(
    () => slots.find((slot) => String(slot.id) === String(externalSlotId)) ?? null,
    [externalSlotId, slots]
  );
  const effectiveSelectedSlotId = externalSlot?.id ?? properties.selectedSlotId;

  function selectSlot(slot) {
    if (slot) publishOperationalSelection(selectionForSlot(slot, properties));
    else clearOperationalSelection('vessel');
    properties.onSelectSlot(slot);
  }

  useEffect(() => {
    if (!properties.selectedSlotId) return;
    const slot = slots.find((item) => String(item.id) === String(properties.selectedSlotId));
    if (slot) publishOperationalSelection(selectionForSlot(slot, properties));
  }, [properties.selectedSlotId, properties.plan?.id, properties.sequencing]);

  useEffect(() => {
    if (!externalSlot || String(properties.selectedSlotId) === String(externalSlot.id)) return;
    properties.onSelectSlot(externalSlot);
  }, [externalSlot?.id]);

  return <div className="integrated-operational-workspace vessel-integrated-workspace">
    <OperationalWorkspaceManual scope="workspace gráfico integrado do Vessel Planner" />
    <OperationalSelectionPanel title="Composição operacional do Vessel Planner" />
    <Operational2DCommandCenter data={properties} scope="Vessel Planner · Navio, cais, Yard, Rail e equipamentos" onCommand={properties.onOperational2DCommand} />
    <VesselPlannerWorkspaceBase
      {...properties}
      selectedSlotId={effectiveSelectedSlotId}
      onSelectSlot={selectSlot}
    />
  </div>;
}
