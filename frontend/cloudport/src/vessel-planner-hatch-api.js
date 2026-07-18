import { request } from './api.js';

export const hatchCoverApi = {
  list: (planId) => request(`/api/vessel-planner/planos/${planId}/tampas-porao`),
  synchronize: (planId) => request(`/api/vessel-planner/planos/${planId}/tampas-porao/sincronizar`, {
    method: 'POST'
  }),
  startTask: (planId, taskId, resource, observation = '') => request(
    `/api/vessel-planner/planos/${planId}/tampas-porao/tarefas/${taskId}/iniciar`,
    { method: 'POST', body: { recurso: resource, observacao: observation } }
  ),
  confirmTask: (planId, taskId, observation = '') => request(
    `/api/vessel-planner/planos/${planId}/tampas-porao/tarefas/${taskId}/confirmar`,
    { method: 'POST', body: { observacao: observation } }
  ),
  cancelTask: (planId, taskId, reason) => request(
    `/api/vessel-planner/planos/${planId}/tampas-porao/tarefas/${taskId}/cancelar`,
    { method: 'POST', body: { motivo: reason } }
  )
};
