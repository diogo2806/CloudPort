import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, hasAnyRole } from '../api.js';
import { DataTable, EmptyState, Loading, Message, MetricCard, PageHeader, Section, StatusBadge } from '../components.jsx';
import { railApi, railOrderTransitions } from '../railApi.js';
import './yard/YardPages.css';

function displayValue(value) {
  if (value === undefined || value === null || value === '') return '—';
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)) {
    const date = new Date(value);
    if (!Number.isNaN(date.getTime())) return date.toLocaleString('pt-BR');
  }
  return String(value);
}

function operationLabel(value) {
  return value === 'DESCARGA_TREM' ? 'Descarga' : value === 'CARGA_TREM' ? 'Carga' : displayValue(value);
}

export function RailWorkListPage({ session }) {
  const [days, setDays] = useState(30);
  const [visits, setVisits] = useState([]);
  const [selectedVisitId, setSelectedVisitId] = useState('');
  const [visit, setVisit] = useState(null);
  const [orders, setOrders] = useState([]);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [search, setSearch] = useState('');
  const [visitsLoading, setVisitsLoading] = useState(true);
  const [workLoading, setWorkLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const canOperate = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO');

  const loadVisits = useCallback(async () => {
    setVisitsLoading(true);
    setError('');
    try {
      const response = await railApi.listarVisitas(days);
      const loadedVisits = Array.isArray(response) ? response : [];
      setVisits(loadedVisits);
      setSelectedVisitId((current) => {
        if (loadedVisits.some((item) => String(item.id) === String(current))) return current;
        const preferred = loadedVisits.find((item) => item.statusVisita === 'CHEGOU') ?? loadedVisits[0];
        return preferred?.id ? String(preferred.id) : '';
      });
    } catch (reason) {
      setVisits([]);
      setSelectedVisitId('');
      setError(formatError(reason));
    } finally {
      setVisitsLoading(false);
    }
  }, [days]);

  const loadWork = useCallback(async () => {
    if (!selectedVisitId) {
      setVisit(null);
      setOrders([]);
      setSelectedOrder(null);
      return;
    }
    setWorkLoading(true);
    setError('');
    try {
      const [visitResponse, ordersResponse] = await Promise.all([
        railApi.consultarVisita(selectedVisitId),
        railApi.listarOrdens(selectedVisitId, statusFilter)
      ]);
      const loadedOrders = Array.isArray(ordersResponse) ? ordersResponse : [];
      setVisit(visitResponse ?? null);
      setOrders(loadedOrders);
      setSelectedOrder((current) => loadedOrders.find((item) => item.id === current?.id) ?? loadedOrders[0] ?? null);
    } catch (reason) {
      setVisit(null);
      setOrders([]);
      setSelectedOrder(null);
      setError(formatError(reason));
    } finally {
      setWorkLoading(false);
    }
  }, [selectedVisitId, statusFilter]);

  useEffect(() => { loadVisits(); }, [loadVisits]);
  useEffect(() => { loadWork(); }, [loadWork]);

  const filteredOrders = useMemo(() => {
    const term = search.trim().toLocaleLowerCase('pt-BR');
    if (!term) return orders;
    return orders.filter((order) => `${order.codigoConteiner} ${order.tipoMovimentacao} ${order.statusMovimentacao}`
      .toLocaleLowerCase('pt-BR')
      .includes(term));
  }, [orders, search]);

  const manifestRows = useMemo(() => [
    ...(visit?.listaDescarga ?? []).map((item) => ({ ...item, operacao: 'Descarga' })),
    ...(visit?.listaCarga ?? []).map((item) => ({ ...item, operacao: 'Carga' }))
  ], [visit]);

  const transitions = selectedOrder ? railOrderTransitions(selectedOrder.statusMovimentacao) : [];

  async function updateOrderStatus(status) {
    if (!selectedOrder || !canOperate || busy) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      await railApi.atualizarStatusOrdem(selectedVisitId, selectedOrder.id, status);
      await loadWork();
      setSuccess(status === 'EM_EXECUCAO'
        ? `Movimentação do contêiner ${selectedOrder.codigoConteiner} iniciada.`
        : `Movimentação do contêiner ${selectedOrder.codigoConteiner} concluída.`);
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  async function reloadAll() {
    setSuccess('');
    await loadVisits();
    await loadWork();
  }

  return <>
    <PageHeader
      eyebrow="Ferrovia"
      title="Lista de trabalho"
      description="Ordens reais de carga e descarga por visita de trem, com acompanhamento e execução do fluxo ferroviário."
      actions={<button className="secondary" type="button" onClick={reloadAll} disabled={visitsLoading || workLoading || busy}>Atualizar</button>}
    />
    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>
    {!canOperate && <Message type="warning">Seu perfil possui acesso de consulta. A execução das movimentações permanece restrita aos perfis operacionais autorizados.</Message>}

    <Section title="Seleção operacional">
      <div className="filter-grid">
        <label className="field"><span>Janela de visitas</span><select value={days} onChange={(event) => setDays(Number(event.target.value))}><option value="7">7 dias</option><option value="15">15 dias</option><option value="30">30 dias</option></select></label>
        <label className="field"><span>Visita de trem</span><select value={selectedVisitId} onChange={(event) => { setSuccess(''); setSelectedVisitId(event.target.value); }} disabled={visitsLoading || !visits.length}><option value="">Selecione</option>{visits.map((item) => <option key={item.id} value={item.id}>{item.identificadorTrem} · {item.operadoraFerroviaria} · {item.statusVisita}</option>)}</select></label>
        <label className="field"><span>Status das ordens</span><select value={statusFilter} onChange={(event) => { setSuccess(''); setStatusFilter(event.target.value); }}><option value="">Pendentes e em execução</option><option value="PENDENTE">Pendentes</option><option value="EM_EXECUCAO">Em execução</option><option value="CONCLUIDA">Concluídas</option></select></label>
        <label className="field"><span>Busca</span><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Contêiner, operação ou status" /></label>
      </div>
    </Section>

    {visitsLoading ? <Loading label="Carregando visitas ferroviárias..." /> : !visits.length ? <EmptyState title="Nenhuma visita ferroviária encontrada" description="Importe ou cadastre um manifesto para formar a lista de trabalho." /> : <>
      <div className="metrics-grid">
        <MetricCard label="Ordens exibidas" value={filteredOrders.length} />
        <MetricCard label="Pendentes" value={orders.filter((item) => item.statusMovimentacao === 'PENDENTE').length} />
        <MetricCard label="Em execução" value={orders.filter((item) => item.statusMovimentacao === 'EM_EXECUCAO').length} />
        <MetricCard label="Vagões" value={visit?.listaVagoes?.length ?? '—'} />
      </div>

      {workLoading ? <Loading label="Carregando lista de trabalho..." /> : <div className="split-grid">
        <Section title={`Ordens ferroviárias (${filteredOrders.length})`} description="A lista segue as ordens persistidas para a visita selecionada.">
          <DataTable rows={filteredOrders} rowKey={(row) => row.id} onRowClick={setSelectedOrder} columns={[
            { key: 'codigoConteiner', label: 'Contêiner' },
            { key: 'tipoMovimentacao', label: 'Operação', render: (row) => operationLabel(row.tipoMovimentacao) },
            { key: 'statusMovimentacao', label: 'Status', render: (row) => <StatusBadge value={row.statusMovimentacao} /> },
            { key: 'criadoEm', label: 'Criada em', render: (row) => displayValue(row.criadoEm) },
            { key: 'atualizadoEm', label: 'Atualizada em', render: (row) => displayValue(row.atualizadoEm) }
          ]} emptyTitle="Nenhuma ordem para o filtro selecionado" />
        </Section>

        <Section title="Execução da ordem">
          {!selectedOrder ? <EmptyState title="Selecione uma ordem" description="Os detalhes e as transições permitidas serão exibidos aqui." /> : <div className="selection-panel">
            <div className="detail-grid">
              <div className="detail-row"><span>Contêiner</span><strong>{displayValue(selectedOrder.codigoConteiner)}</strong></div>
              <div className="detail-row"><span>Operação</span><strong>{operationLabel(selectedOrder.tipoMovimentacao)}</strong></div>
              <div className="detail-row"><span>Status</span><strong><StatusBadge value={selectedOrder.statusMovimentacao} /></strong></div>
              <div className="detail-row"><span>Visita</span><strong>{displayValue(visit?.identificadorTrem)}</strong></div>
              <div className="detail-row"><span>Operadora</span><strong>{displayValue(visit?.operadoraFerroviaria)}</strong></div>
              <div className="detail-row"><span>Vagão</span><strong>{displayValue(manifestRows.find((item) => item.codigoConteiner === selectedOrder.codigoConteiner)?.identificadorVagao)}</strong></div>
            </div>
            {canOperate && <div className="actions">
              {transitions.includes('EM_EXECUCAO') && <button type="button" className="small" disabled={busy} onClick={() => updateOrderStatus('EM_EXECUCAO')}>{busy ? 'Processando...' : 'Iniciar movimentação'}</button>}
              {transitions.includes('CONCLUIDA') && <button type="button" className="small" disabled={busy} onClick={() => updateOrderStatus('CONCLUIDA')}>{busy ? 'Processando...' : 'Concluir movimentação'}</button>}
              {!transitions.length && <span>Não há novas transições para esta ordem.</span>}
            </div>}
          </div>}
        </Section>
      </div>}

      <Section title="Manifesto da visita" description={`${visit?.identificadorTrem ?? 'Visita'} · chegada ${displayValue(visit?.horaChegadaPrevista)} · partida ${displayValue(visit?.horaPartidaPrevista)}`}>
        <DataTable rows={manifestRows} rowKey={(row, index) => `${row.operacao}-${row.codigoConteiner}-${index}`} columns={[
          { key: 'operacao', label: 'Operação' },
          { key: 'codigoConteiner', label: 'Contêiner' },
          { key: 'identificadorVagao', label: 'Vagão' },
          { key: 'statusOperacao', label: 'Status do manifesto', render: (row) => <StatusBadge value={row.statusOperacao} /> }
        ]} emptyTitle="A visita não possui contêineres no manifesto" />
      </Section>
    </>}
  </>;
}
