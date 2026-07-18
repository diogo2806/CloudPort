import { request } from './api.js';

function normalizeText(value) {
  const normalized = String(value ?? '').trim();
  return normalized || undefined;
}

function normalizeEnum(value) {
  return normalizeText(value)?.toUpperCase();
}

function normalizeBoolean(value) {
  return value === true ? true : undefined;
}

function startOfDay(value) {
  const normalized = normalizeText(value);
  return normalized ? `${normalized}T00:00:00` : undefined;
}

function endOfDay(value) {
  const normalized = normalizeText(value);
  return normalized ? `${normalized}T23:59:59` : undefined;
}

export function buildInventoryQuery(filters = {}) {
  return {
    codigo: normalizeText(filters.codigo),
    status: normalizeEnum(filters.status),
    tipoCarga: normalizeEnum(filters.tipoCarga)
  };
}

export function buildCanonicalInventoryQuery(filters = {}) {
  return {
    identificacao: normalizeText(filters.identificacao),
    categoria: normalizeEnum(filters.categoria),
    estado: normalizeEnum(filters.estado),
    condicao: normalizeEnum(filters.condicao),
    proprietario: normalizeText(filters.proprietario),
    operador: normalizeText(filters.operador),
    somenteComHold: normalizeBoolean(filters.somenteComHold),
    somenteReefer: normalizeBoolean(filters.somenteReefer)
  };
}

export function buildGateReportQuery(filters = {}) {
  return {
    inicio: startOfDay(filters.inicio),
    fim: endOfDay(filters.fim),
    tipoOperacao: normalizeEnum(filters.tipoOperacao),
    transportadoraId: normalizeText(filters.transportadoraId)
  };
}

export function selectInventoryRows(payload) {
  return Array.isArray(payload?.conteineres) ? payload.conteineres : [];
}

export function selectCanonicalInventoryRows(payload) {
  return Array.isArray(payload?.unidades) ? payload.unidades : [];
}

export function selectGateReportRows(payload) {
  return Array.isArray(payload?.agendamentos) ? payload.agendamentos : [];
}

export const inventoryReportsApi = {
  obterInventario: (filters = {}) => request('/yard/inventario', {
    query: buildInventoryQuery(filters)
  }),
  obterInventarioCanonico: (filters = {}) => request('/yard/inventario/canonico/unidades', {
    query: buildCanonicalInventoryQuery(filters)
  }),
  obterUnidadeInventario: (unidadeId) => request(`/yard/inventario/canonico/unidades/${unidadeId}`),
  listarTiposInventario: () => request('/yard/inventario/canonico/tipos'),
  listarPrefixosInventario: () => request('/yard/inventario/canonico/prefixos'),
  listarDivergenciasInventario: () => request('/yard/inventario/canonico/divergencias'),
  criarUnidadeInventario: (body) => request('/yard/inventario/canonico/unidades', { method: 'POST', body }),
  atualizarEstadoInventario: (unidadeId, body) => request(`/yard/inventario/canonico/unidades/${unidadeId}/estado`, { method: 'PATCH', body }),
  atualizarPropriedadeInventario: (unidadeId, body) => request(`/yard/inventario/canonico/unidades/${unidadeId}/propriedade`, { method: 'PATCH', body }),
  atualizarPosicaoInventario: (unidadeId, body) => request(`/yard/inventario/canonico/unidades/${unidadeId}/posicao`, { method: 'PATCH', body }),
  adicionarLacreInventario: (unidadeId, body) => request(`/yard/inventario/canonico/unidades/${unidadeId}/lacres`, { method: 'POST', body }),
  adicionarDocumentoInventario: (unidadeId, body) => request(`/yard/inventario/canonico/unidades/${unidadeId}/documentos`, { method: 'POST', body }),
  adicionarAvariaInventario: (unidadeId, body) => request(`/yard/inventario/canonico/unidades/${unidadeId}/avarias`, { method: 'POST', body }),
  adicionarRestricaoInventario: (unidadeId, body) => request(`/yard/inventario/canonico/unidades/${unidadeId}/holds-permissions`, { method: 'POST', body }),
  adicionarManutencaoInventario: (unidadeId, body) => request(`/yard/inventario/canonico/unidades/${unidadeId}/manutencoes`, { method: 'POST', body }),
  registrarReeferInventario: (unidadeId, body) => request(`/yard/inventario/canonico/unidades/${unidadeId}/reefer`, { method: 'POST', body }),
  montarEquipamentosInventario: (body) => request('/yard/inventario/canonico/montagens', { method: 'POST', body }),
  desmontarEquipamentosInventario: (vinculoId, body) => request(`/yard/inventario/canonico/montagens/${vinculoId}/desmontagem`, { method: 'POST', body }),
  registrarContagemInventario: (body) => request('/yard/inventario/canonico/contagens', { method: 'POST', body }),
  resolverDivergenciaInventario: (divergenciaId, body) => request(`/yard/inventario/canonico/divergencias/${divergenciaId}/resolucao`, { method: 'POST', body }),
  obterRelatorioGate: (filters = {}) => request('/gate/relatorios', {
    query: buildGateReportQuery(filters)
  })
};
