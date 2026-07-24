import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, hasAnyRole } from '../../api.js';
import { DataTable, EmptyState, Loading, Message, MetricCard, Section, StatusBadge } from '../../components.jsx';
import { yardStowageWarningApi } from '../../yardStowageWarningApi.js';
import { DetailGrid, FilterField, normalized, YardPageHeader } from './YardShared.jsx';
import './YardStowageWarningsPage.css';

const ACTIVE_STATES = new Set(['ABERTO', 'ATRIBUIDO', 'EM_CORRECAO', 'AGUARDANDO_REVALIDACAO', 'REABERTO']);
const SEVERITIES = ['', 'CRITICA', 'ALTA', 'MEDIA', 'BAIXA'];
const STATES = ['', 'ABERTO', 'ATRIBUIDO', 'EM_CORRECAO', 'AGUARDANDO_REVALIDACAO', 'REABERTO', 'RESOLVIDO'];

function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('pt-BR');
}

function isOverdue(item) {
  if (!item?.prazo || !ACTIVE_STATES.has(item.estado)) return false;
  const deadline = new Date(item.prazo);
  return !Number.isNaN(deadline.getTime()) && deadline.getTime() < Date.now();
}

function BadgeGroup({ title, values }) {
  const entries = Object.entries(values ?? {}).sort((left, right) => right[1] - left[1]);
  return <Section title={title}>
    {entries.length ? <div className="stowage-warning-badges">{entries.map(([key, count]) =>
      <span key={key}><strong>{count}</strong>{key}</span>)}</div> : <EmptyState title="Nenhum aviso ativo" />}
  </Section>;
}

export function YardStowageWarningsPage({ navigate, session }) {
  const [rows, setRows] = useState([]);
  const [summary, setSummary] = useState(null);
  const [history, setHistory] = useState([]);
  const [selected, setSelected] = useState(null);
  const [filters, setFilters] = useState({ search: '', state: '', severity: '', responsible: '', deadline: '' });
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const canOperate = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO');
  const actor = session?.nome || session?.login || 'operador-yard';

  const load = useCallback(async ({ silent = false } = {}) => {
    if (!silent) setLoading(true);
    setError('');
    try {
      const [warnings, warningSummary] = await Promise.all([
        yardStowageWarningApi.listar({ incluirResolvidos: true }),
        yardStowageWarningApi.resumo()
      ]);
      setRows(Array.isArray(warnings) ? warnings : []);
      setSummary(warningSummary ?? null);
      if (selected?.id) {
        const updated = (Array.isArray(warnings) ? warnings : []).find((item) => item.id === selected.id);
        setSelected(updated ?? null);
      }
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível carregar os avisos de estivagem.'));
    } finally {
      if (!silent) setLoading(false);
    }
  }, [selected?.id]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => {
    if (!selected?.id) { setHistory([]); return; }
    yardStowageWarningApi.historico(selected.id)
      .then((items) => setHistory(Array.isArray(items) ? items : []))
      .catch((reason) => setError(formatError(reason, 'Não foi possível carregar o histórico.')));
  }, [selected?.id]);

  const filtered = useMemo(() => rows.filter((item) => {
    const searchable = normalized(`${item.codigoUnidade} ${item.codigoPosicao} ${item.bloco} ${item.regra} ${item.valorObservado} ${item.responsavel}`);
    const deadlineMatches = !filters.deadline
      || (filters.deadline === 'ATRASADO' && isOverdue(item))
      || (filters.deadline === 'SEM_PRAZO' && !item.prazo)
      || (filters.deadline === 'PROXIMAS_24H' && item.prazo && new Date(item.prazo).getTime() >= Date.now()
        && new Date(item.prazo).getTime() <= Date.now() + 86400000);
    return (!filters.search || searchable.includes(normalized(filters.search)))
      && (!filters.state || item.estado === filters.state)
      && (!filters.severity || item.severidade === filters.severity)
      && (!filters.responsible || normalized(item.responsavel).includes(normalized(filters.responsible)))
      && deadlineMatches;
  }), [rows, filters]);

  async function execute(operation, message) {
    if (busy) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      const updated = await operation();
      if (updated?.id) setSelected(updated);
      setSuccess(message);
      await load({ silent: true });
      if (updated?.id) setHistory(await yardStowageWarningApi.historico(updated.id));
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  function assign() {
    if (!selected) return;
    const responsible = window.prompt('Responsável pelo tratamento:', selected.responsavel || '');
    if (!responsible?.trim()) return;
    const deadline = window.prompt('Prazo ISO, por exemplo 2026-07-25T18:00:', selected.prazo?.slice?.(0, 16) || '');
    execute(() => yardStowageWarningApi.atribuir(selected.id, {
      responsavel: responsible.trim(),
      prazo: deadline?.trim() || null,
      ator: actor
    }), 'Aviso atribuído.');
  }

  function startCorrection() {
    if (!selected) return;
    const correctiveAction = window.prompt('Descreva a ação corretiva executada:', selected.acaoCorretiva || '');
    if (!correctiveAction?.trim()) return;
    const evidence = window.prompt('Informe a evidência disponível:', '') || '';
    execute(() => yardStowageWarningApi.iniciarCorrecao(selected.id, {
      acaoCorretiva: correctiveAction.trim(), evidencia: evidence.trim(), ator: actor
    }), 'Correção iniciada e auditada.');
  }

  function submitRevalidation() {
    if (!selected) return;
    const evidence = window.prompt('Evidência final da correção:', '') || '';
    execute(() => yardStowageWarningApi.enviarRevalidacao(selected.id, {
      evidencia: evidence.trim(), ator: actor
    }), 'Aviso enviado para revalidação.');
  }

  function revalidate() {
    if (!selected) return;
    const evidence = window.prompt('Evidência da inspeção/revalidação:', '') || '';
    execute(() => yardStowageWarningApi.revalidar(selected.id, {
      evidencia: evidence.trim(), ator: actor
    }), 'Revalidação concluída com base na condição física atual.');
  }

  function scan() {
    execute(() => yardStowageWarningApi.varrer(actor), 'Inventário varrido e avisos sincronizados.');
  }

  return <>
    <YardPageHeader
      path="/home/patio/avisos-estivagem"
      navigate={navigate}
      title="Avisos de estivagem"
      description="Fila persistida de violações físicas com atribuição, correção, evidências, revalidação, resolução e reabertura."
      actions={<div className="actions"><button className="secondary" onClick={() => load()} disabled={busy}>Atualizar</button>{canOperate && <button onClick={scan} disabled={busy}>Varrer inventário</button>}</div>}
    />
    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>
    <div className="metrics-grid">
      <MetricCard label="Avisos ativos" value={summary?.ativos ?? 0} />
      <MetricCard label="Críticos" value={summary?.criticos ?? 0} detail="Bloqueiam planejamento e dispatch incompatíveis" />
      <MetricCard label="Unidades afetadas" value={Object.keys(summary?.porUnidade ?? {}).length} />
      <MetricCard label="Posições afetadas" value={Object.keys(summary?.porPosicao ?? {}).length} />
    </div>
    <div className="stowage-warning-scope-grid">
      <BadgeGroup title="Badges por bloco" values={summary?.porBloco} />
      <BadgeGroup title="Badges por pilha" values={summary?.porPilha} />
      <BadgeGroup title="Badges por posição" values={summary?.porPosicao} />
      <BadgeGroup title="Badges por unidade" values={summary?.porUnidade} />
    </div>
    <Section title="Filtros da fila" description="Priorize severidade, responsável e prazo sem perder o histórico resolvido.">
      <div className="filter-grid">
        <FilterField label="Busca"><input value={filters.search} onChange={(event) => setFilters((current) => ({ ...current, search: event.target.value }))} placeholder="Unidade, posição, regra ou valor" /></FilterField>
        <FilterField label="Estado"><select value={filters.state} onChange={(event) => setFilters((current) => ({ ...current, state: event.target.value }))}>{STATES.map((value) => <option key={value} value={value}>{value || 'Todos'}</option>)}</select></FilterField>
        <FilterField label="Severidade"><select value={filters.severity} onChange={(event) => setFilters((current) => ({ ...current, severity: event.target.value }))}>{SEVERITIES.map((value) => <option key={value} value={value}>{value || 'Todas'}</option>)}</select></FilterField>
        <FilterField label="Responsável"><input value={filters.responsible} onChange={(event) => setFilters((current) => ({ ...current, responsible: event.target.value }))} placeholder="Nome do responsável" /></FilterField>
        <FilterField label="Prazo"><select value={filters.deadline} onChange={(event) => setFilters((current) => ({ ...current, deadline: event.target.value }))}><option value="">Todos</option><option value="ATRASADO">Atrasados</option><option value="PROXIMAS_24H">Próximas 24h</option><option value="SEM_PRAZO">Sem prazo</option></select></FilterField>
      </div>
    </Section>
    {loading ? <Loading label="Carregando avisos de estivagem..." /> : <div className="split-grid stowage-warning-split">
      <Section title={`Fila operacional (${filtered.length})`}>
        {filtered.length ? <DataTable rows={filtered} rowKey={(row) => row.id} onRowClick={setSelected} columns={[
          { key: 'severidade', label: 'Severidade', render: (row) => <StatusBadge value={row.severidade} /> },
          { key: 'estado', label: 'Estado', render: (row) => <StatusBadge value={row.estado} /> },
          { key: 'codigoUnidade', label: 'Unidade' },
          { key: 'codigoPosicao', label: 'Posição' },
          { key: 'regra', label: 'Regra' },
          { key: 'responsavel', label: 'Responsável' },
          { key: 'prazo', label: 'Prazo', render: (row) => <span className={isOverdue(row) ? 'deadline-overdue' : ''}>{formatDateTime(row.prazo)}</span> }
        ]} /> : <EmptyState title="Nenhum aviso corresponde aos filtros" />}
      </Section>
      <Section title="Tratamento do aviso">
        {!selected ? <EmptyState title="Selecione um aviso" description="A regra, a evidência e todo o ciclo auditável aparecerão aqui." /> : <>
          <DetailGrid value={selected} fields={[
            ['id', 'ID'], ['codigoUnidade', 'Unidade'], ['codigoPosicao', 'Posição'], ['bloco', 'Bloco'],
            ['regra', 'Regra violada'], ['severidade', 'Severidade'], ['estado', 'Estado'],
            ['valorObservado', 'Valor observado'], ['valorEsperado', 'Valor esperado'], ['acaoSugerida', 'Ação sugerida'],
            ['responsavel', 'Responsável'], ['prazo', 'Prazo'], ['acaoCorretiva', 'Ação corretiva'],
            ['evidencia', 'Evidência'], ['resultadoRevalidacao', 'Resultado da revalidação'],
            ['ultimaRevalidacaoEm', 'Última revalidação'], ['ocorrencias', 'Ocorrências']
          ]} />
          {canOperate && ACTIVE_STATES.has(selected.estado) && <div className="actions stowage-warning-actions">
            <button type="button" className="secondary" disabled={busy} onClick={assign}>Atribuir</button>
            {!['EM_CORRECAO', 'AGUARDANDO_REVALIDACAO'].includes(selected.estado) && <button type="button" disabled={busy} onClick={startCorrection}>Iniciar correção</button>}
            {selected.estado === 'EM_CORRECAO' && <button type="button" disabled={busy} onClick={submitRevalidation}>Enviar para revalidação</button>}
            {selected.estado === 'AGUARDANDO_REVALIDACAO' && <button type="button" disabled={busy} onClick={revalidate}>Revalidar condição</button>}
          </div>}
          <h3>Histórico auditável</h3>
          {history.length ? <div className="stowage-warning-history">{history.map((event) => <article key={event.id}>
            <div><StatusBadge value={event.evento} /><span>{formatDateTime(event.ocorridoEm)}</span></div>
            <strong>{event.estadoAnterior || 'NOVO'} → {event.estadoNovo}</strong>
            <p>{event.detalhes || 'Sem detalhes adicionais.'}</p>
            <small>Ator: {event.ator}{event.evidencia ? ` · Evidência: ${event.evidencia}` : ''}</small>
          </article>)}</div> : <EmptyState title="Histórico ainda não disponível" />}
        </>}
      </Section>
    </div>}
    <Section title="Manual contextual" description="Fluxo obrigatório para que a conclusão operacional não encerre o caso sem inspeção.">
      <ol className="stowage-warning-manual">
        <li>Varrer o inventário para detectar violações usando peso, altura, tipo, reefer, perigoso, capacidade, reserva, apoio e regra da pilha.</li>
        <li>Atribuir responsável e prazo. Avisos críticos bloqueiam novo planejamento e dispatch incompatíveis.</li>
        <li>Registrar a ação corretiva e anexar evidência operacional.</li>
        <li>Enviar para revalidação. A conclusão da instrução de trabalho, isoladamente, não encerra o aviso.</li>
        <li>Revalidar a condição física. O caso só fica resolvido quando a violação deixa de existir; se voltar, o mesmo caso é reaberto.</li>
      </ol>
      <a href="https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/patio-avisos-estivagem.md" target="_blank" rel="noreferrer">Abrir manual completo</a>
    </Section>
  </>;
}
