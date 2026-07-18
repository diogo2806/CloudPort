import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, request, sanitizeText } from '../api.js';

function displayDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('pt-BR');
}

function displayQuantity(value) {
  const number = Number(value);
  return Number.isFinite(number)
    ? new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 3 }).format(number)
    : '—';
}

function statusLabel(value) {
  return String(value ?? 'NÃO INICIADA').replaceAll('_', ' ');
}

function localDateTimeValue(value) {
  return String(value ?? '').replace(/Z$/, '').slice(0, 19);
}

export function CraneExecutionTimeline({ plan, sequencing, disabled }) {
  const [execution, setExecution] = useState(null);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadExecution = useCallback(async () => {
    if (!plan?.id) {
      setExecution(null);
      return null;
    }
    setLoading(true);
    setError('');
    try {
      const response = await request(`/api/vessel-planner/planos/${plan.id}/execucao-guindastes`);
      setExecution(response);
      return response;
    } catch (reason) {
      if (reason?.status === 404) {
        setExecution(null);
        return null;
      }
      setError(formatError(reason));
      return null;
    } finally {
      setLoading(false);
    }
  }, [plan?.id]);

  useEffect(() => {
    loadExecution();
  }, [loadExecution]);

  const movements = useMemo(() => Array.isArray(execution?.movimentos)
    ? [...execution.movimentos].sort((left, right) => Number(left.ordemPlanejada) - Number(right.ordemPlanejada))
    : [], [execution]);

  async function command(name, path, body, message) {
    if (busy || disabled) return null;
    setBusy(name);
    setError('');
    setSuccess('');
    try {
      const response = await request(path, { method: 'POST', body });
      setExecution(response);
      setSuccess(message);
      return response;
    } catch (reason) {
      setError(formatError(reason));
      if (reason?.status === 409) await loadExecution();
      return null;
    } finally {
      setBusy('');
    }
  }

  async function createExecution() {
    const numGuindastes = Number(sequencing?.numGuindastes ?? 2);
    await command(
      'create',
      `/api/vessel-planner/planos/${plan.id}/execucao-guindastes`,
      { numGuindastes, duracaoMovimentoMinutos: 5 },
      'Execução da sequência persistida.'
    );
  }

  async function startMovement(movement) {
    await command(
      `start-${movement.id}`,
      `/api/vessel-planner/execucoes-guindastes/${execution.id}/movimentos/${movement.id}/iniciar`,
      { versao: movement.versao },
      `Movimento #${movement.ordemPlanejada} iniciado.`
    );
  }

  async function completeMovement(movement) {
    const informed = globalThis.prompt?.(
      'Quantidade realizada:',
      String(movement.quantidadePlanejada ?? 1)
    );
    if (informed === null || informed === undefined) return;
    const quantity = Number(String(informed).replace(',', '.'));
    if (!Number.isFinite(quantity) || quantity < 0) {
      setError('Informe uma quantidade realizada válida.');
      return;
    }
    await command(
      `complete-${movement.id}`,
      `/api/vessel-planner/execucoes-guindastes/${execution.id}/movimentos/${movement.id}/concluir`,
      { versao: movement.versao, quantidadeRealizada: quantity },
      `Movimento #${movement.ordemPlanejada} concluído.`
    );
  }

  async function failMovement(movement) {
    const exception = sanitizeText(globalThis.prompt?.('Descreva a exceção operacional:', '') ?? '');
    if (!exception) return;
    const informed = globalThis.prompt?.('Quantidade realizada antes da falha:', '0');
    if (informed === null || informed === undefined) return;
    const quantity = Number(String(informed).replace(',', '.'));
    if (!Number.isFinite(quantity) || quantity < 0) {
      setError('Informe uma quantidade realizada válida.');
      return;
    }
    await command(
      `fail-${movement.id}`,
      `/api/vessel-planner/execucoes-guindastes/${execution.id}/movimentos/${movement.id}/falhar`,
      { versao: movement.versao, excecao: exception, quantidadeRealizada: quantity },
      `Falha registrada no movimento #${movement.ordemPlanejada}.`
    );
  }

  async function replanMovement(movement) {
    const crane = Number(globalThis.prompt?.('Novo guindaste:', String(movement.guindasteId)));
    if (!Number.isInteger(crane) || crane < 1) return;
    const order = Number(globalThis.prompt?.('Nova ordem:', String(movement.ordemPlanejada)));
    if (!Number.isInteger(order) || order < 1) return;
    const start = globalThis.prompt?.('Nova janela de início (AAAA-MM-DDTHH:mm:ss):', localDateTimeValue(movement.janelaInicioPlanejada));
    if (!start) return;
    const end = globalThis.prompt?.('Nova janela de fim (AAAA-MM-DDTHH:mm:ss):', localDateTimeValue(movement.janelaFimPlanejada));
    if (!end) return;
    const reason = sanitizeText(globalThis.prompt?.('Motivo do replanejamento:', '') ?? '');
    if (!reason) return;
    await command(
      `replan-${movement.id}`,
      `/api/vessel-planner/execucoes-guindastes/${execution.id}/movimentos/${movement.id}/replanejar`,
      {
        versao: movement.versao,
        guindasteId: crane,
        ordemPlanejada: order,
        janelaInicio: start,
        janelaFim: end,
        motivo: reason
      },
      `Movimento #${movement.ordemPlanejada} replanejado.`
    );
  }

  async function reconcile() {
    const observation = sanitizeText(globalThis.prompt?.('Observação da reconciliação:', '') ?? '');
    await command(
      'reconcile',
      `/api/vessel-planner/execucoes-guindastes/${execution.id}/reconciliar`,
      { versao: execution.versao, observacao: observation || null },
      'Execução reconciliada com o realizado.'
    );
  }

  if (!plan?.id) return null;

  return <section className="crane-execution-panel" aria-label="Execução persistida dos guindastes">
    <header className="crane-execution-heading">
      <div><span className="view-kicker">Planejado × realizado</span><h3>Execução dos guindastes</h3></div>
      <div className="actions">
        <button type="button" className="secondary" onClick={loadExecution} disabled={loading || Boolean(busy) || disabled}>{loading ? 'Carregando...' : 'Recarregar'}</button>
        {!execution && <button type="button" onClick={createExecution} disabled={loading || Boolean(busy) || disabled}>{busy === 'create' ? 'Persistindo...' : 'Persistir execução'}</button>}
      </div>
    </header>

    {error && <div className="crane-execution-message error">{error}</div>}
    {success && <div className="crane-execution-message success">{success}</div>}

    {!execution && !loading
      ? <div className="visual-empty">A sequência calculada ainda não possui execução persistida.</div>
      : execution && <>
        <div className="crane-execution-summary">
          <article><span>Status</span><strong>{statusLabel(execution.status)}</strong></article>
          <article><span>Progresso</span><strong>{displayQuantity(execution.percentualConcluido)}%</strong></article>
          <article><span>Planejado</span><strong>{displayQuantity(execution.quantidadePlanejada)}</strong></article>
          <article><span>Realizado</span><strong>{displayQuantity(execution.quantidadeRealizada)}</strong></article>
          <article><span>Divergência</span><strong>{displayQuantity(execution.divergenciaQuantidade)}</strong></article>
          <article><span>Exceções</span><strong>{execution.movimentosComFalha ?? 0}</strong></article>
        </div>
        <progress className="crane-execution-progress" max="100" value={Number(execution.percentualConcluido ?? 0)}>{execution.percentualConcluido}%</progress>
        <div className="crane-execution-timeline">
          {movements.map((movement) => {
            const planned = movement.status === 'PLANEJADO' || movement.status === 'REPLANEJADO';
            const running = movement.status === 'EM_EXECUCAO';
            return <article key={movement.id} className={`crane-execution-movement status-${String(movement.status).toLowerCase()}`}>
              <div className="crane-movement-order"><span>Q{movement.guindasteId}</span><strong>#{movement.ordemPlanejada}</strong></div>
              <div className="crane-movement-content">
                <div className="crane-movement-title"><strong>{movement.codigoContainer}</strong><span>{statusLabel(movement.status)}{movement.atrasado ? ' · ATRASADO' : ''}</span></div>
                <p>B{movement.bay} · R{movement.rowBay} · T{movement.tier} · {movement.tipoOperacao}</p>
                <small>Janela: {displayDateTime(movement.janelaInicioPlanejada)} até {displayDateTime(movement.janelaFimPlanejada)}</small>
                <small>Realizado: {displayQuantity(movement.quantidadeRealizada)} de {displayQuantity(movement.quantidadePlanejada)}</small>
                {movement.iniciadoEm && <small>Início: {displayDateTime(movement.iniciadoEm)} · {movement.iniciadoPor || 'SISTEMA'}</small>}
                {movement.concluidoEm && <small>Fim: {displayDateTime(movement.concluidoEm)} · {movement.concluidoPor || 'SISTEMA'}</small>}
                {movement.motivoReplanejamento && <p className="crane-movement-note">Replanejamento: {movement.motivoReplanejamento}</p>}
                {movement.excecao && <p className="crane-movement-exception">Exceção: {movement.excecao}</p>}
              </div>
              <div className="crane-movement-actions">
                {planned && <button type="button" onClick={() => startMovement(movement)} disabled={Boolean(busy) || disabled}>Iniciar</button>}
                {planned && <button type="button" className="secondary" onClick={() => replanMovement(movement)} disabled={Boolean(busy) || disabled}>Replanejar</button>}
                {running && <button type="button" onClick={() => completeMovement(movement)} disabled={Boolean(busy) || disabled}>Concluir</button>}
                {running && <button type="button" className="warning" onClick={() => failMovement(movement)} disabled={Boolean(busy) || disabled}>Falha</button>}
              </div>
            </article>;
          })}
        </div>
        {execution.status === 'AGUARDANDO_RECONCILIACAO' && <div className="crane-reconcile-action"><button type="button" onClick={reconcile} disabled={Boolean(busy) || disabled}>{busy === 'reconcile' ? 'Reconciliando...' : 'Reconciliar execução'}</button></div>}
        {execution.reconciliadoEm && <div className="crane-reconciled"><strong>Reconciliada em {displayDateTime(execution.reconciliadoEm)}</strong><span>{execution.reconciliadoPor || 'SISTEMA'}{execution.observacaoReconciliacao ? ` · ${execution.observacaoReconciliacao}` : ''}</span></div>}
      </>}
  </section>;
}
