import { useCallback, useEffect, useState } from 'react';
import { api, formatError } from './api.js';
import VisitPlanEditor from './VisitPlanEditor.jsx';

export default function Ui10VisitPlanSection({ visit, onVisitPersisted }) {
  const [items, setItems] = useState([]);
  const [plan, setPlan] = useState(null);
  const [validation, setValidation] = useState(null);
  const [busyKey, setBusyKey] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = useCallback(async (visitId, silent = false) => {
    if (!visitId) return;
    if (!silent) setLoading(true);
    try {
      const [persistedItems, persistedPlan] = await Promise.all([
        api.listarItensVisita(visitId),
        api.obterPlanoEstiva(visitId)
      ]);
      setItems(persistedItems || []);
      setPlan(persistedPlan || null);
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      if (!silent) setLoading(false);
    }
  }, []);

  useEffect(() => {
    setItems([]);
    setPlan(null);
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

  async function concludePlan(planId) {
    const concluded = await run(
      'plan-conclude',
      () => api.concluirPlanoEstiva(visit.id, planId),
      'Plano de estiva concluído com persistência confirmada.'
    );
    if (!concluded) return null;
    setPlan(concluded);
    setValidation(null);
    await load(visit.id, true);
    return concluded;
  }

  if (!visit) return null;

  return <>
    {error && <div className="message error" role="alert">{error}</div>}
    {success && <div className="message success" role="status">{success}</div>}
    <VisitPlanEditor
      visit={visit}
      items={items}
      plan={plan}
      validation={validation}
      busyKey={busyKey}
      onSaveVisit={saveVisit}
      onSaveItem={saveItem}
      onValidatePlan={validatePlan}
      onConcludePlan={concludePlan}
    />
    {loading && <div className="loading">Carregando visita, itens e plano de estiva...</div>}
  </>;
}
