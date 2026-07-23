import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, formatError, request } from '../api.js';
import { DataTable, EmptyState, JsonDetails, Loading, Message, MetricCard, PageHeader, Section, StatusBadge } from '../components.jsx';
import {
  EDI_MESSAGE_TYPES,
  EDI_PROCESSING_STATUSES,
  EDI_STATUS_LABELS,
  buildEdiListQuery,
  normalizeEdiFilters,
  normalizeEdiPage,
  normalizeEdiProcessing,
  summarizeEdiProcessings,
  validateReprocessReason
} from '../ediMonitor.js';

function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('pt-BR');
}

function EdiDetailPanel({ processingId, refreshToken, onChanged }) {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState('');

  useEffect(() => {
    if (!processingId) { setDetail(null); return undefined; }
    let active = true;
    setLoading(true); setError(''); setSuccess('');
    api.obterProcessamentoEdi(processingId)
      .then((payload) => { if (active) setDetail(normalizeEdiProcessing(payload)); })
      .catch((reasonError) => { if (active) { setDetail(null); setError(formatError(reasonError)); } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [processingId, refreshToken]);

  async function execute(action) {
    if (busy || !processingId) return;
    setBusy(action); setError(''); setSuccess('');
    try {
      const motivo = validateReprocessReason(reason);
      const path = action === 'reprocessar'
        ? `/api/edi/processamentos/${processingId}/reprocessar`
        : action === 'quarentena'
          ? `/api/edi/processamentos/${processingId}/quarentena`
          : `/api/edi/processamentos/${processingId}/cancelar`;
      await request(path, { method: 'POST', body: { motivo } });
      setReason('');
      setSuccess(action === 'reprocessar' ? 'Mensagem recolocada na fila de processamento.' : action === 'quarentena' ? 'Mensagem colocada em quarentena.' : 'Mensagem cancelada.');
      onChanged?.();
    } catch (reasonError) {
      setError(formatError(reasonError));
    } finally { setBusy(''); }
  }

  if (!processingId) return <EmptyState title="Selecione um processamento" description="Escolha uma mensagem para inspecionar histórico, tentativas, correlação e ações controladas." />;
  if (loading) return <Loading label="Carregando processamento..." />;

  return <>
    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>
    {detail && <div className="card-list">
      <article className="content-card"><strong>Envelope e recepção</strong><div className="card-meta"><span>Tipo: {detail.tipoMensagem || '—'}</span><span>Recebido em: {formatDateTime(detail.criadoEm)}</span><span>Referência: {detail.referenciaMensagem || '—'}</span><span>Interchange: {detail.identificadorInterchange || '—'}</span><span>Mensagem: {detail.identificadorMensagem || '—'}</span><span>Chave de idempotência: {detail.chaveIdempotencia || '—'}</span></div></article>
      <article className="content-card"><strong>Situação</strong><div className="card-meta"><span>Status: <StatusBadge value={detail.statusDescricao} /></span><span>Tentativa: {detail.tentativa ?? '—'}</span><span>Próxima tentativa: {formatDateTime(detail.proximaTentativaEm)}</span><span>Atualizado em: {formatDateTime(detail.atualizadoEm)}</span></div></article>
      <article className="content-card"><strong>Auditoria e vínculo operacional</strong><div className="card-meta"><span>Motivo: {detail.motivoRejeicao || detail.motivoReprocessamento || 'Nenhum'}</span><span>Correlation ID: {detail.correlationId || '—'}</span><span>Usuário: {detail.usuarioReprocessamento || '—'}</span><span>Bay plan: {detail.bayPlanId ? `#${detail.bayPlanId}` : 'Nenhum'}</span><span>Navio: {detail.codigoNavio || '—'}</span><span>Viagem: {detail.codigoViagem || '—'}</span></div></article>
    </div>}
    <div className="inline-form">
      <label className="field"><span>Justificativa operacional</span><input value={reason} onChange={(event) => setReason(event.target.value)} maxLength={500} placeholder="Obrigatória para todas as ações" /></label>
      <div className="actions">
        <button type="button" disabled={Boolean(busy) || !reason || !detail?.reprocessavel} onClick={() => execute('reprocessar')}>{busy === 'reprocessar' ? 'Reprocessando...' : 'Reprocessar'}</button>
        <button type="button" className="secondary" disabled={Boolean(busy) || !reason || !detail?.quarentenavel} onClick={() => execute('quarentena')}>{busy === 'quarentena' ? 'Movendo...' : 'Colocar em quarentena'}</button>
        <button type="button" className="danger" disabled={Boolean(busy) || !reason || !detail?.cancelavel} onClick={() => execute('cancelar')}>{busy === 'cancelar' ? 'Cancelando...' : 'Cancelar mensagem'}</button>
      </div>
    </div>
    <JsonDetails value={detail} title="Conteúdo seguro do processamento" />
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
    try { setPage(normalizeEdiPage(await api.listarProcessamentosEdi(buildEdiListQuery(nextFilters)))); }
    catch (reason) { setError(formatError(reason)); setPage(normalizeEdiPage(null)); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { load(filters); }, [filters, load]);
  const summary = useMemo(() => summarizeEdiProcessings(page.rows), [page.rows]);
  function changeFilter(partial) { setFilters((current) => normalizeEdiFilters({ ...current, ...partial, pagina: partial.pagina ?? 0 })); }
  function refresh() { load(filters); setRefreshToken((value) => value + 1); }

  const columns = [
    { key: 'id', label: 'ID' },
    { key: 'tipoMensagem', label: 'Tipo' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.statusDescricao} /> },
    { key: 'referenciaMensagem', label: 'Referência', render: (row) => row.referenciaMensagem || '—' },
    { key: 'codigoNavio', label: 'Navio', render: (row) => row.codigoNavio || '—' },
    { key: 'codigoViagem', label: 'Viagem', render: (row) => row.codigoViagem || '—' },
    { key: 'tentativa', label: 'Tentativa', render: (row) => row.tentativa ?? '—' },
    { key: 'correlationId', label: 'Correlation ID', render: (row) => row.correlationId || '—' },
    { key: 'motivoRejeicao', label: 'Motivo', render: (row) => row.motivoRejeicao || row.motivoReprocessamento || '—' },
    { key: 'criadoEm', label: 'Recebido em', render: (row) => formatDateTime(row.criadoEm) },
    { key: 'acoes', label: 'Ações', render: (row) => <button type="button" className="secondary" onClick={() => setSelectedId(row.id)}>Inspecionar</button> }
  ];

  return <>
    <PageHeader eyebrow="Integrações" title="Central de integrações e EDI" description="Rastreabilidade, idempotência, tentativas, vínculos operacionais e reprocessamento controlado." actions={<button className="secondary" onClick={refresh}>Atualizar</button>} />
    <Message type="error">{error}</Message>
    <div className="metrics-grid">
      <MetricCard label="Mensagens na página" value={summary.total} detail={`Total geral: ${page.totalElementos}`} />
      <MetricCard label="Concluídas" value={summary.concluidos} />
      <MetricCard label="Rejeitadas" value={summary.rejeitados} />
      <MetricCard label="Em quarentena" value={summary.quarentena} />
      <MetricCard label="Canceladas" value={summary.cancelados} />
    </div>
    <Section title="Fila de mensagens" description="Mensagens recebidas pelo terminal, mais recentes primeiro." actions={<div className="actions"><label className="compact-field">Tipo<select value={filters.tipo} onChange={(event) => changeFilter({ tipo: event.target.value })}><option value="">Todos</option>{EDI_MESSAGE_TYPES.map((tipo) => <option key={tipo} value={tipo}>{tipo}</option>)}</select></label><label className="compact-field">Status<select value={filters.status} onChange={(event) => changeFilter({ status: event.target.value })}><option value="">Todos</option>{EDI_PROCESSING_STATUSES.map((status) => <option key={status} value={status}>{EDI_STATUS_LABELS[status]}</option>)}</select></label></div>}>
      {loading ? <Loading label="Carregando integrações..." /> : page.rows.length ? <DataTable rows={page.rows} columns={columns} gridId="integracoes-painel-edi" exportFileName="central-integracoes" rowKey={(row) => row.id} /> : <EmptyState title="Nenhuma mensagem encontrada" description="Ajuste os filtros ou envie uma mensagem ao terminal." />}
      <div className="actions"><button type="button" className="secondary" disabled={loading || page.primeira} onClick={() => changeFilter({ pagina: Math.max(0, filters.pagina - 1) })}>Página anterior</button><span>Página {page.pagina + 1} de {Math.max(1, page.totalPaginas)}</span><button type="button" className="secondary" disabled={loading || page.ultima} onClick={() => changeFilter({ pagina: filters.pagina + 1 })}>Próxima página</button></div>
    </Section>
    <Section title="Inspeção e controle" description="Ações exigem justificativa, usuário e correlation ID para auditoria."><EdiDetailPanel processingId={selectedId} refreshToken={refreshToken} onChanged={refresh} /></Section>
  </>;
}
