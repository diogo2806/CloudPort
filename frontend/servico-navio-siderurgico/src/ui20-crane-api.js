import { loadRuntimeConfig, readSession } from './api.js';

let baseApiUrlPromise;

function normalize(value) {
  return String(value ?? '').replace(/\/+$/, '');
}

function correlationId() {
  return globalThis.crypto?.randomUUID?.() ?? `ui20-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function traceId() {
  const bytes = new Uint8Array(16);
  if (globalThis.crypto?.getRandomValues) globalThis.crypto.getRandomValues(bytes);
  else for (let index = 0; index < bytes.length; index += 1) bytes[index] = Math.floor(Math.random() * 256);
  return Array.from(bytes, (value) => value.toString(16).padStart(2, '0')).join('');
}

async function baseApiUrl() {
  if (!baseApiUrlPromise) baseApiUrlPromise = loadRuntimeConfig().then((config) => normalize(config.baseApiUrl));
  return baseApiUrlPromise;
}

export function setCraneBaseUrlForTests(value) {
  baseApiUrlPromise = Promise.resolve(normalize(value));
}

async function request(visitaId, method, body) {
  const session = readSession();
  const correlation = correlationId();
  const trace = traceId();
  const headers = new Headers({ Accept: 'application/json' });
  if (session?.token) {
    headers.set('Authorization', `Bearer ${session.token}`);
    headers.set('X-Correlation-Id', correlation);
    headers.set('X-Trace-Id', trace);
    headers.set('traceparent', `00-${trace}-${trace.slice(0, 16)}-01`);
  }
  let payload = body;
  if (body !== undefined) {
    headers.set('Content-Type', 'application/json');
    payload = JSON.stringify({ ...body, usuario: body.usuario ?? session?.nome ?? 'operador' });
  }
  const response = await fetch(`${await baseApiUrl()}/visitas-navio/${visitaId}/crane-plan`, { method, headers, body: payload });
  const result = await response.json().catch(() => null);
  if (!response.ok) {
    const error = new Error(result?.mensagem ?? result?.message ?? `Falha HTTP ${response.status}`);
    error.payload = result;
    error.status = response.status;
    throw error;
  }
  return result;
}

export const craneApi = {
  obterPlanoGuindaste: (visitaId) => request(visitaId, 'GET'),
  salvarPlanoGuindaste: (visitaId, plano) => request(visitaId, 'POST', plano)
};
