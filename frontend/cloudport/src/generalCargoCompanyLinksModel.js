export const COMPANY_ROLE_LABELS = Object.freeze({
  CLIENTE: 'Cliente',
  EMBARCADOR: 'Embarcador',
  CONSIGNATARIO: 'Consignatário',
  IMPORTADOR: 'Importador',
  EXPORTADOR: 'Exportador',
  DONO_CARGA: 'Dono da carga',
  OPERADOR: 'Operador',
  AGENTE: 'Agente',
  TRANSPORTADORA: 'Transportadora'
});

export const BILL_COMPANY_ROLES = Object.freeze(Object.keys(COMPANY_ROLE_LABELS));
export const LOT_COMPANY_ROLES = Object.freeze([
  'CLIENTE',
  'DONO_CARGA',
  'OPERADOR',
  'TRANSPORTADORA'
]);

export function companyName(company) {
  return company?.nomeFantasia?.trim() || company?.razaoSocial?.trim() || company?.codigo || 'Empresa sem nome';
}

export function companyOptionLabel(company) {
  const status = company?.ativo === false ? ' · INATIVA' : '';
  return `${company?.codigo || 'SEM CÓDIGO'} · ${companyName(company)}${status}`;
}

export function companiesForRole(companies, role, linkedCompanyId = '') {
  return (Array.isArray(companies) ? companies : [])
    .filter((company) => {
      const linked = String(company?.id || '') === String(linkedCompanyId || '');
      const compatible = Array.isArray(company?.papeis) && company.papeis.includes(role);
      return linked || (company?.ativo !== false && compatible);
    })
    .sort((left, right) => companyOptionLabel(left).localeCompare(companyOptionLabel(right), 'pt-BR'));
}

export function selectionFromLinks(links) {
  return Object.fromEntries((Array.isArray(links) ? links : []).map((link) => [link.papel, link.empresaId]));
}

export function buildLinksPayload(selection, roles) {
  return {
    vinculos: (Array.isArray(roles) ? roles : [])
      .filter((role) => selection?.[role])
      .map((role) => ({ papel: role, empresaId: selection[role] }))
  };
}

export function selectedCompany(companies, companyId) {
  return (Array.isArray(companies) ? companies : []).find(
    (company) => String(company?.id || '') === String(companyId || '')
  ) || null;
}
