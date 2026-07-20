import { request, sanitizeText } from './api.js';

function requiredIdentifier(value, label) {
  const identifier = Number(value);
  if (!Number.isInteger(identifier) || identifier <= 0) throw new Error(`${label} é obrigatório.`);
  return identifier;
}

function cleanBody(body) {
  return Object.fromEntries(Object.entries(body).map(([key, value]) => {
    if (typeof value === 'string') return [key, sanitizeText(value)];
    return [key, value];
  }));
}

export const navioOperationalApi = {
  listarEscalas: (dias = 30) => request('/escalas', { query: { dias } }),
  obterProntidaoBerco: (escalaId) => request(`/escalas/${requiredIdentifier(escalaId, 'Escala')}/prontidao-berco`),
  listarHistoricoProntidaoBerco: (escalaId) => request(`/escalas/${requiredIdentifier(escalaId, 'Escala')}/prontidao-berco/historico`),
  confirmarProntidaoBerco: (escalaId, body) => request(`/escalas/${requiredIdentifier(escalaId, 'Escala')}/prontidao-berco`, {
    method: 'POST',
    body: cleanBody({ ...body, caladoMetros: Number(body.caladoMetros) })
  }),
  iniciarOperacaoEscala: (escalaId) => request(`/escalas/${requiredIdentifier(escalaId, 'Escala')}/fase`, {
    method: 'PATCH',
    body: { fase: 'OPERANDO' }
  }),
  obterExecucaoGuindastes: (planId) => request(`/api/vessel-planner/planos/${requiredIdentifier(planId, 'Plano de estiva')}/execucao-guindastes`),
  listarEventosGuindastes: (execucaoId) => request(`/api/vessel-planner/execucoes-guindastes/${requiredIdentifier(execucaoId, 'Execução de guindastes')}/eventos-operacionais`),
  registrarParalisacaoGuindaste: (execucaoId, body) => request(`/api/vessel-planner/execucoes-guindastes/${requiredIdentifier(execucaoId, 'Execução de guindastes')}/paralisacoes`, {
    method: 'POST',
    body: cleanBody({ ...body, guindasteId: Number(body.guindasteId) })
  }),
  encerrarParalisacaoGuindaste: (execucaoId, eventoId, body = {}) => request(`/api/vessel-planner/execucoes-guindastes/${requiredIdentifier(execucaoId, 'Execução de guindastes')}/paralisacoes/${requiredIdentifier(eventoId, 'Paralisação')}/encerrar`, {
    method: 'POST',
    body: cleanBody(body)
  }),
  registrarHandoverGuindaste: (execucaoId, body) => request(`/api/vessel-planner/execucoes-guindastes/${requiredIdentifier(execucaoId, 'Execução de guindastes')}/handovers`, {
    method: 'POST',
    body: cleanBody({ ...body, guindasteId: Number(body.guindasteId) })
  })
};
