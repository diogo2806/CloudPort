import { request, sanitizeText } from '../../api.js';

function commandBody(reason, extra = {}) {
  const motivo = sanitizeText(reason);
  if (!motivo) throw new Error('O motivo operacional é obrigatório.');
  return { ...extra, motivo };
}

export const yardOperationalApi = {
  movimentarConteiner: (conteinerId, destino, reason) => request(`/yard/patio/conteineres/${conteinerId}/movimentar`, {
    method: 'POST',
    body: commandBody(reason, {
      linhaDestino: destino.linha,
      colunaDestino: destino.coluna,
      camadaDestino: destino.camadaOperacional
    })
  }),
  atualizarRestricaoPilha: (posicaoId, restriction, reason) => request(`/yard/patio/posicoes/${posicaoId}/restricao-pilha`, {
    method: 'PATCH',
    body: commandBody(reason, restriction)
  })
};
