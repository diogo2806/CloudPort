import { request, sanitizeText } from './api.js';

const PLANOS_POSICAO_404_COOLDOWN_MS = 60_000;
let planosPosicaoIndisponivelAte = 0;

async function listarPlanos(query = {}) {
  if (Date.now() < planosPosicaoIndisponivelAte) return [];

  try {
    return await request('/api/scheduler/planos-posicao', { query });
  } catch (error) {
    if (error?.status === 404) {
      planosPosicaoIndisponivelAte = Date.now() + PLANOS_POSICAO_404_COOLDOWN_MS;
    }
    throw error;
  }
}

export const yardPredictiveApi = {
  listarPlanos,
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
