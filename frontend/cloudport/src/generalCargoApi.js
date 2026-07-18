import { request } from './api.js';

const BASE = '/api/carga-geral';
const OPERATIONS = `${BASE}/operacoes-stuff-unstuff`;

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
  listarOperacoesStuffUnstuff: () => request(OPERATIONS),
  obterOperacaoStuffUnstuff: (id) => request(`${OPERATIONS}/${encodeURIComponent(id)}`),
  criarOperacaoStuffUnstuff: (body) => request(OPERATIONS, { method: 'POST', body }),
  iniciarOperacaoStuffUnstuff: (id) => request(`${OPERATIONS}/${encodeURIComponent(id)}/iniciar`, { method: 'POST' }),
  registrarExecucaoStuffUnstuff: (id, body) => request(`${OPERATIONS}/${encodeURIComponent(id)}/execucoes`, { method: 'POST', body }),
  concluirOperacaoStuffUnstuff: (id, body) => request(`${OPERATIONS}/${encodeURIComponent(id)}/concluir`, { method: 'POST', body }),
  cancelarOperacaoStuffUnstuff: (id, body) => request(`${OPERATIONS}/${encodeURIComponent(id)}/cancelar`, { method: 'POST', body })
};
