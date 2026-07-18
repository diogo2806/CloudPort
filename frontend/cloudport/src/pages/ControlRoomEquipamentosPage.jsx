import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { formatError, hasAnyRole } from '../api.js';
import { DataTable, EmptyState, JsonDetails, Loading, Message, MetricCard, PageHeader, Section, StatusBadge } from '../components.jsx';
import { controlRoomApi, subscribeControlRoom } from '../controlRoomApi.js';

const COMMAND_TYPES = [
  ['SINCRONIZAR_TELEMETRIA', 'Sincronizar telemetria'],
  ['ENVIAR_MENSAGEM', 'Enviar mensagem ao operador'],
  ['MOVER_PARA_POSICAO', 'Mover para posição'],
  ['INDISPONIBILIZAR', 'Indisponibilizar'],
  ['DISPONIBILIZAR', 'Disponibilizar'],
  ['RESETAR_POSICAO', 'Resetar posição ao vivo']
];

function displayValue(value) {
  if (value === undefined || value === null || value === '') return '—';
  if (typeof value === 'boolean') return value ? 'Sim' : 'Não';
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)) {
    const date = new Date(value);
    if (!Number.isNaN(date.getTime())) return date.toLocaleString('pt-BR');
  }
  return String(value);
}

function normalize(value) {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

function DetailGrid({ value }) {
  if (!value) return <EmptyState title="Selecione um equipamento" />;
  const fields = [
    ['Equipamento', value.identificador],
    ['Tipo', value.tipoEquipamento],
    ['Status', value.statusOperacional],
    ['Conectividade', value.conectividade],
    ['Dispositivo', value.dispositivo],
    ['Protocolo', value.protocolo],
    ['Firmware', value.firmware],
    ['Último heartbeat', value.ultimoHeartbeatEm],
    ['Posição', value.posicaoMaisProxima],
    ['Linha / coluna', `${displayValue(value.linha)} / ${displayValue(value.coluna)}`],
    ['Heading', value.heading],
    ['Status VMT', value.statusVmt],
    ['WI atual', value.workInstructionAtualId],
    ['Telemetria recebida', value.recebidoEm],
    ['Alarmes ativos', value.alarmesAtivos]
  ];
  return <dl className="detail-grid">{fields.map(([label, field]) => <div key={label}><dt>{label}</dt><dd>{displayValue(field)}</dd></div>)}</dl>;
}

export function ControlRoomEquipamentosPage({ session }) {
  const [data, setData] = useState({ resumo: null, equipamentos: [], alarmes: [], comandos: [], indisponibilidades: [], dispositivos: [] });
  const [history, setHistory] = useState([]);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [streamState, setStreamState] = useState('CONECTANDO');
  const [lastEvent, setLastEvent] = useState(null);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [type, setType] = useState('');
  const [connectivity, setConnectivity] = useState('');
  const [alarmStatus, setAlarmStatus] = useState('');
  const [alarmSeverity, setAlarmSeverity] = useState('');
  const [commandType, setCommandType] = useState('SINCRONIZAR_TELEMETRIA');
  const [commandMessage, setCommandMessage] = useState('');
  const [commandPosition, setCommandPosition] = useState('');
  const [downtimeReason, setDowntimeReason] = useState('');
  const [downtimeNote, setDowntimeNote] = useState('');
  const reloadTimer = useRef(null);
  const canOperate = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO');

  const load = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    setError('');
    try {
      const [resumo, equipamentos, alarmes, comandos, indisponibilidades, dispositivos] = await Promise.all([
        controlRoomApi.obterResumo(),
        controlRoomApi.listarEquipamentos(),
        controlRoomApi.listarAlarmes(),
        controlRoomApi.listarComandos(),
        controlRoomApi.listarIndisponibilidades(),
        controlRoomApi.listarDispositivos()
      ]);
      setData({
        resumo: resumo ?? null,
        equipamentos: equipamentos ?? [],
        alarmes: alarmes ?? [],
        comandos: comandos ?? [],
        indisponibilidades: indisponibilidades ?? [],
        dispositivos: dispositivos ?? []
      });
      setSelected((current) => {
        if (!current) return equipamentos?.[0] ?? null;
        return equipamentos?.find((item) => item.identificador === current.identificador) ?? equipamentos?.[0] ?? null;
      });
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      if (!silent) setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    const controller = new AbortController();
    subscribeControlRoom((event) => {
      setLastEvent(event.payload);
      clearTimeout(reloadTimer.current);
      reloadTimer.current = setTimeout(() => load(true), 250);
    }, setStreamState, controller.signal).catch((reason) => {
      if (!controller.signal.aborted) {
        setStreamState('ERRO');
        setError(formatError(reason, 'O canal em tempo real foi interrompido.'));
      }
    });
    return () => {
      controller.abort();
      clearTimeout(reloadTimer.current);
    };
  }, [load]);

  useEffect(() => {
    if (!selected?.identificador) {
      setHistory([]);
      return;
    }
    controlRoomApi.listarHistorico(selected.identificador, 100)
      .then((items) => setHistory(items ?? []))
      .catch((reason) => setError(formatError(reason)));
  }, [selected?.identificador, data.resumo?.atualizadoEm]);

  const equipmentRows = useMemo(() => data.equipamentos.filter((item) => {
    const text = normalize(`${item.identificador} ${item.tipoEquipamento} ${item.posicaoMaisProxima} ${item.dispositivo}`);
    return (!search || text.includes(normalize(search)))
      && (!status || item.statusOperacional === status)
      && (!type || item.tipoEquipamento === type)
      && (!connectivity || item.conectividade === connectivity);
  }), [data.equipamentos, search, status, type, connectivity]);

  const alarmRows = useMemo(() => data.alarmes.filter((item) =>
    (!alarmStatus || item.status === alarmStatus)
    && (!alarmSeverity || item.severidade === alarmSeverity)
  ), [data.alarmes, alarmStatus, alarmSeverity]);

  const selectedCommands = useMemo(() => data.comandos
    .filter((item) => !selected || item.equipamento === selected.identificador)
    .slice(0, 30), [data.comandos, selected]);
  const selectedDowntimes = useMemo(() => data.indisponibilidades
    .filter((item) => !selected || item.equipamento === selected.identificador)
    .slice(0, 30), [data.indisponibilidades, selected]);

  async function execute(operation, message) {
    if (busy) return;
    setBusy(true); setError(''); setSuccess('');
    try {
      await operation();
      setSuccess(message);
      await load(true);
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  function sendCommand(event) {
    event.preventDefault();
    if (!selected) return;
    const parametros = commandType === 'MOVER_PARA_POSICAO' && commandPosition.trim()
      ? { posicao: commandPosition.trim() }
      : {};
    execute(
      () => controlRoomApi.criarComando(selected.identificador, {
        tipo: commandType,
        mensagem: commandMessage,
        parametros
      }),
      `Comando criado para ${selected.identificador}.`
    ).then(() => { setCommandMessage(''); setCommandPosition(''); });
  }

  function startDowntime(event) {
    event.preventDefault();
    if (!selected || !downtimeReason.trim()) return;
    execute(
      () => controlRoomApi.iniciarIndisponibilidade(selected.identificador, { motivo: downtimeReason, observacao: downtimeNote }),
      `Indisponibilidade iniciada para ${selected.identificador}.`
    ).then(() => { setDowntimeReason(''); setDowntimeNote(''); });
  }

  const summary = data.resumo ?? {};
  const statuses = Array.from(new Set(data.equipamentos.map((item) => item.statusOperacional).filter(Boolean))).sort();
  const types = Array.from(new Set(data.equipamentos.map((item) => item.tipoEquipamento).filter(Boolean))).sort();
  const connectivities = Array.from(new Set(data.equipamentos.map((item) => item.conectividade).filter(Boolean))).sort();

  return <>
    <PageHeader
      eyebrow="Control Room"
      title="Equipamentos e telemetria"
      description="Monitoramento ao vivo, comandos remotos, alarmes, indisponibilidades e integração com dispositivos operacionais."
      actions={<div className="inline"><StatusBadge value={streamState} /><button className="secondary" onClick={() => load()} disabled={loading || busy}>Atualizar</button></div>}
    />
    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>
    {!canOperate && <Message type="warning">Seu perfil possui acesso de consulta. Comandos e alterações operacionais permanecem restritos.</Message>}

    <div className="metrics-grid">
      <MetricCard label="Equipamentos" value={summary.totalEquipamentos ?? '—'} />
      <MetricCard label="Operacionais" value={summary.operacionais ?? '—'} />
      <MetricCard label="Conectados" value={summary.conectados ?? '—'} />
      <MetricCard label="Telemetria atrasada" value={summary.telemetriaAtrasada ?? '—'} />
      <MetricCard label="Alarmes ativos" value={summary.alarmesAtivos ?? '—'} />
      <MetricCard label="Comandos pendentes" value={summary.comandosPendentes ?? '—'} />
      <MetricCard label="Indisponibilidades" value={summary.indisponibilidadesAbertas ?? '—'} />
    </div>

    <Section title="Visão operacional dos equipamentos" description="Selecione um equipamento para consultar sua telemetria e executar ações autorizadas.">
      <div className="filter-grid">
        <label className="field"><span>Busca</span><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Equipamento, tipo, posição ou dispositivo" /></label>
        <label className="field"><span>Status</span><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Todos</option>{statuses.map((item) => <option key={item}>{item}</option>)}</select></label>
        <label className="field"><span>Tipo</span><select value={type} onChange={(event) => setType(event.target.value)}><option value="">Todos</option>{types.map((item) => <option key={item}>{item}</option>)}</select></label>
        <label className="field"><span>Conectividade</span><select value={connectivity} onChange={(event) => setConnectivity(event.target.value)}><option value="">Todas</option>{connectivities.map((item) => <option key={item}>{item}</option>)}</select></label>
      </div>
      {loading ? <Loading label="Carregando Control Room..." /> : <DataTable
        rows={equipmentRows}
        rowKey={(row) => row.id}
        onRowClick={setSelected}
        columns={[
          { key: 'identificador', label: 'Equipamento' },
          { key: 'tipoEquipamento', label: 'Tipo' },
          { key: 'statusOperacional', label: 'Status', render: (row) => <StatusBadge value={row.statusOperacional} /> },
          { key: 'conectividade', label: 'Conectividade', render: (row) => <StatusBadge value={row.conectividade} /> },
          { key: 'posicaoMaisProxima', label: 'Posição', render: (row) => displayValue(row.posicaoMaisProxima) },
          { key: 'statusVmt', label: 'VMT', render: (row) => displayValue(row.statusVmt) },
          { key: 'workInstructionAtualId', label: 'WI atual', render: (row) => displayValue(row.workInstructionAtualId) },
          { key: 'recebidoEm', label: 'Última leitura', render: (row) => displayValue(row.recebidoEm) },
          { key: 'alarmesAtivos', label: 'Alarmes' }
        ]}
        emptyTitle="Nenhum equipamento encontrado"
      />}
    </Section>

    <div className="split-grid">
      <Section title="Detalhes do equipamento"><DetailGrid value={selected} /><JsonDetails value={selected} title="Estado técnico atual" /></Section>
      <Section title="Comando remoto" description="O comando é persistido e entregue ao dispositivo no próximo polling.">
        {!selected ? <EmptyState title="Selecione um equipamento" /> : !canOperate ? <EmptyState title="Perfil somente consulta" /> : <form className="form-grid" onSubmit={sendCommand}>
          <label className="field"><span>Comando</span><select value={commandType} onChange={(event) => setCommandType(event.target.value)}>{COMMAND_TYPES.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label>
          {commandType === 'MOVER_PARA_POSICAO' && <label className="field"><span>Posição de destino</span><input value={commandPosition} onChange={(event) => setCommandPosition(event.target.value)} required /></label>}
          <label className="field"><span>Mensagem ao dispositivo/operador</span><textarea value={commandMessage} onChange={(event) => setCommandMessage(event.target.value)} maxLength={500} /></label>
          <button disabled={busy}>Enviar comando</button>
        </form>}
      </Section>
    </div>

    <div className="split-grid">
      <Section title="Histórico de telemetria">
        <DataTable rows={history} rowKey={(row) => row.id} columns={[
          { key: 'capturadoEm', label: 'Capturado em', render: (row) => displayValue(row.capturadoEm) },
          { key: 'posicaoMaisProxima', label: 'Posição' },
          { key: 'latitude', label: 'Latitude', render: (row) => displayValue(row.latitude) },
          { key: 'longitude', label: 'Longitude', render: (row) => displayValue(row.longitude) },
          { key: 'heading', label: 'Heading', render: (row) => displayValue(row.heading) },
          { key: 'statusVmt', label: 'Status VMT', render: (row) => <StatusBadge value={row.statusVmt} /> },
          { key: 'sequencia', label: 'Sequência' }
        ]} emptyTitle="Sem histórico para o equipamento" />
      </Section>
      <Section title="Indisponibilidade operacional">
        {selected && canOperate && <form className="form-grid" onSubmit={startDowntime}>
          <label className="field"><span>Motivo</span><input value={downtimeReason} onChange={(event) => setDowntimeReason(event.target.value)} maxLength={120} required /></label>
          <label className="field"><span>Observação</span><textarea value={downtimeNote} onChange={(event) => setDowntimeNote(event.target.value)} maxLength={1000} /></label>
          <button className="warning" disabled={busy || Boolean(selected.indisponibilidadeAbertaId)}>Iniciar indisponibilidade</button>
        </form>}
        <DataTable rows={selectedDowntimes} rowKey={(row) => row.id} columns={[
          { key: 'motivo', label: 'Motivo' },
          { key: 'inicioEm', label: 'Início', render: (row) => displayValue(row.inicioEm) },
          { key: 'fimEm', label: 'Fim', render: (row) => displayValue(row.fimEm) },
          { key: 'abertoPor', label: 'Aberto por' },
          { key: 'action', label: 'Ação', render: (row) => !row.fimEm && canOperate ? <button className="small" disabled={busy} onClick={() => execute(() => controlRoomApi.encerrarIndisponibilidade(row.id), 'Indisponibilidade encerrada.')}>Encerrar</button> : '—' }
        ]} emptyTitle="Sem indisponibilidades registradas" />
      </Section>
    </div>

    <Section title="Alarmes de equipamentos" description="A central global continua consolidando alertas de todos os domínios; esta grade detalha as ocorrências técnicas dos equipamentos.">
      <div className="filter-grid">
        <label className="field"><span>Status</span><select value={alarmStatus} onChange={(event) => setAlarmStatus(event.target.value)}><option value="">Todos</option><option>ATIVO</option><option>RECONHECIDO</option><option>RESOLVIDO</option></select></label>
        <label className="field"><span>Severidade</span><select value={alarmSeverity} onChange={(event) => setAlarmSeverity(event.target.value)}><option value="">Todas</option><option>CRITICA</option><option>ALTA</option><option>MEDIA</option><option>BAIXA</option></select></label>
      </div>
      <DataTable rows={alarmRows} rowKey={(row) => row.id} columns={[
        { key: 'equipamento', label: 'Equipamento' },
        { key: 'tipo', label: 'Tipo' },
        { key: 'severidade', label: 'Severidade', render: (row) => <StatusBadge value={row.severidade} /> },
        { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'mensagem', label: 'Mensagem' },
        { key: 'abertoEm', label: 'Aberto em', render: (row) => displayValue(row.abertoEm) },
        { key: 'action', label: 'Ações', render: (row) => canOperate && row.status !== 'RESOLVIDO' ? <div className="actions">{row.status === 'ATIVO' && <button className="small" disabled={busy} onClick={() => execute(() => controlRoomApi.reconhecerAlarme(row.id), 'Alarme reconhecido.')}>Reconhecer</button>}<button className="small" disabled={busy} onClick={() => execute(() => controlRoomApi.resolverAlarme(row.id), 'Alarme resolvido.')}>Resolver</button></div> : '—' }
      ]} emptyTitle="Nenhum alarme encontrado" />
    </Section>

    <div className="split-grid">
      <Section title="Comandos recentes"><DataTable rows={selectedCommands} rowKey={(row) => row.id} columns={[
        { key: 'tipo', label: 'Comando' },
        { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'dispositivo', label: 'Dispositivo', render: (row) => displayValue(row.dispositivo) },
        { key: 'solicitadoPor', label: 'Solicitado por' },
        { key: 'criadoEm', label: 'Criado em', render: (row) => displayValue(row.criadoEm) },
        { key: 'retornoDispositivo', label: 'Retorno', render: (row) => displayValue(row.retornoDispositivo) }
      ]} emptyTitle="Sem comandos registrados" /></Section>
      <Section title="Dispositivos integrados"><DataTable rows={data.dispositivos} rowKey={(row) => row.id} columns={[
        { key: 'identificador', label: 'Dispositivo' },
        { key: 'equipamento', label: 'Equipamento' },
        { key: 'protocolo', label: 'Protocolo' },
        { key: 'statusIntegracao', label: 'Integração', render: (row) => <StatusBadge value={row.statusIntegracao} /> },
        { key: 'firmware', label: 'Firmware', render: (row) => displayValue(row.firmware) },
        { key: 'ultimoHeartbeatEm', label: 'Heartbeat', render: (row) => displayValue(row.ultimoHeartbeatEm) }
      ]} emptyTitle="Nenhum dispositivo integrado" /></Section>
    </div>

    <JsonDetails value={lastEvent} title="Último evento do canal em tempo real" />
  </>;
}
