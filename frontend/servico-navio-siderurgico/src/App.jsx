import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, clearSession, formatError, hasAnyRole, loadRuntimeConfig, readSession, saveSession } from './api.js';

const PHASES = { PREVISTA: 'FUNDEADA', FUNDEADA: 'ATRACADA', ATRACADA: 'OPERANDO', OPERANDO: 'OPERACAO_CONCLUIDA', OPERACAO_CONCLUIDA: 'PARTIU' };
const EMPTY_SUMMARY = { totalItensPlanejados: 0, totalItensOperados: 0, pesoPlanejado: 0, pesoOperado: 0, percentualProgresso: 0, divergenciasPoraoPosicao: 0, itensBloqueados: 0 };
const EMPTY_INTEGRATION = { itensComReserva: 0, itensComOrdem: 0, itensSemReserva: 0, itensSemOrdem: 0, ordensEmExecucao: 0, ordensConcluidas: 0, totalAlertas: 0, statusPredominante: 'NAO_GERADO' };
const ALLOWED_ROLES = ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE'];

const clean = (value) => String(value ?? '').normalize('NFKC').replace(/[<>"'`\\]/g, '').trim();
const formatNumber = (value, digits = 0) => Number(value ?? 0).toLocaleString('pt-BR', { minimumFractionDigits: digits, maximumFractionDigits: digits });
const statusClass = (status) => `status status-${String(status ?? 'indefinido').toLowerCase().replaceAll('_', '-')}`;
const dateTime = (value) => value ? new Date(value).toLocaleString('pt-BR') : '—';

function AuthGate({ onAuthenticated }) {
  const [login, setLogin] = useState('');
  const [senha, setSenha] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const trustedOrigins = useRef(new Set());

  useEffect(() => {
    let active = true;
    loadRuntimeConfig().then((config) => {
      if (!active) return;
      trustedOrigins.current = new Set([window.location.origin, 'http://localhost:4200', ...(config.trustedParentOrigins ?? [])]);
      window.parent?.postMessage({ type: 'CLOUDPORT_CONTROL_ROOM_READY' }, '*');
    }).catch((reason) => active && setError(formatError(reason)));

    const receiveSession = (event) => {
      if (!trustedOrigins.current.has(event.origin) || event.data?.type !== 'CLOUDPORT_AUTH_SESSION') return;
      try {
        const session = saveSession(event.data.session);
        if (!hasAnyRole(session, ...ALLOWED_ROLES)) throw new Error('A conta não possui permissão operacional para acessar o Control Room.');
        onAuthenticated(session);
      } catch (reason) {
        clearSession();
        setError(formatError(reason));
      }
    };
    window.addEventListener('message', receiveSession);
    return () => { active = false; window.removeEventListener('message', receiveSession); };
  }, [onAuthenticated]);

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    setBusy(true); setError('');
    try {
      const session = saveSession(await api.autenticar(clean(login), senha));
      if (!hasAnyRole(session, ...ALLOWED_ROLES)) throw new Error('A conta não possui permissão operacional para acessar o Control Room.');
      setSenha(''); onAuthenticated(session);
    } catch (reason) {
      clearSession(); setError(formatError(reason, 'Não foi possível autenticar.'));
    } finally { setBusy(false); }
  }

  return <main className="auth-shell"><form className="auth-card" onSubmit={submit}>
    <span className="eyebrow">CloudPort</span><h1>Control Room Navio + Pátio</h1><p>Entre com uma conta operacional autorizada.</p>
    <label>Login<input value={login} onChange={(event) => setLogin(event.target.value)} autoComplete="username" required /></label>
    <label>Senha<input type="password" value={senha} onChange={(event) => setSenha(event.target.value)} autoComplete="current-password" required /></label>
    {error && <div className="message error" role="alert">{error}</div>}
    <button disabled={busy || !login || !senha}>{busy ? 'Autenticando...' : 'Entrar'}</button>
  </form></main>;
}

function Metric({ label, value, detail }) {
  return <article className="metric"><span>{label}</span><strong>{value}</strong>{detail && <small>{detail}</small>}</article>;
}

function OrdersTable({ orders, priorities, onPriority, onSuspend, onResume, busyKey, readOnly = false }) {
  if (!orders.length) return <p className="empty">Nenhuma ordem encontrada.</p>;
  return <div className="table-wrap"><table><thead><tr><th>Lote</th><th>Status</th><th>Origem</th><th>Destino</th><th>Seq.</th><th>Prioridade</th><th>Ações</th></tr></thead><tbody>
    {orders.map((order) => <tr key={order.id ?? `${order.codigoLote}-${order.sequenciaNavio}`}>
      <td><strong>{order.codigoLote}</strong><small>{order.tipoMovimento}</small></td>
      <td><span className={statusClass(order.statusOrdem)}>{order.statusOrdem}</span></td><td>{order.origem || '—'}</td><td>{order.destino || order.posicaoPlanejada || '—'}</td><td>{order.sequenciaNavio ?? '—'}</td>
      <td>{readOnly ? priorities[order.id] ?? order.prioridadeOperacional ?? 0 : <div className="inline"><input type="number" min="0" value={priorities[order.id] ?? order.prioridadeOperacional ?? order.sequenciaNavio ?? 0} onChange={(event) => onPriority(order, Number(event.target.value), false)} /><button className="small secondary" disabled={busyKey === `priority-${order.id}`} onClick={() => onPriority(order, priorities[order.id] ?? order.prioridadeOperacional ?? 0, true)}>Salvar</button></div>}</td>
      <td>{readOnly ? '—' : order.statusOrdem === 'SUSPENSA' ? <button className="small" disabled={busyKey === `resume-${order.id}`} onClick={() => onResume(order)}>Retomar</button> : <button className="small warning" disabled={busyKey === `suspend-${order.id}`} onClick={() => onSuspend(order)}>Suspender</button>}</td>
    </tr>)}
  </tbody></table></div>;
}

function WorkQueue({ queue, edit, expanded, priorities, busyKey, onToggle, onEdit, onAction, onInstruction }) {
  const jobs = queue.jobList ?? [];
  return <article className="queue"><button className="queue-head" onClick={onToggle} aria-expanded={expanded}><span><strong>{queue.identificador}</strong><small>{queue.agrupamento} · {queue.berco || 'sem berço'} · {queue.blocoZona || 'sem zona'}</small></span><span><span className={statusClass(queue.status)}>{queue.status}</span><strong>{jobs.length || queue.totalOrdens || 0} jobs</strong>{expanded ? '▴' : '▾'}</span></button>
    {expanded && <div className="queue-body"><div className="queue-editor">
      <label>POW<input value={edit.pow} onChange={(event) => onEdit({ ...edit, pow: event.target.value })} /></label><label>Pool<input value={edit.poolOperacional} onChange={(event) => onEdit({ ...edit, poolOperacional: event.target.value })} /></label><button className="secondary" disabled={busyKey === `pow-${queue.id}`} onClick={() => onAction('pow')}>Salvar POW/pool</button>
      <label>Equipamento<input value={edit.equipamento} onChange={(event) => onEdit({ ...edit, equipamento: event.target.value })} /></label><button className="secondary" disabled={busyKey === `equipment-${queue.id}`} onClick={() => onAction('equipment')}>Salvar equipamento</button><label>Limite<input type="number" min="1" value={edit.limite ?? ''} onChange={(event) => onEdit({ ...edit, limite: event.target.value ? Number(event.target.value) : null })} /></label>
    </div><div className="actions">{queue.status === 'ATIVA' ? <button className="warning" onClick={() => onAction('deactivate')}>Desativar</button> : <button onClick={() => onAction('activate')}>Ativar</button>}<button onClick={() => onAction('dispatch')}>Despachar</button></div>
    {jobs.length ? <div className="table-wrap nested"><table><thead><tr><th>Lote</th><th>Status</th><th>Destino</th><th>Prioridade</th><th>Ações</th></tr></thead><tbody>{jobs.map((job) => <tr key={job.id}><td>{job.codigoLote}</td><td><span className={statusClass(job.statusOrdem)}>{job.statusOrdem}</span></td><td>{job.destino || job.posicaoPlanejada || '—'}</td><td>{priorities[job.id] ?? job.prioridadeOperacional ?? 0}</td><td><div className="actions compact"><button className="small secondary" onClick={() => onInstruction('reset', job)}>Resetar</button><button className="small danger" onClick={() => onInstruction('cancel', job)}>Cancelar</button></div></td></tr>)}</tbody></table></div> : <p className="empty">A fila não possui jobs.</p>}
    </div>}
  </article>;
}

function ControlRoom({ session, onLogout }) {
  const [navios, setNavios] = useState([]); const [visits, setVisits] = useState([]); const [visitId, setVisitId] = useState(null);
  const [summary, setSummary] = useState(EMPTY_SUMMARY); const [integration, setIntegration] = useState(EMPTY_INTEGRATION);
  const [items, setItems] = useState([]); const [events, setEvents] = useState([]); const [reservations, setReservations] = useState([]);
  const [orders, setOrders] = useState([]); const [queues, setQueues] = useState([]); const [workQueues, setWorkQueues] = useState([]); const [uncovered, setUncovered] = useState([]); const [alerts, setAlerts] = useState([]);
  const [statusFilter, setStatusFilter] = useState(''); const [zoneFilter, setZoneFilter] = useState(''); const [severityFilter, setSeverityFilter] = useState('');
  const [autoRefresh, setAutoRefresh] = useState(true); const [lastUpdate, setLastUpdate] = useState(null); const [expanded, setExpanded] = useState({}); const [edits, setEdits] = useState({}); const [priorities, setPriorities] = useState({});
  const [busy, setBusy] = useState(false); const [busyKey, setBusyKey] = useState(''); const [error, setError] = useState(''); const [success, setSuccess] = useState(''); const [result, setResult] = useState(null);
  const activeRequest = useRef(null);
  const selectedVisit = useMemo(() => visits.find((visit) => visit.id === visitId), [visits, visitId]);

  const loadSnapshot = useCallback(async (id, silent = false) => {
    if (!id) return;
    if (activeRequest.current) return activeRequest.current;
    if (!silent) setBusy(true);
    const promise = Promise.all([api.listarItensVisita(id), api.obterResumo(id), api.listarEventos(id), api.obterResumoIntegracaoPatio(id), api.listarReservasPatio(id), api.listarOrdensPatio(id), api.listarFilasPatio(id), api.listarWorkQueuesPatio(id), api.listarOrdensSemCoberturaPatio(id), api.listarAlertasIntegracaoPatio(id)]).then(([newItems, newSummary, newEvents, newIntegration, newReservations, newOrders, newQueues, newWorkQueues, newUncovered, newAlerts]) => {
      setItems(newItems); setSummary(newSummary); setEvents(newEvents); setIntegration(newIntegration); setReservations(newReservations); setOrders(newOrders); setQueues(newQueues); setWorkQueues(newWorkQueues); setUncovered(newUncovered); setAlerts(newAlerts); setLastUpdate(new Date());
      setPriorities((current) => [...newOrders, ...newWorkQueues.flatMap((queue) => queue.jobList ?? [])].reduce((acc, order) => order.id ? { ...acc, [order.id]: current[order.id] ?? order.prioridadeOperacional ?? order.sequenciaNavio ?? 0 } : acc, {}));
      setEdits((current) => newWorkQueues.reduce((acc, queue) => ({ ...acc, [queue.id]: current[queue.id] ?? { pow: queue.pow || '', poolOperacional: queue.poolOperacional || '', equipamento: queue.equipamento || '', limite: null } }), {}));
    }).finally(() => { activeRequest.current = null; if (!silent) setBusy(false); });
    activeRequest.current = promise; return promise;
  }, []);

  useEffect(() => { let active = true; (async () => { try { const [ships, newVisits] = await Promise.all([api.listarNavios(), api.listarVisitas()]); if (!active) return; setNavios(ships); setVisits(newVisits); setVisitId(newVisits[0]?.id ?? null); } catch (reason) { setError(formatError(reason, 'Não foi possível carregar os dados operacionais.')); } })(); return () => { active = false; }; }, []);
  useEffect(() => { if (visitId) loadSnapshot(visitId).catch((reason) => setError(formatError(reason))); }, [visitId, loadSnapshot]);
  useEffect(() => { if (!autoRefresh || !visitId) return undefined; const timer = setInterval(() => loadSnapshot(visitId, true).catch((reason) => setError(formatError(reason))), 30000); return () => clearInterval(timer); }, [autoRefresh, visitId, loadSnapshot]);

  async function action(key, operation, message) { setBusyKey(key); setError(''); setSuccess(''); try { const response = await operation(); if (response !== undefined) setResult(response); setSuccess(message); } catch (reason) { setError(formatError(reason)); } finally { setBusyKey(''); } }
  const refresh = () => action('refresh', () => loadSnapshot(visitId), 'Control Room atualizado.');
  async function queueAction(queue, type) { const edit = edits[queue.id]; const operations = { activate: () => api.ativarWorkQueuePatio(queue.id), deactivate: () => api.desativarWorkQueuePatio(queue.id), pow: () => api.atualizarPowWorkQueuePatio(queue.id, { pow: edit.pow || null, poolOperacional: edit.poolOperacional || null }), equipment: () => api.atualizarEquipamentoWorkQueuePatio(queue.id, { equipamento: edit.equipamento || null }), dispatch: () => api.despacharWorkQueuePatio(queue.id, { limiteOrdens: edit.limite || null, observacao: 'Dispatch acionado pelo Control Room React' }) }; await action(`${type}-${queue.id}`, operations[type], 'Work queue atualizada.'); await loadSnapshot(visitId, true); }
  async function instructionAction(type, order) { await action(`${type}-${order.id}`, () => type === 'reset' ? api.resetarWorkInstructionPatio(order.id) : api.cancelarWorkInstructionPatio(order.id), type === 'reset' ? 'Work instruction resetada.' : 'Work instruction cancelada.'); await loadSnapshot(visitId, true); }
  function changePriority(order, value, persist) { setPriorities((current) => ({ ...current, [order.id]: value })); if (persist) action(`priority-${order.id}`, () => api.atualizarPrioridadeOrdemPatio(visitId, order.id, value), 'Prioridade atualizada.').then(() => loadSnapshot(visitId, true)); }

  const filteredOrders = orders.filter((order) => (!statusFilter || order.statusOrdem === statusFilter) && (!zoneFilter || `${order.origem} ${order.destino} ${order.posicaoPlanejada}`.toUpperCase().includes(zoneFilter.toUpperCase())));
  const filteredQueues = workQueues.filter((queue) => (!statusFilter || queue.status === statusFilter) && (!zoneFilter || `${queue.identificador} ${queue.berco} ${queue.blocoZona} ${queue.pow} ${queue.poolOperacional}`.toUpperCase().includes(zoneFilter.toUpperCase())));
  const filteredAlerts = alerts.filter((alert) => !severityFilter || alert.severidade === severityFilter);
  const imminent = orders.filter((order) => ['PENDENTE', 'EM_EXECUCAO'].includes(order.statusOrdem)).sort((a, b) => (a.sequenciaNavio ?? 999999) - (b.sequenciaNavio ?? 999999)).slice(0, 5);
  const nextPhase = PHASES[selectedVisit?.fase];

  return <div className="app"><header className="topbar"><div><span className="eyebrow">CloudPort</span><h1>Control Room Navio + Pátio</h1></div><div className="top-actions"><span>{session.nome}</span><button className="secondary" onClick={refresh}>Atualizar</button><button className="danger" onClick={onLogout}>Sair</button></div></header>
    <main className="content">{error && <div className="message error" role="alert">{error}</div>}{success && <div className="message success">{success}</div>}
      <section className="panel selector"><label>Visita<select value={visitId ?? ''} onChange={(event) => setVisitId(Number(event.target.value))}>{visits.map((visit) => <option key={visit.id} value={visit.id}>{visit.codigoVisita} · {visit.navioNome || navios.find((ship) => ship.id === visit.navioId)?.nome}</option>)}</select></label><div><span className={statusClass(selectedVisit?.fase)}>{selectedVisit?.fase || 'SEM_VISITA'}</span><small>{selectedVisit?.bercoAtual || selectedVisit?.bercoPrevisto || 'sem berço'} · atualização {lastUpdate ? dateTime(lastUpdate) : 'pendente'}</small></div>{nextPhase && <button onClick={() => action('phase', async () => { const updated = await api.alterarFaseVisita(visitId, nextPhase); setVisits((current) => current.map((visit) => visit.id === updated.id ? updated : visit)); await loadSnapshot(visitId, true); }, `Fase alterada para ${nextPhase}.`)}>Avançar para {nextPhase}</button>}</section>
      <section className="metrics"><Metric label="Progresso" value={`${formatNumber(summary.percentualProgresso, 1)}%`} detail={`${summary.totalItensOperados}/${summary.totalItensPlanejados} itens`} /><Metric label="Peso operado" value={`${formatNumber(summary.pesoOperado, 1)} t`} detail={`${formatNumber(summary.pesoPlanejado, 1)} t planejadas`} /><Metric label="Ordens" value={integration.itensComOrdem} detail={`${integration.ordensEmExecucao} em execução`} /><Metric label="Alertas" value={integration.totalAlertas} detail={integration.statusPredominante} /></section>
      <section className="panel"><div className="section-head"><div><span className="eyebrow">Operação</span><h2>Ações e filtros</h2></div><label className="check"><input type="checkbox" checked={autoRefresh} onChange={() => setAutoRefresh((value) => !value)} />Atualização automática</label></div><div className="actions"><button onClick={() => action('reserve', async () => { await api.gerarReservasPatio(visitId); await loadSnapshot(visitId, true); }, 'Reservas de pátio geradas.')}>Gerar reservas</button><button onClick={() => action('orders', async () => { await api.gerarOrdensPatio(visitId); await loadSnapshot(visitId, true); }, 'Ordens de pátio geradas.')}>Gerar ordens</button><button className="secondary" onClick={() => action('sync', async () => { await api.sincronizarStatusPatio(visitId); await loadSnapshot(visitId, true); }, 'Status sincronizado.')}>Sincronizar Yard</button><button className="secondary" onClick={() => action('simulate', () => api.replanejarPatioVisita(visitId, false), 'Replanejamento simulado.')}>Simular</button><button className="warning" onClick={() => action('apply', async () => { const response = await api.replanejarPatioVisita(visitId, true); await loadSnapshot(visitId, true); return response; }, 'Replanejamento aplicado.')}>Aplicar replanejamento</button><button className="secondary" onClick={() => action('report', () => api.obterRelatorioOperacionalIntegrado(visitId), 'Relatório carregado.')}>Relatório</button></div><div className="filters"><label>Status<select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}><option value="">Todos</option>{['PENDENTE','EM_EXECUCAO','BLOQUEADA','SUSPENSA','CONCLUIDA','CANCELADA'].map((status) => <option key={status}>{status}</option>)}</select></label><label>Bloco/zona<input value={zoneFilter} onChange={(event) => setZoneFilter(event.target.value)} /></label><label>Severidade<select value={severityFilter} onChange={(event) => setSeverityFilter(event.target.value)}><option value="">Todas</option>{['BAIXA','MEDIA','ALTA','CRITICA'].map((severity) => <option key={severity}>{severity}</option>)}</select></label></div></section>
      <section className="panel"><div className="section-head"><div><span className="eyebrow">Execução</span><h2>Movimentos iminentes</h2></div><span>{imminent.length}</span></div><div className="imminent">{imminent.map((order) => <article key={order.id}><span>#{order.sequenciaNavio ?? '—'}</span><strong>{order.codigoLote}</strong><small>{order.origem || '—'} → {order.destino || order.posicaoPlanejada || '—'}</small><span className={statusClass(order.statusOrdem)}>{order.statusOrdem}</span></article>)}{!imminent.length && <p className="empty">Não existem movimentos iminentes.</p>}</div></section>
      <section className="panel"><div className="section-head"><div><span className="eyebrow">Equipment Control</span><h2>Work queues e job lists</h2></div><span>{filteredQueues.length}</span></div>{filteredQueues.map((queue) => <WorkQueue key={queue.id ?? queue.identificador} queue={queue} expanded={!!expanded[queue.id]} edit={edits[queue.id] ?? { pow: '', poolOperacional: '', equipamento: '', limite: null }} priorities={priorities} busyKey={busyKey} onToggle={() => setExpanded((current) => ({ ...current, [queue.id]: !current[queue.id] }))} onEdit={(edit) => setEdits((current) => ({ ...current, [queue.id]: edit }))} onAction={(type) => queueAction(queue, type)} onInstruction={instructionAction} />)}{!filteredQueues.length && <p className="empty">Nenhuma work queue encontrada.</p>}</section>
      <section className="panel"><div className="section-head"><div><span className="eyebrow">Yard</span><h2>Ordens de pátio</h2></div><span>{filteredOrders.length}/{orders.length}</span></div><OrdersTable orders={filteredOrders} priorities={priorities} onPriority={changePriority} onSuspend={(order) => action(`suspend-${order.id}`, async () => { await api.suspenderOrdemPatio(visitId, order.id); await loadSnapshot(visitId, true); }, 'Ordem suspensa.')} onResume={(order) => action(`resume-${order.id}`, async () => { await api.retomarOrdemPatio(visitId, order.id); await loadSnapshot(visitId, true); }, 'Ordem retomada.')} busyKey={busyKey} /></section>
      <div className="columns"><section className="panel"><div className="section-head"><h2>Alertas</h2><span>{filteredAlerts.length}</span></div><div className="list">{filteredAlerts.map((alert, index) => <article key={`${alert.tipo}-${index}`}><div><strong>{alert.tipo}</strong><span className={statusClass(alert.severidade)}>{alert.severidade}</span></div><p>{alert.mensagem}</p></article>)}{!filteredAlerts.length && <p className="empty">Nenhum alerta.</p>}</div></section><section className="panel"><div className="section-head"><h2>Eventos recentes</h2><span>{events.length}</span></div><div className="list scroll">{events.slice(0, 30).map((event) => <article key={event.id}><div><strong>{event.tipoEvento}</strong><time>{dateTime(event.criadoEm)}</time></div><p>{event.descricao}</p><small>{event.usuario}</small></article>)}</div></section></div>
      <div className="columns"><section className="panel"><div className="section-head"><h2>Ordens sem cobertura</h2><span>{uncovered.length}</span></div><OrdersTable orders={uncovered} priorities={priorities} onPriority={() => {}} onSuspend={() => {}} onResume={() => {}} busyKey={busyKey} readOnly /></section><section className="panel"><div className="section-head"><h2>Reservas</h2><span>{reservations.length}</span></div><div className="list scroll">{reservations.map((reservation) => <article key={reservation.id ?? `${reservation.itemOperacaoNavioId}-${reservation.posicaoPatioId}`}><div><strong>{reservation.posicaoPatioId}</strong><span className={statusClass(reservation.status)}>{reservation.status}</span></div><p>Item {reservation.itemOperacaoNavioId} · {reservation.bloco || 'sem bloco'} · {reservation.tipoReserva}</p></article>)}</div></section></div>
      {result && <section className="panel"><div className="section-head"><h2>Resultado da última operação</h2><button className="small secondary" onClick={() => setResult(null)}>Fechar</button></div><pre>{JSON.stringify(result, null, 2)}</pre></section>}
      <footer>{items.length} itens · {queues.length} filas agrupadas · {summary.divergenciasPoraoPosicao} divergências</footer>{busy && <div className="loading">Carregando...</div>}
    </main></div>;
}

export default function App() {
  const [session, setSession] = useState(() => readSession());
  const [ready, setReady] = useState(false); const [error, setError] = useState('');
  useEffect(() => { loadRuntimeConfig().then(() => setReady(true)).catch((reason) => setError(formatError(reason))); }, []);
  const authenticate = useCallback((newSession) => setSession(newSession), []);
  const logout = useCallback(() => { clearSession(); setSession(null); }, []);
  if (error) return <main className="auth-shell"><div className="auth-card"><h1>Control Room indisponível</h1><div className="message error">{error}</div></div></main>;
  if (!ready) return <main className="auth-shell"><div className="auth-card"><h1>CloudPort</h1><p>Carregando configuração...</p></div></main>;
  return session && hasAnyRole(session, ...ALLOWED_ROLES) ? <ControlRoom session={session} onLogout={logout} /> : <AuthGate onAuthenticated={authenticate} />;
}
