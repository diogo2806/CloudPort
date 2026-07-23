import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, hasAnyRole } from '../api.js';
import { DataTable, EmptyState, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';
import { isoGroupsApi } from '../isoGroupsApi.js';

const EMPTY_FORM = { codigo: '', descricao: '', categoria: 'CONTEINER', refrigerado: false };

export function IsoGroupsPage({ session }) {
  const canEdit = hasAnyRole(session, 'ADMIN_PORTO');
  const [rows, setRows] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [filters, setFilters] = useState({ termo: '', categoria: '', ativo: '' });
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const query = {
        termo: filters.termo || undefined,
        categoria: filters.categoria || undefined,
        ativo: filters.ativo === '' ? undefined : filters.ativo === 'true'
      };
      setRows(await isoGroupsApi.listar(query));
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => { load(); }, [load]);

  function clearForm() {
    setSelectedId(null); setForm(EMPTY_FORM); setError(''); setMessage('');
  }

  function selectRow(row) {
    setSelectedId(row.id);
    setForm({ codigo: row.codigo, descricao: row.descricao, categoria: row.categoria, refrigerado: Boolean(row.refrigerado) });
    setMessage(''); setError('');
  }

  async function submit(event) {
    event.preventDefault();
    if (!canEdit || busy) return;
    setBusy(true); setError(''); setMessage('');
    try {
      if (selectedId) {
        await isoGroupsApi.atualizar(selectedId, form);
        setMessage('Grupo ISO atualizado com sucesso.');
      } else {
        await isoGroupsApi.criar(form);
        setMessage('Grupo ISO cadastrado com sucesso.');
      }
      clearForm();
      await load();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  async function toggleStatus(row) {
    if (!canEdit || busy) return;
    setBusy(true); setError(''); setMessage('');
    try {
      await isoGroupsApi.alterarSituacao(row.id, !row.ativo);
      setMessage(row.ativo ? 'Grupo ISO inativado.' : 'Grupo ISO reativado.');
      await load();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  const columns = useMemo(() => [
    { key: 'codigo', label: 'Código' },
    { key: 'descricao', label: 'Descrição' },
    { key: 'categoria', label: 'Categoria' },
    { key: 'refrigerado', label: 'Reefer', render: (row) => row.refrigerado ? 'Sim' : 'Não' },
    { key: 'ativo', label: 'Situação', render: (row) => <StatusBadge status={row.ativo ? 'ATIVO' : 'INATIVO'} /> },
    { key: 'tiposAssociados', label: 'Tipos vinculados' },
    { key: 'acoes', label: 'Ações', render: (row) => canEdit ? <button className="secondary" onClick={(event) => { event.stopPropagation(); toggleStatus(row); }}>{row.ativo ? 'Inativar' : 'Reativar'}</button> : null }
  ], [canEdit, busy]);

  return <>
    <PageHeader eyebrow="Cadastros · Equipamentos" title="Grupos ISO" description="Cadastro mestre que classifica funcionalmente os tipos ISO. Grupo ISO e grupo de equivalência são conceitos independentes." actions={<button className="secondary" onClick={load}>Atualizar</button>} />
    <Message type="error">{error}</Message><Message type="success">{message}</Message>
    {!canEdit && <Message type="warning">Seu perfil pode consultar Grupos ISO, mas somente ADMIN_PORTO pode manter o cadastro.</Message>}
    <Section title="Manual operacional" description="Finalidade: padronizar classes de equipamento usadas por inventário, gate, EDI e planejamento. Fluxo: cadastre, valide categoria/reefer, associe ao Tipo ISO e inative quando deixar de ser utilizado. Bloqueios: código duplicado, categoria incompatível, grupo inativo ou registro em uso. Exemplo: REEFER para tipos refrigerados. Atalho: use os filtros abaixo. Processo completo: Cadastros > Tipos e prefixos de equipamentos." />
    <Section title="Pesquisa">
      <div className="planner-selection-grid">
        <label className="field"><span>Código ou descrição</span><input value={filters.termo} onChange={(event) => setFilters((current) => ({ ...current, termo: event.target.value }))} /></label>
        <label className="field"><span>Categoria</span><select value={filters.categoria} onChange={(event) => setFilters((current) => ({ ...current, categoria: event.target.value }))}><option value="">Todas</option><option>CONTEINER</option><option>CHASSI</option><option>CARRETA</option><option>ACESSORIO</option></select></label>
        <label className="field"><span>Situação</span><select value={filters.ativo} onChange={(event) => setFilters((current) => ({ ...current, ativo: event.target.value }))}><option value="">Todas</option><option value="true">Ativos</option><option value="false">Inativos</option></select></label>
      </div>
    </Section>
    <Section title={selectedId ? 'Editar Grupo ISO' : 'Cadastrar Grupo ISO'}>
      <form className="planner-selection-grid" onSubmit={submit}>
        <label className="field"><span>Código</span><input required maxLength="10" value={form.codigo} onChange={(event) => setForm((current) => ({ ...current, codigo: event.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Descrição</span><input required maxLength="120" value={form.descricao} onChange={(event) => setForm((current) => ({ ...current, descricao: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Categoria</span><select value={form.categoria} onChange={(event) => setForm((current) => ({ ...current, categoria: event.target.value }))} disabled={!canEdit || busy}><option>CONTEINER</option><option>CHASSI</option><option>CARRETA</option><option>ACESSORIO</option></select></label>
        <label className="field checkbox"><input type="checkbox" checked={form.refrigerado} onChange={(event) => setForm((current) => ({ ...current, refrigerado: event.target.checked }))} disabled={!canEdit || busy} /><span>Grupo refrigerado</span></label>
        <div className="field"><span>Ações</span><div className="actions"><button type="submit" disabled={!canEdit || busy}>{busy ? 'Salvando...' : selectedId ? 'Atualizar' : 'Cadastrar'}</button><button type="button" className="secondary" onClick={clearForm}>Limpar</button></div></div>
      </form>
    </Section>
    <Section title="Grupos cadastrados">
      {loading ? <Loading /> : rows.length ? <DataTable rows={rows} rowKey="id" columns={columns} onRowClick={selectRow} /> : <EmptyState title="Nenhum Grupo ISO encontrado" />}
    </Section>
  </>;
}
