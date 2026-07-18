import { request, sanitizeText } from './api.js';

function normalizeIdentifier(value) {
  const normalized = Number(value);
  return Number.isInteger(normalized) && normalized > 0 ? normalized : undefined;
}

function normalizeStatus(value) {
  const normalized = sanitizeText(value).toUpperCase();
  return normalized || undefined;
}

export function buildBillingQuery(filters = {}) {
  return {
    transportadoraId: normalizeIdentifier(filters.transportadoraId),
    status: normalizeStatus(filters.status)
  };
}

export function normalizeTariffPayload(payload = {}) {
  return {
    codigo: sanitizeText(payload.codigo).toUpperCase(),
    descricao: sanitizeText(payload.descricao),
    tipoOperacao: sanitizeText(payload.tipoOperacao).toUpperCase(),
    valor: Number(payload.valor),
    inicioVigencia: sanitizeText(payload.inicioVigencia),
    fimVigencia: sanitizeText(payload.fimVigencia) || null,
    ativa: payload.ativa !== false
  };
}

export function normalizeInvoicePayload(payload = {}) {
  return {
    transportadoraId: normalizeIdentifier(payload.transportadoraId),
    vencimento: sanitizeText(payload.vencimento),
    cobrancaIds: Array.isArray(payload.cobrancaIds)
      ? payload.cobrancaIds.map(normalizeIdentifier).filter(Boolean)
      : []
  };
}

export const billingCapApi = {
  listarTarifas: (ativas) => request('/billing/tarifas', { query: { ativas } }),
  salvarTarifa: (payload) => request('/billing/tarifas', {
    method: 'POST',
    body: normalizeTariffPayload(payload)
  }),
  gerarCobranca: (agendamentoId) => request(`/billing/cobrancas/agendamentos/${normalizeIdentifier(agendamentoId)}`, {
    method: 'POST'
  }),
  listarCobrancas: (filters = {}) => request('/billing/cobrancas', {
    query: buildBillingQuery(filters)
  }),
  gerarFatura: (payload) => request('/billing/faturas', {
    method: 'POST',
    body: normalizeInvoicePayload(payload)
  }),
  listarFaturas: (filters = {}) => request('/billing/faturas', {
    query: buildBillingQuery(filters)
  }),
  registrarPagamento: (faturaId, payload) => request(`/billing/faturas/${normalizeIdentifier(faturaId)}/pagamentos`, {
    method: 'POST',
    body: {
      valor: Number(payload.valor),
      forma: sanitizeText(payload.forma).toUpperCase(),
      referencia: sanitizeText(payload.referencia) || null,
      pagoEm: payload.pagoEm || null
    }
  }),
  consultarCap: () => request('/cap/resumo')
};
