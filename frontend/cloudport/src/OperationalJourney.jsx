import { buildOperationalJourney, nextJourneyAction, normalizeRoles } from './operationalJourney.js';
import './operationalJourney.css';

const STATE_LABELS = {
  COMPLETED: 'Concluída',
  CURRENT: 'Atual',
  FUTURE: 'Futura',
  BLOCKED: 'Bloqueada',
  CANCELLED: 'Cancelada'
};

const STATE_ICONS = {
  COMPLETED: '✓',
  CURRENT: '●',
  FUTURE: '○',
  BLOCKED: '!',
  CANCELLED: '×'
};

export function OperationalJourney({
  title = 'Jornada operacional',
  processId,
  steps,
  currentStepId,
  session,
  blockers = [],
  cancelled = false,
  responsible,
  elapsed,
  history = [],
  help,
  onOpenStep,
  onPrimaryAction
}) {
  const journey = buildOperationalJourney({ steps, currentStepId, roles: normalizeRoles(session), blockers, cancelled });
  const nextAction = nextJourneyAction(journey);

  return <section className="operational-journey" aria-label={title}>
    <header className="operational-journey__header">
      <div>
        <span className="eyebrow">{processId ? `Processo ${processId}` : 'Fluxo operacional'}</span>
        <h2>{title}</h2>
        <p>{responsible ? `Responsável atual: ${responsible}` : 'Responsável atual não informado'}{elapsed ? ` · Tempo decorrido: ${elapsed}` : ''}</p>
      </div>
      {help && <details className="operational-journey__help">
        <summary aria-label="Abrir manual da jornada">?</summary>
        <div>
          <h3>Manual</h3>
          <p><strong>Finalidade:</strong> {help.purpose}</p>
          <p><strong>Fluxo operacional:</strong> {help.flow}</p>
          <p><strong>Campos:</strong> {help.fields}</p>
          <p><strong>Permissões:</strong> {help.permissions}</p>
          <p><strong>Estados possíveis:</strong> concluída, atual, futura, bloqueada e cancelada.</p>
          <p><strong>Motivos de bloqueio:</strong> {help.blockers}</p>
          <p><strong>Exemplo:</strong> {help.example}</p>
          <p><strong>Atalhos:</strong> {help.shortcuts}</p>
          {help.fullProcessUrl && <a href={help.fullProcessUrl}>Abrir processo completo</a>}
        </div>
      </details>}
    </header>

    <ol className="operational-journey__steps">
      {journey.map((step) => <li className={`operational-journey__step state-${step.state.toLowerCase()}`} key={step.id}>
        <button type="button" onClick={() => onOpenStep?.(step)} disabled={!step.permitted} aria-current={step.state === 'CURRENT' ? 'step' : undefined}>
          <span className="operational-journey__icon" aria-hidden="true">{STATE_ICONS[step.state]}</span>
          <span className="operational-journey__content">
            <strong>{step.label}</strong>
            <small>{STATE_LABELS[step.state]}{step.permissionLabel ? ` · ${step.permissionLabel}` : ''}</small>
            {step.description && <span>{step.description}</span>}
            {step.blockingReason && <em>{step.blockingReason}</em>}
          </span>
        </button>
      </li>)}
    </ol>

    <div className="operational-journey__footer">
      <div>
        <span className="eyebrow">Próxima ação</span>
        <strong>{nextAction?.label || 'Jornada concluída'}</strong>
      </div>
      {nextAction && <button type="button" disabled={nextAction.disabled} onClick={() => onPrimaryAction?.(nextAction.step)}>{nextAction.label}</button>}
    </div>

    {history.length > 0 && <details className="operational-journey__history">
      <summary>Histórico resumido ({history.length})</summary>
      <ul>{history.map((item, index) => <li key={item.id ?? index}><strong>{item.label}</strong>{item.when ? ` · ${item.when}` : ''}{item.by ? ` · ${item.by}` : ''}</li>)}</ul>
    </details>}
  </section>;
}
