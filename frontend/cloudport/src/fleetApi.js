import { request } from './api.js';

const BASE = '/gate/frota/veiculos';

export const fleetApi = {
  listar: (query) => request(BASE, { query }),
  listarElegiveis: (transportadoraId) => request(`${BASE}/elegiveis`, { query: { transportadoraId } }),
  criar: (body) => request(BASE, { method: 'POST', body }),
  atualizar: (id, body) => request(`${BASE}/${encodeURIComponent(id)}`, { method: 'PUT', body }),
  atualizarStatus: (id, ativo) => request(`${BASE}/${encodeURIComponent(id)}/status`, { method: 'PATCH', query: { ativo } })
};
