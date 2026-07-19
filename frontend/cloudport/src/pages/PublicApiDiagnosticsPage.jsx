import { useMemo, useState } from 'react';
import { DataTable, EmptyState, JsonDetails, Message, MetricCard, PageHeader, Section, StatusBadge } from '../components.jsx';
import {
  PUBLIC_API_CONTRACTS,
  findPublicApiContract,
  runPublicApiDiagnostic
} from '../publicApiDiagnostics.js';

function statusLabel(result) {
  if (!result) return '—';
  if (result.ok) return 'SUCESSO';
  return result.status ? `FALHA HTTP ${result.status}` : 'FALHA DE REDE';
}

export function PublicApiDiagnosticsPage() {
  const [clientId, setClientId] = useState('');
  const [clientSecret, setClientSecret] = useState('');
  const [contractId, setContractId] = useState(PUBLIC_API_CONTRACTS[0].id);
  const [pathValues, setPathValues] = useState({});
  const [queryValues, setQueryValues] = useState({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);

  const contract = useMemo(() => findPublicApiContract(contractId), [contractId]);

  function selectContract(nextId) {
    setContractId(nextId);
    setPathValues({});
    setQueryValues({});
    setResult(null);
    setError('');
  }

  async function execute(event) {
    event.preventDefault();
    if (busy || !contract) return;
    setBusy(true); setError(''); setResult(null);
    try {
      const outcome = await runPublicApiDiagnostic(contract, { clientId, clientSecret, pathValues, queryValues });
      setResult(outcome);
      setHistory((current) => [outcome, ...current].slice(0, 20));
      if (!outcome.ok) setError(outcome.mensagem);
    } catch (reason) {
      setError(reason?.message ?? 'Não foi possível executar o diagnóstico.');
    } finally { setBusy(false); }
  }

  const catalogColumns = [
    { key: 'metodo', label: 'Método' },
    { key: 'caminho', label: 'Contrato' },
    { key: 'descricao', label: 'Descrição' },
    { key: 'acoes', label: 'Ações', render: (row) => <button type="button" className="secondary" onClick={() => selectContract(row.id)}>Selecionar</button> }
  ];

  const historyColumns = [
    { key: 'executadoEm', label: 'Executado em', render: (row) => new Date(row.executadoEm).toLocaleString('pt-BR') },
    { key: 'caminho', label: 'Contrato' },
    { key: 'resultado', label: 'Resultado', render: (row) => <StatusBadge value={statusLabel(row)} /> },
    { key: 'latenciaMs', label: 'Latência', render: (row) => `${row.latenciaMs} ms` },
    { key: 'correlationId', label: 'Correlation ID', render: (row) => row.correlationId || '—' }
  ];

  return <>
    <PageHeader
      eyebrow="Integrações"
      title="Diagnóstico da API pública"
      description="Verificação dos contratos /api/public/v1/* com as credenciais do cliente de integração. As credenciais ficam apenas na memória desta tela."
    />
    <Message type="error">{error}</Message>
    <Section title="Execução de diagnóstico" description="Informe as credenciais do cliente, selecione o contrato e execute a chamada real contra o backend.">
      <form className="inline-form" onSubmit={execute}>
        <label className="field"><span>Client ID</span><input value={clientId} onChange={(event) => setClientId(event.target.value)} maxLength={120} autoComplete="off" required /></label>
        <label className="field"><span>Client secret</span><input type="password" value={clientSecret} onChange={(event) => setClientSecret(event.target.value)} autoComplete="off" required /></label>
        <label className="field"><span>Contrato</span><select value={contractId} onChange={(event) => selectContract(event.target.value)}>{PUBLIC_API_CONTRACTS.map((item) => <option key={item.id} value={item.id}>{item.metodo} {item.caminho}</option>)}</select></label>
        {contract?.pathParams.map((param) => <label className="field" key={`path-${param.nome}`}><span>{param.nome} (rota)</span><input value={pathValues[param.nome] ?? ''} onChange={(event) => setPathValues((current) => ({ ...current, [param.nome]: event.target.value }))} inputMode="numeric" placeholder={`Ex.: ${param.exemplo}`} required /></label>)}
        {contract?.queryParams.map((param) => <label className="field" key={`query-${param.nome}`}><span>{param.nome}{param.obrigatorio ? ' *' : ''}</span><input value={queryValues[param.nome] ?? ''} onChange={(event) => setQueryValues((current) => ({ ...current, [param.nome]: event.target.value }))} placeholder={`Ex.: ${param.exemplo}`} required={Boolean(param.obrigatorio)} /></label>)}
        <button disabled={busy || !clientId || !clientSecret}>{busy ? 'Executando...' : 'Executar diagnóstico'}</button>
      </form>
      {result && <div className="metrics-grid">
        <MetricCard label="Resultado" value={statusLabel(result)} detail={result.caminho} />
        <MetricCard label="Status HTTP" value={result.status ?? '—'} />
        <MetricCard label="Latência" value={`${result.latenciaMs} ms`} />
        <MetricCard label="Correlation ID" value={result.correlationId ?? '—'} />
      </div>}
      <JsonDetails value={result?.payload} title="Resposta do contrato" />
    </Section>
    <Section title="Catálogo de contratos" description="Contratos GET expostos em /api/public/v1 para clientes de integração autenticados.">
      <DataTable rows={PUBLIC_API_CONTRACTS} columns={catalogColumns} gridId="integracoes-api-publica-catalogo" exportFileName="contratos-api-publica" rowKey={(row) => row.id} />
    </Section>
    <Section title="Histórico da sessão" description="Últimas execuções realizadas nesta tela (não persistidas).">
      {history.length
        ? <DataTable rows={history} columns={historyColumns} gridId="integracoes-api-publica-historico" exportFileName="diagnosticos-api-publica" rowKey={(row, index) => `${row.executadoEm}-${index}`} />
        : <EmptyState title="Nenhum diagnóstico executado" description="Execute um contrato para registrar o histórico da sessão." />}
    </Section>
  </>;
}
