import { request, sanitizeText } from './api.js';

export const dispatchApi = {
  obterResumo: () => request('/api/dispatch/resumo'),
  listarDecisoes: (limite = 50) => request('/api/dispatch/decisoes', { query: { limite } }),
  obterRanking: (workQueueId, equipamentoId) => request(`/api/dispatch/work-queues/${Number(workQueueId)}/ranking`, {
    query: equipamentoId ? { equipamentoId: Number(equipamentoId) } : {}
  }),
  autoDispatch: (payload) => request('/api/dispatch/auto-dispatch', {
    method: 'POST',
    body: {
      ...payload,
      workQueueId: Number(payload.workQueueId),
      equipamentoPatioId: Number(payload.equipamentoPatioId),
      ordemTrabalhoPatioId: payload.ordemTrabalhoPatioId ? Number(payload.ordemTrabalhoPatioId) : null,
      codigoUnidade: sanitizeText(payload.codigoUnidade),
      operador: sanitizeText(payload.operador),
      faseVisita: sanitizeText(payload.faseVisita).toUpperCase(),
      pow: sanitizeText(payload.pow),
      pool: sanitizeText(payload.pool),
      chaveIdempotencia: sanitizeText(payload.chaveIdempotencia),
      motivo: sanitizeText(payload.motivo),
      origemAcao: 'PORTAL_DISPATCH',
      correlationId: sanitizeText(payload.correlationId)
    }
  }),
  listarEtapas: (ordemId) => request(`/api/dispatch/work-instructions/${Number(ordemId)}/etapas`),
  avancarEtapa: (ordemId, tipo, payload) => request(`/api/dispatch/work-instructions/${Number(ordemId)}/etapas/${sanitizeText(tipo).toUpperCase()}`, {
    method: 'POST',
    body: payload
  }),
  listarConfiguracoes: () => request('/api/dispatch/configuracoes'),
  criarConfiguracao: (payload) => request('/api/dispatch/configuracoes', { method: 'POST', body: payload }),
  ativarConfiguracao: (id) => request(`/api/dispatch/configuracoes/${Number(id)}/ativar`, { method: 'POST' }),
  rollbackConfiguracao: (id, motivo) => request(`/api/dispatch/configuracoes/${Number(id)}/rollback`, {
    method: 'POST',
    query: { motivo: sanitizeText(motivo) }
  }),
  listarRotas: () => request('/api/dispatch/rotas'),
  criarRota: (payload) => request('/api/dispatch/rotas', { method: 'POST', body: payload })
};
