import { useMemo } from 'react';
import Ui20CraneSequences from './Ui20CraneSequences.jsx';
import { PLAN_STATUSES, dateTime, defaultAllocation, number, queueOperational, statusClass, validatePlan } from './ui20-model.js';

const Metric = ({ label, value, detail }) => <article className="metric"><span>{label}</span><strong>{value}</strong><small>{detail}</small></article>;

export default function Ui20Plan({ visitId, monitor, plan, draft, queues, equipment, dirty, busy, onChange, onSave, onReload }) {
  const errors = useMemo(() => validatePlan(draft, queues, visitId), [draft, queues, visitId]);
  const productivity = monitor?.produtividade || {};
  const patchAllocation = (index, patch) => onChange({ ...draft, guindastes: draft.guindastes.map((item, current) => current === index ? { ...item, ...patch } : item) });
  const add = () => {
    const used = new Set(draft.guindastes.map((item) => Number(item.workQueueId)));
    const queue = queues.find((item) => !used.has(Number(item.id)) && queueOperational(item, visitId, draft.berco, item.porao));
    onChange({ ...draft, guindastes: [...draft.guindastes, defaultAllocation(draft.guindastes.length, queue)] });
  };
  return <>
    <section className="panel quay-monitor">
      <div className="section-head"><div><span className="eyebrow">Quay Monitor</span><h2>Plano operacional de guindastes</h2></div><div className="actions"><span className={statusClass(plan?.status || monitor?.statusPlanoGuindaste)}>{plan?.status || monitor?.statusPlanoGuindaste || 'SEM_PLANO'}</span>{dirty && <span className="unsaved-badge">Não salvo</span>}</div></div>
      <div className="metrics quay-metrics"><Metric label="Berço" value={productivity.berco || monitor?.bercoAtual || monitor?.bercoPrevisto || '—'} detail={monitor?.fase || '—'} /><Metric label="Movimentos" value={`${productivity.movimentosRealizados || 0}/${productivity.movimentosPlanejados || 0}`} detail={`${productivity.movimentosPendentes || 0} pendentes`} /><Metric label="Produtividade" value={`${number(productivity.produtividadeAtualMovimentosHora, 2)} mov/h`} detail={`${number(productivity.produtividadePlanejadaMovimentosHora, 2)} planejada`} /><Metric label="Previsão" value={dateTime(productivity.previsaoTermino)} detail={`${number(productivity.percentualConclusao, 1)}% concluído`} /></div>
      {(monitor?.alertas || []).length > 0 && <div className="message warning-message">{monitor.alertas.join(' ')}</div>}
      <div className="plan-header-grid"><label>Berço<input value={draft.berco} onChange={(event) => onChange({ ...draft, berco: event.target.value })} /></label><label>Status<select value={draft.status} onChange={(event) => onChange({ ...draft, status: event.target.value })}>{PLAN_STATUSES.map((value) => <option key={value}>{value}</option>)}</select></label><label>Observação<input value={draft.observacao} onChange={(event) => onChange({ ...draft, observacao: event.target.value })} /></label><div className="actions plan-actions"><button className="secondary" onClick={add}>Adicionar</button><button className="secondary" onClick={onReload}>Recarregar</button><button disabled={busy || errors.length > 0} onClick={onSave}>Salvar plano</button></div></div>
      <div className="plan-allocation-list">{draft.guindastes.map((item, index) => {
        const selected = queues.find((queue) => Number(queue.id) === Number(item.workQueueId));
        const valid = queues.filter((queue) => queueOperational(queue, visitId, draft.berco, item.porao));
        return <article className="plan-allocation" key={item.id || `new-${index}`}><div className="plan-allocation-title"><strong>Alocação {index + 1}</strong><span>{selected?.identificador || 'Sem work queue válida'}</span><button className="small danger" onClick={() => onChange({ ...draft, guindastes: draft.guindastes.filter((_, current) => current !== index) })}>Remover</button></div><div className="plan-allocation-grid">
          <label>Guindaste<input list="equipment-list" value={item.codigoGuindaste} onChange={(event) => patchAllocation(index, { codigoGuindaste: event.target.value })} /></label><label>Recurso de cais<input value={item.recursoCais || ''} onChange={(event) => patchAllocation(index, { recursoCais: event.target.value })} /></label><label>Porão<input type="number" min="1" value={item.porao} onChange={(event) => patchAllocation(index, { porao: event.target.value })} /></label>
          <label>Work queue<select value={item.workQueueId} onChange={(event) => { const queue = queues.find((entry) => Number(entry.id) === Number(event.target.value)); patchAllocation(index, { workQueueId: event.target.value, codigoGuindaste: item.codigoGuindaste || queue?.equipamento || '', recursoCais: item.recursoCais || queue?.pow || '', porao: item.porao || queue?.porao || '' }); }}><option value="">Selecione</option>{selected && !valid.some((queue) => queue.id === selected.id) && <option value={selected.id}>⚠ {selected.identificador}</option>}{valid.map((queue) => <option key={queue.id} value={queue.id}>{queue.identificador} · {queue.equipamento} · {queue.pow}</option>)}</select></label>
          <label>Sequência<input type="number" min="1" value={item.sequencia} onChange={(event) => patchAllocation(index, { sequencia: event.target.value })} /></label><label>Movimentos<input type="number" min="1" value={item.movimentosPlanejados} onChange={(event) => patchAllocation(index, { movimentosPlanejados: event.target.value })} /></label><label>Produtividade<input type="number" min="0.01" step="0.01" value={item.produtividadePlanejadaMovimentosHora} onChange={(event) => patchAllocation(index, { produtividadePlanejadaMovimentosHora: event.target.value })} /></label><label>Início<input type="datetime-local" value={item.inicioPlanejado} onChange={(event) => patchAllocation(index, { inicioPlanejado: event.target.value })} /></label><label>Fim<input type="datetime-local" value={item.fimPlanejado} onChange={(event) => patchAllocation(index, { fimPlanejado: event.target.value })} /></label><label>Observação<input value={item.observacao || ''} onChange={(event) => patchAllocation(index, { observacao: event.target.value })} /></label>
        </div></article>;
      })}</div>
      <datalist id="equipment-list">{equipment.map((item) => <option key={item.equipamentoPatioId} value={item.equipamentoIdentificador} />)}</datalist>
      {errors.length > 0 && <div className="plan-validation"><strong>Plano inválido:</strong><ul>{errors.map((item) => <li key={item}>{item}</li>)}</ul></div>}
    </section>
    <Ui20CraneSequences visitId={visitId} plan={plan} />
  </>;
}
