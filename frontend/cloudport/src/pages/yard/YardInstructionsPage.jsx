import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, hasAnyRole } from '../../api.js';
import { DataTable, EmptyState, Loading, Message, Section, StatusBadge } from '../../components.jsx';
import { yardInstructionApi } from '../../yardInstructionApi.js';
import { DetailGrid, FilterField, normalized, YardPageHeader } from './YardShared.jsx';
import './YardInstructionsPage.css';

const EMPTY_FORM = {
  codigoConteiner: '',
  tipoOperacao: 'MOVIMENTACAO',
  origem: '',
  destino: '',
  prioridade: 'NORMAL',
  agendadaEm: '',
  equipamento: '',
  equipe: '',
  observacoes: ''
};

function formatDateTime(value) {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleString('pt-BR');
}

export function YardInstructionCreateModal({ open, session, onClose, onCreated }) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (open) {
      setForm(EMPTY_FORM);
      setError('');
    }
  }, [open]);

  if (!open) return null;

  function change(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      const created = await yardInstructionApi.criar({
        ...form,
        agendadaEm: form.agendadaEm || null,
        criadoPor: session?.nome || 'operador'
      });
      onCreated?.(created);
      onClose();
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível criar a instrução de trabalho.'));
    } finally {
      setBusy(false);
    }
  }

  return <div className="yard-instruction-modal-backdrop" onMouseDown={onClose}>
    <section className="yard-instruction-modal" role="dialog" aria-modal="true" aria-labelledby="yard-instruction-modal-title" onMouseDown={(event) => event.stopPropagation()}>
      <header><div><span className="eyebrow">Pátio</span><h2 id="yard-instruction-modal-title">Nova instrução de trabalho</h2></div><button type="button" className="icon-button" aria-label="Fechar" onClick={onClose}>×</button></header>
      <Message type="error">{error}</Message>
      <form className="yard-instruction-form" onSubmit={submit}>
        <label>Contêiner<input required maxLength="30" value={form.codigoConteiner} onChange={(event) => change('codigoConteiner', event.target.value.toUpperCase())} /></label>
        <label>Operação<select value={form.tipoOperacao} onChange={(event) => change('tipoOperacao', event.target.value)}><option>MOVIMENTACAO</option><option>BLOQUEIO</option><option>DESBLOQUEIO</option><option>INSPECAO</option></select></label>
        <label>Origem<input maxLength="80" value={form.origem} onChange={(event) => change('origem', event.target.value)} /></label>
        <label>Destino<input required={form.tipoOperacao === 'MOVIMENTACAO'} maxLength="80" value={form.destino} onChange={(event) => change('destino', event.target.value)} /></label>
        <label>Prioridade<select value={form.prioridade} onChange={(event) => change('prioridade', event.target.value)}><option>NORMAL</option><option>ALTA</option><option>EMERGENCIAL</option></select></label>
        <label>Agendamento<input type="datetime-local" value={form.agendadaEm} onChange={(event) => change('agendadaEm', event.target.value)} /></label>
        <label>Equipamento<input maxLength="30" value={form.equipamento} onChange={(event) => change('equipamento', event.target.value)} /></label>
        <label>Equipe<input maxLength="80" value={form.equipe} onChange={(event) => change('equipe', event.target.value)} /></label>
        <label className="wide">Observações<textarea maxLength="1000" value={form.observacoes} onChange={(event) => change('observacoes', event.target.value)} /></label>
        <div className="actions wide"><button type="button" className="secondary" onClick={onClose}>Cancelar</button><button type="submit" disabled={busy}>{busy ? 'Criando...' : 'Criar instrução'}</button></div>
      </form>
    </section>
  </div>;
}

export function YardInstructionsPanel({ session, compact = false }) {
  const [rows, setRows] = useState([]);
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState(null);
  const [creating, setCreating] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const canOperate = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await yardInstructionApi.pesquisar();
      setRows(Array.isArray(response) ? response : []);
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível carregar as instruções de trabalho.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = useMemo(() => rows.filter((item) =>
    (!status || item.status === status)
    && (!search || normalized(`${item.codigoConteiner} ${item.tipoOperacao} ${item.destino} ${item.equipamento} ${item.equipe}`).includes(normalized(search)))
  ), [rows, search, status]);

  async function transition(operation, message) {
    if (!selected || busy) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      const updated = await operation(selected.id);
      setSelected(updated);
      setSuccess(message);
      await load();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  function cancel() {
    const justification = window.prompt('Informe a justificativa do cancelamento:');
    if (!justification) return;
    transition((id) => yardInstructionApi.cancelar(id, justification), 'Instrução cancelada.');
  }

  return <div className={compact ? 'yard-instructions compact' : 'yard-instructions'}>
    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>
    <div className="yard-instruction-toolbar">
      <FilterField label="Busca"><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Contêiner, destino, equipamento ou equipe" /></FilterField>
      <FilterField label="Status"><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Todos</option><option>PENDENTE</option><option>EM_EXECUCAO</option><option>CONCLUIDA</option><option>CANCELADA</option></select></FilterField>
      <div className="actions"><button type="button" className="secondary" onClick={load}>Atualizar</button>{canOperate && <button type="button" onClick={() => setCreating(true)}>Nova instrução</button>}</div>
    </div>
    {loading ? <Loading label="Carregando instruções..." /> : <div className="split-grid">
      <Section title={`Instruções (${filtered.length})`}>
        {filtered.length ? <DataTable rows={filtered} rowKey={(row) => row.id} onRowClick={setSelected} columns={[
          { key: 'id', label: 'ID' },
          { key: 'codigoConteiner', label: 'Contêiner' },
          { key: 'tipoOperacao', label: 'Operação' },
          { key: 'destino', label: 'Destino' },
          { key: 'prioridade', label: 'Prioridade', render: (row) => <StatusBadge value={row.prioridade} /> },
          { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
          { key: 'agendadaEm', label: 'Agendada', render: (row) => formatDateTime(row.agendadaEm) }
        ]} /> : <EmptyState title="Nenhuma instrução encontrada" />}
      </Section>
      <Section title="Detalhes e execução">
        {!selected ? <EmptyState title="Selecione uma instrução" /> : <>
          <DetailGrid value={selected} fields={[
            ['id', 'ID'], ['codigoConteiner', 'Contêiner'], ['tipoOperacao', 'Operação'], ['origem', 'Origem'],
            ['destino', 'Destino'], ['prioridade', 'Prioridade'], ['status', 'Status'], ['equipamento', 'Equipamento'],
            ['equipe', 'Equipe'], ['criadoPor', 'Criado por'], ['observacoes', 'Observações'],
            ['iniciadaEm', 'Iniciada em'], ['concluidaEm', 'Concluída em'], ['justificativaCancelamento', 'Cancelamento']
          ]} />
          {canOperate && <div className="actions">
            {selected.status === 'PENDENTE' && <button type="button" disabled={busy} onClick={() => transition(yardInstructionApi.iniciar, 'Execução iniciada.')}>Iniciar</button>}
            {selected.status === 'EM_EXECUCAO' && <button type="button" disabled={busy} onClick={() => transition(yardInstructionApi.concluir, 'Instrução concluída e inventário atualizado.')}>Concluir</button>}
            {['PENDENTE', 'EM_EXECUCAO'].includes(selected.status) && <button type="button" className="danger" disabled={busy} onClick={cancel}>Cancelar</button>}
          </div>}
        </>}
      </Section>
    </div>}
    <YardInstructionCreateModal open={creating} session={session} onClose={() => setCreating(false)} onCreated={(created) => { setSelected(created); setSuccess('Instrução criada.'); load(); }} />
  </div>;
}

export function YardInstructionsPage({ navigate, session }) {
  return <>
    <YardPageHeader path="/home/patio/instrucoes" navigate={navigate} title="Instruções de trabalho" description="Criação, priorização e execução de movimentações, bloqueios, desbloqueios e inspeções do pátio." />
    <YardInstructionsPanel session={session} />
  </>;
}
