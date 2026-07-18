import { readSession } from './api.js';

let configuredBaseUrl;
let baseApiUrlPromise;

function normalizeBaseUrl(value) {
  return String(value ?? '').replace(/\/+$/, '');
}

async function resolveBaseApiUrl() {
  if (configuredBaseUrl !== undefined) return configuredBaseUrl;
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
    ?? `navio-replan-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function requiredReason(value) {
  const reason = String(value ?? '').trim();
  if (!reason) throw new Error('O motivo do replanejamento é obrigatório.');
  return reason;
}

async function request(visitaId, aplicar, motivo, options = {}) {
  const id = positiveId(visitaId, 'visitaId');
  const session = readSession();
  if (!session?.token) throw new Error('Sessão expirada. Entre novamente para replanejar.');
  const requestCorrelationId = correlationId();
  const response = await fetch(`${await resolveBaseApiUrl()}/visitas-navio/${id}/integracao-patio/replanejar`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: `Bearer ${session.token}`,
      'X-Correlation-Id': requestCorrelationId
    },
    body: JSON.stringify({
      aplicar: Boolean(aplicar),
      usuario: session.nome,
      motivo: requiredReason(motivo),
      correlationId: requestCorrelationId,
      limiteRehandleAceitavel: options.limiteRehandleAceitavel ?? null,
      pesosCriterios: options.pesosCriterios ?? {}
    })
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

export function setReplanningBaseUrlForTests(value) {
  configuredBaseUrl = normalizeBaseUrl(value);
  baseApiUrlPromise = undefined;
}

export const replanejamentoRealApi = {
  simular: (visitaId, motivo, options) => request(visitaId, false, motivo, options),
  aplicar: (visitaId, motivo, options) => request(visitaId, true, motivo, options)
};
