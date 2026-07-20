import { useEffect, useMemo, useRef, useState } from 'react';
import { formatError, hasAnyRole, normalizePage } from '../api.js';
import { DataTable, EmptyState, Loading, Message, MetricCard, Section, StatusBadge } from '../components.jsx';
import { navioOperationalApi } from '../navioOperationalApi.js';

const CHECKLIST_FIELDS = [
  ['bercoConfirmado', 'Berço confirmado'],
  ['caladoConfirmado', 'Calado confirmado'],
  ['defensasConfirmadas', 'Defensas confirmadas'],
  ['amarracaoConfirmada', 'Amarração confirmada'],
  ['acessoConfirmado', 'Acesso confirmado'],
  ['recursosConfirmados', 'Recursos confirmados'],
  ['restricoesAvaliadas', 'Restrições avaliadas'],
  ['liberacoesConfirmadas', 'Liberações confirmadas']
];

function localDateTimeNow() {
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
}

function initialReadiness() {
  return {
    berco: '',
    caladoMetros: '',
    bercoConfirmado: false,
    caladoConfirmado: false,
    defensasConfirmadas: false,
    amarracaoConfirmada: false,
    acessoConfirmado: false,
    recursosConfirmados: false,
    restricoesAvaliadas: false,
    liberacoesConfirmadas: false,
    recursos: '',
    restricoes: '',
    liberacoes: '',
    observacoes: ''
  };
}

function initialStoppage() {
  return {
    guindasteId: '1',
    natureza: 'OPERACIONAL',
    inicio: localDateTimeNow(),
    fim: '',
    motivo: '',
    impacto: '',
    turno: '',
    pendencias: '',
    observacao: ''
  };
}

function initialHandover() {
  return {
    guindasteId: '1',
    ocorridoEm: localDateTimeNow(),
    turnoOrigem: '',
    turnoDestino: '',
    responsavelDestino: '',
    pendencias: '',
    observacao: ''
  };
}

function toBackendDateTime(value) {
  return value ? `${value.length === 16 ? `${value}:00` : value}` : null;
}

function formatDateTime(value) {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleString('pt-BR');
}

function ManualOperacaoNavio() {
  return <details className="manual-panel">
    <summary>ⓘ Manual da operação de navio</summary>
    <div className="manual-content">
      <h3>Finalidade da tela</h3>
      <p>Confirmar a prontidão física e documental do berço antes do início da escala e registrar paralisações, trocas de turno e handover por guindaste sem alterar o plano aprovado.</p>
      <h3>Fluxo operacional</h3>
      <ol><li>Selecione uma escala atracada.</li><li>Preencha e confirme o checklist do berço.</li><li>Inicie a operação somente quando o status estiver pronto.</li><li>Informe o plano de estiva para carregar a execução de guindastes.</li><li>Registre paralisações e handovers durante a execução.</li><li>Encerre paralisações operacionais quando o equipamento for liberado.</li></ol>
      <h3>Explicação dos campos</h3>
      <p><strong>Berço e calado:</strong> condição física confirmada. <strong>Defensas, amarração e acesso:</strong> segurança da interface navio-terra. <strong>Recursos, restrições e liberações:</strong> disponibilidade operacional e autorizações. <strong>Impacto:</strong> efeito da paralisação na sequência. <strong>Pendências:</strong> itens entregues ao próximo turno.</p>
      <h3>Permissões necessárias</h3>
      <p>ADMIN_PORTO e PLANEJADOR confirmam prontidão, iniciam a escala e registram eventos. OPERADOR_GATE possui consulta operacional.</p>
      <h3>Estados possíveis</h3>
      <p>Prontidão: PENDENTE ou PRONTO. Escala: ATRACADO ou OPERANDO neste fluxo. Paralisação: ABERTA ou ENCERRADA. Handover: REGISTRADO. Execução: PLANEJADA, EM_EXECUCAO, AGUARDANDO_RECONCILIACAO ou RECONCILIADA.</p>
      <h3>Motivos de bloqueio</h3>
      <p>Checklist ausente ou incompleto, escala fora de ATRACADO, plano sem execução, guindaste inexistente, intervalo sobreposto, paralisação planejada sem fim ou movimento iniciado durante paralisação ativa.</p>
      <h3>Exemplos</h3>
      <p>Uma falha no spreader deve ser registrada como paralisação OPERACIONAL, com impacto e turno. Uma manutenção com janela conhecida deve ser PLANEJADA. No handover, registre responsável do próximo turno e todas as pendências abertas.</p>
      <h3>Atalhos</h3>
      <p><kbd>Alt</kbd> + <kbd>E</kbd> posiciona na seleção da escala. <kbd>Alt</kbd> + <kbd>P</kbd> posiciona no plano de estiva. Os botões Atualizar recarregam somente o contexto correspondente.</p>
      <h3>Processo completo</h3>
      <p><a href="https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/operacao-navio-prontidao-handover.md" target="_blank" rel="noreferrer">Abrir procedimento operacional completo</a>.</p>
    </div>
  </details>;
}

export function NavioOperationalConsole({ session }) {
  const scaleSelectRef = useRef(null);
  const planInputRef = useRef(null);
  const [scales, setScales] = useState([]);
  const [selectedScaleId, setSelectedScaleId] = useState('');
  const [readiness, setReadiness] = useState(null);
  const [readinessHistory, setReadinessHistory] = useState([]);
  const [readinessForm, setReadinessForm] = useState(initialReadiness);
  const [planId, setPlanId] = useState('');
  const [execution, setExecution] = useState(null);
  const [events, setEvents] = useState([]);
  const [stoppageForm, setStoppageForm] = useState(initialStoppage);
  const [handoverForm, setHandoverForm] = useState(initialHandover);
  const [loadingScales, setLoadingScales] = useState(true);
  const [loadingContext, setLoadingContext] = useState(false);
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const canCommand = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR');
  const selectedScale = useMemo(() => scales.find((item) => String(item.id) === String(selectedScaleId)), [scales, selectedScaleId]);

  useEffect(() => {
    const shortcut = (event) => {
      if (!event.altKey) return;
      if (event.key.toLowerCase() === 'e') { event.preventDefault(); scaleSelectRef.current?.focus(); }
      if (event.key.toLowerCase() === 'p') { event.preventDefault(); planInputRef.current?.focus(); }
    };
    window.addEventListener('keydown', shortcut);
    return () => window.removeEventListener('keydown', shortcut);
  }, []);

  async function loadScales() {
    setLoadingScales(true); setError('');
    try {
      const rows = normalizePage(await navioOperationalApi.listarEscalas(30));
      setScales(rows);
      setSelectedScaleId((current) => current || (rows[0]?.id ? String(rows[0].id) : ''));
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível carregar as escalas.'));
    } finally { setLoadingScales(false); }
  }

  async function loadReadiness(scaleId = selectedScaleId) {
    if (!scaleId) { setReadiness(null); setReadinessHistory([]); return; }
    setLoadingContext(true); setError('');
    try {
      const [current, history] = await Promise.all([
        navioOperationalApi.obterProntidaoBerco(scaleId).catch((reason) => reason?.status === 404 ? null : Promise.reject(reason)),
        navioOperationalApi.listarHistoricoProntidaoBerco(scaleId)
      ]);
      setReadiness(current);
      setReadinessHistory(Array.isArray(history) ? history : []);
      if (current) {
        setReadinessForm((form) => ({
          ...form,
          berco: current.berco ?? '',
          caladoMetros: current.caladoMetros ?? '',
          recursos: current.recursos ?? '',
          restricoes: current.restricoes ?? '',
          liberacoes: current.liberacoes ?? '',
          observacoes: current.observacoes ?? ''
        }));
      } else if (selectedScale) {
        setReadinessForm((form) => ({ ...form, berco: selectedScale.bercoPrevisto ?? '' }));
      }
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível carregar a prontidão do berço.'));
    } finally { setLoadingContext(false); }
  }

  async function loadExecution() {
    if (!planId) return;
    setLoadingContext(true); setError(''); setSuccess('');
    try {
      const loadedExecution = await navioOperationalApi.obterExecucaoGuindastes(planId);
      setExecution(loadedExecution);
      setEvents(await navioOperationalApi.listarEventosGuindastes(loadedExecution.id));
    } catch (reason) {
      setExecution(null); setEvents([]);
      setError(formatError(reason, 'Não foi possível carregar a execução de guindastes.'));
    } finally { setLoadingContext(false); }
  }

  useEffect(() => { loadScales(); }, []);
  useEffect(() => { loadReadiness(selectedScaleId); }, [selectedScaleId]);

  async function submitReadiness(event) {
    event.preventDefault();
    if (!selectedScaleId || busy) return;
    setBusy('readiness'); setError(''); setSuccess('');
    try {
      const response = await navioOperationalApi.confirmarProntidaoBerco(selectedScaleId, readinessForm);
      setReadiness(response);
      setSuccess(`Checklist versão ${response.versaoChecklist} registrado. Status: ${response.pronto ? 'PRONTO' : 'PENDENTE'}.`);
      await loadReadiness(selectedScaleId);
    } catch (reason) { setError(formatError(reason, 'Não foi possível confirmar a prontidão.')); }
    finally { setBusy(''); }
  }

  async function startOperation() {
    if (!selectedScaleId || busy) return;
    setBusy('start'); setError(''); setSuccess('');
    try {
      await navioOperationalApi.iniciarOperacaoEscala(selectedScaleId);
      setSuccess('Operação da escala iniciada com a prontidão validada pelo backend.');
      await loadScales();
    } catch (reason) { setError(formatError(reason, 'A operação não pôde ser iniciada.')); }
    finally { setBusy(''); }
  }

  async function submitStoppage(event) {
    event.preventDefault();
    if (!execution?.id || busy) return;
    setBusy('stoppage'); setError(''); setSuccess('');
    try {
      await navioOperationalApi.registrarParalisacaoGuindaste(execution.id, {
        ...stoppageForm,
        inicio: toBackendDateTime(stoppageForm.inicio),
        fim: toBackendDateTime(stoppageForm.fim)
      });
      setStoppageForm(initialStoppage());
      setEvents(await navioOperationalApi.listarEventosGuindastes(execution.id));
      setSuccess('Paralisação registrada na linha do tempo do guindaste.');
    } catch (reason) { setError(formatError(reason, 'Não foi possível registrar a paralisação.')); }
    finally { setBusy(''); }
  }

  async function submitHandover(event) {
    event.preventDefault();
    if (!execution?.id || busy) return;
    setBusy('handover'); setError(''); setSuccess('');
    try {
      await navioOperationalApi.registrarHandoverGuindaste(execution.id, {
        ...handoverForm,
        ocorridoEm: toBackendDateTime(handoverForm.ocorridoEm)
      });
      setHandoverForm(initialHandover());
      setEvents(await navioOperationalApi.listarEventosGuindastes(execution.id));
      setSuccess('Handover registrado com responsável e pendências do próximo turno.');
    } catch (reason) { setError(formatError(reason, 'Não foi possível registrar o handover.')); }
    finally { setBusy(''); }
  }

  async function closeStoppage(eventId) {
    if (!execution?.id || busy) return;
    setBusy(`close-${eventId}`); setError(''); setSuccess('');
    try {
      await navioOperationalApi.encerrarParalisacaoGuindaste(execution.id, eventId, { fim: toBackendDateTime(localDateTimeNow()), observacao: 'Encerrada pelo Control Room.' });
      setEvents(await navioOperationalApi.listarEventosGuindastes(execution.id));
      setSuccess('Paralisação encerrada e equipamento liberado para novos movimentos.');
    } catch (reason) { setError(formatError(reason, 'Não foi possível encerrar a paralisação.')); }
    finally { setBusy(''); }
  }

  const eventColumns = [
    { key: 'inicio', label: 'Início', render: (row) => formatDateTime(row.inicio) },
    { key: 'guindasteId', label: 'Guindaste' },
    { key: 'tipo', label: 'Evento', render: (row) => <StatusBadge value={row.tipo} /> },
    { key: 'natureza', label: 'Natureza', render: (row) => row.natureza || '—' },
    { key: 'estado', label: 'Estado', render: (row) => <StatusBadge value={row.estado} /> },
    { key: 'motivo', label: 'Motivo / pendências', render: (row) => row.motivo || row.pendencias || '—' },
    { key: 'impacto', label: 'Impacto', render: (row) => row.impacto || '—' },
    { key: 'responsavel', label: 'Responsável' },
    { key: 'acao', label: 'Ação', render: (row) => row.tipo === 'PARALISACAO' && row.estado === 'ABERTA' ? <button type="button" className="secondary" disabled={!canCommand || Boolean(busy)} onClick={() => closeStoppage(row.id)}>Encerrar</button> : '—' }
  ];

  return <div className="stacked-sections">
    <ManualOperacaoNavio />
    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>

    <Section title="Prontidão do berço e autorização da escala" actions={<button type="button" className="secondary" onClick={() => loadReadiness()} disabled={!selectedScaleId || loadingContext}>Atualizar prontidão</button>}>
      {loadingScales ? <Loading label="Carregando escalas..." /> : <div className="form-grid">
        <label className="field"><span>Escala</span><select ref={scaleSelectRef} value={selectedScaleId} onChange={(event) => setSelectedScaleId(event.target.value)}><option value="">Selecione</option>{scales.map((item) => <option key={item.id} value={item.id}>{item.nomeNavio} · {item.viagemEntrada} · {item.fase}</option>)}</select></label>
        <div className="field"><span>Fase atual</span><StatusBadge value={selectedScale?.fase ?? 'SEM_ESCALA'} /></div>
        <div className="field"><span>Prontidão atual</span><StatusBadge value={readiness?.pronto ? 'PRONTO' : 'PENDENTE'} /></div>
        <div className="field"><span>Versão do checklist</span><strong>{readiness?.versaoChecklist ?? '—'}</strong></div>
      </div>}
      {readiness?.motivosBloqueio?.length ? <Message type="warning">{readiness.motivosBloqueio.join(' ')}</Message> : null}
      <form onSubmit={submitReadiness}>
        <div className="form-grid">
          <label className="field"><span>Berço confirmado</span><input value={readinessForm.berco} onChange={(event) => setReadinessForm((form) => ({ ...form, berco: event.target.value }))} maxLength={40} required /></label>
          <label className="field"><span>Calado operacional (m)</span><input type="number" min="0" step="0.001" value={readinessForm.caladoMetros} onChange={(event) => setReadinessForm((form) => ({ ...form, caladoMetros: event.target.value }))} required /></label>
        </div>
        <div className="check-grid">{CHECKLIST_FIELDS.map(([key, label]) => <label key={key} className="check-field"><input type="checkbox" checked={readinessForm[key]} onChange={(event) => setReadinessForm((form) => ({ ...form, [key]: event.target.checked }))} /><span>{label}</span></label>)}</div>
        <div className="form-grid">
          <label className="field"><span>Recursos disponíveis</span><textarea value={readinessForm.recursos} onChange={(event) => setReadinessForm((form) => ({ ...form, recursos: event.target.value }))} maxLength={1000} /></label>
          <label className="field"><span>Restrições avaliadas</span><textarea value={readinessForm.restricoes} onChange={(event) => setReadinessForm((form) => ({ ...form, restricoes: event.target.value }))} maxLength={1000} /></label>
          <label className="field"><span>Liberações obtidas</span><textarea value={readinessForm.liberacoes} onChange={(event) => setReadinessForm((form) => ({ ...form, liberacoes: event.target.value }))} maxLength={1000} /></label>
          <label className="field"><span>Observações</span><textarea value={readinessForm.observacoes} onChange={(event) => setReadinessForm((form) => ({ ...form, observacoes: event.target.value }))} maxLength={1000} /></label>
        </div>
        <div className="actions"><button type="submit" disabled={!canCommand || !selectedScaleId || selectedScale?.fase !== 'ATRACADO' || Boolean(busy)}>{busy === 'readiness' ? 'Registrando...' : 'Confirmar checklist'}</button><button type="button" onClick={startOperation} disabled={!canCommand || !selectedScaleId || selectedScale?.fase !== 'ATRACADO' || !readiness?.pronto || Boolean(busy)}>{busy === 'start' ? 'Iniciando...' : 'Iniciar operação'}</button></div>
      </form>
      {readinessHistory.length ? <p className="muted">Histórico preservado: {readinessHistory.length} versão(ões). Última confirmação por {readiness?.responsavel ?? '—'} em {formatDateTime(readiness?.confirmadoEm)}.</p> : null}
    </Section>

    <Section title="Execução e eventos por guindaste" actions={<button type="button" className="secondary" onClick={loadExecution} disabled={!planId || loadingContext}>Atualizar execução</button>}>
      <div className="form-grid"><label className="field"><span>ID do plano de estiva</span><input ref={planInputRef} type="number" min="1" value={planId} onChange={(event) => setPlanId(event.target.value)} placeholder="Ex.: 42" /></label><div className="field"><span>Execução</span><strong>{execution?.id ?? 'Não carregada'}</strong></div><div className="field"><span>Status</span><StatusBadge value={execution?.status ?? 'SEM_EXECUCAO'} /></div><div className="field"><span>Guindastes</span><strong>{execution?.numeroGuindastes ?? '—'}</strong></div></div>
      {execution ? <div className="metrics-grid"><MetricCard label="Movimentos" value={execution.totalMovimentos ?? 0} /><MetricCard label="Concluídos" value={execution.movimentosConcluidos ?? 0} /><MetricCard label="Em execução" value={execution.movimentosEmExecucao ?? 0} /><MetricCard label="Progresso" value={`${execution.percentualConcluido ?? 0}%`} /></div> : <EmptyState title="Informe o plano de estiva" description="A execução é consultada pelo plano aprovado para não confundir a identidade da escala com a visita do Vessel Planner." />}
    </Section>

    {execution && <>
      <Section title="Registrar paralisação">
        <form onSubmit={submitStoppage}><div className="form-grid">
          <label className="field"><span>Guindaste</span><input type="number" min="1" max={execution.numeroGuindastes ?? 8} value={stoppageForm.guindasteId} onChange={(event) => setStoppageForm((form) => ({ ...form, guindasteId: event.target.value }))} required /></label>
          <label className="field"><span>Natureza</span><select value={stoppageForm.natureza} onChange={(event) => setStoppageForm((form) => ({ ...form, natureza: event.target.value, fim: event.target.value === 'OPERACIONAL' ? '' : form.fim }))}><option value="OPERACIONAL">Operacional</option><option value="PLANEJADA">Planejada</option></select></label>
          <label className="field"><span>Início</span><input type="datetime-local" value={stoppageForm.inicio} onChange={(event) => setStoppageForm((form) => ({ ...form, inicio: event.target.value }))} required /></label>
          <label className="field"><span>Fim {stoppageForm.natureza === 'PLANEJADA' ? '(obrigatório)' : '(opcional)'}</span><input type="datetime-local" value={stoppageForm.fim} onChange={(event) => setStoppageForm((form) => ({ ...form, fim: event.target.value }))} required={stoppageForm.natureza === 'PLANEJADA'} /></label>
          <label className="field"><span>Motivo</span><textarea value={stoppageForm.motivo} onChange={(event) => setStoppageForm((form) => ({ ...form, motivo: event.target.value }))} maxLength={1000} required /></label>
          <label className="field"><span>Impacto operacional</span><textarea value={stoppageForm.impacto} onChange={(event) => setStoppageForm((form) => ({ ...form, impacto: event.target.value }))} maxLength={1000} required /></label>
          <label className="field"><span>Turno</span><input value={stoppageForm.turno} onChange={(event) => setStoppageForm((form) => ({ ...form, turno: event.target.value }))} maxLength={120} required /></label>
          <label className="field"><span>Pendências</span><textarea value={stoppageForm.pendencias} onChange={(event) => setStoppageForm((form) => ({ ...form, pendencias: event.target.value }))} maxLength={2000} /></label>
        </div><button type="submit" disabled={!canCommand || Boolean(busy)}>{busy === 'stoppage' ? 'Registrando...' : 'Registrar paralisação'}</button></form>
      </Section>

      <Section title="Registrar handover de turno">
        <form onSubmit={submitHandover}><div className="form-grid">
          <label className="field"><span>Guindaste</span><input type="number" min="1" max={execution.numeroGuindastes ?? 8} value={handoverForm.guindasteId} onChange={(event) => setHandoverForm((form) => ({ ...form, guindasteId: event.target.value }))} required /></label>
          <label className="field"><span>Ocorrido em</span><input type="datetime-local" value={handoverForm.ocorridoEm} onChange={(event) => setHandoverForm((form) => ({ ...form, ocorridoEm: event.target.value }))} required /></label>
          <label className="field"><span>Turno de origem</span><input value={handoverForm.turnoOrigem} onChange={(event) => setHandoverForm((form) => ({ ...form, turnoOrigem: event.target.value }))} maxLength={120} required /></label>
          <label className="field"><span>Turno de destino</span><input value={handoverForm.turnoDestino} onChange={(event) => setHandoverForm((form) => ({ ...form, turnoDestino: event.target.value }))} maxLength={120} required /></label>
          <label className="field"><span>Responsável do próximo turno</span><input value={handoverForm.responsavelDestino} onChange={(event) => setHandoverForm((form) => ({ ...form, responsavelDestino: event.target.value }))} maxLength={120} required /></label>
          <label className="field"><span>Pendências entregues</span><textarea value={handoverForm.pendencias} onChange={(event) => setHandoverForm((form) => ({ ...form, pendencias: event.target.value }))} maxLength={2000} required /></label>
          <label className="field"><span>Observações</span><textarea value={handoverForm.observacao} onChange={(event) => setHandoverForm((form) => ({ ...form, observacao: event.target.value }))} maxLength={1000} /></label>
        </div><button type="submit" disabled={!canCommand || Boolean(busy)}>{busy === 'handover' ? 'Registrando...' : 'Registrar handover'}</button></form>
      </Section>

      <Section title="Linha do tempo operacional">{loadingContext ? <Loading /> : events.length ? <DataTable rows={events} columns={eventColumns} rowKey={(row) => row.id} gridId="navio-eventos-guindastes" exportFileName="eventos-operacionais-guindastes" /> : <EmptyState title="Nenhum evento registrado" description="O plano permanece separado da execução realizada." />}</Section>
    </>}
  </div>;
}
