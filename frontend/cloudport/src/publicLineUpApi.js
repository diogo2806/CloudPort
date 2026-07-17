import { request } from './api.js';

export function listarLineUpPublico(dias = 30) {
  return request('/public/line-up-navios', {
    query: { dias },
    public: true
  });
}
