import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, hasAnyRole } from '../api.js';
import { DataTable, EmptyState, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';
import { isoGroupsApi } from '../isoGroupsApi.js';
import { tipoIsoApi } from '../tipoIsoApi.js';

const EMPTY = { codigo: '', isoId: '', descricao: '', categoria: 'CONTEINER', grupoIsoId: '', arquetipoIso: '', indicadorArquetipo: false, comprimentoMm: '', larguraMm: '', alturaMm: '', taraKg: '', capacidadeKg: '', refrigerado: false, grupoEquivalencia: '', provisorioEdi: false };
const numberOrNull = (value) => value === '' ? null : Number(value);

export function TipoIsoPage({ session }) {
  const canEdit = hasAnyRole(session, 'ADMIN_PORTO');
  const [rows, setRows] = useState([]);
  const [groups, setGroups] = useState([]);
  const [filters, setFilters] = useState({ termo: '', categoria: '', ativo: '', refrigerado: '', arquetipo: '' });
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const query = Object.fromEntries(Object.entries(filters).map(([key, value]) => [key, value === '' ? undefined : (key === 'ativo' || key === 'refrigerado' || key === 'arquetipo' ? value === 'true' : value)]));
      const [types, isoGroups] = await Promise.all([tipoIsoApi.listar(query), isoGroupsApi.listar({ ativo: true })]);
      setRows(types ?? []); setGroups(isoGroups ?? []);
    } catch (reason) { setError(formatError(reason)); } finally { setLoading(false); }
  }, [filters]);

  useEffect(() => { load(); }, [load]);

  function selectRow(row) {
    setSelectedId(row.id);
    setForm({ codigo: row.codigo ?? '', isoId: row.isoId ?? '', descricao: row.descricao ?? '', categoria: row.categoria ?? 'CONTEINER', grupoIsoId: row.grupoIsoId ?? '', arquetipoIso: row.arquetipoIso ?? '', indicadorArquetipo: Boolean(row.indicadorArquetipo), comprimentoMm: row.comprimentoMm ?? '', larguraMm: row.larguraMm ?? '', alturaMm: row.alturaMm ?? '', taraKg: row.taraKg ?? '', capacidadeKg: row.capacidadeKg ?? '', refrigerado: Boolean(row.refrigerado), grupoEquivalencia: row.grupoEquivalencia ?? '', provisorioEdi: Boolean(row.provisorioEdi) });
    setMessage(''); setError('');
  }

  function clearForm() { setSelectedId(null); setForm(EMPTY); setMessage(''); setError(''); }

  async function submit(event) {
    event.preventDefault(); if (!canEdit || busy) return;
    setBusy(true); setError(''); setMessage('');
    const payload = { ...form, grupoIsoId: numberOrNull(form.grupoIsoId), comprimentoMm: numberOrNull(form.comprimentoMm), larguraMm: numberOrNull(form.larguraMm), alturaMm: numberOrNull(form.alturaMm), taraKg: numberOrNull(form.taraKg), capacidadeKg: numberOrNull(form.capacidadeKg), usuario: session?.usuario?.login || session?.login || 'PORTAL' };
    try {
      if (selectedId) await tipoIsoApi.atualizar(selectedId, payload); else await tipoIsoApi.criar(payload);
      setMessage(selectedId ? 'Tipo ISO atualizado com sucesso.' : 'Tipo ISO cadastrado com sucesso.');
      clearForm(); await load();
    } catch (reason) { setError(formatError(reason)); } finally { setBusy(false); }
  }

  async function toggle(row) {
    if (!canEdit || busy) return;
    setBusy(true); setError(''); setMessage('');
    try {
      await tipoIsoApi.alterarSituacao(row.id, !row.ativo, session?.usuario?.login || session?.login || 'PORTAL');
      setMessage(row.ativo ? 'Tipo ISO inativado.' : 'Tipo ISO reativado.'); await load();
    } catch (reason) { setError(formatError(reason)); } finally { setBusy(false); }
  }

  const columns = useMemo(() => [
    { key: 'isoId', label: 'ISO ID' }, { key: 'codigo', label: 'Código interno' }, { key: 'descricao', label: 'Descrição' },
    { key: 'grupoIsoCodigo', label: 'Grupo ISO' }, { key: 'categoria', label: 'Categoria' },
    { key: 'refrigerado', label: 'Reefer', render: (row) => row.refrigerado ? 'Sim' : 'Não' },
    { key: 'indicadorArquetipo', label: 'Arquétipo', render: (row) => row.indicadorArquetipo ? 'Sim' : 'Não' },
    { key: 'dependencias', label: 'Dependências' },
    { key: 'ativo', label: 'Situação', render: (row) => <StatusBadge value={row.ativo ? 'ATIVO' : 'INATIVO'} /> },
    { key: 'acoes', label: 'Ações', render: (row) => canEdit ? <button className="secondary" onClick={(event) => { event.stopPropagation(); toggle(row); }}>{row.ativo ? 'Inativar' : 'Reativar'}</button> : null }
  ], [canEdit, busy]);

  return <>
    <PageHeader eyebrow="Cadastros · Equipamentos" title="Tipos ISO de equipamentos" description="Manutenção mestre de ISO ID, Grupo ISO, arquétipo, dimensões, equivalência operacional e situação." actions={<button className="secondary" onClick={load}>Atualizar</button>} />
    <Message type="error">{error}</Message><Message type="success">{message}</Message>
    {!canEdit && <Message type="warning">Seu perfil pode consultar, mas somente ADMIN_PORTO pode criar, editar, inativar ou reativar.</Message>}
    <Section title="Manual da tela" description="Finalidade: manter os tipos ISO usados por inventário, gate, EDI, pátio, reservas e planejamento. Fluxo: pesquise, selecione, cadastre ou edite e depois inative quando não houver dependências. Campos: ISO ID é o identificador estrutural imutável; código interno é a referência local; Grupo ISO classifica o equipamento; arquétipo define o modelo de referência; equivalência operacional agrupa tipos intercambiáveis. Permissões: consulta para perfis autorizados e manutenção para ADMIN_PORTO. Estados: ativo, inativo e provisório EDI. Bloqueios: ISO inválido ou duplicado, Grupo ISO incompatível e registro em uso. Exemplo: 42R1, grupo REEFER, arquétipo 42R1. Atalhos: use os filtros e clique na linha para editar. Processo completo: Cadastros > Tipos ISO de equipamentos." />
    <Section title="Filtros"><div className="planner-selection-grid">
      <label className="field"><span>ISO ID, código ou descrição</span><input value={filters.termo} onChange={(e) => setFilters((c) => ({ ...c, termo: e.target.value }))} /></label>
      <label className="field"><span>Categoria</span><select value={filters.categoria} onChange={(e) => setFilters((c) => ({ ...c, categoria: e.target.value }))}><option value="">Todas</option>{['CONTEINER','CHASSI','CARRETA','ACESSORIO'].map((v) => <option key={v}>{v}</option>)}</select></label>
      <label className="field"><span>Situação</span><select value={filters.ativo} onChange={(e) => setFilters((c) => ({ ...c, ativo: e.target.value }))}><option value="">Todas</option><option value="true">Ativos</option><option value="false">Inativos</option></select></label>
      <label className="field"><span>Reefer</span><select value={filters.refrigerado} onChange={(e) => setFilters((c) => ({ ...c, refrigerado: e.target.value }))}><option value="">Todos</option><option value="true">Sim</option><option value="false">Não</option></select></label>
      <label className="field"><span>Arquétipo</span><select value={filters.arquetipo} onChange={(e) => setFilters((c) => ({ ...c, arquetipo: e.target.value }))}><option value="">Todos</option><option value="true">Sim</option><option value="false">Não</option></select></label>
    </div></Section>
    <Section title={selectedId ? 'Editar tipo ISO' : 'Cadastrar tipo ISO'}>
      <form className="planner-selection-grid" onSubmit={submit}>
        <label className="field"><span>ISO ID</span><input required pattern="[1-9A-Z][0-9A-Z]{3}" maxLength="4" value={form.isoId} onChange={(e) => setForm((c) => ({ ...c, isoId: e.target.value.toUpperCase() }))} disabled={!canEdit || busy || Boolean(selectedId)} /></label>
        <label className="field"><span>Código interno</span><input required maxLength="30" value={form.codigo} onChange={(e) => setForm((c) => ({ ...c, codigo: e.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Descrição</span><input required maxLength="120" value={form.descricao} onChange={(e) => setForm((c) => ({ ...c, descricao: e.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Categoria</span><select value={form.categoria} onChange={(e) => setForm((c) => ({ ...c, categoria: e.target.value }))} disabled={!canEdit || busy}>{['CONTEINER','CHASSI','CARRETA','ACESSORIO'].map((v) => <option key={v}>{v}</option>)}</select></label>
        <label className="field"><span>Grupo ISO</span><select value={form.grupoIsoId} onChange={(e) => setForm((c) => ({ ...c, grupoIsoId: e.target.value }))} disabled={!canEdit || busy}><option value="">Sem grupo</option>{groups.filter((g) => g.categoria === form.categoria && Boolean(g.refrigerado) === Boolean(form.refrigerado)).map((g) => <option key={g.id} value={g.id}>{g.codigo} · {g.descricao}</option>)}</select></label>
        <label className="field"><span>Arquétipo ISO</span><input maxLength="30" value={form.arquetipoIso} onChange={(e) => setForm((c) => ({ ...c, arquetipoIso: e.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
        {['comprimentoMm','larguraMm','alturaMm'].map((key) => <label className="field" key={key}><span>{key === 'comprimentoMm' ? 'Comprimento (mm)' : key === 'larguraMm' ? 'Largura (mm)' : 'Altura (mm)'}</span><input type="number" min="1" value={form[key]} onChange={(e) => setForm((c) => ({ ...c, [key]: e.target.value }))} disabled={!canEdit || busy} /></label>)}
        <label className="field"><span>Tara (kg)</span><input type="number" min="0" step="0.001" value={form.taraKg} onChange={(e) => setForm((c) => ({ ...c, taraKg: e.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Capacidade segura (kg)</span><input type="number" min="0" step="0.001" value={form.capacidadeKg} onChange={(e) => setForm((c) => ({ ...c, capacidadeKg: e.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Equivalência operacional</span><input maxLength="60" value={form.grupoEquivalencia} onChange={(e) => setForm((c) => ({ ...c, grupoEquivalencia: e.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
        <label className="field checkbox"><input type="checkbox" checked={form.refrigerado} onChange={(e) => setForm((c) => ({ ...c, refrigerado: e.target.checked, grupoIsoId: '' }))} disabled={!canEdit || busy} /><span>Reefer</span></label>
        <label className="field checkbox"><input type="checkbox" checked={form.indicadorArquetipo} onChange={(e) => setForm((c) => ({ ...c, indicadorArquetipo: e.target.checked }))} disabled={!canEdit || busy} /><span>É arquétipo</span></label>
        <label className="field checkbox"><input type="checkbox" checked={form.provisorioEdi} onChange={(e) => setForm((c) => ({ ...c, provisorioEdi: e.target.checked }))} disabled={!canEdit || busy} /><span>Provisório recebido por EDI</span></label>
        <div className="field"><span>Ações</span><div className="actions"><button disabled={!canEdit || busy}>{busy ? 'Salvando...' : selectedId ? 'Atualizar' : 'Cadastrar'}</button><button type="button" className="secondary" onClick={clearForm}>Limpar</button></div></div>
      </form>
    </Section>
    <Section title="Tipos ISO cadastrados">{loading ? <Loading /> : rows.length ? <DataTable rows={rows} rowKey="id" columns={columns} onRowClick={selectRow} /> : <EmptyState title="Nenhum tipo ISO encontrado" />}</Section>
  </>;
}
