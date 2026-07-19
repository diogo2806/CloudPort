import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, formatError } from '../api.js';
import { DataTable, EmptyState, JsonDetails, Loading, Message, MetricCard, PageHeader, Section, StatusBadge } from '../components.jsx';
import {
  EDI_MESSAGE_TYPES,
  EDI_PROCESSING_STATUSES,
  EDI_STATUS_LABELS,
  buildEdiListQuery,
  normalizeEdiFilters,
  normalizeEdiPage,
  summarizeEdiProcessings,
  validateReprocessReason
} from '../ediMonitor.js';

function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('pt-BR');
}

function EdiDetailPanel({ processingId, refreshToken, onReprocessed }) {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!processingId) { setDetail(null); return undefined; }
    let active = true;
    setLoading(true); setError(''); setSuccess('');
    api.obterProcessamentoEdi(processingId)
      .then((payload) => { if (active) setDetail(payload); })
      .catch((reasonError) => { if (active) { setDetail(null); setError(formatError(reasonError)); } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [processingId, refreshToken]);

  async function reprocess(event) {
    event.preventDefault();
    if (busy || !processingId) return;
    setBusy(true); setError(''); setSuccess('');
    try {
      const motivo = validateReprocessReason(reason);
      const created = await api.reprocessarProcessamentoEdi(processingId, motivo);
      setReason('');
      setSuccess(`Reprocessamento aceito. Novo processamento #${created?.id ?? '—'} registrado.`);
      onReprocessed?.();
    } catch (reasonError) {
      setError(formatError(reasonError));
    } finally { setBusy(false); }
  }

  if (!processingId) return <EmptyState title="Selecione um processamento" description="Escolha uma mensagem na tabela para inspecionar recepção, tentativas, erro, correlação e efeito produzido." />;
  if (loading) return <Loading label="Carregando processamento..." />;

  return <>
    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>
    {detail && <div className="card-list">
      <article className="content-card"><strong>Recepção</strong><div className="card-meta"><span>Tipo: {detail.tipoMensagem ?? '—'}</span><span>Recebido em: {formatDateTime(detail.criadoEm)}</span><span>Referência: {detail.referenciaMensagem || '—'}</span><span>Interchange: {detail.identificadorInterchange || '—'}</span><span>Mensagem: {detail.identificadorMensagem || '—'}</span><span>Chave de idempotência: {detail.chaveIdempotencia || '—'}</span></div></article>
      <article className="content-card"><strong>Tentativas e situação</strong><div className="card-meta"><span>Status: <StatusBadge value={EDI_STATUS_LABELS[detail.status] ?? detail.status} /></span><span>Tentativa: {detail.tentativa ?? '—'}</span><span>Próxima tentativa: {formatDateTime(detail.proximaTentativaEm)}</span><span>Atualizado em: {formatDateTime(detail.atualizadoEm)}</span></div></article>
      <article className="content-card"><strong>Erro e correlação</strong><div className="card-meta"><span>Motivo de rejeição: {detail.motivoRejeicao || 'Nenhum'}</span><span>Correlation ID: {detail.correlationId || '—'}</span><span>Reprocessamento de: {detail.reprocessamentoDeId ? `#${detail.reprocessamentoDeId}` : '—'}</span><span>Motivo de reprocessamento: {detail.motivoReprocessamento || '—'}</span><span>Usuário do reprocessamento: {detail.usuarioReprocessamento || '—'}</span></div></article>
      <article className="content-card"><strong>Efeito produzido</strong><div className="card-meta"><span>Bay plan gerado: {detail.bayPlanId ? `#${detail.bayPlanId}` : 'Nenhum'}</span><span>Navio: {detail.codigoNavio || '—'}</span><span>Viagem: {detail.codigoViagem || '—'}</span></div></article>
    </div>}
    <form className="inline-form" onSubmit={reprocess}>
      <label className="field"><span>Motivo do reprocessamento</span><input value={reason} onChange={(event) => setReason(event.target.value)} maxLength={500} placeholder="Motivo operacional auditável" /></label>
      <button disabled={busy || !reason}>{busy ? 'Reprocessando...' : 'Reprocessar mensagem'}</button>
    </form>
    <JsonDetails value={detail} title="Payload completo do processamento" />
  </>;
}

export function EdiMonitorPage() {
  const [filters, setFilters] = useState(() => normalizeEdiFilters({}));
  const [page, setPage] = useState(() => normalizeEdiPage(null));
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedId, setSelectedId] = useState(null);
  const [refreshToken, setRefreshToken] = useState(0);

  const load = useCallback(async (nextFilters) => {
    setLoading(true); setError('');
    try {
      const payload = await api.listarProcessamentosEdi(buildEdiListQuery(nextFilters));
      setPage(normalizeEdiPage(payload));
    } catch (reason) {
      setError(formatError(reason));
      setPage(normalizeEdiPage(null));
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(filters); }, [filters, load]);

  const summary = useMemo(() => summarizeEdiProcessings(page.rows), [page.rows]);

  function changeFilter(partial) {
    setFilters((current) => normalizeEdiFilters({ ...current, ...partial, pagina: partial.pagina ?? 0 }));
  }

  const columns = [
    { key: 'id', label: 'ID' },
    { key: 'tipoMensagem', label: 'Tipo' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.statusDescricao} /> },
    { key: 'codigoNavio', label: 'Navio', render: (row) => row.codigoNavio || '—' },
    { key: 'codigoViagem', label: 'Viagem', render: (row) => row.codigoViagem || '—' },
    { key: 'tentativa', label: 'Tentativa', render: (row) => row.tentativa ?? '—' },
    { key: 'correlationId', label: 'Correlation ID', render: (row) => row.correlationId || '—' },
    { key: 'motivoRejeicao', label: 'Motivo do erro', render: (row) => row.motivoRejeicao || '—' },
    { key: 'bayPlanId', label: 'Efeito', render: (row) => row.bayPlanId ? `Bay plan #${row.bayPlanId}` : '—' },
    { key: 'criadoEm', label: 'Recebido em', render: (row) => formatDateTime(row.criadoEm) },
    { key: 'acoes', label: 'Ações', render: (row) => <button type="button" className="secondary" onClick={() => setSelectedId(row.id)}>Inspecionar</button> }
  ];

  return <>
    <PageHeader
      eyebrow="Integrações"
      title="Painel EDI"
      description="Recepção, tentativas, erros, correlação e efeito produzido por mensagem EDI (BAPLIE, COPRAR, COARRI e VERMAS)."
      actions={<button className="secondary" onClick={() => { load(filters); setRefreshToken((value) => value + 1); }}>Atualizar</button>}
    />
    <Message type="error">{error}</Message>
    <div className="metrics-grid">
      <MetricCard label="Mensagens na página" value={summary.total} detail={`Total geral: ${page.totalElementos}`} />
      <MetricCard label="Concluídas" value={summary.concluidos} />
      <MetricCard label="Rejeitadas" value={summary.rejeitados} />
      <MetricCard label="Em quarentena" value={summary.quarentena} />
      <MetricCard label="Aguardando reprocessamento" value={summary.aguardandoReprocessamento} />
    </div>
    <Section title="Fila de processamentos" description="Mensagens EDI recebidas pelo terminal, mais recentes primeiro." actions={
      <div className="actions">
        <label className="compact-field">Tipo<select value={filters.tipo} onChange={(event) => changeFilter({ tipo: event.target.value })}><option value="">Todos</option>{EDI_MESSAGE_TYPES.map((tipo) => <option key={tipo} value={tipo}>{tipo}</option>)}</select></label>
        <label className="compact-field">Status<select value={filters.status} onChange={(event) => changeFilter({ status: event.target.value })}><option value="">Todos</option>{EDI_PROCESSING_STATUSES.map((status) => <option key={status} value={status}>{EDI_STATUS_LABELS[status]}</option>)}</select></label>
        <label className="compact-field">Tamanho<select value={filters.tamanho} onChange={(event) => changeFilter({ tamanho: Number(event.target.value) })}><option value="25">25</option><option value="50">50</option><option value="100">100</option></select></label>
      </div>
    }>
      {loading ? <Loading label="Carregando processamentos EDI..." /> : page.rows.length
        ? <DataTable rows={page.rows} columns={columns} gridId="integracoes-painel-edi" exportFileName="painel-edi" rowKey={(row) => row.id} />
        : <EmptyState title="Nenhum processamento EDI encontrado" description="Ajuste os filtros ou envie uma mensagem EDI para o terminal." />}
      <div className="actions">
        <button type="button" className="secondary" disabled={loading || page.primeira} onClick={() => changeFilter({ pagina: Math.max(0, filters.pagina - 1) })}>Página anterior</button>
        <span>Página {page.pagina + 1} de {Math.max(1, page.totalPaginas)}</span>
        <button type="button" className="secondary" disabled={loading || page.ultima} onClick={() => changeFilter({ pagina: filters.pagina + 1 })}>Próxima página</button>
      </div>
    </Section>
    <Section title="Inspeção da mensagem" description="Detalhe auditável do processamento selecionado, com reprocessamento motivado.">
      <EdiDetailPanel processingId={selectedId} refreshToken={refreshToken} onReprocessed={() => { load(filters); setRefreshToken((value) => value + 1); }} />
    </Section>
  </>;
}
