import { request } from './api.js';

const BASE = '/api/yard/stowage-warnings';

export const yardStowageWarningApi = {
  listar: (query = {}) => request(BASE, { query }),
  resumo: () => request(`${BASE}/summary`),
  historico: (id) => request(`${BASE}/${id}/history`),
  varrer: (ator) => request(`${BASE}/scan`, { method: 'POST', body: { ator } }),
  revalidarUnidade: (codigoUnidade, ator) => request(`${BASE}/unit/${encodeURIComponent(codigoUnidade)}/revalidate`, {
    method: 'POST',
    body: { ator }
  }),
  atribuir: (id, payload) => request(`${BASE}/${id}/assign`, { method: 'POST', body: payload }),
  iniciarCorrecao: (id, payload) => request(`${BASE}/${id}/start-correction`, { method: 'POST', body: payload }),
  enviarRevalidacao: (id, payload) => request(`${BASE}/${id}/submit-revalidation`, { method: 'POST', body: payload }),
  revalidar: (id, payload) => request(`${BASE}/${id}/revalidate`, { method: 'POST', body: payload })
};
