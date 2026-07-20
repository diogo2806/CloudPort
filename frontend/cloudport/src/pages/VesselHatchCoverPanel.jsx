import { useEffect, useMemo, useRef, useState } from 'react';
import { formatError, request, sanitizeText } from '../api.js';
import { VesselOperationalTimeframes } from '../components/OperationalTimeframes.jsx';
import '../vessel-hatch-covers.css';

const OPERATIONS_BY_STATE = {
  FECHADA: ['ABRIR'],
  ABERTA: ['REMOVER', 'FECHAR'],
  REMOVIDA: ['POSICIONAR'],
  POSICIONADA: ['FECHAR']
};

const OPERATION_LABELS = {
  ABRIR: 'Abrir',
  REMOVER: 'Remover',
  POSICIONAR: 'Posicionar',
  FECHAR: 'Fechar'
};

function replaceCover(covers, updated) {
  return covers.map((cover) => String(cover.id) === String(updated.id) ? updated : cover);
}

function currentTask(cover) {
  const tasks = Array.isArray(cover?.tarefas) ? cover.tarefas : [];
  return [...tasks].reverse().find((task) => ['PLANEJADA', 'EM_EXECUCAO'].includes(task.status)) ?? null;
}

function taskDescription(task) {
  if (!task) return 'Nenhuma tarefa operacional pendente.';
  const dependencies = Array.isArray(task.dependenciasIds) && task.dependenciasIds.length
    ? ` · depende de ${task.dependenciasIds.map((id) => `#${id}`).join(', ')}`
    : '';
  return `${OPERATION_LABELS[task.tipo] ?? task.tipo} · ${task.status} · ${task.recurso}${dependencies}`;
}

export function VesselHatchCoverPanel({
  planId,
  canEdit,
  busy,
  selectedCoverCode = '',
  onSelectCover,
  onCoversChange
}) {
  const [covers, setCovers] = useState([]);
  const [resource, setResource] = useState('EQUIPE_TAMPA');
  const [loading, setLoading] = useState(false);
  const [command, setCommand] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const coversChangeRef = useRef(onCoversChange);

  useEffect(() => {
    coversChangeRef.current = onCoversChange;
  }, [onCoversChange]);

  useEffect(() => {
    coversChangeRef.current?.(covers);
  }, [covers]);

  async function load() {
    if (!planId) return;
    setLoading(true);
    setError('');
    try {
      const response = await request(`/api/vessel-planner/planos/${planId}/tampas-porao`);
      setCovers(Array.isArray(response) ? response : []);
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    setCovers([]);
    setSuccess('');
    load();
  }, [planId]);

  const summary = useMemo(() => covers.reduce((result, cover) => {
    result[cover.estado] = (result[cover.estado] ?? 0) + 1;
    return result;
  }, {}), [covers]);

  async function runCommand(key, action, message) {
    if (command || busy || !canEdit) return;
    setCommand(key);
    setError('');
    setSuccess('');
    try {
      const updated = await action();
      setCovers((current) => replaceCover(current, updated));
      setSuccess(message);
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setCommand('');
    }
  }

  function createTask(cover, type) {
    const body = {
      tipo: type,
      recurso: sanitizeText(resource) || 'EQUIPE_TAMPA',
      motivo: `Operação ${OPERATION_LABELS[type] ?? type} planejada no Vessel Planner.`,
      dependenciasIds: []
    };
    if (type === 'REMOVER') {
      body.posicaoDestinoTipo = 'AREA_SEGURA';
      body.posicaoDestinoReferencia = 'Área operacional segura';
    }
    return runCommand(
      `create-${cover.id}-${type}`,
      () => request(`/api/vessel-planner/planos/${planId}/tampas-porao/${cover.id}/tarefas`, { method: 'POST', body }),
      `Tarefa ${OPERATION_LABELS[type] ?? type} criada para ${cover.codigo}.`
    );
  }

  function startTask(cover, task) {
    return runCommand(
      `start-${task.id}`,
      () => request(`/api/vessel-planner/planos/${planId}/tampas-porao/tarefas/${task.id}/iniciar`, {
        method: 'POST',
        body: { motivo: `Execução iniciada para ${cover.codigo}.` }
      }),
      `Execução iniciada para ${cover.codigo}.`
    );
  }

  function confirmTask(cover, task) {
    return runCommand(
      `confirm-${task.id}`,
      () => request(`/api/vessel-planner/planos/${planId}/tampas-porao/tarefas/${task.id}/confirmar`, {
        method: 'POST',
        body: { motivo: `Operação confirmada para ${cover.codigo}.` }
      }),
      `Estado de ${cover.codigo} atualizado após confirmação.`
    );
  }

  function cancelTask(cover, task) {
    return runCommand(
      `cancel-${task.id}`,
      () => request(`/api/vessel-planner/planos/${planId}/tampas-porao/tarefas/${task.id}/cancelar`, {
        method: 'POST',
        body: { motivo: `Operação cancelada no Vessel Planner para ${cover.codigo}.` }
      }),
      `Tarefa de ${cover.codigo} cancelada.`
    );
  }

  return <>
    <VesselOperationalTimeframes planId={planId} />
    <section className="hatch-cover-panel" aria-label="Operação de tampas de porão">
      <header>
        <div><span className="view-kicker">Hatch covers</span><h3>Tampas de porão</h3><p>O estado persistido é sincronizado com os slots e bloqueia movimentos incompatíveis.</p></div>
        <div className="hatch-cover-controls">
          <label>Recurso<input value={resource} maxLength="120" onChange={(event) => setResource(event.target.value)} disabled={!canEdit || Boolean(command)} /></label>
          <button type="button" className="secondary" onClick={load} disabled={loading || Boolean(command)}>{loading ? 'Carregando...' : 'Atualizar'}</button>
        </div>
      </header>
      {error && <div className="hatch-cover-message error">{error}</div>}
      {success && <div className="hatch-cover-message success">{success}</div>}
      <div className="hatch-cover-summary">
        <span>Fechadas <strong>{summary.FECHADA ?? 0}</strong></span>
        <span>Abertas <strong>{summary.ABERTA ?? 0}</strong></span>
        <span>Removidas <strong>{summary.REMOVIDA ?? 0}</strong></span>
        <span>Posicionadas <strong>{summary.POSICIONADA ?? 0}</strong></span>
      </div>
      <div className="hatch-cover-grid">
        {covers.map((cover) => {
          const task = currentTask(cover);
          const operations = task ? [] : (OPERATIONS_BY_STATE[cover.estado] ?? []);
          const selected = String(selectedCoverCode).toUpperCase() === String(cover.codigo).toUpperCase();
          return <article key={cover.id} className={`hatch-cover-card state-${String(cover.estado).toLowerCase()}${selected ? ' selected-cover' : ''}`}>
            <div className="hatch-cover-heading">
              <button type="button" className="hatch-cover-select" onClick={() => onSelectCover?.(cover)} aria-pressed={selected}>
                <strong>{cover.codigo}</strong><small>{cover.posicaoAtual?.referencia || 'Posição não informada'}</small>
              </button>
              <span>{cover.estado}</span>
            </div>
            <p>{taskDescription(task)}</p>
            <div className="hatch-cover-actions">
              {task?.status === 'PLANEJADA' && <button type="button" onClick={() => startTask(cover, task)} disabled={!canEdit || busy || Boolean(command)}>{command === `start-${task.id}` ? 'Iniciando...' : 'Iniciar'}</button>}
              {task?.status === 'EM_EXECUCAO' && <button type="button" onClick={() => confirmTask(cover, task)} disabled={!canEdit || busy || Boolean(command)}>{command === `confirm-${task.id}` ? 'Confirmando...' : 'Confirmar'}</button>}
              {task && <button type="button" className="secondary" onClick={() => cancelTask(cover, task)} disabled={!canEdit || busy || Boolean(command)}>{command === `cancel-${task.id}` ? 'Cancelando...' : 'Cancelar'}</button>}
              {operations.map((type) => <button key={type} type="button" onClick={() => createTask(cover, type)} disabled={!canEdit || busy || Boolean(command)}>{command === `create-${cover.id}-${type}` ? 'Criando...' : OPERATION_LABELS[type]}</button>)}
            </div>
            <details><summary>Histórico ({cover.tarefas?.length ?? 0})</summary><ol>{(cover.tarefas ?? []).slice().reverse().map((item) => <li key={item.id}><strong>#{item.id} · {OPERATION_LABELS[item.tipo] ?? item.tipo}</strong><span>{item.status} · {item.operador}</span></li>)}</ol></details>
          </article>;
        })}
        {!loading && !covers.length && <div className="visual-empty">O perfil do navio não possui tampas de porão mapeadas.</div>}
      </div>
    </section>
  </>;
}
