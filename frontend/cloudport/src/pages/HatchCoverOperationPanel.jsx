import { useCallback, useEffect, useState } from 'react';
import { formatError } from '../api.js';
import { hatchCoverApi } from '../vessel-planner-hatch-api.js';
import '../vessel-planner-hatch.css';

function taskLabel(value) {
  return String(value ?? '').replaceAll('_', ' ');
}

export function HatchCoverOperationPanel({ plan, canEdit, disabled, onStateChange, onChanged }) {
  const [covers, setCovers] = useState([]);
  const [resources, setResources] = useState({});
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const publish = useCallback((value) => {
    const normalized = Array.isArray(value) ? value : [];
    setCovers(normalized);
    onStateChange?.(normalized);
    return normalized;
  }, [onStateChange]);

  const load = useCallback(async (synchronizeWhenEmpty = false) => {
    if (!plan?.id) return publish([]);
    let response = await hatchCoverApi.list(plan.id);
    if (synchronizeWhenEmpty && canEdit && Array.isArray(response) && !response.length) {
      response = await hatchCoverApi.synchronize(plan.id);
    }
    return publish(response);
  }, [canEdit, plan?.id, publish]);

  useEffect(() => {
    setError('');
    setSuccess('');
    load(true).catch((reason) => setError(formatError(reason)));
  }, [load]);

  async function command(name, action, message) {
    if (busy || disabled) return;
    setBusy(name);
    setError('');
    setSuccess('');
    try {
      await action();
      await load(false);
      setSuccess(message);
      onChanged?.();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy('');
    }
  }

  return <section className="hatch-cover-panel" aria-label="Operação persistida das tampas de porão">
    <header className="hatch-cover-panel-heading">
      <div><span className="view-kicker">Hatch covers</span><h3>Operação das tampas de porão</h3></div>
      <button
        type="button"
        className="secondary"
        disabled={!canEdit || disabled || Boolean(busy)}
        onClick={() => command('sync', () => hatchCoverApi.synchronize(plan.id), 'Tampas sincronizadas com a geometria e a sequência.')}
      >{busy === 'sync' ? 'Sincronizando...' : 'Sincronizar'}</button>
    </header>

    {error && <div className="hatch-operation-message error">{error}</div>}
    {success && <div className="hatch-operation-message success">{success}</div>}
    {!covers.length && <div className="visual-empty">Nenhuma tampa persistida para este plano.</div>}

    <div className="hatch-cover-grid">{covers.map((cover) => <article key={cover.id} className={cover.bloqueioAtivo ? 'active' : ''}>
      <div className="hatch-cover-heading">
        <div><strong>{cover.codigo}</strong><small>Bays {cover.bayInicial}–{cover.bayFinal}</small></div>
        <span className={`hatch-position ${String(cover.posicao).toLowerCase()}`}>{taskLabel(cover.posicao)}</span>
      </div>
      <div className="hatch-task-list">{(cover.tarefas ?? []).map((task) => <div key={task.id} className={`hatch-task status-${String(task.status).toLowerCase()}`}>
        <div>
          <strong>{task.ordemOperacional}. {taskLabel(task.tipo)}</strong>
          <small>{taskLabel(task.momentoSequencia)} movimento #{task.ordemMovimentoReferencia ?? 'N/I'} · {taskLabel(task.status)}</small>
        </div>
        {task.status === 'LIBERADA' && <div className="hatch-task-command">
          <input
            aria-label={`Recurso para ${task.tipo} ${cover.codigo}`}
            placeholder="Recurso/equipamento"
            value={resources[task.id] ?? ''}
            onChange={(event) => setResources((current) => ({ ...current, [task.id]: event.target.value }))}
          />
          <button
            type="button"
            disabled={!canEdit || disabled || Boolean(busy) || !(resources[task.id] ?? '').trim()}
            onClick={() => command(
              `start-${task.id}`,
              () => hatchCoverApi.startTask(plan.id, task.id, resources[task.id]),
              `${taskLabel(task.tipo)} iniciado.`
            )}
          >Iniciar</button>
        </div>}
        {task.status === 'EM_EXECUCAO' && <button
          type="button"
          disabled={!canEdit || disabled || Boolean(busy)}
          onClick={() => command(
            `confirm-${task.id}`,
            () => hatchCoverApi.confirmTask(plan.id, task.id),
            `${taskLabel(task.tipo)} confirmado e próxima dependência liberada.`
          )}
        >Confirmar</button>}
      </div>)}</div>
    </article>)}</div>
  </section>;
}
