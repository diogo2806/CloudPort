import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError } from '../api.js';
import {
  DataTable,
  EmptyState,
  Loading,
  Message,
  MetricCard,
  PageHeader,
  Section,
  StatusBadge
} from '../components.jsx';
import {
  agruparEscalasPorBerco,
  calcularAlturaTimelineVertical,
  calcularJanelaTimeline,
  calcularPosicaoVerticalTimeline,
  construirSimulacao,
  gerarMarcadoresTimeline,
  normalizarEscalasLineUp,
  paraDateTimeLocal,
  somarHoras
} from '../vesselLineUp.js';
import { vesselLineUpApi } from '../vesselLineUpApi.js';

function formatarDataHora(valor) {
  if (!valor) return '—';
  const data = valor instanceof Date ? valor : new Date(valor);
  if (Number.isNaN(data.getTime())) return '—';
  return data.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
}

function formatarMarcador(valor) {
  return valor.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
}

function formatarAtraso(minutos) {
  if (minutos === null || minutos === undefined) return 'Sem realizado';
  if (minutos === 0) return 'No horário';
  const absoluto = Math.abs(minutos);
  const horas = Math.floor(absoluto / 60);
  const restante = absoluto % 60;
  const duracao = [horas ? `${horas}h` : '', restante ? `${restante}min` : ''].filter(Boolean).join(' ');
  return minutos > 0 ? `Atraso ${duracao}` : `Adiantado ${duracao}`;
}

function posicaoDoInstante(valor, janela) {
  const tempo = valor.getTime();
  const inicio = janela.inicio.getTime();
  const total = Math.max(janela.fim.getTime() - inicio, 1);
  return Math.max(0, Math.min(100, ((tempo - inicio) / total) * 100));
}

function calcularFaixaHorizontal(escala, totalFaixas) {
  const quantidade = Math.max(Number(totalFaixas) || 1, 1);
  const larguraFaixa = 88 / quantidade;
  return {
    left: `${6 + escala.faixa * larguraFaixa}%`,
    width: `${Math.max(larguraFaixa - 2, 6)}%`
  };
}

function VerticalBerthTimeline({ escalas, instante }) {
  const janela = useMemo(() => calcularJanelaTimeline(escalas), [escalas]);
  const marcadores = useMemo(() => gerarMarcadoresTimeline(janela), [janela]);
  const grupos = useMemo(() => agruparEscalasPorBerco(escalas), [escalas]);
  const altura = useMemo(() => calcularAlturaTimelineVertical(janela), [janela]);
  const simulacaoPercentual = posicaoDoInstante(instante, janela);

  if (!escalas.length) {
    return <EmptyState title="Nenhuma escala no line-up" description="Cadastre escalas com ETA, ETB e ETD para executar a simulação." />;
  }

  const colunas = `112px repeat(${grupos.length}, minmax(190px, 1fr))`;

  return <div className="lineup-vertical-scroll">
    <div className="lineup-vertical-board" style={{ gridTemplateColumns: colunas }}>
      <div className="lineup-vertical-corner"><strong>Horário</strong><span>Fluxo de cima para baixo</span></div>
      {grupos.map((grupo) => <header className="lineup-vertical-berth-header" key={grupo.berco}>
        <strong>{grupo.berco}</strong>
        <span>{grupo.itens.length} escala(s)</span>
      </header>)}

      <div className="lineup-vertical-time-axis" style={{ height: `${altura}px` }}>
        {marcadores.map((marcador) => <span key={marcador.percentual} style={{ top: `${marcador.percentual}%` }}>{formatarMarcador(marcador.data)}</span>)}
      </div>

      {grupos.map((grupo) => <section className="lineup-vertical-berth" key={grupo.berco} style={{ height: `${altura}px` }} aria-label={`Ocupação simulada do ${grupo.berco}`}>
        {marcadores.map((marcador) => <i className="lineup-vertical-grid-line" key={marcador.percentual} style={{ top: `${marcador.percentual}%` }} />)}
        <i className="lineup-vertical-simulation-cursor" style={{ top: `${simulacaoPercentual}%` }} title={`Simulação: ${formatarDataHora(instante)}`} />
        {grupo.itens.map((escala) => {
          const vertical = calcularPosicaoVerticalTimeline(escala, janela);
          const horizontal = calcularFaixaHorizontal(escala, grupo.totalFaixas);
          const etaPercentual = posicaoDoInstante(escala.eta, janela);
          const classes = [
            'lineup-vertical-ship',
            `lineup-phase-${escala.faseSimulada.toLowerCase()}`,
            escala.conflitoBerco ? 'lineup-conflict' : ''
          ].filter(Boolean).join(' ');
          return <div key={escala.chave}>
            <i className="lineup-vertical-eta-marker" style={{ top: `${etaPercentual}%`, left: horizontal.left }} title={`ETA ${escala.nomeNavio}: ${formatarDataHora(escala.eta)}`} />
            <article className={classes} style={{ ...vertical, ...horizontal }} title={`${escala.nomeNavio} · ETB ${formatarDataHora(escala.etb)} · ETD ${formatarDataHora(escala.etd)}`}>
              <strong>{escala.nomeNavio}</strong>
              <span>{escala.viagemEntrada || 'Sem viagem'}</span>
              <small>{formatarDataHora(escala.etb)} → {formatarDataHora(escala.etd)}</small>
              <footer><b>{escala.faseSimulada}</b>{escala.faseSimulada === 'OPERANDO' && <em>{escala.progressoOperacao}%</em>}</footer>
            </article>
          </div>;
        })}
      </section>)}
    </div>
    <footer className="lineup-legend">
      <span><i className="legend-dot prevista" /> Prevista</span>
      <span><i className="legend-dot inbound" /> Inbound</span>
      <span><i className="legend-dot operando" /> Operando</span>
      <span><i className="legend-dot partiu" /> Partiu</span>
      <span><i className="legend-dot eta" /> ETA</span>
      <span><i className="legend-dot simulacao" /> Horário simulado</span>
      <span><i className="legend-dot conflito" /> Conflito de berço</span>
    </footer>
  </div>;
}

export function VesselLineUpPage() {
  const [dias, setDias] = useState(30);
  const [dados, setDados] = useState([]);
  const [instante, setInstante] = useState(() => new Date());
  const [executando, setExecutando] = useState(false);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErro('');
    try {
      setDados(await vesselLineUpApi.listar(dias));
    } catch (motivo) {
      setDados([]);
      setErro(formatError(motivo, 'Não foi possível carregar o line-up de navios.'));
    } finally {
      setCarregando(false);
    }
  }, [dias]);

  useEffect(() => { carregar(); }, [carregar]);

  useEffect(() => {
    if (!executando) return undefined;
    const intervalo = globalThis.setInterval(() => {
      setInstante((atual) => somarHoras(atual, 1));
    }, 1000);
    return () => globalThis.clearInterval(intervalo);
  }, [executando]);

  const escalas = useMemo(() => normalizarEscalasLineUp(dados), [dados]);
  const simuladas = useMemo(() => construirSimulacao(escalas, instante), [escalas, instante]);
  const operando = simuladas.filter((escala) => escala.faseSimulada === 'OPERANDO').length;
  const inbound = simuladas.filter((escala) => escala.faseSimulada === 'INBOUND').length;
  const conflitos = simuladas.filter((escala) => escala.conflitoBerco).length;
  const bercoOcupados = new Set(simuladas.filter((escala) => escala.faseSimulada === 'OPERANDO').map((escala) => escala.berco)).size;

  return <>
    <PageHeader eyebrow="Navio" title="Line-up operacional" description="Simule verticalmente a ocupação dos berços, da chegada à partida, com detecção de conflitos entre navios." actions={<button className="secondary" type="button" onClick={carregar} disabled={carregando}>Atualizar line-up</button>} />
    <Message type="error">{erro}</Message>

    <Section title="Controles da simulação" description="No modo automático, cada segundo avança uma hora operacional.">
      <div className="lineup-controls">
        <label className="compact-field">Janela
          <select value={dias} onChange={(event) => setDias(Number(event.target.value))}>
            <option value="7">7 dias</option><option value="15">15 dias</option><option value="30">30 dias</option><option value="60">60 dias</option>
          </select>
        </label>
        <label className="compact-field">Data e hora simulada
          <input type="datetime-local" value={paraDateTimeLocal(instante)} onChange={(event) => {
            const novaData = new Date(event.target.value);
            if (!Number.isNaN(novaData.getTime())) setInstante(novaData);
          }} />
        </label>
        <div className="lineup-control-buttons">
          <button className="secondary" type="button" onClick={() => setInstante((atual) => somarHoras(atual, -6))}>−6h</button>
          <button className="secondary" type="button" onClick={() => setInstante(new Date())}>Agora</button>
          <button className="secondary" type="button" onClick={() => setInstante((atual) => somarHoras(atual, 6))}>+6h</button>
          <button className="secondary" type="button" onClick={() => setInstante((atual) => somarHoras(atual, 24))}>+24h</button>
          <button type="button" onClick={() => setExecutando((valor) => !valor)}>{executando ? 'Pausar' : 'Executar simulação'}</button>
        </div>
      </div>
    </Section>

    {carregando ? <Loading label="Montando line-up operacional..." /> : <>
      <div className="metrics-grid">
        <MetricCard label="Escalas na janela" value={simuladas.length} />
        <MetricCard label="Navios inbound" value={inbound} />
        <MetricCard label="Navios operando" value={operando} detail={`${bercoOcupados} berço(s) ocupado(s)`} />
        <MetricCard label="Navios em conflito" value={conflitos} detail={conflitos ? 'Replanejamento necessário' : 'Sem sobreposição de berço'} />
      </div>
      <Section title="Ocupação vertical dos berços" description={`O tempo avança de cima para baixo. Cenário simulado em ${formatarDataHora(instante)}.`}><VerticalBerthTimeline escalas={simuladas} instante={instante} /></Section>
      <Section title="Detalhamento das escalas" description="Compare o estado registrado com o estado calculado pela simulação.">
        <DataTable rows={simuladas} rowKey="chave" emptyTitle="Nenhuma escala disponível" columns={[
          { key: 'nomeNavio', label: 'Navio', render: (linha) => <div className="lineup-table-vessel"><strong>{linha.nomeNavio}</strong><span>{linha.codigoImo || 'IMO não informado'} · {linha.viagemEntrada || 'Sem viagem'}</span></div> },
          { key: 'empresaArmadora', label: 'Armador' },
          { key: 'berco', label: 'Berço' },
          { key: 'eta', label: 'ETA', render: (linha) => formatarDataHora(linha.eta) },
          { key: 'etb', label: 'ETB', render: (linha) => formatarDataHora(linha.etb) },
          { key: 'etd', label: 'ETD', render: (linha) => formatarDataHora(linha.etd) },
          { key: 'fase', label: 'Fase atual', render: (linha) => <StatusBadge value={linha.fase} /> },
          { key: 'faseSimulada', label: 'Fase simulada', render: (linha) => <StatusBadge value={linha.faseSimulada} /> },
          { key: 'progressoOperacao', label: 'Operação', render: (linha) => linha.faseSimulada === 'OPERANDO' ? `${linha.progressoOperacao}%` : '—' },
          { key: 'alertas', label: 'Alertas', render: (linha) => <div className="lineup-alerts">{linha.conflitoBerco && <span className="lineup-alert danger">Conflito de berço</span>}<span className={`lineup-alert ${linha.atrasoChegadaMinutos > 0 ? 'warning' : ''}`}>{formatarAtraso(linha.atrasoChegadaMinutos)}</span></div> }
        ]} />
      </Section>
    </>}
  </>;
}
