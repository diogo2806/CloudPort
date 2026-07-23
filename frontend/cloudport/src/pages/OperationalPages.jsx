import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, formatError, getRuntimeConfig, normalizePage, readSession } from '../api.js';
import { DataTable, EmptyState, JsonDetails, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';
import { selectGateAppointments } from '../gateDataset.js';
import { gateOperatorApi, selectGateOperatorVehicles } from '../gateOperatorApi.js';
import { OperationalCockpit } from '../OperationalCockpit.jsx';
import { collectDatasetKeys, humanizeDatasetKey } from '../operationalDataset.js';
import { GateVisualPage } from './GateVisualPage.jsx';
import { NavioOperationalConsole } from './NavioOperationalConsole.jsx';
import { RailLineUpPage } from './RailLineUpPage.jsx';
import { RailRegistrationsPage } from './RailRegistrationsPage.jsx';
import { YardInstructionCreateModal } from './yard/YardInstructionsPage.jsx';

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
  return collectDatasetKeys(rows, preferred).map((key) => ({
    key,
    label: humanizeDatasetKey(key),
    render: (row) => /status|fase|severidade|nivel/i.test(key) ? <StatusBadge value={row?.[key]} /> : displayValue(row?.[key])
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

export function HomeDashboard({ navigate, session }) {
  const [activeSession] = useState(() => session ?? readSession() ?? {});
  return <OperationalCockpit navigate={navigate} session={activeSession} />;
}

export function GenericDatasetPage({ eyebrow, title, description, loader, preferredColumns = [], emptyTitle }) {
  const remote = useLoader(loader, [loader]);
  const rows = useMemo(() => normalizePage(remote.data), [remote.data]);
  const columns = useMemo(() => inferColumns(rows, preferredColumns), [rows, preferredColumns]);
  return <>
    <PageHeader eyebrow={eyebrow} title={title} description={description} actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error}</Message>
    <Section title="Registros">{remote.loading ? <Loading /> : rows.length ? <DataTable rows={rows} columns={columns} gridId={`${eyebrow || 'operacao'}-${title}`} exportFileName={title} rowKey={(row, index) => row.id ?? row.codigo ?? row.identificador ?? index} /> : <EmptyState title={emptyTitle ?? 'Nenhum registro encontrado'} />}</Section>
    <JsonDetails value={remote.data && !rows.length ? remote.data : null} title="Resposta recebida" />
  </>;
}

export function GateDashboardPage() {
  return <GateVisualPage />;
}

export function RailVisitsPage({ session }) {
  return <RailRegistrationsPage session={session} />;
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
  return <><PageHeader eyebrow="Ferrovia" title="Importar manifesto" description="Envie o manifesto operacional para criar ou atualizar a visita ferroviária." /><Message type="error">{error}</Message><Message type="success">{result ? 'Manifesto importado com sucesso.' : ''}</Message><Section title="Arquivo de manifesto"><form className="upload-form" onSubmit={submit}><input type="file" onChange={(event) => setFile(event.target.files?.[0] ?? null)} required /><button disabled={!file || busy}>{busy ? 'Enviando...' : 'Importar'}</button></form></Section><JsonDetails value={result} /></>;
}

export function ShippingPage() {
  const [days, setDays] = useState(30);
  const remote = useLoader(() => api.listarEscalasEmbarque(days), [days]);
  const rows = normalizePage(remote.data);
  return <><PageHeader eyebrow="Embarque" title="Planejamento de estiva" description="Escalas disponíveis para planejamento e acompanhamento do embarque." actions={<select value={days} onChange={(event) => setDays(Number(event.target.value))}><option value="7">7 dias</option><option value="15">15 dias</option><option value="30">30 dias</option><option value="60">60 dias</option></select>} /><Message type="error">{remote.error}</Message><Section title="Escalas"><DataTable rows={rows} columns={[{ key: 'nomeNavio', label: 'Navio' }, { key: 'codigoImo', label: 'IMO' }, { key: 'viagemEntrada', label: 'Viagem' }, { key: 'fase', label: 'Fase', render: (row) => <StatusBadge value={row.fase} /> }, { key: 'chegadaPrevista', label: 'Chegada', render: (row) => displayValue(row.chegadaPrevista) }, { key: 'bercoPrevisto', label: 'Berço' }]} emptyTitle="Nenhuma escala disponível" /></Section></>;
}

export function ControlRoomPage({ session }) {
  const iframeRef = useRef(null);
  const [error, setError] = useState('');
  const [instructionOpen, setInstructionOpen] = useState(false);
  const [instructionMessage, setInstructionMessage] = useState('');
  const url = getRuntimeConfig().navioControlRoomUrl;
  const targetOrigin = useMemo(() => { try { return url ? new URL(url, window.location.href).origin : ''; } catch { return ''; } }, [url]);
  const sendSession = useCallback(() => {
    if (!iframeRef.current?.contentWindow || !targetOrigin || !session?.token) return;
    iframeRef.current.contentWindow.postMessage({ type: 'CLOUDPORT_AUTH_SESSION', session: { token: session.token, nome: session.nome, roles: session.roles ?? [] } }, targetOrigin);
  }, [session, targetOrigin]);
  useEffect(() => {
    const listener = (event) => { if (event.origin === targetOrigin && event.data?.type === 'CLOUDPORT_CONTROL_ROOM_READY') sendSession(); };
    window.addEventListener('message', listener);
    return () => window.removeEventListener('message', listener);
  }, [sendSession, targetOrigin]);
  const actions = <div className="actions"><button type="button" onClick={() => setInstructionOpen(true)}>Nova instrução</button>{url && targetOrigin && <button className="secondary" onClick={sendSession}>Reenviar sessão</button>}</div>;
  const operational = <NavioOperationalConsole session={session} />;
  if (!url || !targetOrigin) return <><PageHeader eyebrow="Navio" title="Control Room" description="Prontidão do berço, eventos de guindaste e integração Navio + Pátio." actions={actions} />{operational}<Message type="error">A URL do monitor incorporado não foi configurada em assets/configuracao.json. O console operacional permanece disponível.</Message><Message type="success">{instructionMessage}</Message><YardInstructionCreateModal open={instructionOpen} session={session} onClose={() => setInstructionOpen(false)} onCreated={() => setInstructionMessage('Instrução criada com sucesso.')} /></>;
  return <><PageHeader eyebrow="Navio" title="Control Room" description="Prontidão bloqueante, execução por guindaste e monitor incorporado com sessão única." actions={actions} />{operational}<Message type="error">{error}</Message><Message type="success">{instructionMessage}</Message><section className="iframe-panel"><iframe ref={iframeRef} src={url} title="Control Room Navio e Pátio" onLoad={sendSession} onError={() => setError('Não foi possível carregar o Control Room.')} /></section><YardInstructionCreateModal open={instructionOpen} session={session} onClose={() => setInstructionOpen(false)} onCreated={() => setInstructionMessage('Instrução criada com sucesso.')} /></>;
}

const loadGateAppointments = () => api.obterCentralGate().then(selectGateAppointments);
const loadGateOperatorVehicles = () => gateOperatorApi.obterPainel().then(selectGateOperatorVehicles);

export const DATASET_ROUTES = {
  '/home/gate/agendamentos': { eyebrow: 'Gate', title: 'Agendamentos', description: 'Agendamentos operacionais do gate.', loader: () => api.listarGateAgendamentos(), preferredColumns: ['codigo', 'status', 'tipoOperacao', 'placaVeiculo', 'transportadoraNome'] },
  '/home/gate/janelas': { eyebrow: 'Gate', title: 'Janelas de atendimento', description: 'Janelas configuradas para recebimento e expedição.', loader: () => api.listarGateJanelas(), preferredColumns: ['id', 'data', 'horaInicio', 'horaFim', 'capacidade', 'status'] },
  '/home/gate/relatorios': { eyebrow: 'Gate', title: 'Relatórios', description: 'Visão consolidada do gate.', loader: loadGateAppointments, preferredColumns: ['codigo', 'status', 'tipoOperacaoDescricao'] },
  '/home/gate/operador': { eyebrow: 'Gate', title: 'Console do operador', description: 'Filas e veículos do painel operacional.', loader: loadGateOperatorVehicles, preferredColumns: ['placa', 'statusDescricao', 'motorista', 'transportadora', 'filaOperacional', 'fluxoOperacional', 'tempoFilaMinutos'] },
  '/home/gate/operador/console': { eyebrow: 'Gate', title: 'Console do operador', description: 'Filas e veículos do painel operacional.', loader: loadGateOperatorVehicles, preferredColumns: ['placa', 'statusDescricao', 'motorista', 'transportadora', 'filaOperacional', 'fluxoOperacional', 'tempoFilaMinutos'] },
  '/home/gate/operador/eventos': { eyebrow: 'Gate', title: 'Eventos do operador', description: 'Histórico operacional recente do Gate.', loader: () => gateOperatorApi.listarEventos(), preferredColumns: ['registradoEm', 'tipo', 'nivel', 'descricao', 'placaVeiculo', 'transportadora', 'usuario'] },
  '/home/ferrovia/lista-trabalho': { eyebrow: 'Ferrovia', title: 'Lista de trabalho', description: 'Visitas com operações ferroviárias pendentes.', loader: () => api.listarVisitasFerrovia(30), preferredColumns: ['identificadorTrem', 'statusVisita', 'horaChegadaPrevista'] },
  '/home/embarque/steel-coils': { eyebrow: 'Embarque', title: 'Steel coils', description: 'Escalas e planejamento de cargas siderúrgicas.', loader: () => api.listarEscalasEmbarque(30), preferredColumns: ['nomeNavio', 'codigoImo', 'viagemEntrada', 'fase'] }
};
