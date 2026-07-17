import { api as baseApi, request } from './api.js';

function requireIdentifier(value, label) {
  const identifier = Number(value);
  if (!Number.isInteger(identifier) || identifier <= 0) {
    throw new Error(`${label} é obrigatório.`);
  }
  return identifier;
}

export const steelCoilApi = {
  listarNavios: () => baseApi.listarNaviosEstivagemBulk(),
  listarTemplates: () => baseApi.listarTemplatesEstivagemBulk(),
  listarEscalas: (dias = 60) => baseApi.listarEscalasEmbarque(dias),
  listarPlanos: (navioId, visitaNavioId) => request('/api/estivagem-bulk/planos', {
    query: {
      navioId: requireIdentifier(navioId, 'Navio'),
      visitaNavioId: requireIdentifier(visitaNavioId, 'Visita de navio')
    }
  }),
  criarPlano: (dados) => request('/api/estivagem-bulk/planos', {
    method: 'POST',
    body: {
      ...dados,
      navioId: requireIdentifier(dados?.navioId, 'Navio'),
      visitaNavioId: requireIdentifier(dados?.visitaNavioId, 'Visita de navio')
    }
  }),
  buscarPlano: (planoId) => baseApi.buscarPlanoEstivagemBulk(planoId),
  adicionarBobina: (planoId, bobina) => baseApi.adicionarBobinaEstivagemBulk(planoId, bobina),
  posicionarBobina: (planoId, posicao) => baseApi.posicionarBobinaEstivagemBulk(planoId, posicao),
  analisarTanktop: (planoId) => baseApi.analisarTanktopEstivagemBulk(planoId),
  analisarEmpilhamento: (planoId, poraoId) => baseApi.analisarEmpilhamentoEstivagemBulk(planoId, poraoId),
  calcularEstabilidade: (planoId) => baseApi.calcularEstabilidadeEstivagemBulk(planoId),
  calcularSecuring: (planoId) => request(`/api/estivagem-bulk/planos/${requireIdentifier(planoId, 'Plano')}/tacktop`),
  validarPlano: (planoId) => baseApi.validarPlanoEstivagemBulk(planoId),
  obterRelatorio: (planoId) => baseApi.obterRelatorioEstivagemBulk(planoId)
};
