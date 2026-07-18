import { request } from './api.js';

export const inventoryLostFoundApi = {
  listar: () => request('/api/inventario/casos'),
  abrir: (payload) => request('/api/inventario/casos', { method: 'POST', body: payload }),
  investigar: (id, payload) => request(`/api/inventario/casos/${id}/investigate`, { method: 'POST', body: payload }),
  associar: (id, payload) => request(`/api/inventario/casos/${id}/associate`, { method: 'POST', body: payload }),
  regularizar: (id, decisao) => request(`/api/inventario/casos/${id}/regularize`, { method: 'POST', body: { decisao } }),
  baixar: (id, decisao) => request(`/api/inventario/casos/${id}/write-off`, { method: 'POST', body: { decisao } }),
  encerrar: (id, decisao) => request(`/api/inventario/casos/${id}/close`, { method: 'POST', body: { decisao } })
};
