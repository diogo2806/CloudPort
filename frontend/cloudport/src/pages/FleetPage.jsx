import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, hasAnyRole } from '../api.js';
import { fleetApi } from '../fleetApi.js';
import { DataTable, EmptyState, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';

const EMPTY_FORM = {
  placa: '',
  placaCarreta: '',
  modelo: '',
  tipo: 'CAMINHAO',
  transportadoraId: '',
  ativo: true
};

export function FleetPage({ session }) {
  const [rows, setRows] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const canManageAll = hasAnyRole(session, 'ADMIN_PORTO', 'OPERADOR_GATE', 'PLANEJADOR');

  const transportadoraId = useMemo(() => {
    if (canManageAll) return form.transportadoraId;
    return session?.transportadoraId ?? '';
  }, [canManageAll, form.transportadoraId, session]);

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fleetApi.listar({ busca: search || undefined });
      setRows(Array.isArray(response) ? response : []);
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível carregar a frota.'));
    } finally {
      setLoading(false);
    }
  }, [search]);

  useEffect(() => { reload(); }, [reload]);

  function edit(row) {
    setEditingId(row.id);
    setForm({
      placa: row.placa ?? '',
      placaCarreta: row.placaCarreta ?? '',
      modelo: row.modelo ?? '',
      tipo: row.tipo ?? 'CAMINHAO',
      transportadoraId: row.transportadoraId ?? '',
      ativo: Boolean(row.ativo)
    });
    setSuccess('');
  }

  function clear() {
    setEditingId(null);
    setForm(EMPTY_FORM);
  }

  async function submit(event) {
    event.preventDefault();
    if (busy || !transportadoraId) return;
    setBusy(true);
    setError('');
    setSuccess('');
    const payload = { ...form, transportadoraId: Number(transportadoraId) };
    try {
      if (editingId) {
        await fleetApi.atualizar(editingId, payload);
        setSuccess('Veículo atualizado.');
      } else {
        await fleetApi.criar(payload);
        setSuccess('Veículo cadastrado.');
      }
      clear();
      await reload();
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível salvar o veículo.'));
    } finally {
      setBusy(false);
    }
  }

  async function toggleStatus(row) {
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      await fleetApi.atualizarStatus(row.id, !row.ativo);
      setSuccess(row.ativo ? 'Veículo inativado.' : 'Veículo ativado.');
      await reload();
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível alterar o status.'));
    } finally {
      setBusy(false);
    }
  }

  return <>
    <PageHeader
      eyebrow="Cadastros"
      title="Veículos e carretas de transportadoras"
      description="Cadastre a frota rodoviária usada nos agendamentos e mantenha placas e situação operacional atualizadas."
      actions={<button type="button" className="secondary" onClick={reload}>Atualizar</button>}
    />
    <Message type="error">{error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    <Section title={editingId ? 'Editar veículo' : 'Novo veículo'} description="Placas duplicadas são bloqueadas. Veículos inativos não aparecem na consulta elegível para novos agendamentos.">
      <form className="planner-selection-grid" onSubmit={submit}>
        <label className="field"><span>Placa do veículo</span><input required maxLength="10" value={form.placa} onChange={(event) => setForm((current) => ({ ...current, placa: event.target.value }))} placeholder="ABC1D23" /></label>
        <label className="field"><span>Placa da carreta</span><input maxLength="10" value={form.placaCarreta} onChange={(event) => setForm((current) => ({ ...current, placaCarreta: event.target.value }))} placeholder="DEF4G56" /></label>
        <label className="field"><span>Modelo</span><input maxLength="60" value={form.modelo} onChange={(event) => setForm((current) => ({ ...current, modelo: event.target.value }))} /></label>
        <label className="field"><span>Tipo</span><select value={form.tipo} onChange={(event) => setForm((current) => ({ ...current, tipo: event.target.value }))}><option value="CAMINHAO">Caminhão</option><option value="CARRETA">Carreta</option><option value="CAVALO_MECANICO">Cavalo mecânico</option><option value="VAN">Van</option></select></label>
        {canManageAll && <label className="field"><span>Transportadora ID</span><input required type="number" min="1" value={form.transportadoraId} onChange={(event) => setForm((current) => ({ ...current, transportadoraId: event.target.value }))} /></label>}
        <label className="field"><span>Situação</span><select value={form.ativo ? 'true' : 'false'} onChange={(event) => setForm((current) => ({ ...current, ativo: event.target.value === 'true' }))}><option value="true">Ativo</option><option value="false">Inativo</option></select></label>
        <div className="field"><span>Ações</span><div className="page-actions"><button type="submit" disabled={busy || !transportadoraId}>{busy ? 'Salvando...' : 'Salvar'}</button>{editingId && <button type="button" className="secondary" onClick={clear}>Cancelar</button>}</div></div>
      </form>
    </Section>

    <Section title="Frota cadastrada" actions={<input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Placa, tipo ou transportadora" aria-label="Buscar veículos" />}>
      {loading ? <Loading label="Carregando frota..." /> : rows.length ? <DataTable rows={rows} rowKey="id" columns={[
        { key: 'placa', label: 'Placa' },
        { key: 'placaCarreta', label: 'Carreta' },
        { key: 'modelo', label: 'Modelo' },
        { key: 'tipo', label: 'Tipo', render: (row) => <StatusBadge value={row.tipo} /> },
        { key: 'transportadoraNome', label: 'Transportadora' },
        { key: 'ativo', label: 'Situação', render: (row) => <StatusBadge value={row.ativo ? 'ATIVO' : 'INATIVO'} /> },
        { key: 'actions', label: 'Ações', render: (row) => <div className="page-actions"><button type="button" className="secondary" onClick={() => edit(row)}>Editar</button><button type="button" className="secondary" disabled={busy} onClick={() => toggleStatus(row)}>{row.ativo ? 'Inativar' : 'Ativar'}</button></div> }
      ]} /> : <EmptyState title="Nenhum veículo encontrado" description="Cadastre o primeiro veículo da transportadora ou ajuste a busca." />}
    </Section>
  </>;
}
