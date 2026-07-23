import { sanitizeText } from './api.js';

export const EDI_MESSAGE_TYPES = ['BAPLIE', 'COPRAR', 'COARRI', 'VERMAS'];

export const EDI_PROCESSING_STATUSES = [
  'RECEBIDO',
  'PROCESSANDO',
  'AGUARDANDO_REPROCESSAMENTO',
  'CONCLUIDO',
  'REJEITADO',
  'QUARENTENA',
  'CANCELADO'
];

export const EDI_STATUS_LABELS = {
  RECEBIDO: 'Recebido',
  PROCESSANDO: 'Processando',
  AGUARDANDO_REPROCESSAMENTO: 'Aguardando reprocessamento',
  CONCLUIDO: 'Concluído',
  REJEITADO: 'Rejeitado',
  QUARENTENA: 'Quarentena',
  CANCELADO: 'Cancelado'
};

const REPROCESSABLE_STATUSES = new Set(['REJEITADO', 'QUARENTENA', 'AGUARDANDO_REPROCESSAMENTO']);
const QUARANTINABLE_STATUSES = new Set(['RECEBIDO', 'PROCESSANDO', 'AGUARDANDO_REPROCESSAMENTO', 'REJEITADO']);
const CANCELLABLE_STATUSES = new Set(['RECEBIDO', 'PROCESSANDO', 'AGUARDANDO_REPROCESSAMENTO', 'REJEITADO', 'QUARENTENA']);

export function normalizeEdiFilters(filters = {}) {
  const tipo = EDI_MESSAGE_TYPES.includes(filters.tipo) ? filters.tipo : '';
  const status = EDI_PROCESSING_STATUSES.includes(filters.status) ? filters.status : '';
  const pagina = Number.isInteger(Number(filters.pagina)) && Number(filters.pagina) >= 0 ? Number(filters.pagina) : 0;
  const rawSize = Number(filters.tamanho);
  const tamanho = Number.isInteger(rawSize) && rawSize >= 1 && rawSize <= 200 ? rawSize : 50;
  return { tipo, status, pagina, tamanho };
}

export function buildEdiListQuery(filters = {}) {
  const { tipo, status, pagina, tamanho } = normalizeEdiFilters(filters);
  const query = { pagina, tamanho };
  if (tipo) query.tipo = tipo;
  if (status) query.status = status;
  return query;
}

export function normalizeEdiProcessing(row) {
  if (!row || typeof row !== 'object') return null;
  const id = Number(row.id);
  if (!Number.isInteger(id) || id <= 0) return null;
  const status = sanitizeText(row.status).toUpperCase();
  return {
    id,
    tipoMensagem: sanitizeText(row.tipoMensagem).toUpperCase(),
    status,
    statusDescricao: EDI_STATUS_LABELS[status] ?? status ?? '—',
    codigoNavio: sanitizeText(row.codigoNavio),
    codigoViagem: sanitizeText(row.codigoViagem),
    referenciaMensagem: sanitizeText(row.referenciaMensagem),
    identificadorInterchange: sanitizeText(row.identificadorInterchange),
    identificadorMensagem: sanitizeText(row.identificadorMensagem),
    chaveIdempotencia: sanitizeText(row.chaveIdempotencia),
    correlationId: sanitizeText(row.correlationId),
    motivoRejeicao: sanitizeText(row.motivoRejeicao),
    motivoReprocessamento: sanitizeText(row.motivoReprocessamento),
    usuarioReprocessamento: sanitizeText(row.usuarioReprocessamento),
    reprocessamentoDeId: row.reprocessamentoDeId ?? null,
    tentativa: Number.isInteger(Number(row.tentativa)) ? Number(row.tentativa) : null,
    proximaTentativaEm: row.proximaTentativaEm ?? null,
    bayPlanId: row.bayPlanId ?? null,
    criadoEm: row.criadoEm ?? null,
    atualizadoEm: row.atualizadoEm ?? null,
    reprocessavel: REPROCESSABLE_STATUSES.has(status),
    quarentenavel: QUARANTINABLE_STATUSES.has(status),
    cancelavel: CANCELLABLE_STATUSES.has(status)
  };
}

export function normalizeEdiPage(payload) {
  const rows = (Array.isArray(payload?.conteudo) ? payload.conteudo : Array.isArray(payload) ? payload : [])
    .map(normalizeEdiProcessing)
    .filter(Boolean);
  return {
    rows,
    pagina: Number.isInteger(payload?.pagina) ? payload.pagina : 0,
    tamanho: Number.isInteger(payload?.tamanho) ? payload.tamanho : rows.length,
    totalElementos: Number.isInteger(Number(payload?.totalElementos)) ? Number(payload.totalElementos) : rows.length,
    totalPaginas: Number.isInteger(payload?.totalPaginas) ? payload.totalPaginas : 1,
    primeira: payload?.primeira !== false,
    ultima: payload?.ultima !== false
  };
}

export function summarizeEdiProcessings(rows = []) {
  const summary = { total: 0, concluidos: 0, rejeitados: 0, quarentena: 0, cancelados: 0, emProcessamento: 0, aguardandoReprocessamento: 0 };
  rows.forEach((row) => {
    if (!row) return;
    summary.total += 1;
    if (row.status === 'CONCLUIDO') summary.concluidos += 1;
    else if (row.status === 'REJEITADO') summary.rejeitados += 1;
    else if (row.status === 'QUARENTENA') summary.quarentena += 1;
    else if (row.status === 'CANCELADO') summary.cancelados += 1;
    else if (row.status === 'AGUARDANDO_REPROCESSAMENTO') summary.aguardandoReprocessamento += 1;
    else summary.emProcessamento += 1;
  });
  return summary;
}

export function validateReprocessReason(reason) {
  const motivo = sanitizeText(reason);
  if (motivo.length < 5) throw new Error('Informe um motivo operacional com pelo menos 5 caracteres.');
  return motivo;
}
