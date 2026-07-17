import { request, sanitizeText } from './api.js';

function cleanOptional(value) {
  const cleaned = sanitizeText(value);
  return cleaned || undefined;
}

function entryPayload(form) {
  return {
    nome: sanitizeText(form?.nome),
    documento: sanitizeText(form?.documento),
    tipoPessoa: sanitizeText(form?.tipoPessoa).toUpperCase(),
    empresa: cleanOptional(form?.empresa),
    cracha: cleanOptional(form?.cracha),
    pontoAcesso: sanitizeText(form?.pontoAcesso),
    motivo: cleanOptional(form?.motivo)
  };
}

function exitPayload(form) {
  return {
    documento: sanitizeText(form?.documento),
    pontoAcesso: sanitizeText(form?.pontoAcesso),
    motivo: cleanOptional(form?.motivo)
  };
}

export const gatePeopleAccessApi = {
  obterResumo: () => request('/gate/pessoas/resumo'),
  listarPresentes: () => request('/gate/pessoas/presentes'),
  listarMovimentacoes: (documento, limite = 100) => request('/gate/pessoas/movimentacoes', {
    query: { documento: cleanOptional(documento), limite }
  }),
  registrarEntrada: (form) => request('/gate/pessoas/entradas', {
    method: 'POST',
    body: entryPayload(form)
  }),
  registrarSaida: (form) => request('/gate/pessoas/saidas', {
    method: 'POST',
    body: exitPayload(form)
  })
};
