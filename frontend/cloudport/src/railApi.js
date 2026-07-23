import { request, sanitizeText } from './api.js';

function positiveId(value, field) {
  const id = Number(value);
  if (!Number.isInteger(id) || id <= 0) throw new TypeError(`${field} deve ser um número inteiro positivo.`);
  return id;
}

function requiredText(value, field) {
  const text = sanitizeText(value);
  if (!text) throw new TypeError(`${field} deve ser informado.`);
  return text;
}

function normalizeTrain(payload) {
  return {
    identificador: requiredText(payload?.identificador, 'O identificador').toUpperCase(),
    operadoraFerroviaria: requiredText(payload?.operadoraFerroviaria, 'A operadora ferroviária'),
    descricao: sanitizeText(payload?.descricao),
    ativo: payload?.ativo !== false,
    observacoes: sanitizeText(payload?.observacoes),
    composicaoPadrao: (Array.isArray(payload?.composicaoPadrao) ? payload.composicaoPadrao : []).map((item, index) => ({
      posicaoNoTrem: index + 1,
      identificadorVagao: requiredText(item?.identificadorVagao, 'O identificador do vagão').toUpperCase(),
      tipoVagao: sanitizeText(item?.tipoVagao).toUpperCase() || null
    }))
  };
}

export const railApi = {
  listarVisitas: (dias = 30) => request('/rail/ferrovia/visitas', { query: { dias } }),
  consultarVisita: (idVisita) => request(`/rail/ferrovia/visitas/${positiveId(idVisita, 'O identificador da visita')}`),
  listarTrens: () => request('/rail/ferrovia/trens'),
  consultarTrem: (id) => request(`/rail/ferrovia/trens/${positiveId(id, 'O identificador do trem')}`),
  criarTrem: (payload) => request('/rail/ferrovia/trens', { method: 'POST', body: normalizeTrain(payload) }),
  atualizarTrem: (id, payload) => request(`/rail/ferrovia/trens/${positiveId(id, 'O identificador do trem')}`, { method: 'PUT', body: normalizeTrain(payload) }),
  alterarSituacaoTrem: (id, ativo) => request(`/rail/ferrovia/trens/${positiveId(id, 'O identificador do trem')}/situacao`, { method: 'PATCH', query: { ativo: Boolean(ativo) } }),
  listarOrdens: (idVisita, status = '') => request(`/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/ordens`, { query: status ? { status } : undefined }),
  atualizarStatusOrdem: (idVisita, idOrdem, status) => request(`/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/ordens/${positiveId(idOrdem, 'O identificador da ordem')}/status`, { method: 'PATCH', body: { statusMovimentacao: status } }),
  listarManobras: (idVisita) => request(`/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/manobras`),
  criarManobra: (idVisita, payload) => request(`/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/manobras`, { method: 'POST', body: payload }),
  atualizarStatusManobra: (idVisita, idManobra, payload) => request(`/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/manobras/${positiveId(idManobra, 'O identificador da manobra')}/status`, { method: 'PATCH', body: payload }),
  listarInspecoesVagoes: (idVisita) => request(`/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/inspecoes-vagoes`),
  registrarInspecaoVagao: (idVisita, payload) => request(`/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/inspecoes-vagoes`, { method: 'POST', body: payload }),
  liberarVagaoOverride: (idVisita, idInspecao, payload) => request(`/rail/ferrovia/lista-trabalho/visitas/${positiveId(idVisita, 'O identificador da visita')}/inspecoes-vagoes/${positiveId(idInspecao, 'O identificador da inspeção')}/override`, { method: 'PATCH', body: payload }),
  replanejarConteiner: (idVisita, payload) => request(`/rail/ferrovia/visitas/${positiveId(idVisita, 'O identificador da visita')}/replanejamentos-conteiner`, { method: 'POST', body: payload }),
  listarHistoricoReplanejamento: (idVisita) => request(`/rail/ferrovia/visitas/${positiveId(idVisita, 'O identificador da visita')}/replanejamentos-conteiner`),
  registrarPartida: (idVisita) => request(`/rail/ferrovia/visitas/${positiveId(idVisita, 'O identificador da visita')}/partida`, { method: 'PATCH' })
};
