import { useCallback, useEffect, useState } from 'react';
import { formatError, hasAnyRole, readSession, request } from '../api.js';
import { DataTable, EmptyState, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';

const EMPTY = { identificador: '', operadoraFerroviaria: '', descricao: '', ativo: true, observacoes: '', composicaoPadrao: [] };
const EMPTY_WAGON = { identificadorVagao: '', tipoVagao: '' };

function wagons(value) {
  return (Array.isArray(value) ? value : []).slice().sort((a, b) => Number(a.posicaoNoTrem) - Number(b.posicaoNoTrem)).map((item) => ({ identificadorVagao: item.identificadorVagao ?? '', tipoVagao: item.tipoVagao ?? '' }));
}

function Manual() {
  return <details className="contextual-manual"><summary aria-label="Abrir manual da tela">? Manual</summary><div>
    <h3>Cadastro mestre de trens e composições</h3>
    <p><strong>Finalidade:</strong> manter trens reutilizáveis e sua composição padrão, separados das visitas operacionais.</p>
    <p><strong>Fluxo:</strong> cadastre identificação, operadora e vagões; depois selecione o trem ao planejar uma visita ferroviária.</p>
    <p><strong>Campos:</strong> identificador e operadora formam a chave única; descrição e observações são informativas; ativo controla uso em novas visitas.</p>
    <p><strong>Permissões:</strong> ADMIN_PORTO cria, altera, inativa e exclui sem dependências; PLANEJADOR cria e altera; demais perfis consultam.</p>
    <p><strong>Estados:</strong> ativo ou inativo. Trem inativo permanece no histórico, mas não deve ser usado em novas visitas.</p>
    <p><strong>Bloqueios:</strong> duplicidade por operadora, vagão/posição repetidos e exclusão quando houver visita vinculada.</p>
    <p><strong>Exemplo:</strong> MRS-2048, operadora MRS, vagões GDT-001 a GDT-020 na ordem física.</p>
    <p><strong>Atalhos:</strong> use as setas para reordenar vagões e clique na linha para editar.</p>
    <p><a href="/home/ferrovia/visitas">Abrir processo completo de visitas ferroviárias</a></p>
  </div></details>;
}

export function RailRegistrationsPage({ session }) {
  const activeSession = session ?? readSession();
  const canEdit = hasAnyRole(activeSession, 'ADMIN_PORTO', 'PLANEJADOR');
  const canAdmin = hasAnyRole(activeSession, 'ADMIN_PORTO');
  const [rows, setRows] = useState([]); const [loading, setLoading] = useState(true); const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(EMPTY); const [wagon, setWagon] = useState(EMPTY_WAGON); const [busy, setBusy] = useState(false);
  const [error, setError] = useState(''); const [message, setMessage] = useState('');

  const load = useCallback(async () => { setLoading(true); setError(''); try { setRows(await request('/rail/ferrovia/trens')); } catch (reason) { setRows([]); setError(formatError(reason)); } finally { setLoading(false); } }, []);
  useEffect(() => { load(); }, [load]);
  function clear() { setSelectedId(null); setForm(EMPTY); setWagon(EMPTY_WAGON); setError(''); setMessage(''); }
  async function select(row) { setBusy(true); setError(''); try { const detail = await request(`/rail/ferrovia/trens/${row.id}`); setSelectedId(detail.id); setForm({ identificador: detail.identificador ?? '', operadoraFerroviaria: detail.operadoraFerroviaria ?? '', descricao: detail.descricao ?? '', ativo: detail.ativo !== false, observacoes: detail.observacoes ?? '', composicaoPadrao: wagons(detail.composicaoPadrao) }); } catch (reason) { setError(formatError(reason)); } finally { setBusy(false); } }
  function addWagon() { const id = wagon.identificadorVagao.trim().toUpperCase(); if (!id) return setError('Informe o identificador do vagão.'); if (form.composicaoPadrao.some((item) => item.identificadorVagao.toUpperCase() === id)) return setError('O vagão já faz parte desta composição.'); setForm((current) => ({ ...current, composicaoPadrao: [...current.composicaoPadrao, { identificadorVagao: id, tipoVagao: wagon.tipoVagao.trim().toUpperCase() }] })); setWagon(EMPTY_WAGON); setError(''); }
  function move(index, direction) { const target = index + direction; if (target < 0 || target >= form.composicaoPadrao.length) return; setForm((current) => { const next = [...current.composicaoPadrao]; [next[index], next[target]] = [next[target], next[index]]; return { ...current, composicaoPadrao: next }; }); }
  async function submit(event) { event.preventDefault(); if (!canEdit || busy) return; setBusy(true); setError(''); setMessage(''); const payload = { ...form, identificador: form.identificador.trim().toUpperCase(), operadoraFerroviaria: form.operadoraFerroviaria.trim(), descricao: form.descricao.trim() || null, observacoes: form.observacoes.trim() || null, composicaoPadrao: form.composicaoPadrao.map((item, index) => ({ posicaoNoTrem: index + 1, identificadorVagao: item.identificadorVagao, tipoVagao: item.tipoVagao || null })) }; try { await request(selectedId ? `/rail/ferrovia/trens/${selectedId}` : '/rail/ferrovia/trens', { method: selectedId ? 'PUT' : 'POST', body: payload }); setMessage(selectedId ? 'Trem atualizado com sucesso.' : 'Trem cadastrado com sucesso.'); clear(); await load(); } catch (reason) { setError(formatError(reason)); } finally { setBusy(false); } }
  async function toggle(row) { if (!canAdmin) return; setBusy(true); setError(''); try { await request(`/rail/ferrovia/trens/${row.id}/situacao?ativo=${!row.ativo}`, { method: 'PATCH' }); await load(); } catch (reason) { setError(formatError(reason)); } finally { setBusy(false); } }
  async function remove(row) { if (!canAdmin || !globalThis.confirm(`Excluir o trem ${row.identificador}?`)) return; setBusy(true); setError(''); try { await request(`/rail/ferrovia/trens/${row.id}`, { method: 'DELETE' }); await load(); } catch (reason) { setError(formatError(reason)); } finally { setBusy(false); } }

  return <>
    <PageHeader eyebrow="Cadastros · Ferrovia" title="Trens e composições" description="Mantenha o cadastro mestre reutilizável e a composição padrão dos trens." actions={<><Manual /><button className="secondary" onClick={load} disabled={loading || busy}>Atualizar</button></>} />
    <Message type="error">{error}</Message><Message type="success">{message}</Message>{!canEdit && <Message type="warning">Seu perfil possui acesso somente para consulta.</Message>}
    <Section title={selectedId ? 'Editar trem mestre' : 'Cadastrar trem mestre'} description="As visitas copiarão estes dados e preservarão o snapshot histórico."><form onSubmit={submit}>
      <div className="planner-selection-grid">
        <label className="field"><span>Identificador</span><input required maxLength="40" value={form.identificador} onChange={(e) => setForm((v) => ({ ...v, identificador: e.target.value }))} disabled={!canEdit || busy} placeholder="Ex.: MRS-2048" /></label>
        <label className="field"><span>Operadora ferroviária</span><input required maxLength="80" value={form.operadoraFerroviaria} onChange={(e) => setForm((v) => ({ ...v, operadoraFerroviaria: e.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Descrição</span><input maxLength="120" value={form.descricao} onChange={(e) => setForm((v) => ({ ...v, descricao: e.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Situação</span><select value={String(form.ativo)} onChange={(e) => setForm((v) => ({ ...v, ativo: e.target.value === 'true' }))} disabled={!canEdit || busy}><option value="true">Ativo</option><option value="false">Inativo</option></select></label>
      </div>
      <label className="field"><span>Observações</span><textarea maxLength="500" value={form.observacoes} onChange={(e) => setForm((v) => ({ ...v, observacoes: e.target.value }))} disabled={!canEdit || busy} /></label>
      <div className="section-heading"><div><h3>Composição padrão</h3><p>Inclua os vagões na ordem física que será copiada para novas visitas.</p></div></div>
      <div className="planner-selection-grid"><label className="field"><span>Identificador do vagão</span><input maxLength="35" value={wagon.identificadorVagao} onChange={(e) => setWagon((v) => ({ ...v, identificadorVagao: e.target.value }))} disabled={!canEdit || busy} /></label><label className="field"><span>Tipo do vagão</span><input maxLength="40" value={wagon.tipoVagao} onChange={(e) => setWagon((v) => ({ ...v, tipoVagao: e.target.value }))} disabled={!canEdit || busy} /></label><div className="field"><span>Ação</span><button type="button" className="secondary" onClick={addWagon} disabled={!canEdit || busy}>Adicionar vagão</button></div></div>
      {form.composicaoPadrao.length ? <DataTable rows={form.composicaoPadrao.map((item, index) => ({ ...item, posicaoNoTrem: index + 1, index }))} rowKey={(row) => `${row.identificadorVagao}-${row.index}`} columns={[{ key: 'posicaoNoTrem', label: 'Posição' }, { key: 'identificadorVagao', label: 'Vagão' }, { key: 'tipoVagao', label: 'Tipo', render: (row) => row.tipoVagao || '—' }, { key: 'acoes', label: 'Ações', render: (row) => <div className="actions"><button type="button" className="secondary small" onClick={() => move(row.index, -1)} disabled={!canEdit || busy || row.index === 0}>↑</button><button type="button" className="secondary small" onClick={() => move(row.index, 1)} disabled={!canEdit || busy || row.index === form.composicaoPadrao.length - 1}>↓</button><button type="button" className="danger small" onClick={() => setForm((v) => ({ ...v, composicaoPadrao: v.composicaoPadrao.filter((_, i) => i !== row.index) }))} disabled={!canEdit || busy}>Remover</button></div> }]} /> : <EmptyState title="Nenhum vagão adicionado" description="A composição padrão é opcional." />}
      <div className="actions form-actions"><button type="submit" disabled={!canEdit || busy}>{busy ? 'Salvando...' : selectedId ? 'Atualizar cadastro' : 'Cadastrar trem'}</button><button type="button" className="secondary" onClick={clear} disabled={busy}>Limpar</button></div>
    </form></Section>
    <Section title="Trens cadastrados" description="Clique na linha para editar; ADMIN_PORTO também pode inativar ou excluir quando não houver dependências.">{loading ? <Loading label="Carregando trens..." /> : rows.length ? <DataTable rows={rows} rowKey="id" onRowClick={select} columns={[{ key: 'identificador', label: 'Trem' }, { key: 'operadoraFerroviaria', label: 'Operadora' }, { key: 'descricao', label: 'Descrição', render: (row) => row.descricao || '—' }, { key: 'ativo', label: 'Situação', render: (row) => <StatusBadge value={row.ativo ? 'ATIVO' : 'INATIVO'} /> }, { key: 'acoes', label: 'Ações', render: (row) => <div className="actions"><button type="button" className="secondary small" onClick={(e) => { e.stopPropagation(); toggle(row); }} disabled={!canAdmin || busy}>{row.ativo ? 'Inativar' : 'Ativar'}</button><button type="button" className="danger small" onClick={(e) => { e.stopPropagation(); remove(row); }} disabled={!canAdmin || busy}>Excluir</button></div> }]} /> : <EmptyState title="Nenhum trem cadastrado" description="Cadastre o primeiro trem mestre usando o formulário acima." />}</Section>
  </>;
}
