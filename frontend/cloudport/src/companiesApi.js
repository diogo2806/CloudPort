import { request } from './api.js';

const BASE = '/api/carga-geral/empresas';

export const companiesApi = {
  listar: (query) => request(BASE, { query }),
  criar: (body) => request(BASE, { method: 'POST', body }),
  atualizar: (id, body) => request(`${BASE}/${encodeURIComponent(id)}`, { method: 'PUT', body }),
  atualizarStatus: (id, ativo) => request(`${BASE}/${encodeURIComponent(id)}/status`, { method: 'PATCH', query: { ativo } })
};
