import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, formatError, readSession } from './api.js';
import {
  buildOperatorTasks,
  createOperatorCommand,
  enqueueOperatorCommand,
  operatorQueueStorageKey,
  permittedOperatorSources,
  readOperatorQueue,
  removeCompletedCommands,
  updateOperatorCommand,
  validateOperatorScan,
  writeOperatorQueue
} from './operatorMode.js';
import './operator-mode.css';

const SOURCE_LOADERS = {
  gate: () => api.obterCentralGate(),
  yard: () => api.listarOrdensPatio(),
  rail: () => api.listarVisitasFerrovia(30),
  inventory: () => api.listarEquipamentosInventario()
};

const FULL_MODE_ROUTES = {
  gate: '/home/gate/dashboard',
  yard: '/home/patio/lista-trabalho',
  rail: '/home/ferrovia/lista-trabalho',
  inventory: '/home/patio/inventario'
};

function stateLabel(status) {
  return String(status ?? 'SEM_STATUS').replaceAll('_', ' ');
}

function formatDateTime(value) {
  if (!value) return 'Não informado';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('pt-BR');
}

function expectedTypes(task) {
  return task?.entityType && task.entityType !== 'TAREFA' ? [task.entityType] : [];
}

function normalizeReference(value) {
  return String(value ?? '').toUpperCase().replace(/[^A-Z0-9./_-]/g, '');
}

function sourceTitle(source) {
  return { gate: 'Gate', yard: 'Pátio', rail: 'Ferrovia', inventory: 'Inventário' }[source] ?? source;
}

function commandStateLabel(status) {
  return {
    PENDENTE: 'Pendente',
    AGUARDANDO_RECONEXAO: 'Aguardando conexão',
    ENVIANDO: 'Sincronizando',
    CONCLUIDA: 'Concluída',
    FALHA: 'Falha de sincronização',
    CONFLITO: 'Conflito para resolver',
    DESCARTADA: 'Descartada'
  }[status] ?? status;
}

function playFeedback(success, enabled) {
  if (!enabled || !globalThis.AudioContext) return;
  try {
    const context = new AudioContext();
    const oscillator = context.createOscillator();
    const gain = context.createGain();
    oscillator.frequency.value = success ? 880 : 220;
    gain.gain.value = 0.04;
    oscillator.connect(gain);
    gain.connect(context.destination);
    oscillator.start();
    oscillator.stop(context.currentTime + (success ? 0.08 : 0.16));
    oscillator.addEventListener('ended', () => context.close());
  } catch {
    // Feedback sonoro é opcional; falha de áudio não afeta a operação.
  }
}

function OperatorManual({ onClose }) {
  return <section className="operator-manual" role="dialog" aria-modal="true" aria-labelledby="operator-manual-title">
    <header><div><span>Manual contextual</span><h2 id="operator-manual-title">Modo operador PDA/coletor</h2></div><button type="button" className="operator-icon-button" aria-label="Fechar manual" onClick={onClose}>×</button></header>
    <div className="operator-manual-content">
      <section><h3>Finalidade</h3><p>Executar tarefas curtas de Gate, Pátio, Ferrovia e Inventário em celular, tablet industrial ou coletor, sem navegar pelo menu administrativo completo.</p></section>
      <section><h3>Fluxo operacional</h3><ol><li>Confira conexão, sincronização e tarefa atual.</li><li>Leia a unidade, placa ou posição pelo scanner físico, câmera ou entrada manual.</li><li>Corrija a leitura quando o motivo de rejeição for exibido.</li><li>Revise origem, destino, equipamento, ação e identificador.</li><li>Confirme uma única vez. O cliente bloqueia repetição pela chave idempotente.</li><li>Em conexão instável, acompanhe a fila e sincronize após a retomada.</li></ol></section>
      <section><h3>Campos</h3><ul><li><b>Tarefa atual:</b> item priorizado por bloqueio, execução e prazo.</li><li><b>Próxima tarefa:</b> próximo item da fila autorizada.</li><li><b>Leitura:</b> valor recebido por scanner, câmera ou digitação.</li><li><b>Tipo:</b> contêiner, placa, posição ou tarefa.</li><li><b>Origem/destino:</b> locais da execução.</li><li><b>Equipamento:</b> CHE, vagão, pista ou recurso associado.</li><li><b>Fila de sincronização:</b> comandos pendentes, em falha ou conflito.</li></ul></section>
      <section><h3>Permissões</h3><ul><li>Gate: ADMIN_PORTO, OPERADOR_GATE ou PLANEJADOR.</li><li>Pátio e Ferrovia: ADMIN_PORTO, OPERADOR_PATIO ou PLANEJADOR.</li><li>Inventário: ADMIN_PORTO, OPERADOR_PATIO, PLANEJADOR ou OPERADOR_GATE.</li><li>O backend valida cada comando; o modo compacto não amplia permissões.</li></ul></section>
      <section><h3>Estados</h3><ul><li>Online ou offline.</li><li>Carregando, pronto, sem tarefa ou fonte indisponível.</li><li>Leitura válida ou inválida.</li><li>Comando pendente, aguardando conexão, sincronizando, concluído, falha ou conflito.</li></ul></section>
      <section><h3>Motivos de bloqueio</h3><ul><li>Formato inválido ou dígito ISO 6346 incorreto.</li><li>Objeto lido diferente da tarefa atual.</li><li>Ação sem permissão ou estado incompatível.</li><li>Conexão necessária para validação online.</li><li>Comando duplicado já presente na fila.</li><li>Registro alterado por outro usuário, gerando conflito.</li><li>Tarefa disponível somente no modo completo.</li></ul></section>
      <section><h3>Exemplo</h3><p>Na ordem de pátio da unidade MSCU6639871, leia o código, confirme origem, destino e CHE, abra o resumo e selecione Confirmar. Um segundo toque gera a mesma chave e não cria outro comando.</p></section>
      <section><h3>Atalhos</h3><ul><li>Scanner físico: digite no campo de leitura e envie Enter.</li><li>Enter: validar a leitura manual.</li><li>Esc: fechar câmera, manual ou modo operador quando não houver confirmação aberta.</li><li>Tab e Shift + Tab: percorrer ações.</li><li>Botões amplos permitem operação por toque.</li></ul></section>
      <section><h3>Processo completo</h3><p><a href="https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/modo-operador-pda.md" target="_blank" rel="noreferrer">Abrir documentação completa do modo operador</a>.</p></section>
    </div>
  </section>;
}

function ConnectivityBar({ online, lastSync, queue, loading, onSync }) {
  const pending = queue.filter((item) => !['CONCLUIDA', 'DESCARTADA'].includes(item.status)).length;
  const conflicts = queue.filter((item) => item.status === 'CONFLITO').length;
  return <section className="operator-connectivity" aria-label="Conectividade e sincronização">
    <div><span className={`operator-connection-dot ${online ? 'online' : 'offline'}`} aria-hidden="true" /><strong>{online ? 'Online' : 'Offline'}</strong></div>
    <div><span>Última sincronização</span><strong>{formatDateTime(lastSync)}</strong></div>
    <div><span>Operações pendentes</span><strong>{pending}{conflicts ? ` · ${conflicts} conflito(s)` : ''}</strong></div>
    <button type="button" disabled={!online || loading || pending === 0} onClick={onSync}>{loading ? 'Sincronizando...' : 'Sincronizar fila'}</button>
  </section>;
}

function TaskSummary({ task, nextTask }) {
  if (!task) return <section className="operator-empty"><strong>Nenhuma tarefa disponível</strong><p>Atualize as fontes ou confirme se o perfil possui tarefas operacionais abertas.</p></section>;
  return <section className="operator-task-card" aria-labelledby="operator-current-task">
    <div className="operator-task-priority"><span>Prioridade {task.priority + 1}</span><strong>{sourceTitle(task.source)}</strong></div>
    <h2 id="operator-current-task">{task.title}</h2>
    <div className="operator-task-identifiers"><span>Estado <b>{stateLabel(task.status)}</b></span><span>Referência <b>{task.reference || 'não informada'}</b></span></div>
    <dl>
      <div><dt>Origem</dt><dd>{task.origin}</dd></div>
      <div><dt>Destino</dt><dd>{task.destination}</dd></div>
      <div><dt>Equipamento</dt><dd>{task.equipment || 'Não informado'}</dd></div>
      <div><dt>Prazo</dt><dd>{formatDateTime(task.deadline)}</dd></div>
    </dl>
    <div className="operator-next-task"><span>Próxima tarefa</span><strong>{nextTask?.title ?? 'Nenhuma tarefa na fila'}</strong></div>
  </section>;
}

function ScanPanel({ task, scan, input, onInput, onSubmit, onStartCamera, cameraSupported, cameraActive, onStopCamera, videoRef }) {
  return <section className="operator-scan-panel" aria-labelledby="operator-scan-title">
    <header><div><span>Etapa 1 de 2</span><h2 id="operator-scan-title">Ler identificação</h2></div>{task?.entityType && <strong>Esperado: {task.entityType}</strong>}</header>
    <form onSubmit={onSubmit}>
      <label htmlFor="operator-scan-input">Scanner físico ou entrada manual</label>
      <div className="operator-scan-input"><input id="operator-scan-input" autoComplete="off" inputMode="text" value={input} onChange={(event) => onInput(event.target.value)} placeholder="Leia ou digite o código" /><button type="submit">Validar</button></div>
    </form>
    <div className="operator-scan-actions">
      <button type="button" className="operator-secondary" disabled={!cameraSupported || cameraActive} onClick={onStartCamera}>Usar câmera</button>
      {cameraActive && <button type="button" className="operator-secondary" onClick={onStopCamera}>Fechar câmera</button>}
    </div>
    {!cameraSupported && <p className="operator-hint">Câmera de código não disponível neste navegador. Scanner físico e digitação continuam ativos.</p>}
    {cameraActive && <div className="operator-camera"><video ref={videoRef} muted playsInline aria-label="Imagem da câmera para leitura de código" /><span>Aponte a câmera para o código de barras ou QR.</span></div>}
    {scan && <div className={`operator-scan-result ${scan.valid ? 'valid' : 'invalid'}`} role={scan.valid ? 'status' : 'alert'}>
      <strong>{scan.valid ? `Leitura válida · ${scan.type}` : scan.reason}</strong>
      <span>{scan.valid ? scan.value : scan.correction}</span>
    </div>}
  </section>;
}

function ConfirmationPanel({ task, scan, busy, online, onConfirm, onOpenFull, onBlocker }) {
  if (!task) return null;
  if (!task.action) return <section className="operator-confirm-panel">
    <header><div><span>Etapa 2 de 2</span><h2>Ação no modo completo</h2></div></header>
    <p>Esta tarefa exige campos ou validações adicionais. A leitura permanece disponível para conferência, mas a alteração deve ser concluída na tela operacional completa.</p>
    <button type="button" onClick={() => onOpenFull(task)}>Abrir {sourceTitle(task.source)} no modo completo</button>
  </section>;
  return <section className="operator-confirm-panel" aria-labelledby="operator-confirm-title">
    <header><div><span>Etapa 2 de 2</span><h2 id="operator-confirm-title">Revisar e confirmar</h2></div><strong>{online ? 'Validação online' : 'Será colocado na fila'}</strong></header>
    <dl>
      <div><dt>Ação</dt><dd>{task.actionLabel}</dd></div>
      <div><dt>Objeto lido</dt><dd>{scan?.valid ? `${scan.type} ${scan.value}` : 'Leitura válida obrigatória'}</dd></div>
      <div><dt>Origem</dt><dd>{task.origin}</dd></div>
      <div><dt>Destino</dt><dd>{task.destination}</dd></div>
      <div><dt>Equipamento</dt><dd>{task.equipment}</dd></div>
    </dl>
    <div className="operator-critical-warning"><strong>Operação crítica</strong><span>Confira o objeto físico, a origem e o destino antes de confirmar.</span></div>
    <div className="operator-confirm-actions">
      <button type="button" className="operator-secondary" disabled={busy} onClick={() => onBlocker(task)}>Informar impedimento</button>
      <button type="button" disabled={!scan?.valid || busy} onClick={() => onConfirm(task, scan)}>{busy ? 'Processando...' : online ? `Confirmar ${task.actionLabel}` : 'Adicionar à fila offline'}</button>
    </div>
  </section>;
}

function QueuePanel({ queue, onRetry, onDiscard }) {
  const visible = queue.filter((item) => !['CONCLUIDA', 'DESCARTADA'].includes(item.status));
  if (!visible.length) return null;
  return <section className="operator-queue" aria-labelledby="operator-queue-title">
    <header><h2 id="operator-queue-title">Fila de sincronização</h2><span>{visible.length} operação(ões)</span></header>
    <div>{visible.map((command) => <article key={command.id}>
      <div><strong>{command.action.replaceAll('_', ' ')}</strong><span>{command.scan?.value ?? command.taskId}</span></div>
      <div><strong>{commandStateLabel(command.status)}</strong><span>{command.error || `Tentativas: ${command.attempts}`}</span></div>
      {['FALHA', 'CONFLITO'].includes(command.status) && <div className="operator-queue-actions"><button type="button" className="operator-secondary" onClick={() => onRetry(command.id)}>Repetir</button><button type="button" className="operator-danger" onClick={() => onDiscard(command.id)}>Descartar</button></div>}
    </article>)}</div>
  </section>;
}

export function OperatorModeLauncher() {
  const [open, setOpen] = useState(false);
  return <>
    <button type="button" className="operator-mode-trigger secondary" aria-label="Abrir modo operador para celular, tablet ou PDA" aria-haspopup="dialog" aria-expanded={open} onClick={() => setOpen(true)}><span aria-hidden="true">▣</span><strong>Operador</strong></button>
    {open && <OperatorWorkspace onClose={() => setOpen(false)} />}
  </>;
}

function OperatorWorkspace({ onClose }) {
  const [session] = useState(() => readSession() ?? {});
  const sources = useMemo(() => permittedOperatorSources(session), [session]);
  const storage = globalThis.localStorage;
  const queueKey = useMemo(() => operatorQueueStorageKey(session), [session]);
  const [online, setOnline] = useState(() => globalThis.navigator?.onLine !== false);
  const [results, setResults] = useState({});
  const [tasks, setTasks] = useState([]);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [lastSync, setLastSync] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [input, setInput] = useState('');
  const [scan, setScan] = useState(null);
  const [queue, setQueue] = useState(() => readOperatorQueue(storage, queueKey));
  const [manualOpen, setManualOpen] = useState(false);
  const [highContrast, setHighContrast] = useState(() => storage?.getItem('cloudport:operator-mode:high-contrast') === 'true');
  const [soundEnabled, setSoundEnabled] = useState(() => storage?.getItem('cloudport:operator-mode:sound') !== 'false');
  const [cameraActive, setCameraActive] = useState(false);
  const [busyCommand, setBusyCommand] = useState(false);
  const [blockerNote, setBlockerNote] = useState('');
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const detectionFrameRef = useRef(null);
  const workspaceRef = useRef(null);
  const currentTask = tasks[selectedIndex] ?? null;
  const nextTask = tasks[selectedIndex + 1] ?? null;
  const cameraSupported = Boolean(globalThis.BarcodeDetector && globalThis.navigator?.mediaDevices?.getUserMedia);

  const persistQueue = useCallback((nextQueue) => {
    setQueue(nextQueue);
    writeOperatorQueue(storage, queueKey, nextQueue);
  }, [queueKey, storage]);

  const stopCamera = useCallback(() => {
    if (detectionFrameRef.current) globalThis.cancelAnimationFrame(detectionFrameRef.current);
    detectionFrameRef.current = null;
    streamRef.current?.getTracks?.().forEach((track) => track.stop());
    streamRef.current = null;
    if (videoRef.current) videoRef.current.srcObject = null;
    setCameraActive(false);
  }, []);

  const validateRead = useCallback((value) => {
    const result = validateOperatorScan(value, expectedTypes(currentTask));
    if (result.valid && currentTask?.reference && ['CONTAINER', 'PLACA'].includes(result.type)
      && normalizeReference(currentTask.reference) !== normalizeReference(result.value)) {
      const mismatch = {
        ...result,
        valid: false,
        reason: `A leitura ${result.value} não corresponde à referência ${currentTask.reference} da tarefa atual.`,
        correction: 'Confira a tarefa selecionada e leia novamente o objeto físico correto.'
      };
      setScan(mismatch);
      playFeedback(false, soundEnabled);
      return mismatch;
    }
    setScan(result);
    setInput(result.value || value);
    playFeedback(result.valid, soundEnabled);
    return result;
  }, [currentTask, soundEnabled]);

  const loadTasks = useCallback(async () => {
    setLoading(true);
    setError('');
    const sourceKeys = sources.map((source) => source.key);
    const settled = await Promise.allSettled(sourceKeys.map((source) => SOURCE_LOADERS[source]()));
    const nextResults = Object.fromEntries(sourceKeys.map((source, index) => [source, settled[index].status === 'fulfilled'
      ? { status: 'fulfilled', payload: settled[index].value }
      : { status: 'rejected', error: settled[index].reason }]));
    const nextTasks = buildOperatorTasks(nextResults);
    setResults(nextResults);
    setTasks(nextTasks);
    setSelectedIndex((current) => Math.min(current, Math.max(nextTasks.length - 1, 0)));
    setLastSync(new Date().toISOString());
    setLoading(false);
    const failed = settled.filter((result) => result.status === 'rejected').length;
    if (failed === settled.length && settled.length) setError('Nenhuma fonte operacional pôde ser atualizada.');
    else if (failed) setMessage(`${failed} fonte(s) indisponível(is); as demais tarefas permanecem utilizáveis.`);
  }, [sources]);

  useEffect(() => { loadTasks(); }, [loadTasks]);

  useEffect(() => {
    function handleOnline() { setOnline(true); setMessage('Conexão restabelecida. Revise e sincronize a fila pendente.'); }
    function handleOffline() { setOnline(false); setMessage('Conexão perdida. Novos comandos compatíveis permanecerão na fila local.'); }
    function handleKeyDown(event) {
      if (event.key !== 'Escape') return;
      if (cameraActive) { stopCamera(); return; }
      if (manualOpen) { setManualOpen(false); return; }
      onClose();
    }
    globalThis.addEventListener?.('online', handleOnline);
    globalThis.addEventListener?.('offline', handleOffline);
    globalThis.addEventListener?.('keydown', handleKeyDown);
    workspaceRef.current?.focus();
    return () => {
      globalThis.removeEventListener?.('online', handleOnline);
      globalThis.removeEventListener?.('offline', handleOffline);
      globalThis.removeEventListener?.('keydown', handleKeyDown);
      stopCamera();
    };
  }, [cameraActive, manualOpen, onClose, stopCamera]);

  useEffect(() => {
    setScan(null);
    setInput('');
    setBlockerNote('');
  }, [currentTask?.id]);

  async function startCamera() {
    if (!cameraSupported) return;
    setError('');
    try {
      const detector = new BarcodeDetector();
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } }, audio: false });
      streamRef.current = stream;
      setCameraActive(true);
      await new Promise((resolve) => globalThis.setTimeout(resolve, 0));
      if (!videoRef.current) throw new Error('Visualização da câmera não foi inicializada.');
      videoRef.current.srcObject = stream;
      await videoRef.current.play();
      const detect = async () => {
        if (!videoRef.current || !streamRef.current) return;
        try {
          const codes = await detector.detect(videoRef.current);
          if (codes[0]?.rawValue) {
            validateRead(codes[0].rawValue);
            stopCamera();
            return;
          }
        } catch {
          // Quadros sem leitura ou em movimento são esperados.
        }
        detectionFrameRef.current = globalThis.requestAnimationFrame(detect);
      };
      detectionFrameRef.current = globalThis.requestAnimationFrame(detect);
    } catch (reason) {
      stopCamera();
      setError(formatError(reason, 'Não foi possível abrir a câmera. Use o scanner físico ou a entrada manual.'));
    }
  }

  async function executeCommand(command, queueSnapshot = queue) {
    let nextQueue = updateOperatorCommand(queueSnapshot, command.id, { status: 'ENVIANDO', attempts: Number(command.attempts ?? 0) + 1, error: null });
    persistQueue(nextQueue);
    try {
      if (command.action === 'START_YARD_ORDER') await api.iniciarOrdemPatio(command.sourceId);
      else if (command.action === 'COMPLETE_YARD_ORDER') await api.concluirOrdemPatio(command.sourceId);
      else if (command.action === 'START_RAIL_ORDER') await api.iniciarOrdemFerrovia(command.visitId, command.sourceId);
      else if (command.action === 'COMPLETE_RAIL_ORDER') await api.concluirOrdemFerrovia(command.visitId, command.sourceId);
      else throw new Error('Ação não disponível no modo operador.');
      nextQueue = updateOperatorCommand(nextQueue, command.id, { status: 'CONCLUIDA', error: null });
      persistQueue(removeCompletedCommands(nextQueue));
      setMessage('Operação confirmada e sincronizada.');
      playFeedback(true, soundEnabled);
      await loadTasks();
      return true;
    } catch (reason) {
      const formatted = formatError(reason, 'Não foi possível sincronizar a operação.');
      const conflict = Number(reason?.status) === 409 || /conflito|estado|vers[aã]o|duplic/i.test(formatted);
      nextQueue = updateOperatorCommand(nextQueue, command.id, { status: conflict ? 'CONFLITO' : 'FALHA', error: formatted });
      persistQueue(nextQueue);
      setError(formatted);
      playFeedback(false, soundEnabled);
      return false;
    }
  }

  async function confirmTask(task, validScan) {
    if (busyCommand || !validScan?.valid) return;
    setBusyCommand(true);
    setError('');
    setMessage('');
    const command = createOperatorCommand(task, task.action, validScan, session);
    const queued = enqueueOperatorCommand(queue, command);
    if (!queued.inserted) {
      setMessage('Este comando já está pendente ou em sincronização. Nenhuma duplicidade foi criada.');
      setBusyCommand(false);
      return;
    }
    const initialQueue = online ? queued.queue : updateOperatorCommand(queued.queue, command.id, { status: 'AGUARDANDO_RECONEXAO' });
    persistQueue(initialQueue);
    if (!online) {
      setMessage('Operação adicionada à fila local. Ela será enviada somente após a reconexão e nova validação do backend.');
      setBusyCommand(false);
      setSelectedIndex((current) => Math.min(current + 1, Math.max(tasks.length - 1, 0)));
      return;
    }
    const commandToExecute = initialQueue.find((item) => item.id === command.id) ?? command;
    const succeeded = await executeCommand(commandToExecute, initialQueue);
    if (succeeded) setSelectedIndex((current) => Math.min(current, Math.max(tasks.length - 2, 0)));
    setBusyCommand(false);
  }

  async function syncQueue() {
    if (!online || syncing) return;
    setSyncing(true);
    setError('');
    let snapshot = [...queue];
    const pending = snapshot.filter((item) => ['PENDENTE', 'AGUARDANDO_RECONEXAO', 'FALHA'].includes(item.status));
    for (const command of pending) {
      const current = snapshot.find((item) => item.id === command.id) ?? command;
      await executeCommand(current, snapshot);
      snapshot = readOperatorQueue(storage, queueKey);
    }
    setSyncing(false);
  }

  function retryCommand(id) {
    const next = updateOperatorCommand(queue, id, { status: online ? 'PENDENTE' : 'AGUARDANDO_RECONEXAO', error: null });
    persistQueue(next);
    setMessage(online ? 'Comando liberado para nova tentativa.' : 'Comando aguardará a reconexão.');
  }

  function discardCommand(id) {
    const reason = globalThis.prompt?.('Informe o motivo para descartar o comando conflitante:') ?? '';
    if (!reason.trim()) return;
    const next = removeCompletedCommands(updateOperatorCommand(queue, id, { status: 'DESCARTADA', error: `Descartado: ${reason.trim()}` }));
    persistQueue(next);
    setMessage('Comando descartado localmente com motivo. Confira o estado físico no modo completo.');
  }

  function openFullMode(task) {
    const route = FULL_MODE_ROUTES[task.source] ?? '/home/dashboard';
    const context = { taskId: task.id, source: task.source, reference: task.reference, createdAt: new Date().toISOString() };
    try { globalThis.sessionStorage?.setItem('cloudport:operator-mode:context', JSON.stringify(context)); } catch { /* contexto é opcional */ }
    onClose();
    globalThis.history?.pushState({}, '', route);
    globalThis.dispatchEvent?.(new PopStateEvent('popstate'));
    globalThis.scrollTo?.({ top: 0, behavior: 'smooth' });
  }

  function reportBlocker(task) {
    const note = globalThis.prompt?.('Descreva o impedimento encontrado:') ?? '';
    if (!note.trim()) return;
    setBlockerNote(note.trim());
    try {
      globalThis.sessionStorage?.setItem('cloudport:operator-mode:blocker', JSON.stringify({ taskId: task.id, source: task.source, reference: task.reference, note: note.trim(), createdAt: new Date().toISOString() }));
    } catch { /* registro temporário é opcional */ }
    setMessage('Impedimento preparado. Abra o modo completo para concluir o registro auditável.');
  }

  function toggleContrast() {
    setHighContrast((current) => {
      const next = !current;
      storage?.setItem('cloudport:operator-mode:high-contrast', String(next));
      return next;
    });
  }

  function toggleSound() {
    setSoundEnabled((current) => {
      const next = !current;
      storage?.setItem('cloudport:operator-mode:sound', String(next));
      return next;
    });
  }

  return <div className={`operator-mode-layer${highContrast ? ' high-contrast' : ''}`}>
    <main ref={workspaceRef} className="operator-workspace" role="dialog" aria-modal="true" aria-labelledby="operator-mode-title" tabIndex="-1">
      <header className="operator-workspace-header">
        <div><span>Canal de campo</span><h1 id="operator-mode-title">Modo operador</h1><p>Uma tarefa por vez · {sources.map((source) => source.title).join(' · ') || 'sem módulos autorizados'}</p></div>
        <div className="operator-workspace-actions">
          <button type="button" className="operator-secondary" aria-pressed={highContrast} onClick={toggleContrast}>Alto contraste</button>
          <button type="button" className="operator-secondary" aria-pressed={soundEnabled} onClick={toggleSound}>Som {soundEnabled ? 'ligado' : 'desligado'}</button>
          <button type="button" className="operator-secondary" onClick={() => setManualOpen(true)}>Manual</button>
          <button type="button" className="operator-icon-button" aria-label="Voltar ao modo completo" onClick={onClose}>×</button>
        </div>
      </header>

      <ConnectivityBar online={online} lastSync={lastSync} queue={queue} loading={syncing} onSync={syncQueue} />
      {message && <div className="operator-message" role="status"><span>{message}</span><button type="button" aria-label="Fechar mensagem" onClick={() => setMessage('')}>×</button></div>}
      {error && <div className="operator-message error" role="alert"><span>{error}</span><button type="button" aria-label="Fechar erro" onClick={() => setError('')}>×</button></div>}
      {blockerNote && <div className="operator-message warning" role="status"><span>Impedimento temporário: {blockerNote}</span><button type="button" onClick={() => currentTask && openFullMode(currentTask)}>Concluir registro</button></div>}

      <nav className="operator-task-tabs" aria-label="Fila de tarefas">
        {tasks.slice(0, 8).map((task, index) => <button type="button" key={task.id} className={index === selectedIndex ? 'active' : ''} aria-current={index === selectedIndex ? 'step' : undefined} onClick={() => setSelectedIndex(index)}><span>{index + 1}</span><strong>{task.title}</strong></button>)}
        {tasks.length > 8 && <span>+{tasks.length - 8} tarefa(s)</span>}
      </nav>

      {loading && !tasks.length ? <section className="operator-loading" role="status"><span /><strong>Carregando tarefas autorizadas...</strong></section> : <div className="operator-main-grid">
        <TaskSummary task={currentTask} nextTask={nextTask} />
        <ScanPanel
          task={currentTask}
          scan={scan}
          input={input}
          onInput={setInput}
          onSubmit={(event) => { event.preventDefault(); validateRead(input); }}
          onStartCamera={startCamera}
          cameraSupported={cameraSupported}
          cameraActive={cameraActive}
          onStopCamera={stopCamera}
          videoRef={videoRef}
        />
        <ConfirmationPanel task={currentTask} scan={scan} busy={busyCommand} online={online} onConfirm={confirmTask} onOpenFull={openFullMode} onBlocker={reportBlocker} />
      </div>}

      <QueuePanel queue={queue} onRetry={retryCommand} onDiscard={discardCommand} />

      <footer className="operator-workspace-footer">
        <button type="button" className="operator-secondary" disabled={loading} onClick={loadTasks}>{loading ? 'Atualizando...' : 'Atualizar tarefas'}</button>
        <span>{Object.entries(results).filter(([, result]) => result.status === 'rejected').length} fonte(s) indisponível(is)</span>
        <button type="button" onClick={onClose}>Voltar ao modo completo</button>
      </footer>
    </main>
    {manualOpen && <OperatorManual onClose={() => setManualOpen(false)} />}
  </div>;
}
