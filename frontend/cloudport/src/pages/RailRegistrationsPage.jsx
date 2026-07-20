import { useCallback, useEffect, useState } from 'react';
import { formatError, hasAnyRole, normalizePage, request } from '../api.js';
import { DataTable, EmptyState, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';

const EMPTY_FORM = {
  identificadorTrem: '',
  operadoraFerroviaria: '',
  horaChegadaPrevista: '',
  horaPartidaPrevista: '',
  statusVisita: 'PLANEJADA',
  listaVagoes: []
};

const EMPTY_WAGON = { identificadorVagao: '', tipoVagao: '' };

function toLocalInput(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 16);
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
}

function normalizeWagons(wagons) {
  return (Array.isArray(wagons) ? wagons : [])
    .slice()
    .sort((a, b) => Number(a.posicaoNoTrem) - Number(b.posicaoNoTrem))
    .map((wagon) => ({
      identificadorVagao: wagon.identificadorVagao ?? '',
      tipoVagao: wagon.tipoVagao ?? ''
    }));
}

export function RailRegistrationsPage({ session }) {
  const canEdit = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR');
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [wagon, setWagon] = useState(EMPTY_WAGON);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setRows(normalizePage(await request('/rail/ferrovia/visitas', { query: { dias: 365 } })));
    } catch (reason) {
      setRows([]);
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  function clearForm() {
    setSelectedId(null);
    setForm(EMPTY_FORM);
    setWagon(EMPTY_WAGON);
    setError('');
    setMessage('');
  }

  async function selectTrain(row) {
    setBusy(true);
    setError('');
    setMessage('');
    try {
      const detail = await request(`/rail/ferrovia/visitas/${row.id}`);
      setSelectedId(detail.id);
      setForm({
        identificadorTrem: detail.identificadorTrem ?? '',
        operadoraFerroviaria: detail.operadoraFerroviaria ?? '',
        horaChegadaPrevista: toLocalInput(detail.horaChegadaPrevista),
        horaPartidaPrevista: toLocalInput(detail.horaPartidaPrevista),
        statusVisita: detail.statusVisita ?? 'PLANEJADA',
        listaVagoes: normalizeWagons(detail.listaVagoes)
      });
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  function addWagon() {
    const identifier = wagon.identificadorVagao.trim().toUpperCase();
    if (!identifier) {
      setError('Informe o identificador do vagão.');
      return;
    }
    if (form.listaVagoes.some((item) => item.identificadorVagao.toUpperCase() === identifier)) {
      setError('O vagão já faz parte desta composição.');
      return;
    }
    setForm((current) => ({
      ...current,
      listaVagoes: [...current.listaVagoes, { identificadorVagao: identifier, tipoVagao: wagon.tipoVagao.trim().toUpperCase() }]
    }));
    setWagon(EMPTY_WAGON);
    setError('');
  }

  function removeWagon(index) {
    setForm((current) => ({ ...current, listaVagoes: current.listaVagoes.filter((_, itemIndex) => itemIndex !== index) }));
  }

  function moveWagon(index, direction) {
    const target = index + direction;
    if (target < 0 || target >= form.listaVagoes.length) return;
    setForm((current) => {
      const next = [...current.listaVagoes];
      [next[index], next[target]] = [next[target], next[index]];
      return { ...current, listaVagoes: next };
    });
  }

  async function submit(event) {
    event.preventDefault();
    if (!canEdit || busy) return;
    if (new Date(form.horaPartidaPrevista) <= new Date(form.horaChegadaPrevista)) {
      setError('A partida prevista deve ser posterior à chegada prevista.');
      return;
    }
    setBusy(true);
    setError('');
    setMessage('');
    const payload = {
      ...form,
      identificadorTrem: form.identificadorTrem.trim().toUpperCase(),
      operadoraFerroviaria: form.operadoraFerroviaria.trim(),
      listaDescarga: [],
      listaCarga: [],
      listaVagoes: form.listaVagoes.map((item, index) => ({
        posicaoNoTrem: index + 1,
        identificadorVagao: item.identificadorVagao,
        tipoVagao: item.tipoVagao || null
      }))
    };
    try {
      if (selectedId) {
        await request(`/rail/ferrovia/visitas/${selectedId}`, { method: 'PUT', body: payload });
        setMessage('Trem e composição atualizados com sucesso.');
      } else {
        await request('/rail/ferrovia/visitas', { method: 'POST', body: payload });
        setMessage('Trem e composição cadastrados com sucesso.');
      }
      await load();
      setSelectedId(null);
      setForm(EMPTY_FORM);
      setWagon(EMPTY_WAGON);
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  return <>
    <PageHeader
      eyebrow="Cadastros · Ferrovia"
      title="Trens e composições"
      description="Cadastre a visita ferroviária, os horários previstos e a ordem física dos vagões da composição."
      actions={<button className="secondary" onClick={load} disabled={loading || busy}>Atualizar</button>}
    />
    <Message type="error">{error}</Message>
    <Message type="success">{message}</Message>
    {!canEdit && <Message type="warning">Seu perfil pode consultar trens e composições, mas não pode alterar o cadastro.</Message>}

    <Section title={selectedId ? 'Editar trem e composição' : 'Cadastrar trem e composição'} description="A ordem dos vagões será persistida conforme a lista abaixo.">
      <form onSubmit={submit}>
        <div className="planner-selection-grid">
          <label className="field"><span>Identificador do trem</span><input required maxLength="40" value={form.identificadorTrem} onChange={(event) => setForm((current) => ({ ...current, identificadorTrem: event.target.value }))} disabled={!canEdit || busy} placeholder="Ex.: MRS-2048" /></label>
          <label className="field"><span>Operadora ferroviária</span><input required maxLength="80" value={form.operadoraFerroviaria} onChange={(event) => setForm((current) => ({ ...current, operadoraFerroviaria: event.target.value }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Chegada prevista</span><input required type="datetime-local" value={form.horaChegadaPrevista} onChange={(event) => setForm((current) => ({ ...current, horaChegadaPrevista: event.target.value }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Partida prevista</span><input required type="datetime-local" value={form.horaPartidaPrevista} onChange={(event) => setForm((current) => ({ ...current, horaPartidaPrevista: event.target.value }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Status</span><select value={form.statusVisita} onChange={(event) => setForm((current) => ({ ...current, statusVisita: event.target.value }))} disabled={!canEdit || busy}><option value="PLANEJADA">Planejada</option><option value="A_CAMINHO">A caminho</option><option value="RECEBIDA">Recebida</option><option value="EM_OPERACAO">Em operação</option><option value="CONCLUIDA">Concluída</option><option value="CANCELADA">Cancelada</option></select></label>
        </div>

        <div className="section-heading"><div><h3>Composição de vagões</h3><p>Inclua os vagões e ajuste a sequência com as setas.</p></div></div>
        <div className="planner-selection-grid">
          <label className="field"><span>Identificador do vagão</span><input maxLength="35" value={wagon.identificadorVagao} onChange={(event) => setWagon((current) => ({ ...current, identificadorVagao: event.target.value }))} disabled={!canEdit || busy} placeholder="Ex.: GDT-00123" /></label>
          <label className="field"><span>Tipo do vagão</span><input maxLength="40" value={wagon.tipoVagao} onChange={(event) => setWagon((current) => ({ ...current, tipoVagao: event.target.value }))} disabled={!canEdit || busy} placeholder="Ex.: PLATAFORMA" /></label>
          <div className="field"><span>Ação</span><button type="button" className="secondary" onClick={addWagon} disabled={!canEdit || busy}>Adicionar vagão</button></div>
        </div>

        {form.listaVagoes.length ? <DataTable rows={form.listaVagoes.map((item, index) => ({ ...item, posicaoNoTrem: index + 1, index }))} rowKey={(row) => `${row.identificadorVagao}-${row.index}`} columns={[
          { key: 'posicaoNoTrem', label: 'Posição' },
          { key: 'identificadorVagao', label: 'Vagão' },
          { key: 'tipoVagao', label: 'Tipo', render: (row) => row.tipoVagao || '—' },
          { key: 'acoes', label: 'Ações', render: (row) => <div className="actions"><button type="button" className="secondary small" onClick={() => moveWagon(row.index, -1)} disabled={!canEdit || busy || row.index === 0} aria-label="Mover vagão para cima">↑</button><button type="button" className="secondary small" onClick={() => moveWagon(row.index, 1)} disabled={!canEdit || busy || row.index === form.listaVagoes.length - 1} aria-label="Mover vagão para baixo">↓</button><button type="button" className="danger small" onClick={() => removeWagon(row.index)} disabled={!canEdit || busy}>Remover</button></div> }
        ]} /> : <EmptyState title="Nenhum vagão adicionado" description="A composição pode ser salva sem vagões e completada posteriormente." />}

        <div className="actions form-actions"><button type="submit" disabled={!canEdit || busy}>{busy ? 'Salvando...' : selectedId ? 'Atualizar cadastro' : 'Cadastrar trem'}</button><button type="button" className="secondary" onClick={clearForm} disabled={busy}>Limpar</button></div>
      </form>
    </Section>

    <Section title="Trens cadastrados" description="Selecione uma linha para editar os dados e a composição.">
      {loading ? <Loading label="Carregando trens..." /> : rows.length ? <DataTable rows={rows} rowKey="id" onRowClick={selectTrain} columns={[
        { key: 'identificadorTrem', label: 'Trem' },
        { key: 'operadoraFerroviaria', label: 'Operadora' },
        { key: 'horaChegadaPrevista', label: 'Chegada', render: (row) => row.horaChegadaPrevista ? new Date(row.horaChegadaPrevista).toLocaleString('pt-BR') : '—' },
        { key: 'statusVisita', label: 'Status', render: (row) => <StatusBadge value={row.statusVisita} /> },
        { key: 'listaVagoes', label: 'Vagões', render: (row) => Array.isArray(row.listaVagoes) ? row.listaVagoes.length : 0 }
      ]} /> : <EmptyState title="Nenhum trem cadastrado" description="Cadastre a primeira visita ferroviária usando o formulário acima." />}
    </Section>
  </>;
}
