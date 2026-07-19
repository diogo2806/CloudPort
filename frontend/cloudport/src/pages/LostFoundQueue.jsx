import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, readSession } from '../api.js';
import { DataTable, EmptyState, Loading, Message, Section, StatusBadge } from '../components.jsx';
import { inventoryLostFoundApi } from '../inventoryLostFoundApi.js';
import './LostFoundQueue.css';

const EMPTY_FORM = {
  identificacaoLida: '',
  tipoCaso: 'SEM_REGISTRO',
  evidencia: ''
};

const TIPO_CASO_LABELS = {
  SEM_REGISTRO: 'Sem registro',
  NAO_LOCALIZADA: 'Não localizada',
  TBD: 'Não identificada'
};

function tipoCasoLabel(value) {
  return TIPO_CASO_LABELS[value] || value || '—';
}

function dateTime(value) {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleString('pt-BR');
}

export function LostFoundQueue() {
  const [rows, setRows] = useState([]);
  const [selected, setSelected] = useState(null);
  const [filter, setFilter] = useState('');
  const [status, setStatus] = useState('');
  const [form, setForm] = useState(EMPTY_FORM);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await inventoryLostFoundApi.listar();
      setRows(Array.isArray(response) ? response : []);
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível carregar as ocorrências de unidades de carga.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = useMemo(() => {
    const query = filter.trim().toLowerCase();
    return rows.filter((item) => (!status || item.status === status)
      && (!query || `${item.identificacaoLida} ${item.unidadeIdentificacao} ${item.tipoCaso} ${item.responsavel}`.toLowerCase().includes(query)));
  }, [filter, rows, status]);

  async function execute(action, message) {
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      const updated = await action();
      setSelected(updated);
      setSuccess(message);
      await load();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  function submit(event) {
    event.preventDefault();
    execute(() => inventoryLostFoundApi.abrir({
      ...form,
      operador: readSession()?.nome || 'operador'
    }), 'Caso aberto e incluído na fila operacional.');
    setForm(EMPTY_FORM);
  }

  function investigate() {
    const responsavel = window.prompt('Responsável pela investigação:', selected?.responsavel || '');
    if (!responsavel) return;
    const evidencia = window.prompt('Evidência ou observação inicial:') || '';
    execute(() => inventoryLostFoundApi.investigar(selected.id, { responsavel, evidencia }), 'Investigação iniciada.');
  }

  function associate() {
    const unidadeId = Number(window.prompt('ID da unidade canônica a associar:'));
    if (!Number.isInteger(unidadeId) || unidadeId < 1) return;
    const evidencia = window.prompt('Evidência da associação:') || '';
    execute(() => inventoryLostFoundApi.associar(selected.id, { unidadeId, evidencia }), 'Caso associado à unidade canônica.');
  }

  function decide(operation, message, label) {
    const decisao = window.prompt(label);
    if (!decisao) return;
    execute(() => operation(selected.id, decisao), message);
  }

  return <Section title="Unidades de carga não localizadas" description="Fila persistente de ocorrências envolvendo unidades sem registro, não localizadas ou temporariamente não identificadas.">
    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>
    <form className="lost-found-form" onSubmit={submit}>
      <label>Identificação lida<input required maxLength="40" value={form.identificacaoLida} onChange={(event) => setForm({ ...form, identificacaoLida: event.target.value.toUpperCase() })} /></label>
      <label>Tipo do caso<select value={form.tipoCaso} onChange={(event) => setForm({ ...form, tipoCaso: event.target.value })}><option value="SEM_REGISTRO">Sem registro</option><option value="NAO_LOCALIZADA">Não localizada</option><option value="TBD">Não identificada</option></select></label>
      <label className="wide">Evidência<textarea maxLength="2000" value={form.evidencia} onChange={(event) => setForm({ ...form, evidencia: event.target.value })} /></label>
      <button type="submit" disabled={busy}>Abrir caso</button>
    </form>
    <div className="lost-found-toolbar">
      <label>Pesquisar<input value={filter} onChange={(event) => setFilter(event.target.value)} placeholder="Identificação, unidade, tipo ou responsável" /></label>
      <label>Status<select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Todos</option><option>ABERTO</option><option>EM_INVESTIGACAO</option><option>ASSOCIADO</option><option>REGULARIZADO</option><option>BAIXADO</option><option>ENCERRADO</option></select></label>
      <button type="button" className="secondary" onClick={load}>Atualizar</button>
    </div>
    {loading ? <Loading label="Carregando casos..." /> : <div className="lost-found-grid">
      <div>{filtered.length ? <DataTable rows={filtered} rowKey={(row) => row.id} onRowClick={setSelected} columns={[
        { key: 'identificacaoLida', label: 'Identificação' },
        { key: 'tipoCaso', label: 'Tipo', render: (row) => <StatusBadge value={tipoCasoLabel(row.tipoCaso)} /> },
        { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'responsavel', label: 'Responsável' },
        { key: 'abertoEm', label: 'Abertura', render: (row) => dateTime(row.abertoEm) }
      ]} /> : <EmptyState title="Nenhum caso encontrado" />}</div>
      <aside className="lost-found-detail">{!selected ? <EmptyState title="Selecione um caso" /> : <>
        <h3>{selected.identificacaoLida}</h3>
        <dl><div><dt>Tipo</dt><dd>{tipoCasoLabel(selected.tipoCaso)}</dd></div><div><dt>Status</dt><dd>{selected.status}</dd></div><div><dt>Unidade associada</dt><dd>{selected.unidadeIdentificacao || '—'}</dd></div><div><dt>Responsável</dt><dd>{selected.responsavel || '—'}</dd></div><div><dt>Evidência</dt><dd>{selected.evidencia || '—'}</dd></div><div><dt>Decisão final</dt><dd>{selected.decisaoFinal || '—'}</dd></div></dl>
        <div className="actions">
          {['ABERTO', 'EM_INVESTIGACAO', 'ASSOCIADO'].includes(selected.status) && <button type="button" disabled={busy} onClick={investigate}>Investigar</button>}
          {['ABERTO', 'EM_INVESTIGACAO'].includes(selected.status) && <button type="button" className="secondary" disabled={busy} onClick={associate}>Associar</button>}
          {selected.status === 'ASSOCIADO' && <button type="button" disabled={busy} onClick={() => decide(inventoryLostFoundApi.regularizar, 'Unidade regularizada.', 'Decisão de regularização:')}>Regularizar</button>}
          {['ABERTO', 'EM_INVESTIGACAO', 'ASSOCIADO'].includes(selected.status) && <button type="button" className="danger" disabled={busy} onClick={() => decide(inventoryLostFoundApi.baixar, 'Caso baixado.', 'Decisão de baixa:')}>Baixar</button>}
          {['REGULARIZADO', 'BAIXADO'].includes(selected.status) && <button type="button" disabled={busy} onClick={() => decide(inventoryLostFoundApi.encerrar, 'Caso encerrado.', 'Decisão de encerramento:')}>Encerrar</button>}
        </div>
      </>}</aside>
    </div>}
  </Section>;
}
