import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError } from '../api.js';
import { billingCapApi } from '../billingCapApi.js';
import {
  DataTable,
  EmptyState,
  Loading,
  Message,
  MetricCard,
  PageHeader,
  Section,
  StatusBadge
} from '../components.jsx';

function dateInputValue(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function currency(value) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format(Number(value ?? 0));
}

function dateTime(value) {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleString('pt-BR');
}

function defaultDueDate() {
  const date = new Date();
  date.setDate(date.getDate() + 7);
  return dateInputValue(date);
}

function useBillingData() {
  const [data, setData] = useState({ tarifas: [], cobrancas: [], faturas: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [tarifas, cobrancas, faturas] = await Promise.all([
        billingCapApi.listarTarifas(),
        billingCapApi.listarCobrancas(),
        billingCapApi.listarFaturas()
      ]);
      setData({
        tarifas: Array.isArray(tarifas) ? tarifas : [],
        cobrancas: Array.isArray(cobrancas) ? cobrancas : [],
        faturas: Array.isArray(faturas) ? faturas : []
      });
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return { ...data, loading, error, reload, setError };
}

export function BillingPage() {
  const remote = useBillingData();
  const [busy, setBusy] = useState('');
  const [success, setSuccess] = useState('');
  const [tariff, setTariff] = useState({
    codigo: '',
    descricao: '',
    tipoOperacao: 'ENTRADA',
    valor: '',
    inicioVigencia: dateInputValue(new Date()),
    fimVigencia: '',
    ativa: true
  });
  const [appointmentId, setAppointmentId] = useState('');
  const [invoice, setInvoice] = useState({ transportadoraId: '', vencimento: defaultDueDate() });
  const [payment, setPayment] = useState({ faturaId: '', valor: '', forma: 'PIX', referencia: '' });

  const totals = useMemo(() => {
    const pendingCharges = remote.cobrancas.filter((item) => item.status === 'PENDENTE');
    const openInvoices = remote.faturas.filter((item) => item.status === 'ABERTA');
    return {
      activeTariffs: remote.tarifas.filter((item) => item.ativa).length,
      pendingCharges: pendingCharges.length,
      pendingValue: pendingCharges.reduce((sum, item) => sum + Number(item.valor ?? 0), 0),
      openInvoices: openInvoices.length,
      openValue: openInvoices.reduce((sum, item) => sum + Number(item.saldo ?? item.total ?? 0), 0)
    };
  }, [remote.tarifas, remote.cobrancas, remote.faturas]);

  async function execute(key, action, message) {
    setBusy(key);
    setSuccess('');
    remote.setError('');
    try {
      await action();
      setSuccess(message);
      await remote.reload();
    } catch (reason) {
      remote.setError(formatError(reason));
    } finally {
      setBusy('');
    }
  }

  function saveTariff(event) {
    event.preventDefault();
    execute('tarifa', () => billingCapApi.salvarTarifa(tariff), 'Tarifa salva.');
  }

  function generateCharge(event) {
    event.preventDefault();
    execute('cobranca', () => billingCapApi.gerarCobranca(appointmentId), 'Cobrança gerada.');
  }

  function generateInvoice(event) {
    event.preventDefault();
    execute('fatura', () => billingCapApi.gerarFatura(invoice), 'Fatura gerada.');
  }

  function registerPayment(event) {
    event.preventDefault();
    execute('pagamento', () => billingCapApi.registrarPagamento(payment.faturaId, payment), 'Pagamento registrado.');
  }

  return <>
    <PageHeader
      eyebrow="Billing"
      title="Faturamento operacional"
      description="Cadastre tarifas, gere cobranças de atendimentos concluídos, consolide faturas e registre pagamentos."
      actions={<button type="button" className="secondary" onClick={remote.reload}>Atualizar</button>}
    />
    <Message type="error">{remote.error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    <div className="metrics-grid">
      <MetricCard label="Tarifas ativas" value={totals.activeTariffs} />
      <MetricCard label="Cobranças pendentes" value={totals.pendingCharges} detail={currency(totals.pendingValue)} />
      <MetricCard label="Faturas abertas" value={totals.openInvoices} detail={currency(totals.openValue)} />
    </div>

    <Section title="Tarifa operacional" description="A tarifa vigente para o tipo de operação será usada na geração idempotente da cobrança.">
      <form className="planner-selection-grid" onSubmit={saveTariff}>
        <label className="field"><span>Código</span><input required maxLength="50" value={tariff.codigo} onChange={(event) => setTariff((current) => ({ ...current, codigo: event.target.value }))} placeholder="GATE_ENTRADA" /></label>
        <label className="field"><span>Descrição</span><input required maxLength="160" value={tariff.descricao} onChange={(event) => setTariff((current) => ({ ...current, descricao: event.target.value }))} /></label>
        <label className="field"><span>Operação</span><select value={tariff.tipoOperacao} onChange={(event) => setTariff((current) => ({ ...current, tipoOperacao: event.target.value }))}><option value="ENTRADA">Entrada</option><option value="SAIDA">Saída</option><option value="DEVOLUCAO">Devolução</option><option value="TRANSFERENCIA">Transferência</option></select></label>
        <label className="field"><span>Valor</span><input required type="number" min="0" step="0.01" value={tariff.valor} onChange={(event) => setTariff((current) => ({ ...current, valor: event.target.value }))} /></label>
        <label className="field"><span>Início da vigência</span><input required type="date" value={tariff.inicioVigencia} onChange={(event) => setTariff((current) => ({ ...current, inicioVigencia: event.target.value }))} /></label>
        <label className="field"><span>Fim da vigência</span><input type="date" min={tariff.inicioVigencia} value={tariff.fimVigencia} onChange={(event) => setTariff((current) => ({ ...current, fimVigencia: event.target.value }))} /></label>
        <label className="field"><span>Situação</span><select value={tariff.ativa ? 'true' : 'false'} onChange={(event) => setTariff((current) => ({ ...current, ativa: event.target.value === 'true' }))}><option value="true">Ativa</option><option value="false">Inativa</option></select></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={busy === 'tarifa'}>{busy === 'tarifa' ? 'Salvando...' : 'Salvar tarifa'}</button></div>
      </form>
    </Section>

    <Section title="Processamento financeiro">
      <div className="planner-selection-grid">
        <form className="panel" onSubmit={generateCharge}>
          <h3>Gerar cobrança</h3>
          <label className="field"><span>Agendamento ID</span><input required type="number" min="1" value={appointmentId} onChange={(event) => setAppointmentId(event.target.value)} /></label>
          <button type="submit" disabled={busy === 'cobranca'}>{busy === 'cobranca' ? 'Gerando...' : 'Gerar cobrança'}</button>
        </form>
        <form className="panel" onSubmit={generateInvoice}>
          <h3>Gerar fatura</h3>
          <label className="field"><span>Transportadora ID</span><input required type="number" min="1" value={invoice.transportadoraId} onChange={(event) => setInvoice((current) => ({ ...current, transportadoraId: event.target.value }))} /></label>
          <label className="field"><span>Vencimento</span><input required type="date" min={dateInputValue(new Date())} value={invoice.vencimento} onChange={(event) => setInvoice((current) => ({ ...current, vencimento: event.target.value }))} /></label>
          <button type="submit" disabled={busy === 'fatura'}>{busy === 'fatura' ? 'Gerando...' : 'Faturar pendências'}</button>
        </form>
        <form className="panel" onSubmit={registerPayment}>
          <h3>Registrar pagamento</h3>
          <label className="field"><span>Fatura ID</span><input required type="number" min="1" value={payment.faturaId} onChange={(event) => setPayment((current) => ({ ...current, faturaId: event.target.value }))} /></label>
          <label className="field"><span>Valor</span><input required type="number" min="0.01" step="0.01" value={payment.valor} onChange={(event) => setPayment((current) => ({ ...current, valor: event.target.value }))} /></label>
          <label className="field"><span>Forma</span><select value={payment.forma} onChange={(event) => setPayment((current) => ({ ...current, forma: event.target.value }))}><option value="PIX">PIX</option><option value="BOLETO">Boleto</option><option value="TRANSFERENCIA">Transferência</option><option value="CARTAO">Cartão</option></select></label>
          <label className="field"><span>Referência</span><input maxLength="100" value={payment.referencia} onChange={(event) => setPayment((current) => ({ ...current, referencia: event.target.value }))} /></label>
          <button type="submit" disabled={busy === 'pagamento'}>{busy === 'pagamento' ? 'Registrando...' : 'Registrar pagamento'}</button>
        </form>
      </div>
    </Section>

    {remote.loading ? <Loading label="Carregando faturamento..." /> : <>
      <Section title="Tarifas">
        {remote.tarifas.length ? <DataTable rows={remote.tarifas} rowKey="id" columns={[
          { key: 'codigo', label: 'Código' },
          { key: 'descricao', label: 'Descrição' },
          { key: 'tipoOperacao', label: 'Operação', render: (row) => <StatusBadge value={row.tipoOperacao} /> },
          { key: 'valor', label: 'Valor', render: (row) => currency(row.valor) },
          { key: 'inicioVigencia', label: 'Início' },
          { key: 'fimVigencia', label: 'Fim' },
          { key: 'ativa', label: 'Situação', render: (row) => <StatusBadge value={row.ativa ? 'ATIVA' : 'INATIVA'} /> }
        ]} /> : <EmptyState title="Nenhuma tarifa cadastrada" />}
      </Section>
      <Section title="Cobranças">
        <DataTable rows={remote.cobrancas} rowKey="id" columns={[
          { key: 'id', label: 'ID' },
          { key: 'agendamento', label: 'Agendamento' },
          { key: 'transportadora', label: 'Transportadora' },
          { key: 'descricao', label: 'Descrição' },
          { key: 'valor', label: 'Valor', render: (row) => currency(row.valor) },
          { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
          { key: 'ocorridoEm', label: 'Ocorrido em', render: (row) => dateTime(row.ocorridoEm) }
        ]} emptyTitle="Nenhuma cobrança encontrada" />
      </Section>
      <Section title="Faturas">
        <DataTable rows={remote.faturas} rowKey="id" columns={[
          { key: 'id', label: 'ID' },
          { key: 'numero', label: 'Número' },
          { key: 'transportadora', label: 'Transportadora' },
          { key: 'vencimento', label: 'Vencimento' },
          { key: 'total', label: 'Total', render: (row) => currency(row.total) },
          { key: 'valorPago', label: 'Pago', render: (row) => currency(row.valorPago) },
          { key: 'saldo', label: 'Saldo', render: (row) => currency(row.saldo) },
          { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> }
        ]} emptyTitle="Nenhuma fatura encontrada" />
      </Section>
    </>}
  </>;
}

export function CapPage() {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setSummary(await billingCapApi.consultarCap());
    } catch (reason) {
      setError(formatError(reason));
      setSummary(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return <>
    <PageHeader
      eyebrow="CAP"
      title="Portal da transportadora"
      description="Acompanhe seus agendamentos, cobranças e faturas em uma única visão."
      actions={<button type="button" className="secondary" onClick={reload}>Atualizar</button>}
    />
    <Message type="error">{error}</Message>
    {loading ? <Loading label="Carregando portal da transportadora..." /> : summary ? <>
      <Section title={summary.transportadora} description={`Código da transportadora: ${summary.transportadoraId}`}>
        <div className="metrics-grid">
          <MetricCard label="Agendamentos" value={summary.totalAgendamentos ?? 0} />
          <MetricCard label="Pendentes" value={summary.agendamentosPendentes ?? 0} />
          <MetricCard label="Concluídos" value={summary.agendamentosConcluidos ?? 0} />
          <MetricCard label="Cobranças pendentes" value={summary.cobrancasPendentes ?? 0} detail={currency(summary.valorCobrancasPendentes)} />
          <MetricCard label="Faturas abertas" value={summary.faturasAbertas ?? 0} detail={currency(summary.valorFaturasAbertas)} />
        </div>
      </Section>
      <Section title="Agendamentos recentes">
        <DataTable rows={summary.agendamentosRecentes ?? []} rowKey="id" columns={[
          { key: 'codigo', label: 'Código' },
          { key: 'tipoOperacao', label: 'Operação', render: (row) => <StatusBadge value={row.tipoOperacao} /> },
          { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
          { key: 'horarioPrevistoChegada', label: 'Chegada prevista', render: (row) => dateTime(row.horarioPrevistoChegada) },
          { key: 'horarioRealChegada', label: 'Chegada real', render: (row) => dateTime(row.horarioRealChegada) },
          { key: 'horarioRealSaida', label: 'Saída real', render: (row) => dateTime(row.horarioRealSaida) }
        ]} emptyTitle="Nenhum agendamento encontrado" />
      </Section>
      <Section title="Faturas recentes">
        <DataTable rows={summary.faturasRecentes ?? []} rowKey="id" columns={[
          { key: 'numero', label: 'Número' },
          { key: 'emitidaEm', label: 'Emissão', render: (row) => dateTime(row.emitidaEm) },
          { key: 'vencimento', label: 'Vencimento' },
          { key: 'total', label: 'Total', render: (row) => currency(row.total) },
          { key: 'valorPago', label: 'Pago', render: (row) => currency(row.valorPago) },
          { key: 'saldo', label: 'Saldo', render: (row) => currency(row.saldo) },
          { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> }
        ]} emptyTitle="Nenhuma fatura encontrada" />
      </Section>
    </> : <EmptyState title="Resumo indisponível" description="Não foi possível carregar os dados vinculados à transportadora." />}
  </>;
}
