import { request, sanitizeText } from './api.js';

export const yardPredictiveApi = {
  listarPlanos: (query = {}) => request('/api/scheduler/planos-posicao', { query }),
  listarHistorico: (planoId) => request(`/api/scheduler/planos-posicao/${Number(planoId)}/historico`),
  alterarEstado: (planoId, estadoDestino, motivo, operador) => request(`/api/scheduler/planos-posicao/${Number(planoId)}/estado`, {
    method: 'POST',
    body: {
      estadoDestino: sanitizeText(estadoDestino).toUpperCase(),
      motivo: sanitizeText(motivo),
      operador: sanitizeText(operador) || 'operador'
    }
  }),
  obterYardImpact: (horizonteHoras = 6) => request('/api/scheduler/yard-impact', {
    query: { horizonteHoras: Number(horizonteHoras) }
  })
};
