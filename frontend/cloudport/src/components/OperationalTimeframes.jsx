import { useEffect, useMemo, useState } from 'react';
import { api, formatError } from '../api.js';
import { yardPredictiveApi } from '../yardPredictiveApi.js';
import {
  OPERATIONAL_STATES,
  TIMEFRAME_MODES,
  buildVesselTimeframeScene,
  buildYardTimeframeScene,
  operationalSceneSummary,
  timeframeDescriptor
} from '../operational-timeframes.js';
import '../operational-timeframes.css';

const PROCESS_LINK = 'https://github.com/diogo2806/CloudPort/blob/main/docs/implementados/BUS1610-BUS1620-timeframes-estados-2d.md';

function displayDateTime(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'não informado' : date.toLocaleString('pt-BR');
}

function stateClass(state) {
  return String(state || 'livre').toLowerCase().replace(/_/g, '-');
}

function OperationalManual({ domain }) {
  const yard = domain === 'yard';
  return <details className="operational-timeframe-manual">
    <summary aria-label={`Abrir manual dos timeframes do ${yard ? 'pátio' : 'navio'}`}>? Manual</summary>
    <div>
      <h4>Finalidade da tela</h4>
      <p>Comparar o estado físico com planos futuros, propostas, estivagem e movimentos iminentes sem perder a posição selecionada.</p>
      <h4>Fluxo operacional</h4>
      <p>Selecione um timeframe, ative a comparação, escolha um elemento gráfico e confira a origem, o instante de referência e os estados sobrepostos.</p>
      <h4>Explicação dos campos</h4>
      <p><strong>Timeframe</strong> define o horizonte principal. <strong>Comparar com</strong> abre o segundo horizonte. A legenda explica os símbolos; o detalhe da seleção mostra as fontes.</p>
      <h4>Permissões necessárias</h4>
      <p>A visualização respeita as permissões de consulta do domínio. Alterações continuam restritas aos comandos operacionais já existentes para ADMIN_PORTO, PLANEJADOR e, no pátio, OPERADOR_PATIO.</p>
      <h4>Estados possíveis</h4>
      <p>Proposta, tentativo, definitivo, reservado, atribuído, despachado, em execução, bloqueado, falha e concluído.</p>
      <h4>Motivos de bloqueio</h4>
      <p>Posição interditada, área não permitida, tampa incompatível, instrução suspensa, plano expirado, conflito físico ou ausência de dados autorizados.</p>
      <h4>Exemplos</h4>
      <p>Compare Current com Future para identificar destinos planejados; use Imminent para localizar trabalhos despachados ou em execução.</p>
      <h4>Atalhos</h4>
      <p>Tab percorre controles e elementos. Enter ou Espaço seleciona. Esc fecha o detalhe nativo do navegador quando aplicável.</p>
      <a href={PROCESS_LINK} target="_blank" rel="noreferrer">Abrir processo completo</a>
    </div>
  </details>;
}

function TimeframeToolbar({ domain, timeframe, setTimeframe, compare, setCompare, comparison, setComparison, loading, onReload }) {
  return <div className="operational-timeframe-toolbar">
    <div className="operational-timeframe-heading">
      <div><span>BUS1610 e BUS1620</span><h3>{domain === 'yard' ? 'Horizontes gráficos do pátio' : 'Horizontes gráficos do navio'}</h3></div>
      <OperationalManual domain={domain} />
    </div>
    <div className="operational-timeframe-controls">
      <label>Timeframe<select value={timeframe} onChange={(event) => setTimeframe(event.target.value)}>{TIMEFRAME_MODES.map((mode) => <option key={mode.value} value={mode.value}>{mode.label}</option>)}</select></label>
      <label className="operational-timeframe-checkbox"><input type="checkbox" checked={compare} onChange={(event) => setCompare(event.target.checked)} /> Comparação lado a lado</label>
      {compare && <label>Comparar com<select value={comparison} onChange={(event) => setComparison(event.target.value)}>{TIMEFRAME_MODES.filter((mode) => mode.value !== timeframe).map((mode) => <option key={mode.value} value={mode.value}>{mode.label}</option>)}</select></label>}
      <button type="button" className="secondary" disabled={loading} onClick={onReload}>{loading ? 'Atualizando...' : 'Atualizar fontes'}</button>
    </div>
  </div>;
}

function StateLegend() {
  return <div className="operational-state-legend" aria-label="Legenda dos estados operacionais">
    {OPERATIONAL_STATES.map((state) => <span key={state.value} title={state.description}><i className={`state-${stateClass(state.value)}`} aria-hidden="true">{state.symbol}</i>{state.label}</span>)}
  </div>;
}

function SceneSummary({ scene }) {
  const summary = operationalSceneSummary(scene);
  return <div className="operational-timeframe-summary">
    {OPERATIONAL_STATES.filter((state) => summary[state.value]).map((state) => <span key={state.value}><i aria-hidden="true">{state.symbol}</i>{state.label} <strong>{summary[state.value]}</strong></span>)}
    {!Object.keys(summary).length && <span>Nenhum estado operacional neste horizonte.</span>}
  </div>;
}

function selectionTitle(item) {
  const details = (item?.descriptors ?? []).map((entry) => [
    `${entry.symbol} ${entry.label}`,
    entry.origin,
    entry.reference ? displayDateTime(entry.reference) : '',
    entry.detail
  ].filter(Boolean).join(' · '));
  return details.length ? details.join('\n') : 'Elemento sem estado operacional.';
}

function SelectionInspector({ item }) {
  if (!item) return <div className="operational-timeframe-inspector empty">Selecione um slot ou camada para comparar suas fontes.</div>;
  return <aside className="operational-timeframe-inspector">
    <header><strong>{item.containerCode || item.label || item.key}</strong><span>{item.stateLabel}</span></header>
    <ol>{(item.descriptors ?? []).map((entry, index) => <li key={`${entry.state}-${entry.origin}-${index}`}>
      <i className={`state-${stateClass(entry.state)}`} aria-hidden="true">{entry.symbol}</i>
      <div><strong>{entry.label}</strong><span>{entry.origin}</span><small>{entry.reference ? displayDateTime(entry.reference) : 'Instante não informado'}{entry.detail ? ` · ${entry.detail}` : ''}</small></div>
    </li>)}</ol>
  </aside>;
}

function YardScene({ scene, selectedKey, onSelect }) {
  const mode = timeframeDescriptor(scene.timeframe);
  return <section className="operational-timeframe-pane" aria-label={`Pátio no horizonte ${mode.label}`}>
    <header><div><strong>{mode.label}</strong><span>{mode.description}</span></div><small>Referência: {displayDateTime(scene.referenceTime)}</small></header>
    <SceneSummary scene={scene} />
    <div className="operational-yard-timeframe-blocks">{scene.blocks.map((block) => <article key={block.bloco}>
      <h4>{block.bloco}</h4>
      <div>{block.stacks.map((stack) => <section key={stack.key} className="operational-yard-timeframe-stack">
        <header>L{stack.linha} · C{stack.coluna}</header>
        <div>{stack.layers.map((layer) => <button
          type="button"
          key={layer.key}
          className={`operational-timeframe-cell state-${stateClass(layer.state)}${layer.dimmed ? ' dimmed' : ''}${selectedKey === layer.key ? ' selected' : ''}`}
          onClick={() => onSelect(layer.key)}
          aria-pressed={selectedKey === layer.key}
          aria-label={`${block.bloco}, linha ${stack.linha}, coluna ${stack.coluna}, camada ${layer.label}, ${layer.stateLabel}, ${layer.containerCode || 'sem contêiner'}`}
          title={selectionTitle(layer)}
        ><span>{layer.label}</span><i aria-hidden="true">{layer.symbol}</i><small>{layer.containerCode || 'Livre'}</small></button>)}</div>
      </section>)}</div>
    </article>)}</div>
  </section>;
}

function VesselScene({ scene, selectedKey, onSelect }) {
  const mode = timeframeDescriptor(scene.timeframe);
  const bays = useMemo(() => Array.from(new Set(scene.slots.map((slot) => slot.bay))).sort((left, right) => left - right), [scene.slots]);
  return <section className="operational-timeframe-pane" aria-label={`Navio no horizonte ${mode.label}`}>
    <header><div><strong>{mode.label}</strong><span>{mode.description}</span></div><small>Referência: {displayDateTime(scene.referenceTime)}</small></header>
    <SceneSummary scene={scene} />
    <div className="operational-vessel-timeframe-bays">{bays.map((bay) => {
      const slots = scene.slots.filter((slot) => slot.bay === bay).sort((left, right) => right.tier - left.tier || left.rowBay - right.rowBay);
      return <article key={bay}><h4>Bay {bay}</h4><div>{slots.map((slot) => <button
        type="button"
        key={slot.key}
        className={`operational-timeframe-cell state-${stateClass(slot.state)}${slot.dimmed ? ' dimmed' : ''}${selectedKey === slot.key ? ' selected' : ''}`}
        onClick={() => onSelect(slot.key)}
        aria-pressed={selectedKey === slot.key}
        aria-label={`Bay ${slot.bay}, row ${slot.rowBay}, tier ${slot.tier}, ${slot.stateLabel}, ${slot.containerCode || 'sem contêiner'}`}
        title={selectionTitle(slot)}
      ><span>R{slot.rowBay} T{slot.tier}</span><i aria-hidden="true">{slot.symbol}</i><small>{slot.containerCode || 'Livre'}</small></button>)}</div></article>;
    })}</div>
  </section>;
}

function useTimeframeControls() {
  const [timeframe, setTimeframe] = useState('COMPOSITE');
  const [compare, setCompare] = useState(true);
  const [comparison, setComparison] = useState('CURRENT');
  useEffect(() => {
    if (comparison === timeframe) setComparison(timeframe === 'CURRENT' ? 'FUTURE' : 'CURRENT');
  }, [comparison, timeframe]);
  return { timeframe, setTimeframe, compare, setCompare, comparison, setComparison };
}

export function YardOperationalTimeframes({ blocks }) {
  const controls = useTimeframeControls();
  const [plans, setPlans] = useState([]);
  const [selectedKey, setSelectedKey] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function load() {
    setLoading(true);
    setError('');
    try {
      const response = await yardPredictiveApi.listarPlanos();
      setPlans(Array.isArray(response) ? response : []);
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  const scenes = useMemo(() => {
    const modes = controls.compare ? [controls.timeframe, controls.comparison] : [controls.timeframe];
    return modes.map((mode) => buildYardTimeframeScene(blocks, plans, mode));
  }, [blocks, plans, controls.compare, controls.timeframe, controls.comparison]);
  const selectedItem = scenes.flatMap((scene) => scene.blocks.flatMap((block) => block.stacks.flatMap((stack) => stack.layers))).find((item) => item.key === selectedKey) ?? null;

  return <section className="operational-timeframes-shell">
    <TimeframeToolbar domain="yard" {...controls} loading={loading} onReload={load} />
    {error && <div className="operational-timeframe-error">{error}</div>}
    <StateLegend />
    <div className={`operational-timeframe-comparison panes-${scenes.length}`}>{scenes.map((scene) => <YardScene key={scene.timeframe} scene={scene} selectedKey={selectedKey} onSelect={setSelectedKey} />)}</div>
    <SelectionInspector item={selectedItem} />
  </section>;
}

export function VesselOperationalTimeframes({ planId }) {
  const controls = useTimeframeControls();
  const [data, setData] = useState({ plan: null, restow: null, sequencing: null });
  const [selectedKey, setSelectedKey] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function load() {
    if (!planId) return;
    setLoading(true);
    setError('');
    try {
      const [plan, restow, sequencing] = await Promise.all([
        api.obterPlanoVesselPlanner(planId),
        api.obterRestowPlano(planId).catch(() => null),
        api.obterSequenciamentoGuindastes(planId, 2).catch(() => null)
      ]);
      setData({ plan, restow, sequencing });
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    setData({ plan: null, restow: null, sequencing: null });
    setSelectedKey('');
    load();
  }, [planId]);

  const scenes = useMemo(() => {
    if (!data.plan) return [];
    const modes = controls.compare ? [controls.timeframe, controls.comparison] : [controls.timeframe];
    return modes.map((mode) => buildVesselTimeframeScene(data.plan, data.restow, data.sequencing, mode));
  }, [data, controls.compare, controls.timeframe, controls.comparison]);
  const selectedItem = scenes.flatMap((scene) => scene.slots).find((item) => item.key === selectedKey) ?? null;

  return <section className="operational-timeframes-shell vessel-operational-timeframes">
    <TimeframeToolbar domain="vessel" {...controls} loading={loading} onReload={load} />
    {error && <div className="operational-timeframe-error">{error}</div>}
    {!loading && !data.plan && !error && <div className="operational-timeframe-empty">O plano ainda não possui dados para os horizontes gráficos.</div>}
    {!!scenes.length && <><StateLegend /><div className={`operational-timeframe-comparison panes-${scenes.length}`}>{scenes.map((scene) => <VesselScene key={scene.timeframe} scene={scene} selectedKey={selectedKey} onSelect={setSelectedKey} />)}</div><SelectionInspector item={selectedItem} /></>}
  </section>;
}
