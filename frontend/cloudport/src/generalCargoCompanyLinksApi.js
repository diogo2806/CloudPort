import { request } from './api.js';

function resourcePath(resourceType, resourceId) {
  const segment = resourceType === 'LOTE' ? 'lotes' : 'conhecimentos';
  return `/api/carga-geral/${segment}/${encodeURIComponent(resourceId)}/empresas`;
}

export const generalCargoCompanyLinksApi = {
  listar: (resourceType, resourceId) => request(resourcePath(resourceType, resourceId)),
  salvar: (resourceType, resourceId, body) => request(resourcePath(resourceType, resourceId), {
    method: 'PUT',
    body
  })
};
