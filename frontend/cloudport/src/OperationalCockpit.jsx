import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, formatError, readSession } from './api.js';
import { Message, PageHeader } from './components.jsx';
import {
  COCKPIT_REFRESH_OPTIONS,
  applyCockpitPreferences,
  buildCockpitBlock,
  cockpitStorageKey,
  createCockpitSnapshot,
  defaultCockpitPreferences,
  isCockpitStale,
  moveCockpitBlock,
  permittedCockpitDefinitions,
  readCockpitStorage,
  sanitizeCockpitPreferences,
  toggleCockpitBlock,
  writeCockpitStorage
} from './operationalCockpit.js';
import './operational-cockpit.css';

const LOADERS = {
  visibility: () => api.obterDashboardVisibilidade(),
  gate: () => api.obterCentralGate(),
  yard: () => api.listarOrdensPatio(),
  rail: () => api.listarVisitasFerrovia(30),
  vessel: () => api.listarEscalasEmbarque(30),
  equipment: () => api.listarTelemetriaEquipamentosPatio(),
  edi: () => api.listarProcessamentosEdi({ tipo: 'BAPLIE', pagina: 0, tamanho: 50 })
};

function formatDateTime(value) {
  if (!value) return 'Ainda não atualizado';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Data indisponível' : date.toLocaleString('pt-BR');
}

function stateLabel(state) {
  return {
    loading: 'Carregando',
    error: 'Indisponível',
    empty: 'Sem pendências',
    attention: 'Requer atenção',
    ready: 'Disponível'
  }[state] ?? state;
}

function trendSymbol(direction) {
  return { up: '↑', down: '↓', stable: '→', new: '•', unavailable: '×' }[direction] ?? '•';
}

function blockIcon(key) {
  return {
    alerts: '!',
    gate: '⇥',
    yard: '▥',
    rail: '▰',
    vessel: '◒',
    equipment: '◉',
    edi: '⇄'
  }[key] ?? '□';
}

function Distribution({ entries = [], title }) {
  if (!entries.length) return <p className="cockpit-no-distribution">Sem distribuição por estado nesta leitura.</p>;
  const maximum = Math.max(...entries.map((entry) => Number(entry.value) || 0), 1);
  return <div className="cockpit-distribution">
    <div className="cockpit-bars" role="img" aria-label={`Distribuição de ${title}: ${entries.map((entry) => `${entry.label} ${entry.value}`).join(', ')}`}>
      {entries.map((entry) => <div className="cockpit-bar-row" key={entry.label}>
        <span title={entry.label}>{entry.label.replaceAll('_', ' ')}</span>
        <div aria-hidden="true"><i style={{ width: `${Math.max((Number(entry.value) / maximum) * 100, 3)}%` }} /></div>
        <strong>{entry.value}</strong>
      </div>)}
    </div>
    <table className="cockpit-distribution-table">
      <caption>Dados equivalentes do gráfico de {title}</caption>
      <thead><tr><th>Estado</th><th>Quantidade</th></tr></thead>
      <tbody>{entries.map((entry) => <tr key={entry.label}><td>{entry.label.replaceAll('_', ' ')}</td><td>{entry.value}</td></tr>)}</tbody>
    </table>
  </div>;
}

function CockpitCard({ block, index, total, personalizing, onOpen, onMove, onHide, onReload }) {
  const value = block.state === 'error' ? '—' : block.value ?? 0;
  return <article className={`cockpit-card cockpit-state-${block.state}`}>
    <header>
      <div className="cockpit-card-title">
        <span className="cockpit-card-icon" aria-hidden="true">{blockIcon(block.key)}</span>
        <div><h2>{block.title}</h2><p>{block.description}</p></div>
      </div>
      <span className="cockpit-state-label">{stateLabel(block.state)}</span>
    </header>

    {block.state === 'error' ? <div className="cockpit-error" role="status">
      <strong>Fonte temporariamente indisponível</strong>
      <p>{block.error}</p>
      <button type="button" className="secondary small" onClick={onReload}>Tentar novamente</button>
    </div> : <>
      <div className="cockpit-metric">
        <strong>{value}</strong>
        <div><span>{block.detail}</span><small>{block.period}</small></div>
      </div>
      <div className={`cockpit-trend cockpit-trend-${block.trend.favorable === true ? 'good' : block.trend.favorable === false ? 'bad' : 'neutral'}`}>
        <span aria-hidden="true">{trendSymbol(block.trend.direction)}</span>
        <strong>{block.trend.label}</strong>
      </div>
      <Distribution entries={block.distribution} title={block.title} />
      <button type="button" className="cockpit-drilldown" onClick={() => onOpen(block.route)}>Abrir lista correspondente <span aria-hidden="true">→</span></button>
    </>}

    <footer>
      <span>Atualizado em {formatDateTime(block.updatedAt)}</span>
      {personalizing && <div className="cockpit-card-controls" aria-label={`Organizar bloco ${block.title}`}>
        <button type="button" className="secondary small" disabled={index === 0} onClick={() => onMove(block.key, -1)} aria-label={`Mover ${block.title} para cima`}>↑</button>
        <button type="button" className="secondary small" disabled={index === total - 1} onClick={() => onMove(block.key, 1)} aria-label={`Mover ${block.title} para baixo`}>↓</button>
        <button type="button" className="secondary small" onClick={() => onHide(block.key)}>Ocultar</button>
      </div>}
    </footer>
  </article>;
}

export function OperationalCockpit({ navigate, session: suppliedSession }) {
  const session = suppliedSession ?? readSession() ?? {};
  const definitions = useMemo(() => permittedCockpitDefinitions(session), [session]);
  const storage = globalThis.localStorage;
  const preferencesKey = useMemo(() => cockpitStorageKey(session, 'preferences'), [session]);
  const snapshotKey = useMemo(() => cockpitStorageKey(session, 'snapshot'), [session]);
  const [preferences, setPreferences] = useState(() => sanitizeCockpitPreferences(
    readCockpitStorage(storage, preferencesKey, defaultCockpitPreferences(definitions)),
    definitions
  ));
  const [previousSnapshot, setPreviousSnapshot] = useState(() => readCockpitStorage(storage, snapshotKey, { values: {} }));
  const [results, setResults] = useState({});
  const [updatedAt, setUpdatedAt] = useState(null);
  const [loading, setLoading] = useState(false);
  const [generalError, setGeneralError] = useState('');
  const [personalizing, setPersonalizing] = useState(false);
  const [clock, setClock] = useState(Date.now());

  useEffect(() => {
    const next = sanitizeCockpitPreferences(readCockpitStorage(storage, preferencesKey, defaultCockpitPreferences(definitions)), definitions);
    setPreferences(next);
    setPreviousSnapshot(readCockpitStorage(storage, snapshotKey, { values: {} }));
  }, [definitions, preferencesKey, snapshotKey, storage]);

  const load = useCallback(async () => {
    setLoading(true);
    setGeneralError('');
    setPreviousSnapshot(readCockpitStorage(storage, snapshotKey, { values: {} }));
    const loaderNames = [...new Set(definitions.map((definition) => definition.loader))];
    const settled = await Promise.allSettled(loaderNames.map((name) => LOADERS[name]()));
    const nextResults = Object.fromEntries(loaderNames.map((name, index) => {
      const result = settled[index];
      return [name, result.status === 'fulfilled'
        ? { status: 'fulfilled', payload: result.value }
        : { status: 'rejected', error: result.reason }];
    }));
    const timestamp = new Date().toISOString();
    setResults(nextResults);
    setUpdatedAt(timestamp);
    setClock(Date.now());
    if (settled.every((result) => result.status === 'rejected')) {
      setGeneralError('Nenhum bloco do cockpit pôde ser atualizado. Verifique a conexão, a sessão e as integrações do runtime.');
    }
    setLoading(false);
  }, [definitions, snapshotKey, storage]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    if (!preferences.refreshSeconds) return undefined;
    const interval = globalThis.setInterval(load, preferences.refreshSeconds * 1000);
    return () => globalThis.clearInterval(interval);
  }, [load, preferences.refreshSeconds]);

  useEffect(() => {
    const timer = globalThis.setInterval(() => setClock(Date.now()), 15000);
    return () => globalThis.clearInterval(timer);
  }, []);

  const rawBlocks = useMemo(() => definitions.map((definition) => {
    if (loading && !results[definition.loader]) {
      return { ...definition, state: 'loading', value: null, detail: 'Carregando dados autorizados...', distribution: [], updatedAt, trend: { direction: 'new', label: 'Aguardando leitura', favorable: null } };
    }
    return buildCockpitBlock(definition, results[definition.loader] ?? {}, previousSnapshot?.values?.[definition.key], updatedAt);
  }), [definitions, loading, results, previousSnapshot, updatedAt]);

  useEffect(() => {
    if (!updatedAt || loading || !rawBlocks.length) return;
    writeCockpitStorage(storage, snapshotKey, createCockpitSnapshot(rawBlocks, updatedAt));
  }, [rawBlocks, updatedAt, loading, snapshotKey, storage]);

  const orderedBlocks = useMemo(() => applyCockpitPreferences(rawBlocks, preferences), [rawBlocks, preferences]);
  const visibleBlocks = orderedBlocks.filter((block) => !preferences.hidden.includes(block.key));
  const hiddenBlocks = orderedBlocks.filter((block) => preferences.hidden.includes(block.key));
  const unavailable = rawBlocks.filter((block) => block.state === 'error').length;
  const attention = rawBlocks.reduce((total, block) => total + (Number(block.attention) || 0), 0);
  const stale = isCockpitStale(updatedAt, preferences.refreshSeconds || 60, clock);

  function updatePreferences(next) {
    const sanitized = sanitizeCockpitPreferences(next, definitions);
    setPreferences(sanitized);
    writeCockpitStorage(storage, preferencesKey, sanitized);
  }

  function moveBlock(key, direction) {
    updatePreferences(moveCockpitBlock(preferences, key, direction, definitions));
  }

  function toggleBlock(key) {
    updatePreferences(toggleCockpitBlock(preferences, key, definitions));
  }

  function resetPreferences() {
    updatePreferences(defaultCockpitPreferences(definitions));
  }

  return <div className="operational-cockpit">
    <PageHeader
      eyebrow="Cockpit operacional"
      title="O que precisa da sua atenção agora"
      description="Central de trabalho personalizada pelos seus papéis, com exceções, filas, operações, disponibilidade e integrações. Cada bloco falha e atualiza de forma independente."
      actions={<>
        <label className="cockpit-refresh-field">Atualização automática
          <select value={preferences.refreshSeconds} onChange={(event) => updatePreferences({ ...preferences, refreshSeconds: Number(event.target.value) })}>
            {COCKPIT_REFRESH_OPTIONS.map((seconds) => <option value={seconds} key={seconds}>{seconds === 0 ? 'Desativada' : `${seconds} s`}</option>)}
          </select>
        </label>
        <button type="button" className="secondary" onClick={() => setPersonalizing((current) => !current)} aria-pressed={personalizing}>{personalizing ? 'Concluir personalização' : 'Personalizar'}</button>
        <button type="button" disabled={loading} onClick={load}>{loading ? 'Atualizando...' : 'Atualizar agora'}</button>
      </>}
    />

    <Message type="error">{generalError}</Message>

    <section className={`cockpit-summary${stale ? ' stale' : ''}`} aria-label="Resumo da atualização do cockpit">
      <div><span>Prioridades detectadas</span><strong>{attention}</strong></div>
      <div><span>Blocos permitidos</span><strong>{definitions.length}</strong></div>
      <div><span>Fontes indisponíveis</span><strong>{unavailable}</strong></div>
      <div><span>Atualização</span><strong>{stale ? 'Dados desatualizados' : formatDateTime(updatedAt)}</strong></div>
    </section>

    {personalizing && <section className="cockpit-personalization" aria-label="Personalizar cockpit">
      <div><h2>Organização dos blocos</h2><p>Use as setas nos cartões para mudar a ordem. Preferências são salvas somente para este usuário.</p></div>
      <button type="button" className="secondary" onClick={resetPreferences}>Restaurar padrão do perfil</button>
      {hiddenBlocks.length > 0 && <div className="cockpit-hidden-blocks"><strong>Blocos ocultos</strong>{hiddenBlocks.map((block) => <button type="button" className="secondary small" key={block.key} onClick={() => toggleBlock(block.key)}>Mostrar {block.title}</button>)}</div>}
    </section>}

    {!visibleBlocks.length ? <section className="cockpit-empty">
      <h2>Nenhum bloco visível</h2>
      <p>Todos os blocos permitidos foram ocultados. Restaure o padrão ou reative um bloco na personalização.</p>
      <button type="button" onClick={resetPreferences}>Restaurar blocos</button>
    </section> : <section className="cockpit-grid" aria-label="Blocos operacionais">
      {visibleBlocks.map((block, index) => <CockpitCard
        key={block.key}
        block={block}
        index={index}
        total={visibleBlocks.length}
        personalizing={personalizing}
        onOpen={navigate}
        onMove={moveBlock}
        onHide={toggleBlock}
        onReload={load}
      />)}
    </section>}
  </div>;
}
