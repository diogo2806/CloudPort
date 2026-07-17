import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError } from '../api.js';
import {
  filterLineUp,
  formatLineUpDate,
  lineUpStatusLabel,
  normalizeLineUp,
  scheduleMoment
} from '../publicLineUp.js';
import { listarLineUpPublico } from '../publicLineUpApi.js';

const REFRESH_INTERVAL_MS = 60_000;
const PHASES = ['PREVISTA', 'INBOUND', 'ATRACADO', 'OPERANDO', 'PARTIU'];

function ScheduleCell({ actual, expected }) {
  const moment = scheduleMoment(actual, expected);
  return <div className="lineup-schedule-cell">
    <strong>{formatLineUpDate(moment.value)}</strong>
    <small className={moment.actual ? 'actual' : ''}>{moment.actual ? 'Realizado' : 'Previsto'}</small>
  </div>;
}

function VesselCard({ item }) {
  return <article className="lineup-card">
    <div className="lineup-card-title">
      <div><span className="eyebrow">{item.codigoImo ? `IMO ${item.codigoImo}` : 'IMO a confirmar'}</span><h2>{item.nomeNavio || 'Navio a confirmar'}</h2></div>
      <span className={`lineup-status status-${item.fase.toLowerCase()}`}>{lineUpStatusLabel(item.fase)}</span>
    </div>
    <dl className="lineup-card-grid">
      <div><dt>Viagem</dt><dd>{item.viagemEntrada || item.viagemSaida || 'A confirmar'}</dd></div>
      <div><dt>Berço</dt><dd>{item.berco || 'A confirmar'}</dd></div>
      <div><dt>Chegada</dt><dd>{formatLineUpDate(scheduleMoment(item.chegadaEfetiva, item.chegadaPrevista).value)}</dd></div>
      <div><dt>Atracação</dt><dd>{formatLineUpDate(scheduleMoment(item.atracacaoEfetiva, item.atracacaoPrevista).value)}</dd></div>
      <div><dt>Partida</dt><dd>{formatLineUpDate(scheduleMoment(item.partidaEfetiva, item.partidaPrevista).value)}</dd></div>
    </dl>
  </article>;
}

export function PublicVesselLineUpPage({ navigate, authenticated = false }) {
  const [days, setDays] = useState(30);
  const [items, setItems] = useState([]);
  const [search, setSearch] = useState('');
  const [phase, setPhase] = useState('');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [updatedAt, setUpdatedAt] = useState(null);

  const load = useCallback(async (silent = false) => {
    if (silent) setRefreshing(true);
    else setLoading(true);
    setError('');
    try {
      const response = await listarLineUpPublico(days);
      setItems(normalizeLineUp(response));
      setUpdatedAt(new Date());
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível consultar o line-up de navios.'));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [days]);

  useEffect(() => {
    let active = true;
    async function initialLoad() {
      setLoading(true);
      setError('');
      try {
        const response = await listarLineUpPublico(days);
        if (active) {
          setItems(normalizeLineUp(response));
          setUpdatedAt(new Date());
        }
      } catch (reason) {
        if (active) setError(formatError(reason, 'Não foi possível consultar o line-up de navios.'));
      } finally {
        if (active) setLoading(false);
      }
    }
    initialLoad();
    const interval = globalThis.setInterval(() => {
      if (active) load(true);
    }, REFRESH_INTERVAL_MS);
    return () => {
      active = false;
      globalThis.clearInterval(interval);
    };
  }, [days, load]);

  const filtered = useMemo(() => filterLineUp(items, search, phase), [items, search, phase]);
  const metrics = useMemo(() => ({
    total: items.length,
    approaching: items.filter((item) => item.fase === 'INBOUND').length,
    berthed: items.filter((item) => item.fase === 'ATRACADO' || item.fase === 'OPERANDO').length
  }), [items]);

  return <main className="public-lineup-page">
    <header className="public-lineup-header">
      <button className="public-lineup-brand" type="button" onClick={() => navigate('/line-up')} aria-label="Página inicial do line-up">
        <span className="brand-mark small">CP</span>
        <span><strong>CloudPort</strong><small>Line-up de navios</small></span>
      </button>
      <button className="secondary" type="button" onClick={() => navigate(authenticated ? '/home/dashboard' : '/login')}>
        {authenticated ? 'Voltar ao portal' : 'Área do cliente'}
      </button>
    </header>

    <section className="public-lineup-hero">
      <div>
        <span className="eyebrow">Programação pública</span>
        <h1>Line-up de navios</h1>
        <p>Acompanhe chegadas, atracações, operações e partidas programadas no terminal.</p>
      </div>
      <div className="public-lineup-update">
        <span>Última atualização</span>
        <strong>{updatedAt ? formatLineUpDate(updatedAt) : 'Aguardando consulta'}</strong>
        <small>Atualização automática a cada minuto</small>
      </div>
    </section>

    <section className="public-lineup-metrics" aria-label="Resumo do line-up">
      <article><span>Escalas publicadas</span><strong>{metrics.total}</strong></article>
      <article><span>Em aproximação</span><strong>{metrics.approaching}</strong></article>
      <article><span>Atracados ou operando</span><strong>{metrics.berthed}</strong></article>
    </section>

    <section className="public-lineup-content">
      <div className="public-lineup-toolbar">
        <label className="field lineup-search"><span>Buscar</span><input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Navio, IMO, viagem ou berço" /></label>
        <label className="field"><span>Situação</span><select value={phase} onChange={(event) => setPhase(event.target.value)}><option value="">Todas</option>{PHASES.map((value) => <option key={value} value={value}>{lineUpStatusLabel(value)}</option>)}</select></label>
        <label className="field"><span>Período</span><select value={days} onChange={(event) => setDays(Number(event.target.value))}><option value={7}>7 dias</option><option value={15}>15 dias</option><option value={30}>30 dias</option><option value={60}>60 dias</option></select></label>
        <button type="button" onClick={() => load(true)} disabled={loading || refreshing}>{refreshing ? 'Atualizando...' : 'Atualizar'}</button>
      </div>

      {error && <div className="message error" role="alert"><span>{error}</span><button className="small danger" type="button" onClick={() => load(false)}>Tentar novamente</button></div>}
      {loading && <div className="loading"><span className="spinner" />Consultando programação...</div>}
      {!loading && !error && filtered.length === 0 && <div className="empty-state"><strong>Nenhuma escala encontrada</strong><span>Altere os filtros ou consulte outro período.</span></div>}

      {!loading && !error && filtered.length > 0 && <>
        <div className="lineup-table-wrap">
          <table className="lineup-table">
            <thead><tr><th>Navio</th><th>Viagem</th><th>Chegada</th><th>Atracação</th><th>Partida</th><th>Berço</th><th>Situação</th></tr></thead>
            <tbody>{filtered.map((item, index) => <tr key={`${item.codigoImo}-${item.viagemEntrada}-${index}`}>
              <td><div className="lineup-vessel"><strong>{item.nomeNavio || 'A confirmar'}</strong><small>{item.codigoImo ? `IMO ${item.codigoImo}` : 'IMO a confirmar'}</small></div></td>
              <td>{item.viagemEntrada || item.viagemSaida || 'A confirmar'}</td>
              <td><ScheduleCell actual={item.chegadaEfetiva} expected={item.chegadaPrevista} /></td>
              <td><ScheduleCell actual={item.atracacaoEfetiva} expected={item.atracacaoPrevista} /></td>
              <td><ScheduleCell actual={item.partidaEfetiva} expected={item.partidaPrevista} /></td>
              <td>{item.berco || 'A confirmar'}</td>
              <td><span className={`lineup-status status-${item.fase.toLowerCase()}`}>{lineUpStatusLabel(item.fase)}</span></td>
            </tr>)}</tbody>
          </table>
        </div>
        <div className="lineup-card-list">{filtered.map((item, index) => <VesselCard key={`${item.codigoImo}-${item.viagemEntrada}-card-${index}`} item={item} />)}</div>
      </>}
    </section>

    <footer className="public-lineup-footer">
      <p>Horários sujeitos a alterações operacionais, meteorológicas, marítimas e de autoridade portuária.</p>
      <p>Dados publicados pelo CloudPort a partir do cronograma operacional do terminal.</p>
    </footer>
  </main>;
}
