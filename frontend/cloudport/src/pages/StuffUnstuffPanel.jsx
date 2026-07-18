import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, readSession } from '../api.js';
import { DataTable, EmptyState, Message, Section, StatusBadge } from '../components.jsx';
import { generalCargoApi } from '../generalCargoApi.js';

function newCommandId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (character) => {
    const random = Math.floor(Math.random() * 16);
    const value = character === 'x' ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}

function blankOperation() {
  return {
    tipo: 'STUFF', conteinerId: '', armazemId: '', posicaoOperacao: '', equipeRecurso: '', lacreInicial: ''
  };
}

function blankPlannedItem() {
  return { loteId: '', quantidadePlanejada: '', volumePlanejadoM3: '', pesoPlanejadoKg: '' };
}

function blankExecution(itemId = '') {
  return {
    commandId: newCommandId(), itemId, quantidade: '', volumeM3: '', pesoKg: '',
    codigoAvaria: '', descricaoAvaria: '', divergencia: ''
  };
}

function dateTime(value) {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleString('pt-BR');
}

function currentUser() {
  const session = readSession();
  return session?.nome || session?.email || 'operador';
}

function decimal(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function difference(planned, performed) {
  return Math.max(0, decimal(planned) - decimal(performed)).toFixed(3);
}

function reconciliationStatus(planned, performed) {
  const plannedValue = decimal(planned);
  const performedValue = decimal(performed);
  if (performedValue === 0) return 'PENDENTE';
  if (performedValue < plannedValue) return 'PARCIAL';
  if (performedValue === plannedValue) return 'CONCILIADO';
  return 'EXCEDIDO';
}

function operationItemsAsPlan(items = []) {
  return items.map((item) => ({
    loteId: item.loteId,
    loteCodigo: item.loteCodigo,
    quantidadePlanejada: item.quantidadePlanejada,
    volumePlanejadoM3: item.volumePlanejadoM3,
    pesoPlanejadoKg: item.pesoPlanejadoKg
  }));
}

export function StuffUnstuffPanel({ lotes = [], conteineres = [], onChanged }) {
  const [operacoes, setOperacoes] = useState([]);
  const [selectedOperationId, setSelectedOperationId] = useState('');
  const [selectedOperation, setSelectedOperation] = useState(null);
  const [planVersions, setPlanVersions] = useState([]);
  const [selectedPlanVersion, setSelectedPlanVersion] = useState(null);
  const [operation, setOperation] = useState(blankOperation);
  const [plannedItem, setPlannedItem] = useState(blankPlannedItem);
  const [plannedItems, setPlannedItems] = useState([]);
  const [versionItem, setVersionItem] = useState(blankPlannedItem);
  const [versionItems, setVersionItems] = useState([]);
  const [versionReason, setVersionReason] = useState('');
  const [releaseReason, setReleaseReason] = useState('');
  const [execution, setExecution] = useState(() => blankExecution());
  const [finalSeal, setFinalSeal] = useState('');
  const [conclusionNote, setConclusionNote] = useState('');
  const [cancelReason, setCancelReason] = useState('');
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadOperations = useCallback(async () => {
    try {
      const result = await generalCargoApi.listarOperacoesStuffUnstuff();
      setOperacoes(Array.isArray(result) ? result : []);
    } catch (reason) {
      setError(formatError(reason));
    }
  }, []);

  const loadSelectedOperation = useCallback(async (id) => {
    if (!id) {
      setSelectedOperation(null);
      setPlanVersions([]);
      setSelectedPlanVersion(null);
      setVersionItems([]);
      setExecution(blankExecution());
      return;
    }
    try {
      const [operationResult, plansResult] = await Promise.all([
        generalCargoApi.obterOperacaoStuffUnstuff(id),
        generalCargoApi.listarPlanosStuffUnstuff(id)
      ]);
      const plans = Array.isArray(plansResult) ? plansResult : [];
      setSelectedOperation(operationResult);
      setPlanVersions(plans);
      setSelectedPlanVersion(plans[0] || null);
      setVersionItems(operationItemsAsPlan(operationResult?.itens));
      setExecution(blankExecution(operationResult?.itens?.[0]?.id || ''));
    } catch (reason) {
      setError(formatError(reason));
    }
  }, []);

  useEffect(() => { loadOperations(); }, [loadOperations]);
  useEffect(() => { loadSelectedOperation(selectedOperationId); }, [loadSelectedOperation, selectedOperationId]);

  const availableLots = useMemo(
    () => lotes.filter((lot) => !plannedItems.some((item) => item.loteId === lot.id)),
    [lotes, plannedItems]
  );

  const availableVersionLots = useMemo(
    () => lotes.filter((lot) => !versionItems.some((item) => item.loteId === lot.id)),
    [lotes, versionItems]
  );

  const latestPlan = planVersions[0] || null;
  const terminal = selectedOperation?.status === 'CONCLUIDA' || selectedOperation?.status === 'CANCELADA';
  const planReleased = latestPlan?.status === 'LIBERADO';
  const canVersion = selectedOperation?.status === 'PLANEJADA' && !terminal;
  const canRelease = canVersion && latestPlan?.status === 'RASCUNHO';
  const canStart = canVersion && planReleased;
  const canExecute = planReleased && ['EM_EXECUCAO', 'PARCIAL'].includes(selectedOperation?.status);

  const reconciliationRows = useMemo(() => {
    if (!selectedPlanVersion) return [];
    return (selectedPlanVersion.itens ?? []).map((planItem) => {
      const performed = selectedOperation?.itens?.find((item) => item.loteId === planItem.loteId);
      return {
        loteId: planItem.loteId,
        loteCodigo: planItem.loteCodigo,
        quantidadePlanejada: planItem.quantidadePlanejada,
        quantidadeRealizada: performed?.quantidadeRealizada ?? 0,
        quantidadePendente: difference(planItem.quantidadePlanejada, performed?.quantidadeRealizada),
        volumePlanejadoM3: planItem.volumePlanejadoM3,
        volumeRealizadoM3: performed?.volumeRealizadoM3 ?? 0,
        volumePendenteM3: difference(planItem.volumePlanejadoM3, performed?.volumeRealizadoM3),
        pesoPlanejadoKg: planItem.pesoPlanejadoKg,
        pesoRealizadoKg: performed?.pesoRealizadoKg ?? 0,
        pesoPendenteKg: difference(planItem.pesoPlanejadoKg, performed?.pesoRealizadoKg),
        status: reconciliationStatus(planItem.quantidadePlanejada, performed?.quantidadeRealizada)
      };
    });
  }, [selectedOperation, selectedPlanVersion]);

  async function execute(key, action, message, operationId = selectedOperationId) {
    setBusy(key);
    setError('');
    setSuccess('');
    try {
      const result = await action();
      setSuccess(message);
      let refreshId = operationId;
      if (result?.id && result?.tipo && result?.conteinerId) {
        refreshId = result.id;
        setSelectedOperationId(result.id);
      }
      await loadOperations();
      if (refreshId) await loadSelectedOperation(refreshId);
      if (onChanged) await onChanged();
      return result;
    } catch (reason) {
      setError(formatError(reason));
      return null;
    } finally {
      setBusy('');
    }
  }

  function addPlannedItem(event) {
    event.preventDefault();
    if (!plannedItem.loteId) return;
    const lot = lotes.find((candidate) => candidate.id === plannedItem.loteId);
    setPlannedItems((current) => [...current, { ...plannedItem, loteCodigo: lot?.codigo || plannedItem.loteId }]);
    setPlannedItem(blankPlannedItem());
  }

  function addVersionItem(event) {
    event.preventDefault();
    if (!versionItem.loteId) return;
    const lot = lotes.find((candidate) => candidate.id === versionItem.loteId);
    setVersionItems((current) => [...current, { ...versionItem, loteCodigo: lot?.codigo || versionItem.loteId }]);
    setVersionItem(blankPlannedItem());
  }

  function updateVersionItem(loteId, field, value) {
    setVersionItems((current) => current.map((item) => (
      item.loteId === loteId ? { ...item, [field]: value } : item
    )));
  }

  async function createOperation(event) {
    event.preventDefault();
    if (!plannedItems.length) {
      setError('Adicione ao menos um cargo lot ao planejamento.');
      return;
    }
    const created = await execute('create', () => generalCargoApi.criarOperacaoStuffUnstuff({
      ...operation,
      usuario: currentUser(),
      itens: plannedItems.map(({ loteCodigo, ...item }) => item)
    }), 'Operação criada com a versão inicial do plano em rascunho.', '');
    if (created) {
      setOperation(blankOperation());
      setPlannedItems([]);
    }
  }

  async function createPlanVersion(event) {
    event.preventDefault();
    if (!versionItems.length) {
      setError('A nova versão deve conter ao menos um cargo lot.');
      return;
    }
    const result = await execute('version', () => generalCargoApi.criarVersaoPlanoStuffUnstuff(
      selectedOperationId,
      {
        usuario: currentUser(),
        motivo: versionReason,
        itens: versionItems.map(({ loteCodigo, ...item }) => item)
      }
    ), 'Nova versão imutável criada em rascunho.');
    if (result) setVersionReason('');
  }

  async function releasePlan(event) {
    event.preventDefault();
    if (!latestPlan) return;
    const result = await execute('release', () => generalCargoApi.liberarPlanoStuffUnstuff(
      selectedOperationId,
      {
        versao: latestPlan.versao,
        usuario: currentUser(),
        motivo: releaseReason
      }
    ), 'Plano validado por capacidade e liberado para execução.');
    if (result) setReleaseReason('');
  }

  async function startOperation() {
    await execute('start', () => generalCargoApi.iniciarOperacaoStuffUnstuff(
      selectedOperationId, currentUser()
    ), 'Operação iniciada com o plano liberado.');
  }

  async function registerExecution(event) {
    event.preventDefault();
    const result = await execute('execute', () => generalCargoApi.registrarExecucaoStuffUnstuff(selectedOperationId, {
      ...execution,
      usuario: currentUser()
    }), 'Execução parcial registrada e conciliada por item.');
    if (result) setExecution(blankExecution(execution.itemId));
  }

  async function concludeOperation(event) {
    event.preventDefault();
    await execute('conclude', () => generalCargoApi.concluirOperacaoStuffUnstuff(selectedOperationId, {
      lacreFinal: finalSeal || null,
      observacao: conclusionNote || null,
      usuario: currentUser()
    }), 'Operação concluída e contêiner liberado.');
    setFinalSeal('');
    setConclusionNote('');
  }

  async function cancelOperation(event) {
    event.preventDefault();
    await execute('cancel', () => generalCargoApi.cancelarOperacaoStuffUnstuff(selectedOperationId, {
      motivo: cancelReason,
      usuario: currentUser()
    }), 'Operação cancelada, saldos compensados e contêiner liberado.');
    setCancelReason('');
  }

  return <Section
    title="Operações de stuff e unstuff"
    description="Crie versões imutáveis do plano, libere a versão válida e concilie a execução física por cargo lot."
  >
    <Message type="error">{error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    <form className="planner-selection-grid" onSubmit={createOperation}>
      <label className="field"><span>Operação</span><select value={operation.tipo} onChange={(event) => setOperation((current) => ({ ...current, tipo: event.target.value }))}><option>STUFF</option><option>UNSTUFF</option></select></label>
      <label className="field"><span>Contêiner canônico</span><select required value={operation.conteinerId} onChange={(event) => setOperation((current) => ({ ...current, conteinerId: event.target.value }))}><option value="">Selecione</option>{conteineres.map((conteiner) => <option key={conteiner.unidadeId} value={conteiner.identificacao}>{conteiner.identificacao} | {conteiner.estado} | {conteiner.posicaoAtual || 'sem posição'}</option>)}</select></label>
      <label className="field"><span>Armazém</span><input maxLength="80" value={operation.armazemId} onChange={(event) => setOperation((current) => ({ ...current, armazemId: event.target.value }))} /></label>
      <label className="field"><span>Local da operação</span><input maxLength="120" value={operation.posicaoOperacao} onChange={(event) => setOperation((current) => ({ ...current, posicaoOperacao: event.target.value }))} /></label>
      <label className="field"><span>Equipe ou recurso</span><input maxLength="120" value={operation.equipeRecurso} onChange={(event) => setOperation((current) => ({ ...current, equipeRecurso: event.target.value }))} /></label>
      <label className="field"><span>Lacre inicial</span><input maxLength="80" value={operation.lacreInicial} onChange={(event) => setOperation((current) => ({ ...current, lacreInicial: event.target.value }))} /></label>
      <div className="field"><span>Ação</span><button type="submit" disabled={busy === 'create' || !plannedItems.length || !operation.conteinerId}>{busy === 'create' ? 'Criando...' : 'Criar operação'}</button></div>
    </form>

    {!conteineres.length && <EmptyState title="Nenhum contêiner elegível no inventário" />}

    <form className="planner-selection-grid" onSubmit={addPlannedItem}>
      <label className="field"><span>Cargo lot</span><select required value={plannedItem.loteId} onChange={(event) => setPlannedItem((current) => ({ ...current, loteId: event.target.value }))}><option value="">Selecione</option>{availableLots.map((lot) => <option key={lot.id} value={lot.id}>{lot.codigo} | saldo {lot.quantidadeSaldo}</option>)}</select></label>
      <label className="field"><span>Quantidade planejada</span><input required type="number" min="0.001" step="0.001" value={plannedItem.quantidadePlanejada} onChange={(event) => setPlannedItem((current) => ({ ...current, quantidadePlanejada: event.target.value }))} /></label>
      <label className="field"><span>Volume planejado m³</span><input required type="number" min="0" step="0.001" value={plannedItem.volumePlanejadoM3} onChange={(event) => setPlannedItem((current) => ({ ...current, volumePlanejadoM3: event.target.value }))} /></label>
      <label className="field"><span>Peso planejado kg</span><input required type="number" min="0" step="0.001" value={plannedItem.pesoPlanejadoKg} onChange={(event) => setPlannedItem((current) => ({ ...current, pesoPlanejadoKg: event.target.value }))} /></label>
      <div className="field"><span>Ação</span><button type="submit" className="secondary">Adicionar ao plano</button></div>
    </form>

    {plannedItems.length ? <DataTable rows={plannedItems} rowKey="loteId" columns={[
      { key: 'loteCodigo', label: 'Cargo lot' },
      { key: 'quantidadePlanejada', label: 'Quantidade' },
      { key: 'volumePlanejadoM3', label: 'Volume m³' },
      { key: 'pesoPlanejadoKg', label: 'Peso kg' },
      { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="danger small" onClick={() => setPlannedItems((current) => current.filter((item) => item.loteId !== row.loteId))}>Remover</button> }
    ]} /> : <EmptyState title="Nenhum cargo lot no planejamento" />}

    <DataTable
      gridId="stuff-unstuff-operations"
      exportFileName="operacoes-stuff-unstuff"
      rows={operacoes}
      rowKey="id"
      columns={[
        { key: 'tipo', label: 'Tipo', render: (row) => <StatusBadge value={row.tipo} /> },
        { key: 'conteinerId', label: 'Contêiner' },
        { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'posicaoOperacao', label: 'Local' },
        { key: 'equipeRecurso', label: 'Equipe/recurso' },
        { key: 'criadoEm', label: 'Criada em', render: (row) => dateTime(row.criadoEm) },
        { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="secondary small" onClick={() => setSelectedOperationId(row.id)}>Planejar e operar</button> }
      ]}
      emptyTitle="Nenhuma operação de stuff ou unstuff"
    />

    {selectedOperation && <>
      <div className="planner-selection-grid">
        <div className="field"><span>Operação selecionada</span><strong>{selectedOperation.tipo} | {selectedOperation.conteinerId}</strong></div>
        <div className="field"><span>Status operacional</span><StatusBadge value={selectedOperation.status} /></div>
        <div className="field"><span>Versão atual</span><strong>{latestPlan ? `v${latestPlan.versao}` : '—'}</strong></div>
        <div className="field"><span>Estado do plano</span>{latestPlan ? <StatusBadge value={latestPlan.status} /> : <strong>Sem plano</strong>}</div>
        <div className="field"><span>Lacre inicial</span><strong>{selectedOperation.lacreInicial || '—'}</strong></div>
        <div className="field"><span>Lacre final</span><strong>{selectedOperation.lacreFinal || '—'}</strong></div>
      </div>

      <DataTable rows={planVersions} rowKey="id" columns={[
        { key: 'versao', label: 'Versão', render: (row) => `v${row.versao}` },
        { key: 'status', label: 'Estado', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'criadoPor', label: 'Criada por' },
        { key: 'criadoEm', label: 'Criada em', render: (row) => dateTime(row.criadoEm) },
        { key: 'liberadoPor', label: 'Liberada por' },
        { key: 'liberadoEm', label: 'Liberada em', render: (row) => dateTime(row.liberadoEm) },
        { key: 'motivo', label: 'Motivo' },
        { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="secondary small" onClick={() => setSelectedPlanVersion(row)}>Comparar</button> }
      ]} emptyTitle="Nenhuma versão do plano encontrada" />

      {canVersion && <>
        <form className="planner-selection-grid" onSubmit={addVersionItem}>
          <label className="field"><span>Adicionar cargo lot à nova versão</span><select value={versionItem.loteId} onChange={(event) => setVersionItem((current) => ({ ...current, loteId: event.target.value }))}><option value="">Selecione</option>{availableVersionLots.map((lot) => <option key={lot.id} value={lot.id}>{lot.codigo} | saldo {lot.quantidadeSaldo}</option>)}</select></label>
          <label className="field"><span>Quantidade planejada</span><input type="number" min="0.001" step="0.001" value={versionItem.quantidadePlanejada} onChange={(event) => setVersionItem((current) => ({ ...current, quantidadePlanejada: event.target.value }))} /></label>
          <label className="field"><span>Volume planejado m³</span><input type="number" min="0" step="0.001" value={versionItem.volumePlanejadoM3} onChange={(event) => setVersionItem((current) => ({ ...current, volumePlanejadoM3: event.target.value }))} /></label>
          <label className="field"><span>Peso planejado kg</span><input type="number" min="0" step="0.001" value={versionItem.pesoPlanejadoKg} onChange={(event) => setVersionItem((current) => ({ ...current, pesoPlanejadoKg: event.target.value }))} /></label>
          <div className="field"><span>Ação</span><button type="submit" className="secondary" disabled={!versionItem.loteId}>Adicionar</button></div>
        </form>

        <DataTable rows={versionItems} rowKey="loteId" columns={[
          { key: 'loteCodigo', label: 'Cargo lot' },
          { key: 'quantidadePlanejada', label: 'Quantidade', render: (row) => <input required type="number" min="0.001" step="0.001" value={row.quantidadePlanejada} onChange={(event) => updateVersionItem(row.loteId, 'quantidadePlanejada', event.target.value)} /> },
          { key: 'volumePlanejadoM3', label: 'Volume m³', render: (row) => <input required type="number" min="0" step="0.001" value={row.volumePlanejadoM3} onChange={(event) => updateVersionItem(row.loteId, 'volumePlanejadoM3', event.target.value)} /> },
          { key: 'pesoPlanejadoKg', label: 'Peso kg', render: (row) => <input required type="number" min="0" step="0.001" value={row.pesoPlanejadoKg} onChange={(event) => updateVersionItem(row.loteId, 'pesoPlanejadoKg', event.target.value)} /> },
          { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="danger small" onClick={() => setVersionItems((current) => current.filter((item) => item.loteId !== row.loteId))}>Remover</button> }
        ]} emptyTitle="A nova versão não possui itens" />

        <form className="planner-selection-grid" onSubmit={createPlanVersion}>
          <label className="field"><span>Motivo da nova versão</span><input required maxLength="1000" value={versionReason} onChange={(event) => setVersionReason(event.target.value)} /></label>
          <div className="field"><span>Ação</span><button type="submit" disabled={!versionItems.length || busy === 'version'}>{busy === 'version' ? 'Versionando...' : 'Criar nova versão'}</button></div>
        </form>

        <form className="planner-selection-grid" onSubmit={releasePlan}>
          <label className="field"><span>Motivo da liberação</span><input required disabled={!canRelease} maxLength="1000" value={releaseReason} onChange={(event) => setReleaseReason(event.target.value)} /></label>
          <div className="field"><span>Ação</span><button type="submit" disabled={!canRelease || busy === 'release'}>{busy === 'release' ? 'Validando...' : `Liberar v${latestPlan?.versao ?? ''}`}</button></div>
          <div className="field"><span>Efeito</span><strong>Apenas valida e libera; não altera estoque.</strong></div>
        </form>
      </>}

      {selectedPlanVersion && <DataTable rows={reconciliationRows} rowKey="loteId" columns={[
        { key: 'loteCodigo', label: `Cargo lot da v${selectedPlanVersion.versao}` },
        { key: 'quantidadePlanejada', label: 'Qtd. planejada' },
        { key: 'quantidadeRealizada', label: 'Qtd. realizada' },
        { key: 'quantidadePendente', label: 'Qtd. pendente' },
        { key: 'volumePlanejadoM3', label: 'Vol. planejado' },
        { key: 'volumeRealizadoM3', label: 'Vol. realizado' },
        { key: 'volumePendenteM3', label: 'Vol. pendente' },
        { key: 'pesoPlanejadoKg', label: 'Peso planejado' },
        { key: 'pesoRealizadoKg', label: 'Peso realizado' },
        { key: 'pesoPendenteKg', label: 'Peso pendente' },
        { key: 'status', label: 'Conciliação', render: (row) => <StatusBadge value={row.status} /> }
      ]} emptyTitle="A versão selecionada não possui itens" />}

      <div className="planner-selection-grid">
        <div className="field"><span>Bloqueio operacional</span><strong>{!latestPlan ? 'Crie uma versão do plano.' : !planReleased ? 'Libere a versão mais recente.' : selectedOperation.status === 'PLANEJADA' ? 'Inicie a operação.' : terminal ? 'Operação encerrada.' : 'Execução permitida.'}</strong></div>
        <div className="field"><span>Ação</span><button type="button" disabled={!canStart || busy === 'start'} onClick={startOperation}>{busy === 'start' ? 'Iniciando...' : 'Iniciar operação'}</button></div>
      </div>

      <DataTable rows={selectedOperation.itens ?? []} rowKey="id" columns={[
        { key: 'loteCodigo', label: 'Cargo lot' },
        { key: 'quantidadePlanejada', label: 'Qtd. planejada' },
        { key: 'quantidadeRealizada', label: 'Qtd. realizada' },
        { key: 'volumePlanejadoM3', label: 'Vol. planejado' },
        { key: 'volumeRealizadoM3', label: 'Vol. realizado' },
        { key: 'pesoPlanejadoKg', label: 'Peso planejado' },
        { key: 'pesoRealizadoKg', label: 'Peso realizado' },
        { key: 'codigoAvaria', label: 'Avaria' },
        { key: 'divergencia', label: 'Divergência' },
        { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="secondary small" disabled={!canExecute} onClick={() => setExecution(blankExecution(row.id))}>Apontar</button> }
      ]} />

      <form className="planner-selection-grid" onSubmit={registerExecution}>
        <label className="field"><span>Command ID</span><input readOnly value={execution.commandId} /></label>
        <label className="field"><span>Item</span><select required disabled={!canExecute} value={execution.itemId} onChange={(event) => setExecution(blankExecution(event.target.value))}>{(selectedOperation.itens ?? []).map((item) => <option key={item.id} value={item.id}>{item.loteCodigo}</option>)}</select></label>
        <label className="field"><span>Quantidade realizada</span><input required disabled={!canExecute} type="number" min="0.001" step="0.001" value={execution.quantidade} onChange={(event) => setExecution((current) => ({ ...current, quantidade: event.target.value }))} /></label>
        <label className="field"><span>Volume realizado m³</span><input required disabled={!canExecute} type="number" min="0" step="0.001" value={execution.volumeM3} onChange={(event) => setExecution((current) => ({ ...current, volumeM3: event.target.value }))} /></label>
        <label className="field"><span>Peso realizado kg</span><input required disabled={!canExecute} type="number" min="0" step="0.001" value={execution.pesoKg} onChange={(event) => setExecution((current) => ({ ...current, pesoKg: event.target.value }))} /></label>
        <label className="field"><span>Código da avaria</span><input disabled={!canExecute} maxLength="80" value={execution.codigoAvaria} onChange={(event) => setExecution((current) => ({ ...current, codigoAvaria: event.target.value }))} /></label>
        <label className="field"><span>Descrição da avaria</span><input disabled={!canExecute} maxLength="1000" value={execution.descricaoAvaria} onChange={(event) => setExecution((current) => ({ ...current, descricaoAvaria: event.target.value }))} /></label>
        <label className="field"><span>Divergência</span><input disabled={!canExecute} maxLength="1000" value={execution.divergencia} onChange={(event) => setExecution((current) => ({ ...current, divergencia: event.target.value }))} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={!canExecute || !execution.itemId || busy === 'execute'}>{busy === 'execute' ? 'Registrando...' : 'Registrar execução'}</button></div>
      </form>

      <form className="planner-selection-grid" onSubmit={concludeOperation}>
        <label className="field"><span>Lacre final</span><input disabled={!canExecute} maxLength="80" value={finalSeal} onChange={(event) => setFinalSeal(event.target.value)} /></label>
        <label className="field"><span>Observação de conclusão</span><input disabled={!canExecute} maxLength="1000" value={conclusionNote} onChange={(event) => setConclusionNote(event.target.value)} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={!canExecute || busy === 'conclude'}>{busy === 'conclude' ? 'Concluindo...' : 'Concluir operação'}</button></div>
      </form>

      <form className="planner-selection-grid" onSubmit={cancelOperation}>
        <label className="field"><span>Motivo do cancelamento</span><input required disabled={terminal} maxLength="1000" value={cancelReason} onChange={(event) => setCancelReason(event.target.value)} /></label>
        <div className="field"><span>Ação</span><button type="submit" className="danger" disabled={terminal || busy === 'cancel'}>{busy === 'cancel' ? 'Cancelando...' : 'Cancelar e compensar'}</button></div>
      </form>

      <DataTable rows={selectedOperation.historico ?? []} rowKey="id" columns={[
        { key: 'ocorridoEm', label: 'Data', render: (row) => dateTime(row.ocorridoEm) },
        { key: 'tipo', label: 'Evento', render: (row) => <StatusBadge value={row.tipo} /> },
        { key: 'usuario', label: 'Usuário' },
        { key: 'descricao', label: 'Descrição' },
        { key: 'correlationId', label: 'Correlação' }
      ]} emptyTitle="Nenhum evento registrado" />
    </>}
  </Section>;
}
