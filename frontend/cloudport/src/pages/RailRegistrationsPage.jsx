import { useCallback, useEffect, useState } from 'react';
import { formatError, hasAnyRole, normalizePage, readSession, request } from '../api.js';
import { DataTable, EmptyState, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';

const EMPTY = { identificadorTrem: '', operadoraFerroviaria: '', horaChegadaPrevista: '', horaPartidaPrevista: '', statusVisita: 'PLANEJADO', listaVagoes: [] };
const EMPTY_WAGON = { identificadorVagao: '', tipoVagao: '' };
const STATUS = [['PLANEJADO', 'Planejado'], ['CHEGOU', 'Chegou'], ['PROCESSANDO', 'Processando'], ['CONCLUIDO', 'Concluído'], ['PARTIU', 'Partiu']];

function localDateTime(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 16);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}

function wagons(value) {
  return (Array.isArray(value) ? value : []).slice().sort((a, b) => Number(a.posicaoNoTrem) - Number(b.posicaoNoTrem)).map((item) => ({ identificadorVagao: item.identificadorVagao ?? '', tipoVagao: item.tipoVagao ?? '' }));
}

export function RailRegistrationsPage({ session }) {
  const activeSession = session ?? readSession();
  const canEdit = hasAnyRole(activeSession, 'ADMIN_PORTO', 'PLANEJADOR');
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [wagon, setWagon] = useState(EMPTY_WAGON);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try { setRows(normalizePage(await request('/rail/ferrovia/visitas', { query: { dias: 365 } }))); }
    catch (reason) { setRows([]); setError(formatError(reason)); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  function clear() { setSelectedId(null); setForm(EMPTY); setWagon(EMPTY_WAGON); setError(''); setMessage(''); }

  async function select(row) {
    setBusy(true); setError(''); setMessage('');
    try {
      const detail = await request(`/rail/ferrovia/visitas/${row.id}`);
      setSelectedId(detail.id);
      setForm({ identificadorTrem: detail.identificadorTrem ?? '', operadoraFerroviaria: detail.operadoraFerroviaria ?? '', horaChegadaPrevista: localDateTime(detail.horaChegadaPrevista), horaPartidaPrevista: localDateTime(detail.horaPartidaPrevista), statusVisita: detail.statusVisita ?? 'PLANEJADO', listaVagoes: wagons(detail.listaVagoes) });
    } catch (reason) { setError(formatError(reason)); }
    finally { setBusy(false); }
  }

  function addWagon() {
    const id = wagon.identificadorVagao.trim().toUpperCase();
    if (!id) return setError('Informe o identificador do vagão.');
    if (form.listaVagoes.some((item) => item.identificadorVagao.toUpperCase() === id)) return setError('O vagão já faz parte desta composição.');
    setForm((current) => ({ ...current, listaVagoes: [...current.listaVagoes, { identificadorVagao: id, tipoVagao: wagon.tipoVagao.trim().toUpperCase() }] }));
    setWagon(EMPTY_WAGON); setError('');
  }

  function move(index, direction) {
    const target = index + direction;
    if (target < 0 || target >= form.listaVagoes.length) return;
    setForm((current) => { const next = [...current.listaVagoes]; [next[index], next[target]] = [next[target], next[index]]; return { ...current, listaVagoes: next }; });
  }

  async function submit(event) {
    event.preventDefault();
    if (!canEdit || busy) return;
    if (new Date(form.horaPartidaPrevista) <= new Date(form.horaChegadaPrevista)) return setError('A partida prevista deve ser posterior à chegada prevista.');
    setBusy(true); setError(''); setMessage('');
    const payload = { ...form, identificadorTrem: form.identificadorTrem.trim().toUpperCase(), operadoraFerroviaria: form.operadoraFerroviaria.trim(), listaDescarga: [], listaCarga: [], listaVagoes: form.listaVagoes.map((item, index) => ({ posicaoNoTrem: index + 1, identificadorVagao: item.identificadorVagao, tipoVagao: item.tipoVagao || null })) };
    try {
      await request(selectedId ? `/rail/ferrovia/visitas/${selectedId}` : '/rail/ferrovia/visitas', { method: selectedId ? 'PUT' : 'POST', body: payload });
      setMessage(selectedId ? 'Trem e composição atualizados com sucesso.' : 'Trem e composição cadastrados com sucesso.');
      setSelectedId(null); setForm(EMPTY); setWagon(EMPTY_WAGON); await load();
    } catch (reason) { setError(formatError(reason)); }
    finally { setBusy(false); }
  }

  return <>
    <PageHeader eyebrow="Cadastros · Ferrovia" title="Trens e composições" description="Cadastre a visita ferroviária, os horários previstos e a ordem física dos vagões da composição." actions={<button className="secondary" onClick={load} disabled={loading || busy}>Atualizar</button>} />
    <Message type="error">{error}</Message><Message type="success">{message}</Message>
    {!canEdit && <Message type="warning">Seu perfil pode consultar trens e composições, mas não pode alterar o cadastro.</Message>}
    <Section title={selectedId ? 'Editar trem e composição' : 'Cadastrar trem e composição'} description="A ordem dos vagões será persistida conforme a lista abaixo.">
      <form onSubmit={submit}>
        <div className="planner-selection-grid">
          <label className="field"><span>Identificador do trem</span><input required maxLength="40" value={form.identificadorTrem} onChange={(e) => setForm((v) => ({ ...v, identificadorTrem: e.target.value }))} disabled={!canEdit || busy} placeholder="Ex.: MRS-2048" /></label>
          <label className="field"><span>Operadora ferroviária</span><input required maxLength="80" value={form.operadoraFerroviaria} onChange={(e) => setForm((v) => ({ ...v, operadoraFerroviaria: e.target.value }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Chegada prevista</span><input required type="datetime-local" value={form.horaChegadaPrevista} onChange={(e) => setForm((v) => ({ ...v, horaChegadaPrevista: e.target.value }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Partida prevista</span><input required type="datetime-local" value={form.horaPartidaPrevista} onChange={(e) => setForm((v) => ({ ...v, horaPartidaPrevista: e.target.value }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Status</span><select value={form.statusVisita} onChange={(e) => setForm((v) => ({ ...v, statusVisita: e.target.value }))} disabled={!canEdit || busy}>{STATUS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        </div>
        <div className="section-heading"><div><h3>Composição de vagões</h3><p>Inclua os vagões e ajuste a sequência com as setas.</p></div></div>
        <div className="planner-selection-grid">
          <label className="field"><span>Identificador do vagão</span><input maxLength="35" value={wagon.identificadorVagao} onChange={(e) => setWagon((v) => ({ ...v, identificadorVagao: e.target.value }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Tipo do vagão</span><input maxLength="40" value={wagon.tipoVagao} onChange={(e) => setWagon((v) => ({ ...v, tipoVagao: e.target.value }))} disabled={!canEdit || busy} /></label>
          <div className="field"><span>Ação</span><button type="button" className="secondary" onClick={addWagon} disabled={!canEdit || busy}>Adicionar vagão</button></div>
        </div>
        {form.listaVagoes.length ? <DataTable rows={form.listaVagoes.map((item, index) => ({ ...item, posicaoNoTrem: index + 1, index }))} rowKey={(row) => `${row.identificadorVagao}-${row.index}`} columns={[
          { key: 'posicaoNoTrem', label: 'Posição' }, { key: 'identificadorVagao', label: 'Vagão' }, { key: 'tipoVagao', label: 'Tipo', render: (row) => row.tipoVagao || '—' },
          { key: 'acoes', label: 'Ações', render: (row) => <div className="actions"><button type="button" className="secondary small" onClick={() => move(row.index, -1)} disabled={!canEdit || busy || row.index === 0}>↑</button><button type="button" className="secondary small" onClick={() => move(row.index, 1)} disabled={!canEdit || busy || row.index === form.listaVagoes.length - 1}>↓</button><button type="button" className="danger small" onClick={() => setForm((v) => ({ ...v, listaVagoes: v.listaVagoes.filter((_, i) => i !== row.index) }))} disabled={!canEdit || busy}>Remover</button></div> }
        ]} /> : <EmptyState title="Nenhum vagão adicionado" description="A composição pode ser salva sem vagões e completada posteriormente." />}
        <div className="actions form-actions"><button type="submit" disabled={!canEdit || busy}>{busy ? 'Salvando...' : selectedId ? 'Atualizar cadastro' : 'Cadastrar trem'}</button><button type="button" className="secondary" onClick={clear} disabled={busy}>Limpar</button></div>
      </form>
    </Section>
    <Section title="Trens cadastrados" description="Selecione uma linha para editar os dados e a composição.">
      {loading ? <Loading label="Carregando trens..." /> : rows.length ? <DataTable rows={rows} rowKey="id" onRowClick={select} columns={[
        { key: 'identificadorTrem', label: 'Trem' }, { key: 'operadoraFerroviaria', label: 'Operadora' }, { key: 'horaChegadaPrevista', label: 'Chegada', render: (row) => row.horaChegadaPrevista ? new Date(row.horaChegadaPrevista).toLocaleString('pt-BR') : '—' }, { key: 'statusVisita', label: 'Status', render: (row) => <StatusBadge value={row.statusVisita} /> }
      ]} /> : <EmptyState title="Nenhum trem cadastrado" description="Cadastre a primeira visita ferroviária usando o formulário acima." />}
    </Section>
  </>;
}
