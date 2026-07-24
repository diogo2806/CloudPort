import { request, sanitizeText } from './api.js';

function cleanQuery(query = {}) {
  return Object.fromEntries(Object.entries(query)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => [key, sanitizeText(value)]));
}

export const yardStowageWarningsApi = {
  listar: (query = {}) => request('/yard/patio/avisos-estivagem', { query: cleanQuery(query) }),
  historico: (id) => request(`/yard/patio/avisos-estivagem/${Number(id)}/historico`),
  revalidarInventario: () => request('/yard/patio/avisos-estivagem/revalidar', { method: 'POST' }),
  atribuir: (id, responsavel, prazo) => request(`/yard/patio/avisos-estivagem/${Number(id)}/atribuir`, {
    method: 'POST',
    body: { responsavel: sanitizeText(responsavel), prazo: prazo || null }
  }),
  iniciarCorrecao: (id, acaoCorretiva, evidencia) => request(`/yard/patio/avisos-estivagem/${Number(id)}/iniciar-correcao`, {
    method: 'POST',
    body: { acaoCorretiva: sanitizeText(acaoCorretiva), evidencia: sanitizeText(evidencia) || null }
  }),
  aguardarRevalidacao: (id, evidencia) => request(`/yard/patio/avisos-estivagem/${Number(id)}/aguardar-revalidacao`, {
    method: 'POST',
    body: { evidencia: sanitizeText(evidencia) || null }
  }),
  revalidar: (id, evidencia) => request(`/yard/patio/avisos-estivagem/${Number(id)}/revalidar`, {
    method: 'POST',
    body: { evidencia: sanitizeText(evidencia) || null }
  })
};
