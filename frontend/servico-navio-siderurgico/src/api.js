const SESSION_KEY = 'cloudportControlRoomSession';
const REQUEST_TIMEOUT_MS = 5000;
const SSE_RECONNECT_MS = 2000;

let runtimeConfig = {
  baseApiUrl: '',
  trustedParentOrigins: []
};

function normalizeBaseUrl(value) {
  return String(value ?? '').replace(/\/+$/, '');
}

function decodeJwt(token) {
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length < 2) return null;
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padding = '='.repeat((4 - (base64.length % 4)) % 4);
    return JSON.parse(decodeURIComponent(atob(base64 + padding)
      .split('')
      .map((character) => `%${(`00${character.charCodeAt(0).toString(16)}`).slice(-2)}`)
      .join('')));
  } catch {
    return null;
  }
}

function normalizeRole(role) {
  const normalized = String(role ?? '').trim().toUpperCase();
  if (!normalized) return '';
  return normalized.startsWith('ROLE_') ? normalized : `ROLE_${normalized}`;
}

function storageAvailable() {
  return typeof sessionStorage !== 'undefined';
}

export function readSession() {
  if (!storageAvailable()) return null;
  const raw = sessionStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    const session = JSON.parse(raw);
    if (!session?.token) return null;
    const payload = decodeJwt(session.token);
    if (typeof payload?.exp === 'number' && payload.exp * 1000 <= Date.now()) {
      clearSession();
      return null;
    }
    return {
      token: session.token,
      nome: session.nome || payload?.nome || payload?.sub || 'operador',
      roles: Array.isArray(session.roles) ? session.roles.map(normalizeRole).filter(Boolean) : []
    };
  } catch {
    clearSession();
    return null;
  }
}

export function saveSession(response) {
  const source = response?.data ?? response ?? {};
  const token = source.token ?? source.accessToken ?? response?.token ?? response?.accessToken ?? '';
  if (!token) throw new Error('O serviço de autenticação não retornou um token JWT.');
  const payload = decodeJwt(token) ?? {};
  const responseRoles = Array.isArray(source.roles) ? source.roles : (source.roles ? [source.roles] : []);
  const tokenRoles = Array.isArray(payload.roles) ? payload.roles : (payload.role ? [payload.role] : []);
  const roles = Array.from(new Set([...responseRoles, ...tokenRoles].map(normalizeRole).filter(Boolean)));
  const session = {
    token,
    nome: payload.nome ?? source.nome ?? source.name ?? source.login ?? payload.sub ?? 'operador',
    roles
  };
  if (storageAvailable()) sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  return session;
}

export function clearSession() {
  if (storageAvailable()) sessionStorage.removeItem(SESSION_KEY);
}

export function hasAnyRole(session, ...roles) {
  const current = session?.roles ?? [];
  return roles.some((role) => current.includes(normalizeRole(role)));
}

export async function loadRuntimeConfig() {
  const response = await fetch('/assets/configuracao.json', { cache: 'no-store' });
  if (!response.ok) throw new Error('Não foi possível carregar a configuração do Control Room.');
  const loaded = await response.json();
  runtimeConfig = {
    baseApiUrl: normalizeBaseUrl(loaded.baseApiUrl),
    trustedParentOrigins: Array.isArray(loaded.trustedParentOrigins) ? loaded.trustedParentOrigins : []
  };
  return runtimeConfig;
}

export function setRuntimeConfigForTests(config = {}) {
  runtimeConfig = {
    baseApiUrl: normalizeBaseUrl(config.baseApiUrl),
    trustedParentOrigins: Array.isArray(config.trustedParentOrigins) ? config.trustedParentOrigins : []
  };
}

function createCorrelationId() {
  return globalThis.crypto?.randomUUID?.() ?? `control-room-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function createTraceId() {
  const bytes = new Uint8Array(16);
  if (globalThis.crypto?.getRandomValues) globalThis.crypto.getRandomValues(bytes);
  else for (let index = 0; index < bytes.length; index += 1) bytes[index] = Math.floor(Math.random() * 256);
  return Array.from(bytes, (value) => value.toString(16).padStart(2, '0')).join('');
}

function isFormData(body) {
  return typeof FormData !== 'undefined' && body instanceof FormData;
}

function commandBody(motivo, extra = {}) {
  let normalized = String(motivo ?? '').trim();
  if (!normalized && typeof window !== 'undefined' && typeof window.prompt === 'function') {
    normalized = String(window.prompt('Informe o motivo da operação administrativa:', '') ?? '').trim();
  }
  if (!normalized) throw new Error('O motivo da operação é obrigatório.');
  return { ...extra, motivo: normalized };
}

function enrichCommand(path, method, body, session, correlationId) {
  const commandMethod = ['POST', 'PUT', 'PATCH'].includes(method);
  const operationalRoute = path.includes('/visitas-navio/') || path.includes('/yard/patio/');
  if (!session?.token || !commandMethod || !operationalRoute || !body || Array.isArray(body) || typeof body !== 'object' || isFormData(body)) {
    return body;
  }
  const command = {
    ...body,
    usuario: body.usuario ?? session.nome,
    origemAcao: body.origemAcao ?? 'CONTROL_ROOM_NAVIO_PATIO',
    correlationId: body.correlationId ?? correlationId
  };
  if (path.endsWith('/dispatch')) command.operador = session.nome;
  return command;
}

async function request(path, options = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), options.timeoutMs ?? REQUEST_TIMEOUT_MS);
  const session = readSession();
  const method = String(options.method ?? 'GET').toUpperCase();
  const publicResource = path.endsWith('/auth/login');
  const correlationId = createCorrelationId();
  const traceId = createTraceId();
  const body = enrichCommand(path, method, options.body, publicResource ? null : session, correlationId);
  const headers = new Headers(options.headers ?? {});
  headers.set('Accept', options.accept ?? 'application/json');
  if (body !== undefined && !isFormData(body)) headers.set('Content-Type', 'application/json');
  if (session?.token && !publicResource) {
    headers.set('Authorization', `Bearer ${session.token}`);
    headers.set('X-Correlation-Id', correlationId);
    headers.set('X-Trace-Id', traceId);
    headers.set('traceparent', `00-${traceId}-${traceId.slice(0, 16)}-01`);
  }
  try {
    const response = await fetch(`${runtimeConfig.baseApiUrl}${path}`, {
      ...options,
      method,
      headers,
      body: body === undefined || isFormData(body) ? body : JSON.stringify(body),
      signal: controller.signal
    });
    const contentType = response.headers.get('content-type') ?? '';
    const payload = options.responseType === 'blob'
      ? await response.blob()
      : contentType.includes('application/json')
        ? await response.json()
        : await response.text();
    if (!response.ok) {
      const error = new Error(payload?.mensagem ?? payload?.erro ?? payload?.message ?? `Falha HTTP ${response.status}`);
      error.payload = payload;
      error.status = response.status;
      throw error;
    }
    return payload;
  } catch (error) {
    if (error?.name === 'AbortError') throw new Error('A requisição excedeu o tempo limite.');
    throw error;
  } finally {
    clearTimeout(timer);
  }
}

async function download(path, filename, accept) {
  const blob = await request(path, {
    responseType: 'blob',
    accept,
    timeoutMs: 30000
  });
  if (typeof document === 'undefined' || typeof URL === 'undefined') return blob;
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
  return blob;
}

function parseSseBlock(block) {
  const event = { id: '', name: 'message', data: '' };
  for (const rawLine of block.split('\n')) {
    const line = rawLine.replace(/\r$/, '');
    if (!line || line.startsWith(':')) continue;
    const separator = line.indexOf(':');
    const field = separator < 0 ? line : line.slice(0, separator);
    const value = separator < 0 ? '' : line.slice(separator + 1).replace(/^ /, '');
    if (field === 'id') event.id = value;
    else if (field === 'event') event.name = value;
    else if (field === 'data') event.data += `${value}\n`;
  }
  event.data = event.data.replace(/\n$/, '');
  if (!event.data) return null;
  try {
    event.payload = JSON.parse(event.data);
  } catch {
    event.payload = event.data;
  }
  return event;
}

export function subscribeSse(path, handlers = {}) {
  let closed = false;
  let lastEventId = '';
  let controller = null;
  let reconnectTimer = null;

  const connect = async () => {
    if (closed) return;
    controller = new AbortController();
    const session = readSession();
    const headers = new Headers({ Accept: 'text/event-stream' });
    if (session?.token) headers.set('Authorization', `Bearer ${session.token}`);
    if (lastEventId) headers.set('Last-Event-ID', lastEventId);
    headers.set('X-Correlation-Id', createCorrelationId());
    handlers.onState?.('CONECTANDO');
    try {
      const response = await fetch(`${runtimeConfig.baseApiUrl}${path}`, {
        headers,
        cache: 'no-store',
        signal: controller.signal
      });
      if (!response.ok || !response.body) throw new Error(`Falha ao conectar ao stream: HTTP ${response.status}`);
      handlers.onState?.('CONECTADO');
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      while (!closed) {
        const { value, done } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n');
        let separator = buffer.indexOf('\n\n');
        while (separator >= 0) {
          const block = buffer.slice(0, separator);
          buffer = buffer.slice(separator + 2);
          const event = parseSseBlock(block);
          if (event) {
            if (event.id) lastEventId = event.id;
            handlers.onEvent?.(event);
          }
          separator = buffer.indexOf('\n\n');
        }
      }
    } catch (error) {
      if (!closed && error?.name !== 'AbortError') handlers.onError?.(error);
    } finally {
      if (!closed) {
        handlers.onState?.('RECONECTANDO');
        reconnectTimer = setTimeout(connect, SSE_RECONNECT_MS);
      }
    }
  };

  connect();
  return () => {
    closed = true;
    if (reconnectTimer) clearTimeout(reconnectTimer);
    controller?.abort();
    handlers.onState?.('DESCONECTADO');
  };
}

function optionalVisitQuery(visitaNavioId) {
  return visitaNavioId == null ? '' : `?visitaNavioId=${encodeURIComponent(visitaNavioId)}`;
}

export const api = {
  autenticar: (login, senha) => request('/auth/login', { method: 'POST', body: { login, senha } }),
  listarNavios: () => request('/navios-siderurgicos'),
  listarVisitas: () => request('/visitas-navio'),
  alterarFaseVisita: (visitaId, fase, motivo) => {
    const comando = commandBody(motivo);
    return request(`/visitas-navio/${visitaId}/fase`, {
      method: 'PATCH',
      body: { fase, observacao: comando.motivo }
    });
  },
  listarItensVisita: (visitaId) => request(`/visitas-navio/${visitaId}/itens`),
  obterResumo: (visitaId) => request(`/visitas-navio/${visitaId}/resumo-operacional`),
  listarEventos: (visitaId) => request(`/visitas-navio/${visitaId}/eventos`),
  assinarControlRoom: (visitaId, handlers) => subscribeSse(`/visitas-navio/${visitaId}/stream`, handlers),
  assinarEventos: (visitaId, handlers) => subscribeSse(`/visitas-navio/${visitaId}/eventos/stream`, handlers),
  obterControlRoom: (visitaId) => request(`/visitas-navio/${visitaId}/control-room`),
  obterQuayMonitor: (visitaId) => request(`/visitas-navio/${visitaId}/quay-monitor`),
  otimizarOperacaoGlobal: (visitaId) => request(`/visitas-navio/${visitaId}/otimizacao-global`, { method: 'POST', body: {} }),
  validarRestricoesEstruturais: (visitaId, configuracao) => request(`/visitas-navio/${visitaId}/validacoes-estruturais`, { method: 'POST', body: configuracao }),
  baixarRelatorioCsv: (visitaId) => download(`/visitas-navio/${visitaId}/relatorio-operacional-integrado.csv`, `relatorio-operacional-visita-${visitaId}.csv`, 'text/csv'),
  baixarRelatorioPdf: (visitaId) => download(`/visitas-navio/${visitaId}/relatorio-operacional-integrado.pdf`, `relatorio-operacional-visita-${visitaId}.pdf`, 'application/pdf'),
  listarTelemetriaEquipamentos: () => request('/yard/patio/equipamentos/telemetria'),
  assinarTelemetriaEquipamentos: (handlers) => subscribeSse('/yard/patio/equipamentos/telemetria/stream', handlers),
  obterResumoIntegracaoPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio`),
  gerarReservasPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio/reservas`, { method: 'POST', body: { tipoReserva: 'TENTATIVA', somentePendentes: true } }),
  listarReservasPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio/reservas`),
  gerarOrdensPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio/gerar-ordens`, { method: 'POST', body: { tipoMovimento: null, modo: 'SOMENTE_PENDENTES', gerarReservasAutomaticas: true } }),
  listarOrdensPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio/ordens`),
  atualizarPrioridadeOrdemPatio: (visitaId, ordemId, prioridadeOperacional, motivo) => request(`/visitas-navio/${visitaId}/integracao-patio/ordens/${ordemId}/prioridade`, { method: 'PATCH', body: commandBody(motivo, { prioridadeOperacional, prioridadeBusca: false }) }),
  suspenderOrdemPatio: (visitaId, ordemId, motivo) => request(`/visitas-navio/${visitaId}/integracao-patio/ordens/${ordemId}/suspender`, { method: 'PATCH', body: commandBody(motivo) }),
  retomarOrdemPatio: (visitaId, ordemId, motivo) => request(`/visitas-navio/${visitaId}/integracao-patio/ordens/${ordemId}/retomar`, { method: 'PATCH', body: commandBody(motivo) }),
  listarFilasPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio/filas`),
  listarWorkQueuesPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio/work-queues`),
  ativarWorkQueuePatio: (id, motivo) => request(`/yard/patio/work-queues/${id}/ativar`, { method: 'PATCH', body: commandBody(motivo) }),
  desativarWorkQueuePatio: (id, motivo) => request(`/yard/patio/work-queues/${id}/desativar`, { method: 'PATCH', body: commandBody(motivo) }),
  atualizarRecursosWorkQueuePatio: (id, body = {}, motivo) => request(`/yard/patio/work-queues/${id}/recursos-operacionais`, { method: 'PATCH', body: commandBody(motivo ?? body.motivo, body) }),
  despacharWorkQueuePatio: (id, body = {}, motivo) => request(`/yard/patio/work-queues/${id}/dispatch`, { method: 'POST', body: commandBody(motivo ?? body.motivo ?? body.observacao, body) }),
  suspenderWorkInstructionPatio: (id, motivo) => request(`/yard/patio/work-instructions/${id}/suspender`, { method: 'POST', body: commandBody(motivo) }),
  retomarWorkInstructionPatio: (id, motivo) => request(`/yard/patio/work-instructions/${id}/retomar`, { method: 'POST', body: commandBody(motivo) }),
  bloquearWorkInstructionPatio: (id, motivo) => request(`/yard/patio/work-instructions/${id}/bloquear`, { method: 'POST', body: commandBody(motivo) }),
  concluirWorkInstructionPatio: (id, motivo) => request(`/yard/patio/work-instructions/${id}/concluir`, { method: 'POST', body: commandBody(motivo) }),
  resetarWorkInstructionPatio: (id, motivo) => request(`/yard/patio/work-instructions/${id}/reset`, { method: 'POST', body: commandBody(motivo) }),
  cancelarWorkInstructionPatio: (id, motivo) => request(`/yard/patio/work-instructions/${id}/cancelar`, { method: 'POST', body: commandBody(motivo) }),
  atualizarPrioridadesWorkInstructionPatio: (id, body = {}, motivo) => request(`/yard/patio/work-instructions/${id}/prioridades`, { method: 'PATCH', body: commandBody(motivo ?? body.motivo, body) }),
  obterDrillDownWorkInstructionPatio: (id) => request(`/yard/patio/work-instructions/${id}/drill-down`),
  obterMatrizEstadosWorkInstructionPatio: () => request('/yard/patio/work-instructions/matriz-estados'),
  listarJobListsEquipamentoPatio: (visitaNavioId) => request(`/yard/patio/equipamentos/job-lists${optionalVisitQuery(visitaNavioId)}`),
  obterJobListEquipamentoPatio: (equipamentoId, visitaNavioId) => request(`/yard/patio/equipamentos/${equipamentoId}/job-list${optionalVisitQuery(visitaNavioId)}`),
  atualizarPowWorkQueuePatio: (id, body = {}, motivo) => request(`/yard/patio/work-queues/${id}/pow`, { method: 'PATCH', body: commandBody(motivo ?? body.motivo, body) }),
  atualizarEquipamentoWorkQueuePatio: (id, body = {}, motivo) => request(`/yard/patio/work-queues/${id}/equipamento`, { method: 'PATCH', body: commandBody(motivo ?? body.motivo, body) }),
  listarOrdensSemCoberturaPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio/sem-cobertura`),
  listarAlertasIntegracaoPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio/alertas`),
  sincronizarStatusPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio/sincronizar-status`, { method: 'POST', body: {} }),
  replanejarPatioVisita: (visitaId, aplicar) => request(`/visitas-navio/${visitaId}/integracao-patio/replanejar`, { method: 'POST', body: { aplicar } }),
  obterRelatorioOperacionalIntegrado: (visitaId) => request(`/visitas-navio/${visitaId}/relatorio-operacional-integrado`)
};

export function formatError(error, fallback = 'Não foi possível concluir a operação.') {
  const payload = error?.payload ?? error?.error ?? {};
  const message = payload?.mensagem ?? payload?.erro ?? payload?.message ?? error?.message ?? fallback;
  const code = payload?.codigo ? ` [${payload.codigo}]` : '';
  const details = payload?.detalhes ? ` - ${typeof payload.detalhes === 'string' ? payload.detalhes : JSON.stringify(payload.detalhes)}` : '';
  const correlation = payload?.correlationId ? ` (correlationId: ${payload.correlationId})` : '';
  return `${message}${code}${details}${correlation}`;
}
