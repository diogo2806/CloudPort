import { useCallback, useEffect, useState } from 'react';
import { api, formatError } from './api.js';
import { navioAdministrativeApi } from './operacoesAdministrativasNavioApi.js';
import VisitPlanEditor from './VisitPlanEditor.jsx';

export default function Ui10VisitPlanSection({ visit, onVisitPersisted }) {
  const [items, setItems] = useState([]);
  const [plan, setPlan] = useState(null);
  const [planHistory, setPlanHistory] = useState([]);
  const [validation, setValidation] = useState(null);
  const [busyKey, setBusyKey] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = useCallback(async (visitId, silent = false) => {
    if (!visitId) return;
    if (!silent) setLoading(true);
    try {
      const [persistedItems, persistedPlan, persistedEvents] = await Promise.all([
        api.listarItensVisita(visitId),
        api.obterPlanoEstiva(visitId),
        api.listarEventos(visitId)
      ]);
      setItems(persistedItems || []);
      setPlan(persistedPlan || null);
      setPlanHistory((persistedEvents || []).filter((event) => String(event?.tipoEvento ?? '').startsWith('PLANO_')));
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      if (!silent) setLoading(false);
    }
  }, []);

  useEffect(() => {
    setItems([]);
    setPlan(null);
    setPlanHistory([]);
    setValidation(null);
    setError('');
    setSuccess('');
    if (visit?.id) load(visit.id);
  }, [visit?.id, load]);

  async function run(key, operation, message) {
    setBusyKey(key);
    setError('');
    setSuccess('');
    try {
      const result = await operation();
      setSuccess(message);
      return result;
    } catch (reason) {
      setError(formatError(reason));
      return null;
    } finally {
      setBusyKey('');
    }
  }

  async function saveVisit(payload) {
    const updated = await run(
      'visit-update',
      () => api.atualizarVisita(visit.id, payload),
      'Visita atualizada com persistência confirmada.'
    );
    if (!updated) return null;
    onVisitPersisted?.(updated);
    await load(visit.id, true);
    return updated;
  }

  async function cancelVisit(motivo) {
    const updated = await run(
      'visit-cancel',
      () => navioAdministrativeApi.cancelarVisita(visit.id, motivo),
      'Visita cancelada com reservas, itens e ordens compensados.'
    );
    if (!updated) return null;
    onVisitPersisted?.(updated);
    await load(visit.id, true);
    return updated;
  }

  async function saveItem(itemId, payload) {
    const updated = await run(
      `item-update-${itemId}`,
      () => api.atualizarItemVisita(visit.id, itemId, payload),
      'Item da visita atualizado com persistência confirmada.'
    );
    if (!updated) return null;
    setItems((current) => current.map((item) => Number(item.id) === Number(updated.id) ? updated : item));
    await load(visit.id, true);
    return updated;
  }

  async function cancelItem(itemId, motivo) {
    const updated = await run(
      `item-cancel-${itemId}`,
      () => navioAdministrativeApi.cancelarItem(visit.id, itemId, motivo),
      'Item cancelado e impactos operacionais compensados.'
    );
    if (!updated) return null;
    setItems((current) => current.map((item) => Number(item.id) === Number(updated.id) ? updated : item));
    await load(visit.id, true);
    return updated;
  }

  async function validatePlan(planId) {
    const result = await run(
      'plan-validate',
      () => api.validarPlanoEstiva(visit.id, planId),
      'Validação do plano de estiva concluída.'
    );
    if (!result) return null;
    setValidation(result);
    await load(visit.id, true);
    return result;
  }

  async function publishPlan(planId, motivo) {
    const published = await run(
      'plan-publish',
      () => navioAdministrativeApi.publicarPlano(visit.id, planId, motivo),
      'Plano de estiva concluído e publicado com persistência confirmada.'
    );
    if (!published) return null;
    setPlan(published);
    setValidation(null);
    await load(visit.id, true);
    return published;
  }

  async function invalidatePlan(planId, motivo) {
    const invalidated = await run(
      'plan-invalidate',
      () => navioAdministrativeApi.invalidarPlano(visit.id, planId, motivo),
      'Plano de estiva invalidado. Uma nova versão pode ser criada para aprovação.'
    );
    if (!invalidated) return null;
    setPlan(invalidated);
    setValidation(null);
    await load(visit.id, true);
    return invalidated;
  }

  async function cancelPlan(planId, motivo) {
    const canceled = await run(
      'plan-cancel',
      () => navioAdministrativeApi.cancelarPlano(visit.id, planId, motivo),
      'Plano de estiva cancelado com persistência confirmada.'
    );
    if (!canceled) return null;
    setPlan(canceled);
    setValidation(null);
    await load(visit.id, true);
    return canceled;
  }

  async function createPlanVersion() {
    const created = await run(
      'plan-new-version',
      () => navioAdministrativeApi.criarNovaVersaoPlano(visit.id, plan?.posicoes || []),
      'Nova versão do plano criada a partir das posições anteriores.'
    );
    if (!created) return null;
    setPlan(created);
    setValidation(null);
    await load(visit.id, true);
    return created;
  }

  if (!visit) return null;

  return <>
    {error && <div className="message error" role="alert">{error}</div>}
    {success && <div className="message success" role="status">{success}</div>}
    <VisitPlanEditor
      visit={visit}
      items={items}
      plan={plan}
      planHistory={planHistory}
      validation={validation}
      busyKey={busyKey}
      onSaveVisit={saveVisit}
      onCancelVisit={cancelVisit}
      onSaveItem={saveItem}
      onCancelItem={cancelItem}
      onValidatePlan={validatePlan}
      onPublishPlan={publishPlan}
      onInvalidatePlan={invalidatePlan}
      onCancelPlan={cancelPlan}
      onCreatePlanVersion={createPlanVersion}
    />
    {loading && <div className="loading">Carregando visita, itens, plano e histórico de estiva...</div>}
  </>;
}
