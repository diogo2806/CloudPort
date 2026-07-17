const SESSION_KEY = 'usuarioAtual';
const USERNAME_KEY = 'nomeUsuario';
const REQUEST_TIMEOUT_MS = 12000;

const sessionExpirationSubscribers = new Set();

let runtimeConfig = {
  baseApiUrl: '',
  navioControlRoomUrl: ''
};

function storage() {
  return typeof globalThis.localStorage !== 'undefined' ? globalThis.localStorage : null;
}

export function sanitizeText(value) {
  return String(value ?? '')
    .normalize('NFKC')
    .replace(/[<>"'`\\\u0000-\u001F\u007F]/g, '')
    .trim();
}

function normalizeBaseUrl(value) {
  return String(value ?? '').trim().replace(/\/+$/, '');
}

export function normalizeRole(role) {
  const normalized = sanitizeText(role).toUpperCase().replace(/[^A-Z0-9_]/g, '');
  if (!normalized) return '';
  return normalized.startsWith('ROLE_') ? normalized : `ROLE_${normalized}`;
}

export function decodeJwt(token) {
  if (!token || typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length < 2) return null;
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padding = '='.repeat((4 - (base64.length % 4)) % 4);
    const decoded = typeof atob === 'function'
      ? atob(base64 + padding)
      : Buffer.from(base64 + padding, 'base64').toString('binary');
    const json = decodeURIComponent(decoded
      .split('')
      .map((character) => `%${(`00${character.charCodeAt(0).toString(16)}`).slice(-2)}`)
      .join(''));
    return JSON.parse(json);
  } catch {
    return null;
  }
}

function uniqueRoles(...sources) {
  return Array.from(new Set(sources
    .flatMap((source) => Array.isArray(source) ? source : source ? [source] : [])
    .map(normalizeRole)
    .filter(Boolean)));
}

export function mapSession(response) {
  const source = response?.data ?? response ?? {};
  const token = source.token ?? source.accessToken ?? response?.token ?? response?.accessToken ?? '';
  if (!token) throw new Error('O serviço de autenticação não retornou um token JWT.');
  const payload = decodeJwt(token) ?? {};
  const roles = uniqueRoles(source.roles, payload.roles, payload.role, source.perfil, payload.perfil);
  return {
    id: payload.userId ?? source.id ?? source.userId ?? '',
    nome: sanitizeText(payload.nome ?? source.nome ?? source.name ?? source.login ?? payload.sub ?? 'operador'),
    email: sanitizeText(source.email ?? payload.email ?? ''),
    token,
    perfil: sanitizeText(payload.perfil ?? source.perfil ?? roles[0] ?? ''),
    roles,
    transportadoraDocumento: sanitizeText(payload.transportadoraDocumento ?? source.transportadoraDocumento ?? ''),
    transportadoraNome: sanitizeText(payload.transportadoraNome ?? source.transportadoraNome ?? '')
  };
}

export function saveSession(response) {
  const session = mapSession(response);
  const target = storage();
  target?.setItem(SESSION_KEY, JSON.stringify(session));
  target?.setItem(USERNAME_KEY, JSON.stringify(session.nome));
  return session;
}

export function readSession() {
  const target = storage();
  const raw = target?.getItem(SESSION_KEY);
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
      ...session,
      nome: sanitizeText(session.nome || payload?.nome || payload?.sub || 'operador'),
      roles: uniqueRoles(session.roles, payload?.roles, payload?.role, session.perfil, payload?.perfil)
    };
  } catch {
    clearSession();
    return null;
  }
}

export function clearSession() {
  const target = storage();
  target?.removeItem(SESSION_KEY);
  target?.removeItem(USERNAME_KEY);
}

export function subscribeSessionExpired(listener) {
  if (typeof listener !== 'function') {
    throw new TypeError('O listener de expiração de sessão deve ser uma função.');
  }
  sessionExpirationSubscribers.add(listener);
  return () => sessionExpirationSubscribers.delete(listener);
}

export function notifySessionExpired() {
  sessionExpirationSubscribers.forEach((listener) => {
    try {
      listener();
    } catch {
      // Um consumidor com falha não pode impedir que os demais encerrem a sessão.
    }
  });
}

export function hasAnyRole(session, ...roles) {
  const current = uniqueRoles(session?.roles, session?.perfil);
  return roles.some((role) => current.includes(normalizeRole(role)));
}

export async function loadRuntimeConfig() {
  const response = await fetch('/assets/configuracao.json', { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`Arquivo de configuração ausente ou inacessível (status ${response.status}).`);
  }
  const loaded = await response.json();
  const baseApiUrl = normalizeBaseUrl(loaded?.baseApiUrl);
  if (!baseApiUrl) throw new Error('O campo "baseApiUrl" não foi informado no arquivo de configuração.');
  runtimeConfig = {
    baseApiUrl,
    navioControlRoomUrl: normalizeBaseUrl(loaded?.navioControlRoomUrl)
  };
  return { ...runtimeConfig };
}

export function getRuntimeConfig() {
  return { ...runtimeConfig };
}

export function createCorrelationId() {
  return globalThis.crypto?.randomUUID?.() ?? `cloudport-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function buildQuery(query) {
  if (!query) return '';
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return;
    if (Array.isArray(value)) {
      value.filter((item) => item !== undefined && item !== null && item !== '')
        .forEach((item) => params.append(key, String(item)));
    } else {
      params.set(key, String(value));
    }
  });
  const serialized = params.toString();
  return serialized ? `?${serialized}` : '';
}

function enrichCommand(path, method, body, session, correlationId) {
  const commandMethod = ['POST', 'PUT', 'PATCH'].includes(method);
  if (!commandMethod || body === undefined || body instanceof FormData || Array.isArray(body) || typeof body !== 'object' || body === null) {
    return body;
  }
  const command = { ...body };
  const operationalPath = path.includes('/gate/')
    || path.includes('/yard/')
    || path.includes('/rail/')
    || path.includes('/visitas-navio')
    || path.includes('/api/edi/processamentos/');
  if (operationalPath) {
    command.usuario ??= session?.nome ?? 'operador';
    command.origemAcao ??= 'PORTAL_CLOUDPORT_REACT';
    command.correlationId ??= correlationId;
  }
  if (path.endsWith('/dispatch')) command.operador = session?.nome ?? 'operador';
  return command;
}

export async function request(path, options = {}) {
  const method = String(options.method ?? 'GET').toUpperCase();
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), options.timeoutMs ?? REQUEST_TIMEOUT_MS);
  const session = options.public ? null : readSession();
  const correlationId = createCorrelationId();
  const headers = new Headers(options.headers ?? {});
  headers.set('Accept', 'application/json');
  headers.set('X-Correlation-Id', correlationId);
  if (session?.token) headers.set('Authorization', `Bearer ${session.token}`);

  let body = enrichCommand(path, method, options.body, session, correlationId);
  if (body !== undefined && !(body instanceof FormData) && !options.rawBody) {
    headers.set('Content-Type', 'application/json');
    body = JSON.stringify(body);
  }

  try {
    const response = await fetch(`${runtimeConfig.baseApiUrl}${path}${buildQuery(options.query)}`, {
      method,
      headers,
      body,
      signal: controller.signal
    });
    const contentType = response.headers.get('content-type') ?? '';
    const payload = response.status === 204
      ? null
      : contentType.includes('application/json')
        ? await response.json()
        : await response.text();
    if (!response.ok) {
      if (response.status === 401) {
        const authenticatedRequest = Boolean(session?.token);
        clearSession();
        if (authenticatedRequest) notifySessionExpired();
      }
      const error = new Error(payload?.mensagem ?? payload?.erro ?? payload?.message ?? `Falha HTTP ${response.status}`);
      error.payload = payload;
      error.status = response.status;
      error.correlationId = response.headers.get('X-Correlation-Id') ?? correlationId;
      throw error;
    }
    return payload;
  } catch (error) {
    if (error?.name === 'AbortError') throw new Error('A requisição excedeu o tempo limite.');
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}

export function normalizePage(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.conteudo)) return payload.conteudo;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.dados)) return payload.dados;
  return [];
}

export function formatError(error, fallback = 'Não foi possível concluir a operação.') {
  const payload = error?.payload ?? error?.error ?? {};
  const message = sanitizeText(payload?.mensagem ?? payload?.erro ?? payload?.message ?? error?.message ?? fallback) || fallback;
  const code = sanitizeText(payload?.codigo);
  const details = sanitizeText(payload?.detalhes);
  const correlation = sanitizeText(payload?.correlationId ?? error?.correlationId);
  return `${message}${code ? ` [${code}]` : ''}${details ? ` - ${details}` : ''}${correlation ? ` (correlationId: ${correlation})` : ''}`;
}

function commandBody(reason, extra = {}) {
  const motivo = sanitizeText(reason);
  if (!motivo) throw new Error('O motivo operacional é obrigatório.');
  return { ...extra, motivo };
}

function optionalVisitQuery(visitaNavioId) {
  return visitaNavioId === undefined || visitaNavioId === null || visitaNavioId === ''
    ? undefined
    : { visitaNavioId };
}

export const api = {
  autenticar: (login, senha) => request('/auth/login', { method: 'POST', body: { login: sanitizeText(login), senha }, public: true }),
  listarAbas: () => request('/api/navegacao/abas'),
  listarRoles: () => request('/api/roles'),
  criarRole: (name) => request('/api/roles', { method: 'POST', body: { name } }),
  atualizarRole: (id, name) => request(`/api/roles/${id}`, { method: 'PUT', body: { name } }),
  excluirRole: (id) => request(`/api/roles/${id}`, { method: 'DELETE' }),
  listarUsuarios: () => request('/api/usuarios'),
  listarSeguranca: (query) => request('/api/configuracoes/seguranca', { query }),
  listarNotificacoes: () => request('/api/configuracoes/notificacoes'),
  atualizarNotificacao: (id, habilitado) => request(`/api/configuracoes/notificacoes/${id}`, { method: 'PATCH', body: { habilitado } }),
  listarPrivacidade: () => request('/api/configuracoes/privacidade'),
  listarGateAgendamentos: (query = { page: 0, size: 100 }) => request('/gate/agendamentos', { query }),
  listarGateJanelas: (query = { page: 0, size: 100 }) => request('/gate/janelas', { query }),
  obterCentralGate: () => request('/gate/agendamentos/visao-completa'),
  listarVisitasFerrovia: (dias = 7) => request('/rail/ferrovia/visitas', { query: { dias } }),
  importarManifestoFerrovia: (file) => {
    const formData = new FormData();
    formData.append('arquivo', file, sanitizeText(file?.name).substring(0, 120));
    return request('/rail/ferrovia/visitas/importacoes', { method: 'POST', body: formData, timeoutMs: 30000 });
  },
  obterMapaPatio: (query = {}) => request('/yard/patio/mapa', { query }),
  obterFiltrosMapaPatio: () => request('/yard/patio/filtros'),
  listarPosicoesPatio: () => request('/yard/patio/posicoes'),
  listarPosicoesReservaveisPatio: () => request('/yard/patio/reservas/posicoes'),
  listarMovimentacoesPatio: () => request('/yard/patio/movimentacoes'),
  listarConteineresPatio: () => request('/yard/patio/conteineres'),
  listarRecursosPatio: () => request('/yard/patio/mapa').then((payload) => payload?.equipamentos ?? []),
  listarOrdensPatio: (status) => request('/yard/patio/ordens', { query: status ? { status } : undefined }),
  listarWorkQueuesPatio: (visitaNavioId) => request('/yard/patio/work-queues', { query: optionalVisitQuery(visitaNavioId) }),
  ativarWorkQueuePatio: (id, reason) => request(`/yard/patio/work-queues/${id}/ativar`, { method: 'PATCH', body: commandBody(reason) }),
  desativarWorkQueuePatio: (id, reason) => request(`/yard/patio/work-queues/${id}/desativar`, { method: 'PATCH', body: commandBody(reason) }),
  despacharWorkQueuePatio: (id, body = {}, reason) => request(`/yard/patio/work-queues/${id}/dispatch`, { method: 'POST', body: commandBody(reason ?? body.motivo ?? body.observacao, body) }),
  suspenderWorkInstructionPatio: (id, reason) => request(`/yard/patio/work-instructions/${id}/suspender`, { method: 'POST', body: commandBody(reason) }),
  retomarWorkInstructionPatio: (id, reason) => request(`/yard/patio/work-instructions/${id}/retomar`, { method: 'POST', body: commandBody(reason) }),
  bloquearWorkInstructionPatio: (id, reason) => request(`/yard/patio/work-instructions/${id}/bloquear`, { method: 'POST', body: commandBody(reason) }),
  concluirWorkInstructionPatio: (id, reason) => request(`/yard/patio/work-instructions/${id}/concluir`, { method: 'POST', body: commandBody(reason) }),
  resetarWorkInstructionPatio: (id, reason) => request(`/yard/patio/work-instructions/${id}/reset`, { method: 'POST', body: commandBody(reason) }),
  cancelarWorkInstructionPatio: (id, reason) => request(`/yard/patio/work-instructions/${id}/cancelar`, { method: 'POST', body: commandBody(reason) }),
  obterDrillDownWorkInstructionPatio: (id) => request(`/yard/patio/work-instructions/${id}/drill-down`),
  obterMatrizEstadosWorkInstructionPatio: () => request('/yard/patio/work-instructions/matriz-estados'),
  listarJobListsEquipamentoPatio: (visitaNavioId) => request('/yard/patio/equipamentos/job-lists', { query: optionalVisitQuery(visitaNavioId) }),
  listarTelemetriaEquipamentosPatio: () => request('/yard/patio/equipamentos/telemetria'),
  obterHeatmapPatio: () => request('/yard/patio/otimizacao-avancada/heatmap'),
  obterNivelOcupacaoPatio: () => request('/yard/patio/otimizacao-avancada/nivel-ocupacao'),
  listarConflitosRtgPatio: () => request('/yard/patio/otimizacao-avancada/rtg/conflitos'),
  analisarReshufflingPatio: () => request('/yard/patio/otimizacao-avancada/reshuffling/plano'),
  executarReshufflingPatio: (reason) => request('/yard/patio/otimizacao-avancada/reshuffling/executar', { method: 'POST', body: commandBody(reason) }),
  listarOrdensOtimizadasPatio: () => request('/yard/patio/ordens/otimizacao/nearest-neighbor'),
  obterEstatisticasOtimizacaoPatio: () => request('/yard/patio/ordens/otimizacao/estatisticas'),
  listarEscalasEmbarque: (dias = 30) => request('/escalas', { query: { dias } }),
  uploadBaplie: (file) => {
    const formData = new FormData();
    formData.append('arquivo', file, sanitizeText(file?.name).substring(0, 120));
    return request('/api/edi/baplie/upload', { method: 'POST', body: formData, timeoutMs: 30000 });
  },
  enviarBaplieTexto: (conteudo) => request('/api/edi/baplie/texto', {
    method: 'POST',
    body: String(conteudo ?? ''),
    headers: { 'Content-Type': 'text/plain; charset=UTF-8' },
    rawBody: true,
    timeoutMs: 30000
  }),
  listarProcessamentosEdi: (query = { tipo: 'BAPLIE', pagina: 0, tamanho: 50 }) => request('/api/edi/processamentos', { query }),
  obterProcessamentoEdi: (id) => request(`/api/edi/processamentos/${id}`),
  reprocessarProcessamentoEdi: (id, motivo) => request(`/api/edi/processamentos/${id}/reprocessar`, { method: 'POST', body: { motivo } }),
  listarBayPlansAtivos: () => request('/api/edi/bay-plan/ativos'),
  listarBayPlansPorNavio: (codigoNavio) => request(`/api/edi/bay-plan/navio/${encodeURIComponent(codigoNavio)}`),
  obterBayPlan: (id) => request(`/api/edi/bay-plan/${id}`),
  criarPlanoVesselPlanner: (bayPlanId) => request('/api/vessel-planner/planos', { method: 'POST', body: { bayPlanId } }),
  obterPlanoVesselPlanner: (id) => request(`/api/vessel-planner/planos/${id}`),
  alocarContainerNoPlano: (id, body) => request(`/api/vessel-planner/planos/${id}/slots/alocar`, { method: 'POST', body }),
  autoEstivarPlano: (id) => request(`/api/vessel-planner/planos/${id}/auto-estivagem`, { method: 'POST' }),
  obterEstabilidadePlano: (id) => request(`/api/vessel-planner/planos/${id}/estabilidade`),
  obterRestowPlano: (id) => request(`/api/vessel-planner/planos/${id}/restow`),
  obterSequenciamentoGuindastes: (id, numGuindastes = 2) => request(`/api/vessel-planner/planos/${id}/sequenciamento-guindastes`, { query: { numGuindastes } }),
  validarPlanoVesselPlanner: (id) => request(`/api/vessel-planner/planos/${id}/validar`, { method: 'POST' }),
  listarNaviosEstivagemBulk: () => request('/api/estivagem-bulk/navios'),
  listarTemplatesEstivagemBulk: () => request('/api/estivagem-bulk/navios/templates'),
  listarPlanosEstivagemBulk: (navioId, codigoViagem) => request('/api/estivagem-bulk/planos', { query: { navioId, codigoViagem } }),
  criarPlanoEstivagemBulk: (dados) => request('/api/estivagem-bulk/planos', { method: 'POST', body: dados }),
  buscarPlanoEstivagemBulk: (planoId) => request(`/api/estivagem-bulk/planos/${planoId}`),
  adicionarBobinaEstivagemBulk: (planoId, bobina) => request(`/api/estivagem-bulk/planos/${planoId}/bobinas`, { method: 'POST', body: bobina }),
  posicionarBobinaEstivagemBulk: (planoId, posicao) => request(`/api/estivagem-bulk/planos/${planoId}/posicionar`, { method: 'POST', body: posicao }),
  analisarTanktopEstivagemBulk: (planoId) => request(`/api/estivagem-bulk/planos/${planoId}/tanktop`),
  analisarEmpilhamentoEstivagemBulk: (planoId, poraoId) => request(`/api/estivagem-bulk/planos/${planoId}/empilhamento/${poraoId}`),
  calcularEstabilidadeEstivagemBulk: (planoId) => request(`/api/estivagem-bulk/planos/${planoId}/estabilidade`),
  calcularSecuringEstivagemBulk: (planoId) => request(`/api/estivagem-bulk/planos/${planoId}/tacktop`, { method: 'POST' }),
  validarPlanoEstivagemBulk: (planoId) => request(`/api/estivagem-bulk/planos/${planoId}/validar`, { method: 'POST' }),
  obterRelatorioEstivagemBulk: (planoId) => request(`/api/estivagem-bulk/planos/${planoId}/relatorio`),
  obterDashboardVisibilidade: () => request('/api/v1/visibilidade/dashboard')
};
