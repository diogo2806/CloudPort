const SESSION_KEY = 'cloudportControlRoomSession';
const REQUEST_TIMEOUT_MS = 5000;

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

function isFormData(body) {
  return typeof FormData !== 'undefined' && body instanceof FormData;
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
  const body = enrichCommand(path, method, options.body, publicResource ? null : session, correlationId);
  const headers = new Headers(options.headers ?? {});
  headers.set('Accept', 'application/json');
  if (body !== undefined && !isFormData(body)) headers.set('Content-Type', 'application/json');
  if (session?.token && !publicResource) {
    headers.set('Authorization', `Bearer ${session.token}`);
    headers.set('X-Correlation-Id', correlationId);
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
    const payload = contentType.includes('application/json') ? await response.json() : await response.text();
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

export const api = {
  autenticar: (login, senha) => request('/auth/login', { method: 'POST', body: { login, senha } }),
  listarNavios: () => request('/navios-siderurgicos'),
  listarVisitas: () => request('/visitas-navio'),
  alterarFaseVisita: (visitaId, fase) => request(`/visitas-navio/${visitaId}/fase`, { method: 'PATCH', body: { fase } }),
  listarItensVisita: (visitaId) => request(`/visitas-navio/${visitaId}/itens`),
  obterResumo: (visitaId) => request(`/visitas-navio/${visitaId}/resumo-operacional`),
  listarEventos: (visitaId) => request(`/visitas-navio/${visitaId}/eventos`),
  obterResumoIntegracaoPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio`),
  gerarReservasPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio/reservas`, { method: 'POST', body: { tipoReserva: 'TENTATIVA', somentePendentes: true } }),
  listarReservasPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio/reservas`),
  gerarOrdensPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio/gerar-ordens`, { method: 'POST', body: { tipoMovimento: null, modo: 'SOMENTE_PENDENTES', gerarReservasAutomaticas: true } }),
  listarOrdensPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio/ordens`),
  atualizarPrioridadeOrdemPatio: (visitaId, ordemId, prioridadeOperacional) => request(`/visitas-navio/${visitaId}/integracao-patio/ordens/${ordemId}/prioridade`, { method: 'PATCH', body: { prioridadeOperacional, prioridadeBusca: false } }),
  suspenderOrdemPatio: (visitaId, ordemId) => request(`/visitas-navio/${visitaId}/integracao-patio/ordens/${ordemId}/suspender`, { method: 'PATCH', body: {} }),
  retomarOrdemPatio: (visitaId, ordemId) => request(`/visitas-navio/${visitaId}/integracao-patio/ordens/${ordemId}/retomar`, { method: 'PATCH', body: {} }),
  listarFilasPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio/filas`),
  listarWorkQueuesPatio: (visitaId) => request(`/visitas-navio/${visitaId}/integracao-patio/work-queues`),
  ativarWorkQueuePatio: (id) => request(`/yard/patio/work-queues/${id}/ativar`, { method: 'PATCH', body: {} }),
  desativarWorkQueuePatio: (id) => request(`/yard/patio/work-queues/${id}/desativar`, { method: 'PATCH', body: {} }),
  atualizarPowWorkQueuePatio: (id, body) => request(`/yard/patio/work-queues/${id}/pow`, { method: 'PATCH', body }),
  atualizarEquipamentoWorkQueuePatio: (id, body) => request(`/yard/patio/work-queues/${id}/equipamento`, { method: 'PATCH', body }),
  atualizarRecursosWorkQueuePatio: (id, body) => request(`/yard/patio/work-queues/${id}/recursos-operacionais`, { method: 'PATCH', body }),
  despacharWorkQueuePatio: (id, body) => request(`/yard/patio/work-queues/${id}/dispatch`, { method: 'POST', body }),
  resetarWorkInstructionPatio: (id) => request(`/yard/patio/work-instructions/${id}/reset`, { method: 'POST', body: {} }),
  cancelarWorkInstructionPatio: (id) => request(`/yard/patio/work-instructions/${id}/cancelar`, { method: 'POST', body: {} }),
  suspenderWorkInstructionPatio: (id, motivo) => request(`/yard/patio/work-instructions/${id}/suspender`, { method: 'POST', body: { motivo } }),
  retomarWorkInstructionPatio: (id, motivo) => request(`/yard/patio/work-instructions/${id}/retomar`, { method: 'POST', body: { motivo } }),
  bloquearWorkInstructionPatio: (id, motivo) => request(`/yard/patio/work-instructions/${id}/bloquear`, { method: 'POST', body: { motivo } }),
  concluirWorkInstructionPatio: (id, motivo) => request(`/yard/patio/work-instructions/${id}/concluir`, { method: 'POST', body: { motivo } }),
  atualizarPrioridadesWorkInstructionPatio: (id, body) => request(`/yard/patio/work-instructions/${id}/prioridades`, { method: 'PATCH', body }),
  obterDrillDownWorkInstructionPatio: (id) => request(`/yard/patio/work-instructions/${id}/drill-down`),
  obterMatrizEstadosWorkInstructionPatio: () => request('/yard/patio/work-instructions/matriz-estados'),
  listarJobListsEquipamentoPatio: (visitaId) => request(`/yard/patio/equipamentos/job-lists${visitaId ? `?visitaNavioId=${visitaId}` : ''}`),
  obterJobListEquipamentoPatio: (equipamentoId, visitaId) => request(`/yard/patio/equipamentos/${equipamentoId}/job-list${visitaId ? `?visitaNavioId=${visitaId}` : ''}`),
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
  const details = payload?.detalhes ? ` - ${payload.detalhes}` : '';
  const correlation = payload?.correlationId ? ` (correlationId: ${payload.correlationId})` : '';
  return `${message}${code}${details}${correlation}`;
}
