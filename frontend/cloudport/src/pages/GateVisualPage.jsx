import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, formatError, normalizePage } from '../api.js';
import { EmptyState, Loading, Message, MetricCard, PageHeader, Section, StatusBadge } from '../components.jsx';
import { gateOperatorApi } from '../gateOperatorApi.js';
import { GateOperationsTabs } from './GateOperationsTabs.jsx';
import {
  buildGateLanes,
  classifyGateSla,
  flattenGateVehicles,
  gateVehicleKey,
  getGateJourney,
  groupGateWindows,
  isGateProblemVehicle
} from '../gateVisualModel.js';

const AUTO_REFRESH_MS = 30000;

function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('pt-BR');
}

function formatDay(value) {
  if (!value || value === 'Sem data') return value || 'Sem data';
  const date = new Date(`${value}T12:00:00`);
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleDateString('pt-BR', { weekday: 'short', day: '2-digit', month: '2-digit' });
}

function formatHour(value) {
  return value ? String(value).slice(0, 5) : '—';
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function vehicleTitle(vehicle) {
  return vehicle?.placa ?? vehicle?.placaVeiculo ?? 'Veículo sem placa';
}

function vehicleStatus(vehicle) {
  return vehicle?.statusDescricao ?? vehicle?.status ?? 'Aguardando';
}

function GateVehicleCard({ vehicle, selected, onSelect }) {
  const sla = classifyGateSla(vehicle?.tempoFilaMinutos);
  const problem = isGateProblemVehicle(vehicle);
  return <button type="button" className={`gate-vehicle-card ${selected ? 'selected' : ''} ${problem ? 'problem' : ''}`} onClick={() => onSelect(vehicle)}>
    <span className="gate-vehicle-top"><strong>{vehicleTitle(vehicle)}</strong><span className={`gate-sla gate-sla-${sla.level}`}>{sla.label}</span></span>
    <span className="gate-vehicle-meta">{vehicle?.motorista || vehicle?.agendamento?.motoristaNome || 'Motorista não informado'}</span>
    <span className="gate-vehicle-meta">{vehicle?.transportadora || vehicle?.agendamento?.transportadoraNome || 'Transportadora não informada'}</span>
    <span className="gate-vehicle-footer"><StatusBadge value={vehicleStatus(vehicle)} />{problem && <small>Requer atenção</small>}</span>
  </button>;
}

function GateLane({ lane, selectedKey, onSelect }) {
  const total = lane.vehicles.length || lane.declaredCount || 0;
  return <article className={`gate-lane gate-lane-${lane.flow.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`}>
    <header className="gate-lane-header"><div><span>{lane.stage}</span><h3>{lane.name}</h3></div><strong>{total}</strong></header>
    <div className="gate-lane-summary"><span>{lane.averageMinutes ? `Média ${lane.averageMinutes} min` : 'Fluxo em tempo real'}</span><span>{lane.flow}</span></div>
    <div className="gate-vehicle-list">
      {lane.vehicles.length ? lane.vehicles.map((vehicle) => <GateVehicleCard key={gateVehicleKey(vehicle) ?? `${lane.id}-${vehicleTitle(vehicle)}`} vehicle={vehicle} selected={selectedKey === gateVehicleKey(vehicle)} onSelect={onSelect} />) : <div className="gate-lane-empty">Pista livre</div>}
    </div>
  </article>;
}

function Journey({ vehicle }) {
  return <div className="gate-journey">{getGateJourney(vehicle).map((stage, index) => <div className={`gate-stage gate-stage-${stage.state}`} key={stage.key}><span className="gate-stage-marker">{stage.state === 'complete' ? '✓' : index + 1}</span><div><strong>{stage.label}</strong><small>{stage.detail}</small></div></div>)}</div>;
}

function DocumentList({ documents }) {
  if (!documents.length) return <EmptyState title="Nenhum documento anexado" description="O painel exibirá OCR, imagens e arquivos assim que forem vinculados ao agendamento." />;
  return <div className="gate-document-list">{documents.map((document, index) => <article key={document?.id ?? index}><div><strong>{document?.tipoDocumento || document?.nomeArquivo || `Documento ${index + 1}`}</strong><small>{document?.numero || document?.nomeArquivo || 'Sem identificação'}</small></div><StatusBadge value={document?.statusValidacao ?? document?.status ?? 'PENDENTE'} /></article>)}</div>;
}

function VehicleInspector({ vehicle, onPrint, printing }) {
  if (!vehicle) return <EmptyState title="Selecione um veículo" description="Clique em um cartão de pista para acompanhar a jornada, os documentos e as ocorrências." />;
  const appointment = vehicle.agendamento ?? {};
  const documents = Array.isArray(vehicle.documentos) ? vehicle.documentos : [];
  const exceptions = Array.isArray(vehicle.excecoes) ? vehicle.excecoes : [];
  const images = documents.filter((document) => String(document?.contentType ?? '').startsWith('image/'));
  const damages = exceptions.filter((exception) => /avaria|damage|dano/i.test(`${exception?.codigo ?? ''} ${exception?.descricao ?? ''}`));
  const canPrint = Boolean(vehicle?.podeImprimirComprovante || vehicle?.gatePass || appointment?.gatePass);

  return <div className="gate-inspector">
    <header><div><span className="eyebrow">Atendimento selecionado</span><h3>{vehicleTitle(vehicle)}</h3><p>{vehicle?.motorista || appointment?.motoristaNome || 'Motorista não informado'} · {vehicle?.transportadora || appointment?.transportadoraNome || 'Transportadora não informada'}</p></div><div className="actions"><button type="button" onClick={() => onPrint(vehicle)} disabled={printing || !canPrint}>{printing ? 'Gerando...' : 'Imprimir / reimprimir EIR'}</button></div></header>
    <div className="gate-inspector-facts"><span><small>Agendamento</small><strong>{appointment?.codigo ?? '—'}</strong></span><span><small>Operação</small><strong>{appointment?.tipoOperacaoDescricao ?? appointment?.tipoOperacao ?? '—'}</strong></span><span><small>Chegada prevista</small><strong>{formatDateTime(appointment?.horarioPrevistoChegada)}</strong></span><span><small>Chegada real</small><strong>{formatDateTime(appointment?.horarioRealChegada)}</strong></span></div>
    <Journey vehicle={vehicle} />
    <div className="gate-inspector-tabs">
      <section><h4>Documentos e OCR</h4><DocumentList documents={documents} /></section>
      <section><h4>Imagens</h4>{images.length ? <div className="gate-media-grid">{images.map((image, index) => <article key={image?.id ?? index}><strong>{image?.nomeArquivo || `Imagem ${index + 1}`}</strong><small>{image?.statusValidacao ?? 'Pendente'}</small></article>)}</div> : <EmptyState title="Sem imagens" />}</section>
      <section><h4>Avarias e ocorrências</h4>{exceptions.length ? <div className="gate-exception-list">{exceptions.map((exception, index) => <article key={`${exception?.codigo ?? 'ocorrencia'}-${index}`}><StatusBadge value={exception?.nivel ?? 'ATENÇÃO'} /><div><strong>{exception?.descricao ?? exception?.codigo ?? 'Ocorrência operacional'}</strong>{damages.includes(exception) && <small>Avaria identificada</small>}</div></article>)}</div> : <EmptyState title="Nenhuma ocorrência registrada" />}</section>
    </div>
  </div>;
}

export function GateVisualPage() {
  const [panel, setPanel] = useState(null);
  const [appointments, setAppointments] = useState([]);
  const [windows, setWindows] = useState([]);
  const [selectedKey, setSelectedKey] = useState(null);
  const [loading, setLoading] = useState(true);
  const [printing, setPrinting] = useState(false);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [error, setError] = useState('');
  const [now, setNow] = useState(() => new Date());

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [panelResponse, appointmentResponse, windowResponse] = await Promise.all([
        gateOperatorApi.obterPainel(),
        api.listarGateAgendamentos({ page: 0, size: 500 }),
        api.listarGateJanelas({ page: 0, size: 500 })
      ]);
      setPanel(panelResponse ?? {});
      setAppointments(normalizePage(appointmentResponse));
      setWindows(normalizePage(windowResponse));
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível carregar a operação visual do gate.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { const timer = setInterval(() => setNow(new Date()), 1000); return () => clearInterval(timer); }, []);
  useEffect(() => { if (!autoRefresh) return undefined; const timer = setInterval(load, AUTO_REFRESH_MS); return () => clearInterval(timer); }, [autoRefresh, load]);

  const lanes = useMemo(() => buildGateLanes(panel, appointments), [panel, appointments]);
  const vehicles = useMemo(() => flattenGateVehicles(lanes), [lanes]);
  const problems = useMemo(() => vehicles.filter((vehicle) => isGateProblemVehicle(vehicle)), [vehicles]);
  const calendar = useMemo(() => groupGateWindows(windows, appointments), [windows, appointments]);
  const selectedVehicle = useMemo(() => vehicles.find((vehicle) => gateVehicleKey(vehicle) === selectedKey) ?? problems[0] ?? vehicles[0] ?? null, [vehicles, problems, selectedKey]);

  useEffect(() => { if (!selectedKey && (problems[0] || vehicles[0])) setSelectedKey(gateVehicleKey(problems[0] ?? vehicles[0])); }, [problems, vehicles, selectedKey]);

  async function printEir(vehicle) {
    const popup = window.open('', '_blank', 'width=850,height=950');
    if (!popup) { setError('O navegador bloqueou a janela de impressão do EIR. Libere pop-ups para o CloudPort.'); return; }
    popup.document.write('<p style="font-family:sans-serif;padding:24px">Gerando EIR...</p>');
    setPrinting(true);
    setError('');
    try {
      const receipt = await gateOperatorApi.obterComprovante(vehicle.id);
      popup.document.open();
      popup.document.write(`<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>EIR ${escapeHtml(vehicleTitle(vehicle))}</title><style>body{font-family:Arial,sans-serif;margin:32px;color:#172033}header{border-bottom:2px solid #172033;margin-bottom:24px;padding-bottom:12px}pre{white-space:pre-wrap;font:14px/1.5 monospace}.meta{color:#5f7187}</style></head><body><header><h1>CloudPort · EIR</h1><div class="meta">Veículo ${escapeHtml(vehicleTitle(vehicle))} · ${escapeHtml(new Date().toLocaleString('pt-BR'))}</div></header><pre>${escapeHtml(receipt)}</pre><script>window.onload=()=>window.print();<\/script></body></html>`);
      popup.document.close();
    } catch (reason) {
      popup.close();
      setError(formatError(reason, 'Não foi possível gerar o EIR.'));
    } finally {
      setPrinting(false);
    }
  }

  return <>
    <PageHeader eyebrow="Gate visual" title="Controle operacional das pistas" description="Filas, etapas, capacidade de janelas, SLA, documentos, avarias e transações problemáticas em uma única visão operacional." actions={<div className="gate-visual-toolbar"><label className="compact-field"><input type="checkbox" checked={autoRefresh} onChange={(event) => setAutoRefresh(event.target.checked)} />Atualização automática</label><span className="gate-live-clock">{now.toLocaleTimeString('pt-BR')}</span><button className="secondary" type="button" onClick={load} disabled={loading}>Atualizar agora</button></div>} />
    <Message type="error">{error}</Message>
    {loading && !panel ? <Loading label="Carregando pistas e atendimentos..." /> : <>
      <div className="metrics-grid"><MetricCard label="Veículos no fluxo" value={vehicles.length} detail={`${lanes.length} pista(s) ativa(s)`} /><MetricCard label="Em atendimento" value={panel?.veiculosAtendimento?.length ?? 0} detail="OCR, balança ou inspeção" /><MetricCard label="Janelas ocupadas" value={calendar.reduce((total, day) => total + day.windows.filter((item) => item.occupied > 0).length, 0)} detail={`${windows.length} janela(s) configurada(s)`} /><MetricCard label="Problemas / SLA" value={problems.length} detail="Exceções, documentos ou espera crítica" /></div>
      <Section title="Quadro visual das pistas" description="Cada veículo permanece selecionável durante toda a jornada, da fila de entrada à saída."><div className="gate-lane-board">{lanes.length ? lanes.map((lane) => <GateLane key={lane.id} lane={lane} selectedKey={gateVehicleKey(selectedVehicle)} onSelect={(vehicle) => setSelectedKey(gateVehicleKey(vehicle))} />) : <EmptyState title="Nenhum veículo no fluxo atual" />}</div></Section>
      <Section title="Operações complementares" description="Controle de troca de cavalo, duração dos chamados e ordenação das filas."><GateOperationsTabs /></Section>
      <Section title="Calendário e capacidade das janelas" description="Ocupação calculada pelos agendamentos associados a cada janela.">{calendar.length ? <div className="gate-calendar">{calendar.slice(0, 7).map((day) => <article className="gate-day" key={day.date}><header><strong>{formatDay(day.date)}</strong><span>{day.windows.reduce((total, item) => total + item.occupied, 0)} agendamento(s)</span></header><div className="gate-window-grid">{day.windows.map((windowItem) => <div className={`gate-window ${windowItem.percentage >= 100 ? 'full' : ''}`} key={windowItem.id ?? `${day.date}-${windowItem.horaInicio}`}><div><strong>{formatHour(windowItem.horaInicio)}–{formatHour(windowItem.horaFim)}</strong><small>{windowItem.canalEntradaDescricao ?? windowItem.canalEntrada ?? 'Canal geral'}</small></div><span>{windowItem.occupied}/{windowItem.capacity || '—'}</span><div className="gate-capacity"><i style={{ width: `${windowItem.percentage}%` }} /></div></div>)}</div></article>)}</div> : <EmptyState title="Nenhuma janela configurada" />}</Section>
      <div className="gate-detail-grid"><Section title="Jornada gráfica e atendimento"><VehicleInspector vehicle={selectedVehicle} onPrint={printEir} printing={printing} /></Section><Section title="Transações com problema" description="Priorização automática por SLA, exceções e falhas documentais.">{problems.length ? <div className="gate-problem-list">{problems.map((vehicle) => <button type="button" key={gateVehicleKey(vehicle)} onClick={() => setSelectedKey(gateVehicleKey(vehicle))}><div><strong>{vehicleTitle(vehicle)}</strong><small>{vehicle.filaOperacional} · {vehicleStatus(vehicle)}</small></div><span className={`gate-sla gate-sla-${classifyGateSla(vehicle.tempoFilaMinutos).level}`}>{classifyGateSla(vehicle.tempoFilaMinutos).label}</span></button>)}</div> : <EmptyState title="Nenhuma transação problemática" description="Todos os atendimentos estão dentro do SLA e sem exceções registradas." />}</Section></div>
    </>}
  </>;
}
