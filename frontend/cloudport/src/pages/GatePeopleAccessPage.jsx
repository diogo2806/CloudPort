import { useCallback, useEffect, useState } from 'react';
import { formatError } from '../api.js';
import { DataTable, Field, Loading, Message, MetricCard, PageHeader, Section, StatusBadge } from '../components.jsx';
import { gatePeopleAccessApi } from '../gatePeopleAccessApi.js';

const INITIAL_FORM = {
  operacao: 'ENTRADA',
  nome: '',
  documento: '',
  tipoPessoa: 'VISITANTE',
  empresa: '',
  cracha: '',
  pontoAcesso: 'Portaria principal',
  motivo: ''
};

function dateTime(value) {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleString('pt-BR');
}

function duration(minutes) {
  const value = Number(minutes ?? 0);
  if (!Number.isFinite(value) || value <= 0) return 'menos de 1 min';
  const hours = Math.floor(value / 60);
  const rest = value % 60;
  if (!hours) return `${rest} min`;
  return rest ? `${hours}h ${rest}min` : `${hours}h`;
}

export function GatePeopleAccessPage() {
  const [form, setForm] = useState(INITIAL_FORM);
  const [summary, setSummary] = useState({ totalPresentes: 0, presentesPorTipo: {} });
  const [presentPeople, setPresentPeople] = useState([]);
  const [movements, setMovements] = useState([]);
  const [documentFilter, setDocumentFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = useCallback(async (filter = '') => {
    setLoading(true);
    setError('');
    try {
      const [summaryResult, presentResult, movementResult] = await Promise.all([
        gatePeopleAccessApi.obterResumo(),
        gatePeopleAccessApi.listarPresentes(),
        gatePeopleAccessApi.listarMovimentacoes(filter, 100)
      ]);
      setSummary(summaryResult ?? { totalPresentes: 0, presentesPorTipo: {} });
      setPresentPeople(Array.isArray(presentResult) ? presentResult : []);
      setMovements(Array.isArray(movementResult) ? movementResult : []);
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível carregar o controle de pessoas.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load('');
  }, [load]);

  function updateField(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function prepareExit(person) {
    setForm((current) => ({
      ...current,
      operacao: 'SAIDA',
      nome: person.nome ?? '',
      documento: person.documento ?? '',
      tipoPessoa: person.tipoPessoa ?? 'VISITANTE',
      empresa: person.empresa ?? '',
      cracha: person.cracha ?? '',
      motivo: ''
    }));
    setSuccess('Pessoa selecionada. Confirme o ponto de acesso e registre a saída.');
    window.scrollTo?.({ top: 0, behavior: 'smooth' });
  }

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      if (form.operacao === 'ENTRADA') {
        await gatePeopleAccessApi.registrarEntrada(form);
        setSuccess('Entrada registrada com sucesso.');
      } else {
        await gatePeopleAccessApi.registrarSaida(form);
        setSuccess('Saída registrada com sucesso.');
      }
      setForm((current) => ({ ...INITIAL_FORM, pontoAcesso: current.pontoAcesso }));
      await load(documentFilter);
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível registrar a movimentação.'));
    } finally {
      setBusy(false);
    }
  }

  async function filterHistory(event) {
    event.preventDefault();
    await load(documentFilter);
  }

  const byType = summary?.presentesPorTipo ?? {};

  return <>
    <PageHeader
      eyebrow="Gate"
      title="Controle de entrada e saída de pessoas"
      description="Registre acessos, acompanhe quem está no terminal e consulte o histórico auditável por documento."
      actions={<button className="secondary" onClick={() => load(documentFilter)} disabled={loading}>Atualizar</button>}
    />

    <Message type="error" onClose={() => setError('')}>{error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    <div className="metrics-grid">
      <MetricCard label="Pessoas no terminal" value={summary?.totalPresentes ?? presentPeople.length} />
      <MetricCard label="Visitantes" value={byType.VISITANTE ?? 0} />
      <MetricCard label="Terceirizados" value={byType.TERCEIRIZADO ?? 0} />
      <MetricCard label="Motoristas" value={byType.MOTORISTA ?? 0} />
    </div>

    <Section
      title="Registrar movimentação"
      description="Uma pessoa não pode ter duas entradas abertas nem registrar saída sem estar presente no terminal."
    >
      <form className="inline-form" onSubmit={submit}>
        <Field label="Operação">
          <select value={form.operacao} onChange={(event) => updateField('operacao', event.target.value)}>
            <option value="ENTRADA">Entrada</option>
            <option value="SAIDA">Saída</option>
          </select>
        </Field>
        {form.operacao === 'ENTRADA' && <Field label="Nome completo">
          <input value={form.nome} onChange={(event) => updateField('nome', event.target.value)} maxLength={140} required />
        </Field>}
        <Field label="Documento">
          <input value={form.documento} onChange={(event) => updateField('documento', event.target.value)} maxLength={30} required />
        </Field>
        {form.operacao === 'ENTRADA' && <>
          <Field label="Tipo de pessoa">
            <select value={form.tipoPessoa} onChange={(event) => updateField('tipoPessoa', event.target.value)}>
              <option value="FUNCIONARIO">Funcionário</option>
              <option value="TERCEIRIZADO">Terceirizado</option>
              <option value="VISITANTE">Visitante</option>
              <option value="MOTORISTA">Motorista</option>
              <option value="TRIPULANTE">Tripulante</option>
              <option value="AUTORIDADE">Autoridade</option>
              <option value="OUTRO">Outro</option>
            </select>
          </Field>
          <Field label="Empresa ou vínculo">
            <input value={form.empresa} onChange={(event) => updateField('empresa', event.target.value)} maxLength={140} />
          </Field>
          <Field label="Crachá">
            <input value={form.cracha} onChange={(event) => updateField('cracha', event.target.value)} maxLength={50} />
          </Field>
        </>}
        <Field label="Ponto de acesso">
          <input value={form.pontoAcesso} onChange={(event) => updateField('pontoAcesso', event.target.value)} maxLength={120} required />
        </Field>
        <Field label="Motivo ou observação">
          <input value={form.motivo} onChange={(event) => updateField('motivo', event.target.value)} maxLength={500} />
        </Field>
        <button type="submit" disabled={busy}>{busy ? 'Registrando...' : `Registrar ${form.operacao === 'ENTRADA' ? 'entrada' : 'saída'}`}</button>
      </form>
    </Section>

    <Section
      title="Pessoas presentes"
      description="Clique em uma pessoa para preparar o registro de saída."
      actions={<span>{presentPeople.length} registro(s)</span>}
    >
      {loading ? <Loading /> : <DataTable
        rows={presentPeople}
        rowKey="id"
        onRowClick={prepareExit}
        emptyTitle="Nenhuma pessoa está com entrada aberta"
        columns={[
          { key: 'nome', label: 'Nome' },
          { key: 'documento', label: 'Documento' },
          { key: 'tipoPessoa', label: 'Tipo', render: (row) => <StatusBadge value={row.tipoPessoa} /> },
          { key: 'empresa', label: 'Empresa' },
          { key: 'entradaEm', label: 'Entrada', render: (row) => dateTime(row.entradaEm) },
          { key: 'pontoEntrada', label: 'Ponto de entrada' },
          { key: 'permanenciaMinutos', label: 'Permanência', render: (row) => duration(row.permanenciaMinutos) }
        ]}
      />}
    </Section>

    <Section title="Histórico de movimentações" description="Últimas 100 entradas e saídas, com operador e correlação para auditoria.">
      <form className="inline-form" onSubmit={filterHistory}>
        <Field label="Filtrar por documento">
          <input value={documentFilter} onChange={(event) => setDocumentFilter(event.target.value)} maxLength={30} placeholder="Deixe vazio para todos" />
        </Field>
        <button className="secondary" type="submit" disabled={loading}>Aplicar filtro</button>
        {documentFilter && <button className="secondary" type="button" onClick={() => { setDocumentFilter(''); void load(''); }}>Limpar</button>}
      </form>
      {loading ? <Loading /> : <DataTable
        rows={movements}
        rowKey="id"
        emptyTitle="Nenhuma movimentação encontrada"
        columns={[
          { key: 'registradoEm', label: 'Data e hora', render: (row) => dateTime(row.registradoEm) },
          { key: 'direcao', label: 'Movimento', render: (row) => <StatusBadge value={row.direcao} /> },
          { key: 'nome', label: 'Nome' },
          { key: 'documento', label: 'Documento' },
          { key: 'tipoPessoa', label: 'Tipo', render: (row) => <StatusBadge value={row.tipoPessoa} /> },
          { key: 'pontoAcesso', label: 'Ponto' },
          { key: 'usuarioResponsavel', label: 'Operador' },
          { key: 'permanenciaMinutos', label: 'Permanência', render: (row) => row.direcao === 'SAIDA' ? duration(row.permanenciaMinutos) : '—' }
        ]}
      />}
    </Section>
  </>;
}
