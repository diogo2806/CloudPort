import { readSession, request } from '../../api.js';

export const yardGeometryApi = {
  listar: () => request('/yard/patio/geometrias'),
  criar: (payload) => request('/yard/patio/geometrias', { method: 'POST', body: payload }),
  atualizar: (id, payload) => request(`/yard/patio/geometrias/${id}`, { method: 'PUT', body: payload }),
  excluir: (id, motivo) => request(`/yard/patio/geometrias/${id}`, {
    method: 'DELETE',
    body: {
      motivo,
      usuario: readSession()?.nome ?? 'operador'
    }
  })
};
