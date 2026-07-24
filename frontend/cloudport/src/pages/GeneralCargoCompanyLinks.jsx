import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError } from '../api.js';
import { DataTable, EmptyState, Loading, Message, Section, StatusBadge } from '../components.jsx';
import { generalCargoCompanyLinksApi } from '../generalCargoCompanyLinksApi.js';
import {
  COMPANY_ROLE_LABELS,
  buildLinksPayload,
  companiesForRole,
  companyOptionLabel,
  selectedCompany,
  selectionFromLinks
} from '../generalCargoCompanyLinksModel.js';

export function GeneralCargoCompanyLinks({
  resourceType,
  resourceId,
  resourceLabel,
  companies,
  roles,
  onSaved
}) {
  const [links, setLinks] = useState([]);
  const [selection, setSelection] = useState({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = useCallback(async () => {
    if (!resourceId) {
      setLinks([]);
      setSelection({});
      setError('');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const response = await generalCargoCompanyLinksApi.listar(resourceType, resourceId);
      const normalized = Array.isArray(response) ? response : [];
      setLinks(normalized);
      setSelection(selectionFromLinks(normalized));
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }, [resourceId, resourceType]);

  useEffect(() => { load(); }, [load]);

  const linkedByRole = useMemo(
    () => Object.fromEntries(links.map((link) => [link.papel, link])),
    [links]
  );

  async function save(event) {
    event.preventDefault();
    if (!resourceId) return;
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const response = await generalCargoCompanyLinksApi.salvar(
        resourceType,
        resourceId,
        buildLinksPayload(selection, roles)
      );
      const normalized = Array.isArray(response) ? response : [];
      setLinks(normalized);
      setSelection(selectionFromLinks(normalized));
      setSuccess('Vínculos empresariais atualizados.');
      onSaved?.(normalized);
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setSaving(false);
    }
  }

  const title = resourceType === 'LOTE'
    ? 'Empresas do cargo lot'
    : 'Empresas do Bill of Lading';
  const description = resourceId
    ? `${resourceLabel || resourceId}. Selecione somente empresas ativas e compatíveis com cada papel.`
    : `Selecione um ${resourceType === 'LOTE' ? 'cargo lot' : 'Bill of Lading'} para consultar e manter os vínculos.`;

  return <Section title={title} description={description}>
    <Message type="error">{error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>
    {!resourceId ? <EmptyState title="Nenhum registro selecionado" /> : loading ? (
      <Loading label="Carregando empresas vinculadas..." />
    ) : !Array.isArray(companies) || companies.length === 0 ? (
      <EmptyState title="Nenhuma empresa disponível" description="Cadastre empresas ou verifique sua permissão de consulta." />
    ) : <>
      <form className="planner-selection-grid" onSubmit={save}>
        {roles.map((role) => {
          const selectedId = selection[role] || '';
          const selected = selectedCompany(companies, selectedId);
          const options = companiesForRole(companies, role, selectedId);
          const blocked = selected && (selected.ativo === false || !selected.papeis?.includes(role));
          return <label className="field" key={role}>
            <span>{COMPANY_ROLE_LABELS[role] || role}</span>
            <select
              value={selectedId}
              onChange={(event) => setSelection((current) => ({ ...current, [role]: event.target.value }))}
            >
              <option value="">Não informado</option>
              {options.map((company) => <option key={company.id} value={company.id}>
                {companyOptionLabel(company)}
              </option>)}
            </select>
            {blocked && <small>Vínculo atual bloqueado: empresa inativa ou sem o papel necessário.</small>}
          </label>;
        })}
        <div className="field">
          <span>Ação</span>
          <button type="submit" disabled={saving}>{saving ? 'Salvando...' : 'Salvar empresas'}</button>
        </div>
      </form>
      <DataTable
        rows={links}
        rowKey={(row) => `${row.papel}-${row.empresaId}`}
        columns={[
          { key: 'papel', label: 'Papel', render: (row) => COMPANY_ROLE_LABELS[row.papel] || row.papel },
          { key: 'codigo', label: 'Código' },
          { key: 'razaoSocial', label: 'Razão social' },
          { key: 'ativa', label: 'Situação', render: (row) => <StatusBadge value={row.ativa ? 'ATIVA' : 'INATIVA'} /> },
          { key: 'vinculadoPor', label: 'Alterado por' }
        ]}
        emptyTitle="Nenhuma empresa vinculada"
      />
    </>}
  </Section>;
}
