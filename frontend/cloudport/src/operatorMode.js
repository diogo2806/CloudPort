const ROLE_PREFIX = 'ROLE_';

export const OPERATOR_SOURCE_DEFINITIONS = [
  { key: 'gate', title: 'Gate', roles: ['ADMIN_PORTO', 'OPERADOR_GATE', 'PLANEJADOR'] },
  { key: 'yard', title: 'Pátio', roles: ['ADMIN_PORTO', 'OPERADOR_PATIO', 'PLANEJADOR'] },
  { key: 'rail', title: 'Ferrovia', roles: ['ADMIN_PORTO', 'OPERADOR_PATIO', 'PLANEJADOR'] },
  { key: 'inventory', title: 'Inventário', roles: ['ADMIN_PORTO', 'OPERADOR_PATIO', 'PLANEJADOR', 'OPERADOR_GATE'] }
];

const ACTIVE_TERMS = ['PEND', 'PLANEJ', 'CRIAD', 'AGEND', 'FILA', 'AGUARD', 'CHAMAD', 'CHEGOU', 'PROCESS', 'EXECU', 'BLOQUE', 'DIVERG'];
const FINISHED_TERMS = ['CONCLUID', 'FINALIZ', 'CANCEL', 'PARTIU', 'ENCERRAD'];

function normalizeRole(value) {
  const normalized = String(value ?? '').toUpperCase().replace(/[^A-Z0-9_]/g, '');
  if (!normalized) return '';
  return normalized.startsWith(ROLE_PREFIX) ? normalized : `${ROLE_PREFIX}${normalized}`;
}

function sessionRoles(session = {}) {
  return [...new Set([...(session.roles ?? []), session.perfil].map(normalizeRole).filter(Boolean))];
}

function hasAnyRole(session, required = []) {
  const current = sessionRoles(session);
  return required.some((role) => current.includes(normalizeRole(role)));
}

export function permittedOperatorSources(session = {}) {
  return OPERATOR_SOURCE_DEFINITIONS.filter((source) => hasAnyRole(session, source.roles)).map((source) => ({ ...source, roles: [...source.roles] }));
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

function statusOf(item = {}) {
  return String(item.status ?? item.estado ?? item.fase ?? item.situacao ?? item.statusDescricao ?? 'SEM_STATUS')
    .toUpperCase()
    .replace(/[^A-Z0-9_]/g, '_');
}

function textOf(item, fields = []) {
  for (const field of fields) {
    const value = String(item?.[field] ?? '').trim();
    if (value) return value;
  }
  return '';
}

function includesAny(value, terms) {
  const normalized = String(value ?? '').toUpperCase();
  return terms.some((term) => normalized.includes(term));
}

function isActive(item) {
  const status = statusOf(item);
  if (includesAny(status, FINISHED_TERMS)) return false;
  return includesAny(status, ACTIVE_TERMS) || status === 'SEM_STATUS';
}

function dateValue(item) {
  const value = item.prazo ?? item.dataLimite ?? item.previstoPara ?? item.horaChegadaPrevista ?? item.inicioPrevisto ?? item.criadoEm;
  const parsed = new Date(value ?? 0).getTime();
  return Number.isFinite(parsed) && parsed > 0 ? parsed : Number.MAX_SAFE_INTEGER;
}

function priorityOf(item) {
  const status = statusOf(item);
  if (includesAny(status, ['BLOQUE', 'ERRO', 'FALHA', 'DIVERG', 'ATRAS', 'VENCID'])) return 0;
  if (includesAny(status, ['EXECU', 'PROCESS', 'CHEGOU', 'FILA'])) return 1;
  return 2;
}

function baseTask(source, item, index) {
  const id = item.id ?? item.codigo ?? item.identificador ?? item.numero ?? `${source}-${index + 1}`;
  return {
    id: `${source}-${id}`,
    source,
    sourceId: id,
    status: statusOf(item),
    priority: priorityOf(item),
    deadline: dateValue(item) === Number.MAX_SAFE_INTEGER ? null : new Date(dateValue(item)).toISOString(),
    raw: item
  };
}

function gateTasks(payload) {
  return rowsFrom(payload, ['agendamentos', 'veiculos', 'fila', 'atendimentos'])
    .filter(isActive)
    .map((item, index) => {
      const plate = textOf(item, ['placaVeiculo', 'placa', 'truckPlate']);
      const code = textOf(item, ['codigo', 'appointmentCode', 'identificador']);
      return {
        ...baseTask('gate', item, index),
        title: plate ? `Atender veículo ${plate}` : `Atender agendamento ${code || index + 1}`,
        entityType: plate ? 'PLACA' : 'TAREFA',
        reference: plate || code,
        origin: textOf(item, ['filaOperacional', 'gate', 'facility', 'origem']) || 'Fila do Gate',
        destination: textOf(item, ['pista', 'lane', 'destino']) || 'Próxima etapa do Gate',
        equipment: textOf(item, ['equipamento', 'balanca', 'scanner']),
        action: null,
        actionLabel: 'Abrir no modo completo',
        onlineRequired: true
      };
    });
}

function yardTasks(payload) {
  return rowsFrom(payload, ['ordens', 'workInstructions', 'instrucoes'])
    .filter(isActive)
    .map((item, index) => {
      const status = statusOf(item);
      const unit = textOf(item, ['numeroConteiner', 'unidade', 'containerNumero', 'identificadorUnidade', 'codigoUnidade']);
      const action = includesAny(status, ['PEND', 'PLANEJ', 'CRIAD', 'DISPONIVEL']) ? 'START_YARD_ORDER'
        : includesAny(status, ['EXECU', 'PROCESS']) ? 'COMPLETE_YARD_ORDER'
          : null;
      return {
        ...baseTask('yard', item, index),
        title: unit ? `Movimentar unidade ${unit}` : `Executar ordem de pátio ${item.id ?? index + 1}`,
        entityType: unit ? 'CONTAINER' : 'TAREFA',
        reference: unit || String(item.id ?? ''),
        origin: textOf(item, ['posicaoOrigem', 'origem', 'fromPosition']) || 'Origem não informada',
        destination: textOf(item, ['posicaoDestino', 'destino', 'toPosition']) || 'Destino não informado',
        equipment: textOf(item, ['cheCodigo', 'equipamentoCodigo', 'equipamento']) || 'CHE não atribuído',
        action,
        actionLabel: action === 'START_YARD_ORDER' ? 'Iniciar ordem' : action === 'COMPLETE_YARD_ORDER' ? 'Concluir ordem' : 'Tratar impedimento',
        onlineRequired: true
      };
    });
}

function railOrderTasks(visit, visitIndex) {
  const orders = rowsFrom(visit, ['ordens', 'operacoes', 'workList', 'listaTrabalho']);
  return orders.filter(isActive).map((order, orderIndex) => {
    const status = statusOf(order);
    const unit = textOf(order, ['numeroConteiner', 'containerNumero', 'unidade', 'codigoUnidade']);
    const action = includesAny(status, ['PEND', 'PLANEJ', 'CRIAD']) ? 'START_RAIL_ORDER'
      : includesAny(status, ['EXECU', 'PROCESS']) ? 'COMPLETE_RAIL_ORDER'
        : null;
    const visitId = visit.id ?? visit.visitaId ?? visitIndex + 1;
    const orderId = order.id ?? order.ordemId ?? orderIndex + 1;
    return {
      ...baseTask('rail', order, orderIndex),
      id: `rail-${visitId}-${orderId}`,
      sourceId: orderId,
      visitId,
      title: unit ? `Conferir unidade ${unit}` : `Executar ordem ferroviária ${orderId}`,
      entityType: unit ? 'CONTAINER' : 'TAREFA',
      reference: unit || String(orderId),
      origin: textOf(order, ['origem', 'posicaoOrigem']) || textOf(visit, ['identificadorTrem', 'trem']) || 'Composição ferroviária',
      destination: textOf(order, ['destino', 'posicaoDestino']) || 'Destino operacional',
      equipment: textOf(order, ['identificadorVagao', 'vagao', 'equipamento']) || 'Vagão não informado',
      action,
      actionLabel: action === 'START_RAIL_ORDER' ? 'Iniciar ordem' : action === 'COMPLETE_RAIL_ORDER' ? 'Concluir ordem' : 'Tratar impedimento',
      onlineRequired: true,
      raw: { ...order, visita: visit }
    };
  });
}

function railTasks(payload) {
  const visits = rowsFrom(payload, ['visitas', 'trens', 'content']);
  const nested = visits.flatMap(railOrderTasks);
  if (nested.length) return nested;
  return visits.filter(isActive).map((visit, index) => ({
    ...baseTask('rail', visit, index),
    title: `Conferir visita ${textOf(visit, ['identificadorTrem', 'codigo', 'id']) || index + 1}`,
    entityType: 'TAREFA',
    reference: textOf(visit, ['identificadorTrem', 'codigo']) || String(visit.id ?? ''),
    origin: textOf(visit, ['operadoraFerroviaria', 'origem']) || 'Recepção ferroviária',
    destination: textOf(visit, ['linha', 'destino']) || 'Pátio ferroviário',
    equipment: `${rowsFrom(visit, ['vagoes']).length} vagão(ões)`,
    action: null,
    actionLabel: 'Abrir no modo completo',
    onlineRequired: true
  }));
}

function inventoryTasks(payload) {
  return rowsFrom(payload, ['equipamentos', 'inventario', 'unidades', 'content'])
    .filter((item) => isActive(item) || includesAny(statusOf(item), ['DIVERG', 'NAO_LOCALIZ', 'AVARIA']))
    .map((item, index) => {
      const reference = textOf(item, ['numero', 'numeroConteiner', 'identificador', 'codigo']);
      return {
        ...baseTask('inventory', item, index),
        title: `Conferir inventário ${reference || index + 1}`,
        entityType: reference && /^[A-Z]{4}\d{7}$/.test(reference.toUpperCase().replace(/[^A-Z0-9]/g, '')) ? 'CONTAINER' : 'TAREFA',
        reference,
        origin: textOf(item, ['posicaoAtual', 'posicao', 'localizacao']) || 'Posição não informada',
        destination: 'Confirmar posição física',
        equipment: textOf(item, ['tipoEquipamento', 'tipo', 'categoria']),
        action: null,
        actionLabel: 'Registrar conferência no modo completo',
        onlineRequired: true
      };
    });
}

const TASK_BUILDERS = { gate: gateTasks, yard: yardTasks, rail: railTasks, inventory: inventoryTasks };

export function buildOperatorTasks(results = {}) {
  return Object.entries(results).flatMap(([source, result]) => {
    if (result?.status === 'rejected' || result?.error) return [];
    const builder = TASK_BUILDERS[source];
    return builder ? builder(result?.payload ?? result?.value ?? result) : [];
  }).sort((left, right) => left.priority - right.priority
    || (left.deadline ? new Date(left.deadline).getTime() : Number.MAX_SAFE_INTEGER) - (right.deadline ? new Date(right.deadline).getTime() : Number.MAX_SAFE_INTEGER)
    || left.title.localeCompare(right.title));
}

const ISO_6346_VALUES = (() => {
  const values = {};
  let value = 10;
  for (const letter of 'ABCDEFGHIJKLMNOPQRSTUVWXYZ') {
    while (value % 11 === 0) value += 1;
    values[letter] = value;
    value += 1;
  }
  return values;
})();

export function isValidIso6346(value) {
  const normalized = String(value ?? '').toUpperCase().replace(/[^A-Z0-9]/g, '');
  if (!/^[A-Z]{4}\d{7}$/.test(normalized)) return false;
  const body = normalized.slice(0, 10);
  const expected = Number(normalized[10]);
  const sum = [...body].reduce((total, character, index) => {
    const numeric = /\d/.test(character) ? Number(character) : ISO_6346_VALUES[character];
    return total + numeric * (2 ** index);
  }, 0);
  const remainder = (sum % 11) % 10;
  return remainder === expected;
}

function parseStructuredScan(raw) {
  const value = String(raw ?? '').trim();
  if (!value) return null;
  try {
    const parsed = JSON.parse(value);
    if (parsed && typeof parsed === 'object') {
      const scannedValue = parsed.value ?? parsed.valor ?? parsed.container ?? parsed.unidade ?? parsed.placa ?? parsed.position ?? parsed.posicao ?? parsed.task ?? parsed.tarefa;
      const type = parsed.type ?? parsed.tipo;
      if (scannedValue) return { type: type ? String(type).toUpperCase() : '', value: String(scannedValue) };
    }
  } catch {
    // A leitura pode ser texto simples; JSON inválido não é erro por si só.
  }
  try {
    const url = new URL(value);
    for (const [parameter, type] of [['container', 'CONTAINER'], ['unit', 'CONTAINER'], ['unidade', 'CONTAINER'], ['plate', 'PLACA'], ['placa', 'PLACA'], ['position', 'POSICAO'], ['posicao', 'POSICAO'], ['task', 'TAREFA'], ['tarefa', 'TAREFA']]) {
      const parameterValue = url.searchParams.get(parameter);
      if (parameterValue) return { type, value: parameterValue };
    }
  } catch {
    // Não é URL; segue como leitura simples.
  }
  const prefixed = value.match(/^(CONTAINER|CONTEINER|UNIT|UNIDADE|PLACA|PLATE|POSICAO|POSITION|TASK|TAREFA)\s*[:=]\s*(.+)$/i);
  if (prefixed) {
    const aliases = { CONTEINER: 'CONTAINER', UNIT: 'CONTAINER', UNIDADE: 'CONTAINER', PLATE: 'PLACA', POSITION: 'POSICAO', TASK: 'TAREFA' };
    const type = aliases[prefixed[1].toUpperCase()] ?? prefixed[1].toUpperCase();
    return { type, value: prefixed[2] };
  }
  return { type: '', value };
}

function inferScanType(value) {
  const compact = String(value ?? '').toUpperCase().replace(/\s/g, '');
  if (/^[A-Z]{4}\d{7}$/.test(compact)) return 'CONTAINER';
  if (/^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$/.test(compact) || /^[A-Z]{3}\d{4}$/.test(compact)) return 'PLACA';
  if (/^[A-Z0-9_-]{1,12}[./-][A-Z0-9_.-]{1,20}$/i.test(compact)) return 'POSICAO';
  if (/^(TASK|TAREFA|WI|ORDEM)[-_]?[A-Z0-9-]+$/i.test(compact)) return 'TAREFA';
  return 'DESCONHECIDO';
}

export function validateOperatorScan(raw, expectedTypes = []) {
  const structured = parseStructuredScan(raw);
  if (!structured) {
    return { valid: false, type: 'DESCONHECIDO', value: '', reason: 'Nenhum código foi informado.', correction: 'Aproxime o código da câmera, acione o scanner ou digite o identificador.' };
  }
  const value = String(structured.value ?? '').trim().toUpperCase().replace(structured.type === 'POSICAO' ? /\s+/g : /[^A-Z0-9./_-]/g, '');
  const type = structured.type || inferScanType(value);
  const expected = expectedTypes.map((item) => String(item).toUpperCase());
  if (type === 'CONTAINER' && !isValidIso6346(value)) {
    return { valid: false, type, value, reason: 'Número de contêiner inválido pelo dígito verificador ISO 6346.', correction: 'Confirme as quatro letras, os seis dígitos de série e o dígito verificador.' };
  }
  if (type === 'PLACA' && !(/^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$/.test(value) || /^[A-Z]{3}\d{4}$/.test(value))) {
    return { valid: false, type, value, reason: 'Placa fora do padrão brasileiro antigo ou Mercosul.', correction: 'Digite sete caracteres, sem espaços ou símbolos.' };
  }
  if (type === 'POSICAO' && value.length < 3) {
    return { valid: false, type, value, reason: 'Posição incompleta.', correction: 'Leia ou informe o bloco e a posição completa.' };
  }
  if (type === 'DESCONHECIDO') {
    return { valid: false, type, value, reason: 'Formato não reconhecido.', correction: 'Use um contêiner ISO 6346, placa, posição, QR estruturado ou identificador de tarefa.' };
  }
  if (expected.length && !expected.includes(type)) {
    return { valid: false, type, value, reason: `A tarefa espera ${expected.join(' ou ')}, mas a leitura foi identificada como ${type}.`, correction: 'Confira o objeto físico e leia o código indicado no resumo da tarefa.' };
  }
  return { valid: true, type, value, reason: '', correction: '' };
}

function hash(value) {
  let result = 2166136261;
  for (const character of String(value)) {
    result ^= character.charCodeAt(0);
    result = Math.imul(result, 16777619);
  }
  return (result >>> 0).toString(36);
}

export function operatorCommandKey(task, action, scannedValue = '') {
  return `op-${hash([task?.source, task?.sourceId, task?.visitId, action, scannedValue].join('|'))}`;
}

export function createOperatorCommand(task, action, scan, user = {}) {
  const createdAt = new Date().toISOString();
  const idempotencyKey = operatorCommandKey(task, action, scan?.value);
  return {
    id: idempotencyKey,
    idempotencyKey,
    taskId: task.id,
    source: task.source,
    sourceId: task.sourceId,
    visitId: task.visitId ?? null,
    action,
    scan: scan ? { type: scan.type, value: scan.value } : null,
    status: 'PENDENTE',
    attempts: 0,
    createdAt,
    updatedAt: createdAt,
    user: user.id ?? user.email ?? user.nome ?? 'operador',
    error: null
  };
}

export function enqueueOperatorCommand(queue = [], command) {
  const existing = queue.find((item) => item.idempotencyKey === command.idempotencyKey && !['DESCARTADA', 'CONCLUIDA'].includes(item.status));
  if (existing) return { queue: [...queue], inserted: false, command: existing };
  return { queue: [...queue, command], inserted: true, command };
}

export function updateOperatorCommand(queue = [], id, patch = {}) {
  const updatedAt = new Date().toISOString();
  return queue.map((item) => item.id === id ? { ...item, ...patch, updatedAt } : item);
}

export function removeCompletedCommands(queue = []) {
  return queue.filter((item) => !['CONCLUIDA', 'DESCARTADA'].includes(item.status));
}

export function operatorQueueStorageKey(session = {}) {
  const identity = String(session.id ?? session.email ?? session.nome ?? 'anonimo')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9_-]+/g, '-');
  return `cloudport:operator-mode:${identity || 'anonimo'}:queue`;
}

export function readOperatorQueue(storage, key) {
  try {
    const parsed = JSON.parse(storage?.getItem(key) ?? '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function writeOperatorQueue(storage, key, queue) {
  try {
    storage?.setItem(key, JSON.stringify(queue));
    return true;
  } catch {
    return false;
  }
}
