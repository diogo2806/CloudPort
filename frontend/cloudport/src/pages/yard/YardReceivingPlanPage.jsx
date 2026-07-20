import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, request, sanitizeText } from '../../api.js';
import { DataTable, EmptyState, JsonDetails, Loading, Message, MetricCard, Section, StatusBadge } from '../../components.jsx';
import { gateOperationsApi } from '../../gateOperationsApi.js';
import { displayValue, YardPageHeader } from './YardShared.jsx';
import { PredictivePositionPlansPanel } from './PredictivePositionPlansPanel.jsx';
import { buildReceivingRows, countReceivingRows } from './yardReceivingPlanModel.js';

function blockersText(row) {
  return row.blockers?.length ? row.blockers.join(' ') : row.warnings?.join(' ') || 'Sem bloqueios';
}

function visitLabel(visit) {
  return `${visit.codigo || `Visita #${visit.id}`} · ${visit.placa || 'sem placa'} · ${visit.stageAtualNome || 'sem estágio'}`;
}

export function YardReceivingPlanPage({ navigate }) {
  const [facilityId, setFacilityId] = useState('');
  const [dashboard, setDashboard] = useState(null);
  const [complements, setComplements] = useState({ billsOfLading: [], regrasAcesso: [] });
  const [selectedVisitId, setSelectedVisitId] = useState(null);
  const [plan, setPlan] = useState(null);
  const [selectedGroup, setSelectedGroup] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadGate = useCallback(async (targetFacility) => {
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      const payload = await gateOperationsApi.obterPainel(targetFacility || undefined);
      const selectedFacilityId = payload?.facilitySelecionadaId;
      const complementaryPayload = selectedFacilityId
        ? await gateOperationsApi.listarComplementos(selectedFacilityId)
        : { billsOfLading: [], regrasAcesso: [] };
      const visits = payload?.visitasAtivas ?? [];

      setDashboard(payload);
      setComplements(complementaryPayload ?? { billsOfLading: [], regrasAcesso: [] });
      setFacilityId(String(selectedFacilityId ?? ''));
      setSelectedVisitId((current) => visits.some((visit) => String(visit.id) === String(current))
        ? current
        : visits[0]?.id ?? null);
    } catch (reason) {
      setDashboard(null);
      setComplements({ billsOfLading: [], regrasAcesso: [] });
      setSelectedVisitId(null);
      setError(formatError(reason, 'Não foi possível carregar o contexto operacional do Gate.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadGate(); }, [loadGate]);
  useEffect(() => {
    setPlan(null);
    setSelectedGroup(null);
    setSuccess('');
  }, [selectedVisitId]);

  const visits = dashboard?.visitasAtivas ?? [];
  const selectedVisit = visits.find((visit) => String(visit.id) === String(selectedVisitId)) ?? null;
  const references = dashboard?.referencias ?? {};
  const receivingRows = useMemo(
    () => buildReceivingRows(selectedVisit, references, complements),
    [selectedVisit, references, complements]
  );
  const summary = useMemo(() => countReceivingRows(receivingRows), [receivingRows]);
  const eligibleRows = useMemo(() => receivingRows.filter((row) => row.eligible), [receivingRows]);
  const blockedRows = useMemo(() => receivingRows.filter((row) => !row.eligible), [receivingRows]);
  const groups = plan?.grupos ?? [];
  const selectedContainers = selectedGroup?.conteineres ?? [];
  const alerts = useMemo(() => [
    ...(plan?.alertas ?? []).map((message) => ({ scope: 'PLANO', message })),
    ...(selectedGroup?.alertas ?? []).map((message) => ({ scope: 'GRUPO', message }))
  ], [plan, selectedGroup]);
  const decisionRows = useMemo(() => selectedContainers.map((container) => {
    const source = receivingRows.find((row) => String(row.id) === String(container.id));
    return {
      id: container.id,
      unidade: container.codigo,
      operacao: source?.transaction?.tipoOperacao || container.categoria,
      referencia: source?.referenceLabel || 'Sem referência comercial',
      grupo: selectedGroup?.nome,
      posicao: 'Plano preditivo abaixo',
      exchangeArea: 'Pendente de reserva',
      workInstruction: 'Pendente de confirmação',
      status: 'SIMULADA'
    };
  }), [receivingRows, selectedContainers, selectedGroup]);

  async function generatePlan() {
    if (busy || !selectedVisit) return;
    if (!eligibleRows.length) {
      setError('A visita selecionada não possui transações elegíveis para planejamento.');
      return;
    }

    setBusy(true);
    setError('');
    setSuccess('');
    try {
      const response = await request('/yard/patio/planejamento-recebimento', {
        method: 'POST',
        body: eligibleRows.map((row) => row.container)
      });
      setPlan(response);
      setSelectedGroup(response?.grupos?.[0] ?? null);
      setSuccess('Simulação gerada a partir das transações elegíveis da truck visit. Nenhuma reserva operacional foi confirmada.');
    } catch (reason) {
      setPlan(null);
      setSelectedGroup(null);
      setError(formatError(reason, 'Não foi possível simular o plano de recebimento.'));
    } finally {
      setBusy(false);
    }
  }

  const transactionColumns = useMemo(() => [
    { key: 'sequence', label: '#', render: (row) => row.transaction?.sequencia ?? '—' },
    { key: 'unit', label: 'Unidade', render: (row) => row.unit || 'Não identificada' },
    { key: 'operation', label: 'Operação', render: (row) => row.transaction?.tipoOperacao || '—' },
    { key: 'reference', label: 'Referência comercial', render: (row) => sanitizeText(row.referenceLabel) },
    { key: 'status', label: 'Transação', render: (row) => <StatusBadge value={row.transaction?.status || 'PENDENTE'} /> },
    { key: 'eligibility', label: 'Elegibilidade', render: (row) => <StatusBadge value={row.eligible ? 'ELEGIVEL' : 'BLOQUEADA'} /> },
    { key: 'reason', label: 'Decisão', render: (row) => sanitizeText(blockersText(row)) }
  ], []);

  return <>
    <YardPageHeader
      path="/home/patio/planejamento-recebimento"
      navigate={navigate}
      title="Recebimento integrado Gate × Yard"
      description="Seleciona uma truck visit, valida referências comerciais e simula o agrupamento das unidades elegíveis antes da reserva de posição, exchange area e work instruction."
      actions={<div className="inline">
        <select
          aria-label="Instalação do Gate"
          value={facilityId}
          onChange={(event) => { setFacilityId(event.target.value); loadGate(event.target.value); }}
          disabled={loading || busy}
        >
          {(dashboard?.facilities ?? []).map((facility) => <option value={facility.id} key={facility.id}>{facility.nome}</option>)}
        </select>
        <button className="secondary" type="button" onClick={() => loadGate(facilityId)} disabled={loading || busy}>Atualizar</button>
        <button type="button" disabled={loading || busy || !selectedVisit || !eligibleRows.length} onClick={generatePlan}>
          {busy ? 'Simulando...' : 'Simular planejamento'}
        </button>
      </div>}
    />

    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>

    {loading ? <Loading label="Carregando visitas e referências do Gate..." /> : !dashboard ? null : <>
      <div className="metrics-grid">
        <MetricCard label="Visitas ativas" value={visits.length} />
        <MetricCard label="Transações da visita" value={summary.total} />
        <MetricCard label="Elegíveis" value={summary.eligible} detail={`${summary.withWarnings} com aviso(s)`} />
        <MetricCard label="Bloqueadas" value={summary.blocked} />
      </div>

      <div className="split-grid">
        <Section title="Contexto da truck visit" description="A entrada operacional vem do Gate; não é necessário montar ou colar o contrato JSON da API.">
          <label className="field">
            <span>Truck visit</span>
            <select value={selectedVisitId ?? ''} onChange={(event) => setSelectedVisitId(event.target.value)}>
              {!visits.length && <option value="">Nenhuma visita ativa</option>}
              {visits.map((visit) => <option value={visit.id} key={visit.id}>{visitLabel(visit)}</option>)}
            </select>
          </label>
          {!selectedVisit ? <EmptyState title="Nenhuma truck visit disponível" description="Crie ou faça check-in de uma visita no Gate para iniciar o planejamento." /> : <div className="detail-grid">
            <div className="detail-row"><span>Visita</span><strong>{displayValue(selectedVisit.codigo)}</strong></div>
            <div className="detail-row"><span>Veículo</span><strong>{displayValue(selectedVisit.placa)}</strong></div>
            <div className="detail-row"><span>Transportadora</span><strong>{displayValue(selectedVisit.transportadora)}</strong></div>
            <div className="detail-row"><span>Pista</span><strong>{displayValue(selectedVisit.laneCodigo)}</strong></div>
            <div className="detail-row"><span>Estágio</span><strong>{displayValue(selectedVisit.stageAtualNome)}</strong></div>
            <div className="detail-row"><span>Status</span><strong><StatusBadge value={selectedVisit.status} /></strong></div>
          </div>}
        </Section>

        <Section title="Decisão operacional" description="A tela distingue simulação de confirmação para não apresentar reservas inexistentes como concluídas.">
          <div className="detail-grid">
            <div className="detail-row"><span>Unidades</span><strong>Transações elegíveis da visita</strong></div>
            <div className="detail-row"><span>Referências</span><strong>Booking, BL, ordem e pré-aviso</strong></div>
            <div className="detail-row"><span>Posição</span><strong>Calculada pelo planejamento preditivo</strong></div>
            <div className="detail-row"><span>Exchange area</span><strong>Reserva pendente no fluxo Gate × Yard</strong></div>
            <div className="detail-row"><span>Work instruction</span><strong>Criação pendente de confirmação operacional</strong></div>
            <div className="detail-row"><span>Estado desta ação</span><strong><StatusBadge value={plan ? 'SIMULADA' : 'NAO_SIMULADA'} /></strong></div>
          </div>
          <Message>“Simular planejamento” não reserva posição, exchange area nem work instruction. A confirmação deve ocorrer no orquestrador transacional do BUS1510.</Message>
        </Section>
      </div>

      <Section title="Transações da visita" description="Somente unidades identificadas, sem trouble e com referência comercial compatível seguem para a simulação.">
        <DataTable
          rows={receivingRows}
          rowKey="id"
          columns={transactionColumns}
          emptyTitle="A visita não possui transações"
          gridId="yard-receiving-gate-transactions"
          exportFileName="transacoes-recebimento-gate-yard"
        />
      </Section>

      {blockedRows.length > 0 && <Section title="Motivos de bloqueio" description="Corrija a origem no Gate antes de tentar planejar novamente.">
        <div className="card-list">
          {blockedRows.map((row) => <article className="content-card" key={row.id}>
            <div className="card-meta"><StatusBadge value="BLOQUEADA" /><span>{row.unit || `Transação #${row.transaction?.sequencia}`}</span></div>
            <p>{sanitizeText(row.blockers.join(' '))}</p>
          </article>)}
        </div>
      </Section>}

      {plan && <>
        <div className="metrics-grid">
          <MetricCard label="Contêineres simulados" value={plan.totalConteineres ?? 0} />
          <MetricCard label="Grupos sugeridos" value={plan.totalGrupos ?? 0} />
          <MetricCard label="Capacidade" value={`${plan.totalTeus ?? 0} TEU`} />
          <MetricCard label="Peso previsto" value={`${Number(plan.pesoTotalToneladas ?? 0).toFixed(3)} t`} />
        </div>

        <Section title="Grupos sugeridos" description="A prioridade menor deve ser tratada primeiro pelo planejador; a seleção ainda é uma simulação.">
          <DataTable
            rows={groups}
            rowKey="chaveAgrupamento"
            onRowClick={setSelectedGroup}
            columns={[
              { key: 'prioridade', label: 'Prioridade' },
              { key: 'nome', label: 'Grupo', render: (row) => sanitizeText(row.nome) },
              { key: 'inicioJanelaRecebimento', label: 'Início', render: (row) => displayValue(row.inicioJanelaRecebimento) },
              { key: 'fimJanelaRecebimento', label: 'Fim', render: (row) => displayValue(row.fimJanelaRecebimento) },
              { key: 'quantidadeConteineres', label: 'Contêineres' },
              { key: 'teus', label: 'TEU' },
              { key: 'perigoso', label: 'Segregação', render: (row) => <StatusBadge value={row.perigoso ? `IMO_${row.classeImo}` : row.refrigerado ? 'REEFER' : 'PADRAO'} /> }
            ]}
            emptyTitle="Nenhum grupo foi gerado"
          />
        </Section>

        <div className="split-grid">
          <Section title="Inspector da seleção automática" description="Exibe em uma única decisão a unidade, referências e etapas operacionais ainda pendentes.">
            {!selectedGroup ? <EmptyState title="Selecione um grupo" /> : <DataTable
              rows={decisionRows}
              rowKey="id"
              columns={[
                { key: 'unidade', label: 'Unidade' },
                { key: 'operacao', label: 'Operação' },
                { key: 'referencia', label: 'Referência' },
                { key: 'posicao', label: 'Posição' },
                { key: 'exchangeArea', label: 'Exchange area' },
                { key: 'workInstruction', label: 'Work instruction' },
                { key: 'status', label: 'Estado', render: (row) => <StatusBadge value={row.status} /> }
              ]}
              emptyTitle="Grupo sem unidades"
            />}
          </Section>

          <Section title="Alertas e diagnóstico">
            {!alerts.length ? <Message type="success">Nenhuma pendência crítica identificada pelo agrupador.</Message> : <div className="card-list">
              {alerts.map((alert, index) => <article className="content-card" key={`${alert.scope}-${index}`}>
                <div className="card-meta"><StatusBadge value={alert.scope} /></div>
                <p>{sanitizeText(alert.message)}</p>
              </article>)}
            </div>}
            <JsonDetails value={selectedGroup ?? plan} title="Diagnóstico técnico da simulação" />
          </Section>
        </div>
      </>}
    </>}

    <PredictivePositionPlansPanel />
  </>;
}
