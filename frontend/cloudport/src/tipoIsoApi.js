import { request } from './api.js';

const BASE = '/yard/tipos-iso';

export const tipoIsoApi = {
  listar: (query = {}) => request(BASE, { query }),
  detalhar: (id) => request(`${BASE}/${id}`),
  criar: (body) => request(BASE, { method: 'POST', body }),
  atualizar: (id, body) => request(`${BASE}/${id}`, { method: 'PUT', body }),
  alterarSituacao: (id, ativo, usuario) => request(`${BASE}/${id}/situacao`, { method: 'PATCH', body: { ativo, usuario } }),
  dependencias: (id) => request(`${BASE}/${id}/dependencias`)
};