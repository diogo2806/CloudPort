const ROLE_PREFIX = 'ROLE_';

export const COCKPIT_REFRESH_OPTIONS = [0, 30, 60, 120, 300];

export const COCKPIT_DEFINITIONS = [
  {
    key: 'alerts',
    loader: 'visibility',
    title: 'Exceções críticas',
    description: 'Alertas ativos que exigem reconhecimento ou tratamento.',
    route: '/home/alertas',
    roles: [],
    riskMetric: true,
    period: 'Estado atual da central de alertas'
  },
  {
    key: 'gate',
    loader: 'gate',
    title: 'Filas e SLA do Gate',
    description: 'Agendamentos aguardando atendimento, atrasados ou retidos.',
    route: '/home/gate/dashboard',
    roles: ['ADMIN_PORTO', 'OPERADOR_GATE', 'PLANEJADOR'],
    riskMetric: true,
    period: 'Janela operacional atual'
  },
  {
    key: 'yard',
    loader: 'yard',
    title: 'Trabalho do pátio',
    description: 'Ordens bloqueadas, suspensas ou ainda pendentes de execução.',
    route: '/home/patio/lista-trabalho',
    roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO'],
    riskMetric: true,
    period: 'Ordens abertas no momento'
  },
  {
    key: 'rail',
    loader: 'rail',
    title: 'Operação ferroviária',
    description: 'Visitas de trem recebidas, processando ou próximas da operação.',
    route: '/home/ferrovia/line-up',
    roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO'],
    riskMetric: false,
    period: 'Próximos 30 dias'
  },
  {
    key: 'vessel',
    loader: 'vessel',
    title: 'Navios e escalas',
    description: 'Escalas ativas ou previstas na janela de planejamento.',
    route: '/home/navio/line-up',
    roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE'],
    riskMetric: false,
    period: 'Próximos 30 dias'
  },
  {
    key: 'equipment',
    loader: 'equipment',
    title: 'Disponibilidade de equipamentos',
    description: 'CHEs e equipamentos sem prontidão, conexão ou telemetria válida.',
    route: '/home/control-room',
    roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO', 'OPERADOR_GATE'],
    riskMetric: true,
    period: 'Última telemetria disponível'
  },
  {
    key: 'edi',
    loader: 'edi',
    title: 'Integrações EDI',
    description: 'Processamentos com erro, rejeição ou necessidade de reprocessamento.',
    route: '/home/integracoes/edi',
    roles: ['ADMIN_PORTO', 'PLANEJADOR'],
    riskMetric: true,
    period: 'Últimos 50 processamentos'
  }
];

function normalizeRole(value) {
  const role = String(value ?? '').toUpperCase().replace(/[^A-Z0-9_]/g, '');
  if (!role) return '';
  return role.startsWith(ROLE_PREFIX) ? role : `${ROLE_PREFIX}${role}`;
}

function sessionRoles(session = {}) {
  return [...new Set([...(session.roles ?? []), session.perfil]
    .map(normalizeRole)
    .filter(Boolean))];
}

function hasRole(session, roles = []) {
  if (!roles.length) return true;
  const current = sessionRoles(session);
  return roles.some((role) => current.includes(normalizeRole(role)));
}

export function permittedCockpitDefinitions(session = {}) {
  return COCKPIT_DEFINITIONS
    .filter((definition) => hasRole(session, definition.roles))
    .map((definition) => ({ ...definition, roles: [...definition.roles] }));
}

function rowsFrom(payload, keys = []) {
  if (Array.isArray(payload)) return payload;
  for (const key of keys) {
    if (Array.isArray(payload?.[key])) return payload[key];
  }
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.conteudo)) return payload.conteudo;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.dados)) return payload.dados;
  return [];
}

function finiteNumber(...values) {
  for (const value of values) {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return 0;
}

function statusOf(item = {}) {
  return String(item.status ?? item.estado ?? item.fase ?? item.situacao ?? item.statusDescricao ?? 'SEM_STATUS')
    .toUpperCase()
    .replace(/[^A-Z0-9_]/g, '_');
}

function statusDistribution(rows) {
  const counts = new Map();
  rows.forEach((row) => {
    const status = statusOf(row);
    counts.set(status, (counts.get(status) ?? 0) + 1);
  });
  return [...counts]
    .sort((left, right) => right[1] - left[1] || left[0].localeCompare(right[0]))
    .slice(0, 5)
    .map(([label, value]) => ({ label, value }));
}

function includesAny(value, terms) {
  const normalized = String(value ?? '').toUpperCase();
  return terms.some((term) => normalized.includes(term));
}

function buildAlerts(payload) {
  const value = finiteNumber(payload?.alertasAtivos, payload?.totalAlertas, payload?.alertasNaoReconhecidos);
  const distribution = [
    ['CRÍTICOS', payload?.alertasCriticos ?? payload?.criticos],
    ['ALTOS', payload?.alertasAltos ?? payload?.altos],
    ['NÃO RECONHECIDOS', payload?.alertasNaoReconhecidos ?? payload?.naoReconhecidos]
  ].filter(([, count]) => Number.isFinite(Number(count)))
    .map(([label, count]) => ({ label, value: Number(count) }));
  return { value, total: value, attention: value, distribution, detail: value ? 'Ocorrências aguardando ação.' : 'Nenhuma exceção ativa informada.' };
}

function buildGate(payload) {
  const rows = rowsFrom(payload, ['agendamentos', 'veiculos', 'fila', 'atendimentos']);
  const waiting = rows.filter((row) => includesAny(statusOf(row), ['AGEND', 'FILA', 'AGUARD', 'CHAMAD', 'CHEGOU', 'PRE_GATE']));
  const late = rows.filter((row) => finiteNumber(row.tempoFilaMinutos, row.minutosEmFila, row.atrasoMinutos) > 30
    || includesAny(statusOf(row), ['ATRAS', 'VENCID', 'RETID', 'BLOQUEAD']));
  const attention = new Set([...waiting, ...late]).size;
  return {
    value: attention,
    total: rows.length,
    attention,
    distribution: statusDistribution(rows),
    detail: `${waiting.length} aguardando · ${late.length} com atraso ou retenção.`
  };
}

function buildYard(payload) {
  const rows = rowsFrom(payload, ['ordens', 'workInstructions', 'instrucoes']);
  const blocked = rows.filter((row) => includesAny(statusOf(row), ['BLOQUE', 'SUSPEN', 'ERRO', 'FALHA', 'PURGATORIO']));
  const pending = rows.filter((row) => includesAny(statusOf(row), ['PEND', 'PLANEJ', 'DISPONIVEL', 'CRIADA']));
  const attention = new Set([...blocked, ...pending]).size;
  return {
    value: attention,
    total: rows.length,
    attention: blocked.length,
    distribution: statusDistribution(rows),
    detail: `${blocked.length} bloqueadas ou suspensas · ${pending.length} pendentes.`
  };
}

function buildRail(payload) {
  const rows = rowsFrom(payload, ['visitas', 'trens', 'content']);
  const active = rows.filter((row) => !includesAny(statusOf(row), ['PARTIU', 'CANCEL', 'ENCERRAD']));
  const processing = active.filter((row) => includesAny(statusOf(row), ['CHEGOU', 'PROCESS', 'OPERACAO', 'RECEBID']));
  return {
    value: active.length,
    total: rows.length,
    attention: processing.length,
    distribution: statusDistribution(rows),
    detail: `${processing.length} recebidas ou em processamento.`
  };
}

function buildVessel(payload) {
  const rows = rowsFrom(payload, ['escalas', 'visitas', 'content']);
  const active = rows.filter((row) => !includesAny(statusOf(row), ['PARTIU', 'CANCEL', 'FINALIZ', 'ENCERRAD']));
  const operating = active.filter((row) => includesAny(statusOf(row), ['ATRAC', 'OPERANDO', 'OPERACAO', 'BERCO']));
  return {
    value: active.length,
    total: rows.length,
    attention: operating.length,
    distribution: statusDistribution(rows),
    detail: `${operating.length} atracadas ou em operação.`
  };
}

function buildEquipment(payload) {
  const rows = rowsFrom(payload, ['equipamentos', 'telemetrias', 'content']);
  const unavailable = rows.filter((row) => {
    const status = statusOf(row);
    return includesAny(status, ['OFFLINE', 'INDISPON', 'FALHA', 'MANUTEN', 'PARADO', 'SEM_SINAL'])
      || row.conectado === false
      || row.disponivel === false
      || row.pronto === false;
  });
  return {
    value: unavailable.length,
    total: rows.length,
    attention: unavailable.length,
    distribution: statusDistribution(rows),
    detail: `${Math.max(rows.length - unavailable.length, 0)} disponíveis · ${unavailable.length} indisponíveis.`
  };
}

function buildEdi(payload) {
  const rows = rowsFrom(payload, ['processamentos', 'content']);
  const failed = rows.filter((row) => includesAny(statusOf(row), ['ERRO', 'FALHA', 'REJEIT', 'INVALID', 'PENDENTE_REPROCESSAMENTO']));
  return {
    value: failed.length,
    total: rows.length,
    attention: failed.length,
    distribution: statusDistribution(rows),
    detail: `${failed.length} com erro, rejeição ou reprocessamento pendente.`
  };
}

const BUILDERS = {
  alerts: buildAlerts,
  gate: buildGate,
  yard: buildYard,
  rail: buildRail,
  vessel: buildVessel,
  equipment: buildEquipment,
  edi: buildEdi
};

export function trendFrom(currentValue, previousValue, riskMetric = false) {
  if (!Number.isFinite(Number(previousValue))) return { direction: 'new', label: 'Primeira leitura', favorable: null };
  const delta = Number(currentValue) - Number(previousValue);
  if (delta === 0) return { direction: 'stable', label: 'Sem alteração', favorable: true };
  const direction = delta > 0 ? 'up' : 'down';
  const favorable = riskMetric ? delta < 0 : delta > 0;
  return {
    direction,
    label: `${delta > 0 ? '+' : ''}${delta} desde a leitura anterior`,
    favorable
  };
}

export function buildCockpitBlock(definition, result = {}, previousValue, updatedAt = new Date().toISOString()) {
  if (result.status === 'rejected' || result.error) {
    return {
      ...definition,
      state: 'error',
      error: String(result.error?.message ?? result.error ?? 'Fonte indisponível.'),
      value: null,
      total: 0,
      attention: 0,
      distribution: [],
      updatedAt,
      trend: { direction: 'unavailable', label: 'Sem comparação', favorable: null }
    };
  }

  const builder = BUILDERS[definition.key];
  const metrics = builder ? builder(result.payload ?? result.value ?? result) : { value: 0, total: 0, attention: 0, distribution: [], detail: '' };
  const state = metrics.total === 0 && metrics.value === 0 ? 'empty' : metrics.attention > 0 ? 'attention' : 'ready';
  return {
    ...definition,
    ...metrics,
    state,
    updatedAt,
    trend: trendFrom(metrics.value, previousValue, definition.riskMetric)
  };
}

export function createCockpitSnapshot(blocks = [], updatedAt = new Date().toISOString()) {
  return {
    updatedAt,
    values: Object.fromEntries(blocks.filter((block) => Number.isFinite(Number(block.value))).map((block) => [block.key, Number(block.value)]))
  };
}

export function defaultCockpitPreferences(definitions = COCKPIT_DEFINITIONS) {
  return {
    order: definitions.map((definition) => definition.key),
    hidden: [],
    refreshSeconds: 60
  };
}

export function sanitizeCockpitPreferences(preferences = {}, definitions = COCKPIT_DEFINITIONS) {
  const allowed = new Set(definitions.map((definition) => definition.key));
  const requestedOrder = Array.isArray(preferences.order) ? preferences.order.filter((key) => allowed.has(key)) : [];
  const remaining = definitions.map((definition) => definition.key).filter((key) => !requestedOrder.includes(key));
  const refreshSeconds = COCKPIT_REFRESH_OPTIONS.includes(Number(preferences.refreshSeconds)) ? Number(preferences.refreshSeconds) : 60;
  return {
    order: [...new Set([...requestedOrder, ...remaining])],
    hidden: [...new Set(Array.isArray(preferences.hidden) ? preferences.hidden.filter((key) => allowed.has(key)) : [])],
    refreshSeconds
  };
}

export function applyCockpitPreferences(blocks = [], preferences = {}) {
  const sanitized = sanitizeCockpitPreferences(preferences, blocks);
  const byKey = new Map(blocks.map((block) => [block.key, block]));
  return sanitized.order.map((key) => byKey.get(key)).filter(Boolean);
}

export function moveCockpitBlock(preferences, key, direction, definitions = COCKPIT_DEFINITIONS) {
  const sanitized = sanitizeCockpitPreferences(preferences, definitions);
  const index = sanitized.order.indexOf(key);
  const target = index + (direction < 0 ? -1 : 1);
  if (index < 0 || target < 0 || target >= sanitized.order.length) return sanitized;
  const order = [...sanitized.order];
  [order[index], order[target]] = [order[target], order[index]];
  return { ...sanitized, order };
}

export function toggleCockpitBlock(preferences, key, definitions = COCKPIT_DEFINITIONS) {
  const sanitized = sanitizeCockpitPreferences(preferences, definitions);
  const hidden = new Set(sanitized.hidden);
  if (hidden.has(key)) hidden.delete(key);
  else hidden.add(key);
  return { ...sanitized, hidden: [...hidden] };
}

export function cockpitStorageKey(session = {}, suffix = 'preferences') {
  const identity = String(session.id ?? session.email ?? session.nome ?? 'anonimo')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9_-]+/g, '-');
  return `cloudport:cockpit:${identity || 'anonimo'}:${suffix}`;
}

export function readCockpitStorage(storage, key, fallback = null) {
  try {
    const value = JSON.parse(storage?.getItem(key) ?? 'null');
    return value ?? fallback;
  } catch {
    return fallback;
  }
}

export function writeCockpitStorage(storage, key, value) {
  try {
    storage?.setItem(key, JSON.stringify(value));
    return true;
  } catch {
    return false;
  }
}

export function isCockpitStale(updatedAt, refreshSeconds, now = Date.now()) {
  if (!updatedAt) return true;
  const timestamp = new Date(updatedAt).getTime();
  if (!Number.isFinite(timestamp)) return true;
  const threshold = Math.max(Number(refreshSeconds) || 60, 30) * 2000;
  return now - timestamp > threshold;
}
