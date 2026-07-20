import { useEffect, useMemo, useState } from 'react';
import { api, formatError, hasAnyRole, sanitizeText } from '../../api.js';
import { DataTable, EmptyState, Loading, Message, MetricCard, Section, StatusBadge } from '../../components.jsx';
import { dispatchApi } from '../../dispatchApi.js';
import { displayValue, YardPageHeader } from './YardShared.jsx';

const INITIAL_ROUTE = {
  origem: '', destino: '', distanciaMetros: '', sentido: '', congestionamentoPercentual: 0,
  bloqueado: false, motivoInterdicao: '', limiteRegionalChe: ''
};

export function DispatchEquipamentosPage({ navigate, session }) {
  const [summary, setSummary] = useState(null);
  const [queues, setQueues] = useState([]);
  const [configurations, setConfigurations] = useState([]);
  const [routes, setRoutes] = useState([]);
  const [ranking, setRanking] = useState([]);
  const [selectedQueueId, setSelectedQueueId] = useState('');
  const [selectedOrderId, setSelectedOrderId] = useState('');
  const [routeForm, setRouteForm] = useState(INITIAL_ROUTE);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const canConfigure = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR');
  const selectedQueue = useMemo(
    () => queues.find((queue) => String(queue.id) === String(selectedQueueId)),
    [queues, selectedQueueId]
  );

  async function load() {
    setLoading(true);
    setError('');
    try {
      const [summaryData, queueData, configurationData, routeData] = await Promise.all([
        dispatchApi.obterResumo(),
        api.listarWorkQueuesPatio(),
        dispatchApi.listarConfiguracoes(),
        dispatchApi.listarRotas()
      ]);
      setSummary(summaryData ?? {});
      setQueues(Array.isArray(queueData) ? queueData : []);
      setConfigurations(Array.isArray(configurationData) ? configurationData : []);
      setRoutes(Array.isArray(routeData) ? routeData : []);
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function loadRanking() {
    if (!selectedQueue?.id || !selectedQueue?.equipamentoPatioId) {
      setError('Selecione uma work queue ativa com CHE real associado.');
      return;
    }
    setBusy(true); setError(''); setSuccess('');
    try {
      const data = await dispatchApi.obterRanking(selectedQueue.id, selectedQueue.equipamentoPatioId);
      setRanking(Array.isArray(data) ? data : []);
      setSelectedOrderId(data?.find((item) => item.elegivel)?.ordemTrabalhoPatioId ?? '');
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  async function dispatchSelected() {
    const selected = ranking.find((item) => String(item.ordemTrabalhoPatioId) === String(selectedOrderId));
    if (!selectedQueue || !selected) {
      setError('Selecione uma fila e uma work instruction elegível.');
      return;
    }
    setBusy(true); setError(''); setSuccess('');
    try {
      const decision = await dispatchApi.autoDispatch({
        workQueueId: selectedQueue.id,
        equipamentoPatioId: selectedQueue.equipamentoPatioId,
        ordemTrabalhoPatioId: selected.ordemTrabalhoPatioId,
        codigoUnidade: selected.codigoUnidade,
        operador: session?.nome || session?.email || 'operador-portal',
        faseVisita: 'EM_OPERACAO',
        pow: selectedQueue.pow,
        pool: selectedQueue.poolOperacional,
        chaveIdempotencia: `PORTAL-${selectedQueue.id}-${selected.ordemTrabalhoPatioId}-${Date.now()}`,
        motivo: 'Autodespacho confirmado na tela Dispatch e equipamentos.',
        correlationId: `portal-${Date.now()}`
      });
      setSuccess(`Work instruction ${decision.ordemTrabalhoPatioId} atribuída ao CHE ${decision.equipamento}.`);
      await load();
      await loadRanking();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  async function activateConfiguration(id) {
    setBusy(true); setError(''); setSuccess('');
    try {
      await dispatchApi.ativarConfiguracao(id);
      setSuccess(`Configuração ${id} ativada.`);
      await load();
    } catch (reason) {
      setError(formatError(reason));
    } finally { setBusy(false); }
  }

  async function rollbackConfiguration(id) {
    const reason = globalThis.prompt?.('Motivo do rollback:');
    if (!sanitizeText(reason)) return;
    setBusy(true); setError(''); setSuccess('');
    try {
      await dispatchApi.rollbackConfiguracao(id, reason);
      setSuccess(`Rollback da configuração ${id} criado e ativado como nova versão.`);
      await load();
    } catch (reasonError) {
      setError(formatError(reasonError));
    } finally { setBusy(false); }
  }

  async function saveRoute(event) {
    event.preventDefault();
    setBusy(true); setError(''); setSuccess('');
    try {
      await dispatchApi.criarRota({
        ...routeForm,
        distanciaMetros: Number(routeForm.distanciaMetros),
        congestionamentoPercentual: Number(routeForm.congestionamentoPercentual || 0),
        limiteRegionalChe: routeForm.limiteRegionalChe ? Number(routeForm.limiteRegionalChe) : null
      });
      setRouteForm(INITIAL_ROUTE);
      setSuccess('Nova versão do segmento de rota criada.');
      await load();
    } catch (reason) {
      setError(formatError(reason));
    } finally { setBusy(false); }
  }

  return <>
    <YardPageHeader
      path="/home/patio/dispatch-equipamentos"
      navigate={navigate}
      title="Dispatch e equipamentos"
      description="Seleciona work instructions por família de CHE, rota, telemetria, prioridade, atraso, capacidade e restrições operacionais."
      actions={<button className="secondary" type="button" onClick={load} disabled={loading || busy}>Atualizar</button>}
    />
    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>
    {loading ? <Loading label="Carregando dispatch e equipamentos..." /> : <>
      <div className="metrics-grid">
        <MetricCard label="Configurações ativas" value={summary?.configuracoesAtivas ?? 0} />
        <MetricCard label="Decisões em 24 horas" value={summary?.decisoesUltimas24Horas ?? 0} />
        <MetricCard label="Instruções em execução" value={summary?.instrucoesEmExecucao ?? 0} />
        <MetricCard label="Telemetrias atrasadas" value={summary?.telemetriasAtrasadas ?? 0} />
        <MetricCard label="Auxiliares reservados" value={summary?.auxiliaresReservados ?? 0} />
      </div>

      <Section title="Seleção e autodespacho" description="A work queue precisa estar ativa, coberta por POW/pool e associada a um CHE operacional.">
        <div className="form-grid">
          <label className="field"><span>Work queue</span><select value={selectedQueueId} onChange={(event) => { setSelectedQueueId(event.target.value); setRanking([]); setSelectedOrderId(''); }}><option value="">Selecione</option>{queues.map((queue) => <option key={queue.id} value={queue.id}>{queue.identificador} · {queue.status} · {queue.equipamento || 'sem CHE'}</option>)}</select></label>
          <label className="field"><span>CHE associado</span><input value={selectedQueue?.equipamento || ''} readOnly placeholder="Selecione uma fila" /></label>
          <label className="field"><span>POW</span><input value={selectedQueue?.pow || ''} readOnly /></label>
          <label className="field"><span>Pool</span><input value={selectedQueue?.poolOperacional || ''} readOnly /></label>
        </div>
        <div className="actions"><button type="button" className="secondary" disabled={busy || !selectedQueueId} onClick={loadRanking}>Calcular ranking</button><button type="button" disabled={busy || !selectedOrderId} onClick={dispatchSelected}>{busy ? 'Processando...' : 'Despachar selecionada'}</button></div>
        {!ranking.length ? <EmptyState title="Ranking ainda não calculado" description="Selecione uma work queue e calcule o ranking auditável." /> : <DataTable
          rows={ranking}
          rowKey="ordemTrabalhoPatioId"
          onRowClick={(row) => row.elegivel && setSelectedOrderId(row.ordemTrabalhoPatioId)}
          columns={[
            { key: 'posicao', label: '#' },
            { key: 'codigoUnidade', label: 'Unidade' },
            { key: 'origem', label: 'Origem' },
            { key: 'destino', label: 'Destino' },
            { key: 'score', label: 'Score' },
            { key: 'etaSegundos', label: 'ETA (s)' },
            { key: 'elegivel', label: 'Estado', render: (row) => <StatusBadge value={row.elegivel ? 'ELEGÍVEL' : 'BLOQUEADA'} /> },
            { key: 'motivosBloqueio', label: 'Bloqueios', render: (row) => (row.motivosBloqueio ?? []).join(' ') || '—' },
            { key: 'memoriaCalculo', label: 'Memória de cálculo' }
          ]}
        />}
      </Section>

      <Section title="Últimas decisões" description="Cada decisão mantém configuração, score, rota, ETA, telemetria, auxiliar e ciclo de etapas.">
        <DataTable rows={summary?.ultimasDecisoes ?? []} rowKey="id" columns={[
          { key: 'id', label: 'Decisão' },
          { key: 'ordemTrabalhoPatioId', label: 'WI' },
          { key: 'equipamento', label: 'CHE' },
          { key: 'tipoEquipamento', label: 'Família' },
          { key: 'modo', label: 'Modo' },
          { key: 'score', label: 'Score' },
          { key: 'etaSegundos', label: 'ETA (s)' },
          { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
          { key: 'auxiliar', label: 'Auxiliar', render: (row) => row.auxiliar?.identificacao || '—' },
          { key: 'criadoEm', label: 'Criada em', render: (row) => displayValue(row.criadoEm) }
        ]} emptyTitle="Nenhuma decisão registrada" />
      </Section>

      <Section title="Configurações versionadas" description="A resolução aplica FILA, POOL, POW, BLOCO, PÁTIO e TERMINAL, nessa ordem.">
        <DataTable rows={configurations} rowKey="id" columns={[
          { key: 'id', label: 'ID' },
          { key: 'tipoEquipamento', label: 'CHE' },
          { key: 'tipoEscopo', label: 'Escopo' },
          { key: 'valorEscopo', label: 'Valor' },
          { key: 'versao', label: 'Versão' },
          { key: 'modo', label: 'Modo' },
          { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
          { key: 'vigenteDe', label: 'Vigente de', render: (row) => displayValue(row.vigenteDe) },
          { key: 'actions', label: 'Ações', render: (row) => canConfigure ? <div className="actions"><button className="secondary small" disabled={busy || row.status === 'ATIVA'} onClick={() => activateConfiguration(row.id)}>Ativar</button><button className="secondary small" disabled={busy} onClick={() => rollbackConfiguration(row.id)}>Rollback</button></div> : 'Somente leitura' }
        ]} />
      </Section>

      <Section title="Rotas, interdições e congestionamento" description="Cada alteração cria uma nova versão e desativa o segmento anterior.">
        {canConfigure && <form className="form-grid" onSubmit={saveRoute}>
          <label className="field"><span>Origem</span><input required value={routeForm.origem} onChange={(event) => setRouteForm({ ...routeForm, origem: event.target.value })} /></label>
          <label className="field"><span>Destino</span><input required value={routeForm.destino} onChange={(event) => setRouteForm({ ...routeForm, destino: event.target.value })} /></label>
          <label className="field"><span>Distância em metros</span><input required type="number" min="0" step="0.1" value={routeForm.distanciaMetros} onChange={(event) => setRouteForm({ ...routeForm, distanciaMetros: event.target.value })} /></label>
          <label className="field"><span>Congestionamento (%)</span><input type="number" min="0" max="100" step="0.1" value={routeForm.congestionamentoPercentual} onChange={(event) => setRouteForm({ ...routeForm, congestionamentoPercentual: event.target.value })} /></label>
          <label className="field"><span>Limite regional de CHE</span><input type="number" min="1" value={routeForm.limiteRegionalChe} onChange={(event) => setRouteForm({ ...routeForm, limiteRegionalChe: event.target.value })} /></label>
          <label className="field"><span>Sentido</span><input value={routeForm.sentido} onChange={(event) => setRouteForm({ ...routeForm, sentido: event.target.value })} /></label>
          <label className="field"><span>Interdição</span><select value={routeForm.bloqueado ? 'SIM' : 'NAO'} onChange={(event) => setRouteForm({ ...routeForm, bloqueado: event.target.value === 'SIM' })}><option value="NAO">Liberada</option><option value="SIM">Bloqueada</option></select></label>
          <label className="field"><span>Motivo da interdição</span><input value={routeForm.motivoInterdicao} onChange={(event) => setRouteForm({ ...routeForm, motivoInterdicao: event.target.value })} /></label>
          <div className="actions"><button type="submit" disabled={busy}>Criar versão da rota</button></div>
        </form>}
        <DataTable rows={routes} rowKey="id" columns={[
          { key: 'origem', label: 'Origem' }, { key: 'destino', label: 'Destino' },
          { key: 'distanciaMetros', label: 'Distância (m)' }, { key: 'congestionamentoPercentual', label: 'Congest. (%)' },
          { key: 'limiteRegionalChe', label: 'Limite CHE' },
          { key: 'bloqueado', label: 'Estado', render: (row) => <StatusBadge value={row.bloqueado ? 'INTERDITADA' : row.ativo ? 'ATIVA' : 'INATIVA'} /> },
          { key: 'versao', label: 'Versão' }, { key: 'motivoInterdicao', label: 'Motivo' }
        ]} emptyTitle="Nenhuma rota parametrizada" />
      </Section>
    </>}
  </>;
}
