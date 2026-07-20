import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, readSession } from '../api.js';
import { DataTable, EmptyState, Message, Section, StatusBadge } from '../components.jsx';
import { generalCargoApi } from '../generalCargoApi.js';

function blankWeighing() {
  return {
    metodoPesagem: 'METODO_1',
    taraKg: '',
    pesoBrutoKg: '',
    vgmKg: '',
    capacidadeMaximaKg: '',
    equipamentoPesagem: '',
    responsavelPesagem: '',
    observacao: ''
  };
}

function currentUser() {
  const session = readSession();
  return session?.nome || session?.email || 'operador';
}

function dateTime(value) {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleString('pt-BR');
}

function formatKg(value) {
  if (value === null || value === undefined || value === '') return '—';
  const parsed = Number(value);
  return Number.isFinite(parsed) ? `${parsed.toLocaleString('pt-BR', { maximumFractionDigits: 3 })} kg` : `${value} kg`;
}

function formFromResponse(response) {
  if (!response) return blankWeighing();
  return {
    metodoPesagem: response.metodoPesagem || 'METODO_1',
    taraKg: response.taraKg ?? '',
    pesoBrutoKg: response.pesoBrutoKg ?? '',
    vgmKg: response.vgmKg ?? '',
    capacidadeMaximaKg: response.capacidadeMaximaKg ?? '',
    equipamentoPesagem: response.equipamentoPesagem || '',
    responsavelPesagem: response.responsavelPesagem || '',
    observacao: ''
  };
}

export function StuffUnstuffWeighingPanel({ onChanged }) {
  const [operations, setOperations] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [selectedOperation, setSelectedOperation] = useState(null);
  const [weighing, setWeighing] = useState(null);
  const [form, setForm] = useState(blankWeighing);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadOperations = useCallback(async () => {
    try {
      const result = await generalCargoApi.listarOperacoesStuffUnstuff();
      const stuffOperations = (Array.isArray(result) ? result : []).filter((operation) => operation.tipo === 'STUFF');
      setOperations(stuffOperations);
    } catch (reason) {
      setError(formatError(reason));
    }
  }, []);

  const loadSelected = useCallback(async (operationId) => {
    if (!operationId) {
      setSelectedOperation(null);
      setWeighing(null);
      setForm(blankWeighing());
      return;
    }
    setError('');
    try {
      const [operationResult, weighingResult] = await Promise.all([
        generalCargoApi.obterOperacaoStuffUnstuff(operationId),
        generalCargoApi.obterPesagemStuffing(operationId)
      ]);
      setSelectedOperation(operationResult);
      setWeighing(weighingResult);
      setForm(formFromResponse(weighingResult));
    } catch (reason) {
      setError(formatError(reason));
    }
  }, []);

  useEffect(() => { loadOperations(); }, [loadOperations]);
  useEffect(() => { loadSelected(selectedId); }, [loadSelected, selectedId]);

  const executionComplete = useMemo(() => (
    Boolean(selectedOperation?.itens?.length)
      && selectedOperation.itens.every((item) => Number(item.quantidadeRealizada) >= Number(item.quantidadePlanejada))
  ), [selectedOperation]);

  const terminal = ['CONCLUIDA', 'CANCELADA'].includes(selectedOperation?.status);
  const canConfirm = Boolean(selectedOperation) && executionComplete && !terminal;

  async function submit(event) {
    event.preventDefault();
    if (!selectedId || !canConfirm || busy) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      const response = await generalCargoApi.confirmarPesagemStuffing(selectedId, {
        ...form,
        usuario: currentUser()
      });
      setWeighing(response);
      setForm(formFromResponse(response));
      setSuccess(response.liberadoParaConclusao
        ? 'Pesagem e VGM confirmados. O stuffing está liberado por peso para conclusão.'
        : 'Pesagem registrada, mas o stuffing permanece bloqueado por excesso de peso.');
      await loadOperations();
      await loadSelected(selectedId);
      if (onChanged) await onChanged();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  return <Section
    title="Pesagem e VGM do stuffing"
    description="Confirme tara, peso bruto, método, equipamento, responsável e capacidade antes de concluir e liberar o contêiner."
  >
    <Message type="error">{error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    <DataTable
      gridId="stuffing-weighing-operations"
      exportFileName="pesagens-vgm-stuffing"
      rows={operations}
      rowKey="id"
      columns={[
        { key: 'conteinerId', label: 'Contêiner' },
        { key: 'status', label: 'Operação', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'posicaoOperacao', label: 'Local' },
        { key: 'equipeRecurso', label: 'Equipe/recurso' },
        { key: 'criadoEm', label: 'Criada em', render: (row) => dateTime(row.criadoEm) },
        { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="secondary small" onClick={() => setSelectedId(row.id)}>Abrir pesagem</button> }
      ]}
      emptyTitle="Nenhuma operação de stuffing encontrada"
    />

    {!selectedOperation && <EmptyState title="Selecione um stuffing para conferir a pesagem" />}

    {selectedOperation && <>
      <div className="planner-selection-grid">
        <div className="field"><span>Contêiner</span><strong>{selectedOperation.conteinerId}</strong></div>
        <div className="field"><span>Estado da operação</span><StatusBadge value={selectedOperation.status} /></div>
        <div className="field"><span>Estado da pesagem</span><StatusBadge value={weighing?.statusPesagemVgm || 'PENDENTE'} /></div>
        <div className="field"><span>Liberação por peso</span><strong>{weighing?.liberadoParaConclusao ? 'LIBERADO' : 'BLOQUEADO'}</strong></div>
        <div className="field"><span>Confirmada em</span><strong>{dateTime(weighing?.pesagemConfirmadaEm)}</strong></div>
        <div className="field"><span>Motivo do bloqueio</span><strong>{weighing?.motivoBloqueioPeso || '—'}</strong></div>
      </div>

      <DataTable rows={selectedOperation.itens ?? []} rowKey="id" columns={[
        { key: 'loteCodigo', label: 'Cargo lot' },
        { key: 'pesoPlanejadoKg', label: 'Peso planejado', render: (row) => formatKg(row.pesoPlanejadoKg) },
        { key: 'pesoRealizadoKg', label: 'Peso executado', render: (row) => formatKg(row.pesoRealizadoKg) },
        { key: 'quantidadePlanejada', label: 'Qtd. planejada' },
        { key: 'quantidadeRealizada', label: 'Qtd. realizada' }
      ]} emptyTitle="Nenhum item executado" />

      <div className="planner-selection-grid">
        <div className="field"><span>Pré-condição</span><strong>{executionComplete ? 'Execução integral confirmada.' : 'Conclua todos os itens antes de pesar.'}</strong></div>
        <div className="field"><span>Regra do método 1</span><strong>O contêiner carregado é pesado diretamente.</strong></div>
        <div className="field"><span>Regra do método 2</span><strong>VGM = tara + peso executado da carga.</strong></div>
        <div className="field"><span>Tolerância</span><strong>Diferença máxima de 1 kg.</strong></div>
      </div>

      <form className="planner-selection-grid" onSubmit={submit}>
        <label className="field"><span>Método de pesagem</span><select disabled={!canConfirm} value={form.metodoPesagem} onChange={(event) => setForm((current) => ({ ...current, metodoPesagem: event.target.value }))}><option value="METODO_1">Método 1: contêiner carregado</option><option value="METODO_2">Método 2: tara + carga</option></select></label>
        <label className="field"><span>Tara kg</span><input required disabled={!canConfirm} type="number" min="0.001" step="0.001" value={form.taraKg} onChange={(event) => setForm((current) => ({ ...current, taraKg: event.target.value }))} /></label>
        <label className="field"><span>Peso bruto kg</span><input required disabled={!canConfirm} type="number" min="0.001" step="0.001" value={form.pesoBrutoKg} onChange={(event) => setForm((current) => ({ ...current, pesoBrutoKg: event.target.value }))} /></label>
        <label className="field"><span>VGM kg</span><input required disabled={!canConfirm} type="number" min="0.001" step="0.001" value={form.vgmKg} onChange={(event) => setForm((current) => ({ ...current, vgmKg: event.target.value }))} /></label>
        <label className="field"><span>Capacidade máxima kg</span><input required disabled={!canConfirm} type="number" min="0.001" step="0.001" value={form.capacidadeMaximaKg} onChange={(event) => setForm((current) => ({ ...current, capacidadeMaximaKg: event.target.value }))} /></label>
        <label className="field"><span>Equipamento de pesagem</span><input required disabled={!canConfirm} maxLength="120" value={form.equipamentoPesagem} onChange={(event) => setForm((current) => ({ ...current, equipamentoPesagem: event.target.value }))} /></label>
        <label className="field"><span>Responsável pela pesagem</span><input required disabled={!canConfirm} maxLength="120" value={form.responsavelPesagem} onChange={(event) => setForm((current) => ({ ...current, responsavelPesagem: event.target.value }))} /></label>
        <label className="field"><span>Observação</span><input disabled={!canConfirm} maxLength="1000" value={form.observacao} onChange={(event) => setForm((current) => ({ ...current, observacao: event.target.value }))} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={!canConfirm || busy}>{busy ? 'Validando...' : 'Confirmar pesagem e VGM'}</button></div>
      </form>
    </>}
  </Section>;
}
