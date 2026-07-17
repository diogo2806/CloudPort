import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, hasAnyRole } from '../api.js';
import { railApi } from '../railApi.js';
import { nextRailOrderStatus, sortRailOrders, summarizeRailOrders } from '../railOperations.js';
import { DataTable, EmptyState, Loading, Message, MetricCard, PageHeader, Section, StatusBadge } from '../components.jsx';

const ORDER_STATUSES = ['PENDENTE', 'EM_EXECUCAO', 'CONCLUIDA'];

function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('pt-BR');
}

function movementLabel(value) {
  if (value === 'DESCARGA_TREM') return 'Descarga do trem';
  if (value === 'CARGA_TREM') return 'Carga no trem';
  return value || '—';
}

function actionLabel(status) {
  if (status === 'PENDENTE') return 'Iniciar';
  if (status === 'EM_EXECUCAO') return 'Concluir';
  return 'Concluída';
}

export function RailWorkListPage({ session }) {
  const [days, setDays] = useState(30);
  const [visits, setVisits] = useState([]);
  const [selectedVisitId, setSelectedVisitId] = useState('');
  const [orders, setOrders] = useState([]);
  const [statusFilter, setStatusFilter] = useState('TODAS');
  const [loadingVisits, setLoadingVisits] = useState(true);
  const [loadingOrders, setLoadingOrders] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [activeOrderId, setActiveOrderId] = useState(null);

  const canOperate = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO');

  const loadVisits = useCallback(async () => {
    setLoadingVisits(true);
    setError('');
    try {
      const response = await railApi.listarVisitas(days);
      const loaded = Array.isArray(response) ? response : [];
      setVisits(loaded);
      setSelectedVisitId((current) => {
        if (loaded.some((visit) => String(visit.id) === String(current))) return current;
        const operational = loaded.find((visit) => ['CHEGOU', 'PROCESSANDO'].includes(visit.statusVisita));
        return String((operational ?? loaded[0])?.id ?? '');
      });
    } catch (reason) {
      setVisits([]);
      setSelectedVisitId('');
      setError(formatError(reason, 'Não foi possível carregar as visitas ferroviárias.'));
    } finally {
      setLoadingVisits(false);
    }
  }, [days]);

  const loadOrders = useCallback(async () => {
    if (!selectedVisitId) {
      setOrders([]);
      return;
    }
    setLoadingOrders(true);
    setError('');
    try {
      const groups = await Promise.all(ORDER_STATUSES.map((status) => railApi.listarOrdens(selectedVisitId, status)));
      setOrders(sortRailOrders(groups.flatMap((group) => Array.isArray(group) ? group : [])));
    } catch (reason) {
      setOrders([]);
      setError(formatError(reason, 'Não foi possível carregar a lista de trabalho ferroviária.'));
    } finally {
      setLoadingOrders(false);
    }
  }, [selectedVisitId]);

  useEffect(() => { loadVisits(); }, [loadVisits]);
  useEffect(() => { loadOrders(); }, [loadOrders]);

  const selectedVisit = useMemo(
    () => visits.find((visit) => String(visit.id) === String(selectedVisitId)) ?? null,
    [visits, selectedVisitId]
  );
  const summary = useMemo(() => summarizeRailOrders(orders), [orders]);
  const visibleOrders = useMemo(
    () => statusFilter === 'TODAS' ? orders : orders.filter((order) => order.statusMovimentacao === statusFilter),
    [orders, statusFilter]
  );

  async function advanceOrder(order) {
    const nextStatus = nextRailOrderStatus(order?.statusMovimentacao);
    if (!nextStatus || !selectedVisitId || activeOrderId !== null) return;
    setActiveOrderId(order.id);
    setError('');
    setSuccess('');
    try {
      await railApi.atualizarStatusOrdem(selectedVisitId, order.id, nextStatus);
      setSuccess(`Ordem do contêiner ${order.codigoConteiner} atualizada para ${nextStatus}.`);
      await loadOrders();
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível atualizar a ordem ferroviária.'));
    } finally {
      setActiveOrderId(null);
    }
  }

  return <>
    <PageHeader
      eyebrow="Ferrovia"
      title="Lista de trabalho do trem"
      description="Acompanhe e avance as ordens reais de carga e descarga por visita ferroviária."
      actions={<div className="inline">
        <label className="compact-field">Janela
          <select value={days} onChange={(event) => setDays(Number(event.target.value))}>
            <option value="7">7 dias</option><option value="15">15 dias</option><option value="30">30 dias</option>
          </select>
        </label>
        <button className="secondary" onClick={() => { loadVisits(); loadOrders(); }}>Atualizar</button>
      </div>}
    />
    <Message type="error" onClose={() => setError('')}>{error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    <Section title="Visita ferroviária" description="As ordens são geradas quando a visita entra na fase CHEGOU.">
      {loadingVisits ? <Loading label="Carregando visitas..." /> : visits.length ? <label className="field">
        <span>Selecione o trem</span>
        <select value={selectedVisitId} onChange={(event) => setSelectedVisitId(event.target.value)}>
          {visits.map((visit) => <option key={visit.id} value={visit.id}>
            {visit.identificadorTrem} · {visit.operadoraFerroviaria} · {visit.statusVisita}
          </option>)}
        </select>
      </label> : <EmptyState title="Nenhuma visita ferroviária disponível" />}
    </Section>

    {selectedVisit && <>
      <div className="metrics-grid">
        <MetricCard label="Trem" value={selectedVisit.identificadorTrem} detail={selectedVisit.operadoraFerroviaria} />
        <MetricCard label="Pendentes" value={summary.pendentes} />
        <MetricCard label="Em execução" value={summary.emExecucao} />
        <MetricCard label="Concluídas" value={summary.concluidas} detail={`${summary.total} ordem(ns) no total`} />
      </div>

      <Section
        title="Ordens de movimentação"
        description={`Status da visita: ${selectedVisit.statusVisita}. Chegada prevista: ${formatDateTime(selectedVisit.horaChegadaPrevista)}.`}
        actions={<label className="compact-field">Status
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
            <option value="TODAS">Todas</option>
            <option value="PENDENTE">Pendentes</option>
            <option value="EM_EXECUCAO">Em execução</option>
            <option value="CONCLUIDA">Concluídas</option>
          </select>
        </label>}
      >
        {loadingOrders ? <Loading label="Carregando ordens..." /> : <DataTable
          rows={visibleOrders}
          rowKey="id"
          emptyTitle="Nenhuma ordem para o filtro selecionado"
          columns={[
            { key: 'codigoConteiner', label: 'Contêiner' },
            { key: 'tipoMovimentacao', label: 'Movimento', render: (row) => movementLabel(row.tipoMovimentacao) },
            { key: 'statusMovimentacao', label: 'Status', render: (row) => <StatusBadge value={row.statusMovimentacao} /> },
            { key: 'criadoEm', label: 'Criada em', render: (row) => formatDateTime(row.criadoEm) },
            { key: 'atualizadoEm', label: 'Atualizada em', render: (row) => formatDateTime(row.atualizadoEm) },
            { key: 'acao', label: 'Ação', render: (row) => <button
              type="button"
              className="secondary"
              disabled={!canOperate || !nextRailOrderStatus(row.statusMovimentacao) || activeOrderId !== null}
              onClick={() => advanceOrder(row)}
            >{activeOrderId === row.id ? 'Atualizando...' : actionLabel(row.statusMovimentacao)}</button> }
          ]}
        />}
        {!canOperate && <Message>Seu perfil possui acesso de consulta. A execução exige ADMIN_PORTO, PLANEJADOR ou OPERADOR_PATIO.</Message>}
      </Section>
    </>}
  </>;
}
