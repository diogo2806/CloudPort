import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, request, sanitizeText } from '../api.js';
import { DataTable, EmptyState, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';

const EMPTY_CREATE = {
  visitaTremId: '',
  identificadorLocomotiva: '',
  operadoraFerroviaria: '',
  fabricante: '',
  modelo: '',
  numeroSerie: '',
  pesoToneladas: '',
  comprimentoMetros: '',
  larguraMetros: '',
  alturaMetros: '',
  observacoes: ''
};

function numberOrNull(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}

function fieldValue(event) {
  return sanitizeText(event.target.value);
}

export function RailLocomotiveTransfersPage() {
  const [rows, setRows] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [createForm, setCreateForm] = useState(EMPTY_CREATE);
  const [custodyForm, setCustodyForm] = useState({ nomeMaquinista: '', documentoEntrega: '', responsavelTerminal: '', observacoes: '' });
  const [planningForm, setPlanningForm] = useState({ visitaNavioId: '', codigoVisitaNavio: '', modalidadeEmbarque: 'RORO_REBOCADA', deckPlanejado: '', posicaoPlanejada: '', observacoes: '' });
  const [checklist, setChecklist] = useState({ freioEstacionamentoAplicado: false, bateriasIsoladas: false, combustivelProtegido: false, calcosInstalados: false, planoAmarracaoAprovado: false });
  const [boardingForm, setBoardingForm] = useState({ posicaoReal: '', observacoes: '' });

  const selected = useMemo(() => rows.find((row) => row.id === selectedId) ?? null, [rows, selectedId]);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await request('/rail/ferrovia/locomotivas-transferencia');
      setRows(Array.isArray(response) ? response : []);
    } catch (reason) {
      setRows([]);
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  async function execute(path, body, message) {
    if (busy) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      const updated = await request(path, { method: 'POST', body });
      setSuccess(message);
      await load();
      setSelectedId(updated?.id ?? selectedId);
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  async function createTransfer(event) {
    event.preventDefault();
    const visitaTremId = numberOrNull(createForm.visitaTremId);
    const pesoToneladas = numberOrNull(createForm.pesoToneladas);
    const comprimentoMetros = numberOrNull(createForm.comprimentoMetros);
    const larguraMetros = numberOrNull(createForm.larguraMetros);
    const alturaMetros = numberOrNull(createForm.alturaMetros);
    if (!visitaTremId || !pesoToneladas || !comprimentoMetros || !larguraMetros || !alturaMetros) {
      setError('Informe a visita ferroviária e todas as dimensões com valores maiores que zero.');
      return;
    }
    await execute('/rail/ferrovia/locomotivas-transferencia', {
      ...createForm,
      visitaTremId,
      pesoToneladas,
      comprimentoMetros,
      larguraMetros,
      alturaMetros
    }, 'Locomotiva vinculada à visita ferroviária.');
    setCreateForm(EMPTY_CREATE);
  }

  function updateCreate(name, value) {
    setCreateForm((current) => ({ ...current, [name]: value }));
  }

  const columns = [
    { key: 'identificadorLocomotiva', label: 'Locomotiva' },
    { key: 'identificadorTrem', label: 'Trem de entrada' },
    { key: 'operadoraFerroviaria', label: 'Operadora' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
    { key: 'codigoVisitaNavio', label: 'Navio de saída', render: (row) => row.codigoVisitaNavio || '—' },
    { key: 'modalidadeEmbarque', label: 'Modalidade', render: (row) => row.modalidadeEmbarque || '—' },
    { key: 'posicaoReal', label: 'Posição a bordo', render: (row) => row.posicaoReal || row.posicaoPlanejada || '—' },
    { key: 'acao', label: 'Ação', render: (row) => <button className="secondary" onClick={() => setSelectedId(row.id)}>Operar</button> }
  ];

  return <>
    <PageHeader
      eyebrow="Ferrovia → Navio"
      title="Transferência de locomotivas"
      description="Controle a chegada pela malha ferroviária, a entrega de custódia pelo maquinista e o embarque da locomotiva como carga no navio."
      actions={<button className="secondary" onClick={load} disabled={loading || busy}>Atualizar</button>}
    />
    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>

    <Section title="Registrar locomotiva recebida pelo trem">
      <form className="upload-form" onSubmit={createTransfer}>
        <label className="field"><span>ID da visita de trem</span><input type="number" min="1" value={createForm.visitaTremId} onChange={(event) => updateCreate('visitaTremId', event.target.value)} required /></label>
        <label className="field"><span>Identificador da locomotiva</span><input value={createForm.identificadorLocomotiva} onChange={(event) => updateCreate('identificadorLocomotiva', fieldValue(event))} maxLength="60" required /></label>
        <label className="field"><span>Operadora ferroviária</span><input value={createForm.operadoraFerroviaria} onChange={(event) => updateCreate('operadoraFerroviaria', fieldValue(event))} maxLength="80" required /></label>
        <label className="field"><span>Fabricante</span><input value={createForm.fabricante} onChange={(event) => updateCreate('fabricante', fieldValue(event))} maxLength="80" /></label>
        <label className="field"><span>Modelo</span><input value={createForm.modelo} onChange={(event) => updateCreate('modelo', fieldValue(event))} maxLength="80" /></label>
        <label className="field"><span>Número de série</span><input value={createForm.numeroSerie} onChange={(event) => updateCreate('numeroSerie', fieldValue(event))} maxLength="80" /></label>
        <label className="field"><span>Peso (t)</span><input type="number" min="0.001" step="0.001" value={createForm.pesoToneladas} onChange={(event) => updateCreate('pesoToneladas', event.target.value)} required /></label>
        <label className="field"><span>Comprimento (m)</span><input type="number" min="0.001" step="0.001" value={createForm.comprimentoMetros} onChange={(event) => updateCreate('comprimentoMetros', event.target.value)} required /></label>
        <label className="field"><span>Largura (m)</span><input type="number" min="0.001" step="0.001" value={createForm.larguraMetros} onChange={(event) => updateCreate('larguraMetros', event.target.value)} required /></label>
        <label className="field"><span>Altura (m)</span><input type="number" min="0.001" step="0.001" value={createForm.alturaMetros} onChange={(event) => updateCreate('alturaMetros', event.target.value)} required /></label>
        <label className="field"><span>Observações</span><input value={createForm.observacoes} onChange={(event) => updateCreate('observacoes', fieldValue(event))} maxLength="1000" /></label>
        <button disabled={busy}>{busy ? 'Salvando...' : 'Registrar chegada'}</button>
      </form>
    </Section>

    <Section title="Transferências intermodais">
      {loading ? <Loading /> : rows.length
        ? <DataTable rows={rows} columns={columns} rowKey={(row) => row.id} />
        : <EmptyState title="Nenhuma locomotiva em transferência" />}
    </Section>

    {selected && <Section title={`Operar ${selected.identificadorLocomotiva}`}>
      <div className="inline"><strong>Status atual:</strong><StatusBadge value={selected.status} /><button className="secondary" onClick={() => setSelectedId(null)}>Fechar</button></div>

      {selected.status === 'AGUARDANDO_ENTREGA' && <form className="upload-form" onSubmit={(event) => {
        event.preventDefault();
        execute(`/rail/ferrovia/locomotivas-transferencia/${selected.id}/entrega-custodia`, custodyForm, 'Custódia transferida do maquinista para o terminal.');
      }}>
        <label className="field"><span>Maquinista</span><input value={custodyForm.nomeMaquinista} onChange={(event) => setCustodyForm((current) => ({ ...current, nomeMaquinista: fieldValue(event) }))} required /></label>
        <label className="field"><span>Documento/termo de entrega</span><input value={custodyForm.documentoEntrega} onChange={(event) => setCustodyForm((current) => ({ ...current, documentoEntrega: fieldValue(event) }))} required /></label>
        <label className="field"><span>Responsável do terminal</span><input value={custodyForm.responsavelTerminal} onChange={(event) => setCustodyForm((current) => ({ ...current, responsavelTerminal: fieldValue(event) }))} required /></label>
        <label className="field"><span>Observações</span><input value={custodyForm.observacoes} onChange={(event) => setCustodyForm((current) => ({ ...current, observacoes: fieldValue(event) }))} /></label>
        <button disabled={busy}>Assumir custódia</button>
      </form>}

      {selected.status === 'SOB_CUSTODIA_TERMINAL' && <form className="upload-form" onSubmit={(event) => {
        event.preventDefault();
        const visitaNavioId = numberOrNull(planningForm.visitaNavioId);
        if (!visitaNavioId) { setError('Informe um ID válido para a visita de navio.'); return; }
        execute(`/rail/ferrovia/locomotivas-transferencia/${selected.id}/planejamento-embarque`, { ...planningForm, visitaNavioId }, 'Embarque planejado para a visita de navio.');
      }}>
        <label className="field"><span>ID da visita de navio</span><input type="number" min="1" value={planningForm.visitaNavioId} onChange={(event) => setPlanningForm((current) => ({ ...current, visitaNavioId: event.target.value }))} required /></label>
        <label className="field"><span>Código da visita de navio</span><input value={planningForm.codigoVisitaNavio} onChange={(event) => setPlanningForm((current) => ({ ...current, codigoVisitaNavio: fieldValue(event) }))} required /></label>
        <label className="field"><span>Modalidade</span><select value={planningForm.modalidadeEmbarque} onChange={(event) => setPlanningForm((current) => ({ ...current, modalidadeEmbarque: event.target.value }))}><option value="RORO_REBOCADA">Ro-Ro rebocada</option><option value="RORO_AUTOPROPULSADA">Ro-Ro autopropulsada</option><option value="LOLO_ICAMENTO">Lo-Lo por içamento</option></select></label>
        <label className="field"><span>Deck</span><input value={planningForm.deckPlanejado} onChange={(event) => setPlanningForm((current) => ({ ...current, deckPlanejado: fieldValue(event) }))} required /></label>
        <label className="field"><span>Posição planejada</span><input value={planningForm.posicaoPlanejada} onChange={(event) => setPlanningForm((current) => ({ ...current, posicaoPlanejada: fieldValue(event) }))} required /></label>
        <label className="field"><span>Observações</span><input value={planningForm.observacoes} onChange={(event) => setPlanningForm((current) => ({ ...current, observacoes: fieldValue(event) }))} /></label>
        <button disabled={busy}>Planejar embarque</button>
      </form>}

      {selected.status === 'PLANEJADA_PARA_EMBARQUE' && <form className="upload-form" onSubmit={(event) => {
        event.preventDefault();
        execute(`/rail/ferrovia/locomotivas-transferencia/${selected.id}/liberacao-embarque`, checklist, 'Checklist concluído e locomotiva liberada para embarque.');
      }}>
        {Object.entries({ freioEstacionamentoAplicado: 'Freio de estacionamento aplicado', bateriasIsoladas: 'Baterias isoladas', combustivelProtegido: 'Sistema de combustível protegido', calcosInstalados: 'Calços instalados', planoAmarracaoAprovado: 'Plano de amarração aprovado' }).map(([key, label]) => <label className="field" key={key}><span>{label}</span><input type="checkbox" checked={checklist[key]} onChange={(event) => setChecklist((current) => ({ ...current, [key]: event.target.checked }))} /></label>)}
        <button disabled={busy || Object.values(checklist).some((value) => !value)}>Liberar para embarque</button>
      </form>}

      {selected.status === 'PRONTA_PARA_EMBARQUE' && <form className="upload-form" onSubmit={(event) => {
        event.preventDefault();
        execute(`/rail/ferrovia/locomotivas-transferencia/${selected.id}/confirmacao-embarque`, boardingForm, 'Locomotiva confirmada a bordo do navio.');
      }}>
        <label className="field"><span>Posição real a bordo</span><input value={boardingForm.posicaoReal} onChange={(event) => setBoardingForm((current) => ({ ...current, posicaoReal: fieldValue(event) }))} required /></label>
        <label className="field"><span>Observações</span><input value={boardingForm.observacoes} onChange={(event) => setBoardingForm((current) => ({ ...current, observacoes: fieldValue(event) }))} /></label>
        <button disabled={busy}>Confirmar embarque</button>
      </form>}

      {selected.status === 'EMBARCADA' && <p>A locomotiva foi embarcada em {selected.codigoVisitaNavio}, na posição {selected.posicaoReal}.</p>}
    </Section>}
  </>;
}
