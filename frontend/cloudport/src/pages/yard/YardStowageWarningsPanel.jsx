import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, sanitizeText } from '../../api.js';
import { DataTable, Loading, Message, Section, StatusBadge } from '../../components.jsx';
import { yardStowageWarningsApi } from '../../yardStowageWarningsApi.js';
import { DetailGrid, FilterField } from './YardShared.jsx';

const ACTIVE_STATUSES = new Set(['ABERTO', 'ATRIBUIDO', 'EM_CORRECAO', 'AGUARDANDO_REVALIDACAO', 'REABERTO']);

function formatDateTime(value) {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? sanitizeText(value) : parsed.toLocaleString('pt-BR');
}

export function YardStowageWarningsPanel({ canOperate, navigate }) {
  const [filters, setFilters] = useState({ status: '', severidade: '', responsavel: '', bloco: '', codigoUnidade: '' });
  const [warnings, setWarnings] = useState([]);
  const [selected, setSelected] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const result = await yardStowageWarningsApi.listar(filters);
      setWarnings(result ?? []);
      if (selected?.id) {
        const updated = (result ?? []).find((item) => item.id === selected.id) ?? null;
        setSelected(updated);
      }
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível carregar os avisos de estivagem.'));
    } finally {
      setLoading(false);
    }
  }, [filters, selected?.id]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    if (!selected?.id) {
      setHistory([]);
      return;
    }
    yardStowageWarningsApi.historico(selected.id)
      .then((result) => setHistory(result ?? []))
      .catch((reason) => setError(formatError(reason, 'Não foi possível carregar o histórico do aviso.')));
  }, [selected?.id, selected?.atualizadoEm]);

  const activeCount = useMemo(() => warnings.filter((warning) => ACTIVE_STATUSES.has(warning.status)).length, [warnings]);
  const criticalCount = useMemo(() => warnings.filter((warning) => warning.severidade === 'CRITICA' && ACTIVE_STATUSES.has(warning.status)).length, [warnings]);

  async function execute(action) {
    setBusy(true);
    setError('');
    try {
      await action();
      await load();
    } catch (reason) {
      setError(formatError(reason, 'A operação do aviso não pôde ser concluída.'));
    } finally {
      setBusy(false);
    }
  }

  function assign() {
    const responsible = globalThis.prompt('Responsável pelo aviso:', selected?.responsavel ?? '');
    if (!responsible?.trim()) return;
    const deadline = globalThis.prompt('Prazo ISO opcional (ex.: 2026-07-24T18:00):', selected?.prazo ?? '');
    execute(() => yardStowageWarningsApi.atribuir(selected.id, responsible, deadline || null));
  }

  function startCorrection() {
    const action = globalThis.prompt('Ação corretiva executada:');
    if (!action?.trim()) return;
    const evidence = globalThis.prompt('Evidência ou referência operacional opcional:') ?? '';
    execute(() => yardStowageWarningsApi.iniciarCorrecao(selected.id, action, evidence));
  }

  function sendToRevalidation() {
    const evidence = globalThis.prompt('Evidência final para revalidação:') ?? '';
    execute(() => yardStowageWarningsApi.aguardarRevalidacao(selected.id, evidence));
  }

  function revalidate() {
    const evidence = globalThis.prompt('Nova evidência de campo opcional:') ?? '';
    execute(() => yardStowageWarningsApi.revalidar(selected.id, evidence));
  }

  return <Section
    title={`Avisos de estivagem (${activeCount} ativos)`}
    description={`${criticalCount} crítico(s). A resolução ocorre somente após nova leitura física confirmar que a regra deixou de ser violada.`}
  >
    <Message type="error">{error}</Message>
    <div className="filter-grid">
      <FilterField label="Status"><select value={filters.status} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))}><option value="">Todos</option>{['ABERTO', 'ATRIBUIDO', 'EM_CORRECAO', 'AGUARDANDO_REVALIDACAO', 'RESOLVIDO', 'REABERTO'].map((value) => <option key={value}>{value}</option>)}</select></FilterField>
      <FilterField label="Severidade"><select value={filters.severidade} onChange={(event) => setFilters((current) => ({ ...current, severidade: event.target.value }))}><option value="">Todas</option>{['CRITICA', 'ALTA', 'MEDIA', 'BAIXA'].map((value) => <option key={value}>{value}</option>)}</select></FilterField>
      <FilterField label="Responsável"><input value={filters.responsavel} onChange={(event) => setFilters((current) => ({ ...current, responsavel: event.target.value }))} /></FilterField>
      <FilterField label="Bloco"><input value={filters.bloco} onChange={(event) => setFilters((current) => ({ ...current, bloco: event.target.value }))} /></FilterField>
      <FilterField label="Unidade"><input value={filters.codigoUnidade} onChange={(event) => setFilters((current) => ({ ...current, codigoUnidade: event.target.value.toUpperCase() }))} /></FilterField>
    </div>
    <div className="actions">
      <button type="button" className="secondary" disabled={busy || loading} onClick={load}>Atualizar fila</button>
      {canOperate && <button type="button" disabled={busy} onClick={() => execute(yardStowageWarningsApi.revalidarInventario)}>Revalidar inventário</button>}
    </div>
    {loading ? <Loading label="Carregando avisos..." /> : <div className="split-grid">
      <DataTable
        rows={warnings}
        rowKey={(row) => row.id}
        onRowClick={setSelected}
        columns={[
          { key: 'severidade', label: 'Severidade', render: (row) => <StatusBadge value={row.severidade} /> },
          { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
          { key: 'codigoUnidade', label: 'Unidade' },
          { key: 'regra', label: 'Regra' },
          { key: 'bloco', label: 'Bloco' },
          { key: 'posicao', label: 'Posição', render: (row) => `L${row.linha}/C${row.coluna}/${sanitizeText(row.camada)}` },
          { key: 'responsavel', label: 'Responsável' },
          { key: 'prazo', label: 'Prazo', render: (row) => formatDateTime(row.prazo) },
          { key: 'ultimaRevalidacaoEm', label: 'Última validação', render: (row) => formatDateTime(row.ultimaRevalidacaoEm) }
        ]}
        emptyTitle="Nenhum aviso corresponde aos filtros"
      />
      <div>
        <Section title="Detalhes do aviso">
          <DetailGrid value={selected} fields={[
            ['id', 'ID'], ['codigoUnidade', 'Unidade'], ['regra', 'Regra'], ['severidade', 'Severidade'], ['status', 'Status'],
            ['descricao', 'Descrição'], ['valorObservado', 'Valor observado'], ['valorEsperado', 'Valor esperado'],
            ['acaoSugerida', 'Ação sugerida'], ['responsavel', 'Responsável'], ['prazo', 'Prazo'],
            ['acaoCorretiva', 'Ação corretiva'], ['evidencia', 'Evidência'], ['resultado', 'Resultado'], ['recorrencias', 'Recorrências']
          ]} />
          {selected && canOperate && ACTIVE_STATUSES.has(selected.status) && <div className="actions">
            <button type="button" className="secondary" disabled={busy} onClick={assign}>Atribuir</button>
            <button type="button" className="secondary" disabled={busy} onClick={startCorrection}>Iniciar correção</button>
            <button type="button" className="secondary" disabled={busy} onClick={sendToRevalidation}>Aguardar revalidação</button>
            <button type="button" disabled={busy} onClick={revalidate}>Revalidar agora</button>
          </div>}
          {selected && <button type="button" className="secondary" onClick={() => navigate?.('/home/patio/instrucoes')}>Abrir processo operacional</button>}
          <details>
            <summary>Manual contextual da correção</summary>
            <p>1. Confirme a unidade e a posição física indicadas. 2. Compare o valor observado com a regra esperada. 3. Atribua um responsável e prazo. 4. Execute a ação sugerida ou registre uma ação equivalente. 5. Anexe a evidência. 6. Envie para revalidação. O aviso só será resolvido quando a nova leitura do inventário não encontrar mais a violação.</p>
          </details>
        </Section>
        <Section title="Histórico completo">
          <DataTable rows={history} rowKey={(row) => row.id} columns={[
            { key: 'criadoEm', label: 'Data', render: (row) => formatDateTime(row.criadoEm) },
            { key: 'tipoEvento', label: 'Evento' }, { key: 'ator', label: 'Ator' },
            { key: 'statusNovo', label: 'Novo status' }, { key: 'detalhes', label: 'Detalhes' },
            { key: 'evidencia', label: 'Evidência' }, { key: 'resultado', label: 'Resultado' }
          ]} emptyTitle="Selecione um aviso para consultar o histórico" />
        </Section>
      </div>
    </div>}
  </Section>;
}
