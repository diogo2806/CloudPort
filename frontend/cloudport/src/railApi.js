import { request } from './api.js';

export const railApi = {
  listarVisitas: (dias = 30) => request('/rail/ferrovia/visitas', { query: { dias } }),
  listarOrdens: (visitaId, status) => request(`/rail/ferrovia/lista-trabalho/visitas/${visitaId}/ordens`, {
    query: status ? { status } : undefined
  }),
  atualizarStatusOrdem: (visitaId, ordemId, statusMovimentacao) => request(
    `/rail/ferrovia/lista-trabalho/visitas/${visitaId}/ordens/${ordemId}/status`,
    { method: 'PATCH', body: { statusMovimentacao } }
  )
};
