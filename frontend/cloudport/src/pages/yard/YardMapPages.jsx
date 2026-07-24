import { useEffect, useMemo, useState } from 'react';
import { api, hasAnyRole, readSession, sanitizeText } from '../../api.js';
import { DataTable, Loading, Message, MetricCard, Section, StatusBadge } from '../../components.jsx';
import { yardStowageWarningApi } from '../../yardStowageWarningApi.js';
import { OpenStreetMapYardMap } from './GoogleYardMap.jsx';
import { OperationalYardViews } from './OperationalYardViews.jsx';
import { YardAllocationEditor } from './YardAllocationEditor.jsx';
import { useYardContingency } from './yardContingency.js';
import { yardGeometryApi } from './yardGeometryApi.js';
import { mergeYardEquipment, yardRestrictionSummary } from './yardLiveMap.js';
import { yardOperationalApi } from './yardOperationalApi.js';
import { YardReeferPanel } from './YardReeferPanel.jsx';
import { buildStacks, DetailGrid, FilterField, normalized, Pagination, positionKey, usePagination, useRemote, YardPageHeader } from './YardShared.jsx';
import { YardStowageWarningsPage } from './YardStowageWarningsPage.jsx';

const FINAL_ORDER_STATUSES = new Set(['CONCLUIDA', 'CANCELADA']);

function formatSynchronization(value) {
  if (!value) return 'Nenhuma sincronização válida';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('pt-BR');
}

function warningPositionKey(position) {
  const layer = position?.camadaOperacional ?? position?.camadaDestino;
  return `${position?.linha}/${position?.coluna}/${layer}`;
}

export function YardMapPage({ navigate }) {
  const [filters, setFilters] = useState({ status: '', tipoCarga: '', destino: '', camada: '', tipoEquipamento: '' });
  const [selectedStack, setSelectedStack] = useState(null);
  const [showStowageWarnings, setShowStowageWarnings] = useState(false);
  const session = readSession();
  const canOperate = hasAnyRole(session, 'ROOT', 'ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO');
  const canEditGeometry = hasAnyRole(session, 'ROOT', 'ADMIN_PORTO', 'PLANEJADOR');
  const remote = useYardContingency(async () => {
    const query = Object.fromEntries(Object.entries(filters).filter(([, value]) => value));
    const [map, positions, availableFilters, orders, movements, telemetry, allContainers, reeferTelemetry, geometries, stowageWarnings] = await Promise.all([
      api.obterMapaPatio(query),
      api.listarPosicoesReservaveisPatio(),
      api.obterFiltrosMapaPatio(),
      api.listarOrdensPatio(),
      api.listarMovimentacoesPatio(),
      api.listarTelemetriaEquipamentosPatio(),
      api.listarConteineresPatio(),
      yardOperationalApi.listarTelemetriaReefers(),
      yardGeometryApi.listar(),
      yardStowageWarningApi.resumo()
    ]);
    return {
      map: map ?? {},
      positions: positions ?? [],
      filters: availableFilters ?? {},
      orders: orders ?? [],
      movements: movements ?? [],
      telemetry: telemetry ?? [],
      allContainers: allContainers ?? [],
      reeferTelemetry: reeferTelemetry ?? [],
      geometries: geometries ?? [],
      stowageWarnings: stowageWarnings ?? { ativos: 0, criticos: 0, porPosicao: {} }
    };
  }, [filters.status, filters.tipoCarga, filters.destino, filters.camada, filters.tipoEquipamento]);

  useEffect(() => {
    const interval = globalThis.setInterval(() => remote.reload({ silent: true }), 10000);
    return () => globalThis.clearInterval(interval);
  }, [remote.reload]);

  if (showStowageWarnings) {
    return <>
      <div className="actions"><button className="secondary" onClick={() => setShowStowageWarnings(false)}>Voltar ao mapa operacional</button></div>
      <YardStowageWarningsPage navigate={navigate} session={session} />
    </>;
  }

  const operationalCommandsEnabled = canOperate && !remote.isOffline;
  const geometryCommandsEnabled = canEditGeometry && !remote.isOffline;
  const map = remote.data?.map ?? {};
  const containers = remote.data?.allContainers ?? [];
  const equipment = useMemo(
    () => mergeYardEquipment(map.equipamentos, remote.data?.telemetry),
    [map.equipamentos, remote.data?.telemetry]
  );
  const enrichedPositions = useMemo(() => {
    const containersByPosition = new Map(containers.map((container) => [positionKey(container), container]));
    const warningsByPosition = remote.data?.stowageWarnings?.porPosicao ?? {};
    return (remote.data?.positions ?? []).map((position) => {
      const container = containersByPosition.get(positionKey(position));
      return {
        ...position,
        conteinerId: container?.id ?? null,
        tipoCarga: container?.tipoCarga ?? null,
        destino: container?.destino ?? null,
        statusConteiner: position.statusConteiner ?? container?.status ?? null,
        avisosEstivagem: warningsByPosition[warningPositionKey(position)] ?? 0
      };
    });
  }, [remote.data?.positions, remote.data?.stowageWarnings, containers]);
  const stacks = useMemo(() => buildStacks(enrichedPositions, remote.data?.orders), [enrichedPositions, remote.data?.orders]);
  const restrictions = useMemo(() => yardRestrictionSummary(stacks), [stacks]);
  const routes = useMemo(() => {
    const containersByCode = new Map(containers.map((container) => [sanitizeText(container.codigo), container]));
    return (remote.data?.orders ?? [])
      .filter((order) => !FINAL_ORDER_STATUSES.has(order.statusOrdem))
      .map((order) => {
        const container = containersByCode.get(sanitizeText(order.codigoConteiner));
        if (!container || container.linha === undefined || container.coluna === undefined) return null;
        return {
          id: order.id,
          codigoConteiner: order.codigoConteiner,
          origem: { linha: container.linha, coluna: container.coluna },
          destino: { linha: order.linhaDestino, coluna: order.colunaDestino }
        };
      })
      .filter(Boolean);
  }, [containers, remote.data?.orders]);

  function optionList(key) {
    return remote.data?.filters?.[key] ?? [];
  }

  async function saveGeometry(payload) {
    if (remote.isOffline) throw new Error('A edição de geometria é bloqueada no modo de contingência.');
    const { id, ...body } = payload;
    const saved = id
      ? await yardGeometryApi.atualizar(id, body)
      : await yardGeometryApi.criar(body);
    await remote.reload();
    return saved;
  }

  async function deleteGeometry(id, reason) {
    if (remote.isOffline) throw new Error('A exclusão de geometria é bloqueada no modo de contingência.');
    await yardGeometryApi.excluir(id, reason);
    await remote.reload();
  }

  return <>
    <YardPageHeader path="/home/patio/mapa" navigate={navigate} title="Mapa operacional" description="Vistas de bloco, seção, scan e microvisão com heatmaps, rotas, reefers, CHEs, allocations, workspaces e simulação antes da movimentação." actions={<div className="actions"><button className="secondary" onClick={remote.reload}>Atualizar</button><button onClick={() => setShowStowageWarnings(true)}>Avisos de estivagem ({remote.data?.stowageWarnings?.ativos ?? 0})</button></div>} />
    <Message type={remote.isOffline ? 'warning' : 'success'}>Estado: {remote.connectionStatus} · Última sincronização: {formatSynchronization(remote.lastSynchronization)}{remote.snapshotExpired ? ' · FOTOGRAFIA EXPIRADA' : ''}</Message>
    <Message type="error">{remote.error}</Message>
    {remote.isOffline && remote.hasSnapshot && <Message type="warning">MAPA CONGELADO. Os dados não estão em tempo real e todos os comandos que alteram o estado oficial estão bloqueados.</Message>}
    {remote.reconciliation && <Message type="warning">A reconexão encontrou divergências entre a fotografia local e o estado oficial. A tela foi atualizada com os dados do servidor; revise as posições e ordens antes de operar.</Message>}
    {!canOperate && <Message type="warning">Seu perfil pode consultar todas as vistas, mas não pode confirmar movimentações, replanejar allocations nem editar restrições.</Message>}
    {(remote.data?.stowageWarnings?.criticos ?? 0) > 0 && <Message type="error">Existem {remote.data.stowageWarnings.criticos} aviso(s) crítico(s) de estivagem. Planejamentos e dispatches incompatíveis estão bloqueados.</Message>}
    <Section title="Filtros do mapa" description="Os valores são fornecidos pelo backend do Yard."><div className="filter-grid">
      <FilterField label="Status do contêiner"><select value={filters.status} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))}><option value="">Todos</option>{optionList('statusDisponiveis').map((value) => <option key={value}>{value}</option>)}</select></FilterField>
      <FilterField label="Tipo de carga"><select value={filters.tipoCarga} onChange={(event) => setFilters((current) => ({ ...current, tipoCarga: event.target.value }))}><option value="">Todos</option>{optionList('tiposCargaDisponiveis').map((value) => <option key={value}>{value}</option>)}</select></FilterField>
      <FilterField label="Destino"><select value={filters.destino} onChange={(event) => setFilters((current) => ({ ...current, destino: event.target.value }))}><option value="">Todos</option>{optionList('destinosDisponiveis').map((value) => <option key={value}>{value}</option>)}</select></FilterField>
      <FilterField label="Camada"><select value={filters.camada} onChange={(event) => setFilters((current) => ({ ...current, camada: event.target.value }))}><option value="">Todas</option>{optionList('camadasOperacionaisDisponiveis').map((value) => <option key={value}>{value}</option>)}</select></FilterField>
      <FilterField label="Equipamento"><select value={filters.tipoEquipamento} onChange={(event) => setFilters((current) => ({ ...current, tipoEquipamento: event.target.value }))}><option value="">Todos</option>{optionList('tiposEquipamentoDisponiveis').map((value) => <option key={value}>{value}</option>)}</select></FilterField>
    </div></Section>
    {remote.loading && !remote.hasSnapshot ? <Loading label="Carregando mapa, rotas e telemetria..." /> : <>
      <div className="metrics-grid"><MetricCard label="Blocos" value={stacks.length} /><MetricCard label="Posições reais" value={enrichedPositions.length} /><MetricCard label="Geometrias" value={remote.data?.geometries?.length ?? 0} /><MetricCard label="Contêineres" value={containers.length} /><MetricCard label={remote.isOffline ? 'CHEs na fotografia' : 'CHEs em tempo real'} value={equipment.length} detail={`${routes.length} rota(s) ativa(s)`} /><MetricCard label="Pilhas bloqueadas" value={restrictions.blocked} detail={`${restrictions.interdicted} interditada(s)`} /><MetricCard label="Avisos de estivagem" value={remote.data?.stowageWarnings?.ativos ?? 0} detail={`${remote.data?.stowageWarnings?.criticos ?? 0} crítico(s)`} /></div>
      <Section title="Pátio georreferenciado" description="Polígonos GeoJSON persistidos aparecem sobre o OpenStreetMap gratuito. Root, planejadores e administradores podem criar e ajustar o desenho clicando diretamente no mapa.">
        <OpenStreetMapYardMap blocks={stacks} selectedStack={selectedStack} onSelectStack={setSelectedStack} routes={routes} equipment={equipment} geometries={remote.data?.geometries} canEditGeometry={geometryCommandsEnabled} onSaveGeometry={saveGeometry} onDeleteGeometry={deleteGeometry} />
        {!!routes.length && <div className="yard-route-summary">{routes.slice(0, 20).map((route) => <span key={route.id}>WI #{route.id} · {sanitizeText(route.codigoConteiner)} · L{route.origem.linha}/C{route.origem.coluna} → L{route.destino.linha}/C{route.destino.coluna}</span>)}</div>}
      </Section>
      <Section title="Console operacional do pátio" description="A seleção é sincronizada com o mapa. Arraste um contêiner para uma posição livre; o sistema simula e só persiste após confirmação motivada.">
        <OperationalYardViews blocks={stacks} movements={remote.data?.movements} telemetry={equipment} alerts={map.alertas} filters={filters} selectedStack={selectedStack} onSelectStack={setSelectedStack} onApplyFilters={(workspaceFilters) => setFilters((current) => ({ ...current, ...workspaceFilters }))} canOperate={operationalCommandsEnabled} onReload={remote.reload} />
      </Section>
      <Section title="Reefers e alarmes de temperatura" description="Leituras reais, faixa configurada, alimentação elétrica e alerta de telemetria desatualizada."><YardReeferPanel telemetry={remote.data?.reeferTelemetry} /></Section>
      <Section title="Editor gráfico de allocations" description="Selecione uma work instruction e reposicione visualmente seu destino. A alteração só é aplicada após simulação e justificativa."><YardAllocationEditor blocks={stacks} canOperate={operationalCommandsEnabled} onReload={remote.reload} /></Section>
      {!!map.alertas?.length && <Section title="Alertas do mapa"><div className="card-list">{map.alertas.map((alert, index) => <article className="content-card" key={`${alert.tipoAlerta}-${index}`}><div className="card-meta"><StatusBadge value={alert.nivelSeveridade} /><span>{sanitizeText(alert.tipoAlerta)}</span></div><h3>{sanitizeText(alert.mensagem)}</h3><p>{sanitizeText(alert.recomendacao)}</p></article>)}</div></Section>}
    </>}
  </>;
}

export function YardPositionsPage({ navigate }) {
  const [query, setQuery] = useState('');
  const [block, setBlock] = useState('');
  const [availability, setAvailability] = useState('');
  const [selected, setSelected] = useState(null);
  const remote = useRemote(() => api.listarPosicoesReservaveisPatio(), []);
  const rows = useMemo(() => (remote.data ?? []).filter((position) => {
    const matchesQuery = !query || normalized(`${position.codigoConteiner} ${position.linha} ${position.coluna} ${position.camadaOperacional} ${position.notaOperacional}`).includes(normalized(query));
    const matchesBlock = !block || position.bloco === block;
    const state = position.interditada ? 'INTERDITADA' : position.bloqueada || !position.areaPermitida ? 'BLOQUEADA' : position.ocupada ? 'OCUPADA' : 'DISPONIVEL';
    return matchesQuery && matchesBlock && (!availability || state === availability);
  }), [remote.data, query, block, availability]);
  const blocks = useMemo(() => Array.from(new Set((remote.data ?? []).map((position) => position.bloco).filter(Boolean))).sort(), [remote.data]);
  const paged = usePagination(rows);
  return <>
    <YardPageHeader path="/home/patio/posicoes" navigate={navigate} title="Posições do pátio" description="Consulta navegável das posições reais, capacidade da pilha, notas e restrições de placement usadas pelo Yard." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error}</Message>
    <Section title="Filtros"><div className="filter-grid"><FilterField label="Busca"><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Contêiner, posição ou nota" /></FilterField><FilterField label="Bloco"><select value={block} onChange={(event) => setBlock(event.target.value)}><option value="">Todos</option>{blocks.map((value) => <option key={value}>{value}</option>)}</select></FilterField><FilterField label="Disponibilidade"><select value={availability} onChange={(event) => setAvailability(event.target.value)}><option value="">Todas</option>{['DISPONIVEL', 'OCUPADA', 'BLOQUEADA', 'INTERDITADA'].map((value) => <option key={value}>{value}</option>)}</select></FilterField></div></Section>
    <div className="split-grid"><Section title={`Posições (${rows.length})`}>{remote.loading ? <Loading /> : <><DataTable rows={paged.rows} rowKey={(row) => row.id} onRowClick={setSelected} columns={[
      { key: 'bloco', label: 'Bloco' }, { key: 'linha', label: 'Linha' }, { key: 'coluna', label: 'Coluna' }, { key: 'camadaOperacional', label: 'Camada' }, { key: 'ocupada', label: 'Situação', render: (row) => <StatusBadge value={row.interditada ? 'INTERDITADA' : row.bloqueada || !row.areaPermitida ? 'BLOQUEADA' : row.ocupada ? 'OCUPADA' : 'DISPONIVEL'} /> }, { key: 'codigoConteiner', label: 'Contêiner' }, { key: 'notaOperacional', label: 'Nota' }, { key: 'ocupacaoPilha', label: 'Pilha', render: (row) => `${row.ocupacaoPilha ?? 0}/${row.capacidadePilha ?? '—'}` }
    ]} emptyTitle="Nenhuma posição corresponde aos filtros" /><Pagination page={paged.page} totalPages={paged.totalPages} totalRows={rows.length} onChange={paged.setPage} /></>}</Section><Section title="Detalhes e restrições"><DetailGrid value={selected} fields={[["id", "ID"], ["bloco", "Bloco"], ["linha", "Linha"], ["coluna", "Coluna"], ["camadaOperacional", "Camada"], ["codigoConteiner", "Contêiner"], ["statusConteiner", "Status do contêiner"], ["notaOperacional", "Nota operacional"], ["tiposCargaPermitidos", "Cargas permitidas"], ["pesoMaximoToneladas", "Peso máximo (t)"], ["alturaMaximaMetros", "Altura máxima (m)"], ["camadaMaxima", "Camada máxima"], ["capacidadePilha", "Capacidade da pilha"], ["ocupacaoPilha", "Ocupação da pilha"]]} /></Section></div>
  </>;
}
