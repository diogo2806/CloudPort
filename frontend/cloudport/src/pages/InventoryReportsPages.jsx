import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError } from '../api.js';
import {
  inventoryReportsApi,
  selectGateReportRows,
  selectInventoryRows
} from '../inventoryReportsApi.js';
import {
  DataTable,
  EmptyState,
  Loading,
  Message,
  MetricCard,
  PageHeader,
  Section,
  StatusBadge
} from '../components.jsx';

function displayValue(value) {
  if (value === undefined || value === null || value === '') return '—';
  if (typeof value === 'number') return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 }).format(value);
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)) {
    const date = new Date(value);
    if (!Number.isNaN(date.getTime())) return date.toLocaleString('pt-BR');
  }
  return String(value);
}

function percentage(value) {
  return value === undefined || value === null ? '—' : `${displayValue(value)}%`;
}

function dateInputValue(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function defaultGateFilters() {
  const end = new Date();
  const start = new Date();
  start.setDate(start.getDate() - 7);
  return {
    inicio: dateInputValue(start),
    fim: dateInputValue(end),
    tipoOperacao: '',
    transportadoraId: ''
  };
}

function useRemote(loader, dependencies) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await loader();
      setData(response);
      return response;
    } catch (reason) {
      setError(formatError(reason));
      setData(null);
      return null;
    } finally {
      setLoading(false);
    }
  }, dependencies);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, loading, error, reload };
}

export function YardInventoryPage() {
  const [draftFilters, setDraftFilters] = useState({ codigo: '', status: '', tipoCarga: '' });
  const [filters, setFilters] = useState({ codigo: '', status: '', tipoCarga: '' });
  const remote = useRemote(
    () => inventoryReportsApi.obterInventario(filters),
    [filters.codigo, filters.status, filters.tipoCarga]
  );
  const rows = useMemo(() => selectInventoryRows(remote.data), [remote.data]);
  const summary = remote.data?.resumo ?? {};

  function submit(event) {
    event.preventDefault();
    setFilters({ ...draftFilters });
  }

  function clear() {
    const empty = { codigo: '', status: '', tipoCarga: '' };
    setDraftFilters(empty);
    setFilters(empty);
  }

  return <>
    <PageHeader
      eyebrow="Pátio"
      title="Inventário de contêineres"
      description="Consulta consolidada das unidades, posição, situação operacional, restrições e peso no pátio."
      actions={<button type="button" className="secondary" onClick={remote.reload}>Atualizar</button>}
    />
    <Message type="error">{remote.error}</Message>

    <Section title="Filtros operacionais">
      <form className="planner-selection-grid" onSubmit={submit}>
        <label className="field"><span>Código</span><input value={draftFilters.codigo} onChange={(event) => setDraftFilters((current) => ({ ...current, codigo: event.target.value }))} placeholder="Ex.: ABCD1234567" maxLength="30" /></label>
        <label className="field"><span>Status</span><select value={draftFilters.status} onChange={(event) => setDraftFilters((current) => ({ ...current, status: event.target.value }))}><option value="">Todos</option><option value="ALOCADO">Alocado</option><option value="ARMAZENADO">Armazenado</option><option value="RESERVADO">Reservado</option><option value="INSPECIONANDO">Inspecionando</option><option value="INSPECIONADO">Inspecionado</option><option value="AGUARDANDO_RETIRADA">Aguardando retirada</option><option value="LIBERADO">Liberado</option><option value="DESPACHADO">Despachado</option><option value="RETIDO">Retido</option><option value="DANIFICADO">Danificado</option></select></label>
        <label className="field"><span>Tipo de carga</span><select value={draftFilters.tipoCarga} onChange={(event) => setDraftFilters((current) => ({ ...current, tipoCarga: event.target.value }))}><option value="">Todos</option><option value="SECO">Seco</option><option value="REFRIGERADO">Refrigerado</option><option value="PERIGOSO">Perigoso</option><option value="GRANELEIRO">Graneleiro</option><option value="OUTRO">Outro</option></select></label>
        <div className="field"><span>Ações</span><div className="actions"><button type="submit">Consultar</button><button type="button" className="secondary" onClick={clear}>Limpar</button></div></div>
      </form>
    </Section>

    {remote.loading ? <Loading label="Carregando inventário..." /> : <>
      <div className="metrics-grid">
        <MetricCard label="Contêineres" value={summary.totalConteiners ?? 0} />
        <MetricCard label="Em operação" value={summary.totalOperacionais ?? 0} />
        <MetricCard label="Liberados/Despachados" value={summary.totalLiberados ?? 0} />
        <MetricCard label="Retidos" value={summary.totalRetidos ?? 0} />
        <MetricCard label="Danificados" value={summary.totalDanificados ?? 0} />
        <MetricCard label="Refrigerados" value={summary.totalRefrigerados ?? 0} />
        <MetricCard label="Perigosos" value={summary.totalPerigosos ?? 0} />
        <MetricCard label="Peso total" value={`${displayValue(summary.pesoTotalToneladas ?? 0)} t`} detail={summary.atualizadoEm ? `Atualizado em ${displayValue(summary.atualizadoEm)}` : undefined} />
      </div>
      <Section title="Unidades no inventário">
        {rows.length ? <DataTable rows={rows} rowKey="identificador" columns={[
          { key: 'identificacao', label: 'Contêiner' },
          { key: 'statusOperacional', label: 'Status', render: (row) => <StatusBadge value={row.statusOperacional} /> },
          { key: 'tipoCarga', label: 'Carga', render: (row) => <StatusBadge value={row.tipoCarga} /> },
          { key: 'posicaoPatio', label: 'Posição' },
          { key: 'pesoToneladas', label: 'Peso', render: (row) => row.pesoToneladas === undefined || row.pesoToneladas === null ? '—' : `${displayValue(row.pesoToneladas)} t` },
          { key: 'restricoes', label: 'Restrições' },
          { key: 'ultimaAtualizacao', label: 'Atualizado', render: (row) => displayValue(row.ultimaAtualizacao) }
        ]} emptyTitle="Nenhum contêiner encontrado" /> : <EmptyState title="Nenhum contêiner encontrado" description="Altere os filtros ou verifique se há unidades registradas no pátio." />}
      </Section>
    </>}
  </>;
}

export function GateReportsPage() {
  const [draftFilters, setDraftFilters] = useState(defaultGateFilters);
  const [filters, setFilters] = useState(defaultGateFilters);
  const remote = useRemote(
    () => inventoryReportsApi.obterRelatorioGate(filters),
    [filters.inicio, filters.fim, filters.tipoOperacao, filters.transportadoraId]
  );
  const rows = useMemo(() => selectGateReportRows(remote.data), [remote.data]);
  const summary = remote.data?.resumo ?? {};
  const occupancyRows = Array.isArray(summary.ocupacaoPorHora) ? summary.ocupacaoPorHora : [];
  const turnaroundRows = Array.isArray(summary.turnaroundPorDia) ? summary.turnaroundPorDia : [];

  function submit(event) {
    event.preventDefault();
    setFilters({ ...draftFilters });
  }

  function clear() {
    const defaults = defaultGateFilters();
    setDraftFilters(defaults);
    setFilters(defaults);
  }

  return <>
    <PageHeader
      eyebrow="Gate"
      title="Relatórios operacionais"
      description="Indicadores de pontualidade, no-show, ocupação de janelas, abandono e turnaround com dados persistidos do Gate."
      actions={<button type="button" className="secondary" onClick={remote.reload}>Atualizar</button>}
    />
    <Message type="error">{remote.error}</Message>

    <Section title="Período e operação">
      <form className="planner-selection-grid" onSubmit={submit}>
        <label className="field"><span>Início</span><input type="date" value={draftFilters.inicio} onChange={(event) => setDraftFilters((current) => ({ ...current, inicio: event.target.value }))} /></label>
        <label className="field"><span>Fim</span><input type="date" value={draftFilters.fim} min={draftFilters.inicio || undefined} onChange={(event) => setDraftFilters((current) => ({ ...current, fim: event.target.value }))} /></label>
        <label className="field"><span>Operação</span><select value={draftFilters.tipoOperacao} onChange={(event) => setDraftFilters((current) => ({ ...current, tipoOperacao: event.target.value }))}><option value="">Todas</option><option value="ENTRADA">Entrada</option><option value="SAIDA">Saída</option><option value="DEVOLUCAO">Devolução</option><option value="TRANSFERENCIA">Transferência</option></select></label>
        <label className="field"><span>Transportadora ID</span><input type="number" min="1" value={draftFilters.transportadoraId} onChange={(event) => setDraftFilters((current) => ({ ...current, transportadoraId: event.target.value }))} placeholder="Todas" /></label>
        <div className="field"><span>Ações</span><div className="actions"><button type="submit">Gerar relatório</button><button type="button" className="secondary" onClick={clear}>Restaurar período</button></div></div>
      </form>
    </Section>

    {remote.loading ? <Loading label="Gerando relatório do Gate..." /> : <>
      <div className="metrics-grid">
        <MetricCard label="Agendamentos" value={summary.totalAgendamentos ?? 0} />
        <MetricCard label="Pontualidade" value={percentage(summary.percentualPontualidade)} />
        <MetricCard label="No-show" value={percentage(summary.percentualNoShow)} />
        <MetricCard label="Ocupação de slots" value={percentage(summary.percentualOcupacaoSlots)} />
        <MetricCard label="Turnaround médio" value={summary.tempoMedioTurnaroundMinutos === undefined ? '—' : `${displayValue(summary.tempoMedioTurnaroundMinutos)} min`} />
        <MetricCard label="Abandono" value={percentage(summary.percentualAbandono)} detail={summary.variacaoAbandonoPercentual === undefined ? undefined : `Variação: ${percentage(summary.variacaoAbandonoPercentual)}`} />
      </div>

      <Section title="Agendamentos do período">
        <DataTable rows={rows} rowKey="codigo" columns={[
          { key: 'codigo', label: 'Código' },
          { key: 'tipoOperacao', label: 'Operação', render: (row) => <StatusBadge value={row.tipoOperacao} /> },
          { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
          { key: 'transportadora', label: 'Transportadora' },
          { key: 'horarioPrevistoChegada', label: 'Chegada prevista', render: (row) => displayValue(row.horarioPrevistoChegada) },
          { key: 'horarioRealChegada', label: 'Chegada real', render: (row) => displayValue(row.horarioRealChegada) },
          { key: 'horarioRealSaida', label: 'Saída real', render: (row) => displayValue(row.horarioRealSaida) }
        ]} emptyTitle="Nenhum agendamento no período" />
      </Section>

      {occupancyRows.length > 0 && <Section title="Ocupação por hora"><DataTable rows={occupancyRows} columns={[
        { key: 'horaInicio', label: 'Hora' },
        { key: 'totalAgendamentos', label: 'Agendados' },
        { key: 'capacidadeSlot', label: 'Capacidade' }
      ]} /></Section>}

      {turnaroundRows.length > 0 && <Section title="Turnaround por dia"><DataTable rows={turnaroundRows} columns={[
        { key: 'dia', label: 'Data', render: (row) => displayValue(row.dia) },
        { key: 'tempoMedioMinutos', label: 'Tempo médio', render: (row) => `${displayValue(row.tempoMedioMinutos)} min` }
      ]} /></Section>}
    </>}
  </>;
}
