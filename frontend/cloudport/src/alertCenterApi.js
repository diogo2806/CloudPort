import { request, sanitizeText } from './api.js';

const ALERTS_BASE_PATH = '/api/v1/visibilidade/alertas';

export const alertCenterApi = {
  listar: ({ status = 'ativo', severidade = [], tipo = [], page = 0, size = 50 } = {}) => request(`${ALERTS_BASE_PATH}/filtrados`, {
    query: {
      status: sanitizeText(status),
      severidade,
      tipo,
      page,
      size,
      sort: 'dataGerada,desc'
    }
  }),
  resumo: () => request(`${ALERTS_BASE_PATH}/resumo`),
  reconhecer: (id, usuario) => request(`${ALERTS_BASE_PATH}/${id}/reconhecer`, {
    method: 'PATCH',
    body: { usuario: sanitizeText(usuario) }
  }),
  resolver: (id, usuario) => request(`${ALERTS_BASE_PATH}/${id}/resolver`, {
    method: 'PATCH',
    body: { usuario: sanitizeText(usuario) }
  })
};
