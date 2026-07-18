import { useEffect, useMemo, useState } from 'react';
import { formatError, hasAnyRole, readSession } from '../api.js';
import { vesselPlannerReconciliacaoApi } from '../vessel-planner-reconciliacao-api.js';

const DECISOES = [
  ['ACEITAR_BAPLIE', 'Aceitar BAPLIE'],
  ['ACEITAR_PLANO', 'Aceitar plano aprovado'],
  ['ACEITAR_INVENTARIO', 'Aceitar inventário'],
  ['ACEITAR_EXECUCAO', 'Aceitar execução'],
  ['CORRECAO_EXTERNA', 'Correção externa'],
  ['ACEITAR_DIVERGENCIA', 'Aceitar divergência']
];

function texto(valor) {
  return valor === undefined || valor === null || valor === '' ? '—' : String(valor);
}

function dataHora(valor) {
  if (!valor) return '—';
  const data = new Date(valor);
  return Number.isNaN(data.getTime()) ? valor : data.toLocaleString('pt-BR');
}

export function VesselPlannerReconciliationPanel({ planId, planStatus, canCommand, busy }) {
  const [reconciliacao, setReconciliacao] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selectedId, setSelectedId] = useState('');
  const [decisao, setDecisao] = useState('CORRECAO_EXTERNA');
  const [motivo, setMotivo] = useState('');
  const canReconcile = canCommand || hasAnyRole(readSession(), 'ADMIN_PORTO', 'PLANEJADOR');

  const divergencias = useMemo(
    () => Array.isArray(reconciliacao?.divergencias) ? reconciliacao.divergencias : [],
    [reconciliacao]
  );
  const abertas = useMemo(() => divergencias.filter((item) => item.status === 'ABERTA'), [divergencias]);
  const selecionada = useMemo(
    () => abertas.find((item) => String(item.id) === String(selectedId)) ?? abertas[0] ?? null,
    [abertas, selectedId]
  );

  async function carregarAtual() {
    if (!planId) return;
    setLoading(true);
    setError('');
    try {
      const atual = await vesselPlannerReconciliacaoApi.buscarAtual(planId);
      setReconciliacao(atual);
    } catch (reason) {
      if (reason?.status === 404) {
        setReconciliacao(null);
      } else {
        setError(formatError(reason));
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    setSelectedId('');
    setMotivo('');
    carregarAtual();
  }, [planId, planStatus]);

  async function executarReconciliacao() {
    if (!planId || !canReconcile || busy || loading) return;
    setLoading(true);
    setError('');
    try {
      const resultado = await vesselPlannerReconciliacaoApi.reconciliar(planId);
      setReconciliacao(resultado);
      setSelectedId('');
      setMotivo('');
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }

  async function resolver(event) {
    event.preventDefault();
    if (!selecionada || !motivo.trim() || !canReconcile || busy || loading) return;
    setLoading(true);
    setError('');
    try {
      const resultado = await vesselPlannerReconciliacaoApi.resolver(
        planId,
        reconciliacao.id,
        selecionada.id,
        decisao,
        motivo
      );
      setReconciliacao(resultado);
      setSelectedId('');
      setMotivo('');
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }

  return <section className="vessel-view-card" aria-label="Reconciliação BAPLIE e execução">
    <header>
      <div><span className="view-kicker">DATA1180</span><h3>Reconciliação BAPLIE e execução</h3></div>
      <button type="button" onClick={executarReconciliacao} disabled={!canReconcile || busy || loading}>
        {loading ? 'Processando...' : reconciliacao ? 'Reconciliar novamente' : 'Executar reconciliação'}
      </button>
    </header>

    {error && <div className="message error" role="alert">{error}</div>}

    {!reconciliacao && !loading && <div className="visual-empty">
      Execute a reconciliação antes de validar, publicar ou concluir o plano.
    </div>}

    {reconciliacao && <>
      <div className="metrics-grid">
        <article><span>Status</span><strong>{texto(reconciliacao.status)}</strong></article>
        <article><span>Unidades</span><strong>{reconciliacao.totalUnidades ?? 0}</strong></article>
        <article><span>Divergências</span><strong>{reconciliacao.totalDivergencias ?? 0}</strong></article>
        <article><span>Críticas abertas</span><strong>{reconciliacao.totalCriticasAbertas ?? 0}</strong></article>
      </div>

      {reconciliacao.bloqueiaOperacao && <div className="message error" role="alert">
        A validação, publicação e conclusão estão bloqueadas até a resolução das divergências críticas.
      </div>}

      <div className="planner-detail-card">
        <div className="card-meta">
          <span>Reconciliação #{reconciliacao.id}</span>
          <span>{dataHora(reconciliacao.executadaEm)}</span>
          <span>Solicitante: {texto(reconciliacao.solicitante)}</span>
        </div>
      </div>

      <div className="planner-split">
        <div className="reconciliation-list">
          {divergencias.map((item) => <button
            key={item.id}
            type="button"
            className={String(selecionada?.id) === String(item.id) ? 'planner-detail-card selected' : 'planner-detail-card'}
            onClick={() => setSelectedId(String(item.id))}
          >
            <div className="card-meta"><strong>{item.codigoContainer}</strong><span>{item.severidade}</span><span>{item.status}</span></div>
            <p>{item.tipo} · {item.campo}</p>
            <small>{item.fonteReferencia}: {texto(item.valorReferencia)} → {item.fonteDivergente}: {texto(item.valorDivergente)}</small>
          </button>)}
          {!divergencias.length && <div className="visual-empty">Nenhuma divergência identificada.</div>}
        </div>

        <aside className="planner-detail-card">
          {selecionada ? <>
            <span className="view-kicker">Divergência selecionada</span>
            <h3>{selecionada.codigoContainer} · {selecionada.campo}</h3>
            <dl>
              <div><dt>Referência</dt><dd>{selecionada.fonteReferencia}: {texto(selecionada.valorReferencia)}</dd></div>
              <div><dt>Divergente</dt><dd>{selecionada.fonteDivergente}: {texto(selecionada.valorDivergente)}</dd></div>
              <div><dt>Detectada</dt><dd>{dataHora(selecionada.detectadaEm)}</dd></div>
            </dl>
            <form onSubmit={resolver}>
              <label className="field"><span>Decisão auditável</span><select value={decisao} onChange={(event) => setDecisao(event.target.value)} disabled={!canReconcile || loading}>{DECISOES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
              <label className="field"><span>Motivo</span><textarea value={motivo} onChange={(event) => setMotivo(event.target.value)} maxLength="1000" rows="4" disabled={!canReconcile || loading} /></label>
              <button disabled={!canReconcile || loading || busy || !motivo.trim()}>Resolver sem alterar fontes</button>
            </form>
          </> : <div className="visual-empty">Não há divergências abertas.</div>}
        </aside>
      </div>
    </>}
  </section>;
}
