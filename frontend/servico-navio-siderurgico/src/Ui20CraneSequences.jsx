import { useCallback, useEffect, useMemo, useState } from 'react';
import { craneApi } from './ui20-crane-api.js';
import { dateTime, statusClass } from './ui20-model.js';

const movementId = (allocation) => `crane-plan-${allocation.id}`;

export default function Ui20CraneSequences({ visitId, plan }) {
  const allocations = useMemo(() => (plan?.guindastes || []).filter((item) => item.id), [plan]);
  const [sequences, setSequences] = useState([]);
  const [history, setHistory] = useState({});
  const [busyKey, setBusyKey] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    if (!visitId) return;
    try {
      setSequences(await craneApi.listarSequencias(visitId) || []);
    } catch (reason) {
      setError(reason.message || 'Falha ao carregar sequências de guindaste.');
    }
  }, [visitId]);

  useEffect(() => { load(); }, [load, plan?.atualizadoEm]);

  async function initialize() {
    setBusyKey('initialize'); setError('');
    try {
      for (const allocation of allocations) {
        await craneApi.criarSequencia(visitId, allocation);
      }
      await load();
    } catch (reason) {
      setError(reason.message || 'Falha ao criar sequências.');
    } finally {
      setBusyKey('');
    }
  }

  async function transition(allocation, action) {
    const key = `${movementId(allocation)}-${action}`;
    let reason = null;
    if (['pause', 'cancel'].includes(action)) {
      reason = typeof window?.prompt === 'function'
        ? String(window.prompt(action === 'pause' ? 'Motivo da pausa:' : 'Motivo do cancelamento:', '') ?? '').trim()
        : 'Ação operacional';
      if (!reason) return;
    }
    setBusyKey(key); setError('');
    try {
      await craneApi.transicionarSequencia(allocation, action, reason);
      await load();
      if (history[movementId(allocation)]) await toggleHistory(allocation, true);
    } catch (reasonError) {
      setError(reasonError.message || 'Falha ao atualizar a sequência.');
    } finally {
      setBusyKey('');
    }
  }

  async function toggleHistory(allocation, forceReload = false) {
    const id = movementId(allocation);
    if (history[id] && !forceReload) {
      setHistory((current) => ({ ...current, [id]: null }));
      return;
    }
    setBusyKey(`${id}-history`); setError('');
    try {
      const values = await craneApi.listarHistoricoSequencia(allocation);
      setHistory((current) => ({ ...current, [id]: values || [] }));
    } catch (reason) {
      setError(reason.message || 'Falha ao carregar o histórico.');
    } finally {
      setBusyKey('');
    }
  }

  const byMovement = new Map(sequences.map((sequence) => [sequence.movementId, sequence]));
  const missing = allocations.filter((allocation) => !byMovement.has(movementId(allocation))).length;

  return <section className="panel crane-sequences">
    <div className="section-head"><div><span className="eyebrow">BUS1150</span><h2>Execução das viagens de guindaste</h2></div><div className="actions"><span>{allocations.length} movimento(s)</span><button className="secondary" disabled={!missing || busyKey === 'initialize'} onClick={initialize}>{busyKey === 'initialize' ? 'Sincronizando...' : `Sincronizar ${missing || ''}`}</button><button className="secondary" onClick={load}>Atualizar</button></div></div>
    {error && <div className="message error">{error}</div>}
    {!allocations.length && <p className="empty">Publique ou salve o plano de guindastes para gerar movimentos executáveis.</p>}
    <div className="plan-allocation-list">{allocations.map((allocation) => {
      const id = movementId(allocation);
      const sequence = byMovement.get(id);
      const audit = history[id];
      return <article className="plan-allocation" key={id}>
        <div className="plan-allocation-title"><div><strong>{allocation.codigoGuindaste}</strong><span>Movimento {id} · Work queue {allocation.workQueueId}</span></div><span className={statusClass(sequence?.status || 'PLANNED')}>{sequence?.status || 'NÃO SINCRONIZADO'}</span></div>
        <div className="plan-allocation-grid">
          <label>Início planejado<strong>{dateTime(allocation.inicioPlanejado)}</strong></label>
          <label>Início real<strong>{dateTime(sequence?.startedAt)}</strong></label>
          <label>Fim real<strong>{dateTime(sequence?.finishedAt)}</strong></label>
          <label>Operador<strong>{sequence?.operatorId || '—'}</strong></label>
          <label>Versão<strong>{sequence?.version ?? '—'}</strong></label>
          <label>Notas<strong>{sequence?.notes || allocation.observacao || '—'}</strong></label>
        </div>
        <div className="actions">
          <button className="small" disabled={!sequence || !['PLANNED', 'PAUSED'].includes(sequence.status) || busyKey} onClick={() => transition(allocation, 'start')}>Iniciar</button>
          <button className="small secondary" disabled={!sequence || sequence.status !== 'STARTED' || busyKey} onClick={() => transition(allocation, 'pause')}>Pausar</button>
          <button className="small" disabled={!sequence || sequence.status !== 'STARTED' || busyKey} onClick={() => transition(allocation, 'finish')}>Finalizar</button>
          <button className="small danger" disabled={!sequence || ['FINISHED', 'CANCELLED'].includes(sequence.status) || busyKey} onClick={() => transition(allocation, 'cancel')}>Cancelar</button>
          <button className="small secondary" disabled={!sequence || busyKey === `${id}-history`} onClick={() => toggleHistory(allocation)}>{audit ? 'Ocultar histórico' : 'Histórico'}</button>
        </div>
        {audit && <div className="list scroll">{audit.map((entry, index) => <article key={`${entry.occurredAt}-${index}`}><div><strong>{entry.type}</strong><time>{dateTime(entry.occurredAt)}</time></div><p>{entry.statusBefore || 'NOVO'} → {entry.statusAfter || '—'}{entry.reason ? ` · ${entry.reason}` : ''}</p><small>{entry.operatorId}</small></article>)}</div>}
      </article>;
    })}</div>
  </section>;
}
