import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, readSession } from '../api.js';
import { DataTable, EmptyState, Message, Section, StatusBadge } from '../components.jsx';
import { generalCargoApi } from '../generalCargoApi.js';

function currentUser() {
  const session = readSession();
  return session?.nome || session?.email || 'operador';
}

function dateTime(value) {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleString('pt-BR');
}

function localInput(date) {
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

function initialWindow() {
  const start = new Date();
  start.setSeconds(0, 0);
  start.setMinutes(Math.ceil(start.getMinutes() / 15) * 15);
  const end = new Date(start.getTime() + (2 * 60 * 60 * 1000));
  return { janelaInicio: localInput(start), janelaFim: localInput(end) };
}

function blankSchedule() {
  return {
    docaId: '',
    areaEsperaId: '',
    recursoId: '',
    ...initialWindow(),
    observacao: ''
  };
}

function formFromSchedule(schedule) {
  if (!schedule) return blankSchedule();
  return {
    docaId: schedule.docaId || '',
    areaEsperaId: schedule.areaEsperaId || '',
    recursoId: schedule.recursoId || '',
    janelaInicio: schedule.janelaInicio ? localInput(new Date(schedule.janelaInicio)) : '',
    janelaFim: schedule.janelaFim ? localInput(new Date(schedule.janelaFim)) : '',
    observacao: schedule.observacaoReserva || ''
  };
}

function toOffsetDateTime(value) {
  if (!value) return null;
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed.toISOString();
}

export function StuffUnstuffDockSchedulePanel({ onChanged }) {
  const [operations, setOperations] = useState([]);
  const [schedules, setSchedules] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [selectedOperation, setSelectedOperation] = useState(null);
  const [plans, setPlans] = useState([]);
  const [currentSchedule, setCurrentSchedule] = useState(null);
  const [form, setForm] = useState(blankSchedule);
  const [cancelReason, setCancelReason] = useState('');
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadAgenda = useCallback(async () => {
    setError('');
    try {
      const [operationsResult, schedulesResult] = await Promise.all([
        generalCargoApi.listarOperacoesStuffUnstuff(),
        generalCargoApi.listarProgramacoesDocaStuffUnstuff()
      ]);
      setOperations(Array.isArray(operationsResult) ? operationsResult : []);
      setSchedules(Array.isArray(schedulesResult) ? schedulesResult : []);
    } catch (reason) {
      setError(formatError(reason));
    }
  }, []);

  const loadSelected = useCallback(async (operationId) => {
    if (!operationId) {
      setSelectedOperation(null);
      setPlans([]);
      setCurrentSchedule(null);
      setForm(blankSchedule());
      return;
    }
    setError('');
    try {
      const [operationResult, plansResult] = await Promise.all([
        generalCargoApi.obterOperacaoStuffUnstuff(operationId),
        generalCargoApi.listarPlanosStuffUnstuff(operationId)
      ]);
      let scheduleResult = null;
      try {
        scheduleResult = await generalCargoApi.obterProgramacaoDocaStuffUnstuff(operationId);
      } catch (reason) {
        if (reason?.status !== 404) throw reason;
      }
      setSelectedOperation(operationResult);
      setPlans(Array.isArray(plansResult) ? plansResult : []);
      setCurrentSchedule(scheduleResult);
      setForm(formFromSchedule(scheduleResult));
    } catch (reason) {
      setError(formatError(reason));
    }
  }, []);

  useEffect(() => { loadAgenda(); }, [loadAgenda]);
  useEffect(() => { loadSelected(selectedId); }, [loadSelected, selectedId]);

  const latestPlan = plans[0] || null;
  const activeSchedule = ['RESERVADA', 'EM_USO'].includes(currentSchedule?.status);
  const canReserve = selectedOperation?.status === 'PLANEJADA'
    && latestPlan?.status === 'LIBERADO'
    && currentSchedule?.status !== 'EM_USO';
  const canCancel = currentSchedule?.status === 'RESERVADA';

  const windowStatus = useMemo(() => {
    if (!currentSchedule || currentSchedule.status !== 'RESERVADA') return '—';
    const now = Date.now();
    const start = new Date(currentSchedule.janelaInicio).getTime();
    const end = new Date(currentSchedule.janelaFim).getTime();
    if (now < start) return 'AGUARDANDO JANELA';
    if (now >= end) return 'JANELA EXPIRADA';
    return 'JANELA ABERTA';
  }, [currentSchedule]);

  async function reserve(event) {
    event.preventDefault();
    if (!selectedId || !canReserve || busy) return;
    const janelaInicio = toOffsetDateTime(form.janelaInicio);
    const janelaFim = toOffsetDateTime(form.janelaFim);
    if (!janelaInicio || !janelaFim) {
      setError('Informe uma janela operacional válida.');
      return;
    }
    setBusy('reserve');
    setError('');
    setSuccess('');
    try {
      const result = await generalCargoApi.reservarProgramacaoDocaStuffUnstuff(selectedId, {
        ...form,
        janelaInicio,
        janelaFim,
        usuario: currentUser()
      });
      setCurrentSchedule(result);
      setForm(formFromSchedule(result));
      setSuccess('Doca, área de espera, janela, recurso, contêiner e cargo lots reservados sem conflito.');
      await loadAgenda();
      await loadSelected(selectedId);
      if (onChanged) await onChanged();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy('');
    }
  }

  async function cancel(event) {
    event.preventDefault();
    if (!selectedId || !canCancel || busy) return;
    setBusy('cancel');
    setError('');
    setSuccess('');
    try {
      const result = await generalCargoApi.cancelarProgramacaoDocaStuffUnstuff(selectedId, {
        motivo: cancelReason,
        usuario: currentUser()
      });
      setCurrentSchedule(result);
      setCancelReason('');
      setSuccess('Programação cancelada e recursos liberados para outra operação.');
      await loadAgenda();
      await loadSelected(selectedId);
      if (onChanged) await onChanged();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy('');
    }
  }

  return <Section
    title="Agenda de docas e staging"
    description="Reserve doca, área de espera, janela operacional, recurso, contêiner e cargo lots antes de iniciar stuff ou unstuff. Sobreposições são bloqueadas pelo backend e pelo banco."
  >
    <Message type="error">{error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    <DataTable
      gridId="stuff-unstuff-dock-schedule"
      exportFileName="agenda-docas-stuff-unstuff"
      rows={schedules}
      rowKey="id"
      columns={[
        { key: 'docaId', label: 'Doca' },
        { key: 'areaEsperaId', label: 'Área de espera' },
        { key: 'janelaInicio', label: 'Início', render: (row) => dateTime(row.janelaInicio) },
        { key: 'janelaFim', label: 'Fim', render: (row) => dateTime(row.janelaFim) },
        { key: 'recursoId', label: 'Recurso' },
        { key: 'conteinerId', label: 'Contêiner' },
        { key: 'tipoOperacao', label: 'Operação', render: (row) => <StatusBadge value={row.tipoOperacao} /> },
        { key: 'status', label: 'Estado', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'lotes', label: 'Cargo lots', render: (row) => (row.lotes || []).map((lot) => lot.loteCodigo).join(', ') || '—' },
        { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="secondary small" onClick={() => setSelectedId(row.operacaoId)}>Abrir programação</button> }
      ]}
      emptyTitle="Nenhuma programação de doca registrada"
    />

    <div className="planner-selection-grid">
      <label className="field"><span>Operação planejada</span><select value={selectedId} onChange={(event) => setSelectedId(event.target.value)}><option value="">Selecione</option>{operations.filter((operation) => operation.status === 'PLANEJADA' || operation.id === selectedId).map((operation) => <option key={operation.id} value={operation.id}>{operation.tipo} | {operation.conteinerId} | {operation.status}</option>)}</select></label>
      <div className="field"><span>Plano mais recente</span><strong>{latestPlan ? `v${latestPlan.versao} — ${latestPlan.status}` : '—'}</strong></div>
      <div className="field"><span>Programação</span>{currentSchedule ? <StatusBadge value={currentSchedule.status} /> : <strong>NÃO RESERVADA</strong>}</div>
      <div className="field"><span>Janela</span><strong>{windowStatus}</strong></div>
      <div className="field"><span>Ocupação</span><strong>{activeSchedule ? 'RECURSOS RESERVADOS' : 'RECURSOS LIVRES'}</strong></div>
    </div>

    {!selectedOperation && <EmptyState title="Selecione uma operação para reservar a doca" />}

    {selectedOperation && <>
      <form className="planner-selection-grid" onSubmit={reserve}>
        <label className="field"><span>Doca</span><input required disabled={!canReserve} maxLength="80" value={form.docaId} onChange={(event) => setForm((current) => ({ ...current, docaId: event.target.value }))} /></label>
        <label className="field"><span>Área de espera</span><input required disabled={!canReserve} maxLength="80" value={form.areaEsperaId} onChange={(event) => setForm((current) => ({ ...current, areaEsperaId: event.target.value }))} /></label>
        <label className="field"><span>Recurso operacional</span><input required disabled={!canReserve} maxLength="120" value={form.recursoId} onChange={(event) => setForm((current) => ({ ...current, recursoId: event.target.value }))} /></label>
        <label className="field"><span>Início da janela</span><input required disabled={!canReserve} type="datetime-local" value={form.janelaInicio} onChange={(event) => setForm((current) => ({ ...current, janelaInicio: event.target.value }))} /></label>
        <label className="field"><span>Fim da janela</span><input required disabled={!canReserve} type="datetime-local" value={form.janelaFim} onChange={(event) => setForm((current) => ({ ...current, janelaFim: event.target.value }))} /></label>
        <label className="field"><span>Observação da reserva</span><input disabled={!canReserve} maxLength="1000" value={form.observacao} onChange={(event) => setForm((current) => ({ ...current, observacao: event.target.value }))} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={!canReserve || busy === 'reserve'}>{busy === 'reserve' ? 'Validando conflitos...' : currentSchedule ? 'Reprogramar recursos' : 'Reservar recursos'}</button></div>
        <div className="field"><span>Bloqueio atual</span><strong>{selectedOperation.status !== 'PLANEJADA' ? 'A operação já iniciou ou foi encerrada.' : latestPlan?.status !== 'LIBERADO' ? 'Libere a versão mais recente do plano.' : currentSchedule?.status === 'EM_USO' ? 'A programação está em uso.' : 'Programação permitida.'}</strong></div>
      </form>

      <DataTable rows={selectedOperation.itens || []} rowKey="id" columns={[
        { key: 'loteCodigo', label: 'Cargo lot reservado' },
        { key: 'quantidadePlanejada', label: 'Quantidade' },
        { key: 'volumePlanejadoM3', label: 'Volume m³' },
        { key: 'pesoPlanejadoKg', label: 'Peso kg' }
      ]} emptyTitle="A operação não possui cargo lots" />

      <form className="planner-selection-grid" onSubmit={cancel}>
        <label className="field"><span>Motivo para liberar a programação</span><input required disabled={!canCancel} maxLength="1000" value={cancelReason} onChange={(event) => setCancelReason(event.target.value)} /></label>
        <div className="field"><span>Ação</span><button type="submit" className="danger" disabled={!canCancel || busy === 'cancel'}>{busy === 'cancel' ? 'Liberando...' : 'Cancelar programação'}</button></div>
        <div className="field"><span>Regra</span><strong>Depois do início, os recursos são liberados somente ao concluir ou cancelar a operação.</strong></div>
      </form>
    </>}
  </Section>;
}
