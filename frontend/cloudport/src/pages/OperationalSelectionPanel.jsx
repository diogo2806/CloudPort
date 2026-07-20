import { useState } from 'react';
import {
  OPERATIONAL_DOMAINS,
  activateOperationalContext,
  cancelOperationalSimulation,
  formatOperationalLocation,
  markOperationalSimulationReady,
  prepareOperationalSimulation,
  useOperationalSelection
} from '../operational-selection.js';
import '../operational-workspace.css';

const DOMAIN_ORDER = ['vessel', 'yard', 'rail', 'equipment'];
const DRAG_TYPE = 'application/x-cloudport-operational-domain';

function ContextCard({ domain, context, active, onActivate }) {
  function dragStart(event) {
    event.dataTransfer.effectAllowed = 'link';
    event.dataTransfer.setData(DRAG_TYPE, domain);
    event.dataTransfer.setData('text/plain', domain);
  }

  function drop(event) {
    event.preventDefault();
    const sourceDomain = event.dataTransfer.getData(DRAG_TYPE) || event.dataTransfer.getData('text/plain');
    if (sourceDomain) prepareOperationalSimulation(sourceDomain, domain);
  }

  return <article
    className={`operational-context-card ${active ? 'active' : ''} ${context ? 'available' : 'empty'}`}
    draggable={Boolean(context)}
    onDragStart={dragStart}
    onDragOver={(event) => event.preventDefault()}
    onDrop={drop}
  >
    <header><strong>{OPERATIONAL_DOMAINS[domain]}</strong><span>{context ? 'Sincronizado' : 'Sem seleção'}</span></header>
    {context ? <>
      <button type="button" className="context-card-main" onClick={onActivate}>
        <strong>{context.label}</strong>
        <small>{formatOperationalLocation(context)}</small>
      </button>
      <dl>
        <div><dt>Unidade</dt><dd>{context.unitCode || '—'}</dd></div>
        <div><dt>WI</dt><dd>{context.workInstruction || '—'}</dd></div>
        <div><dt>CHE</dt><dd>{context.equipment || '—'}</dd></div>
      </dl>
      <small className="context-drag-hint">Arraste este contexto sobre outro domínio para simular.</small>
    </> : <p>Selecione um elemento neste domínio para compor o workspace integrado.</p>}
  </article>;
}

export function OperationalWorkspaceManual({ scope = 'workspace operacional 2D' }) {
  return <details className="operational-workspace-manual">
    <summary aria-label={`Abrir manual do ${scope}`}>ⓘ Manual</summary>
    <div className="content-card">
      <h3>Finalidade da tela</h3>
      <p>Sincronizar a unidade selecionada entre Navio, Pátio, Ferrovia e equipamentos, preservando posição, work instruction, origem, destino e recurso operacional.</p>
      <h3>Fluxo operacional</h3>
      <ol><li>Selecione uma unidade ou posição no desenho.</li><li>Navegue por zoom, duplo clique ou breadcrumbs.</li><li>Arraste um contexto de domínio sobre outro para gerar uma simulação.</li><li>Confira bloqueios, informe o motivo e encaminhe a proposta ao comando transacional correspondente.</li></ol>
      <h3>Explicação dos campos</h3>
      <ul><li>Unidade: contêiner ou carga selecionada.</li><li>WI: work instruction associada.</li><li>CHE: equipamento atribuído ou sugerido.</li><li>Origem e destino: posições físicas usadas na simulação.</li><li>Nível: visão geral, bloco, linha, pilha, tier ou slot.</li></ul>
      <h3>Permissões necessárias</h3>
      <p>A consulta segue as permissões da tela. A confirmação continua restrita aos perfis autorizados pelo backend para planejar ou operar o domínio de destino.</p>
      <h3>Estados possíveis</h3>
      <ul><li>Sem seleção.</li><li>Sincronizado.</li><li>Simulado.</li><li>Bloqueado.</li><li>Pronto para confirmação transacional.</li></ul>
      <h3>Motivos de bloqueio</h3>
      <ul><li>Origem ou destino não selecionado.</li><li>Unidades divergentes entre os domínios.</li><li>Destino sem posição física.</li><li>Restrição operacional, permissão insuficiente ou validação definitiva do backend.</li></ul>
      <h3>Exemplo</h3>
      <p>Selecione o contêiner no navio, abra sua pilha no pátio e arraste o cartão Navio sobre Pátio para comparar origem, destino, WI e CHE antes de confirmar a movimentação.</p>
      <h3>Atalhos</h3>
      <ul><li>Duplo clique: aprofundar um nível.</li><li>Backspace: retornar preservando contexto.</li><li>+ e -: ajustar zoom.</li><li>0: restaurar viewport.</li><li>Esc: cancelar seleção de área.</li></ul>
      <p><a href="https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/workspace-operacional-2d.md" target="_blank" rel="noreferrer">Abrir processo completo</a></p>
    </div>
  </details>;
}

export function OperationalSelectionPanel({ title = 'Seleção operacional compartilhada' }) {
  const selection = useOperationalSelection();
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const simulation = selection.simulation;

  function readyForTransaction() {
    const result = markOperationalSimulationReady(reason);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    setError('');
  }

  function cancel() {
    cancelOperationalSimulation();
    setReason('');
    setError('');
  }

  return <section className="operational-selection-panel" aria-label={title}>
    <header><div><span>BUS1590</span><h3>{title}</h3></div><small>Navio × Pátio × Ferrovia × CHE</small></header>
    <div className="operational-domain-grid">
      {DOMAIN_ORDER.map((domain) => <ContextCard
        key={domain}
        domain={domain}
        context={selection.contexts?.[domain]}
        active={selection.active?.domain === domain}
        onActivate={() => activateOperationalContext(domain)}
      />)}
    </div>
    {simulation && <div className={`operational-simulation ${simulation.valid ? 'valid' : 'blocked'}`}>
      <header><strong>Simulação entre domínios</strong><span>{simulation.status}</span></header>
      <p><strong>{OPERATIONAL_DOMAINS[simulation.source?.domain] ?? 'Origem'}</strong> {simulation.source?.label ?? 'não selecionada'} → <strong>{OPERATIONAL_DOMAINS[simulation.target?.domain] ?? 'Destino'}</strong> {simulation.target?.label ?? 'não selecionado'}</p>
      {simulation.reasons?.length > 0 && <ul>{simulation.reasons.map((item) => <li key={item}>{item}</li>)}</ul>}
      {simulation.valid && <label>Motivo operacional<textarea value={reason} onChange={(event) => setReason(event.target.value)} maxLength={500} placeholder="Justifique a proposta antes da confirmação" /></label>}
      {error && <p className="operational-simulation-error">{error}</p>}
      <div><button type="button" disabled={!simulation.valid || simulation.status === 'READY_FOR_TRANSACTION'} onClick={readyForTransaction}>Encaminhar para confirmação</button><button type="button" className="secondary" onClick={cancel}>Cancelar simulação</button></div>
      {simulation.status === 'READY_FOR_TRANSACTION' && <small>A proposta está preparada. A persistência deve ser concluída pelo comando autorizado do domínio de destino.</small>}
    </div>}
  </section>;
}
