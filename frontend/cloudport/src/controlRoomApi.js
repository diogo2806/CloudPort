import { createCorrelationId, getRuntimeConfig, readSession, request, sanitizeText } from './api.js';

export function buildControlRoomQuery(query = {}) {
  return Object.fromEntries(Object.entries(query).filter(([, value]) => value !== undefined && value !== null && value !== ''));
}

export function normalizeCommandPayload(payload = {}) {
  const tipo = sanitizeText(payload.tipo).toUpperCase();
  if (!tipo) throw new Error('Selecione o tipo de comando.');
  const parametros = payload.parametros && typeof payload.parametros === 'object' && !Array.isArray(payload.parametros)
    ? payload.parametros
    : {};
  return {
    tipo,
    parametros,
    mensagem: sanitizeText(payload.mensagem),
    correlationId: sanitizeText(payload.correlationId) || createCorrelationId()
  };
}

export function parseSseBlock(block) {
  const event = { id: '', name: 'message', data: '' };
  String(block ?? '').split(/\r?\n/).forEach((line) => {
    if (!line || line.startsWith(':')) return;
    const separator = line.indexOf(':');
    const field = separator >= 0 ? line.slice(0, separator) : line;
    const rawValue = separator >= 0 ? line.slice(separator + 1) : '';
    const value = rawValue.startsWith(' ') ? rawValue.slice(1) : rawValue;
    if (field === 'id') event.id = value;
    if (field === 'event') event.name = value;
    if (field === 'data') event.data += `${event.data ? '\n' : ''}${value}`;
  });
  if (!event.data) return null;
  try {
    return { ...event, payload: JSON.parse(event.data) };
  } catch {
    return { ...event, payload: event.data };
  }
}

export async function subscribeControlRoom(onEvent, onState = () => {}, signal) {
  const session = readSession();
  if (!session?.token) throw new Error('Sessão não encontrada para abrir o canal em tempo real.');
  const baseApiUrl = String(getRuntimeConfig().baseApiUrl ?? '').replace(/\/+$/, '');
  const response = await fetch(`${baseApiUrl}/yard/control-room/stream`, {
    headers: {
      Accept: 'text/event-stream',
      Authorization: `Bearer ${session.token}`,
      'X-Correlation-Id': createCorrelationId()
    },
    cache: 'no-store',
    signal
  });
  if (!response.ok || !response.body) {
    throw new Error(`Não foi possível abrir o canal em tempo real (status ${response.status}).`);
  }
  onState('CONECTADO');
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const blocks = buffer.split(/\r?\n\r?\n/);
      buffer = blocks.pop() ?? '';
      blocks.map(parseSseBlock).filter(Boolean).forEach(onEvent);
    }
  } finally {
    reader.releaseLock();
    onState(signal?.aborted ? 'ENCERRADO' : 'DESCONECTADO');
  }
}

export const controlRoomApi = {
  obterResumo: () => request('/yard/control-room/resumo'),
  listarEquipamentos: (query) => request('/yard/control-room/equipamentos', { query: buildControlRoomQuery(query) }),
  listarHistorico: (equipamento, limite = 100) => request(`/yard/control-room/equipamentos/${encodeURIComponent(equipamento)}/historico`, { query: { limite } }),
  listarAlarmes: (query) => request('/yard/control-room/alarmes', { query: buildControlRoomQuery(query) }),
  reconhecerAlarme: (id) => request(`/yard/control-room/alarmes/${id}/reconhecer`, { method: 'PATCH' }),
  resolverAlarme: (id) => request(`/yard/control-room/alarmes/${id}/resolver`, { method: 'PATCH' }),
  listarComandos: (equipamento) => request('/yard/control-room/comandos', { query: buildControlRoomQuery({ equipamento }) }),
  criarComando: (equipamento, payload) => request(`/yard/control-room/equipamentos/${encodeURIComponent(equipamento)}/comandos`, {
    method: 'POST',
    body: normalizeCommandPayload(payload)
  }),
  listarIndisponibilidades: (equipamento) => request('/yard/control-room/indisponibilidades', { query: buildControlRoomQuery({ equipamento }) }),
  iniciarIndisponibilidade: (equipamento, payload) => request(`/yard/control-room/equipamentos/${encodeURIComponent(equipamento)}/indisponibilidades`, {
    method: 'POST',
    body: { motivo: sanitizeText(payload?.motivo), observacao: sanitizeText(payload?.observacao) }
  }),
  encerrarIndisponibilidade: (id, observacao = '') => request(`/yard/control-room/indisponibilidades/${id}/encerrar`, {
    method: 'PATCH',
    body: { observacao: sanitizeText(observacao) }
  }),
  listarDispositivos: () => request('/yard/control-room/dispositivos')
};
