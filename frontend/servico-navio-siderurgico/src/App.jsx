import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, clearSession, formatError, hasAnyRole, loadRuntimeConfig, readSession, saveSession } from './api.js';

const ROLES = ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE'];
const PHASES = { PREVISTA: 'FUNDEADA', FUNDEADA: 'ATRACADA', ATRACADA: 'OPERANDO', OPERANDO: 'OPERACAO_CONCLUIDA', OPERACAO_CONCLUIDA: 'PARTIU' };
const EMPTY_SUMMARY = { totalItensPlanejados: 0, totalItensOperados: 0, pesoPlanejado: 0, pesoOperado: 0, percentualProgresso: 0, divergenciasPoraoPosicao: 0 };
const EMPTY_INTEGRATION = { itensComOrdem: 0, ordensEmExecucao: 0, totalAlertas: 0, statusPredominante: 'NAO_GERADO' };
const EMPTY_QUEUE_EDIT = { pow: '', poolOperacional: '', porao: '', planoGuindasteId: '', recursoCaisId: '', equipamentoPatioId: '', limite: null };
const EXCEPTIONS = {
  SEM_FILA: ['FILA', 'Sem fila', 'Ordem sem work queue.'],
  SEM_POW: ['POW', 'Sem POW', 'Fila sem ponto de trabalho.'],
  SEM_EQUIPAMENTO: ['CHE', 'Sem equipamento', 'Fila sem CHE associado.'],
  SEM_JOB_LIST: ['JOB', 'Sem job list', 'Fila sem instruções disponíveis.'],
  POSICAO_INVALIDA: ['POS', 'Posição inválida', 'Posição fora do mapa operacional.'],
  RESERVA_BLOQUEADA: ['RES', 'Reserva bloqueada', 'Reserva indisponível para consumo.'],
  DIVERGENCIA_NAVIO_PATIO: ['DIV', 'Divergência Navio x Pátio', 'Planejado e realizado incompatíveis.']
};

const clean = (value) => String(value ?? '').normalize('NFKC').replace(/[<>"'`\\]/g, '').trim();
const normalize = (value) => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase();
const statusClass = (value) => `status status-${String(value ?? 'indefinido').toLowerCase().replaceAll('_', '-')}`;
const dateTime = (value) => value ? new Date(value).toLocaleString('pt-BR') : '—';
const number = (value, digits = 0) => Number(value ?? 0).toLocaleString('pt-BR', { minimumFractionDigits: digits, maximumFractionDigits: digits });
const nullableNumber = (value) => value === '' || value === null || value === undefined ? null : Number(value);

function exceptionType(value) {
  const text = normalize(value);
  if (text.includes('SEM POW') || text.includes('SEM_POW')) return 'SEM_POW';
  if (text.includes('SEM EQUIP') || text.includes('SEM_EQUIP')) return 'SEM_EQUIPAMENTO';
  if (text.includes('SEM JOB') || text.includes('SEM_JOB')) return 'SEM_JOB_LIST';
  if (text.includes('POSICAO') && (text.includes('INVALID') || text.includes('INEXIST'))) return 'POSICAO_INVALIDA';
  if (text.includes('RESERVA') && (text.includes('BLOQUE') || text.includes('INTERDIT'))) return 'RESERVA_BLOQUEADA';
  if (text.includes('DIVERGEN')) return 'DIVERGENCIA_NAVIO_PATIO';
  return 'SEM_FILA';
}

function AuthGate({ onAuthenticated }) {
  const [login, setLogin] = useState('');
  const [senha, setSenha] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const origins = useRef(new Set());

  useEffect(() => {
    let active = true;
    loadRuntimeConfig().then((config) => {
      if (!active) return;
      origins.current = new Set([window.location.origin, 'http://localhost:4200', ...(config.trustedParentOrigins ?? [])]);
      window.parent?.postMessage({ type: 'CLOUDPORT_CONTROL_ROOM_READY' }, '*');
    }).catch((reason) => active && setError(formatError(reason)));
    const receive = (event) => {
      if (!origins.current.has(event.origin) || event.data?.type !== 'CLOUDPORT_AUTH_SESSION') return;
      try {
        const session = saveSession(event.data.session);
        if (!hasAnyRole(session, ...ROLES)) throw new Error('A conta não possui permissão operacional para acessar o Control Room.');
        onAuthenticated(session);
      } catch (reason) {
        clearSession();
        setError(formatError(reason));
      }
    };
    window.addEventListener('message', receive);
    return () => { active = false; window.removeEventListener('message', receive); };
  }, [onAuthenticated]);

  async function submit(event) {
    event.preventDefault();
    setBusy(true); setError('');
    try {
      const session = saveSession(await api.autenticar(clean(login), senha));
      if (!hasAnyRole(session, ...ROLES)) throw new Error('A conta não possui permissão operacional para acessar o Control Room.');
      onAuthenticated(session);
    } catch (reason) {
      clearSession(); setError(formatError(reason, 'Não foi possível autenticar.'));
    } finally { setBusy(false); }
  }

  return <main className="auth-shell"><form className="auth-card" onSubmit={submit}>
    <span className="eyebrow">CloudPort</span><h1>Control Room Navio + Pátio</h1><p>Entre com uma conta operacional autorizada.</p>
    <label>Login<input value={login} onChange={(event) => setLogin(event.target.value)} autoComplete="username" required /></label>
    <label>Senha<input type="password" value={senha} onChange={(event) => setSenha(event.target.value)} autoComplete="current-password" required /></label>
    {error && <div className="message error" role="alert">{error}</div>}<button disabled={busy}>{busy ? 'Autenticando...' : 'Entrar'}</button>
  </form></main>;
}

const Metric = ({ label, value, detail }) => <article className="metric"><span>{label}</span><strong>{value}</strong><small>{detail}</small></article>;
const StreamStatus = ({ value }) => <span className={`stream-status stream-${value.toLowerCase()}`}><i />{value}</span>;
const ExceptionBadge = ({ type }) => <span className={`exception-badge exception-${type.toLowerCase().replaceAll('_', '-')}`}>{EXCEPTIONS[type][0]}</span>;

function OrdersTable({ orders, priorities, busyKey, readOnly, onPriority, onSuspend, onResume, onDetails }) {
  if (!orders.length) return <p className="empty">Nenhuma ordem encontrada.</p>;
  return <div className="table-wrap"><table><thead><tr><th>Lote</th><th>Status</th><th>Origem</th><th>Destino</th><th>Seq.</th><th>Prioridade</th><th>Ações</th></tr></thead><tbody>{orders.map((order) => <tr key={order.id ?? `${order.codigoLote}-${order.sequenciaNavio}`}>
    <td><strong>{order.codigoLote}</strong><small>{order.tipoMovimento}</small></td><td><span className={statusClass(order.statusOrdem)}>{order.statusOrdem}</span></td><td>{order.origem || '—'}</td><td>{order.destino || order.posicaoPlanejada || '—'}</td><td>{order.sequenciaNavio ?? '—'}</td>
    <td>{readOnly ? priorities[order.id] ?? order.prioridadeOperacional ?? 0 : <div className="inline"><input type="number" min="0" value={priorities[order.id] ?? order.prioridadeOperacional ?? 0} onChange={(event) => onPriority(order, Number(event.target.value), false)} /><button className="small secondary" disabled={busyKey === `priority-${order.id}`} onClick={() => onPriority(order, priorities[order.id] ?? 0, true)}>Salvar</button></div>}</td>
    <td><div className="actions compact"><button className="small secondary" onClick={() => onDetails(order)}>Detalhes</button>{!readOnly && (order.statusOrdem === 'SUSPENSA' ? <button className="small" onClick={() => onResume(order)}>Retomar</button> : <button className="small warning" onClick={() => onSuspend(order)}>Suspender</button>)}</div></td>
  </tr>)}</tbody></table></div>;
}

function WorkQueue({ queue, expanded, edit, priorities, busyKey, onToggle, onEdit, onAction, onInstruction, onDetails }) {
  const jobs = queue.jobList ?? [];
  const derived = !queue.id || String(queue.identificador).startsWith('SEM_FILA|');
  const coberturaValida = queue.status === 'ATIVA'
    && queue.pow
    && queue.poolOperacional
    && queue.planoGuindasteId
    && queue.recursoCaisId
    && queue.equipamentoPatioId;
  return <article className={`queue ${derived ? 'queue-derived' : ''}`}><button className="queue-head" onClick={onToggle} aria-expanded={expanded}>
    <span><strong>{queue.identificador}</strong><small>{queue.berco || 'sem berço'} · {queue.blocoZona || 'sem zona'}</small></span><span>{derived && <ExceptionBadge type="SEM_FILA" />}<span className={statusClass(queue.status)}>{queue.status}</span><strong>{jobs.length || queue.totalOrdens || 0} jobs</strong>{expanded ? '▴' : '▾'}</span>
  </button>{expanded && <div className="queue-body">
    {!derived && <><div className="queue-editor">
      <label>POW<input value={edit.pow} onChange={(event) => onEdit({ ...edit, pow: event.target.value })} /></label>
      <label>Pool<input value={edit.poolOperacional} onChange={(event) => onEdit({ ...edit, poolOperacional: event.target.value })} /></label>
      <label>Porão<input type="number" min="1" value={edit.porao} onChange={(event) => onEdit({ ...edit, porao: event.target.value })} /></label>
      <label>Plano guindaste ID<input type="number" min="1" value={edit.planoGuindasteId} onChange={(event) => onEdit({ ...edit, planoGuindasteId: event.target.value })} /></label>
      <label>Recurso cais ID<input type="number" min="1" value={edit.recursoCaisId} onChange={(event) => onEdit({ ...edit, recursoCaisId: event.target.value })} /></label>
      <label>CHE real ID<input type="number" min="1" value={edit.equipamentoPatioId} onChange={(event) => onEdit({ ...edit, equipamentoPatioId: event.target.value })} /></label>
      <button className="secondary" disabled={busyKey === `resources-${queue.id}`} onClick={() => onAction('resources')}>Salvar recursos operacionais</button>
      <label>Limite<input type="number" min="1" value={edit.limite ?? ''} onChange={(event) => onEdit({ ...edit, limite: event.target.value ? Number(event.target.value) : null })} /></label>
    </div><div className="actions"><button className={queue.status === 'ATIVA' ? 'warning' : ''} onClick={() => onAction(queue.status === 'ATIVA' ? 'deactivate' : 'activate')}>{queue.status === 'ATIVA' ? 'Desativar' : 'Ativar'}</button><button disabled={!coberturaValida || busyKey === `dispatch-${queue.id}`} title={coberturaValida ? '' : 'Associe POW, pool, plano, recurso de cais e CHE real antes do dispatch.'} onClick={() => onAction('dispatch')}>Despachar</button></div></>}
    {jobs.length ? <div className="table-wrap nested"><table><thead><tr><th>Lote</th><th>Status</th><th>Destino</th><th>Prioridade</th><th>Ações</th></tr></thead><tbody>{jobs.map((job) => <tr key={job.id}><td>{job.codigoLote}</td><td><span className={statusClass(job.statusOrdem)}>{job.statusOrdem}</span></td><td>{job.destino || job.posicaoPlanejada || '—'}</td><td>{priorities[job.id] ?? job.prioridadeOperacional ?? 0}</td><td><div className="actions compact"><button className="small secondary" onClick={() => onDetails(job)}>Detalhes</button>{!derived && !['CONCLUIDA', 'CANCELADA'].includes(job.statusOrdem) && <>{['EM_EXECUCAO', 'BLOQUEADA', 'SUSPENSA'].includes(job.statusOrdem) && <button className="small secondary" onClick={() => onInstruction('reset', job)}>Resetar</button>}<button className="small danger" onClick={() => onInstruction('cancel', job)}>Cancelar</button></>}</div></td></tr>)}</tbody></table></div> : <p className="empty">A fila não possui jobs.</p>}
  </div>}</article>;
}

function ExceptionPanel({ alerts, uncovered }) {
  const grouped = useMemo(() => {
    const result = Object.fromEntries(Object.keys(EXCEPTIONS).map((key) => [key, []]));
    alerts.forEach((alert) => result[exceptionType(`${alert.tipo} ${alert.mensagem}`)].push(alert));
    uncovered.forEach((order) => result.SEM_FILA.push(order));
    return result;
  }, [alerts, uncovered]);
  return <section className="panel"><div className="section-head"><div><span className="eyebrow">Exceções</span><h2>Mapa de exceções operacionais</h2></div><span>{alerts.length + uncovered.length}</span></div><div className="exception-grid">{Object.entries(EXCEPTIONS).map(([type, meta]) => <article key={type} className={`exception-card exception-${type.toLowerCase().replaceAll('_', '-')}`}><div><ExceptionBadge type={type} /><strong>{meta[1]}</strong><b>{grouped[type].length}</b></div><p>{meta[2]}</p>{grouped[type].slice(0, 2).map((entry, index) => <small key={`${type}-${entry.id ?? index}`}>{entry.mensagem || entry.codigoLote || `Ordem ${entry.id}`}</small>)}</article>)}</div></section>;
}

function EquipmentPanel({ jobLists, queues, onDetails }) {
  const groups = useMemo(() => {
    if (jobLists.length) {
      return jobLists.map((entry) => ({
        id: entry.equipamentoIdentificador,
        equipamentoPatioId: entry.equipamentoPatioId,
        status: entry.equipamentoStatus,
        queues: entry.workQueues ?? [],
        jobs: (entry.workQueues ?? []).flatMap((queue) => queue.jobList ?? [])
      }));
    }
    const result = new Map();
    queues.forEach((queue) => {
      const id = queue.equipamento || 'SEM_EQUIPAMENTO';
      const item = result.get(id) ?? { id, queues: [], jobs: [] };
      item.queues.push(queue); item.jobs.push(...(queue.jobList ?? [])); result.set(id, item);
    });
    return [...result.values()];
  }, [jobLists, queues]);
  return <section className="panel"><div className="section-head"><div><span className="eyebrow">CHE</span><h2>Equipamentos e job lists</h2></div><span>{groups.length}</span></div><div className="equipment-grid">{groups.map((group) => <article key={group.equipamentoPatioId ?? group.id} className={`equipment-card ${group.id === 'SEM_EQUIPAMENTO' ? 'equipment-missing' : ''}`}><div className="equipment-head"><div><strong>{group.id}</strong><small>{group.queues.map((queue) => queue.pow || queue.identificador).join(' · ')}</small></div>{group.id === 'SEM_EQUIPAMENTO' ? <ExceptionBadge type="SEM_EQUIPAMENTO" /> : group.status && <span className={statusClass(group.status)}>{group.status}</span>}</div><div className="equipment-metrics"><span><b>{group.jobs.length}</b> jobs</span><span><b>{group.jobs.filter((job) => job.statusOrdem === 'EM_EXECUCAO').length}</b> executando</span><span><b>{group.queues.length}</b> filas</span></div><div className="equipment-jobs">{group.jobs.slice(0, 6).map((job) => <button className="job-chip" key={job.id} onClick={() => onDetails(job)}>#{job.sequenciaNavio ?? '—'} {job.codigoLote}<small>{job.statusOrdem}</small></button>)}{!group.jobs.length && <p className="empty">Sem jobs.</p>}</div></article>)}</div></section>;
}

function QuayMonitor({ visit, queues, alerts }) {
  return <section className="panel quay-monitor"><div className="section-head"><div><span className="eyebrow">Cais</span><h2>Quay Monitor</h2></div><span>{visit?.bercoAtual || visit?.bercoPrevisto || 'sem berço'}</span></div><div className="quay-grid">{queues.map((queue) => {
    const jobs = queue.jobList ?? [];
    const completed = jobs.filter((job) => job.statusOrdem === 'CONCLUIDA').length;
    const handling = jobs.filter((job) => job.statusOrdem === 'EM_EXECUCAO').length;
    const alarms = alerts.filter((alert) => jobs.some((job) => job.id === alert.ordemTrabalhoPatioId || job.itemOperacaoNavioId === alert.itemOperacaoNavioId)).length;
    const percent = jobs.length ? Math.round(completed * 100 / jobs.length) : 0;
    return <article key={queue.id ?? queue.identificador} className={`quay-card ${alarms ? 'quay-alarm' : ''}`}><div className="quay-title"><div><strong>{queue.pow || queue.equipamento || queue.identificador}</strong><small>{queue.berco || 'sem berço'} · {queue.identificador}</small></div>{alarms > 0 && <span className="alarm-count">{alarms} alerta(s)</span>}</div><div className="move-stages"><span><b>{completed}</b> concluídos</span><span><b>{jobs.length - completed}</b> a executar</span><span><b>{handling}</b> handling</span></div><div className="progress"><i style={{ width: `${percent}%` }} /></div><small>{percent}% da job list concluída</small></article>;
  })}{!queues.length && <p className="empty">Não existem filas para o Quay Monitor.</p>}</div></section>;
}

function InstructionDrawer({ context, onClose }) {
  if (!context) return null;
  const { order, item, reservation, queue, relatedAlerts, relatedEvents, divergences, drillDown } = context;
  return <div className="drawer-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><aside className="instruction-drawer" role="dialog" aria-modal="true"><div className="drawer-head"><div><span className="eyebrow">Work instruction</span><h2>{order.codigoLote || `Ordem ${order.id}`}</h2></div><button className="secondary" onClick={onClose}>Fechar</button></div><div className="detail-grid">
    <article><span>Status</span><strong className={statusClass(order.statusOrdem)}>{order.statusOrdem}</strong></article><article><span>Work queue</span><strong>{queue?.identificador || 'Sem fila'}</strong></article><article><span>POW / CHE</span><strong>{queue?.pow || 'sem POW'} · {queue?.equipamento || 'sem CHE'}</strong></article><article><span>Sequência</span><strong>{order.sequenciaNavio ?? '—'}</strong></article><article><span>Origem</span><strong>{order.origem || '—'}</strong></article><article><span>Destino planejado</span><strong>{order.destino || order.posicaoPlanejada || '—'}</strong></article><article><span>Posição real</span><strong>{order.posicaoReal || item?.posicaoPatioReal || '—'}</strong></article><article><span>Reserva</span><strong>{reservation ? `${reservation.posicaoPatioId} · ${reservation.status}` : 'Sem reserva'}</strong></article>
  </div><section className="drawer-section"><h3>Próximos estados permitidos</h3><p>{(drillDown?.proximosEstadosPermitidos ?? []).join(', ') || 'Nenhuma transição disponível.'}</p></section><section className="drawer-section"><h3>Item de navio</h3>{item ? <pre>{JSON.stringify(item, null, 2)}</pre> : <p className="empty">Item não localizado.</p>}</section><section className="drawer-section"><h3>Divergências</h3><div className="list">{divergences.map((entry, index) => <article key={`${entry.tipo}-${index}`}><div><strong>{entry.tipo}</strong><span className={statusClass(entry.severidade || 'ALTA')}>{entry.severidade || 'ALTA'}</span></div><p>{entry.mensagem}</p></article>)}{!divergences.length && <p className="empty">Nenhuma divergência.</p>}</div></section><section className="drawer-section"><h3>Auditoria da matriz oficial</h3><div className="timeline">{(drillDown?.auditoria ?? []).map((entry, index) => <article key={entry.id ?? index}><time>{dateTime(entry.criadoEm)}</time><div><strong>{entry.acao}</strong><p>{entry.motivo || entry.detalhes}</p><small>{entry.usuario}</small></div></article>)}{!(drillDown?.auditoria ?? []).length && <p className="empty">Nenhuma auditoria relacionada.</p>}</div></section><section className="drawer-section"><h3>Auditoria e eventos</h3><div className="timeline">{relatedEvents.map((event) => <article key={event.id}><time>{dateTime(event.criadoEm)}</time><div><strong>{event.tipoEvento}</strong><p>{event.descricao}</p><small>{event.usuario}</small></div></article>)}{!relatedEvents.length && <p className="empty">Nenhum evento relacionado.</p>}</div></section><section className="drawer-section"><h3>Alertas relacionados</h3><div className="list">{relatedAlerts.map((alert, index) => <article key={`${alert.tipo}-${index}`}><div><strong>{alert.tipo}</strong><span className={statusClass(alert.severidade)}>{alert.severidade}</span></div><p>{alert.mensagem}</p></article>)}{!relatedAlerts.length && <p className="empty">Nenhum alerta relacionado.</p>}</div></section></aside></div>;
}

function ControlRoom({ session, onLogout }) {
  const [navios, setNavios] = useState([]), [visits, setVisits] = useState([]), [visitId, setVisitId] = useState(null);
  const [summary, setSummary] = useState(EMPTY_SUMMARY), [integration, setIntegration] = useState(EMPTY_INTEGRATION);
  const [items, setItems] = useState([]), [events, setEvents] = useState([]), [reservations, setReservations] = useState([]), [orders, setOrders] = useState([]), [queues, setQueues] = useState([]), [workQueues, setWorkQueues] = useState([]), [equipmentJobLists, setEquipmentJobLists] = useState([]), [uncovered, setUncovered] = useState([]), [alerts, setAlerts] = useState([]);
  const [yardQueries, setYardQueries] = useState({ filas: null, cobertura: null });
  const [statusFilter, setStatusFilter] = useState(''), [zoneFilter, setZoneFilter] = useState(''), [severityFilter, setSeverityFilter] = useState('');
  const [lastUpdate, setLastUpdate] = useState(null), [streamStatus, setStreamStatus] = useState('CONECTANDO'), [expanded, setExpanded] = useState({}), [edits, setEdits] = useState({}), [priorities, setPriorities] = useState({}), [selected, setSelected] = useState(null);
  const [busy, setBusy] = useState(false), [busyKey, setBusyKey] = useState(''), [error, setError] = useState(''), [success, setSuccess] = useState(''), [result, setResult] = useState(null);
  const activeRequest = useRef(null), version = useRef(0);
  const selectedVisit = useMemo(() => visits.find((visit) => visit.id === visitId), [visits, visitId]);

  const loadSnapshot = useCallback(async (id, silent = false) => {
    if (!id) return;
    if (activeRequest.current?.visitId === id) return activeRequest.current.promise;
    const requestVersion = ++version.current;
    if (!silent) setBusy(true);
    const promise = Promise.all([api.listarItensVisita(id), api.obterResumo(id), api.listarEventos(id), api.obterResumoIntegracaoPatio(id), api.listarReservasPatio(id), api.listarOrdensPatio(id), api.listarFilasPatio(id), api.listarWorkQueuesPatio(id), api.listarOrdensSemCoberturaPatio(id), api.listarAlertasIntegracaoPatio(id), api.listarJobListsEquipamentoPatio(id)]).then(([a, b, c, d, e, f, g, h, i, j, k]) => {
      if (requestVersion !== version.current) return;
      setItems(a); setSummary(b); setEvents(c); setIntegration(d); setReservations(e); setOrders(f); setQueues(g.dados); setWorkQueues(h); setUncovered(i.dados); setAlerts(j); setEquipmentJobLists(k); setYardQueries({ filas: g, cobertura: i }); setLastUpdate(new Date());
      setPriorities((current) => [...f, ...h.flatMap((queue) => queue.jobList ?? [])].reduce((acc, order) => order.id ? { ...acc, [order.id]: current[order.id] ?? order.prioridadeOperacional ?? 0 } : acc, {}));
      setEdits((current) => h.reduce((acc, queue) => ({ ...acc, [queue.id]: current[queue.id] ?? { pow: queue.pow || '', poolOperacional: queue.poolOperacional || '', porao: queue.porao ?? '', planoGuindasteId: queue.planoGuindasteId ?? '', recursoCaisId: queue.recursoCaisId ?? '', equipamentoPatioId: queue.equipamentoPatioId ?? '', limite: null } }), {}));
    }).finally(() => { if (activeRequest.current?.promise === promise) activeRequest.current = null; if (!silent) setBusy(false); });
    activeRequest.current = { visitId: id, promise }; return promise;
  }, []);

  useEffect(() => { let active = true; Promise.all([api.listarNavios(), api.listarVisitas()]).then(([ships, newVisits]) => { if (!active) return; setNavios(ships); setVisits(newVisits); setVisitId(newVisits[0]?.id ?? null); }).catch((reason) => setError(formatError(reason))); return () => { active = false; }; }, []);
  useEffect(() => { if (visitId) loadSnapshot(visitId).catch((reason) => setError(formatError(reason))); }, [visitId, loadSnapshot]);
  useEffect(() => {
    if (!visitId) return undefined;
    return api.assinarControlRoom(visitId, { onState: setStreamStatus, onEvent: () => loadSnapshot(visitId, true).catch((reason) => setError(formatError(reason))), onError: (reason) => setError(formatError(reason)) });
  }, [visitId, loadSnapshot]);

  async function action(key, operation, message) { setBusyKey(key); setError(''); setSuccess(''); try { const response = await operation(); if (response !== undefined) setResult(response); setSuccess(message); return response; } catch (reason) { setError(formatError(reason)); return undefined; } finally { setBusyKey(''); } }
  async function queueAction(queue, type) {
    const edit = edits[queue.id] ?? EMPTY_QUEUE_EDIT;
    const operations = {
      activate: () => api.ativarWorkQueuePatio(queue.id),
      deactivate: () => api.desativarWorkQueuePatio(queue.id),
      resources: () => api.atualizarRecursosWorkQueuePatio(queue.id, { pow: edit.pow, poolOperacional: edit.poolOperacional, porao: nullableNumber(edit.porao), planoGuindasteId: nullableNumber(edit.planoGuindasteId), recursoCaisId: nullableNumber(edit.recursoCaisId), equipamentoPatioId: nullableNumber(edit.equipamentoPatioId) }),
      dispatch: () => api.despacharWorkQueuePatio(queue.id, { limiteOrdens: edit.limite || null, somentePendentes: true })
    };
    await action(`${type}-${queue.id}`, operations[type], 'Work queue atualizada.');
    await loadSnapshot(visitId, true);
  }
  async function instructionAction(type, order) { await action(`${type}-${order.id}`, () => type === 'reset' ? api.resetarWorkInstructionPatio(order.id) : api.cancelarWorkInstructionPatio(order.id), type === 'reset' ? 'Work instruction resetada.' : 'Work instruction cancelada.'); await loadSnapshot(visitId, true); }
  function changePriority(order, value, persist) { setPriorities((current) => ({ ...current, [order.id]: value })); if (persist) action(`priority-${order.id}`, () => api.atualizarPrioridadesWorkInstructionPatio(order.id, { prioridadeOperacional: value, prioridadeBusca: order.prioridadeBusca ?? false }), 'Prioridade atualizada.').then(() => loadSnapshot(visitId, true)); }
  async function showDetails(order) { const drillDown = await action(`details-${order.id}`, () => api.obterDrillDownWorkInstructionPatio(order.id), 'Detalhes atualizados.'); setSelected({ ...order, drillDown }); }

  const filteredOrders = orders.filter((order) => (!statusFilter || order.statusOrdem === statusFilter) && (!zoneFilter || normalize(`${order.origem} ${order.destino} ${order.posicaoPlanejada}`).includes(normalize(zoneFilter))));
  const filteredQueues = workQueues.filter((queue) => (!statusFilter || queue.status === statusFilter) && (!zoneFilter || normalize(`${queue.identificador} ${queue.berco} ${queue.blocoZona} ${queue.pow}`).includes(normalize(zoneFilter))));
  const filteredAlerts = alerts.filter((alert) => !severityFilter || alert.severidade === severityFilter);
  const imminent = orders.filter((order) => ['PENDENTE', 'EM_EXECUCAO'].includes(order.statusOrdem)).sort((a, b) => (a.sequenciaNavio ?? 9999) - (b.sequenciaNavio ?? 9999)).slice(0, 5);
  const degradedQueries = [yardQueries.filas, yardQueries.cobertura].filter((query) => query?.status === 'DEGRADADA');
  const degradedReason = degradedQueries.map((query) => query.motivoDegradacao).filter(Boolean).join(' ');
  const context = useMemo(() => {
    if (!selected) return null;
    const order = [...orders, ...workQueues.flatMap((queue) => queue.jobList ?? []), ...uncovered].find((entry) => entry.id === selected.id) ?? selected;
    const item = items.find((entry) => entry.id === order.itemOperacaoNavioId), reservation = reservations.find((entry) => entry.itemOperacaoNavioId === order.itemOperacaoNavioId), queue = selected.drillDown?.workQueue ?? workQueues.find((entry) => (entry.jobList ?? []).some((job) => job.id === order.id));
    const relatedAlerts = alerts.filter((entry) => entry.ordemTrabalhoPatioId === order.id || entry.itemOperacaoNavioId === order.itemOperacaoNavioId), needles = [order.id, order.itemOperacaoNavioId, order.codigoLote].filter(Boolean).map(String), relatedEvents = events.filter((entry) => needles.some((needle) => `${entry.tipoEvento} ${entry.descricao}`.includes(needle))), divergences = [...relatedAlerts];
    const planned = order.posicaoPlanejada || order.destino, actual = order.posicaoReal || item?.posicaoPatioReal;
    if (planned && actual && planned !== actual) divergences.push({ tipo: 'DIVERGENCIA_POSICAO', severidade: 'ALTA', mensagem: `Planejada ${planned}; real ${actual}.` });
    if (!queue) divergences.push({ tipo: 'SEM_FILA', severidade: 'ALTA', mensagem: 'Work instruction sem fila persistente.' }); else { if (!queue.pow) divergences.push({ tipo: 'SEM_POW', severidade: 'ALTA', mensagem: 'Fila sem POW.' }); if (!queue.equipamentoPatioId) divergences.push({ tipo: 'SEM_EQUIPAMENTO', severidade: 'ALTA', mensagem: 'Fila sem CHE real.' }); }
    return { order, item, reservation, queue, relatedAlerts, relatedEvents, divergences, drillDown: selected.drillDown };
  }, [selected, orders, workQueues, uncovered, items, reservations, alerts, events]);

  return <div className="app"><header className="topbar"><div><span className="eyebrow">CloudPort</span><h1>Control Room Navio + Pátio</h1></div><div className="top-actions"><StreamStatus value={streamStatus} /><span>{session.nome}</span><button className="secondary" onClick={() => action('refresh', () => loadSnapshot(visitId), 'Atualizado.')}>Atualizar</button><button className="danger" onClick={onLogout}>Sair</button></div></header><main className="content">
    {error && <div className="message error" role="alert">{error}</div>}{success && <div className="message success">{success}</div>}
    {degradedQueries.length > 0 && <div className="message" role="status" style={{ border: '1px solid #fcd34d', background: '#fffbeb', color: '#92400e' }}><strong>Modo de contingência do Yard.</strong> Filas ou cobertura foram derivados localmente e não representam estado operacional confirmado. {degradedReason}</div>}
    <section className="panel selector"><label>Visita<select value={visitId ?? ''} onChange={(event) => setVisitId(Number(event.target.value))}>{visits.map((visit) => <option key={visit.id} value={visit.id}>{visit.codigoVisita} · {visit.navioNome || navios.find((ship) => ship.id === visit.navioId)?.nome}</option>)}</select></label><div><span className={statusClass(selectedVisit?.fase)}>{selectedVisit?.fase || 'SEM_VISITA'}</span><small>{selectedVisit?.bercoAtual || selectedVisit?.bercoPrevisto || 'sem berço'} · {dateTime(lastUpdate)}</small></div>{PHASES[selectedVisit?.fase] && <button onClick={() => action('phase', async () => { const updated = await api.alterarFaseVisita(visitId, PHASES[selectedVisit.fase]); setVisits((current) => current.map((visit) => visit.id === updated.id ? updated : visit)); }, 'Fase atualizada.')}>Avançar fase</button>}</section>
    <section className="metrics"><Metric label="Progresso" value={`${number(summary.percentualProgresso, 1)}%`} detail={`${summary.totalItensOperados}/${summary.totalItensPlanejados} itens`} /><Metric label="Peso operado" value={`${number(summary.pesoOperado, 1)} t`} detail={`${number(summary.pesoPlanejado, 1)} t planejadas`} /><Metric label="Ordens" value={integration.itensComOrdem} detail={`${integration.ordensEmExecucao} em execução`} /><Metric label="Alertas" value={integration.totalAlertas} detail={integration.statusPredominante} /></section>
    <section className="panel"><div className="section-head"><div><span className="eyebrow">Operação</span><h2>Ações e filtros</h2></div><StreamStatus value={streamStatus} /></div><div className="actions"><button onClick={() => action('reserve', async () => { await api.gerarReservasPatio(visitId); await loadSnapshot(visitId, true); }, 'Reservas geradas.')}>Gerar reservas</button><button onClick={() => action('orders', async () => { await api.gerarOrdensPatio(visitId); await loadSnapshot(visitId, true); }, 'Ordens geradas.')}>Gerar ordens</button><button className="secondary" onClick={() => action('sync', async () => { await api.sincronizarStatusPatio(visitId); await loadSnapshot(visitId, true); }, 'Yard sincronizado.')}>Sincronizar Yard</button><button className="secondary" onClick={() => action('simulate', () => api.replanejarPatioVisita(visitId, false), 'Simulação concluída.')}>Simular</button><button className="warning" onClick={() => action('apply', () => api.replanejarPatioVisita(visitId, true), 'Replanejamento aplicado.')}>Aplicar replanejamento</button></div><div className="filters"><label>Status<select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}><option value="">Todos</option>{['PENDENTE', 'EM_EXECUCAO', 'BLOQUEADA', 'SUSPENSA', 'CONCLUIDA', 'CANCELADA'].map((value) => <option key={value}>{value}</option>)}</select></label><label>Bloco/zona<input value={zoneFilter} onChange={(event) => setZoneFilter(event.target.value)} /></label><label>Severidade<select value={severityFilter} onChange={(event) => setSeverityFilter(event.target.value)}><option value="">Todas</option>{['BAIXA', 'MEDIA', 'ALTA', 'CRITICA'].map((value) => <option key={value}>{value}</option>)}</select></label></div></section>
    <section className="panel"><div className="section-head"><div><span className="eyebrow">Execução</span><h2>Movimentos iminentes</h2></div><span>{imminent.length}</span></div><div className="imminent">{imminent.map((order) => <article key={order.id} onClick={() => showDetails(order)}><span>#{order.sequenciaNavio ?? '—'}</span><strong>{order.codigoLote}</strong><small>{order.origem || '—'} → {order.destino || order.posicaoPlanejada || '—'}</small><span className={statusClass(order.statusOrdem)}>{order.statusOrdem}</span></article>)}</div></section>
    <QuayMonitor visit={selectedVisit} queues={filteredQueues} alerts={filteredAlerts} /><EquipmentPanel jobLists={equipmentJobLists} queues={filteredQueues} onDetails={showDetails} /><ExceptionPanel alerts={filteredAlerts} uncovered={uncovered} />
    <section className="panel"><div className="section-head"><div><span className="eyebrow">Equipment Control</span><h2>Work queues e job lists</h2></div><span>{filteredQueues.length}</span></div>{filteredQueues.map((queue) => <WorkQueue key={queue.id ?? queue.identificador} queue={queue} expanded={!!expanded[queue.id ?? queue.identificador]} edit={edits[queue.id] ?? EMPTY_QUEUE_EDIT} priorities={priorities} busyKey={busyKey} onToggle={() => setExpanded((current) => ({ ...current, [queue.id ?? queue.identificador]: !current[queue.id ?? queue.identificador] }))} onEdit={(edit) => setEdits((current) => ({ ...current, [queue.id]: edit }))} onAction={(type) => queueAction(queue, type)} onInstruction={instructionAction} onDetails={showDetails} />)}</section>
    <section className="panel"><div className="section-head"><div><span className="eyebrow">Yard</span><h2>Ordens de pátio</h2></div><span>{filteredOrders.length}/{orders.length}</span></div><OrdersTable orders={filteredOrders} priorities={priorities} busyKey={busyKey} onPriority={changePriority} onDetails={showDetails} onSuspend={(order) => action(`suspend-${order.id}`, () => api.suspenderWorkInstructionPatio(order.id), 'Ordem suspensa.')} onResume={(order) => action(`resume-${order.id}`, () => api.retomarWorkInstructionPatio(order.id), 'Ordem retomada.')} /></section>
    <div className="columns"><section className="panel"><div className="section-head"><h2>Alertas</h2><span>{filteredAlerts.length}</span></div><div className="list">{filteredAlerts.map((alert, index) => { const type = exceptionType(`${alert.tipo} ${alert.mensagem}`); return <article className={`alert-card exception-${type.toLowerCase().replaceAll('_', '-')}`} key={`${alert.tipo}-${index}`}><div><strong>{alert.tipo}</strong><span><ExceptionBadge type={type} /><span className={statusClass(alert.severidade)}>{alert.severidade}</span></span></div><p>{alert.mensagem}</p></article>; })}</div></section><section className="panel"><div className="section-head"><h2>Eventos recentes</h2><span>{events.length}</span></div><div className="list scroll">{events.slice(0, 30).map((event) => <article key={event.id}><div><strong>{event.tipoEvento}</strong><time>{dateTime(event.criadoEm)}</time></div><p>{event.descricao}</p><small>{event.usuario}</small></article>)}</div></section></div>
    <div className="columns"><section className="panel"><div className="section-head"><h2>Ordens sem cobertura</h2><span>{uncovered.length}</span></div><OrdersTable orders={uncovered} priorities={priorities} busyKey={busyKey} readOnly onPriority={() => {}} onSuspend={() => {}} onResume={() => {}} onDetails={showDetails} /></section><section className="panel"><div className="section-head"><h2>Reservas</h2><span>{reservations.length}</span></div><div className="list scroll">{reservations.map((reservation) => <article key={reservation.id}><div><strong>{reservation.posicaoPatioId}</strong><span className={statusClass(reservation.status)}>{reservation.status}</span></div><p>Item {reservation.itemOperacaoNavioId} · {reservation.bloco || 'sem bloco'} · {reservation.tipoReserva}</p></article>)}</div></section></div>
    {result && <section className="panel"><div className="section-head"><h2>Resultado da última operação</h2><button className="small secondary" onClick={() => setResult(null)}>Fechar</button></div><pre>{JSON.stringify(result, null, 2)}</pre></section>}<footer>{items.length} itens · {queues.length} filas · {summary.divergenciasPoraoPosicao} divergências</footer>{busy && <div className="loading">Carregando...</div>}
  </main><InstructionDrawer context={context} onClose={() => setSelected(null)} /></div>;
}

export default function App() {
  const [session, setSession] = useState(() => readSession()), [ready, setReady] = useState(false), [error, setError] = useState('');
  useEffect(() => { loadRuntimeConfig().then(() => setReady(true)).catch((reason) => setError(formatError(reason))); }, []);
  const authenticate = useCallback((value) => setSession(value), []), logout = useCallback(() => { clearSession(); setSession(null); }, []);
  if (error) return <main className="auth-shell"><div className="auth-card"><h1>Control Room indisponível</h1><div className="message error">{error}</div></div></main>;
  if (!ready) return <main className="auth-shell"><div className="auth-card"><h1>CloudPort</h1><p>Carregando configuração...</p></div></main>;
  return session && hasAnyRole(session, ...ROLES) ? <ControlRoom session={session} onLogout={logout} /> : <AuthGate onAuthenticated={authenticate} />;
}
