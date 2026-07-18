import { request, sanitizeText } from './api.js';

const ORDER_STATUSES = new Set(['PENDENTE', 'EM_EXECUCAO', 'CONCLUIDA']);
const MOVEMENT_TYPES = new Set(['DESCARGA_TREM', 'CARGA_TREM']);

function positiveId(value, field) {
  const id = Number(value);
  if (!Number.isInteger(id) || id <= 0) {
    throw new TypeError(`${field} deve ser um número inteiro positivo.`);
  }
  return id;
}

function normalizeStatus(value, optional = false) {
  const status = sanitizeText(value).toUpperCase();
  if (!status && optional) return '';
  if (!ORDER_STATUSES.has(status)) {
    throw new TypeError('Status ferroviário inválido.');
  }
  return status;
}

function normalizeMovementType(value) {
  const type = sanitizeText(value).toUpperCase();
  if (!MOVEMENT_TYPES.has(type)) {
    throw new TypeError('Tipo de movimentação ferroviária inválido.');
  }
  return type;
}

function normalizeReplanning(payload) {
  const version = Number(payload?.versaoComposicao);
  if (!Number.isInteger(version) || version < 0) {
    throw new TypeError('A versão da composição é inválida.');
  }
  const reason = sanitizeText(payload?.motivo);
  if (!reason) {
    throw new TypeError('O motivo do replanejamento deve ser informado.');
  }
  return {
    codigoConteiner: sanitizeText(payload?.codigoConteiner).toUpperCase(),
    tipoMovimentacao: normalizeMovementType(payload?.tipoMovimentacao),
    vagaoOrigem: sanitizeText(payload?.vagaoOrigem).toUpperCase(),
    vagaoDestino: sanitizeText(payload?.vagaoDestino).toUpperCase(),
    versaoComposicao: version,
    motivo: reason
  };
}

export function railOrderTransitions(status) {
  const current = normalizeStatus(status);
  if (current === 'PENDENTE') return ['EM_EXECUCAO', 'CONCLUIDA'];
  if (current === 'EM_EXECUCAO') return ['CONCLUIDA'];
  return [];
}

export const railApi = {
  listarVisitas: (dias = 30) => request('/rail/ferrovia/visitas', { query: { dias } }),
  consultarVisita: (idVisita) => request(`/rail/ferrovia/visitas/${positiveId(idVisita, 'O identificador da visita')}`),
  listarOrdens: (idVisita, status = '') => request(
    `/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/ordens`,
    { query: normalizeStatus(status, true) ? { status: normalizeStatus(status) } : undefined }
  ),
  atualizarStatusOrdem: (idVisita, idOrdem, status) => request(
    `/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/ordens/${positiveId(idOrdem, 'O identificador da ordem')}/status`,
    { method: 'PATCH', body: { statusMovimentacao: normalizeStatus(status) } }
  ),
  replanejarConteiner: (idVisita, payload) => request(
    `/rail/ferrovia/visitas/${positiveId(idVisita, 'O identificador da visita')}/replanejamentos-conteiner`,
    { method: 'POST', body: normalizeReplanning(payload) }
  ),
  listarHistoricoReplanejamento: (idVisita) => request(
    `/rail/ferrovia/visitas/${positiveId(idVisita, 'O identificador da visita')}/replanejamentos-conteiner`
  ),
  registrarPartida: (idVisita) => request(
    `/rail/ferrovia/visitas/${positiveId(idVisita, 'O identificador da visita')}/partida`,
    { method: 'PATCH' }
  )
};
