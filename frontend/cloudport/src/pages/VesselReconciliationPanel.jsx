import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, hasAnyRole, readSession, request, sanitizeText } from '../api.js';

const DECISIONS = [
  ['ACEITAR_BAPLIE', 'Aceitar BAPLIE'],
  ['ACEITAR_PLANO', 'Aceitar plano'],
  ['ACEITAR_INVENTARIO', 'Aceitar inventário'],
  ['ACEITAR_EXECUCAO', 'Aceitar execução'],
  ['CORRIGIDO_NA_ORIGEM', 'Corrigido na origem'],
  ['IGNORAR_JUSTIFICADO', 'Ignorar com justificativa']
];

const SEVERITY_ORDER = { CRITICA: 3, AVISO: 2, INFORMATIVA: 1 };

function label(value) {
  return String(value ?? '—').replaceAll('_', ' ');
}

function displayDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('pt-BR');
}

function SourceValue({ label: sourceLabel, value }) {
  return <div>
    <dt>{sourceLabel}</dt>
    <dd>{value || 'Não informado'}</dd>
  </div>;
}

export function VesselReconciliationPanel({ plan, disabled }) {
  const [reconciliation, setReconciliation] = useState(null);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [selectedId, setSelectedId] = useState('');
  const [decision, setDecision] = useState('CORRIGIDO_NA_ORIGEM');
  const [justification, setJustification] = useState('');
  const canCommand = hasAnyRole(readSession(), 'ADMIN_PORTO', 'PLANEJADOR');

  const load = useCallback(async () => {
    if (!plan?.id) {
      setReconciliation(null);
      return null;
    }
    setLoading(true);
    setError('');
    try {
      const response = await request(`/api/vessel-planner/planos/${plan.id}/reconciliacao`);
      setReconciliation(response);
      return response;
    } catch (reason) {
      setError(formatError(reason));
      return null;
    } finally {
      setLoading(false);
    }
  }, [plan?.id]);

  useEffect(() => {
    load();
  }, [load]);

  const divergences = useMemo(() => {
    const items = Array.isArray(reconciliation?.divergencias)
      ? reconciliation.divergencias
      : [];
    return [...items].sort((left, right) => {
      const statusOrder = Number(left.status !== 'ABERTA') - Number(right.status !== 'ABERTA');
      if (statusOrder !== 0) return statusOrder;
      const severityOrder = (SEVERITY_ORDER[right.severidade] ?? 0) - (SEVERITY_ORDER[left.severidade] ?? 0);
      if (severityOrder !== 0) return severityOrder;
      return String(left.codigoContainer ?? '').localeCompare(String(right.codigoContainer ?? ''));
    });
  }, [reconciliation]);

  const selected = divergences.find((item) => String(item.id) === String(selectedId)) ?? null;
  const commandDisabled = disabled || loading || Boolean(busy) || !canCommand;

  async function reconcile() {
    if (!plan?.id || commandDisabled) return;
    setBusy('reconcile');
    setError('');
    setSuccess('');
    try {
      const response = await request(`/api/vessel-planner/planos/${plan.id}/reconciliacao`, {
        method: 'POST'
      });
      setReconciliation(response);
      setSuccess('BAPLIE, plano, inventário e execução foram reconciliados.');
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy('');
    }
  }

  async function resolve(event) {
    event.preventDefault();
    if (!selected?.id || selected.status !== 'ABERTA' || commandDisabled) return;
    const normalizedJustification = sanitizeText(justification);
    if (!normalizedJustification) {
      setError('Informe a justificativa da resolução.');
      return;
    }

    setBusy(`resolve-${selected.id}`);
    setError('');
    setSuccess('');
    try {
      const response = await request(
        `/api/vessel-planner/planos/${plan.id}/reconciliacao/${selected.id}/resolver`,
        {
          method: 'POST',
          body: {
            decisao: decision,
            justificativa: normalizedJustification
          }
        }
      );
      setReconciliation(response);
      setJustification('');
      setSuccess(`Divergência do contêiner ${selected.codigoContainer} resolvida.`);
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy('');
    }
  }

  if (!plan?.id) return null;

  return <section className="vessel-reconciliation-panel" aria-label="Reconciliação BAPLIE, plano, inventário e execução">
    <header className="vessel-reconciliation-heading">
      <div>
        <span className="view-kicker">DATA1180 · fontes operacionais</span>
        <h3>Reconciliação do plano</h3>
        <small>Última execução: {displayDateTime(reconciliation?.reconciliadoEm)}</small>
      </div>
      <div className="actions">
        <button type="button" className="secondary" onClick={load} disabled={loading || Boolean(busy)}>
          {loading ? 'Carregando...' : 'Recarregar'}
        </button>
        <button type="button" onClick={reconcile} disabled={commandDisabled}>
          {busy === 'reconcile' ? 'Reconciliando...' : 'Reconciliar agora'}
        </button>
      </div>
    </header>

    {error && <div className="crane-execution-message error">{error}</div>}
    {success && <div className="crane-execution-message success">{success}</div>}
    {!canCommand && <div className="vessel-reconciliation-readonly">Modo somente leitura para resolução de divergências.</div>}

    <div className="vessel-reconciliation-summary">
      <article><span>Total</span><strong>{reconciliation?.totalDivergencias ?? 0}</strong></article>
      <article><span>Abertas</span><strong>{reconciliation?.abertas ?? 0}</strong></article>
      <article className={Number(reconciliation?.criticasAbertas) > 0 ? 'critical' : ''}>
        <span>Críticas abertas</span><strong>{reconciliation?.criticasAbertas ?? 0}</strong>
      </article>
      <article><span>Resolvidas</span><strong>{reconciliation?.resolvidas ?? 0}</strong></article>
    </div>

    {Number(reconciliation?.criticasAbertas) > 0 && <div className="vessel-reconciliation-block">
      A publicação ou conclusão permanece bloqueada até a resolução das divergências críticas.
    </div>}

    {!loading && divergences.length === 0
      ? <div className="visual-empty">As quatro fontes estão consistentes e não há divergências registradas.</div>
      : <div className="vessel-reconciliation-layout">
        <div className="vessel-reconciliation-queue">
          {divergences.map((item) => <button
            key={item.id}
            type="button"
            className={[
              'vessel-reconciliation-item',
              `severity-${String(item.severidade).toLowerCase()}`,
              item.status === 'RESOLVIDA' ? 'resolved' : '',
              String(item.id) === String(selectedId) ? 'selected' : ''
            ].filter(Boolean).join(' ')}
            onClick={() => {
              setSelectedId(String(item.id));
              setJustification('');
            }}
          >
            <span><strong>{item.codigoContainer}</strong><i>{label(item.severidade)}</i></span>
            <b>{label(item.tipo)}</b>
            <small>{label(item.status)}{item.resolvidoPor ? ` · ${item.resolvidoPor}` : ''}</small>
          </button>)}
        </div>

        <aside className="vessel-reconciliation-inspector">
          {!selected
            ? <div className="visual-empty">Selecione uma divergência para comparar as fontes.</div>
            : <>
              <div className="vessel-reconciliation-inspector-heading">
                <div><span className="view-kicker">Contêiner</span><h4>{selected.codigoContainer}</h4></div>
                <span className={`reconciliation-severity severity-${String(selected.severidade).toLowerCase()}`}>{label(selected.severidade)}</span>
              </div>
              <p>{label(selected.tipo)} · {label(selected.status)}</p>
              <dl className="vessel-reconciliation-sources">
                <SourceValue label="BAPLIE" value={selected.valorBaplie} />
                <SourceValue label="Plano" value={selected.valorPlano} />
                <SourceValue label="Inventário" value={selected.valorInventario} />
                <SourceValue label="Execução" value={selected.valorExecucao} />
              </dl>

              {selected.status === 'ABERTA'
                ? <form className="vessel-reconciliation-resolution" onSubmit={resolve}>
                  <label>
                    <span>Decisão</span>
                    <select value={decision} onChange={(event) => setDecision(event.target.value)} disabled={commandDisabled}>
                      {DECISIONS.map(([value, text]) => <option key={value} value={value}>{text}</option>)}
                    </select>
                  </label>
                  <label>
                    <span>Justificativa</span>
                    <textarea
                      rows="3"
                      maxLength="1000"
                      value={justification}
                      onChange={(event) => setJustification(event.target.value)}
                      disabled={commandDisabled}
                    />
                  </label>
                  <button type="submit" disabled={commandDisabled || !justification.trim()}>
                    {busy === `resolve-${selected.id}` ? 'Resolvendo...' : 'Registrar resolução'}
                  </button>
                </form>
                : <div className="vessel-reconciliation-decision">
                  <strong>{label(selected.decisao)}</strong>
                  <span>{selected.justificativa || 'Sem justificativa registrada.'}</span>
                  <small>{selected.resolvidoPor || 'SISTEMA'} · {displayDateTime(selected.resolvidoEm)}</small>
                </div>}
            </>}
        </aside>
      </div>}
  </section>;
}
