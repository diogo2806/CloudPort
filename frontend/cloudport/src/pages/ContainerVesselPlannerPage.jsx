import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, formatError, hasAnyRole, normalizePage, sanitizeText } from '../api.js';
import '../vessel-planner.css';
import { DataTable, EmptyState, JsonDetails, Loading, Message, MetricCard, PageHeader, Section, StatusBadge } from '../components.jsx';

const PROCESSING_TERMINAL_STATUS = new Set(['CONCLUIDO', 'REJEITADO', 'QUARENTENA']);

function displayValue(value) {
  if (value === undefined || value === null || value === '') return '—';
  if (typeof value === 'boolean') return value ? 'Sim' : 'Não';
  if (typeof value === 'number') return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 }).format(value);
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)) {
    const date = new Date(value);
    if (!Number.isNaN(date.getTime())) return date.toLocaleString('pt-BR');
  }
  return String(value);
}

function entityKey(entity, index = 0) {
  return String(entity?.id ?? entity?.visitaId ?? entity?.escalaId ?? entity?.codigo ?? entity?.codigoImo ?? index);
}

function scaleLabel(scale) {
  const vessel = scale?.nomeNavio ?? scale?.navioNome ?? scale?.codigoNavio ?? 'Navio sem identificação';
  const voyage = scale?.viagemEntrada ?? scale?.codigoViagem ?? scale?.viagem ?? 'viagem não informada';
  return `${vessel} · ${voyage}`;
}

function inferColumns(rows) {
  if (!Array.isArray(rows) || !rows.length) return [];
  return Object.keys(rows[0]).slice(0, 7).map((key) => ({
    key,
    label: key.replace(/([A-Z])/g, ' $1').replace(/[_-]+/g, ' ').replace(/^./, (letter) => letter.toUpperCase()),
    render: (row) => /status|fase|severidade|nivel/i.test(key)
      ? <StatusBadge value={row?.[key]} />
      : displayValue(row?.[key])
  }));
}

function useRemote(loader, dependencies = [], options = {}) {
  const { immediate = true } = options;
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(immediate);
  const [error, setError] = useState('');

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await loader();
      setData(response);
      return response;
    } catch (reason) {
      setError(formatError(reason));
      return null;
    } finally {
      setLoading(false);
    }
  }, dependencies);

  useEffect(() => {
    if (immediate) reload();
  }, [immediate, reload]);

  return { data, setData, loading, error, setError, reload };
}

function PlannerContext({ scale, bayPlan, plan }) {
  return <div className="planner-context-grid">
    <article><span>Escala</span><strong>{scale ? scaleLabel(scale) : 'Não selecionada'}</strong><small>{scale?.bercoPrevisto ? `Berço ${scale.bercoPrevisto}` : 'Selecione uma escala operacional.'}</small></article>
    <article><span>Bay Plan</span><strong>{bayPlan ? `#${bayPlan.id} · ${bayPlan.codigoNavio}` : 'Não selecionado'}</strong><small>{bayPlan ? `${bayPlan.totalContainers ?? 0} contêiner(es) · versão ${bayPlan.versao ?? '—'}` : 'Importe ou selecione um Bay Plan ativo.'}</small></article>
    <article><span>Plano persistido</span><strong>{plan ? `#${plan.id} · ${plan.status}` : 'Não aberto'}</strong><small>{plan ? `${plan.totalSlotsOcupados ?? 0}/${plan.slots?.length ?? 0} slots ocupados` : 'Crie ou abra um plano existente.'}</small></article>
  </div>;
}

function BaySlotGrid({ plan, selectedSlotId, onSelectSlot }) {
  const slots = Array.isArray(plan?.slots) ? plan.slots : [];
  const bays = useMemo(() => Array.from(new Set(slots.map((slot) => slot.bay))).sort((a, b) => a - b), [slots]);
  const [activeBay, setActiveBay] = useState(null);

  useEffect(() => {
    if (!bays.length) {
      setActiveBay(null);
      return;
    }
    if (!bays.includes(activeBay)) setActiveBay(bays[0]);
  }, [activeBay, bays]);

  const baySlots = useMemo(() => slots
    .filter((slot) => slot.bay === activeBay)
    .sort((left, right) => right.tier - left.tier || left.rowBay - right.rowBay), [activeBay, slots]);

  if (!slots.length) return <EmptyState title="Plano sem slots disponíveis" description="O backend não retornou geometria para este plano." />;

  return <>
    <div className="bay-tabs" role="tablist" aria-label="Bays do navio">
      {bays.map((bay) => <button key={bay} type="button" className={bay === activeBay ? 'active' : 'secondary'} onClick={() => setActiveBay(bay)}>Bay {bay}</button>)}
    </div>
    <div className="vessel-slot-grid" aria-label={`Slots do bay ${activeBay}`}>
      {baySlots.map((slot) => {
        const occupied = Boolean(slot.codigoContainer);
        const restricted = slot.tipoSlot && slot.tipoSlot !== 'NORMAL';
        const selected = String(slot.id) === String(selectedSlotId);
        const classes = ['vessel-slot', occupied ? 'occupied' : 'empty', restricted ? 'restricted' : '', selected ? 'selected' : ''].filter(Boolean).join(' ');
        return <button
          key={slot.id}
          type="button"
          className={classes}
          disabled={occupied}
          onClick={() => onSelectSlot(slot)}
          aria-label={`Bay ${slot.bay}, row ${slot.rowBay}, tier ${slot.tier}${occupied ? `, contêiner ${slot.codigoContainer}` : ', vazio'}`}
        >
          <span>R{slot.rowBay} · T{slot.tier}</span>
          <strong>{slot.codigoContainer || 'Livre'}</strong>
          <small>{slot.tipoSlot || 'NORMAL'} · {slot.maxPesoKg ? `${displayValue(slot.maxPesoKg)} kg` : 'limite não informado'}</small>
          {slot.statusAlertas && <em>{sanitizeText(slot.statusAlertas)}</em>}
        </button>;
      })}
    </div>
  </>;
}

function AnalysisSection({ stability, restow, sequencing }) {
  const restowRows = Array.isArray(restow?.movimentos) ? restow.movimentos : [];
  const sequenceRows = Array.isArray(sequencing?.sequencia) ? sequencing.sequencia : [];
  return <>
    {stability && <Section title="Estabilidade calculada pelo backend" description="Nenhum indicador de segurança é calculado no navegador.">
      <div className="metrics-grid">
        <MetricCard label="Resultado" value={stability.aprovado ? 'Aprovado' : 'Reprovado'} />
        <MetricCard label="Trim" value={`${displayValue(stability.trimMetros)} m`} />
        <MetricCard label="Banda" value={`${displayValue(stability.listGraus)}°`} />
        <MetricCard label="Peso total" value={`${displayValue(stability.pesoTotalToneladas)} t`} />
      </div>
      {Array.isArray(stability.violacoes) && stability.violacoes.length > 0
        ? <DataTable rows={stability.violacoes} columns={inferColumns(stability.violacoes)} emptyTitle="Sem violações" />
        : <EmptyState title="Nenhuma violação retornada" />}
    </Section>}
    {restow && <Section title={`Restow · ${restow.totalRestows ?? restowRows.length} movimento(s)`} description={restow.descricao}>
      <DataTable rows={restowRows} columns={inferColumns(restowRows)} emptyTitle="Nenhum restow necessário" />
    </Section>}
    {sequencing && <Section title={`Sequenciamento · ${sequencing.numGuindastes ?? '—'} guindaste(s)`} description={`${sequencing.totalOperacoes ?? sequenceRows.length} operação(ões) retornadas pelo backend.`}>
      <DataTable rows={sequenceRows} columns={inferColumns(sequenceRows)} emptyTitle="Nenhuma operação sequenciada" />
    </Section>}
  </>;
}

export function ContainerVesselPlannerPage({ session }) {
  const [days, setDays] = useState(30);
  const scales = useRemote(() => api.listarEscalasEmbarque(days), [days]);
  const bayPlans = useRemote(() => api.listarBayPlansAtivos(), []);
  const processings = useRemote(() => api.listarProcessamentosEdi({ tipo: 'BAPLIE', pagina: 0, tamanho: 50 }), []);

  const scaleRows = useMemo(() => normalizePage(scales.data), [scales.data]);
  const processingRows = useMemo(() => normalizePage(processings.data), [processings.data]);
  const bayPlanRows = useMemo(() => normalizePage(bayPlans.data), [bayPlans.data]);

  const [selectedScaleKey, setSelectedScaleKey] = useState('');
  const [selectedBayPlanId, setSelectedBayPlanId] = useState('');
  const [bayPlan, setBayPlan] = useState(null);
  const [plan, setPlan] = useState(null);
  const [planIdToOpen, setPlanIdToOpen] = useState('');
  const [selectedProcessing, setSelectedProcessing] = useState(null);
  const [selectedContainerCode, setSelectedContainerCode] = useState('');
  const [selectedSlotId, setSelectedSlotId] = useState('');
  const [cranes, setCranes] = useState(2);
  const [stability, setStability] = useState(null);
  const [restow, setRestow] = useState(null);
  const [sequencing, setSequencing] = useState(null);
  const [file, setFile] = useState(null);
  const [reprocessReason, setReprocessReason] = useState('');
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const canCommand = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR');
  const planLocked = ['APROVADO', 'VALIDADO', 'FINALIZADO', 'CONCLUIDO'].includes(String(plan?.status ?? '').toUpperCase());

  const selectedScale = useMemo(() => scaleRows.find((row, index) => entityKey(row, index) === selectedScaleKey) ?? null, [scaleRows, selectedScaleKey]);

  useEffect(() => {
    if (!selectedScaleKey && scaleRows.length) setSelectedScaleKey(entityKey(scaleRows[0], 0));
  }, [scaleRows, selectedScaleKey]);

  useEffect(() => {
    if (!selectedBayPlanId && bayPlanRows.length) setSelectedBayPlanId(String(bayPlanRows[0].id));
  }, [bayPlanRows, selectedBayPlanId]);

  useEffect(() => {
    if (!selectedBayPlanId) {
      setBayPlan(null);
      return;
    }
    let active = true;
    setError('');
    api.obterBayPlan(selectedBayPlanId)
      .then((response) => { if (active) setBayPlan(response); })
      .catch((reason) => { if (active) { setBayPlan(null); setError(formatError(reason)); } });
    return () => { active = false; };
  }, [selectedBayPlanId]);

  useEffect(() => {
    const status = selectedProcessing?.status;
    if (!selectedProcessing?.id || PROCESSING_TERMINAL_STATUS.has(status)) return undefined;
    const timer = setInterval(async () => {
      try {
        const updated = await api.obterProcessamentoEdi(selectedProcessing.id);
        setSelectedProcessing(updated);
        processings.setData((current) => {
          const rows = normalizePage(current);
          const content = rows.map((row) => row.id === updated.id ? updated : row);
          return current?.conteudo ? { ...current, conteudo: content } : content;
        });
        if (updated.bayPlanId) {
          setSelectedBayPlanId(String(updated.bayPlanId));
          bayPlans.reload();
        }
      } catch (reason) {
        setError(formatError(reason));
      }
    }, 3000);
    return () => clearInterval(timer);
  }, [bayPlans, processings, selectedProcessing]);

  const allocatedCodes = useMemo(() => new Set((plan?.slots ?? []).map((slot) => slot.codigoContainer).filter(Boolean)), [plan]);
  const unallocated = useMemo(() => (bayPlan?.containers ?? []).filter((container) => !allocatedCodes.has(container.codigoContainer)), [allocatedCodes, bayPlan]);
  const emptySlots = useMemo(() => (plan?.slots ?? []).filter((slot) => !slot.codigoContainer), [plan]);

  useEffect(() => {
    if (!unallocated.some((container) => container.codigoContainer === selectedContainerCode)) {
      setSelectedContainerCode(unallocated[0]?.codigoContainer ?? '');
    }
  }, [selectedContainerCode, unallocated]);

  function resetMessages() {
    setError('');
    setSuccess('');
  }

  async function run(name, action, message) {
    if (busy) return null;
    setBusy(name);
    resetMessages();
    try {
      const response = await action();
      if (message) setSuccess(message);
      return response;
    } catch (reason) {
      setError(formatError(reason));
      return null;
    } finally {
      setBusy('');
    }
  }

  async function refreshPlan(planId = plan?.id) {
    if (!planId) return null;
    const refreshed = await api.obterPlanoVesselPlanner(planId);
    setPlan(refreshed);
    setPlanIdToOpen(String(refreshed.id));
    return refreshed;
  }

  async function uploadBaplie(event) {
    event.preventDefault();
    if (!file) return;
    if (!canCommand) return;
    const form = event.currentTarget;
    const response = await run('upload', () => api.uploadBaplie(file), 'BAPLIE recebido. O processamento será acompanhado automaticamente.');
    if (!response) return;
    setSelectedProcessing(response);
    setFile(null);
    form.reset();
    await processings.reload();
  }

  async function reprocess() {
    if (!canCommand || !selectedProcessing?.id || !reprocessReason.trim()) return;
    const response = await run('reprocess', () => api.reprocessarProcessamentoEdi(selectedProcessing.id, reprocessReason.trim()), 'Reprocessamento solicitado.');
    if (response) {
      setSelectedProcessing(response);
      setReprocessReason('');
      await processings.reload();
    }
  }

  async function createPlan() {
    if (!canCommand || !bayPlan?.id) return;
    const created = await run('create-plan', () => api.criarPlanoVesselPlanner(bayPlan.id), 'Plano criado e persistido.');
    if (created) {
      setPlan(created);
      setPlanIdToOpen(String(created.id));
      setStability(created.estabilidade ?? null);
      setRestow(null);
      setSequencing(null);
    }
  }

  async function openPlan(event) {
    event.preventDefault();
    const id = Number(planIdToOpen);
    if (!Number.isInteger(id) || id <= 0) return;
    const opened = await run('open-plan', () => api.obterPlanoVesselPlanner(id), 'Plano persistido carregado.');
    if (opened) {
      setPlan(opened);
      setSelectedBayPlanId(String(opened.bayPlanId));
      setStability(opened.estabilidade ?? null);
      setRestow(null);
      setSequencing(null);
    }
  }

  async function allocate() {
    const container = unallocated.find((item) => item.codigoContainer === selectedContainerCode);
    const slotId = Number(selectedSlotId);
    if (!canCommand || planLocked || !plan?.id || !container || !Number.isInteger(slotId)) return;
    const response = await run('allocate', () => api.alocarContainerNoPlano(plan.id, {
      codigoContainer: container.codigoContainer,
      slotDestinoId: slotId,
      isoCode: container.isoCode,
      pesoKg: container.pesoKg,
      portoCarga: container.portoCarga,
      portoDescarga: container.portoDescarga,
      classeImo: container.classeImo ?? '',
      reefer: Boolean(container.reefer)
    }), 'Alocação confirmada pelo backend.');
    if (response?.sucesso) {
      setStability(response.estabilidade ?? null);
      setSelectedSlotId('');
      await refreshPlan(plan.id);
    } else if (response) {
      setError(response.mensagem || 'A alocação foi rejeitada pelo backend.');
    }
  }

  async function autoStow() {
    if (!canCommand || planLocked || !plan?.id) return;
    const updated = await run('auto-stow', () => api.autoEstivarPlano(plan.id), 'Autoestivagem concluída pelo backend.');
    if (updated) {
      setPlan(updated);
      setStability(updated.estabilidade ?? null);
    }
  }

  async function loadStability() {
    if (!plan?.id) return;
    const response = await run('stability', () => api.obterEstabilidadePlano(plan.id));
    if (response) setStability(response);
  }

  async function loadRestow() {
    if (!plan?.id) return;
    const response = await run('restow', () => api.obterRestowPlano(plan.id));
    if (response) setRestow(response);
  }

  async function loadSequence() {
    if (!plan?.id) return;
    const response = await run('sequence', () => api.obterSequenciamentoGuindastes(plan.id, cranes));
    if (response) setSequencing(response);
  }

  async function validatePlan() {
    if (!canCommand || planLocked || !plan?.id) return;
    const updated = await run('validate', () => api.validarPlanoVesselPlanner(plan.id), 'Validação concluída e estado persistido pelo backend.');
    if (updated) {
      setPlan(updated);
      setStability(updated.estabilidade ?? null);
    }
  }

  const canReprocess = selectedProcessing && ['REJEITADO', 'QUARENTENA', 'AGUARDANDO_REPROCESSAMENTO'].includes(selectedProcessing.status);

  return <>
    <PageHeader
      eyebrow="Embarque"
      title="Vessel Planner de contêineres"
      description="Importe e acompanhe BAPLIE, selecione a escala e o Bay Plan, abra o plano persistido e execute somente comandos confirmados pelo backend."
      actions={<div className="inline"><label className="compact-field">Janela<select value={days} onChange={(event) => setDays(Number(event.target.value))}><option value="7">7 dias</option><option value="15">15 dias</option><option value="30">30 dias</option><option value="60">60 dias</option></select></label><button type="button" className="secondary" onClick={() => { scales.reload(); bayPlans.reload(); processings.reload(); }}>Atualizar tudo</button></div>}
    />
    <Message type="error" onClose={() => setError('')}>{error || scales.error || bayPlans.error || processings.error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>
    {!canCommand && <Message type="warning">Modo somente leitura. Comandos de importação e alteração exigem perfil ADMIN_PORTO ou PLANEJADOR.</Message>}
    {planLocked && <Message type="warning">O plano está em estado final e não aceita novas alocações ou autoestivagem.</Message>}

    <PlannerContext scale={selectedScale} bayPlan={bayPlan} plan={plan} />

    <Section title="Contexto operacional" description="A escala organiza o trabalho; o Bay Plan e o plano persistido são selecionados explicitamente.">
      <div className="planner-selection-grid">
        <label className="field"><span>Escala</span><select value={selectedScaleKey} onChange={(event) => setSelectedScaleKey(event.target.value)} disabled={scales.loading}><option value="">Selecione</option>{scaleRows.map((scale, index) => <option key={entityKey(scale, index)} value={entityKey(scale, index)}>{scaleLabel(scale)}</option>)}</select></label>
        <label className="field"><span>Bay Plan ativo</span><select value={selectedBayPlanId} onChange={(event) => setSelectedBayPlanId(event.target.value)} disabled={bayPlans.loading}><option value="">Selecione</option>{bayPlanRows.map((item) => <option key={item.id} value={item.id}>#{item.id} · {item.nomeNavio || item.codigoNavio} · {item.codigoViagem} · {item.status}</option>)}</select></label>
        <div className="field"><span>Novo plano</span><button type="button" onClick={createPlan} disabled={!canCommand || !bayPlan?.id || Boolean(busy)}>{busy === 'create-plan' ? 'Criando...' : 'Criar a partir do Bay Plan'}</button></div>
        <form className="inline-form" onSubmit={openPlan}><label className="field"><span>Abrir plano por ID</span><input type="number" min="1" value={planIdToOpen} onChange={(event) => setPlanIdToOpen(event.target.value)} placeholder="Ex.: 42" /></label><button disabled={!planIdToOpen || Boolean(busy)}>{busy === 'open-plan' ? 'Abrindo...' : 'Abrir plano'}</button></form>
      </div>
      <JsonDetails value={selectedScale} title="Detalhes da escala selecionada" />
    </Section>

    <Section title="Recepção e auditoria BAPLIE" description="O identificador de processamento, o status e qualquer rejeição permanecem visíveis até a conclusão.">
      <form className="upload-form" onSubmit={uploadBaplie}><input disabled={!canCommand} type="file" accept=".edi,.txt,.baplie,text/plain" onChange={(event) => setFile(event.target.files?.[0] ?? null)} required /><button disabled={!canCommand || !file || Boolean(busy)}>{busy === 'upload' ? 'Enviando...' : 'Enviar BAPLIE'}</button></form>
      <div className="planner-split">
        <div>
          {processings.loading ? <Loading label="Carregando processamentos..." /> : <DataTable rows={processingRows} columns={[
            { key: 'id', label: 'Processamento' },
            { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
            { key: 'codigoNavio', label: 'Navio' },
            { key: 'codigoViagem', label: 'Viagem' },
            { key: 'bayPlanId', label: 'Bay Plan' },
            { key: 'atualizadoEm', label: 'Atualizado', render: (row) => displayValue(row.atualizadoEm) }
          ]} rowKey="id" onRowClick={setSelectedProcessing} emptyTitle="Nenhum processamento BAPLIE" />}
        </div>
        <aside className="planner-detail-card">
          {selectedProcessing ? <>
            <div className="card-meta"><StatusBadge value={selectedProcessing.status} /><span>Processamento #{selectedProcessing.id}</span></div>
            <h3>{selectedProcessing.codigoNavio || 'Navio ainda não identificado'} · {selectedProcessing.codigoViagem || 'viagem não identificada'}</h3>
            <p>{selectedProcessing.motivoRejeicao || 'Sem motivo de rejeição informado.'}</p>
            <dl><div><dt>Correlation ID</dt><dd>{selectedProcessing.correlationId || '—'}</dd></div><div><dt>Bay Plan</dt><dd>{selectedProcessing.bayPlanId || '—'}</dd></div><div><dt>Tentativa</dt><dd>{selectedProcessing.tentativa ?? '—'}</dd></div></dl>
            {canCommand && canReprocess && <div className="reprocess-box"><label className="field"><span>Motivo do reprocessamento</span><textarea maxLength="500" value={reprocessReason} onChange={(event) => setReprocessReason(event.target.value)} rows="3" /></label><button type="button" onClick={reprocess} disabled={!reprocessReason.trim() || Boolean(busy)}>{busy === 'reprocess' ? 'Solicitando...' : 'Reprocessar'}</button></div>}
          </> : <EmptyState title="Selecione um processamento" description="Clique em uma linha para acompanhar detalhes e rejeições." />}
        </aside>
      </div>
    </Section>

    {bayPlan && <Section title={`Bay Plan #${bayPlan.id}`} description={`${bayPlan.nomeNavio || bayPlan.codigoNavio} · viagem ${bayPlan.codigoViagem} · ${bayPlan.totalContainers ?? 0} contêiner(es)`} actions={<StatusBadge value={bayPlan.status} />}>
      <div className="metrics-grid"><MetricCard label="Contêineres" value={bayPlan.totalContainers} /><MetricCard label="Carregamento" value={bayPlan.totalCarregamento} /><MetricCard label="Descarga" value={bayPlan.totalDescarga} /><MetricCard label="Versão" value={bayPlan.versao} /></div>
      <DataTable rows={bayPlan.containers ?? []} columns={[
        { key: 'codigoContainer', label: 'Contêiner' }, { key: 'tipoOperacao', label: 'Operação', render: (row) => <StatusBadge value={row.tipoOperacao} /> }, { key: 'posicaoBayEdifact', label: 'Posição BAPLIE' }, { key: 'isoCode', label: 'ISO' }, { key: 'pesoKg', label: 'Peso', render: (row) => row.pesoKg ? `${displayValue(row.pesoKg)} kg` : '—' }, { key: 'portoDescarga', label: 'POD' }
      ]} emptyTitle="Bay Plan sem contêineres" />
    </Section>}

    {plan && <>
      <Section title={`Plano #${plan.id}`} description={`${plan.codigoNavio} · viagem ${plan.codigoViagem}`} actions={<div className="actions"><StatusBadge value={plan.status} /><button type="button" className="secondary" onClick={() => run('refresh-plan', () => refreshPlan(plan.id), 'Plano recarregado do backend.')} disabled={Boolean(busy)}>Recarregar</button></div>}>
        <div className="metrics-grid"><MetricCard label="Contêineres" value={plan.totalContainers} /><MetricCard label="Slots ocupados" value={plan.totalSlotsOcupados} /><MetricCard label="Não alocados" value={unallocated.length} /><MetricCard label="Slots livres" value={emptySlots.length} /></div>
        <div className="actions planner-command-bar">
          <button type="button" onClick={autoStow} disabled={!canCommand || planLocked || Boolean(busy) || !unallocated.length}>{busy === 'auto-stow' ? 'Executando...' : 'Autoestivar'}</button>
          <button type="button" className="secondary" onClick={loadStability} disabled={Boolean(busy)}>{busy === 'stability' ? 'Consultando...' : 'Consultar estabilidade'}</button>
          <button type="button" className="secondary" onClick={loadRestow} disabled={Boolean(busy)}>{busy === 'restow' ? 'Consultando...' : 'Analisar restow'}</button>
          <label className="compact-field">Guindastes<select value={cranes} onChange={(event) => setCranes(Number(event.target.value))}><option value="1">1</option><option value="2">2</option><option value="3">3</option><option value="4">4</option></select></label>
          <button type="button" className="secondary" onClick={loadSequence} disabled={Boolean(busy)}>{busy === 'sequence' ? 'Sequenciando...' : 'Sequenciar guindastes'}</button>
          <button type="button" className="warning" onClick={validatePlan} disabled={!canCommand || planLocked || Boolean(busy)}>{busy === 'validate' ? 'Validando...' : 'Validar plano'}</button>
        </div>
      </Section>

      <Section title="Alocação manual" description="Selecione um contêiner não alocado e um slot vazio. Compatibilidade e restrições são validadas pelo backend.">
        <div className="planner-selection-grid">
          <label className="field"><span>Contêiner não alocado</span><select value={selectedContainerCode} onChange={(event) => setSelectedContainerCode(event.target.value)} disabled={!canCommand || planLocked || !unallocated.length}><option value="">Selecione</option>{unallocated.map((container) => <option key={container.codigoContainer} value={container.codigoContainer}>{container.codigoContainer} · {container.isoCode || 'ISO —'} · {container.pesoKg ? `${displayValue(container.pesoKg)} kg` : 'peso —'}</option>)}</select></label>
          <label className="field"><span>Slot vazio</span><select value={selectedSlotId} onChange={(event) => setSelectedSlotId(event.target.value)} disabled={!canCommand || planLocked || !emptySlots.length}><option value="">Selecione no campo ou no mapa</option>{emptySlots.map((slot) => <option key={slot.id} value={slot.id}>Bay {slot.bay} · row {slot.rowBay} · tier {slot.tier} · {slot.tipoSlot}</option>)}</select></label>
          <div className="field"><span>Persistir alocação</span><button type="button" onClick={allocate} disabled={!canCommand || planLocked || !selectedContainerCode || !selectedSlotId || Boolean(busy)}>{busy === 'allocate' ? 'Alocando...' : 'Alocar no slot'}</button></div>
        </div>
      </Section>

      <Section title="Bays, rows, tiers e restrições" description="Slots ocupados, vazios e restritos são renderizados a partir do DTO persistido do plano.">
        <BaySlotGrid plan={plan} selectedSlotId={selectedSlotId} onSelectSlot={(slot) => { if (canCommand && !planLocked) setSelectedSlotId(String(slot.id)); }} />
      </Section>

      <Section title="Contêineres não alocados" description="Diferença entre os contêineres do Bay Plan selecionado e os slots ocupados do plano persistido.">
        <DataTable rows={unallocated} columns={[
          { key: 'codigoContainer', label: 'Contêiner' }, { key: 'tipoOperacao', label: 'Operação', render: (row) => <StatusBadge value={row.tipoOperacao} /> }, { key: 'isoCode', label: 'ISO' }, { key: 'pesoKg', label: 'Peso', render: (row) => row.pesoKg ? `${displayValue(row.pesoKg)} kg` : '—' }, { key: 'portoCarga', label: 'POL' }, { key: 'portoDescarga', label: 'POD' }
        ]} emptyTitle="Todos os contêineres estão alocados" />
      </Section>

      <AnalysisSection stability={stability} restow={restow} sequencing={sequencing} />
      <JsonDetails value={plan} title="Contrato completo do plano persistido" />
    </>}
  </>;
}
