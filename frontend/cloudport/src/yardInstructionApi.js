import { request } from './api.js';

export const yardInstructionApi = {
  pesquisar: (query = {}) => request('/api/yard/work-instructions', { query }),
  obter: (id) => request(`/api/yard/work-instructions/${id}`),
  criar: (payload) => request('/api/yard/work-instructions', { method: 'POST', body: payload }),
  iniciar: (id) => request(`/api/yard/work-instructions/${id}/start`, { method: 'POST' }),
  concluir: (id) => request(`/api/yard/work-instructions/${id}/complete`, { method: 'POST' }),
  cancelar: (id, justificativa) => request(`/api/yard/work-instructions/${id}/cancel`, {
    method: 'POST',
    body: { justificativa }
  })
};
