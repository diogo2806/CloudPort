import { readSession, request, sanitizeText } from './api.js';

function usuarioAtual() {
  return sanitizeText(readSession()?.nome) || 'operador';
}

function basePath(planoId) {
  return `/api/vessel-planner/planos/${Number(planoId)}/reconciliacoes`;
}

export const vesselPlannerReconciliacaoApi = {
  buscarAtual: (planoId) => request(`${basePath(planoId)}/atual`),

  reconciliar: (planoId) => request(basePath(planoId), {
    method: 'POST',
    body: { usuario: usuarioAtual() }
  }),

  resolver: (planoId, reconciliacaoId, divergenciaId, decisao, motivo) => request(
    `${basePath(planoId)}/${Number(reconciliacaoId)}/divergencias/${Number(divergenciaId)}/resolver`,
    {
      method: 'POST',
      body: {
        decisao,
        motivo: sanitizeText(motivo),
        usuario: usuarioAtual()
      }
    }
  )
};
