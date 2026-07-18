import { request, sanitizeText } from '../../api.js';

function commandBody(reason, extra = {}) {
  const motivo = sanitizeText(reason);
  if (!motivo) throw new Error('O motivo operacional é obrigatório.');
  return { ...extra, motivo };
}

function destinationBody(destination) {
  return {
    linhaDestino: destination.linha,
    colunaDestino: destination.coluna,
    camadaDestino: destination.camadaOperacional
  };
}

export const yardOperationalApi = {
  movimentarConteiner: (conteinerId, destino, reason) => request(`/yard/patio/conteineres/${conteinerId}/movimentar`, {
    method: 'POST',
    body: commandBody(reason, destinationBody(destino))
  }),
  replanejarAllocation: (workInstructionId, destino, reason) => request(`/yard/patio/work-instructions/${workInstructionId}/allocation`, {
    method: 'PATCH',
    body: commandBody(reason, destinationBody(destino))
  }),
  atualizarRestricaoPilha: (posicaoId, restriction, reason) => request(`/yard/patio/posicoes/${posicaoId}/restricao-pilha`, {
    method: 'PATCH',
    body: commandBody(reason, restriction)
  }),
  listarTelemetriaReefers: () => request('/yard/patio/reefers/telemetria'),
  registrarTelemetriaReefer: (conteinerId, telemetry) => request(`/yard/patio/reefers/telemetria/${conteinerId}`, {
    method: 'PUT',
    body: telemetry
  })
};
