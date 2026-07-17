import { useCallback, useEffect, useState } from 'react';
import { formatError, normalizePage, request } from '../api.js';
import { DataTable, EmptyState, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';

const INITIAL_FORM = {
  codigoAutorizacao: '',
  identificadorCarga: '',
  tipoCarga: 'TRATOR',
  visitaNavio: '',
  clienteNome: '',
  clienteDocumento: '',
  documentosValidados: false,
  liberacaoAduaneiraConfirmada: false,
  cargaDescarregada: false,
  condutorHabilitado: false,
  observacao: ''
};

function displayDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('pt-BR');
}

export function GateDirectVesselReleasePage() {
  const [form, setForm] = useState(INITIAL_FORM);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadHistory = useCallback(async () => {
    setLoading(true);
    try {
      const response = await request('/gate/retiradas-diretas-navio', {
        query: { page: 0, size: 50 }
      });
      setHistory(normalizePage(response));
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível carregar o histórico de retiradas.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  function updateField(event) {
    const { name, type, checked, value } = event.target;
    setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }));
  }

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      const result = await request('/gate/retiradas-diretas-navio', {
        method: 'POST',
        body: form
      });
      setSuccess(`Saída registrada: ${result.identificadorCarga} deixou o terminal pela autorização ${result.codigoAutorizacao}.`);
      setForm(INITIAL_FORM);
      await loadHistory();
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível registrar a saída direta pelo gate.'));
    } finally {
      setBusy(false);
    }
  }

  const confirmations = [
    ['documentosValidados', 'Documentação da carga validada'],
    ['liberacaoAduaneiraConfirmada', 'Liberação aduaneira confirmada'],
    ['cargaDescarregada', 'Descarga do navio confirmada'],
    ['condutorHabilitado', 'Cliente habilitado para conduzir o equipamento']
  ];

  return <>
    <PageHeader
      eyebrow="Gate"
      title="Saída direta do navio"
      description="Registre a retirada de trator ou outra carga autopropelida que foi descarregada do navio e seguirá diretamente para fora do terminal, sem criar uma entrada fictícia de caminhão."
      actions={<button className="secondary" onClick={loadHistory} disabled={loading}>Atualizar histórico</button>}
    />

    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>

    <Section title="Identificação e autorização" description="A autorização e o identificador da carga são únicos e impedem uma segunda saída do mesmo equipamento.">
      <form onSubmit={submit}>
        <div className="inline-form">
          <label className="field">
            <span>Código da autorização</span>
            <input name="codigoAutorizacao" value={form.codigoAutorizacao} onChange={updateField} maxLength={80} required />
          </label>
          <label className="field">
            <span>Identificador da carga</span>
            <input name="identificadorCarga" value={form.identificadorCarga} onChange={updateField} maxLength={80} placeholder="Chassi, série ou patrimônio" required />
          </label>
          <label className="field">
            <span>Tipo de carga</span>
            <input name="tipoCarga" value={form.tipoCarga} onChange={updateField} maxLength={60} required />
          </label>
          <label className="field">
            <span>Visita do navio</span>
            <input name="visitaNavio" value={form.visitaNavio} onChange={updateField} maxLength={80} required />
          </label>
          <label className="field">
            <span>Cliente/condutor</span>
            <input name="clienteNome" value={form.clienteNome} onChange={updateField} maxLength={120} required />
          </label>
          <label className="field">
            <span>Documento do cliente</span>
            <input name="clienteDocumento" value={form.clienteDocumento} onChange={updateField} maxLength={30} required />
          </label>
          <label className="field">
            <span>Observação</span>
            <textarea name="observacao" value={form.observacao} onChange={updateField} maxLength={500} rows={3} />
          </label>
        </div>

        <Section title="Confirmações obrigatórias" description="O gate só finaliza a saída quando todas as condições operacionais estão confirmadas.">
          <div className="inline-form">
            {confirmations.map(([name, label]) => <label className="compact-field" key={name}>
              <input
                type="checkbox"
                name={name}
                checked={form[name]}
                onChange={updateField}
                style={{ width: 'auto', minHeight: 'auto' }}
              />
              {label}
            </label>)}
          </div>
        </Section>

        <div className="actions">
          <button type="submit" disabled={busy}>{busy ? 'Registrando...' : 'Registrar saída pelo gate'}</button>
          <button type="button" className="secondary" onClick={() => setForm(INITIAL_FORM)} disabled={busy}>Limpar</button>
        </div>
      </form>
    </Section>

    <Section title="Histórico de saídas diretas">
      {loading ? <Loading label="Carregando retiradas..." /> : history.length ? <DataTable
        rows={history}
        rowKey={(row) => row.id}
        columns={[
          { key: 'codigoAutorizacao', label: 'Autorização' },
          { key: 'identificadorCarga', label: 'Carga' },
          { key: 'tipoCarga', label: 'Tipo' },
          { key: 'visitaNavio', label: 'Visita do navio' },
          { key: 'clienteNome', label: 'Cliente' },
          { key: 'saidaEm', label: 'Saída', render: (row) => displayDate(row.saidaEm) },
          { key: 'operador', label: 'Operador' },
          { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.statusDescricao || row.status} /> }
        ]}
      /> : <EmptyState title="Nenhuma saída direta registrada" />}
    </Section>
  </>;
}
