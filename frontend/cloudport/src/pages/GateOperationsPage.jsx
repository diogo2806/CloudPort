import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError } from '../api.js';
import { DataTable, EmptyState, Loading, Message, MetricCard, PageHeader, Section, StatusBadge } from '../components.jsx';
import { gateOperationsApi } from '../gateOperationsApi.js';

function dateTime(value) {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleString('pt-BR');
}

function laneFeatures(lane) {
  return [
    lane.ocrHabilitado ? 'OCR' : null,
    lane.balancaHabilitada ? 'Balança' : null,
    lane.inspecaoHabilitada ? 'Inspeção' : null
  ].filter(Boolean).join(' · ') || 'Operação manual';
}

function LaneMonitor({ lanes, visits }) {
  if (!lanes.length) return <EmptyState title="Nenhuma pista configurada" />;
  return <div className="gate-lane-grid">
    {lanes.map((lane) => {
      const laneVisits = visits.filter((visit) => visit.laneId === lane.id);
      const occupancy = lane.capacidadeFila ? Math.min(100, Math.round((lane.filaAtual / lane.capacidadeFila) * 100)) : 0;
      return <article className={`gate-lane-card status-${String(lane.status).toLowerCase()}`} key={lane.id}>
        <header><div><span className="eyebrow">{lane.direcao}</span><h3>{lane.nome}</h3></div><StatusBadge value={lane.status} /></header>
        <div className="gate-lane-capacity"><strong>{lane.filaAtual}/{lane.capacidadeFila}</strong><span>veículos na fila</span></div>
        <div className="gate-progress"><span style={{ width: `${occupancy}%` }} /></div>
        <small>{laneFeatures(lane)}</small>
        <div className="gate-lane-vehicles">
          {laneVisits.length ? laneVisits.slice(0, 6).map((visit) => <span key={visit.id}>{visit.placa} · {visit.stageAtualNome || 'Sem estágio'}</span>) : <span>Pista livre</span>}
        </div>
      </article>;
    })}
  </div>;
}

function StageBoard({ stages, visits, selectedStageId }) {
  if (!stages.length) return <EmptyState title="Nenhum estágio configurado" />;
  return <div className="gate-stage-board">
    {stages.filter((stage) => stage.ativo).map((stage, index) => {
      const stageVisits = visits.filter((visit) => visit.stageAtualId === stage.id);
      return <div className={`gate-stage ${selectedStageId === stage.id ? 'selected' : ''}`} key={stage.id}>
        <div className="gate-stage-index">{index + 1}</div>
        <div><strong>{stage.nome}</strong><small>{stage.tarefas.length} tarefa(s) · {stageVisits.length} visita(s)</small></div>
      </div>;
    })}
  </div>;
}

function VisitInspector({ visit, stages, busy, onAdvance, onTrouble, onInspect, onIssueDocument, onTransfer }) {
  const [completedTasks, setCompletedTasks] = useState([]);
  const [documentType, setDocumentType] = useState('EIR');
  const [destinationFacility, setDestinationFacility] = useState('');
  const stage = stages.find((item) => item.id === visit?.stageAtualId);
  const requiredTasks = stage?.tarefas?.filter((task) => task.ativa && task.obrigatoria) ?? [];

  useEffect(() => {
    setCompletedTasks([]);
    setDestinationFacility('');
  }, [visit?.id, visit?.stageAtualId]);

  if (!visit) return <EmptyState title="Selecione uma truck visit" description="O inspector exibe transações, tarefas do estágio e ações operacionais." />;

  function toggleTask(code) {
    setCompletedTasks((current) => current.includes(code) ? current.filter((item) => item !== code) : [...current, code]);
  }

  const allRequiredCompleted = requiredTasks.every((task) => completedTasks.includes(task.codigo));
  return <div className="gate-visit-inspector">
    <div className="gate-visit-summary">
      <div><span className="eyebrow">{visit.codigo}</span><h3>{visit.placa}</h3><p>{visit.motorista} · {visit.transportadora}</p></div>
      <StatusBadge value={visit.status} />
    </div>
    <div className="gate-inspector-grid">
      <div><span>Pista</span><strong>{visit.laneCodigo || 'Não atribuída'}</strong></div>
      <div><span>Estágio</span><strong>{visit.stageAtualNome || '—'}</strong></div>
      <div><span>Check-in</span><strong>{dateTime(visit.checkinEm)}</strong></div>
      <div><span>Transações</span><strong>{visit.transacoes?.length ?? 0}</strong></div>
    </div>

    <h4>Business tasks do estágio</h4>
    <div className="gate-task-list">
      {(stage?.tarefas ?? []).map((task) => <label key={task.id}>
        <input type="checkbox" checked={completedTasks.includes(task.codigo)} onChange={() => toggleTask(task.codigo)} disabled={busy || !task.ativa} />
        <span><strong>{task.nome}</strong><small>{task.tipo}{task.obrigatoria ? ' · obrigatória' : ' · opcional'}</small></span>
      </label>)}
      {!stage?.tarefas?.length && <p>Nenhuma tarefa configurada neste estágio.</p>}
    </div>
    <button disabled={busy || !allRequiredCompleted || visit.status === 'TROUBLE'} onClick={() => onAdvance(visit, completedTasks)}>
      {busy ? 'Processando...' : 'Concluir estágio e avançar'}
    </button>

    <h4>Transações</h4>
    <div className="gate-transaction-list">
      {(visit.transacoes ?? []).map((transaction) => <article key={transaction.id}>
        <div><strong>#{transaction.sequencia} · {transaction.tipoOperacao}</strong><small>{transaction.unidadeReferencia || 'Sem unidade'} · <StatusBadge value={transaction.status} /></small></div>
        <div className="inline">
          <button className="secondary" disabled={busy || transaction.troubleAtivo} onClick={() => onTrouble(transaction)}>Trouble</button>
          <button className="secondary" disabled={busy} onClick={() => onInspect(transaction)}>Inspecionar</button>
        </div>
      </article>)}
    </div>

    <div className="gate-document-actions">
      <label>Documento<select value={documentType} onChange={(event) => setDocumentType(event.target.value)}><option value="EIR">EIR</option><option value="TICKET">Ticket</option></select></label>
      <button className="secondary" disabled={busy} onClick={() => onIssueDocument(visit, documentType)}>Emitir documento</button>
    </div>
    <div className="gate-document-actions">
      <label>Facility destino<input value={destinationFacility} onChange={(event) => setDestinationFacility(event.target.value)} inputMode="numeric" placeholder="ID da instalação" /></label>
      <button className="secondary" disabled={busy || !destinationFacility} onClick={() => onTransfer(visit, Number(destinationFacility))}>Transferir instalação</button>
    </div>
  </div>;
}

export function GateOperationsPage({ session }) {
  const [facilityId, setFacilityId] = useState('');
  const [dashboard, setDashboard] = useState(null);
  const [selectedVisitId, setSelectedVisitId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = useCallback(async (targetFacility = facilityId) => {
    setLoading(true); setError('');
    try {
      const payload = await gateOperationsApi.obterPainel(targetFacility || undefined);
      setDashboard(payload);
      setFacilityId(String(payload.facilitySelecionadaId ?? ''));
      setSelectedVisitId((current) => payload.visitasAtivas?.some((visit) => visit.id === current) ? current : payload.visitasAtivas?.[0]?.id ?? null);
    } catch (reason) {
      setError(formatError(reason));
      setDashboard(null);
    } finally {
      setLoading(false);
    }
  }, [facilityId]);

  useEffect(() => { load(''); }, []);

  const visits = dashboard?.visitasAtivas ?? [];
  const stages = dashboard?.stages ?? [];
  const selectedVisit = visits.find((visit) => visit.id === selectedVisitId) ?? null;
  const references = dashboard?.referencias ?? {};
  const capacity = dashboard?.capacidadeAgendamentos ?? {};
  const operator = session?.nome || 'operador';

  async function execute(action, successMessage) {
    if (busy) return;
    setBusy(true); setError(''); setSuccess('');
    try {
      await action();
      setSuccess(successMessage);
      await load(facilityId);
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  function advanceVisit(visit, tasks) {
    execute(() => gateOperationsApi.avancarVisita(visit.id, { tarefasConcluidas: tasks, usuario: operator }), 'Truck visit avançada para o próximo estágio.');
  }

  function openTrouble(transaction) {
    const code = window.prompt('Código do trouble:', 'DIVERGENCIA_OPERACIONAL');
    if (!code) return;
    const description = window.prompt('Descrição do problema:');
    if (!description) return;
    const severity = window.prompt('Severidade: BAIXA, MEDIA, ALTA ou CRITICA', 'MEDIA') || 'MEDIA';
    execute(() => gateOperationsApi.abrirTrouble(transaction.id, { codigo: code, descricao: description, severidade: severity, usuario: operator }), 'Trouble transaction aberta.');
  }

  function resolveTrouble(trouble) {
    const resolution = window.prompt('Informe a resolução do trouble:');
    if (!resolution) return;
    execute(() => gateOperationsApi.resolverTrouble(trouble.id, { resolucao: resolution, usuario: operator }), 'Trouble transaction resolvida.');
  }

  function inspectTransaction(transaction) {
    const result = window.prompt('Resultado: APROVADO, REPROVADO ou COM_RESSALVA', 'APROVADO');
    if (!result) return;
    const notes = window.prompt('Observações da inspeção:', '') || '';
    execute(() => gateOperationsApi.registrarInspecao(transaction.id, { tipo: 'OPERACIONAL', resultado: result, observacoes: notes, usuario: operator }), 'Inspeção registrada.');
  }

  function issueDocument(visit, type) {
    execute(() => gateOperationsApi.emitirDocumento(visit.id, { tipo: type, conteudo: { truckVisit: visit.codigo, placa: visit.placa, emitidoNoPortal: true }, usuario: operator }), `${type} emitido com sucesso.`);
  }

  function transferVisit(visit, destinationFacilityId) {
    execute(() => gateOperationsApi.solicitarTransferencia(visit.id, { facilityDestinoId: destinationFacilityId, usuario: operator }), 'Transferência entre instalações solicitada.');
  }

  const visitColumns = useMemo(() => [
    { key: 'codigo', label: 'Truck visit' },
    { key: 'placa', label: 'Placa' },
    { key: 'transportadora', label: 'Transportadora' },
    { key: 'laneCodigo', label: 'Pista' },
    { key: 'stageAtualNome', label: 'Estágio' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
    { key: 'checkinEm', label: 'Check-in', render: (row) => dateTime(row.checkinEm) },
    { key: 'acoes', label: 'Ação', render: (row) => <button className="secondary" onClick={() => setSelectedVisitId(row.id)}>Inspecionar</button> }
  ], []);

  return <>
    <PageHeader eyebrow="Gate" title="Operação completa" description="Lane monitor, capacidade, truck visits com múltiplas transações, estágios configuráveis, trouble, inspeções, documentos e transferências." actions={<div className="inline"><select value={facilityId} onChange={(event) => { setFacilityId(event.target.value); load(event.target.value); }}>{(dashboard?.facilities ?? []).map((facility) => <option value={facility.id} key={facility.id}>{facility.nome}</option>)}</select><button className="secondary" onClick={() => load(facilityId)} disabled={loading || busy}>Atualizar</button></div>} />
    <Message type="error">{error}</Message><Message type="success">{success}</Message>
    {loading ? <Loading label="Carregando operação do Gate..." /> : !dashboard ? null : <>
      <div className="metrics-grid">
        <MetricCard label="Visitas ativas" value={visits.length} />
        <MetricCard label="Troubles abertos" value={dashboard.troublesAbertos?.length ?? 0} />
        <MetricCard label="Capacidade disponível" value={capacity.disponivel ?? 0} detail={`${capacity.ocupacaoPercentual ?? 0}% ocupada`} />
        <MetricCard label="Referências ativas" value={(references.bookings?.length ?? 0) + (references.ordens?.length ?? 0) + (references.preAvisos?.length ?? 0)} detail={`${references.bookings?.length ?? 0} bookings · ${references.ordens?.length ?? 0} ordens · ${references.preAvisos?.length ?? 0} pré-avisos`} />
      </div>
      <Section title="Lane monitor"><LaneMonitor lanes={dashboard.lanes ?? []} visits={visits} /></Section>
      <Section title="Fluxo configurado"><StageBoard stages={stages} visits={visits} selectedStageId={selectedVisit?.stageAtualId} /></Section>
      <div className="gate-operations-layout">
        <Section title="Truck visits"><DataTable rows={visits} columns={visitColumns} rowKey={(row) => row.id} emptyTitle="Nenhuma truck visit ativa" gridId="gate-operacional-visitas" exportFileName="truck-visits-gate" /></Section>
        <Section title="Inspector"><VisitInspector visit={selectedVisit} stages={stages} busy={busy} onAdvance={advanceVisit} onTrouble={openTrouble} onInspect={inspectTransaction} onIssueDocument={issueDocument} onTransfer={transferVisit} /></Section>
      </div>
      <Section title="Trouble transactions">
        {(dashboard.troublesAbertos ?? []).length ? <DataTable rows={dashboard.troublesAbertos} columns={[
          { key: 'truckVisitCodigo', label: 'Truck visit' },
          { key: 'codigo', label: 'Código' },
          { key: 'severidade', label: 'Severidade', render: (row) => <StatusBadge value={row.severidade} /> },
          { key: 'descricao', label: 'Descrição' },
          { key: 'abertoEm', label: 'Abertura', render: (row) => dateTime(row.abertoEm) },
          { key: 'acao', label: 'Ação', render: (row) => <button disabled={busy} onClick={() => resolveTrouble(row)}>Resolver</button> }
        ]} rowKey={(row) => row.id} gridId="gate-troubles" exportFileName="troubles-gate" /> : <EmptyState title="Nenhum trouble aberto" />}
      </Section>
    </>}
  </>;
}