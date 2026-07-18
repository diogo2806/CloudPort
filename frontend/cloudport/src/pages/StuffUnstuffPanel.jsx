import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, readSession } from '../api.js';
import { DataTable, EmptyState, Message, Section, StatusBadge } from '../components.jsx';
import { generalCargoApi } from '../generalCargoApi.js';

function blankOperation() {
  return {
    tipo: 'STUFF', conteinerId: '', armazemId: '', posicaoOperacao: '', equipeRecurso: '', lacreInicial: ''
  };
}

function blankPlannedItem() {
  return { loteId: '', quantidadePlanejada: '', volumePlanejadoM3: '', pesoPlanejadoKg: '' };
}

function blankExecution() {
  return {
    itemId: '', quantidade: '', volumeM3: '', pesoKg: '', codigoAvaria: '', descricaoAvaria: '', divergencia: ''
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

export function StuffUnstuffPanel({ lotes = [], onChanged }) {
  const [operacoes, setOperacoes] = useState([]);
  const [selectedOperationId, setSelectedOperationId] = useState('');
  const [selectedOperation, setSelectedOperation] = useState(null);
  const [operation, setOperation] = useState(blankOperation);
  const [plannedItem, setPlannedItem] = useState(blankPlannedItem);
  const [plannedItems, setPlannedItems] = useState([]);
  const [execution, setExecution] = useState(blankExecution);
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
      setExecution(blankExecution());
      return;
    }
    try {
      const result = await generalCargoApi.obterOperacaoStuffUnstuff(id);
      setSelectedOperation(result);
      setExecution((current) => ({ ...current, itemId: result?.itens?.[0]?.id || '' }));
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

  async function execute(key, action, message) {
    setBusy(key);
    setError('');
    setSuccess('');
    try {
      const result = await action();
      setSuccess(message);
      await loadOperations();
      if (result?.id) {
        setSelectedOperationId(result.id);
        setSelectedOperation(result);
      } else if (selectedOperationId) {
        await loadSelectedOperation(selectedOperationId);
      }
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
    }), 'Operação de stuff/unstuff criada.');
    if (created) {
      setOperation(blankOperation());
      setPlannedItems([]);
    }
  }

  async function startOperation() {
    await execute('start', () => generalCargoApi.iniciarOperacaoStuffUnstuff(
      selectedOperationId, currentUser()
    ), 'Operação iniciada.');
  }

  async function registerExecution(event) {
    event.preventDefault();
    await execute('execute', () => generalCargoApi.registrarExecucaoStuffUnstuff(selectedOperationId, {
      ...execution,
      usuario: currentUser()
    }), 'Execução parcial registrada.');
    setExecution((current) => ({ ...blankExecution(), itemId: current.itemId }));
  }

  async function concludeOperation(event) {
    event.preventDefault();
    await execute('conclude', () => generalCargoApi.concluirOperacaoStuffUnstuff(selectedOperationId, {
      lacreFinal: finalSeal || null,
      observacao: conclusionNote || null,
      usuario: currentUser()
    }), 'Operação concluída.');
    setFinalSeal('');
    setConclusionNote('');
  }

  async function cancelOperation(event) {
    event.preventDefault();
    await execute('cancel', () => generalCargoApi.cancelarOperacaoStuffUnstuff(selectedOperationId, {
      motivo: cancelReason,
      usuario: currentUser()
    }), 'Operação cancelada e saldos compensados.');
    setCancelReason('');
  }

  const terminal = selectedOperation?.status === 'CONCLUIDA' || selectedOperation?.status === 'CANCELADA';

  return <Section
    title="Operações de stuff e unstuff"
    description="Crie a ordem por contêiner, planeje múltiplos cargo lots, acompanhe o realizado e encerre com trilha operacional."
  >
    <Message type="error">{error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    <form className="planner-selection-grid" onSubmit={createOperation}>
      <label className="field"><span>Operação</span><select value={operation.tipo} onChange={(event) => setOperation((current) => ({ ...current, tipo: event.target.value }))}><option>STUFF</option><option>UNSTUFF</option></select></label>
      <label className="field"><span>Contêiner</span><input required maxLength="80" value={operation.conteinerId} onChange={(event) => setOperation((current) => ({ ...current, conteinerId: event.target.value }))} /></label>
      <label className="field"><span>Armazém</span><input maxLength="80" value={operation.armazemId} onChange={(event) => setOperation((current) => ({ ...current, armazemId: event.target.value }))} /></label>
      <label className="field"><span>Local da operação</span><input maxLength="120" value={operation.posicaoOperacao} onChange={(event) => setOperation((current) => ({ ...current, posicaoOperacao: event.target.value }))} /></label>
      <label className="field"><span>Equipe ou recurso</span><input maxLength="120" value={operation.equipeRecurso} onChange={(event) => setOperation((current) => ({ ...current, equipeRecurso: event.target.value }))} /></label>
      <label className="field"><span>Lacre inicial</span><input maxLength="80" value={operation.lacreInicial} onChange={(event) => setOperation((current) => ({ ...current, lacreInicial: event.target.value }))} /></label>
      <div className="field"><span>Ação</span><button type="submit" disabled={busy === 'create' || !plannedItems.length}>{busy === 'create' ? 'Criando...' : 'Criar operação'}</button></div>
    </form>

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
        { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="secondary small" onClick={() => setSelectedOperationId(row.id)}>Operar</button> }
      ]}
      emptyTitle="Nenhuma operação de stuff ou unstuff"
    />

    {selectedOperation && <>
      <div className="planner-selection-grid">
        <div className="field"><span>Operação selecionada</span><strong>{selectedOperation.tipo} | {selectedOperation.conteinerId}</strong></div>
        <div className="field"><span>Status</span><StatusBadge value={selectedOperation.status} /></div>
        <div className="field"><span>Lacre inicial</span><strong>{selectedOperation.lacreInicial || '—'}</strong></div>
        <div className="field"><span>Lacre final</span><strong>{selectedOperation.lacreFinal || '—'}</strong></div>
        <div className="field"><span>Ação</span><button type="button" disabled={terminal || selectedOperation.status !== 'PLANEJADA' || busy === 'start'} onClick={startOperation}>{busy === 'start' ? 'Iniciando...' : 'Iniciar'}</button></div>
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
        { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="secondary small" disabled={terminal} onClick={() => setExecution((current) => ({ ...current, itemId: row.id }))}>Apontar</button> }
      ]} />

      <form className="planner-selection-grid" onSubmit={registerExecution}>
        <label className="field"><span>Item</span><select required disabled={terminal} value={execution.itemId} onChange={(event) => setExecution((current) => ({ ...current, itemId: event.target.value }))}>{(selectedOperation.itens ?? []).map((item) => <option key={item.id} value={item.id}>{item.loteCodigo}</option>)}</select></label>
        <label className="field"><span>Quantidade realizada</span><input required disabled={terminal} type="number" min="0.001" step="0.001" value={execution.quantidade} onChange={(event) => setExecution((current) => ({ ...current, quantidade: event.target.value }))} /></label>
        <label className="field"><span>Volume realizado m³</span><input required disabled={terminal} type="number" min="0" step="0.001" value={execution.volumeM3} onChange={(event) => setExecution((current) => ({ ...current, volumeM3: event.target.value }))} /></label>
        <label className="field"><span>Peso realizado kg</span><input required disabled={terminal} type="number" min="0" step="0.001" value={execution.pesoKg} onChange={(event) => setExecution((current) => ({ ...current, pesoKg: event.target.value }))} /></label>
        <label className="field"><span>Código da avaria</span><input disabled={terminal} maxLength="80" value={execution.codigoAvaria} onChange={(event) => setExecution((current) => ({ ...current, codigoAvaria: event.target.value }))} /></label>
        <label className="field"><span>Descrição da avaria</span><input disabled={terminal} maxLength="1000" value={execution.descricaoAvaria} onChange={(event) => setExecution((current) => ({ ...current, descricaoAvaria: event.target.value }))} /></label>
        <label className="field"><span>Divergência</span><input disabled={terminal} maxLength="1000" value={execution.divergencia} onChange={(event) => setExecution((current) => ({ ...current, divergencia: event.target.value }))} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={terminal || !execution.itemId || busy === 'execute'}>{busy === 'execute' ? 'Registrando...' : 'Registrar execução'}</button></div>
      </form>

      <form className="planner-selection-grid" onSubmit={concludeOperation}>
        <label className="field"><span>Lacre final</span><input disabled={terminal} maxLength="80" value={finalSeal} onChange={(event) => setFinalSeal(event.target.value)} /></label>
        <label className="field"><span>Observação de conclusão</span><input disabled={terminal} maxLength="1000" value={conclusionNote} onChange={(event) => setConclusionNote(event.target.value)} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={terminal || busy === 'conclude'}>{busy === 'conclude' ? 'Concluindo...' : 'Concluir operação'}</button></div>
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
