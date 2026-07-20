import { createCorrelationId, request, sanitizeText } from './api.js';

function command(payload = {}) {
  return {
    codigoUnidade: sanitizeText(payload.codigoUnidade),
    area: sanitizeText(payload.area),
    posicao: sanitizeText(payload.posicao),
    equipamento: sanitizeText(payload.equipamento),
    operador: sanitizeText(payload.operador),
    condicao: sanitizeText(payload.condicao),
    lacres: sanitizeText(payload.lacres),
    chaveIdempotencia: sanitizeText(payload.chaveIdempotencia) || createCorrelationId()
  };
}

export const yardCustodyApi = {
  listar: (status) => request('/yard/patio/exchange-areas/custodias', {
    query: status ? { status: sanitizeText(status).toUpperCase() } : undefined
  }),
  entregar: (payload) => request('/yard/patio/exchange-areas/custodias/entregas', {
    method: 'POST',
    body: command(payload)
  }),
  receber: (custodiaId, payload) => request(`/yard/patio/exchange-areas/custodias/${Number(custodiaId)}/recebimentos`, {
    method: 'POST',
    body: command(payload)
  })
};
