import { loadRuntimeConfig, readSession } from './api.js';

/**
 * @typedef {Object} ReconciliacaoBarcode
 * @property {number} id
 * @property {number} gatePassId
 * @property {string} codigoGatePass
 * @property {string} tipoDesinconia
 * @property {string} descricao
 * @property {string|null} barcodeEsperado
 * @property {string|null} barcodeRecebido
 * @property {string|null} statusTos
 * @property {string|null} statusLocal
 * @property {number|null} tempoPendenciaHoras
 * @property {string} detectadoEm
 * @property {string|null} resolvidoEm
 * @property {boolean} alertaEnviado
 */

export const TIPOS_DESINCRONIA = [
  'CONTAINER_PRESO',
  'BARCODE_NAO_CONFIRMADO',
  'BARCODE_MISMATCH',
  'TIMEOUT_NAO_RESOLVIDO',
  'STATUS_INCONSISTENTE',
  'ENTRADA_SEM_SAIDA_24H',
  'DISCREPANCIA_TEMPORAL',
  'SAIDA_SEM_ENTRADA',
  'MULTIPLOS_CONTAINERS_PLACA',
  'TEMPO_GATE_EXCEDIDO'
];

let configPromise;

function correlationId() {
  return globalThis.crypto?.randomUUID?.() ?? `reconciliacao-${Date.now()}`;
}

async function runtimeConfig() {
  if (!configPromise) configPromise = loadRuntimeConfig();
  return configPromise;
}

async function request(path, options = {}) {
  const [config, session] = await Promise.all([runtimeConfig(), Promise.resolve(readSession())]);
  if (!session?.token) {
    const error = new Error('A sessão expirou. Entre novamente.');
    error.status = 401;
    throw error;
  }
  const headers = new Headers(options.headers ?? {});
  headers.set('Accept', 'application/json');
  headers.set('Authorization', `Bearer ${session.token}`);
  headers.set('X-Correlation-Id', correlationId());
  if (options.body !== undefined) headers.set('Content-Type', 'application/json');

  const response = await fetch(`${config.baseApiUrl}${path}`, {
    ...options,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  });
  if (response.status === 204) return null;
  const contentType = response.headers.get('content-type') ?? '';
  const payload = contentType.includes('application/json') ? await response.json() : await response.text();
  if (!response.ok) {
    const error = new Error(payload?.mensagem ?? payload?.message ?? payload?.erro ?? `Falha HTTP ${response.status}`);
    error.status = response.status;
    error.payload = payload;
    throw error;
  }
  return payload;
}

export const reconciliacaoBarcodeApi = {
  /** @returns {Promise<ReconciliacaoBarcode[]>} */
  listarNaoResolvidas: () => request('/gate/reconciliacao/nao-resolvidas'),

  /** @param {string} tipo @returns {Promise<ReconciliacaoBarcode[]>} */
  listarPorTipo: (tipo) => request(`/gate/reconciliacao/por-tipo?tipo=${encodeURIComponent(tipo)}`),

  /** @param {number} id @param {string} resolucao @returns {Promise<null>} */
  resolver: (id, resolucao) => request(`/gate/reconciliacao/${Number(id)}/resolver`, {
    method: 'PUT',
    body: { resolucao: String(resolucao ?? '').trim() }
  })
};
