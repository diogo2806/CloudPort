import { request, sanitizeText } from './api.js';

const ORDER_STATUSES = new Set(['PENDENTE', 'EM_EXECUCAO', 'CONCLUIDA']);

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
  registrarPartida: (idVisita) => request(
    `/rail/ferrovia/visitas/${positiveId(idVisita, 'O identificador da visita')}/partida`,
    { method: 'PATCH' }
  )
};
