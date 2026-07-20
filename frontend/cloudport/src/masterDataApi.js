import { request } from './api.js';

export const masterDataApi = {
  listarNavios: () => request('/navios'),
  criarNavio: (payload) => request('/navios', { method: 'POST', body: payload }),
  atualizarNavio: (id, payload) => request(`/navios/${id}`, { method: 'PUT', body: payload }),
  removerNavio: (id) => request(`/navios/${id}`, { method: 'DELETE' }),

  listarBercos: () => request('/yard/recursos/bercos'),
  criarBerco: (payload) => request('/yard/recursos/bercos', { method: 'POST', body: payload }),
  atualizarBerco: (id, payload) => request(`/yard/recursos/bercos/${id}`, { method: 'PUT', body: payload }),
  removerBerco: (id) => request(`/yard/recursos/bercos/${id}`, { method: 'DELETE' }),

  obterInfraestruturaGate: () => request('/gate/operacional/painel'),
  salvarFacility: (payload) => request('/gate/operacional/configuracao/facilities', { method: 'POST', body: payload }),
  salvarGate: (payload) => request('/gate/operacional/configuracao/gates', { method: 'POST', body: payload }),
  salvarLane: (payload) => request('/gate/operacional/configuracao/lanes', { method: 'POST', body: payload }),

  listarTiposEquipamento: () => request('/yard/inventario/canonico/tipos'),
  criarTipoEquipamento: (payload) => request('/yard/inventario/canonico/tipos', { method: 'POST', body: payload }),
  listarPrefixosEquipamento: () => request('/yard/inventario/canonico/prefixos'),
  criarPrefixoEquipamento: (payload) => request('/yard/inventario/canonico/prefixos', { method: 'POST', body: payload })
};
