import { request } from './api.js';

export const gateOperationsApi = {
  obterPainel: (facilityId) => request('/gate/operacional/painel', { query: { facilityId } }),
  listarReferencias: () => request('/gate/operacional/referencias'),
  criarVisita: (payload) => request('/gate/operacional/visitas', { method: 'POST', body: payload }),
  buscarVisita: (visitaId) => request(`/gate/operacional/visitas/${visitaId}`),
  avancarVisita: (visitaId, payload) => request(`/gate/operacional/visitas/${visitaId}/avancar`, { method: 'POST', body: payload }),
  abrirTrouble: (transactionId, payload) => request(`/gate/operacional/transacoes/${transactionId}/troubles`, { method: 'POST', body: payload }),
  resolverTrouble: (troubleId, payload) => request(`/gate/operacional/troubles/${troubleId}/resolver`, { method: 'POST', body: payload }),
  registrarInspecao: (transactionId, payload) => request(`/gate/operacional/transacoes/${transactionId}/inspecoes`, { method: 'POST', body: payload }),
  anexar: (payload) => request('/gate/operacional/anexos', { method: 'POST', body: payload }),
  emitirDocumento: (visitaId, payload) => request(`/gate/operacional/visitas/${visitaId}/documentos`, { method: 'POST', body: payload }),
  reimprimirDocumento: (documentoId) => request(`/gate/operacional/documentos/${documentoId}/reimprimir`, { method: 'POST' }),
  solicitarTransferencia: (visitaId, payload) => request(`/gate/operacional/visitas/${visitaId}/transferencias`, { method: 'POST', body: payload }),
  receberTransferencia: (transferenciaId) => request(`/gate/operacional/transferencias/${transferenciaId}/receber`, { method: 'POST' }),
  salvarFacility: (payload) => request('/gate/operacional/configuracao/facilities', { method: 'POST', body: payload }),
  salvarGate: (payload) => request('/gate/operacional/configuracao/gates', { method: 'POST', body: payload }),
  salvarLane: (payload) => request('/gate/operacional/configuracao/lanes', { method: 'POST', body: payload }),
  salvarStage: (payload) => request('/gate/operacional/configuracao/stages', { method: 'POST', body: payload }),
  salvarTask: (stageId, payload) => request(`/gate/operacional/configuracao/stages/${stageId}/tasks`, { method: 'POST', body: payload }),
  salvarBooking: (payload) => request('/gate/operacional/bookings', { method: 'POST', body: payload }),
  salvarOrder: (payload) => request('/gate/operacional/ordens', { method: 'POST', body: payload }),
  salvarPreadvice: (payload) => request('/gate/operacional/pre-avisos', { method: 'POST', body: payload })
};