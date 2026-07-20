import { request } from './api.js';

const BASE = '/api/carga-geral';
const INTERMODAL = `${BASE}/intermodal`;
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
  registrarAvaria: (body) => request(`${INTERMODAL}/avarias`, { method: 'POST', body }),
  listarAvarias: (loteId) => request(`${INTERMODAL}/lotes/${encodeURIComponent(loteId)}/avarias`),
  inspecionarAvaria: (id, body) => request(`${INTERMODAL}/avarias/${encodeURIComponent(id)}/inspecionar`, { method: 'POST', body }),
  encerrarAvaria: (id, body) => request(`${INTERMODAL}/avarias/${encodeURIComponent(id)}/encerrar`, { method: 'POST', body }),
  listarInventarios: () => request(`${INTERMODAL}/inventarios`),
  obterInventario: (id) => request(`${INTERMODAL}/inventarios/${encodeURIComponent(id)}`),
  abrirInventario: (body) => request(`${INTERMODAL}/inventarios`, { method: 'POST', body }),
  registrarContagemInventario: (id, body) => request(`${INTERMODAL}/inventarios/${encodeURIComponent(id)}/contagens`, { method: 'POST', body }),
  enviarInventarioParaAprovacao: (id, usuario) => request(`${INTERMODAL}/inventarios/${encodeURIComponent(id)}/enviar-aprovacao`, {
    method: 'POST', query: { usuario }
  }),
  conciliarInventario: (id, body) => request(`${INTERMODAL}/inventarios/${encodeURIComponent(id)}/conciliar`, { method: 'POST', body }),
  listarReferencias: (categoria) => request(`${BASE}/referencias`, { query: categoria ? { categoria } : undefined }),
  criarReferencia: (body) => request(`${BASE}/referencias`, { method: 'POST', body }),
  atualizarReferencia: (id, ativo) => request(`${BASE}/referencias/${encodeURIComponent(id)}/status`, { method: 'PATCH', query: { ativo } }),
  listarConteineresElegiveis: () => request(`${INVENTORY_RESERVATIONS}/elegiveis`),
  listarOperacoesStuffUnstuff: () => request(STUFF_UNSTUFF),
  obterOperacaoStuffUnstuff: (id) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}`),
  criarOperacaoStuffUnstuff: (body) => request(STUFF_UNSTUFF, { method: 'POST', body }),
  listarPlanosStuffUnstuff: (id) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/planos`),
  criarVersaoPlanoStuffUnstuff: (id, body) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/planos`, { method: 'POST', body }),
  liberarPlanoStuffUnstuff: (id, body) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/planos/liberar`, { method: 'POST', body }),
  listarProgramacoesDocaStuffUnstuff: (query) => request(`${STUFF_UNSTUFF}/programacoes-doca`, { query }),
  obterProgramacaoDocaStuffUnstuff: (id) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/programacao-doca`),
  reservarProgramacaoDocaStuffUnstuff: (id, body) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/programacao-doca`, { method: 'POST', body }),
  cancelarProgramacaoDocaStuffUnstuff: (id, body) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/programacao-doca/cancelar`, { method: 'POST', body }),
  iniciarOperacaoStuffUnstuff: (id, usuario, correlationId) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/iniciar`, {
    method: 'POST', query: { usuario, correlationId }
  }),
  registrarExecucaoStuffUnstuff: (id, body) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/execucoes`, { method: 'POST', body }),
  listarLacresStuffUnstuff: (id) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/lacres`),
  registrarLacreStuffUnstuff: (id, body) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/lacres`, { method: 'POST', body }),
  obterPesagemStuffing: (id) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/pesagem`),
  confirmarPesagemStuffing: (id, body) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/pesagem`, { method: 'POST', body }),
  concluirOperacaoStuffUnstuff: (id, body) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/concluir`, { method: 'POST', body }),
  cancelarOperacaoStuffUnstuff: (id, body) => request(`${STUFF_UNSTUFF}/${encodeURIComponent(id)}/cancelar`, { method: 'POST', body })
};
