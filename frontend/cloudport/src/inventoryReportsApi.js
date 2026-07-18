import { request } from './api.js';

function normalizeText(value) {
  const normalized = String(value ?? '').trim();
  return normalized || undefined;
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
    status: normalizeText(filters.status)?.toUpperCase(),
    tipoCarga: normalizeText(filters.tipoCarga)?.toUpperCase()
  };
}

export function buildGateReportQuery(filters = {}) {
  return {
    inicio: startOfDay(filters.inicio),
    fim: endOfDay(filters.fim),
    tipoOperacao: normalizeText(filters.tipoOperacao)?.toUpperCase(),
    transportadoraId: normalizeText(filters.transportadoraId)
  };
}

export function selectInventoryRows(payload) {
  return Array.isArray(payload?.conteineres) ? payload.conteineres : [];
}

export function selectGateReportRows(payload) {
  return Array.isArray(payload?.agendamentos) ? payload.agendamentos : [];
}

export const inventoryReportsApi = {
  obterInventario: (filters = {}) => request('/yard/inventario', {
    query: buildInventoryQuery(filters)
  }),
  obterRelatorioGate: (filters = {}) => request('/gate/relatorios', {
    query: buildGateReportQuery(filters)
  })
};
