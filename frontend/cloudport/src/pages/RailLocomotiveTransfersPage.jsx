import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, request, sanitizeText } from '../api.js';
import { DataTable, EmptyState, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';

const EMPTY_CONFIGURATION = {
  visitaTremId: '',
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
  const [visits, setVisits] = useState([]);
  const [rows, setRows] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [configurationForm, setConfigurationForm] = useState(EMPTY_CONFIGURATION);
  const [custodyForm, setCustodyForm] = useState({ nomeMaquinista: '', documentoEntrega: '', responsavelTerminal: '', observacoes: '' });
  const [planningForm, setPlanningForm] = useState({ visitaNavioId: '', codigoVisitaNavio: '', modalidadeEmbarque: 'RORO_REBOCADA', deckPlanejado: '', posicaoPlanejada: '', observacoes: '' });
  const [checklist, setChecklist] = useState({ freioEstacionamentoAplicado: false, bateriasIsoladas: false, combustivelProtegido: false, calcosInstalados: false, planoAmarracaoAprovado: false });
  const [boardingForm, setBoardingForm] = useState({ posicaoReal: '', observacoes: '' });

  const selected = useMemo(() => rows.find((row) => row.id === selectedId) ?? null, [rows, selectedId]);
  const selectedVisit = useMemo(
    () => visits.find((visit) => String(visit.id) === String(configurationForm.visitaTremId)) ?? null,
    [visits, configurationForm.visitaTremId]
  );

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [visitResponse, locomotiveResponse] = await Promise.all([
        request('/rail/ferrovia/visitas', { query: { dias: 30 } }),
        request('/rail/ferrovia/locomotivas')
      ]);
      setVisits(Array.isArray(visitResponse) ? visitResponse : []);
      setRows(Array.isArray(locomotiveResponse) ? locomotiveResponse : []);
    } catch (reason) {
      setVisits([]);
      setRows([]);
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  async function execute(path, body, message) {
    if (busy) return null;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      const updated = await request(path, { method: 'POST', body });
      setSuccess(message);
      await load();
      setSelectedId(updated?.id ?? selectedId);
      return updated;
    } catch (reason) {
      setError(formatError(reason));
      return null;
    } finally {
      setBusy(false);
    }
  }

  async function configureVisit(event) {
    event.preventDefault();
    const visitaTremId = numberOrNull(configurationForm.visitaTremId);
    const pesoToneladas = numberOrNull(configurationForm.pesoToneladas);
    const comprimentoMetros = numberOrNull(configurationForm.comprimentoMetros);
    const larguraMetros = numberOrNull(configurationForm.larguraMetros);
    const alturaMetros = numberOrNull(configurationForm.alturaMetros);
    if (!visitaTremId || !pesoToneladas || !comprimentoMetros || !larguraMetros || !alturaMetros) {
      setError('Selecione a visita e informe todas as dimensões com valores maiores que zero.');
      return;
    }
    const updated = await execute(`/rail/ferrovia/visitas/${visitaTremId}/locomotiva`, {
      fabricante: configurationForm.fabricante,
      modelo: configurationForm.modelo,
      numeroSerie: configurationForm.numeroSerie,
      pesoToneladas,
      comprimentoMetros,
      larguraMetros,
      alturaMetros,
      observacoes: configurationForm.observacoes
    }, 'A visita ferroviária foi configurada como a própria locomotiva.');
    if (updated) setConfigurationForm(EMPTY_CONFIGURATION);
  }

  function updateConfiguration(name, value) {
    setConfigurationForm((current) => ({ ...current, [name]: value }));
  }

  const columns = [
    { key: 'identificadorTrem', label: 'Locomotiva / visita' },
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
      title="Embarque de locomotiva"
      description="A locomotiva é a própria visita ferroviária. O mesmo registro recebe a custódia do terminal e segue até o embarque no navio."
      actions={<button className="secondary" onClick={load} disabled={loading || busy}>Atualizar</button>}
    />
    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>

    <Section
      title="Definir visita como locomotiva isolada"
      description="Não é criado outro trem. A visita selecionada passa a representar diretamente a locomotiva e não pode possuir vagões ou contêineres."
    >
      <form className="upload-form" onSubmit={configureVisit}>
        <label className="field"><span>Visita ferroviária / locomotiva</span><select value={configurationForm.visitaTremId} onChange={(event) => updateConfiguration('visitaTremId', event.target.value)} required><option value="">Selecione</option>{visits.filter((visit) => visit.statusVisita !== 'PARTIU').map((visit) => <option key={visit.id} value={visit.id}>{visit.identificadorTrem} · {visit.operadoraFerroviaria} · {visit.statusVisita}</option>)}</select></label>
        {selectedVisit && <div className="field"><span>Identidade utilizada</span><strong>{selectedVisit.identificadorTrem} é a locomotiva e a visita do trem</strong></div>}
        <label className="field"><span>Fabricante</span><input value={configurationForm.fabricante} onChange={(event) => updateConfiguration('fabricante', fieldValue(event))} maxLength="80" /></label>
        <label className="field"><span>Modelo</span><input value={configurationForm.modelo} onChange={(event) => updateConfiguration('modelo', fieldValue(event))} maxLength="80" /></label>
        <label className="field"><span>Número de série</span><input value={configurationForm.numeroSerie} onChange={(event) => updateConfiguration('numeroSerie', fieldValue(event))} maxLength="80" /></label>
        <label className="field"><span>Peso (t)</span><input type="number" min="0.001" step="0.001" value={configurationForm.pesoToneladas} onChange={(event) => updateConfiguration('pesoToneladas', event.target.value)} required /></label>
        <label className="field"><span>Comprimento (m)</span><input type="number" min="0.001" step="0.001" value={configurationForm.comprimentoMetros} onChange={(event) => updateConfiguration('comprimentoMetros', event.target.value)} required /></label>
        <label className="field"><span>Largura (m)</span><input type="number" min="0.001" step="0.001" value={configurationForm.larguraMetros} onChange={(event) => updateConfiguration('larguraMetros', event.target.value)} required /></label>
        <label className="field"><span>Altura (m)</span><input type="number" min="0.001" step="0.001" value={configurationForm.alturaMetros} onChange={(event) => updateConfiguration('alturaMetros', event.target.value)} required /></label>
        <label className="field"><span>Observações</span><input value={configurationForm.observacoes} onChange={(event) => updateConfiguration('observacoes', fieldValue(event))} maxLength="1000" /></label>
        <button disabled={busy}>{busy ? 'Salvando...' : 'Confirmar visita como locomotiva'}</button>
      </form>
    </Section>

    <Section title="Visitas de locomotivas">
      {loading ? <Loading /> : rows.length
        ? <DataTable rows={rows} columns={columns} rowKey={(row) => row.id} />
        : <EmptyState title="Nenhuma visita configurada como locomotiva" />}
    </Section>

    {selected && <Section title={`Operar visita ${selected.identificadorTrem}`}>
      <div className="inline"><strong>Status atual:</strong><StatusBadge value={selected.status} /><button className="secondary" onClick={() => setSelectedId(null)}>Fechar</button></div>

      {selected.status === 'AGUARDANDO_ENTREGA' && <form className="upload-form" onSubmit={(event) => {
        event.preventDefault();
        execute(`/rail/ferrovia/visitas/${selected.id}/locomotiva/entrega-custodia`, custodyForm, 'Custódia transferida do maquinista para o terminal.');
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
        execute(`/rail/ferrovia/visitas/${selected.id}/locomotiva/planejamento-embarque`, { ...planningForm, visitaNavioId }, 'Embarque planejado para a visita de navio.');
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
        execute(`/rail/ferrovia/visitas/${selected.id}/locomotiva/liberacao-embarque`, checklist, 'Checklist concluído e visita da locomotiva liberada para embarque.');
      }}>
        {Object.entries({ freioEstacionamentoAplicado: 'Freio de estacionamento aplicado', bateriasIsoladas: 'Baterias isoladas', combustivelProtegido: 'Sistema de combustível protegido', calcosInstalados: 'Calços instalados', planoAmarracaoAprovado: 'Plano de amarração aprovado' }).map(([key, label]) => <label className="field" key={key}><span>{label}</span><input type="checkbox" checked={checklist[key]} onChange={(event) => setChecklist((current) => ({ ...current, [key]: event.target.checked }))} /></label>)}
        <button disabled={busy || Object.values(checklist).some((value) => !value)}>Liberar para embarque</button>
      </form>}

      {selected.status === 'PRONTA_PARA_EMBARQUE' && <form className="upload-form" onSubmit={(event) => {
        event.preventDefault();
        execute(`/rail/ferrovia/visitas/${selected.id}/locomotiva/confirmacao-embarque`, boardingForm, 'Visita da locomotiva confirmada a bordo do navio.');
      }}>
        <label className="field"><span>Posição real a bordo</span><input value={boardingForm.posicaoReal} onChange={(event) => setBoardingForm((current) => ({ ...current, posicaoReal: fieldValue(event) }))} required /></label>
        <label className="field"><span>Observações</span><input value={boardingForm.observacoes} onChange={(event) => setBoardingForm((current) => ({ ...current, observacoes: fieldValue(event) }))} /></label>
        <button disabled={busy}>Confirmar embarque</button>
      </form>}

      {selected.status === 'EMBARCADA' && <p>A visita ferroviária {selected.identificadorTrem}, que representa a locomotiva, foi embarcada em {selected.codigoVisitaNavio}, na posição {selected.posicaoReal}.</p>}
    </Section>}
  </>;
}
