import { request } from './api.js';

export const gateAppointmentsApi = {
  listar: (query = { page: 0, size: 100 }) => request('/gate/agendamentos', { query }),
  criar: (body) => request('/gate/agendamentos', { method: 'POST', body }),
  listarJanelas: (query = { page: 0, size: 100 }) => request('/gate/janelas', { query }),
  listarTransportadoras: () => request('/gate/config/transportadoras')
};
