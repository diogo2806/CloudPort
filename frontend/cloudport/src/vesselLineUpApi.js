import { request } from './api.js';

export const vesselLineUpApi = {
  listar: (dias = 30) => request('/escalas/line-up', { query: { dias } })
};
