import { api } from './api.js';

export const ROLES = ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE'];
export const PLAN_STATUSES = ['RASCUNHO', 'PUBLICADO', 'EM_EXECUCAO', 'CONCLUIDO'];
export const TRANSITION_ACTIONS = {
  PENDENTE: ['Retomar', api.retomarWorkInstructionPatio],
  SUSPENSA: ['Suspender', api.suspenderWorkInstructionPatio],
  BLOQUEADA: ['Bloquear', api.bloquearWorkInstructionPatio],
  CONCLUIDA: ['Concluir', api.concluirWorkInstructionPatio]
};
export const EMPTY_SUMMARY = { totalItensPlanejados: 0, totalItensOperados: 0, pesoPlanejado: 0, pesoOperado: 0, percentualProgresso: 0 };
export const EMPTY_INTEGRATION = { itensComOrdem: 0, ordensEmExecucao: 0, totalAlertas: 0, statusPredominante: 'NAO_GERADO' };
export const clean = (value) => String(value ?? '').normalize('NFKC').replace(/[<>"'`\\]/g, '').trim();
export const normalized = (value) => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase();
export const statusClass = (value) => `status status-${String(value ?? 'indefinido').toLowerCase().replaceAll('_', '-')}`;
export const dateTime = (value) => value ? new Date(value).toLocaleString('pt-BR') : '—';
export const number = (value, digits = 0) => Number(value ?? 0).toLocaleString('pt-BR', { minimumFractionDigits: digits, maximumFractionDigits: digits });
export const orderCode = (order) => order?.codigoConteiner || order?.codigoLote || `Ordem ${order?.id ?? '—'}`;
export const orderOrigin = (order) => order?.tipoOrigem || order?.origem || '—';
export const orderDestination = (order) => order?.destino || order?.posicaoPlanejada || '—';
export const localInput = (value) => value ? String(value).slice(0, 16) : '';
export const apiDate = (value) => value && value.length === 16 ? `${value}:00` : value;

export function defaultAllocation(index, queue) {
  const start = new Date(Date.now() + index * 3600000);
  const end = new Date(start.getTime() + 3600000);
  const format = (date) => new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  return {
    codigoGuindaste: queue?.equipamento || '', recursoCais: queue?.pow || '', porao: queue?.porao || '',
    workQueueId: queue?.id || '', sequencia: index + 1, movimentosPlanejados: Math.max(1, queue?.totalOrdens || 1),
    produtividadePlanejadaMovimentosHora: 20, inicioPlanejado: format(start), fimPlanejado: format(end), observacao: ''
  };
}

export function planDraft(plan, visit) {
  return {
    berco: plan?.berco || visit?.bercoAtual || visit?.bercoPrevisto || '', status: plan?.status || 'RASCUNHO',
    observacao: plan?.observacao || '', guindastes: (plan?.guindastes || []).map((item) => ({ ...item,
      inicioPlanejado: localInput(item.inicioPlanejado), fimPlanejado: localInput(item.fimPlanejado) }))
  };
}

export function queueOperational(queue, visitId, berco, porao) {
  return queue?.id && Number(queue.visitaNavioId) === Number(visitId) && queue.status === 'ATIVA'
    && queue.pow && queue.poolOperacional && queue.equipamentoPatioId
    && (!berco || !queue.berco || normalized(queue.berco) === normalized(berco))
    && (!porao || !queue.porao || Number(queue.porao) === Number(porao));
}

export function validatePlan(draft, queues, visitId) {
  const errors = [];
  if (!clean(draft.berco)) errors.push('Informe o berço.');
  if (!draft.guindastes.length) errors.push('Adicione ao menos uma alocação.');
  const sequences = new Set();
  draft.guindastes.forEach((item, index) => {
    const prefix = `Alocação ${index + 1}`;
    const queue = queues.find((entry) => Number(entry.id) === Number(item.workQueueId));
    if (!clean(item.codigoGuindaste)) errors.push(`${prefix}: guindaste obrigatório.`);
    if (!(Number(item.porao) > 0)) errors.push(`${prefix}: porão inválido.`);
    if (!(Number(item.sequencia) > 0) || sequences.has(Number(item.sequencia))) errors.push(`${prefix}: sequência inválida ou repetida.`);
    sequences.add(Number(item.sequencia));
    if (!(Number(item.movimentosPlanejados) > 0) || !(Number(item.produtividadePlanejadaMovimentosHora) > 0)) errors.push(`${prefix}: movimentos e produtividade devem ser positivos.`);
    if (!item.inicioPlanejado || !item.fimPlanejado || item.fimPlanejado <= item.inicioPlanejado) errors.push(`${prefix}: janela planejada inválida.`);
    if (!queueOperational(queue, visitId, draft.berco, item.porao)) errors.push(`${prefix}: selecione uma work queue ativa, coberta e com CHE real.`);
  });
  return errors;
}

export function serializePlan(draft) {
  return {
    berco: clean(draft.berco), status: draft.status, observacao: clean(draft.observacao) || null,
    guindastes: draft.guindastes.map((item) => ({
      id: item.id || null, codigoGuindaste: clean(item.codigoGuindaste), recursoCais: clean(item.recursoCais) || null,
      porao: Number(item.porao), workQueueId: Number(item.workQueueId), sequencia: Number(item.sequencia),
      movimentosPlanejados: Number(item.movimentosPlanejados), produtividadePlanejadaMovimentosHora: Number(item.produtividadePlanejadaMovimentosHora),
      inicioPlanejado: apiDate(item.inicioPlanejado), fimPlanejado: apiDate(item.fimPlanejado), observacao: clean(item.observacao) || null
    }))
  };
}
