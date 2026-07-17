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
  calcularJanelaTimeline,
  calcularPosicaoTimeline,
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

function Timeline({ escalas, instante }) {
  const janela = useMemo(() => calcularJanelaTimeline(escalas), [escalas]);
  const marcadores = useMemo(() => gerarMarcadoresTimeline(janela), [janela]);
  const grupos = useMemo(() => agruparEscalasPorBerco(escalas), [escalas]);
  const simulacaoPercentual = posicaoDoInstante(instante, janela);

  if (!escalas.length) {
    return <EmptyState title="Nenhuma escala no line-up" description="Cadastre escalas com ETA, ETB e ETD para executar a simulação." />;
  }

  return <div className="lineup-timeline">
    <div className="lineup-scale-row">
      <div className="lineup-scale-label">Berço e navio</div>
      <div className="lineup-track lineup-scale">
        {marcadores.map((marcador) => <span key={marcador.percentual} style={{ left: `${marcador.percentual}%` }}>{formatarMarcador(marcador.data)}</span>)}
      </div>
    </div>
    {grupos.map((grupo) => <section className="lineup-berth-group" key={grupo.berco}>
      <header><strong>{grupo.berco}</strong><span>{grupo.itens.length} escala(s)</span></header>
      {grupo.itens.map((escala) => {
        const posicao = calcularPosicaoTimeline(escala, janela);
        const etaPercentual = posicaoDoInstante(escala.eta, janela);
        const classes = [
          'lineup-operation-bar',
          `lineup-phase-${escala.faseSimulada.toLowerCase()}`,
          escala.conflitoBerco ? 'lineup-conflict' : ''
        ].filter(Boolean).join(' ');
        return <div className="lineup-row" key={escala.chave}>
          <div className="lineup-vessel-label">
            <strong>{escala.nomeNavio}</strong>
            <span>{escala.viagemEntrada || 'Viagem não informada'} · {escala.empresaArmadora || 'Armador não informado'}</span>
          </div>
          <div className="lineup-track">
            {marcadores.map((marcador) => <i className="lineup-grid-line" key={marcador.percentual} style={{ left: `${marcador.percentual}%` }} />)}
            <i className="lineup-simulation-cursor" style={{ left: `${simulacaoPercentual}%` }} title={`Simulação: ${formatarDataHora(instante)}`} />
            <i className="lineup-eta-marker" style={{ left: `${etaPercentual}%` }} title={`ETA: ${formatarDataHora(escala.eta)}`} />
            <div
              className={classes}
              style={posicao}
              title={`${escala.nomeNavio} · ETB ${formatarDataHora(escala.etb)} · ETD ${formatarDataHora(escala.etd)}`}
            >
              <span>{escala.faseSimulada}</span>
              {escala.faseSimulada === 'OPERANDO' && <small>{escala.progressoOperacao}%</small>}
            </div>
          </div>
        </div>;
      })}
    </section>)}
    <footer className="lineup-legend">
      <span><i className="legend-dot prevista" /> Prevista</span>
      <span><i className="legend-dot inbound" /> Inbound</span>
      <span><i className="legend-dot operando" /> Operando</span>
      <span><i className="legend-dot partiu" /> Partiu</span>
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
    <PageHeader
      eyebrow="Navio"
      title="Line-up operacional"
      description="Simule a sequência de chegada, atracação, operação e partida dos navios, com detecção de conflito por berço."
      actions={<button className="secondary" type="button" onClick={carregar} disabled={carregando}>Atualizar line-up</button>}
    />
    <Message type="error">{erro}</Message>

    <Section title="Controles da simulação" description="No modo automático, cada segundo avança uma hora operacional.">
      <div className="lineup-controls">
        <label className="compact-field">Janela
          <select value={dias} onChange={(event) => setDias(Number(event.target.value))}>
            <option value="7">7 dias</option>
            <option value="15">15 dias</option>
            <option value="30">30 dias</option>
            <option value="60">60 dias</option>
          </select>
        </label>
        <label className="compact-field">Data e hora simulada
          <input
            type="datetime-local"
            value={paraDateTimeLocal(instante)}
            onChange={(event) => {
              const novaData = new Date(event.target.value);
              if (!Number.isNaN(novaData.getTime())) setInstante(novaData);
            }}
          />
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

      <Section title="Linha do tempo por berço" description={`Cenário simulado em ${formatarDataHora(instante)}.`}>
        <Timeline escalas={simuladas} instante={instante} />
      </Section>

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
