import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, formatError, getRuntimeConfig, normalizePage, sanitizeText } from '../api.js';
import { DataTable, EmptyState, JsonDetails, Loading, Message, MetricCard, PageHeader, Section, StatusBadge } from '../components.jsx';

function displayValue(value) {
  if (value === undefined || value === null || value === '') return '—';
  if (typeof value === 'boolean') return value ? 'Sim' : 'Não';
  if (typeof value === 'object') return Array.isArray(value) ? `${value.length} item(ns)` : JSON.stringify(value);
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)) {
    const date = new Date(value);
    if (!Number.isNaN(date.getTime())) return date.toLocaleString('pt-BR');
  }
  return String(value);
}

function inferColumns(rows, preferred = []) {
  if (!rows.length) return [];
  const keys = Array.from(new Set(rows.slice(0, 10).flatMap((row) => Object.keys(row ?? {}))))
    .filter((key) => !preferred.includes(key));
  return [...preferred, ...keys].slice(0, 8).map((key) => ({
    key,
    label: key.replace(/([A-Z])/g, ' $1').replace(/[_-]+/g, ' ').replace(/^./, (letter) => letter.toUpperCase()),
    render: (row) => /status|fase|severidade|nivel/i.test(key)
      ? <StatusBadge value={row?.[key]} />
      : displayValue(row?.[key])
  }));
}

function useLoader(loader, dependencies = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const reload = useCallback(async () => {
    setLoading(true); setError('');
    try { const response = await loader(); setData(response); return response; }
    catch (reason) { setError(formatError(reason)); setData(null); return null; }
    finally { setLoading(false); }
  }, dependencies);
  useEffect(() => { reload(); }, [reload]);
  return { data, loading, error, reload };
}

export function HomeDashboard({ navigate }) {
  const visibility = useLoader(() => api.obterDashboardVisibilidade(), []);
  const cards = [
    { title: 'Gate', description: 'Agendamentos, janelas, operação e relatórios.', route: '/home/gate/agendamentos' },
    { title: 'Ferrovia', description: 'Visitas, manifestos e listas de trabalho.', route: '/home/ferrovia/visitas' },
    { title: 'Pátio', description: 'Mapa, posições, movimentos, recursos e automação.', route: '/home/patio/mapa' },
    { title: 'Navio', description: 'Control Room integrado Navio + Pátio.', route: '/home/navio/control-room' },
    { title: 'Embarque', description: 'Planejamento e acompanhamento de estiva.', route: '/home/embarque/planejamento' }
  ];
  const dashboard = visibility.data ?? {};
  return <>
    <PageHeader eyebrow="Operação portuária" title="Visão geral" description="Acesso centralizado aos domínios operacionais do CloudPort." actions={<button className="secondary" onClick={visibility.reload}>Atualizar indicadores</button>} />
    <Message type="error">{visibility.error}</Message>
    {visibility.loading ? <Loading label="Carregando indicadores..." /> : <div className="metrics-grid">
      <MetricCard label="Contêineres" value={dashboard.totalConteiners ?? dashboard.totalContainers ?? '—'} />
      <MetricCard label="Alertas ativos" value={dashboard.alertasAtivos ?? dashboard.totalAlertas ?? '—'} />
      <MetricCard label="Navios em operação" value={dashboard.naviosEmOperacao ?? dashboard.totalNavios ?? '—'} />
      <MetricCard label="Ocupação do pátio" value={dashboard.ocupacaoPatioPercentual !== undefined ? `${dashboard.ocupacaoPatioPercentual}%` : '—'} />
    </div>}
    <Section title="Módulos operacionais"><div className="module-grid">{cards.map((card) => <button className="module-card" key={card.route} onClick={() => navigate(card.route)}><strong>{card.title}</strong><span>{card.description}</span><small>Abrir módulo →</small></button>)}</div></Section>
  </>;
}

export function GenericDatasetPage({ eyebrow, title, description, loader, preferredColumns = [], emptyTitle }) {
  const remote = useLoader(loader, [loader]);
  const rows = useMemo(() => normalizePage(remote.data), [remote.data]);
  const columns = useMemo(() => inferColumns(rows, preferredColumns), [rows, preferredColumns]);
  return <>
    <PageHeader eyebrow={eyebrow} title={title} description={description} actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error}</Message>
    <Section title="Registros">
      {remote.loading ? <Loading /> : rows.length ? <DataTable rows={rows} columns={columns} rowKey={(row, index) => row.id ?? row.codigo ?? row.identificador ?? index} /> : <EmptyState title={emptyTitle ?? 'Nenhum registro encontrado'} />}
    </Section>
    <JsonDetails value={remote.data && !rows.length ? remote.data : null} title="Resposta recebida" />
  </>;
}

export function GateDashboardPage() {
  const central = useLoader(() => api.obterCentralGate(), []);
  const appointments = central.data?.agendamentos ?? [];
  return <>
    <PageHeader eyebrow="Gate" title="Central de ação" description="Acompanhamento dos próximos agendamentos e ações disponíveis." actions={<button className="secondary" onClick={central.reload}>Atualizar</button>} />
    <Message type="error">{central.error}</Message>
    {central.loading ? <Loading /> : <>
      <div className="metrics-grid">
        <MetricCard label="Agendamentos" value={appointments.length} />
        <MetricCard label="Transportadora" value={central.data?.usuario?.transportadoraNome || 'Todas'} />
        <MetricCard label="Situação do pátio" value={central.data?.situacaoPatio?.status || '—'} detail={central.data?.situacaoPatio?.descricao} />
        <MetricCard label="Atualizado em" value={central.data?.situacaoPatio?.verificadoEm ? new Date(central.data.situacaoPatio.verificadoEm).toLocaleString('pt-BR') : '—'} />
      </div>
      <Section title="Agendamentos prioritários"><DataTable rows={appointments} columns={[
        { key: 'codigo', label: 'Código' },
        { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'tipoOperacaoDescricao', label: 'Operação' },
        { key: 'horarioPrevistoChegada', label: 'Chegada', render: (row) => displayValue(row.horarioPrevistoChegada) },
        { key: 'placaVeiculo', label: 'Placa' },
        { key: 'transportadoraNome', label: 'Transportadora' },
        { key: 'acaoPrincipal', label: 'Ação', render: (row) => row.acaoPrincipal?.titulo || '—' }
      ]} emptyTitle="Nenhum agendamento prioritário" /></Section>
    </>}
  </>;
}

export function RailVisitsPage() {
  const [days, setDays] = useState(7);
  const remote = useLoader(() => api.listarVisitasFerrovia(days), [days]);
  const rows = normalizePage(remote.data);
  return <>
    <PageHeader eyebrow="Ferrovia" title="Visitas de trem" description="Visitas previstas e situação das listas de carga e descarga." actions={<div className="inline"><label className="compact-field">Janela<select value={days} onChange={(event) => setDays(Number(event.target.value))}><option value="1">1 dia</option><option value="7">7 dias</option><option value="15">15 dias</option><option value="30">30 dias</option></select></label><button className="secondary" onClick={remote.reload}>Atualizar</button></div>} />
    <Message type="error">{remote.error}</Message>
    <Section title="Visitas"><DataTable rows={rows} columns={[
      { key: 'identificadorTrem', label: 'Trem' },
      { key: 'operadoraFerroviaria', label: 'Operadora' },
      { key: 'statusVisita', label: 'Status', render: (row) => <StatusBadge value={row.statusVisita} /> },
      { key: 'horaChegadaPrevista', label: 'Chegada', render: (row) => displayValue(row.horaChegadaPrevista) },
      { key: 'horaPartidaPrevista', label: 'Partida', render: (row) => displayValue(row.horaPartidaPrevista) },
      { key: 'descarga', label: 'Descarga', render: (row) => `${row.listaDescarga?.length ?? 0} contêiner(es)` },
      { key: 'carga', label: 'Carga', render: (row) => `${row.listaCarga?.length ?? 0} contêiner(es)` }
    ]} emptyTitle="Nenhuma visita prevista" /></Section>
  </>;
}

export function RailImportPage() {
  const [file, setFile] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  async function submit(event) {
    event.preventDefault();
    if (!file || busy) return;
    setBusy(true); setError(''); setResult(null);
    try { setResult(await api.importarManifestoFerrovia(file)); setFile(null); event.currentTarget.reset(); }
    catch (reason) { setError(formatError(reason)); }
    finally { setBusy(false); }
  }
  return <>
    <PageHeader eyebrow="Ferrovia" title="Importar manifesto" description="Envie o manifesto operacional para criar ou atualizar a visita ferroviária." />
    <Message type="error">{error}</Message><Message type="success">{result ? 'Manifesto importado com sucesso.' : ''}</Message>
    <Section title="Arquivo de manifesto"><form className="upload-form" onSubmit={submit}><input type="file" onChange={(event) => setFile(event.target.files?.[0] ?? null)} required /><button disabled={!file || busy}>{busy ? 'Enviando...' : 'Importar'}</button></form></Section>
    <JsonDetails value={result} />
  </>;
}

export function YardMapPage() {
  const remote = useLoader(() => api.obterMapaPatio({}), []);
  const map = remote.data ?? {};
  const containers = map.conteineres ?? [];
  const equipment = map.equipamentos ?? [];
  return <>
    <PageHeader eyebrow="Pátio" title="Mapa operacional" description="Ocupação, equipamentos e alertas do pátio." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error}</Message>
    {remote.loading ? <Loading /> : <>
      <div className="metrics-grid"><MetricCard label="Contêineres" value={containers.length} /><MetricCard label="Equipamentos" value={equipment.length} /><MetricCard label="Linhas" value={map.totalLinhas ?? '—'} /><MetricCard label="Colunas" value={map.totalColunas ?? '—'} /></div>
      <Section title="Contêineres no mapa"><DataTable rows={containers} columns={[
        { key: 'codigo', label: 'Contêiner' }, { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> }, { key: 'linha', label: 'Linha' }, { key: 'coluna', label: 'Coluna' }, { key: 'camadaOperacional', label: 'Camada' }, { key: 'destino', label: 'Destino' }, { key: 'tipoCarga', label: 'Carga' }
      ]} emptyTitle="Mapa sem contêineres" /></Section>
      <Section title="Equipamentos"><DataTable rows={equipment} columns={[
        { key: 'identificador', label: 'Equipamento' }, { key: 'tipoEquipamento', label: 'Tipo' }, { key: 'statusOperacional', label: 'Status', render: (row) => <StatusBadge value={row.statusOperacional} /> }, { key: 'linha', label: 'Linha' }, { key: 'coluna', label: 'Coluna' }
      ]} emptyTitle="Nenhum equipamento no mapa" /></Section>
      {!!map.alertas?.length && <Section title="Alertas"><div className="card-list">{map.alertas.map((alert, index) => <article className="content-card" key={`${alert.tipoAlerta}-${index}`}><div className="card-meta"><StatusBadge value={alert.nivelSeveridade} /><span>{alert.tipoAlerta}</span></div><h3>{sanitizeText(alert.mensagem)}</h3><p>{sanitizeText(alert.recomendacao)}</p></article>)}</div></Section>}
    </>}
  </>;
}

export function ShippingPage() {
  const [days, setDays] = useState(30);
  const remote = useLoader(() => api.listarEscalasEmbarque(days), [days]);
  const rows = normalizePage(remote.data);
  return <>
    <PageHeader eyebrow="Embarque" title="Planejamento de estiva" description="Escalas disponíveis para planejamento e acompanhamento do embarque." actions={<select value={days} onChange={(event) => setDays(Number(event.target.value))}><option value="7">7 dias</option><option value="15">15 dias</option><option value="30">30 dias</option><option value="60">60 dias</option></select>} />
    <Message type="error">{remote.error}</Message>
    <Section title="Escalas"><DataTable rows={rows} columns={[
      { key: 'nomeNavio', label: 'Navio' }, { key: 'codigoImo', label: 'IMO' }, { key: 'viagemEntrada', label: 'Viagem' }, { key: 'fase', label: 'Fase', render: (row) => <StatusBadge value={row.fase} /> }, { key: 'chegadaPrevista', label: 'Chegada', render: (row) => displayValue(row.chegadaPrevista) }, { key: 'bercoPrevisto', label: 'Berço' }
    ]} emptyTitle="Nenhuma escala disponível" /></Section>
  </>;
}

export function ControlRoomPage({ session }) {
  const iframeRef = useRef(null);
  const [error, setError] = useState('');
  const url = getRuntimeConfig().navioControlRoomUrl;
  const targetOrigin = useMemo(() => {
    try { return url ? new URL(url, window.location.href).origin : ''; }
    catch { return ''; }
  }, [url]);

  const sendSession = useCallback(() => {
    if (!iframeRef.current?.contentWindow || !targetOrigin || !session?.token) return;
    iframeRef.current.contentWindow.postMessage({
      type: 'CLOUDPORT_AUTH_SESSION',
      session: { token: session.token, nome: session.nome, roles: session.roles ?? [] }
    }, targetOrigin);
  }, [session, targetOrigin]);

  useEffect(() => {
    const listener = (event) => {
      if (event.origin === targetOrigin && event.data?.type === 'CLOUDPORT_CONTROL_ROOM_READY') sendSession();
    };
    window.addEventListener('message', listener);
    return () => window.removeEventListener('message', listener);
  }, [sendSession, targetOrigin]);

  if (!url || !targetOrigin) return <><PageHeader eyebrow="Navio" title="Control Room" description="Integração operacional Navio + Pátio." /><Message type="error">A URL do Control Room não foi configurada em assets/configuracao.json.</Message></>;

  return <>
    <PageHeader eyebrow="Navio" title="Control Room" description="Painel React incorporado com sessão única do portal." actions={<button className="secondary" onClick={sendSession}>Reenviar sessão</button>} />
    <Message type="error">{error}</Message>
    <section className="iframe-panel"><iframe ref={iframeRef} src={url} title="Control Room Navio e Pátio" onLoad={sendSession} onError={() => setError('Não foi possível carregar o Control Room.')} /></section>
  </>;
}

export const DATASET_ROUTES = {
  '/home/gate/agendamentos': { eyebrow: 'Gate', title: 'Agendamentos', description: 'Agendamentos operacionais do gate.', loader: () => api.listarGateAgendamentos(), preferredColumns: ['codigo', 'status', 'tipoOperacao', 'placaVeiculo', 'transportadoraNome'] },
  '/home/gate/janelas': { eyebrow: 'Gate', title: 'Janelas de atendimento', description: 'Janelas configuradas para recebimento e expedição.', loader: () => api.listarGateJanelas(), preferredColumns: ['id', 'data', 'horaInicio', 'horaFim', 'capacidade', 'status'] },
  '/home/gate/relatorios': { eyebrow: 'Gate', title: 'Relatórios', description: 'Visão consolidada do gate.', loader: () => api.obterCentralGate(), preferredColumns: ['codigo', 'status', 'tipoOperacaoDescricao'] },
  '/home/gate/operador': { eyebrow: 'Gate', title: 'Console do operador', description: 'Fila operacional e situação dos agendamentos.', loader: () => api.obterCentralGate(), preferredColumns: ['codigo', 'status', 'mensagemOrientacao'] },
  '/home/gate/operador/console': { eyebrow: 'Gate', title: 'Console do operador', description: 'Fila operacional e situação dos agendamentos.', loader: () => api.obterCentralGate(), preferredColumns: ['codigo', 'status', 'mensagemOrientacao'] },
  '/home/gate/operador/eventos': { eyebrow: 'Gate', title: 'Eventos do operador', description: 'Eventos vinculados aos agendamentos.', loader: () => api.obterCentralGate(), preferredColumns: ['codigo', 'status', 'horarioPrevistoChegada'] },
  '/home/patio/posicoes': { eyebrow: 'Pátio', title: 'Posições', description: 'Posições físicas e ocupação atual.', loader: () => api.listarPosicoesPatio(), preferredColumns: ['id', 'linha', 'coluna', 'camadaOperacional', 'ocupada', 'codigoConteiner'] },
  '/home/patio/movimentacoes': { eyebrow: 'Pátio', title: 'Movimentações', description: 'Histórico recente de movimentos no pátio.', loader: () => api.listarMovimentacoesPatio(), preferredColumns: ['id', 'codigoConteiner', 'tipoMovimento', 'destino', 'registradoEm'] },
  '/home/patio/movimentacao': { eyebrow: 'Pátio', title: 'Contêineres', description: 'Contêineres disponíveis para movimentação.', loader: () => api.listarConteineresPatio(), preferredColumns: ['codigo', 'status', 'linha', 'coluna', 'destino'] },
  '/home/patio/recursos': { eyebrow: 'Pátio', title: 'Recursos', description: 'Recursos e equipamentos disponíveis.', loader: () => api.listarRecursosPatio(), preferredColumns: ['identificador', 'tipo', 'status', 'localizacao'] },
  '/home/patio/lista-trabalho': { eyebrow: 'Pátio', title: 'Lista de trabalho', description: 'Ordens operacionais disponíveis no pátio.', loader: () => api.listarMovimentacoesPatio(), preferredColumns: ['id', 'codigoConteiner', 'tipoMovimento', 'descricao'] },
  '/home/patio/dashboard-kpi': { eyebrow: 'Pátio', title: 'Indicadores', description: 'Indicadores operacionais derivados do mapa atual.', loader: () => api.obterMapaPatio({}), preferredColumns: [] },
  '/home/patio/automacao': { eyebrow: 'Pátio', title: 'Automação', description: 'Situação operacional usada pelo planejador automático.', loader: () => api.obterMapaPatio({}), preferredColumns: [] },
  '/home/patio/simulador': { eyebrow: 'Pátio', title: 'Simulador', description: 'Dados atuais usados na simulação de movimentos.', loader: () => api.obterMapaPatio({}), preferredColumns: [] },
  '/home/ferrovia/lista-trabalho': { eyebrow: 'Ferrovia', title: 'Lista de trabalho', description: 'Visitas com operações ferroviárias pendentes.', loader: () => api.listarVisitasFerrovia(30), preferredColumns: ['identificadorTrem', 'statusVisita', 'horaChegadaPrevista'] },
  '/home/embarque/steel-coils': { eyebrow: 'Embarque', title: 'Steel coils', description: 'Escalas e planejamento de cargas siderúrgicas.', loader: () => api.listarEscalasEmbarque(30), preferredColumns: ['nomeNavio', 'codigoImo', 'viagemEntrada', 'fase'] }
};
