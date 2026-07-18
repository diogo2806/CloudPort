import { readSession } from './api.js';

let baseApiUrlPromise;

function normalizeBaseUrl(value) {
  return String(value ?? '').replace(/\/+$/, '');
}

async function resolveBaseApiUrl() {
  if (!baseApiUrlPromise) {
    baseApiUrlPromise = fetch('/assets/configuracao.json', { cache: 'no-store' })
      .then((response) => response.ok ? response.json() : {})
      .then((config) => normalizeBaseUrl(config?.baseApiUrl))
      .catch(() => '');
  }
  return baseApiUrlPromise;
}

function positiveId(value, label) {
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed <= 0) {
    throw new TypeError(`${label} deve ser um identificador positivo.`);
  }
  return parsed;
}

function correlationId() {
  return globalThis.crypto?.randomUUID?.()
    ?? `navio-admin-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function requireReason(value, label) {
  let reason = String(value ?? '').trim();
  if (!reason && typeof window !== 'undefined' && typeof window.prompt === 'function') {
    reason = String(window.prompt(label, '') ?? '').trim();
  }
  if (!reason) throw new Error('O motivo da operação administrativa é obrigatório.');
  return reason;
}

async function request(path, { method = 'PATCH', body } = {}) {
  const session = readSession();
  if (!session?.token) throw new Error('Sessão expirada. Entre novamente para executar a operação.');
  const requestCorrelationId = correlationId();
  const commandBody = body && typeof body === 'object' && !Array.isArray(body)
    ? {
        ...body,
        usuario: body.usuario ?? session.nome,
        origemAcao: body.origemAcao ?? 'CONTROL_ROOM_NAVIO_ADMIN',
        correlationId: body.correlationId ?? requestCorrelationId
      }
    : body;
  const response = await fetch(`${await resolveBaseApiUrl()}${path}`, {
    method,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: `Bearer ${session.token}`,
      'X-Correlation-Id': requestCorrelationId
    },
    body: commandBody === undefined ? undefined : JSON.stringify(commandBody)
  });
  const contentType = response.headers.get('content-type') ?? '';
  const payload = contentType.includes('application/json')
    ? await response.json()
    : await response.text();
  if (!response.ok) {
    const error = new Error(payload?.mensagem ?? payload?.erro ?? payload?.message ?? `Falha HTTP ${response.status}`);
    error.payload = payload;
    error.status = response.status;
    throw error;
  }
  return payload;
}

function motivated(reason, label) {
  return { motivo: requireReason(reason, label) };
}

export const navioAdministrativeApi = {
  cancelarVisita: (visitaId, motivo) => request(
    `/visitas-navio/${positiveId(visitaId, 'visitaId')}/cancelar`,
    { body: motivated(motivo, 'Informe o motivo do cancelamento da visita:') }
  ),
  cancelarItem: (visitaId, itemId, motivo) => request(
    `/visitas-navio/${positiveId(visitaId, 'visitaId')}/itens/${positiveId(itemId, 'itemId')}/cancelar`,
    { body: motivated(motivo, 'Informe o motivo do cancelamento do item:') }
  ),
  publicarPlano: (visitaId, planoId, motivo) => request(
    `/visitas-navio/${positiveId(visitaId, 'visitaId')}/plano-estiva/${positiveId(planoId, 'planoId')}/publicar`,
    { method: 'POST', body: motivated(motivo, 'Informe o motivo da publicação do plano:') }
  ),
  invalidarPlano: (visitaId, planoId, motivo) => request(
    `/visitas-navio/${positiveId(visitaId, 'visitaId')}/plano-estiva/${positiveId(planoId, 'planoId')}/invalidar`,
    { body: motivated(motivo, 'Informe o motivo da invalidação do plano:') }
  ),
  cancelarPlano: (visitaId, planoId, motivo) => request(
    `/visitas-navio/${positiveId(visitaId, 'visitaId')}/plano-estiva/${positiveId(planoId, 'planoId')}/cancelar`,
    { body: motivated(motivo, 'Informe o motivo do cancelamento do plano:') }
  ),
  criarNovaVersaoPlano: (visitaId, posicoes = []) => request(
    `/visitas-navio/${positiveId(visitaId, 'visitaId')}/plano-estiva`,
    { method: 'POST', body: { posicoes: Array.isArray(posicoes) ? posicoes : [] } }
  ),
  cancelarOrdem: (visitaId, ordemId, motivo) => request(
    `/visitas-navio/${positiveId(visitaId, 'visitaId')}/integracao-patio/ordens/${positiveId(ordemId, 'ordemId')}/cancelar`,
    { body: motivated(motivo, 'Informe o motivo do cancelamento da ordem de pátio:') }
  )
};
