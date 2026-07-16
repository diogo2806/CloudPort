import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, clearSession, formatError, hasAnyRole, loadRuntimeConfig, readSession, saveSession } from './api.js';

const PHASES = {
  PREVISTA: 'FUNDEADA',
  FUNDEADA: 'ATRACADA',
  ATRACADA: 'OPERANDO',
  OPERANDO: 'OPERACAO_CONCLUIDA',
  OPERACAO_CONCLUIDA: 'PARTIU'
};
const EMPTY_SUMMARY = {
  totalItensPlanejados: 0,
  totalItensOperados: 0,
  pesoPlanejado: 0,
  pesoOperado: 0,
  percentualProgresso: 0,
  divergenciasPoraoPosicao: 0,
  itensBloqueados: 0
};
const EMPTY_INTEGRATION = {
  itensComReserva: 0,
  itensComOrdem: 0,
  itensSemReserva: 0,
  itensSemOrdem: 0,
  ordensEmExecucao: 0,
  ordensConcluidas: 0,
  totalAlertas: 0,
  statusPredominante: 'NAO_GERADO'
};
const EMPTY_STRUCTURAL = {
  limitePesoPorPoraoToneladas: '',
  limitePesoPorCamadaToneladas: '',
  desequilibrioBombordoBoresteMaximoPercentual: '',
  alturaMaximaCamadas: '',
  exigirLashingAPartirCamada: '',
  posicoesComLashing: '',
  poroesInterditados: ''
};
const ALLOWED_ROLES = ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE'];

const clean = (value) => String(value ?? '').normalize('NFKC').replace(/[<>"'`\\]/g, '').trim();
const formatNumber = (value, digits = 0) => Number(value ?? 0).toLocaleString('pt-BR', {
  minimumFractionDigits: digits,
  maximumFractionDigits: digits
});
const statusClass = (status) => `status status-${String(status ?? 'indefinido').toLowerCase().replaceAll('_', '-')}`;
const dateTime = (value) => value ? new Date(value).toLocaleString('pt-BR') : '—';
const valueOrNull = (value) => value === '' || value === null || value === undefined ? null : Number(value);
const splitValues = (value) => clean(value).split(/[;,\s]+/).map((entry) => entry.trim()).filter(Boolean);

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
      trustedOrigins.current = new Set([
        window.location.origin,
        'http://localhost:4200',
        ...(config.trustedParentOrigins ?? [])
      ]);
      window.parent?.postMessage({ type: 'CLOUDPORT_CONTROL_ROOM_READY' }, '*');
    }).catch((reason) => active && setError(formatError(reason)));

    const receiveSession = (event) => {
      if (!trustedOrigins.current.has(event.origin) || event.data?.type !== 'CLOUDPORT_AUTH_SESSION') return;
      try {
        const session = saveSession(event.data.session);
        if (!hasAnyRole(session, ...ALLOWED_ROLES)) {
          throw new Error('A conta não possui permissão operacional para acessar o Control Room.');
        }
        onAuthenticated(session);
      } catch (reason) {
        clearSession();
        setError(formatError(reason));
      }
    };
    window.addEventListener('message', receiveSession);
    return () => {
      active = false;
      window.removeEventListener('message', receiveSession);
    };
  }, [onAuthenticated]);

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    try {
      const session = saveSession(await api.autenticar(clean(login), senha));
      if (!hasAnyRole(session, ...ALLOWED_ROLES)) {
        throw new Error('A conta não possui permissão operacional para acessar o Control Room.');
      }
      setSenha('');
      onAuthenticated(session);
    } catch (reason) {
      clearSession();
      setError(formatError(reason, 'Não foi possível autenticar.'));
    } finally {
      setBusy(false);
    }
  }

  return <main className="auth-shell">
    <form className="auth-card" onSubmit={submit}>
      <span className="eyebrow">CloudPort</span>
      <h1>Control Room Navio + Pátio</h1>
      <p>Entre com uma conta operacional autorizada.</p>
      <label>Login<input value={login} onChange={(event) => setLogin(event.target.value)} autoComplete="username" required /></label>
      <label>Senha<input type="password" value={senha} onChange={(event) => setSenha(event.target.value)} autoComplete="current-password" required /></label>
      {error && <div className="message error" role="alert">{error}</div>}
      <button disabled={busy || !login || !senha}>{busy ? 'Autenticando...' : 'Entrar'}</button>
    </form>
  </main>;
}

function Metric({ label, value, detail }) {
  return <article className="metric"><span>{label}</span><strong>{value}</strong>{detail && <small>{detail}</small>}</article>;
}

function OrdersTable({ orders, priorities, onPriority, onSuspend, onResume, busyKey, readOnly = false }) {
  if (!orders.length) return <p className="empty">Nenhuma ordem encontrada.</p>;
  return <div className="table-wrap"><table><thead><tr>
    <th>Lote</th><th>Status</th><th>Origem</th><th>Destino</th><th>Seq.</th><th>Prioridade</th><th>Ações</th>
  </tr></thead><tbody>{orders.map((order) => <tr key={order.id ?? `${order.codigoLote}-${order.sequenciaNavio}`}>
    <td><strong>{order.codigoLote}</strong><small>{order.tipoMovimento}</small></td>
    <td><span className={statusClass(order.statusOrdem)}>{order.statusOrdem}</span></td>
    <td>{order.origem || '—'}</td><td>{order.destino || order.posicaoPlanejada || '—'}</td><td>{order.sequenciaNavio ?? '—'}</td>
    <td>{readOnly ? priorities[order.id] ?? order.prioridadeOperacional ?? 0 : <div className="inline">
      <input type="number" min="0" value={priorities[order.id] ?? order.prioridadeOperacional ?? order.sequenciaNavio ?? 0} onChange={(event) => onPriority(order, Number(event.target.value), false)} />
      <button className="small secondary" disabled={busyKey === `priority-${order.id}`} onClick={() => onPriority(order, priorities[order.id] ?? order.prioridadeOperacional ?? 0, true)}>Salvar</button>
    </div>}</td>
    <td>{readOnly ? '—' : order.statusOrdem === 'SUSPENSA'
      ? <button className="small" disabled={busyKey === `resume-${order.id}`} onClick={() => onResume(order)}>Retomar</button>
      : <button className="small warning" disabled={busyKey === `suspend-${order.id}`} onClick={() => onSuspend(order)}>Suspender</button>}</td>
  </tr>)}</tbody></table></div>;
}

function WorkQueue({ queue, edit, expanded, priorities, busyKey, onToggle, onEdit, onAction, onInstruction }) {
  const jobs = queue.jobList ?? [];
  return <article className="queue">
    <button className="queue-head" onClick={onToggle} aria-expanded={expanded}>
      <span><strong>{queue.identificador}</strong><small>{queue.agrupamento} · {queue.berco || 'sem berço'} · {queue.blocoZona || 'sem zona'}</small></span>
      <span><span className={statusClass(queue.status)}>{queue.status}</span><strong>{jobs.length || queue.totalOrdens || 0} jobs</strong>{expanded ? '▴' : '▾'}</span>
    </button>
    {expanded && <div className="queue-body">
      <div className="queue-editor">
        <label>POW<input value={edit.pow} onChange={(event) => onEdit({ ...edit, pow: event.target.value })} /></label>
        <label>Pool<input value={edit.poolOperacional} onChange={(event) => onEdit({ ...edit, poolOperacional: event.target.value })} /></label>
        <button className="secondary" disabled={busyKey === `pow-${queue.id}`} onClick={() => onAction('pow')}>Salvar POW/pool</button>
        <label>Equipamento<input value={edit.equipamento} onChange={(event) => onEdit({ ...edit, equipamento: event.target.value })} /></label>
        <button className="secondary" disabled={busyKey === `equipment-${queue.id}`} onClick={() => onAction('equipment')}>Salvar equipamento</button>
        <label>Limite<input type="number" min="1" value={edit.limite ?? ''} onChange={(event) => onEdit({ ...edit, limite: event.target.value ? Number(event.target.value) : null })} /></label>
      </div>
      <div className="actions">
        {queue.status === 'ATIVA' ? <button className="warning" onClick={() => onAction('deactivate')}>Desativar</button> : <button onClick={() => onAction('activate')}>Ativar</button>}
        <button onClick={() => onAction('dispatch')}>Despachar</button>
      </div>
      {jobs.length ? <div className="table-wrap nested"><table><thead><tr>
        <th>Lote</th><th>Status</th><th>Destino</th><th>Prioridade</th><th>Ações</th>
      </tr></thead><tbody>{jobs.map((job) => <tr key={job.id}>
        <td>{job.codigoLote}</td><td><span className={statusClass(job.statusOrdem)}>{job.statusOrdem}</span></td>
        <td>{job.destino || job.posicaoPlanejada || '—'}</td><td>{priorities[job.id] ?? job.prioridadeOperacional ?? 0}</td>
        <td><div className="actions compact"><button className="small secondary" onClick={() => onInstruction('reset', job)}>Resetar</button><button className="small danger" onClick={() => onInstruction('cancel', job)}>Cancelar</button></div></td>
      </tr>)}</tbody></table></div> : <p className="empty">A fila não possui jobs.</p>}
    </div>}
  </article>;
}

function QuayMonitor({ data }) {
  if (!data) return <p className="empty">Quay Monitor sem dados.</p>;
  return <div className="monitor-grid">
    <Metric label="Berço" value={data.berco || '—'} detail={data.fase} />
    <Metric label="Movimentos" value={`${data.movimentosExecutados}/${data.movimentosPlanejados}`} detail={`${data.movimentosPendentes} pendentes`} />
    <Metric label="Produtividade" value={`${formatNumber(data.movimentosPorHora, 2)} mov/h`} detail={`Conclusão ${dateTime(data.previsaoConclusao)}`} />
    <Metric label="Risco" value={data.riscoOperacional} detail={`${data.equipamentosAlocados} CHE · ${data.workQueuesAtivas} filas`} />
  </div>;
}

function VesselView({ rows = [] }) {
  if (!rows.length) return <p className="empty">Nenhum porão planejado.</p>;
  return <div className="visual-grid">{rows.map((row) => <article className="visual-card" key={row.porao}>
    <div><strong>Porão {row.porao || 'não definido'}</strong><span className={row.alertas ? 'badge critical' : 'badge'}>{row.alertas} alertas</span></div>
    <p>{row.itensOperados}/{row.itensPlanejados} itens operados</p>
    <small>{formatNumber(row.pesoOperadoToneladas, 1)} t de {formatNumber(row.pesoPlanejadoToneladas, 1)} t</small>
    <progress max={Math.max(1, row.itensPlanejados)} value={row.itensOperados} />
  </article>)}</div>;
}

function YardView({ rows = [] }) {
  if (!rows.length) return <p className="empty">Nenhum bloco relacionado à visita.</p>;
  return <div className="visual-grid">{rows.map((row) => <article className="visual-card yard" key={row.blocoZona}>
    <div><strong>{row.blocoZona}</strong><span>{row.reservasAtivas} reservas</span></div>
    <p>{row.ordensPendentes} pendentes · {row.ordensEmExecucao} em execução</p>
    <small>{row.ordensConcluidas} concluídas · {row.posicoesDivergentes} divergências</small>
  </article>)}</div>;
}

function CheDetail({ rows = [] }) {
  if (!rows.length) return <p className="empty">Nenhum CHE alocado às work queues.</p>;
  return <div className="table-wrap"><table><thead><tr>
    <th>CHE</th><th>Status</th><th>Fila/POW</th><th>Jobs</th><th>VMT/posição</th><th>Telemetria</th>
  </tr></thead><tbody>{rows.map((row, index) => <tr key={`${row.equipamento}-${row.workQueue}-${index}`}>
    <td><strong>{row.equipamento}</strong><small>{row.tipo || 'tipo não informado'}</small></td>
    <td><span className={statusClass(row.statusOperacional)}>{row.statusOperacional || 'SEM_STATUS'}</span></td>
    <td>{row.workQueue || '—'}<small>{row.pow || 'sem POW'} · {row.poolOperacional || 'sem pool'}</small></td>
    <td>{row.jobs}<small>{row.jobsEmExecucao} em execução</small></td>
    <td>{row.posicaoMaisProxima || '—'}<small>WI {row.workInstructionAtualId || '—'}</small></td>
    <td>{dateTime(row.telemetriaAtualizadaEm)}<small>{row.latitude != null ? `${row.latitude}, ${row.longitude}` : 'sem GPS'}</small></td>
  </tr>)}</tbody></table></div>;
}

function OperationalIssues({ title, rows = [], type }) {
  if (!rows.length) return <section className="panel"><div className="section-head"><h2>{title}</h2><span>0</span></div><p className="empty">Nenhum registro.</p></section>;
  return <section className="panel"><div className="section-head"><h2>{title}</h2><span>{rows.length}</span></div><div className="list">{rows.map((row, index) => <article key={`${row.codigo || row.categoria}-${index}`}>
    <div><strong>{type === 'gargalo' ? row.codigo : `${row.codigoLote} · ${row.categoria}`}</strong><span className={statusClass(row.severidade)}>{row.severidade}</span></div>
    <p>{type === 'gargalo' ? row.causa : row.mensagem}</p>
    <small>{type === 'gargalo' ? `${row.recurso} · ${row.quantidadeAfetada} afetados · ${row.acaoRecomendada}` : `${row.planejado || '—'} → ${row.executado || '—'}`}</small>
  </article>)}</div></section>;
}

function StructuralForm({ value, onChange, onValidate, result, busy }) {
  const set = (field, next) => onChange({ ...value, [field]: next });
  return <section className="panel">
    <div className="section-head"><div><span className="eyebrow">Segurança do plano</span><h2>Lashing, estabilidade, segregação e estrutura</h2></div>{result && <span className={statusClass(result.status)}>{result.status}</span>}</div>
    <div className="structural-grid">
      <label>Limite por porão (t)<input type="number" min="0" step="0.001" value={value.limitePesoPorPoraoToneladas} onChange={(event) => set('limitePesoPorPoraoToneladas', event.target.value)} /></label>
      <label>Limite por camada (t)<input type="number" min="0" step="0.001" value={value.limitePesoPorCamadaToneladas} onChange={(event) => set('limitePesoPorCamadaToneladas', event.target.value)} /></label>
      <label>Desequilíbrio BB/BE máximo (%)<input type="number" min="0" step="0.01" value={value.desequilibrioBombordoBoresteMaximoPercentual} onChange={(event) => set('desequilibrioBombordoBoresteMaximoPercentual', event.target.value)} /></label>
      <label>Altura máxima de camadas<input type="number" min="1" value={value.alturaMaximaCamadas} onChange={(event) => set('alturaMaximaCamadas', event.target.value)} /></label>
      <label>Lashing obrigatório a partir da camada<input type="number" min="1" value={value.exigirLashingAPartirCamada} onChange={(event) => set('exigirLashingAPartirCamada', event.target.value)} /></label>
      <label>Posições com lashing<input value={value.posicoesComLashing} onChange={(event) => set('posicoesComLashing', event.target.value)} placeholder="1-3-2-BB; 1-3-2-BE" /></label>
      <label>Porões interditados<input value={value.poroesInterditados} onChange={(event) => set('poroesInterditados', event.target.value)} placeholder="2; 4" /></label>
    </div>
    <div className="actions"><button disabled={busy} onClick={onValidate}>Validar plano estrutural</button></div>
    {result && <div className="validation-summary"><strong>{result.erros?.length || 0} erros · {result.alertas?.length || 0} alertas</strong><small>Não configurado: {(result.verificacoesNaoConfiguradas ?? []).join(', ') || 'nenhuma verificação'}</small></div>}
  </section>;
}

function ControlRoom({ session, onLogout }) {
  const [navios, setNavios] = useState([]);
  const [visits, setVisits] = useState([]);
  const [visitId, setVisitId] = useState(null);
  const [summary, setSummary] = useState(EMPTY_SUMMARY);
  const [integration, setIntegration] = useState(EMPTY_INTEGRATION);
  const [advanced, setAdvanced] = useState(null);
  const [items, setItems] = useState([]);
  const [events, setEvents] = useState([]);
  const [reservations, setReservations] = useState([]);
  const [orders, setOrders] = useState([]);
  const [queues, setQueues] = useState([]);
  const [workQueues, setWorkQueues] = useState([]);
  const [uncovered, setUncovered] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [zoneFilter, setZoneFilter] = useState('');
  const [severityFilter, setSeverityFilter] = useState('');
  const [streamState, setStreamState] = useState('DESCONECTADO');
  const [telemetryStreamState, setTelemetryStreamState] = useState('DESCONECTADO');
  const [lastUpdate, setLastUpdate] = useState(null);
  const [expanded, setExpanded] = useState({});
  const [edits, setEdits] = useState({});
  const [priorities, setPriorities] = useState({});
  const [structuralConfig, setStructuralConfig] = useState(EMPTY_STRUCTURAL);
  const [structuralResult, setStructuralResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [busyKey, setBusyKey] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [result, setResult] = useState(null);
  const activeRequest = useRef(null);
  const snapshotVersion = useRef(0);
  const refreshTimer = useRef(null);
  const selectedVisit = useMemo(() => visits.find((visit) => visit.id === visitId), [visits, visitId]);

  const loadSnapshot = useCallback(async (id, silent = false) => {
    if (!id) return;
    if (activeRequest.current?.visitId === id) return activeRequest.current.promise;
    const version = ++snapshotVersion.current;
    if (!silent) setBusy(true);
    const promise = Promise.all([
      api.listarItensVisita(id),
      api.obterResumo(id),
      api.listarEventos(id),
      api.obterResumoIntegracaoPatio(id),
      api.listarReservasPatio(id),
      api.listarOrdensPatio(id),
      api.listarFilasPatio(id),
      api.listarWorkQueuesPatio(id),
      api.listarOrdensSemCoberturaPatio(id),
      api.listarAlertasIntegracaoPatio(id),
      api.obterControlRoom(id)
    ]).then(([newItems, newSummary, newEvents, newIntegration, newReservations, newOrders, newQueues, newWorkQueues, newUncovered, newAlerts, newAdvanced]) => {
      if (version !== snapshotVersion.current) return;
      setItems(newItems);
      setSummary(newSummary);
      setEvents(newEvents);
      setIntegration(newIntegration);
      setReservations(newReservations);
      setOrders(newOrders);
      setQueues(newQueues);
      setWorkQueues(newWorkQueues);
      setUncovered(newUncovered);
      setAlerts(newAlerts);
      setAdvanced(newAdvanced);
      setLastUpdate(new Date());
      setPriorities((current) => [...newOrders, ...newWorkQueues.flatMap((queue) => queue.jobList ?? [])]
        .reduce((acc, order) => order.id ? { ...acc, [order.id]: current[order.id] ?? order.prioridadeOperacional ?? order.sequenciaNavio ?? 0 } : acc, {}));
      setEdits((current) => newWorkQueues.reduce((acc, queue) => ({
        ...acc,
        [queue.id]: current[queue.id] ?? {
          pow: queue.pow || '',
          poolOperacional: queue.poolOperacional || '',
          equipamento: queue.equipamento || '',
          limite: null
        }
      }), {}));
    }).finally(() => {
      if (activeRequest.current?.promise === promise) activeRequest.current = null;
      if (!silent && version === snapshotVersion.current) setBusy(false);
    });
    activeRequest.current = { visitId: id, promise };
    return promise;
  }, []);

  const scheduleRefresh = useCallback((id) => {
    if (!id) return;
    if (refreshTimer.current) clearTimeout(refreshTimer.current);
    refreshTimer.current = setTimeout(() => {
      loadSnapshot(id, true).catch((reason) => setError(formatError(reason)));
    }, 350);
  }, [loadSnapshot]);

  useEffect(() => {
    let active = true;
    Promise.all([api.listarNavios(), api.listarVisitas()]).then(([ships, newVisits]) => {
      if (!active) return;
      setNavios(ships);
      setVisits(newVisits);
      setVisitId(newVisits[0]?.id ?? null);
    }).catch((reason) => setError(formatError(reason, 'Não foi possível carregar os dados operacionais.')));
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (visitId) loadSnapshot(visitId).catch((reason) => setError(formatError(reason)));
  }, [visitId, loadSnapshot]);

  useEffect(() => {
    if (!visitId) return undefined;
    return api.assinarEventos(visitId, {
      onState: setStreamState,
      onEvent: () => scheduleRefresh(visitId),
      onError: (reason) => setError(formatError(reason, 'O stream operacional foi interrompido.'))
    });
  }, [visitId, scheduleRefresh]);

  useEffect(() => api.assinarTelemetriaEquipamentos({
    onState: setTelemetryStreamState,
    onEvent: () => scheduleRefresh(visitId),
    onError: (reason) => setError(formatError(reason, 'O stream de telemetria foi interrompido.'))
  }), [visitId, scheduleRefresh]);

  useEffect(() => () => {
    if (refreshTimer.current) clearTimeout(refreshTimer.current);
  }, []);

  async function action(key, operation, message) {
    setBusyKey(key);
    setError('');
    setSuccess('');
    try {
      const response = await operation();
      if (response !== undefined) setResult(response);
      setSuccess(message);
      return response;
    } catch (reason) {
      setError(formatError(reason));
      return undefined;
    } finally {
      setBusyKey('');
    }
  }

  const refresh = () => action('refresh', () => loadSnapshot(visitId), 'Control Room atualizado.');

  async function queueAction(queue, type) {
    const edit = edits[queue.id];
    const operations = {
      activate: () => api.ativarWorkQueuePatio(queue.id),
      deactivate: () => api.desativarWorkQueuePatio(queue.id),
      pow: () => api.atualizarPowWorkQueuePatio(queue.id, { pow: edit.pow || null, poolOperacional: edit.poolOperacional || null }),
      equipment: () => api.atualizarEquipamentoWorkQueuePatio(queue.id, { equipamento: edit.equipamento || null }),
      dispatch: () => api.despacharWorkQueuePatio(queue.id, { limiteOrdens: edit.limite || null, observacao: 'Dispatch acionado pelo Control Room React' })
    };
    await action(`${type}-${queue.id}`, operations[type], 'Work queue atualizada.');
    await loadSnapshot(visitId, true);
  }

  async function instructionAction(type, order) {
    await action(
      `${type}-${order.id}`,
      () => type === 'reset' ? api.resetarWorkInstructionPatio(order.id) : api.cancelarWorkInstructionPatio(order.id),
      type === 'reset' ? 'Work instruction resetada.' : 'Work instruction cancelada.'
    );
    await loadSnapshot(visitId, true);
  }

  function changePriority(order, value, persist) {
    setPriorities((current) => ({ ...current, [order.id]: value }));
    if (persist) {
      action(`priority-${order.id}`, () => api.atualizarPrioridadeOrdemPatio(visitId, order.id, value), 'Prioridade atualizada.')
        .then(() => loadSnapshot(visitId, true));
    }
  }

  async function validateStructural() {
    const response = await action('structural', () => api.validarRestricoesEstruturais(visitId, {
      limitePesoPorPoraoToneladas: valueOrNull(structuralConfig.limitePesoPorPoraoToneladas),
      limitePesoPorCamadaToneladas: valueOrNull(structuralConfig.limitePesoPorCamadaToneladas),
      desequilibrioBombordoBoresteMaximoPercentual: valueOrNull(structuralConfig.desequilibrioBombordoBoresteMaximoPercentual),
      alturaMaximaCamadas: valueOrNull(structuralConfig.alturaMaximaCamadas),
      exigirLashingAPartirCamada: valueOrNull(structuralConfig.exigirLashingAPartirCamada),
      posicoesComLashing: splitValues(structuralConfig.posicoesComLashing),
      poroesInterditados: splitValues(structuralConfig.poroesInterditados).map(Number).filter(Number.isFinite),
      regrasSegregacao: []
    }), 'Validação estrutural executada.');
    if (response) setStructuralResult(response);
  }

  const filteredOrders = orders.filter((order) =>
    (!statusFilter || order.statusOrdem === statusFilter)
    && (!zoneFilter || `${order.origem} ${order.destino} ${order.posicaoPlanejada}`.toUpperCase().includes(zoneFilter.toUpperCase()))
  );
  const filteredQueues = workQueues.filter((queue) =>
    (!statusFilter || queue.status === statusFilter)
    && (!zoneFilter || `${queue.identificador} ${queue.berco} ${queue.blocoZona} ${queue.pow} ${queue.poolOperacional}`.toUpperCase().includes(zoneFilter.toUpperCase()))
  );
  const filteredAlerts = alerts.filter((alert) => !severityFilter || alert.severidade === severityFilter);
  const imminent = orders.filter((order) => ['PENDENTE', 'EM_EXECUCAO'].includes(order.statusOrdem))
    .sort((a, b) => (a.sequenciaNavio ?? 999999) - (b.sequenciaNavio ?? 999999)).slice(0, 5);
  const nextPhase = PHASES[selectedVisit?.fase];

  return <div className="app">
    <header className="topbar">
      <div><span className="eyebrow">CloudPort</span><h1>Control Room Navio + Pátio</h1></div>
      <div className="stream-health"><span className={statusClass(streamState)}>Operação {streamState}</span><span className={statusClass(telemetryStreamState)}>VMT {telemetryStreamState}</span></div>
      <div className="top-actions"><span>{session.nome}</span><button className="secondary" onClick={refresh}>Atualizar</button><button className="danger" onClick={onLogout}>Sair</button></div>
    </header>
    <main className="content">
      {error && <div className="message error" role="alert">{error}</div>}
      {success && <div className="message success">{success}</div>}
      <section className="panel selector">
        <label>Visita<select value={visitId ?? ''} onChange={(event) => setVisitId(Number(event.target.value))}>{visits.map((visit) => <option key={visit.id} value={visit.id}>{visit.codigoVisita} · {visit.navioNome || navios.find((ship) => ship.id === visit.navioId)?.nome}</option>)}</select></label>
        <div><span className={statusClass(selectedVisit?.fase)}>{selectedVisit?.fase || 'SEM_VISITA'}</span><small>{selectedVisit?.bercoAtual || selectedVisit?.bercoPrevisto || 'sem berço'} · atualização {lastUpdate ? dateTime(lastUpdate) : 'pendente'}</small></div>
        {nextPhase && <button onClick={() => action('phase', async () => {
          const updated = await api.alterarFaseVisita(visitId, nextPhase);
          setVisits((current) => current.map((visit) => visit.id === updated.id ? updated : visit));
          await loadSnapshot(visitId, true);
        }, `Fase alterada para ${nextPhase}.`)}>Avançar para {nextPhase}</button>}
      </section>

      <section className="metrics">
        <Metric label="Progresso" value={`${formatNumber(summary.percentualProgresso, 1)}%`} detail={`${summary.totalItensOperados}/${summary.totalItensPlanejados} itens`} />
        <Metric label="Peso operado" value={`${formatNumber(summary.pesoOperado, 1)} t`} detail={`${formatNumber(summary.pesoPlanejado, 1)} t planejadas`} />
        <Metric label="Ordens" value={integration.itensComOrdem} detail={`${integration.ordensEmExecucao} em execução`} />
        <Metric label="Gargalos" value={advanced?.gargalos?.length ?? 0} detail={advanced?.quayMonitor?.riscoOperacional ?? integration.statusPredominante} />
      </section>

      <section className="panel">
        <div className="section-head"><div><span className="eyebrow">Operação</span><h2>Ações, relatórios e filtros</h2></div><small>Atualização por eventos versionados, sem polling periódico.</small></div>
        <div className="actions">
          <button onClick={() => action('reserve', async () => { await api.gerarReservasPatio(visitId); await loadSnapshot(visitId, true); }, 'Reservas de pátio geradas.')}>Gerar reservas</button>
          <button onClick={() => action('orders', async () => { await api.gerarOrdensPatio(visitId); await loadSnapshot(visitId, true); }, 'Ordens de pátio geradas.')}>Gerar ordens</button>
          <button className="secondary" onClick={() => action('sync', async () => { await api.sincronizarStatusPatio(visitId); await loadSnapshot(visitId, true); }, 'Status sincronizado.')}>Sincronizar Yard</button>
          <button className="warning" onClick={() => action('optimize', async () => { const response = await api.otimizarOperacaoGlobal(visitId); await loadSnapshot(visitId, true); return response; }, 'Plano global otimizado.')}>Otimizar Navio/Pátio/CHE</button>
          <button className="secondary" onClick={() => action('csv', () => api.baixarRelatorioCsv(visitId), 'Relatório CSV gerado.')}>Baixar CSV</button>
          <button className="secondary" onClick={() => action('pdf', () => api.baixarRelatorioPdf(visitId), 'Relatório PDF gerado.')}>Baixar PDF</button>
        </div>
        <div className="filters">
          <label>Status<select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}><option value="">Todos</option>{['PENDENTE', 'EM_EXECUCAO', 'BLOQUEADA', 'SUSPENSA', 'CONCLUIDA', 'CANCELADA'].map((status) => <option key={status}>{status}</option>)}</select></label>
          <label>Bloco/zona<input value={zoneFilter} onChange={(event) => setZoneFilter(event.target.value)} /></label>
          <label>Severidade<select value={severityFilter} onChange={(event) => setSeverityFilter(event.target.value)}><option value="">Todas</option>{['BAIXA', 'MEDIA', 'ALTA', 'CRITICA'].map((severity) => <option key={severity}>{severity}</option>)}</select></label>
        </div>
      </section>

      <section className="panel"><div className="section-head"><div><span className="eyebrow">Cais</span><h2>Quay Monitor</h2></div></div><QuayMonitor data={advanced?.quayMonitor} /></section>
      <div className="columns wide"><section className="panel"><div className="section-head"><h2>Vessel view</h2><span>{advanced?.vesselView?.length ?? 0}</span></div><VesselView rows={advanced?.vesselView} /></section><section className="panel"><div className="section-head"><h2>Yard view</h2><span>{advanced?.yardView?.length ?? 0}</span></div><YardView rows={advanced?.yardView} /></section></div>
      <section className="panel"><div className="section-head"><div><span className="eyebrow">Equipamentos</span><h2>CHE detail e VMT</h2></div><span>{advanced?.cheDetail?.length ?? 0}</span></div><CheDetail rows={advanced?.cheDetail} /></section>
      <div className="columns"><OperationalIssues title="Previsão de gargalos" rows={advanced?.gargalos} type="gargalo" /><OperationalIssues title="Comparação estiva, pátio e execução" rows={advanced?.divergencias} type="divergencia" /></div>

      <StructuralForm value={structuralConfig} onChange={setStructuralConfig} onValidate={validateStructural} result={structuralResult ?? advanced?.validacaoEstrutural} busy={busyKey === 'structural'} />

      <section className="panel"><div className="section-head"><div><span className="eyebrow">Execução</span><h2>Movimentos iminentes</h2></div><span>{imminent.length}</span></div><div className="imminent">{imminent.map((order) => <article key={order.id}><span>#{order.sequenciaNavio ?? '—'}</span><strong>{order.codigoLote}</strong><small>{order.origem || '—'} → {order.destino || order.posicaoPlanejada || '—'}</small><span className={statusClass(order.statusOrdem)}>{order.statusOrdem}</span></article>)}{!imminent.length && <p className="empty">Não existem movimentos iminentes.</p>}</div></section>
      <section className="panel"><div className="section-head"><div><span className="eyebrow">Equipment Control</span><h2>Work queues e job lists</h2></div><span>{filteredQueues.length}</span></div>{filteredQueues.map((queue) => <WorkQueue key={queue.id ?? queue.identificador} queue={queue} expanded={!!expanded[queue.id]} edit={edits[queue.id] ?? { pow: '', poolOperacional: '', equipamento: '', limite: null }} priorities={priorities} busyKey={busyKey} onToggle={() => setExpanded((current) => ({ ...current, [queue.id]: !current[queue.id] }))} onEdit={(edit) => setEdits((current) => ({ ...current, [queue.id]: edit }))} onAction={(type) => queueAction(queue, type)} onInstruction={instructionAction} />)}{!filteredQueues.length && <p className="empty">Nenhuma work queue encontrada.</p>}</section>
      <section className="panel"><div className="section-head"><div><span className="eyebrow">Yard</span><h2>Ordens de pátio</h2></div><span>{filteredOrders.length}/{orders.length}</span></div><OrdersTable orders={filteredOrders} priorities={priorities} onPriority={changePriority} onSuspend={(order) => action(`suspend-${order.id}`, async () => { await api.suspenderOrdemPatio(visitId, order.id); await loadSnapshot(visitId, true); }, 'Ordem suspensa.')} onResume={(order) => action(`resume-${order.id}`, async () => { await api.retomarOrdemPatio(visitId, order.id); await loadSnapshot(visitId, true); }, 'Ordem retomada.')} busyKey={busyKey} /></section>

      <div className="columns"><section className="panel"><div className="section-head"><h2>Alertas</h2><span>{filteredAlerts.length}</span></div><div className="list">{filteredAlerts.map((alert, index) => <article key={`${alert.tipo}-${index}`}><div><strong>{alert.tipo}</strong><span className={statusClass(alert.severidade)}>{alert.severidade}</span></div><p>{alert.mensagem}</p></article>)}{!filteredAlerts.length && <p className="empty">Nenhum alerta.</p>}</div></section><section className="panel"><div className="section-head"><h2>Eventos recentes</h2><span>{events.length}</span></div><div className="list scroll">{events.slice(0, 30).map((event) => <article key={event.id}><div><strong>{event.tipoEvento}</strong><time>{dateTime(event.criadoEm)}</time></div><p>{event.descricao}</p><small>{event.usuario}</small></article>)}</div></section></div>
      <div className="columns"><section className="panel"><div className="section-head"><h2>Ordens sem cobertura</h2><span>{uncovered.length}</span></div><OrdersTable orders={uncovered} priorities={priorities} onPriority={() => {}} onSuspend={() => {}} onResume={() => {}} busyKey={busyKey} readOnly /></section><section className="panel"><div className="section-head"><h2>Reservas</h2><span>{reservations.length}</span></div><div className="list scroll">{reservations.map((reservation) => <article key={reservation.id ?? `${reservation.itemOperacaoNavioId}-${reservation.posicaoPatioId}`}><div><strong>{reservation.posicaoPatioId}</strong><span className={statusClass(reservation.status)}>{reservation.status}</span></div><p>Item {reservation.itemOperacaoNavioId} · {reservation.bloco || 'sem bloco'} · {reservation.tipoReserva}</p></article>)}</div></section></div>
      {result && <section className="panel"><div className="section-head"><h2>Resultado da última operação</h2><button className="small secondary" onClick={() => setResult(null)}>Fechar</button></div><pre>{JSON.stringify(result, null, 2)}</pre></section>}
      <footer>{items.length} itens · {queues.length} filas agrupadas · {advanced?.divergencias?.length ?? summary.divergenciasPoraoPosicao} divergências</footer>
      {busy && <div className="loading">Carregando...</div>}
    </main>
  </div>;
}

export default function App() {
  const [session, setSession] = useState(() => readSession());
  const [ready, setReady] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    loadRuntimeConfig().then(() => setReady(true)).catch((reason) => setError(formatError(reason)));
  }, []);
  const authenticate = useCallback((newSession) => setSession(newSession), []);
  const logout = useCallback(() => {
    clearSession();
    setSession(null);
  }, []);
  if (error) return <main className="auth-shell"><div className="auth-card"><h1>Control Room indisponível</h1><div className="message error">{error}</div></div></main>;
  if (!ready) return <main className="auth-shell"><div className="auth-card"><h1>CloudPort</h1><p>Carregando configuração...</p></div></main>;
  return session && hasAnyRole(session, ...ALLOWED_ROLES)
    ? <ControlRoom session={session} onLogout={logout} />
    : <AuthGate onAuthenticated={authenticate} />;
}
