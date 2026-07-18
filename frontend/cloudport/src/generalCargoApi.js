import { request } from './api.js';

const BASE = '/api/carga-geral';
const INTERMODAL = `${BASE}/operacoes-intermodais`;
const STUFF_UNSTUFF = `${BASE}/operacoes-stuff-unstuff`;
const INVENTORY_RESERVATIONS = '/yard/inventario/canonico/reservas-carga-geral';

export const generalCargoApi = {
  dashboard: () => request(`${BASE}/dashboard`),
  listarConhecimentos: () => request(`${BASE}/conhecimentos`),
  criarConhecimento: (body) => request(`${BASE}/conhecimentos`, { method: 'POST', body }),
  obterConhecimento: (id) => request(`${BASE}/conhecimentos/${encodeURIComponent(id)}`),
  adicionarItem: (conhecimentoId, body) => request(`${BASE}/conhecimentos/${encodeURIComponent(conhecimentoId)}/itens`, { method: 'POST', body }),
  adicionarLote: (itemId, body) => request(`${BASE}/itens/${encodeURIComponent(itemId)}/lotes`, { method: 'POST', body }),
  listarLotes: (query) => request(`${BASE}/lotes`, { query }),
  obterLote: (id) => request(`${BASE}/lotes/${encodeURIComponent(id)}`),
  registrarMovimentacao: (id, body) => request(`${BASE}/lotes/${encodeURIComponent(id)}/movimentacoes`, { method: 'POST', body }),
  abrirAvaria: (body) => request(`${INTERMODAL}/avarias`, { method: 'POST', body }),
  listarAvarias: (loteId) => request(`${INTERMODAL}/avarias`, { query: { loteId } }),
  adicionarEvidenciaAvaria: (id, body) => request(`${INTERMODAL}/avarias/${encodeURIComponent(id)}/evidencias`, { method: 'POST', body }),
  transicionarAvaria: (id, body) => request(`${INTERMODAL}/avarias/${encodeURIComponent(id)}/transicoes`, { method: 'POST', body }),
  listarReferencias: (categoria) => request(`${BASE}/referencias`, { query: categoria ? { categoria } : undefined }),
  criarReferencia: (body) => request(`${BASE}/referencias`, { method: 'POST', body }),
  atualizarReferencia: (id, ativo) => request(`${BASE}/referencias/${encodeURIComponent(id)}/status`, { method: 'PATCH', query: { ativo } }),
  listarConteineresElegiveis: () => request(`${INVENTORY_RESERVATIONS}/elegiveis`),
  listarOperacoesStuffUnstuff: () => request(STUFF_UNSTUFF),
  obterOperacaoStuffUnstuff: (id) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}`),
  criarOperacaoStuffUnstuff: (body) => request(STUFF_UNSTUFF, { method: 'POST', body }),
  iniciarOperacaoStuffUnstuff: (id, usuario, correlationId) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/iniciar`, {
    method: 'POST', query: { usuario, correlationId }
  }),
  registrarExecucaoStuffUnstuff: (id, body) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/execucoes`, { method: 'POST', body }),
  concluirOperacaoStuffUnstuff: (id, body) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/concluir`, { method: 'POST', body }),
  cancelarOperacaoStuffUnstuff: (id, body) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/cancelar`, { method: 'POST', body })
};
