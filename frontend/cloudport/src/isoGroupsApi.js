import { request } from './api.js';

const BASE = '/yard/inventario/canonico/grupos-iso';

export const isoGroupsApi = {
  listar: (filters = {}) => request(BASE, { query: filters }),
  detalhar: (id) => request(`${BASE}/${id}`),
  criar: (body) => request(BASE, { method: 'POST', body }),
  atualizar: (id, body) => request(`${BASE}/${id}`, { method: 'PUT', body }),
  alterarSituacao: (id, ativo) => request(`${BASE}/${id}/situacao`, { method: 'PATCH', query: { ativo } }),
  excluir: (id) => request(`${BASE}/${id}`, { method: 'DELETE' }),
  associarTipo: (tipoId, grupoIsoId) => request(`/yard/inventario/canonico/tipos/${tipoId}/grupo-iso/${grupoIsoId}`, { method: 'PUT' }),
  desassociarTipo: (tipoId) => request(`/yard/inventario/canonico/tipos/${tipoId}/grupo-iso`, { method: 'DELETE' })
};
