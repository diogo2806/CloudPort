import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError } from '../api.js';
import {
  inventoryReportsApi,
  selectCanonicalInventoryRows,
  selectGateReportRows
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
  if (typeof value === 'number') return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 3 }).format(value);
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

function emptyInventoryFilters() {
  return {
    identificacao: '',
    categoria: '',
    estado: '',
    condicao: '',
    proprietario: '',
    operador: '',
    somenteComHold: false,
    somenteReefer: false
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

function DetailTable({ rows, columns, emptyTitle }) {
  if (!Array.isArray(rows) || rows.length === 0) {
    return <EmptyState title={emptyTitle} />;
  }
  return <DataTable
    rows={rows}
    columns={columns}
    rowKey={(row, index) => row.id ?? row.numero ?? row.codigo ?? `${emptyTitle}-${index}`}
    selectable={false}
    inspectable={false}
    defaultPageSize={10}
  />;
}

function UnitInspector({ detail, loading, error, onClose, onReload }) {
  const [stateDraft, setStateDraft] = useState({ estado: '', motivo: '' });
  const [sealDraft, setSealDraft] = useState({ numero: '', tipo: 'TERMINAL' });
  const [restrictionDraft, setRestrictionDraft] = useState({ tipo: 'HOLD', codigo: '', descricao: '' });
  const [actionError, setActionError] = useState('');
  const [busy, setBusy] = useState(false);

  if (loading) return <Section title="Inspector da unidade"><Loading label="Carregando unidade..." /></Section>;
  if (error) return <Section title="Inspector da unidade"><Message type="error">{error}</Message></Section>;
  if (!detail?.unidade) return null;

  const unit = detail.unidade;

  async function execute(action) {
    setBusy(true);
    setActionError('');
    try {
      await action();
      await onReload(unit.id);
    } catch (reason) {
      setActionError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  return <Section
    title={`Inspector: ${unit.identificacao}`}
    description="Visão única de ciclo de vida, equipamento, documentos, avarias, restrições, manutenção e histórico."
    actions={<button type="button" className="secondary" onClick={onClose}>Fechar</button>}
  >
    <Message type="error">{actionError}</Message>
    <div className="metrics-grid">
      <MetricCard label="Categoria" value={unit.categoria} />
      <MetricCard label="Tipo" value={unit.tipoEquipamentoCodigo} detail={unit.tipoEquipamentoDescricao} />
      <MetricCard label="Ciclo de vida" value={unit.estado} />
      <MetricCard label="Condição" value={unit.condicao} />
      <MetricCard label="Manutenção" value={unit.statusManutencao} />
      <MetricCard label="Posição real" value={unit.posicaoAtual} detail={`Planejada: ${displayValue(unit.posicaoPlanejada)}`} />
      <MetricCard label="Ownership" value={unit.proprietario} detail={`Operador: ${displayValue(unit.operador)}`} />
      <MetricCard label="Holds / Permissions" value={`${unit.holdsAtivos} / ${unit.permissionsAtivas}`} />
    </div>

    <Section title="Ações operacionais rápidas">
      <div className="planner-selection-grid">
        <label className="field"><span>Novo estado</span><select value={stateDraft.estado} onChange={(event) => setStateDraft((current) => ({ ...current, estado: event.target.value }))}>
          <option value="">Selecione</option>
          {['PRE_AVISADA', 'ATIVA', 'NO_PATIO', 'EM_OPERACAO', 'EM_TRANSITO', 'EMBARCADA', 'DESEMBARCADA', 'LIBERADA', 'DESPACHADA', 'INATIVA', 'APOSENTADA'].map((value) => <option key={value}>{value}</option>)}
        </select></label>
        <label className="field"><span>Motivo</span><input value={stateDraft.motivo} onChange={(event) => setStateDraft((current) => ({ ...current, motivo: event.target.value }))} maxLength="500" /></label>
        <div className="field"><span>Ciclo de vida</span><button type="button" disabled={busy || !stateDraft.estado} onClick={() => execute(() => inventoryReportsApi.atualizarEstadoInventario(unit.id, stateDraft))}>Atualizar estado</button></div>

        <label className="field"><span>Número do lacre</span><input value={sealDraft.numero} onChange={(event) => setSealDraft((current) => ({ ...current, numero: event.target.value }))} maxLength="60" /></label>
        <label className="field"><span>Tipo do lacre</span><input value={sealDraft.tipo} onChange={(event) => setSealDraft((current) => ({ ...current, tipo: event.target.value }))} maxLength="40" /></label>
        <div className="field"><span>Lacre</span><button type="button" disabled={busy || !sealDraft.numero} onClick={() => execute(() => inventoryReportsApi.adicionarLacreInventario(unit.id, { ...sealDraft, status: 'ATIVO' }))}>Anexar lacre</button></div>

        <label className="field"><span>Restrição</span><select value={restrictionDraft.tipo} onChange={(event) => setRestrictionDraft((current) => ({ ...current, tipo: event.target.value }))}><option>HOLD</option><option>PERMISSION</option></select></label>
        <label className="field"><span>Código</span><input value={restrictionDraft.codigo} onChange={(event) => setRestrictionDraft((current) => ({ ...current, codigo: event.target.value }))} maxLength="60" /></label>
        <label className="field"><span>Descrição</span><input value={restrictionDraft.descricao} onChange={(event) => setRestrictionDraft((current) => ({ ...current, descricao: event.target.value }))} maxLength="500" /></label>
        <div className="field"><span>Hold / Permission</span><button type="button" disabled={busy || !restrictionDraft.codigo} onClick={() => execute(() => inventoryReportsApi.adicionarRestricaoInventario(unit.id, { ...restrictionDraft, ativa: true }))}>Registrar</button></div>
      </div>
    </Section>

    <Section title="Tipo, dimensões e equivalências">
      <DetailTable rows={[detail.tipoEquipamento]} columns={[
        { key: 'codigo', label: 'Código' },
        { key: 'codigoIso', label: 'ISO' },
        { key: 'categoria', label: 'Categoria' },
        { key: 'comprimentoMm', label: 'Comprimento (mm)' },
        { key: 'larguraMm', label: 'Largura (mm)' },
        { key: 'alturaMm', label: 'Altura (mm)' },
        { key: 'taraKg', label: 'Tara (kg)' },
        { key: 'capacidadeKg', label: 'Capacidade (kg)' },
        { key: 'grupoEquivalencia', label: 'Grupo equivalente' }
      ]} emptyTitle="Tipo não informado" />
      {detail.tiposEquivalentes?.length > 0 && <DetailTable rows={detail.tiposEquivalentes} columns={[
        { key: 'codigo', label: 'Tipo equivalente' }, { key: 'descricao', label: 'Descrição' }, { key: 'codigoIso', label: 'ISO' }
      ]} emptyTitle="Sem equivalências" />}
    </Section>

    <Section title="Lacres"><DetailTable rows={detail.lacres} columns={[
      { key: 'numero', label: 'Número' }, { key: 'tipo', label: 'Tipo' }, { key: 'status', label: 'Status' },
      { key: 'anexadoEm', label: 'Anexado', render: (row) => displayValue(row.anexadoEm) }, { key: 'responsavel', label: 'Responsável' }
    ]} emptyTitle="Nenhum lacre registrado" /></Section>

    <Section title="Documentos"><DetailTable rows={detail.documentos} columns={[
      { key: 'tipo', label: 'Tipo' }, { key: 'numero', label: 'Número' }, { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
      { key: 'validoAte', label: 'Validade' }, { key: 'uri', label: 'Arquivo/URI' }
    ]} emptyTitle="Nenhum documento registrado" /></Section>

    <Section title="Avarias, componentes e condições"><DetailTable rows={detail.avarias} columns={[
      { key: 'componente', label: 'Componente' }, { key: 'tipo', label: 'Avaria' }, { key: 'severidade', label: 'Severidade' },
      { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> }, { key: 'descricao', label: 'Descrição' },
      { key: 'detectadaEm', label: 'Detectada', render: (row) => displayValue(row.detectadaEm) }
    ]} emptyTitle="Nenhuma avaria registrada" /></Section>

    <Section title="Holds e permissions"><DetailTable rows={detail.holdsPermissions} columns={[
      { key: 'tipo', label: 'Tipo', render: (row) => <StatusBadge value={row.tipo} /> }, { key: 'codigo', label: 'Código' },
      { key: 'descricao', label: 'Descrição' }, { key: 'autoridade', label: 'Autoridade' },
      { key: 'ativa', label: 'Ativa', render: (row) => row.ativa ? 'Sim' : 'Não' }, { key: 'validoAte', label: 'Válida até', render: (row) => displayValue(row.validoAte) }
    ]} emptyTitle="Nenhum hold ou permission" /></Section>

    <Section title="Manutenção e reparo"><DetailTable rows={detail.manutencoes} columns={[
      { key: 'ordemServico', label: 'Ordem' }, { key: 'tipoServico', label: 'Serviço' }, { key: 'fornecedor', label: 'Fornecedor' },
      { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
      { key: 'abertaEm', label: 'Abertura', render: (row) => displayValue(row.abertaEm) }, { key: 'concluidaEm', label: 'Conclusão', render: (row) => displayValue(row.concluidaEm) }
    ]} emptyTitle="Nenhuma manutenção registrada" /></Section>

    <Section title="Equipamentos montados e desmontados"><DetailTable rows={detail.equipamentosMontados} columns={[
      { key: 'unidadePrincipal', label: 'Principal' }, { key: 'unidadeRelacionada', label: 'Relacionada' }, { key: 'papel', label: 'Papel' },
      { key: 'ativo', label: 'Montado', render: (row) => row.ativo ? 'Sim' : 'Não' },
      { key: 'montadoEm', label: 'Montagem', render: (row) => displayValue(row.montadoEm) }, { key: 'desmontadoEm', label: 'Desmontagem', render: (row) => displayValue(row.desmontadoEm) }
    ]} emptyTitle="Nenhum equipamento associado" /></Section>

    {unit.refrigerado && <Section title="Controle reefer"><DetailTable rows={detail.reefer} columns={[
      { key: 'lidoEm', label: 'Leitura', render: (row) => displayValue(row.lidoEm) }, { key: 'setpointC', label: 'Setpoint °C' },
      { key: 'temperaturaSupplyC', label: 'Supply °C' }, { key: 'temperaturaReturnC', label: 'Return °C' },
      { key: 'umidadePercentual', label: 'Umidade %' }, { key: 'ligado', label: 'Ligado', render: (row) => row.ligado ? 'Sim' : 'Não' },
      { key: 'alarme', label: 'Alarme' }
    ]} emptyTitle="Nenhuma leitura reefer" /></Section>}

    <Section title="Histórico de atributos"><DetailTable rows={detail.historicoAtributos} columns={[
      { key: 'alteradoEm', label: 'Data', render: (row) => displayValue(row.alteradoEm) }, { key: 'atributo', label: 'Atributo' },
      { key: 'valorAnterior', label: 'Anterior' }, { key: 'valorAtual', label: 'Atual' }, { key: 'origem', label: 'Origem' }, { key: 'responsavel', label: 'Responsável' }
    ]} emptyTitle="Nenhuma alteração registrada" /></Section>
  </Section>;
}

export function YardInventoryPage() {
  const [draftFilters, setDraftFilters] = useState(emptyInventoryFilters);
  const [filters, setFilters] = useState(emptyInventoryFilters);
  const [selectedId, setSelectedId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');
  const [createDraft, setCreateDraft] = useState({ identificacao: '', tipoEquipamentoCodigo: '', proprietario: '', operador: '', posicaoAtual: '' });
  const [createError, setCreateError] = useState('');
  const [creating, setCreating] = useState(false);

  const remote = useRemote(
    () => inventoryReportsApi.obterInventarioCanonico(filters),
    [filters.identificacao, filters.categoria, filters.estado, filters.condicao, filters.proprietario, filters.operador, filters.somenteComHold, filters.somenteReefer]
  );
  const typesRemote = useRemote(() => inventoryReportsApi.listarTiposInventario(), []);
  const rows = useMemo(() => selectCanonicalInventoryRows(remote.data), [remote.data]);
  const summary = remote.data?.resumo ?? {};

  const loadDetail = useCallback(async (unidadeId) => {
    setSelectedId(unidadeId);
    setDetailLoading(true);
    setDetailError('');
    try {
      const response = await inventoryReportsApi.obterUnidadeInventario(unidadeId);
      setDetail(response);
      return response;
    } catch (reason) {
      setDetail(null);
      setDetailError(formatError(reason));
      return null;
    } finally {
      setDetailLoading(false);
    }
  }, []);

  function submit(event) {
    event.preventDefault();
    setFilters({ ...draftFilters });
  }

  function clear() {
    const empty = emptyInventoryFilters();
    setDraftFilters(empty);
    setFilters(empty);
  }

  async function createUnit(event) {
    event.preventDefault();
    setCreating(true);
    setCreateError('');
    try {
      const response = await inventoryReportsApi.criarUnidadeInventario({ ...createDraft, estado: 'PRE_AVISADA', condicao: 'OPERACIONAL' });
      setCreateDraft({ identificacao: '', tipoEquipamentoCodigo: '', proprietario: '', operador: '', posicaoAtual: '' });
      await remote.reload();
      if (response?.unidade?.id) await loadDetail(response.unidade.id);
    } catch (reason) {
      setCreateError(formatError(reason));
    } finally {
      setCreating(false);
    }
  }

  return <>
    <PageHeader
      eyebrow="Pátio · Inventory Management"
      title="Inventário canônico de unidades"
      description="Contêineres, chassis, carretas e acessórios em uma visão única de ciclo de vida, ownership, condição, montagem, documentos, restrições e inventário físico."
      actions={<button type="button" className="secondary" onClick={remote.reload}>Atualizar</button>}
    />
    <Message type="error">{remote.error}</Message>

    <Section title="Filtros operacionais">
      <form className="planner-selection-grid" onSubmit={submit}>
        <label className="field"><span>Identificação</span><input value={draftFilters.identificacao} onChange={(event) => setDraftFilters((current) => ({ ...current, identificacao: event.target.value }))} placeholder="Contêiner, chassi, carreta ou acessório" maxLength="40" /></label>
        <label className="field"><span>Categoria</span><select value={draftFilters.categoria} onChange={(event) => setDraftFilters((current) => ({ ...current, categoria: event.target.value }))}><option value="">Todas</option><option>CONTEINER</option><option>CHASSI</option><option>CARRETA</option><option>ACESSORIO</option></select></label>
        <label className="field"><span>Estado</span><select value={draftFilters.estado} onChange={(event) => setDraftFilters((current) => ({ ...current, estado: event.target.value }))}><option value="">Todos</option>{['PRE_AVISADA', 'ATIVA', 'NO_PATIO', 'EM_OPERACAO', 'EM_TRANSITO', 'EMBARCADA', 'DESEMBARCADA', 'LIBERADA', 'DESPACHADA', 'INATIVA', 'APOSENTADA'].map((value) => <option key={value}>{value}</option>)}</select></label>
        <label className="field"><span>Condição</span><select value={draftFilters.condicao} onChange={(event) => setDraftFilters((current) => ({ ...current, condicao: event.target.value }))}><option value="">Todas</option>{['OPERACIONAL', 'AVARIADO', 'INOPERANTE', 'EM_INSPECAO', 'EM_REPARO', 'AGUARDANDO_PECA'].map((value) => <option key={value}>{value}</option>)}</select></label>
        <label className="field"><span>Proprietário</span><input value={draftFilters.proprietario} onChange={(event) => setDraftFilters((current) => ({ ...current, proprietario: event.target.value }))} /></label>
        <label className="field"><span>Operador</span><input value={draftFilters.operador} onChange={(event) => setDraftFilters((current) => ({ ...current, operador: event.target.value }))} /></label>
        <label className="field"><span>Exceções</span><span><input type="checkbox" checked={draftFilters.somenteComHold} onChange={(event) => setDraftFilters((current) => ({ ...current, somenteComHold: event.target.checked }))} /> Somente com hold</span></label>
        <label className="field"><span>Reefer</span><span><input type="checkbox" checked={draftFilters.somenteReefer} onChange={(event) => setDraftFilters((current) => ({ ...current, somenteReefer: event.target.checked }))} /> Somente refrigerados</span></label>
        <div className="field"><span>Ações</span><div className="actions"><button type="submit">Consultar</button><button type="button" className="secondary" onClick={clear}>Limpar</button></div></div>
      </form>
    </Section>

    <Section title="Cadastrar unidade ou equipamento" description="O tipo define categoria, dimensões, ISO, capacidade, reefer e equivalências.">
      <Message type="error">{createError || typesRemote.error}</Message>
      <form className="planner-selection-grid" onSubmit={createUnit}>
        <label className="field"><span>Identificação</span><input required value={createDraft.identificacao} onChange={(event) => setCreateDraft((current) => ({ ...current, identificacao: event.target.value }))} maxLength="40" /></label>
        <label className="field"><span>Tipo de equipamento</span><select required value={createDraft.tipoEquipamentoCodigo} onChange={(event) => setCreateDraft((current) => ({ ...current, tipoEquipamentoCodigo: event.target.value }))}><option value="">Selecione</option>{(typesRemote.data ?? []).filter((type) => type.ativo).map((type) => <option key={type.id} value={type.codigo}>{type.codigo} · {type.descricao}</option>)}</select></label>
        <label className="field"><span>Proprietário</span><input value={createDraft.proprietario} onChange={(event) => setCreateDraft((current) => ({ ...current, proprietario: event.target.value }))} /></label>
        <label className="field"><span>Operador</span><input value={createDraft.operador} onChange={(event) => setCreateDraft((current) => ({ ...current, operador: event.target.value }))} /></label>
        <label className="field"><span>Posição inicial</span><input value={createDraft.posicaoAtual} onChange={(event) => setCreateDraft((current) => ({ ...current, posicaoAtual: event.target.value }))} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={creating || !createDraft.identificacao || !createDraft.tipoEquipamentoCodigo}>{creating ? 'Cadastrando...' : 'Cadastrar unidade'}</button></div>
      </form>
    </Section>

    {remote.loading ? <Loading label="Carregando inventário canônico..." /> : <>
      <div className="metrics-grid">
        <MetricCard label="Unidades" value={summary.totalUnidades ?? 0} />
        <MetricCard label="Contêineres" value={summary.totalConteiners ?? 0} />
        <MetricCard label="Chassis" value={summary.totalChassis ?? 0} />
        <MetricCard label="Carretas" value={summary.totalCarretas ?? 0} />
        <MetricCard label="Acessórios" value={summary.totalAcessorios ?? 0} />
        <MetricCard label="No pátio" value={summary.totalNoPatio ?? 0} />
        <MetricCard label="Com hold" value={summary.totalComHold ?? 0} />
        <MetricCard label="Avariadas" value={summary.totalAvariadas ?? 0} />
        <MetricCard label="Em manutenção" value={summary.totalEmManutencao ?? 0} />
        <MetricCard label="Reefer" value={summary.totalReefer ?? 0} />
        <MetricCard label="Montadas" value={summary.totalMontadas ?? 0} />
        <MetricCard label="Divergências físicas" value={summary.totalDivergenciasAbertas ?? 0} detail={summary.atualizadoEm ? `Atualizado em ${displayValue(summary.atualizadoEm)}` : undefined} />
      </div>
      <Section title="Unidades e equipamentos">
        {rows.length ? <DataTable
          rows={rows}
          rowKey="id"
          gridId="inventory-management-canonico"
          exportFileName="inventario-canonico"
          onRowClick={(row) => loadDetail(row.id)}
          inspectable={false}
          columns={[
            { key: 'identificacao', label: 'Unidade' },
            { key: 'categoria', label: 'Categoria', render: (row) => <StatusBadge value={row.categoria} /> },
            { key: 'tipoEquipamentoCodigo', label: 'Tipo' },
            { key: 'codigoIso', label: 'ISO' },
            { key: 'estado', label: 'Ciclo de vida', render: (row) => <StatusBadge value={row.estado} /> },
            { key: 'condicao', label: 'Condição', render: (row) => <StatusBadge value={row.condicao} /> },
            { key: 'posicaoAtual', label: 'Posição real' },
            { key: 'posicaoPlanejada', label: 'Planejada' },
            { key: 'proprietario', label: 'Proprietário' },
            { key: 'operador', label: 'Operador' },
            { key: 'holdsAtivos', label: 'Holds' },
            { key: 'avariasAbertas', label: 'Avarias' },
            { key: 'equipamentosVinculados', label: 'Vínculos' },
            { key: 'atualizadoEm', label: 'Atualizado', render: (row) => displayValue(row.atualizadoEm) }
          ]}
          emptyTitle="Nenhuma unidade encontrada"
        /> : <EmptyState title="Nenhuma unidade encontrada" description="Altere os filtros ou cadastre uma unidade no inventário." />}
      </Section>
    </>}

    {selectedId && <UnitInspector detail={detail} loading={detailLoading} error={detailError} onClose={() => { setSelectedId(null); setDetail(null); }} onReload={loadDetail} />}
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
