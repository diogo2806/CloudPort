import { useEffect, useMemo, useState } from 'react';
import {
  VISIT_DATE_FIELDS,
  VISIT_MILESTONE_GROUPS,
  validateVisitMilestones
} from './visitMilestones.js';

const MOVIMENTOS = ['EMBARQUE', 'DESCARGA', 'RESTOW'];
const TIPOS_CARGA = ['BOBINA', 'CHAPA', 'TARUGO', 'PLACA', 'PERFIL', 'VERGALHAO', 'OUTROS'];

const statusClass = (value) => `status status-${String(value ?? 'indefinido').toLowerCase().replaceAll('_', '-')}`;
const number = (value, digits = 0) => Number(value ?? 0).toLocaleString('pt-BR', {
  minimumFractionDigits: digits,
  maximumFractionDigits: digits
});

function text(value) {
  return String(value ?? '').normalize('NFKC').replace(/[<>`\\]/g, '').trim();
}

function dateTime(value) {
  if (!value) return 'sem data';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleString('pt-BR');
}

function toDateTimeInput(value) {
  if (!value) return '';
  return String(value).slice(0, 16);
}

function toLocalDateTime(value) {
  if (!value) return null;
  return value.length === 16 ? `${value}:00` : value;
}

function nullableNumber(value) {
  if (value === '' || value === null || value === undefined) return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function visitDraftFrom(visit) {
  if (!visit) return null;
  return {
    ...visit,
    codigoVisita: visit.codigoVisita ?? '',
    viagemEntrada: visit.viagemEntrada ?? '',
    viagemSaida: visit.viagemSaida ?? '',
    linhaOperadora: visit.linhaOperadora ?? '',
    terminalFacility: visit.terminalFacility ?? '',
    bercoPrevisto: visit.bercoPrevisto ?? '',
    bercoAtual: visit.bercoAtual ?? '',
    observacoes: visit.observacoes ?? '',
    ...Object.fromEntries(VISIT_DATE_FIELDS.map((field) => [field, toDateTimeInput(visit[field])]))
  };
}

function itemDraftFrom(item) {
  if (!item) return null;
  return Object.fromEntries(Object.entries(item).map(([key, value]) => [key, value ?? '']));
}

function visitPayload(draft) {
  return {
    ...draft,
    navioId: Number(draft.navioId),
    codigoVisita: text(draft.codigoVisita),
    viagemEntrada: text(draft.viagemEntrada) || null,
    viagemSaida: text(draft.viagemSaida) || null,
    linhaOperadora: text(draft.linhaOperadora) || null,
    terminalFacility: text(draft.terminalFacility) || null,
    bercoPrevisto: text(draft.bercoPrevisto) || null,
    bercoAtual: text(draft.bercoAtual) || null,
    observacoes: text(draft.observacoes) || null,
    ...Object.fromEntries(VISIT_DATE_FIELDS.map((field) => [field, toLocalDateTime(draft[field])]))
  };
}

function itemPayload(draft) {
  return {
    ...draft,
    id: Number(draft.id),
    visitaNavioId: Number(draft.visitaNavioId),
    tipoMovimento: draft.tipoMovimento,
    codigoLote: text(draft.codigoLote),
    produto: text(draft.produto),
    tipoCarga: draft.tipoCarga,
    quantidade: nullableNumber(draft.quantidade),
    pesoUnitarioToneladas: nullableNumber(draft.pesoUnitarioToneladas),
    pesoTotalToneladas: nullableNumber(draft.pesoTotalToneladas),
    alturaCargaMetros: nullableNumber(draft.alturaCargaMetros),
    poraoPlanejado: nullableNumber(draft.poraoPlanejado),
    poraoReal: nullableNumber(draft.poraoReal),
    posicaoPlanejada: text(draft.posicaoPlanejada) || null,
    posicaoReal: text(draft.posicaoReal) || null,
    origemPatio: text(draft.origemPatio) || null,
    destinoPatio: text(draft.destinoPatio) || null,
    conteinerPatioId: nullableNumber(draft.conteinerPatioId),
    cargaPatioId: nullableNumber(draft.cargaPatioId),
    ordemTrabalhoPatioId: nullableNumber(draft.ordemTrabalhoPatioId),
    movimentoPatioId: nullableNumber(draft.movimentoPatioId),
    posicaoPatioPlanejada: text(draft.posicaoPatioPlanejada) || null,
    posicaoPatioReal: text(draft.posicaoPatioReal) || null,
    sequenciaOperacional: nullableNumber(draft.sequenciaOperacional),
    motivoBloqueio: text(draft.motivoBloqueio) || null,
    observacoes: text(draft.observacoes) || null
  };
}

function administrativeReason(label) {
  if (typeof window === 'undefined' || typeof window.prompt !== 'function') return 'Operação administrativa confirmada';
  return text(window.prompt(label, '') ?? '');
}

function confirmed(message) {
  return typeof window === 'undefined'
    || typeof window.confirm !== 'function'
    || window.confirm(message);
}

function isEditingTarget(target) {
  const tag = String(target?.tagName ?? '').toUpperCase();
  return ['INPUT', 'TEXTAREA', 'SELECT'].includes(tag) || Boolean(target?.isContentEditable);
}

export default function VisitPlanEditor({
  visit,
  items = [],
  plan,
  planHistory = [],
  validation,
  busyKey = '',
  onSaveVisit,
  onCancelVisit,
  onSaveItem,
  onCancelItem,
  onValidatePlan,
  onPublishPlan,
  onInvalidatePlan,
  onCancelPlan,
  onCreatePlanVersion
}) {
  const [visitDraft, setVisitDraft] = useState(() => visitDraftFrom(visit));
  const [itemDraft, setItemDraft] = useState(null);
  const [localError, setLocalError] = useState('');
  const [manualOpen, setManualOpen] = useState(false);

  useEffect(() => {
    setVisitDraft(visitDraftFrom(visit));
    setItemDraft(null);
    setLocalError('');
  }, [visit?.id, visit?.atualizadoEm, visit?.fase]);

  useEffect(() => {
    if (itemDraft && !items.some((item) => item.id === itemDraft.id)) setItemDraft(null);
  }, [items, itemDraft]);

  useEffect(() => {
    function handleShortcut(event) {
      const openByF1 = event.key === 'F1';
      const openByQuestion = event.key === '?' && event.shiftKey && !isEditingTarget(event.target);
      if (!openByF1 && !openByQuestion) return;
      event.preventDefault();
      setManualOpen(true);
    }
    window.addEventListener('keydown', handleShortcut);
    return () => window.removeEventListener('keydown', handleShortcut);
  }, []);

  const visitClosed = ['PARTIU', 'CANCELADA'].includes(visit?.fase);
  const visitDateErrors = useMemo(() => validateVisitMilestones(visitDraft ?? {}), [visitDraft]);
  const visitValid = useMemo(() => Number(visitDraft?.navioId) > 0
    && text(visitDraft?.codigoVisita)
    && visitDateErrors.length === 0, [visitDraft, visitDateErrors]);
  const itemValid = useMemo(() => itemDraft
    && MOVIMENTOS.includes(itemDraft.tipoMovimento)
    && TIPOS_CARGA.includes(itemDraft.tipoCarga)
    && text(itemDraft.codigoLote)
    && text(itemDraft.produto)
    && Number(itemDraft.quantidade) > 0
    && Number(itemDraft.pesoTotalToneladas) > 0, [itemDraft]);

  async function saveVisit(event) {
    event.preventDefault();
    if (visitDateErrors.length) {
      setLocalError(visitDateErrors[0]);
      return;
    }
    if (!visitValid) {
      setLocalError('Informe o código da visita e mantenha um navio válido.');
      return;
    }
    setLocalError('');
    await onSaveVisit(visitPayload(visitDraft));
  }

  async function saveItem(event) {
    event.preventDefault();
    if (!itemValid) {
      setLocalError('Preencha movimento, lote, produto, tipo de carga, quantidade e peso total com valores válidos.');
      return;
    }
    setLocalError('');
    const updated = await onSaveItem(itemDraft.id, itemPayload(itemDraft));
    if (updated) setItemDraft(itemDraftFrom(updated));
  }

  function cancelVisit() {
    if (!confirmed(`Cancelar administrativamente a visita ${visit.codigoVisita}?`)) return;
    const reason = administrativeReason('Informe o motivo do cancelamento da visita:');
    if (!reason) return setLocalError('O motivo do cancelamento da visita é obrigatório.');
    setLocalError('');
    onCancelVisit(reason);
  }

  function cancelItem(item) {
    if (!confirmed(`Cancelar o item ${item.codigoLote}?`)) return;
    const reason = administrativeReason(`Informe o motivo do cancelamento do item ${item.codigoLote}:`);
    if (!reason) return setLocalError('O motivo do cancelamento do item é obrigatório.');
    setLocalError('');
    onCancelItem(item.id, reason);
  }

  function publishPlan() {
    if (!plan?.id || plan.status !== 'VALIDADO') return;
    if (!confirmed(`Concluir e publicar definitivamente o plano de estiva v${plan.versao}?`)) return;
    const reason = administrativeReason('Informe o motivo da publicação do plano:');
    if (!reason) return setLocalError('O motivo da publicação é obrigatório.');
    setLocalError('');
    onPublishPlan(plan.id, reason);
  }

  function invalidatePlan() {
    if (!plan?.id || !['VALIDADO', 'CONCLUIDO'].includes(plan.status)) return;
    if (!confirmed(`Invalidar o plano de estiva v${plan.versao}?`)) return;
    const reason = administrativeReason('Informe o motivo da invalidação do plano:');
    if (!reason) return setLocalError('O motivo da invalidação é obrigatório.');
    setLocalError('');
    onInvalidatePlan(plan.id, reason);
  }

  function cancelPlan() {
    if (!plan?.id || plan.status === 'CANCELADO') return;
    if (!confirmed(`Cancelar o plano de estiva v${plan.versao}?`)) return;
    const reason = administrativeReason('Informe o motivo do cancelamento do plano:');
    if (!reason) return setLocalError('O motivo do cancelamento do plano é obrigatório.');
    setLocalError('');
    onCancelPlan(plan.id, reason);
  }

  if (!visit || !visitDraft) return null;

  return <section className="panel visit-plan-editor">
    <div className="section-head">
      <div><span className="eyebrow">Visita e estiva</span><h2>Edição e administração operacional</h2></div>
      <div className="actions">
        <span>{items.length} itens</span>
        <button type="button" className="small secondary" aria-label="Abrir manual da visita e estiva" aria-expanded={manualOpen} onClick={() => setManualOpen((current) => !current)}>ⓘ Manual</button>
      </div>
    </div>
    {localError && <div className="message error" role="alert">{localError}</div>}

    {manualOpen && <div className="editor-block" role="dialog" aria-label="Manual da visita e estiva">
      <div className="editor-title"><div><h3>Manual da tela</h3><small>Ajuda contextual da visita, marcos, itens e plano de estiva.</small></div><button type="button" className="small secondary" onClick={() => setManualOpen(false)}>Fechar</button></div>
      <p><strong>Finalidade:</strong> consultar e manter a escala, seus marcos previstos e realizados, itens operacionais e versões do plano de estiva.</p>
      <p><strong>Fluxo operacional:</strong> planejar a visita e a janela de recebimento; confirmar a chegada; avançar as fases para registrar atracação, início, conclusão e partida; manter itens; validar e publicar o plano.</p>
      <p><strong>Campos:</strong> ETA, ETB e ETD são previstos; ATA é a chegada efetiva informada; ATB, início/fim da operação e ATD são preenchidos pelas transições; a janela limita o recebimento; o cutoff define o limite operacional.</p>
      <p><strong>Permissões:</strong> consulta para perfis autorizados; alteração, cancelamento, validação e publicação dependem das permissões operacionais configuradas no backend.</p>
      <p><strong>Estados:</strong> PREVISTA, FUNDEADA, ATRACADA, OPERANDO, OPERACAO_CONCLUIDA, PARTIU e CANCELADA. PARTIU e CANCELADA bloqueiam edição.</p>
      <p><strong>Motivos de bloqueio:</strong> cronologia inválida, visita encerrada, item operado/cancelado, plano em estado incompatível, dados obrigatórios ausentes ou ausência de permissão.</p>
      <p><strong>Exemplo:</strong> ETA 08:00, ATA 08:20, ETB 09:00, ATB registrado ao atracar, operação iniciada pela transição OPERANDO e ATD gravado ao partir.</p>
      <p><strong>Atalhos:</strong> F1 abre este manual; Shift + ? abre o manual fora de campos de edição.</p>
      <p><strong>Processo completo:</strong> <a href="https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/visita-navio-marcos-janelas.md" target="_blank" rel="noreferrer">visita de navio, marcos e janelas</a>.</p>
    </div>}

    <form className="editor-block" onSubmit={saveVisit}>
      <div className="editor-title">
        <div><h3>Dados da visita</h3><small>Os dados somente são recarregados após a persistência confirmada pela API.</small></div>
        <div className="actions">
          <button disabled={visitClosed || !visitValid || busyKey === 'visit-update'}>{busyKey === 'visit-update' ? 'Salvando...' : 'Salvar visita'}</button>
          <button type="button" className="danger" disabled={visitClosed || busyKey === 'visit-cancel'} onClick={cancelVisit}>{busyKey === 'visit-cancel' ? 'Cancelando...' : 'Cancelar visita'}</button>
        </div>
      </div>
      {visitDateErrors.length > 0 && <div className="message warning" role="alert"><strong>Corrija a cronologia antes de salvar:</strong>{visitDateErrors.map((error) => <p key={error}>{error}</p>)}</div>}
      <div className="editor-grid">
        <label>Código da visita<input required disabled={visitClosed} value={visitDraft.codigoVisita} onChange={(event) => setVisitDraft({ ...visitDraft, codigoVisita: event.target.value })} /></label>
        <label>Navio<input value={visit.navioNome || visit.navioId} readOnly /></label>
        <label>Viagem de entrada<input disabled={visitClosed} value={visitDraft.viagemEntrada} onChange={(event) => setVisitDraft({ ...visitDraft, viagemEntrada: event.target.value })} /></label>
        <label>Viagem de saída<input disabled={visitClosed} value={visitDraft.viagemSaida} onChange={(event) => setVisitDraft({ ...visitDraft, viagemSaida: event.target.value })} /></label>
        <label>Linha operadora<input disabled={visitClosed} value={visitDraft.linhaOperadora} onChange={(event) => setVisitDraft({ ...visitDraft, linhaOperadora: event.target.value })} /></label>
        <label>Terminal/facility<input disabled={visitClosed} value={visitDraft.terminalFacility} onChange={(event) => setVisitDraft({ ...visitDraft, terminalFacility: event.target.value })} /></label>
        <label>Berço previsto<input disabled={visitClosed} value={visitDraft.bercoPrevisto} onChange={(event) => setVisitDraft({ ...visitDraft, bercoPrevisto: event.target.value })} /></label>
        <label>Berço atual<input disabled={visitClosed} value={visitDraft.bercoAtual} onChange={(event) => setVisitDraft({ ...visitDraft, bercoAtual: event.target.value })} /></label>
        {VISIT_MILESTONE_GROUPS.map((group) => <div className="span-2 editor-block" key={group.key}>
          <div className="editor-title"><div><h4>{group.title}</h4><small>{group.description}</small></div></div>
          <div className="editor-grid">{group.fields.map((field) => <label key={field.name}>{field.label}
            <input
              type="datetime-local"
              value={visitDraft[field.name]}
              readOnly={!field.editable}
              aria-readonly={!field.editable}
              disabled={field.editable && visitClosed}
              onChange={field.editable ? (event) => setVisitDraft({ ...visitDraft, [field.name]: event.target.value }) : undefined}
            />
            <small>{field.help} Origem: {field.source}. {field.editable ? 'Editável nesta tela.' : 'Somente leitura nesta tela.'}</small>
          </label>)}</div>
        </div>)}
        <label className="span-2">Observações<textarea disabled={visitClosed} rows="3" value={visitDraft.observacoes} onChange={(event) => setVisitDraft({ ...visitDraft, observacoes: event.target.value })} /></label>
      </div>
    </form>

    <div className="editor-block">
      <div className="editor-title"><div><h3>Itens da operação</h3><small>Edição e cancelamento são persistidos e auditados individualmente.</small></div></div>
      <div className="table-wrap"><table><thead><tr><th>Lote</th><th>Movimento</th><th>Produto</th><th>Quantidade</th><th>Peso total</th><th>Status</th><th>Ações</th></tr></thead><tbody>{items.map((item) => <tr key={item.id}>
        <td><strong>{item.codigoLote}</strong></td><td>{item.tipoMovimento}</td><td>{item.produto}</td><td>{item.quantidade}</td><td>{number(item.pesoTotalToneladas, 3)} t</td><td><span className={statusClass(item.status)}>{item.status}</span></td><td><div className="actions"><button type="button" className="small secondary" disabled={visitClosed || item.status === 'CANCELADO'} onClick={() => { setItemDraft(itemDraftFrom(item)); setLocalError(''); }}>Editar</button><button type="button" className="small danger" disabled={visitClosed || ['OPERADO', 'CANCELADO'].includes(item.status) || busyKey === `item-cancel-${item.id}`} onClick={() => cancelItem(item)}>{busyKey === `item-cancel-${item.id}` ? 'Cancelando...' : 'Cancelar'}</button></div></td>
      </tr>)}</tbody></table></div>
      {!items.length && <p className="empty">Nenhum item cadastrado para a visita.</p>}

      {itemDraft && <form className="item-editor" onSubmit={saveItem}>
        <div className="editor-title"><div><h3>Editar item {itemDraft.codigoLote}</h3><small>ID {itemDraft.id}</small></div><div className="actions"><button type="button" className="secondary" onClick={() => setItemDraft(null)}>Fechar edição</button><button disabled={visitClosed || !itemValid || busyKey === `item-update-${itemDraft.id}`}>{busyKey === `item-update-${itemDraft.id}` ? 'Salvando...' : 'Salvar item'}</button></div></div>
        <div className="editor-grid">
          <label>Movimento<select disabled={visitClosed} value={itemDraft.tipoMovimento} onChange={(event) => setItemDraft({ ...itemDraft, tipoMovimento: event.target.value })}>{MOVIMENTOS.map((value) => <option key={value}>{value}</option>)}</select></label>
          <label>Tipo de carga<select disabled={visitClosed} value={itemDraft.tipoCarga} onChange={(event) => setItemDraft({ ...itemDraft, tipoCarga: event.target.value })}>{TIPOS_CARGA.map((value) => <option key={value}>{value}</option>)}</select></label>
          <label>Código do lote<input disabled={visitClosed} required value={itemDraft.codigoLote} onChange={(event) => setItemDraft({ ...itemDraft, codigoLote: event.target.value })} /></label>
          <label>Produto<input disabled={visitClosed} required value={itemDraft.produto} onChange={(event) => setItemDraft({ ...itemDraft, produto: event.target.value })} /></label>
          <label>Quantidade<input disabled={visitClosed} type="number" min="1" required value={itemDraft.quantidade} onChange={(event) => setItemDraft({ ...itemDraft, quantidade: event.target.value })} /></label>
          <label>Peso unitário (t)<input disabled={visitClosed} type="number" min="0.001" step="0.001" value={itemDraft.pesoUnitarioToneladas} onChange={(event) => setItemDraft({ ...itemDraft, pesoUnitarioToneladas: event.target.value })} /></label>
          <label>Peso total (t)<input disabled={visitClosed} type="number" min="0.001" step="0.001" required value={itemDraft.pesoTotalToneladas} onChange={(event) => setItemDraft({ ...itemDraft, pesoTotalToneladas: event.target.value })} /></label>
          <label>Altura (m)<input disabled={visitClosed} type="number" min="0.001" step="0.001" value={itemDraft.alturaCargaMetros} onChange={(event) => setItemDraft({ ...itemDraft, alturaCargaMetros: event.target.value })} /></label>
          <label>Porão planejado<input disabled={visitClosed} type="number" min="1" value={itemDraft.poraoPlanejado} onChange={(event) => setItemDraft({ ...itemDraft, poraoPlanejado: event.target.value })} /></label>
          <label>Porão real<input disabled={visitClosed} type="number" min="1" value={itemDraft.poraoReal} onChange={(event) => setItemDraft({ ...itemDraft, poraoReal: event.target.value })} /></label>
          <label>Posição planejada<input disabled={visitClosed} value={itemDraft.posicaoPlanejada} onChange={(event) => setItemDraft({ ...itemDraft, posicaoPlanejada: event.target.value })} /></label>
          <label>Posição real<input disabled={visitClosed} value={itemDraft.posicaoReal} onChange={(event) => setItemDraft({ ...itemDraft, posicaoReal: event.target.value })} /></label>
          <label>Origem no pátio<input disabled={visitClosed} value={itemDraft.origemPatio} onChange={(event) => setItemDraft({ ...itemDraft, origemPatio: event.target.value })} /></label>
          <label>Destino no pátio<input disabled={visitClosed} value={itemDraft.destinoPatio} onChange={(event) => setItemDraft({ ...itemDraft, destinoPatio: event.target.value })} /></label>
          <label>Sequência operacional<input disabled={visitClosed} type="number" min="1" value={itemDraft.sequenciaOperacional} onChange={(event) => setItemDraft({ ...itemDraft, sequenciaOperacional: event.target.value })} /></label>
          <label className="span-2">Observações<textarea disabled={visitClosed} rows="3" value={itemDraft.observacoes} onChange={(event) => setItemDraft({ ...itemDraft, observacoes: event.target.value })} /></label>
        </div>
      </form>}
    </div>

    <div className="editor-block plan-editor">
      <div className="editor-title"><div><h3>Plano de estiva</h3><small>Publicação, invalidação, cancelamento e nova aprovação exigem transições persistidas.</small></div>{plan && <span className={statusClass(plan.status)}>{plan.status}</span>}</div>
      {!plan ? <p className="empty">Nenhum plano de estiva disponível para a visita.</p> : <>
        <div className="plan-summary">
          <span><b>v{plan.versao}</b> versão</span><span><b>{plan.posicoes?.length ?? 0}</b> posições</span><span><b>{number(plan.pesoTotalPlanejado, 3)} t</b> planejadas</span><span><b>{number(plan.pesoTotalRealizado, 3)} t</b> realizadas</span>
        </div>
        {validation && <div className="validation-result">
          <strong>{validation.erros?.length ?? 0} erros · {validation.alertas?.length ?? 0} alertas</strong>
          {(validation.erros ?? []).map((entry) => <p className="validation-error" key={entry}>{entry}</p>)}
          {(validation.alertas ?? []).map((entry) => <p className="validation-warning" key={entry}>{entry}</p>)}
        </div>}
        <div className="actions">
          <button type="button" className="secondary" disabled={visitClosed || busyKey === 'plan-validate' || ['CONCLUIDO', 'INVALIDADO', 'CANCELADO'].includes(plan.status)} onClick={() => onValidatePlan(plan.id)}>{busyKey === 'plan-validate' ? 'Validando...' : 'Validar plano'}</button>
          <button type="button" className="warning" disabled={visitClosed || busyKey === 'plan-publish' || plan.status !== 'VALIDADO'} onClick={publishPlan}>{busyKey === 'plan-publish' ? 'Publicando...' : 'Concluir e publicar'}</button>
          <button type="button" className="secondary" disabled={visitClosed || busyKey === 'plan-invalidate' || !['VALIDADO', 'CONCLUIDO'].includes(plan.status)} onClick={invalidatePlan}>{busyKey === 'plan-invalidate' ? 'Invalidando...' : 'Invalidar plano'}</button>
          <button type="button" className="danger" disabled={visitClosed || busyKey === 'plan-cancel' || plan.status === 'CANCELADO'} onClick={cancelPlan}>{busyKey === 'plan-cancel' ? 'Cancelando...' : 'Cancelar plano'}</button>
          <button type="button" className="secondary" disabled={visitClosed || busyKey === 'plan-new-version' || !['CONCLUIDO', 'INVALIDADO', 'CANCELADO'].includes(plan.status)} onClick={onCreatePlanVersion}>{busyKey === 'plan-new-version' ? 'Criando...' : 'Criar nova versão'}</button>
        </div>
      </>}

      <div className="validation-result">
        <strong>Histórico de publicação e aprovação</strong>
        {!planHistory.length ? <p className="empty">Nenhuma transição de plano registrada.</p> : planHistory.map((event) => <p key={event.id || `${event.tipoEvento}-${event.criadoEm}`}><b>{event.tipoEvento}</b> · {dateTime(event.criadoEm)} · {event.usuario || 'sistema'}<br />{event.descricao}</p>)}
      </div>
    </div>
  </section>;
}