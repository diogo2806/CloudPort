import { request } from './api.js';

const BASE = '/api/carga-geral';
const STUFF_UNSTUFF = `${BASE}/operacoes-stuff-unstuff`;
const INTERMODAL_OPERATIONS = `${BASE}/operacoes-intermodais`;
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
  registrarAvaria: (id, body) => request(`${BASE}/lotes/${encodeURIComponent(id)}/avarias`, { method: 'POST', body }),
  listarReferencias: (categoria) => request(`${BASE}/referencias`, { query: categoria ? { categoria } : undefined }),
  criarReferencia: (body) => request(`${BASE}/referencias`, { method: 'POST', body }),
  atualizarReferencia: (id, ativo) => request(`${BASE}/referencias/${encodeURIComponent(id)}/status`, { method: 'PATCH', query: { ativo } }),
  registrarIdentificacao: (body) => request(`${INTERMODAL_OPERATIONS}/identificacoes`, { method: 'POST', body }),
  resolverIdentificacao: (codigo) => request(`${INTERMODAL_OPERATIONS}/identificacoes/resolver`, { query: { codigo } }),
  listarIdentificacoes: (loteId) => request(`${INTERMODAL_OPERATIONS}/identificacoes`, { query: { loteId } }),
  listarInventarios: () => request(`${INTERMODAL_OPERATIONS}/inventarios`),
  abrirInventario: (body) => request(`${INTERMODAL_OPERATIONS}/inventarios`, { method: 'POST', body }),
  registrarContagem: (inventarioId, body) => request(`${INTERMODAL_OPERATIONS}/inventarios/${encodeURIComponent(inventarioId)}/contagens`, { method: 'POST', body }),
  resolverDivergencia: (inventarioId, body) => request(`${INTERMODAL_OPERATIONS}/inventarios/${encodeURIComponent(inventarioId)}/divergencias`, { method: 'POST', body }),
  concluirInventario: (inventarioId, body) => request(`${INTERMODAL_OPERATIONS}/inventarios/${encodeURIComponent(inventarioId)}/concluir`, { method: 'POST', body }),
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
