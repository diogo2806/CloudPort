import { useCallback, useEffect, useState } from 'react';
import { formatError, hasAnyRole, readSession } from '../api.js';
import { DataTable, EmptyState, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';
import { railApi } from '../railApi.js';

const EMPTY = { identificador: '', operadoraFerroviaria: '', descricao: '', ativo: true, observacoes: '', composicaoPadrao: [] };
const EMPTY_WAGON = { identificadorVagao: '', tipoVagao: '' };

export function RailRegistrationsPage({ session }) {
  const activeSession = session ?? readSession();
  const canEdit = hasAnyRole(activeSession, 'ADMIN_PORTO', 'PLANEJADOR');
  const [rows, setRows] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [wagon, setWagon] = useState(EMPTY_WAGON);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [manualOpen, setManualOpen] = useState(false);

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try { setRows(await railApi.listarTrens()); }
    catch (reason) { setRows([]); setError(formatError(reason)); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  function clear() { setSelectedId(null); setForm(EMPTY); setWagon(EMPTY_WAGON); setError(''); setMessage(''); }

  async function select(row) {
    setBusy(true); setError('');
    try {
      const detail = await railApi.consultarTrem(row.id);
      setSelectedId(detail.id);
      setForm({ identificador: detail.identificador ?? '', operadoraFerroviaria: detail.operadoraFerroviaria ?? '', descricao: detail.descricao ?? '', ativo: detail.ativo !== false, observacoes: detail.observacoes ?? '', composicaoPadrao: detail.composicaoPadrao ?? [] });
    } catch (reason) { setError(formatError(reason)); }
    finally { setBusy(false); }
  }

  function addWagon() {
    const id = wagon.identificadorVagao.trim().toUpperCase();
    if (!id) return setError('Informe o identificador do vagão.');
    if (form.composicaoPadrao.some((item) => item.identificadorVagao?.toUpperCase() === id)) return setError('O vagão já faz parte da composição padrão.');
    setForm((current) => ({ ...current, composicaoPadrao: [...current.composicaoPadrao, { identificadorVagao: id, tipoVagao: wagon.tipoVagao.trim().toUpperCase() }] }));
    setWagon(EMPTY_WAGON); setError('');
  }

  function move(index, direction) {
    const target = index + direction;
    if (target < 0 || target >= form.composicaoPadrao.length) return;
    setForm((current) => { const next = [...current.composicaoPadrao]; [next[index], next[target]] = [next[target], next[index]]; return { ...current, composicaoPadrao: next }; });
  }

  async function submit(event) {
    event.preventDefault();
    if (!canEdit || busy) return;
    setBusy(true); setError(''); setMessage('');
    try {
      if (selectedId) await railApi.atualizarTrem(selectedId, form); else await railApi.criarTrem(form);
      setMessage(selectedId ? 'Cadastro mestre atualizado com sucesso.' : 'Trem cadastrado com sucesso.');
      clear(); await load();
    } catch (reason) { setError(formatError(reason)); }
    finally { setBusy(false); }
  }

  return <>
    <PageHeader eyebrow="Cadastros · Ferrovia" title="Trens e composições" description="Mantenha trens reutilizáveis e suas composições padrão, separados das visitas ferroviárias." actions={<><button className="secondary" onClick={() => setManualOpen((v) => !v)}>Manual</button><button className="secondary" onClick={load} disabled={loading || busy}>Atualizar</button></>} />
    <Message type="error">{error}</Message><Message type="success">{message}</Message>
    {!canEdit && <Message type="warning">Seu perfil possui acesso somente para consulta.</Message>}
    {manualOpen && <Section title="Manual da tela" description="Processo completo: Cadastros > Trens e composições > Ferrovia > Visitas.">
      <p><strong>Finalidade:</strong> manter dados reutilizáveis do trem e sua composição padrão.</p>
      <p><strong>Fluxo operacional:</strong> cadastre o trem, ordene os vagões, salve e depois selecione-o ao planejar uma visita.</p>
      <p><strong>Campos:</strong> identificador e operadora formam a chave única; descrição e observações contextualizam; ativo controla uso em novas visitas.</p>
      <p><strong>Permissões:</strong> ADMIN_PORTO e PLANEJADOR alteram; demais perfis consultam conforme autorização.</p>
      <p><strong>Estados:</strong> ativo ou inativo. Trem inativo permanece no histórico, mas não deve ser escolhido em novas visitas.</p>
      <p><strong>Bloqueios:</strong> identificador duplicado por operadora, vagão repetido e campos obrigatórios ausentes.</p>
      <p><strong>Exemplo:</strong> MRS-2048, operadora MRS, composição padrão com vagões GDT-001 e GDT-002.</p>
      <p><strong>Atalhos:</strong> clique em uma linha para editar; use as setas para reordenar vagões.</p>
    </Section>}
    <Section title={selectedId ? 'Editar cadastro mestre' : 'Cadastrar trem'} description="Os horários e o status operacional pertencem à visita ferroviária, não a este cadastro.">
      <form onSubmit={submit}>
        <div className="planner-selection-grid">
          <label className="field"><span>Identificador</span><input required maxLength="40" value={form.identificador} onChange={(e) => setForm((v) => ({ ...v, identificador: e.target.value }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Operadora ferroviária</span><input required maxLength="80" value={form.operadoraFerroviaria} onChange={(e) => setForm((v) => ({ ...v, operadoraFerroviaria: e.target.value }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Nome operacional</span><input maxLength="120" value={form.descricao} onChange={(e) => setForm((v) => ({ ...v, descricao: e.target.value }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Situação</span><select value={form.ativo ? 'ATIVO' : 'INATIVO'} onChange={(e) => setForm((v) => ({ ...v, ativo: e.target.value === 'ATIVO' }))} disabled={!canEdit || busy}><option value="ATIVO">Ativo</option><option value="INATIVO">Inativo</option></select></label>
          <label className="field"><span>Observações</span><textarea maxLength="500" value={form.observacoes} onChange={(e) => setForm((v) => ({ ...v, observacoes: e.target.value }))} disabled={!canEdit || busy} /></label>
        </div>
        <div className="section-heading"><div><h3>Composição padrão</h3><p>Inclua os vagões e ajuste a sequência.</p></div></div>
        <div className="planner-selection-grid">
          <label className="field"><span>Identificador do vagão</span><input maxLength="35" value={wagon.identificadorVagao} onChange={(e) => setWagon((v) => ({ ...v, identificadorVagao: e.target.value }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Tipo do vagão</span><input maxLength="40" value={wagon.tipoVagao} onChange={(e) => setWagon((v) => ({ ...v, tipoVagao: e.target.value }))} disabled={!canEdit || busy} /></label>
          <div className="field"><span>Ação</span><button type="button" className="secondary" onClick={addWagon} disabled={!canEdit || busy}>Adicionar vagão</button></div>
        </div>
        {form.composicaoPadrao.length ? <DataTable rows={form.composicaoPadrao.map((item, index) => ({ ...item, posicaoNoTrem: index + 1, index }))} rowKey={(row) => `${row.identificadorVagao}-${row.index}`} columns={[{ key: 'posicaoNoTrem', label: 'Posição' }, { key: 'identificadorVagao', label: 'Vagão' }, { key: 'tipoVagao', label: 'Tipo', render: (row) => row.tipoVagao || '—' }, { key: 'acoes', label: 'Ações', render: (row) => <div className="actions"><button type="button" className="secondary small" onClick={() => move(row.index, -1)} disabled={row.index === 0}>↑</button><button type="button" className="secondary small" onClick={() => move(row.index, 1)} disabled={row.index === form.composicaoPadrao.length - 1}>↓</button><button type="button" className="danger small" onClick={() => setForm((v) => ({ ...v, composicaoPadrao: v.composicaoPadrao.filter((_, i) => i !== row.index) }))}>Remover</button></div> }]} /> : <EmptyState title="Nenhum vagão adicionado" description="A composição padrão é opcional." />}
        <div className="actions form-actions"><button type="submit" disabled={!canEdit || busy}>{busy ? 'Salvando...' : selectedId ? 'Atualizar cadastro' : 'Cadastrar trem'}</button><button type="button" className="secondary" onClick={clear} disabled={busy}>Limpar</button></div>
      </form>
    </Section>
    <Section title="Trens cadastrados" description="Clique em uma linha para consultar ou editar.">
      {loading ? <Loading label="Carregando trens..." /> : rows.length ? <DataTable rows={rows} rowKey="id" onRowClick={select} columns={[{ key: 'identificador', label: 'Trem' }, { key: 'operadoraFerroviaria', label: 'Operadora' }, { key: 'descricao', label: 'Nome operacional', render: (row) => row.descricao || '—' }, { key: 'ativo', label: 'Situação', render: (row) => <StatusBadge value={row.ativo ? 'ATIVO' : 'INATIVO'} /> }, { key: 'composicaoPadrao', label: 'Vagões', render: (row) => row.composicaoPadrao?.length ?? 0 }]} /> : <EmptyState title="Nenhum trem cadastrado" description="Cadastre o primeiro trem mestre usando o formulário acima." />}
    </Section>
  </>;
}
