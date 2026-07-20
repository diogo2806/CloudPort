import { request, sanitizeText } from './api.js';

const ORDER_STATUSES = new Set(['PENDENTE', 'EM_EXECUCAO', 'CONCLUIDA']);
const MOVEMENT_TYPES = new Set(['DESCARGA_TREM', 'CARGA_TREM']);
const MANEUVER_STATUSES = new Set(['AUTORIZADA', 'EM_EXECUCAO', 'CONCLUIDA', 'CANCELADA']);
const DEFECT_SEVERITIES = new Set(['BAIXA', 'MEDIA', 'ALTA', 'CRITICA']);

function positiveId(value, field) {
  const id = Number(value);
  if (!Number.isInteger(id) || id <= 0) {
    throw new TypeError(`${field} deve ser um número inteiro positivo.`);
  }
  return id;
}

function normalizeStatus(value, optional = false) {
  const status = sanitizeText(value).toUpperCase();
  if (!status && optional) return '';
  if (!ORDER_STATUSES.has(status)) {
    throw new TypeError('Status ferroviário inválido.');
  }
  return status;
}

function normalizeMovementType(value) {
  const type = sanitizeText(value).toUpperCase();
  if (!MOVEMENT_TYPES.has(type)) {
    throw new TypeError('Tipo de movimentação ferroviária inválido.');
  }
  return type;
}

function normalizeReplanning(payload) {
  const version = Number(payload?.versaoComposicao);
  if (!Number.isInteger(version) || version < 0) {
    throw new TypeError('A versão da composição é inválida.');
  }
  const reason = sanitizeText(payload?.motivo);
  if (!reason) {
    throw new TypeError('O motivo do replanejamento deve ser informado.');
  }
  return {
    codigoConteiner: sanitizeText(payload?.codigoConteiner).toUpperCase(),
    tipoMovimentacao: normalizeMovementType(payload?.tipoMovimentacao),
    vagaoOrigem: sanitizeText(payload?.vagaoOrigem).toUpperCase(),
    vagaoDestino: sanitizeText(payload?.vagaoDestino).toUpperCase(),
    versaoComposicao: version,
    motivo: reason
  };
}

function requiredText(value, field) {
  const text = sanitizeText(value);
  if (!text) throw new TypeError(`${field} deve ser informado.`);
  return text;
}

function normalizeManeuver(payload) {
  const sequence = Number(payload?.sequencia);
  if (!Number.isInteger(sequence) || sequence <= 0) {
    throw new TypeError('A sequência da manobra deve ser um inteiro positivo.');
  }
  const start = requiredText(payload?.inicioPrevisto, 'O início previsto');
  const end = requiredText(payload?.fimPrevisto, 'O fim previsto');
  if (new Date(end).getTime() <= new Date(start).getTime()) {
    throw new TypeError('O fim previsto deve ser posterior ao início previsto.');
  }
  return {
    sequencia: sequence,
    origem: requiredText(payload?.origem, 'A origem'),
    destino: requiredText(payload?.destino, 'O destino'),
    composicao: requiredText(payload?.composicao, 'A composição'),
    linha: requiredText(payload?.linha, 'A linha').toUpperCase(),
    trecho: requiredText(payload?.trecho, 'O trecho').toUpperCase(),
    inicioPrevisto: start,
    fimPrevisto: end
  };
}

function normalizeManeuverStatus(payload) {
  const status = sanitizeText(payload?.status).toUpperCase();
  if (!MANEUVER_STATUSES.has(status)) {
    throw new TypeError('A transição de status da manobra é inválida.');
  }
  return { status, motivo: sanitizeText(payload?.motivo) };
}

function normalizeDefect(defect) {
  const severity = sanitizeText(defect?.severidade).toUpperCase();
  if (!DEFECT_SEVERITIES.has(severity)) {
    throw new TypeError('A severidade do defeito é inválida.');
  }
  return {
    codigo: requiredText(defect?.codigo, 'O código do defeito').toUpperCase(),
    descricao: requiredText(defect?.descricao, 'A descrição do defeito'),
    severidade: severity,
    evidencia: sanitizeText(defect?.evidencia)
  };
}

function normalizeInspection(payload) {
  return {
    identificadorVagao: requiredText(payload?.identificadorVagao, 'O vagão').toUpperCase(),
    rodasAprovadas: Boolean(payload?.rodasAprovadas),
    freiosAprovados: Boolean(payload?.freiosAprovados),
    engatesAprovados: Boolean(payload?.engatesAprovados),
    estruturaAprovada: Boolean(payload?.estruturaAprovada),
    lacresAprovados: Boolean(payload?.lacresAprovados),
    responsavel: requiredText(payload?.responsavel, 'O responsável'),
    observacao: sanitizeText(payload?.observacao),
    defeitos: Array.isArray(payload?.defeitos) ? payload.defeitos.map(normalizeDefect) : []
  };
}

function normalizeOverride(payload) {
  return {
    responsavel: requiredText(payload?.responsavel, 'O responsável pelo override'),
    motivo: requiredText(payload?.motivo, 'O motivo do override')
  };
}

export function railOrderTransitions(status) {
  const current = normalizeStatus(status);
  if (current === 'PENDENTE') return ['EM_EXECUCAO', 'CONCLUIDA'];
  if (current === 'EM_EXECUCAO') return ['CONCLUIDA'];
  return [];
}

export const railApi = {
  listarVisitas: (dias = 30) => request('/rail/ferrovia/visitas', { query: { dias } }),
  consultarVisita: (idVisita) => request(`/rail/ferrovia/visitas/${positiveId(idVisita, 'O identificador da visita')}`),
  listarOrdens: (idVisita, status = '') => request(
    `/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/ordens`,
    { query: normalizeStatus(status, true) ? { status: normalizeStatus(status) } : undefined }
  ),
  atualizarStatusOrdem: (idVisita, idOrdem, status) => request(
    `/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/ordens/${positiveId(idOrdem, 'O identificador da ordem')}/status`,
    { method: 'PATCH', body: { statusMovimentacao: normalizeStatus(status) } }
  ),
  listarManobras: (idVisita) => request(
    `/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/manobras`
  ),
  criarManobra: (idVisita, payload) => request(
    `/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/manobras`,
    { method: 'POST', body: normalizeManeuver(payload) }
  ),
  atualizarStatusManobra: (idVisita, idManobra, payload) => request(
    `/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/manobras/${positiveId(idManobra, 'O identificador da manobra')}/status`,
    { method: 'PATCH', body: normalizeManeuverStatus(payload) }
  ),
  listarInspecoesVagoes: (idVisita) => request(
    `/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/inspecoes-vagoes`
  ),
  registrarInspecaoVagao: (idVisita, payload) => request(
    `/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/inspecoes-vagoes`,
    { method: 'POST', body: normalizeInspection(payload) }
  ),
  liberarVagaoOverride: (idVisita, idInspecao, payload) => request(
    `/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/inspecoes-vagoes/${positiveId(idInspecao, 'O identificador da inspeção')}/override`,
    { method: 'PATCH', body: normalizeOverride(payload) }
  ),
  replanejarConteiner: (idVisita, payload) => request(
    `/rail/ferrovia/visitas/${positiveId(idVisita, 'O identificador da visita')}/replanejamentos-conteiner`,
    { method: 'POST', body: normalizeReplanning(payload) }
  ),
  listarHistoricoReplanejamento: (idVisita) => request(
    `/rail/ferrovia/visitas/${positiveId(idVisita, 'O identificador da visita')}/replanejamentos-conteiner`
  ),
  registrarPartida: (idVisita) => request(
    `/rail/ferrovia/visitas/${positiveId(idVisita, 'O identificador da visita')}/partida`,
    { method: 'PATCH' }
  )
};
