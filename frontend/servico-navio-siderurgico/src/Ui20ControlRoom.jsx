import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, formatError } from './api.js';
import Ui20Plan from './Ui20Plan.jsx';
import { DrillDown, EquipmentPanel, Metric, Stream, WorkQueues } from './Ui20Panels.jsx';
import { EMPTY_INTEGRATION, EMPTY_SUMMARY, TRANSITION_ACTIONS, clean, dateTime, normalized, number, orderCode, orderDestination, orderOrigin, planDraft, serializePlan, statusClass, validatePlan } from './ui20-model.js';

export default function Ui20ControlRoom({ session, onLogout }) {
  const [visits, setVisits] = useState([]); const [visitId, setVisitId] = useState(null); const [summary, setSummary] = useState(EMPTY_SUMMARY); const [integration, setIntegration] = useState(EMPTY_INTEGRATION);
  const [orders, setOrders] = useState([]); const [queues, setQueues] = useState([]); const [uncovered, setUncovered] = useState([]); const [yardQueryStatus, setYardQueryStatus] = useState(null); const [alerts, setAlerts] = useState([]); const [events, setEvents] = useState([]); const [reservations, setReservations] = useState([]);
  const [monitor, setMonitor] = useState(null); const [plan, setPlan] = useState(null); const [draft, setDraftState] = useState(planDraft(null, null)); const [dirty, setDirty] = useState(false); const [jobLists, setJobLists] = useState([]); const [matrix, setMatrix] = useState({}); const [telemetry, setTelemetry] = useState([]);
  const [edits, setEdits] = useState({}); const [expanded, setExpanded] = useState({}); const [detail, setDetail] = useState(null); const [statusFilter, setStatusFilter] = useState(''); const [zoneFilter, setZoneFilter] = useState(''); const [severityFilter, setSeverityFilter] = useState('');
  const [busy, setBusy] = useState(false); const [busyKey, setBusyKey] = useState(''); const [error, setError] = useState(''); const [success, setSuccess] = useState(''); const [stream, setStream] = useState('DESCONECTADO'); const [telemetryStream, setTelemetryStream] = useState('DESCONECTADO'); const [lastUpdate, setLastUpdate] = useState(null);
  const selectedVisit = useMemo(() => visits.find((visit) => Number(visit.id) === Number(visitId)), [visits, visitId]);
  const setDraft = (value) => { setDraftState(value); setDirty(true); };

  const load = useCallback(async (id, silent = false, forcePlan = false) => {
    if (!id) return; if (!silent) setBusy(true);
    try {
      const values = await Promise.all([
        api.obterResumo(id), api.obterResumoIntegracaoPatio(id), api.listarOrdensPatio(id), api.listarWorkQueuesPatio(id),
        api.listarOrdensSemCoberturaPatio(id), api.listarAlertasIntegracaoPatio(id), api.listarEventos(id), api.listarReservasPatio(id),
        api.obterQuayMonitor(id), api.obterPlanoGuindaste(id), api.listarJobListsEquipamentoPatio(id), api.obterMatrizEstadosWorkInstructionPatio(), api.listarTelemetriaEquipamentos()
      ]);
      const [newSummary, newIntegration, newOrders, newQueues, newUncovered, newAlerts, newEvents, newReservations, newMonitor, newPlan, newJobs, newMatrix, newTelemetry] = values;
      setSummary(newSummary || EMPTY_SUMMARY); setIntegration(newIntegration || EMPTY_INTEGRATION); setOrders(newOrders || []); setQueues(newQueues || []); setUncovered(newUncovered?.dados || newUncovered || []); setYardQueryStatus(newUncovered?.status ? newUncovered : null); setAlerts(newAlerts || []); setEvents(newEvents || []); setReservations(newReservations || []); setMonitor(newMonitor); setPlan(newPlan); setJobLists(newJobs || []); setMatrix(newMatrix || {}); setTelemetry(newTelemetry || []); setLastUpdate(new Date());
      setEdits((current) => (newQueues || []).reduce((acc, queue) => ({ ...acc, [queue.id]: current[queue.id] || { porao: queue.porao || '', planoGuindasteId: queue.planoGuindasteId || '', recursoCaisId: queue.recursoCaisId || '', equipamentoPatioId: queue.equipamentoPatioId || '' } }), {}));
      if (forcePlan || !dirty) { setDraftState(planDraft(newPlan, visits.find((visit) => Number(visit.id) === Number(id)))); setDirty(false); }
    } catch (reason) { setError(formatError(reason)); } finally { if (!silent) setBusy(false); }
  }, [dirty, visits]);

  useEffect(() => { api.listarVisitas().then((data) => { setVisits(data || []); setVisitId(data?.[0]?.id || null); }).catch((reason) => setError(formatError(reason))); }, []);
  useEffect(() => { if (visitId) load(visitId, false, true); }, [visitId]);
  useEffect(() => visitId ? api.assinarEventos(visitId, { onState: setStream, onEvent: () => load(visitId, true), onError: () => setStream('RECONECTANDO') }) : undefined, [visitId, load]);
  useEffect(() => api.assinarTelemetriaEquipamentos({ onState: setTelemetryStream, onEvent: () => api.listarTelemetriaEquipamentos().then(setTelemetry), onError: () => setTelemetryStream('RECONECTANDO') }), []);

  async function action(key, operation, message, reload = true) {
    setBusyKey(key); setError(''); setSuccess('');
    try { const result = await operation(); setSuccess(message); if (reload) await load(visitId, true); return result; }
    catch (reason) { setError(formatError(reason)); return null; } finally { setBusyKey(''); }
  }
  async function savePlan() {
    const errors = validatePlan(draft, queues, visitId); if (errors.length) return setError(errors.join(' '));
    const saved = await action('plan', () => api.salvarPlanoGuindaste(visitId, serializePlan(draft)), 'Plano de guindastes salvo.', false);
    if (saved) { setPlan(saved); setDraftState(planDraft(saved, selectedVisit)); setDirty(false); await load(visitId, true); }
  }
  async function saveResources(queue) {
    const edit = edits[queue.id] || {};
    await action(`resources-${queue.id}`, () => api.atualizarRecursosWorkQueuePatio(queue.id, {
      porao: edit.porao ? Number(edit.porao) : null, planoGuindasteId: edit.planoGuindasteId ? Number(edit.planoGuindasteId) : null,
      recursoCaisId: edit.recursoCaisId ? Number(edit.recursoCaisId) : null, equipamentoPatioId: edit.equipamentoPatioId ? Number(edit.equipamentoPatioId) : null
    }, 'Associação operacional pelo Control Room'), 'Recursos operacionais atualizados.');
  }
  async function transition(order, target) {
    const actionInfo = TRANSITION_ACTIONS[target]; if (!actionInfo) return;
    const updated = await action(`transition-${order.id}`, () => actionInfo[1](order.id, `${actionInfo[0]} pelo Control Room`), `Work instruction alterada para ${target}.`);
    if (updated && detail?.workInstruction?.id === order.id) setDetail(await api.obterDrillDownWorkInstructionPatio(order.id));
  }
  async function openDetail(order) {
    setBusyKey(`detail-${order.id}`);
    try { setDetail(await api.obterDrillDownWorkInstructionPatio(order.id)); } catch (reason) { setError(formatError(reason)); } finally { setBusyKey(''); }
  }
  async function savePriority(order, prioridadeOperacional, prioridadeBusca) {
    await action(`priority-${order.id}`, () => api.atualizarPrioridadesWorkInstructionPatio(order.id, { prioridadeOperacional, prioridadeBusca }, 'Prioridades ajustadas no Control Room'), 'Prioridades atualizadas.');
    setDetail(await api.obterDrillDownWorkInstructionPatio(order.id));
  }

  const filteredQueues = queues.filter((queue) => (!statusFilter || queue.status === statusFilter) && (!zoneFilter || normalized(`${queue.identificador} ${queue.berco} ${queue.blocoZona} ${queue.pow}`).includes(normalized(zoneFilter))));
  const filteredOrders = orders.filter((order) => (!statusFilter || order.statusOrdem === statusFilter) && (!zoneFilter || normalized(`${orderOrigin(order)} ${orderDestination(order)}`).includes(normalized(zoneFilter))));
  const filteredAlerts = alerts.filter((alert) => !severityFilter || alert.severidade === severityFilter);

  return <div className="app"><header className="topbar"><div><span className="eyebrow">CloudPort</span><h1>Control Room Navio + Pátio</h1></div><div className="stream-health"><Stream value={stream} /><Stream value={telemetryStream} /></div><div className="top-actions"><span>{session.nome}</span><button className="secondary" onClick={() => load(visitId, false)}>Atualizar</button><button className="danger" onClick={onLogout}>Sair</button></div></header><main className="content">
    {error && <div className="message error">{error}</div>}{success && <div className="message success">{success}</div>}{yardQueryStatus && !yardQueryStatus.confirmado && <div className="message warning-message">Yard em modo degradado: {yardQueryStatus.motivoDegradacao || yardQueryStatus.status}</div>}
    <section className="panel selector"><label>Visita<select value={visitId || ''} onChange={(event) => setVisitId(Number(event.target.value))}>{visits.map((visit) => <option key={visit.id} value={visit.id}>{visit.codigoVisita} · {visit.navioNome || 'navio'}</option>)}</select></label><div><span className={statusClass(selectedVisit?.fase)}>{selectedVisit?.fase || 'SEM_VISITA'}</span><small>{selectedVisit?.bercoAtual || selectedVisit?.bercoPrevisto || 'sem berço'} · {dateTime(lastUpdate)}</small></div></section>
    <section className="metrics"><Metric label="Progresso" value={`${number(summary.percentualProgresso, 1)}%`} detail={`${summary.totalItensOperados}/${summary.totalItensPlanejados} itens`} /><Metric label="Peso operado" value={`${number(summary.pesoOperado, 1)} t`} detail={`${number(summary.pesoPlanejado, 1)} t planejadas`} /><Metric label="Ordens" value={integration.itensComOrdem} detail={`${integration.ordensEmExecucao} em execução`} /><Metric label="Alertas" value={integration.totalAlertas} detail={integration.statusPredominante} /></section>
    <section className="panel"><div className="section-head"><h2>Ações e filtros</h2></div><div className="actions"><button onClick={() => action('reserve', () => api.gerarReservasPatio(visitId), 'Reservas geradas.')}>Gerar reservas</button><button onClick={() => action('orders', () => api.gerarOrdensPatio(visitId), 'Ordens geradas.')}>Gerar ordens</button><button className="secondary" onClick={() => action('sync', () => api.sincronizarStatusPatio(visitId), 'Yard sincronizado.')}>Sincronizar Yard</button></div><div className="filters"><label>Status<select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}><option value="">Todos</option>{['ATIVA','PENDENTE','EM_EXECUCAO','BLOQUEADA','SUSPENSA','CONCLUIDA','CANCELADA'].map((value) => <option key={value}>{value}</option>)}</select></label><label>Bloco/zona<input value={zoneFilter} onChange={(event) => setZoneFilter(event.target.value)} /></label><label>Severidade<select value={severityFilter} onChange={(event) => setSeverityFilter(event.target.value)}><option value="">Todas</option>{['BAIXA','MEDIA','ALTA','CRITICA'].map((value) => <option key={value}>{value}</option>)}</select></label></div></section>
    <Ui20Plan visitId={visitId} monitor={monitor} plan={plan} draft={draft} queues={queues} equipment={jobLists} dirty={dirty} busy={busyKey === 'plan'} onChange={setDraft} onSave={savePlan} onReload={() => load(visitId, false, true)} />
    <EquipmentPanel jobLists={jobLists} telemetry={telemetry} onDetails={openDetail} />
    <WorkQueues queues={filteredQueues} plan={plan} jobLists={jobLists} matrix={matrix} edits={edits} expanded={expanded} busyKey={busyKey} onEdit={(id, edit) => setEdits((current) => ({ ...current, [id]: edit }))} onSave={saveResources} onToggle={(key) => setExpanded((current) => ({ ...current, [key]: !current[key] }))} onDetails={openDetail} onTransition={transition} />
    <section className="panel"><div className="section-head"><h2>Ordens de pátio</h2><span>{filteredOrders.length}</span></div>{!filteredOrders.length ? <p className="empty">Nenhuma ordem.</p> : <div className="table-wrap"><table><thead><tr><th>Unidade</th><th>Status</th><th>Origem</th><th>Destino</th><th>Detalhes</th></tr></thead><tbody>{filteredOrders.map((order) => <tr key={order.id}><td>{orderCode(order)}</td><td><span className={statusClass(order.statusOrdem)}>{order.statusOrdem}</span></td><td>{orderOrigin(order)}</td><td>{orderDestination(order)}</td><td><button className="small secondary" onClick={() => openDetail(order)}>Drill-down</button></td></tr>)}</tbody></table></div>}</section>
    <div className="columns"><section className="panel"><div className="section-head"><h2>Alertas</h2><span>{filteredAlerts.length}</span></div><div className="list">{filteredAlerts.map((alert, index) => <article key={`${alert.tipo}-${index}`}><div><strong>{alert.tipo}</strong><span className={statusClass(alert.severidade)}>{alert.severidade}</span></div><p>{alert.mensagem}</p></article>)}</div></section><section className="panel"><div className="section-head"><h2>Eventos recentes</h2><span>{events.length}</span></div><div className="list scroll">{events.slice(0, 30).map((event) => <article key={event.id}><div><strong>{event.tipoEvento}</strong><time>{dateTime(event.criadoEm)}</time></div><p>{event.descricao}</p><small>{event.usuario}</small></article>)}</div></section></div>
    <div className="columns"><section className="panel"><div className="section-head"><h2>Sem cobertura</h2><span>{uncovered.length}</span></div><div className="list">{uncovered.map((order) => <article key={order.id}><div><strong>{orderCode(order)}</strong><button className="small secondary" onClick={() => openDetail(order)}>Drill-down</button></div><p>{orderOrigin(order)} → {orderDestination(order)}</p></article>)}</div></section><section className="panel"><div className="section-head"><h2>Reservas</h2><span>{reservations.length}</span></div><div className="list scroll">{reservations.map((reservation) => <article key={reservation.id || `${reservation.itemOperacaoNavioId}-${reservation.posicaoPatioId}`}><div><strong>{reservation.posicaoPatioId}</strong><span className={statusClass(reservation.status)}>{reservation.status}</span></div><p>Item {reservation.itemOperacaoNavioId} · {reservation.bloco || 'sem bloco'}</p></article>)}</div></section></div>
    {busy && <div className="loading">Carregando...</div>}
  </main><DrillDown detail={detail} busy={Boolean(busyKey)} onClose={() => setDetail(null)} onTransition={transition} onPriority={savePriority} /></div>;
}
