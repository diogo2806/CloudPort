import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, formatError, normalizePage } from '../api.js';
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
  LINHAS_FERROVIARIAS,
  calcularJanelaFerroviaria,
  calcularPercentualInstante,
  calcularPosicaoVertical,
  construirOcupacoesFerroviarias,
  construirSimulacaoFerroviaria,
  gerarMarcadoresFerroviarios,
  normalizarVisitasFerrovia,
  paraDateTimeLocalFerroviario,
  somarHorasFerroviarias
} from '../railLineUp.js';

function formatarDataHora(valor) {
  if (!valor) return '—';
  const data = valor instanceof Date ? valor : new Date(valor);
  if (Number.isNaN(data.getTime())) return '—';
  return data.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function formatarHora(valor) {
  return valor.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

function classeEtapa(etapa) {
  return String(etapa || 'indefinida').toLowerCase();
}

function SegmentoFerroviario({ segmento, janela }) {
  const posicao = calcularPosicaoVertical(segmento, janela);
  const larguraFaixa = 100 / Math.max(segmento.totalFaixas, 1);
  const left = segmento.faixa * larguraFaixa;
  const classes = [
    'rail-lineup-segment',
    `rail-lineup-segment-${classeEtapa(segmento.etapa)}`,
    segmento.conflito ? 'rail-lineup-segment-conflict' : ''
  ].filter(Boolean).join(' ');
  return <article
    className={classes}
    style={{
      ...posicao,
      left: `calc(${left}% + 5px)`,
      width: `calc(${larguraFaixa}% - 10px)`
    }}
    title={`${segmento.visita.identificadorTrem} · ${segmento.descricao} · ${formatarDataHora(segmento.inicio)} até ${formatarDataHora(segmento.fim)}`}
  >
    <strong>{segmento.visita.identificadorTrem}</strong>
    <span>{segmento.descricao}</span>
    <small>{formatarHora(segmento.inicio)}–{formatarHora(segmento.fim)} · {segmento.visita.quantidadeVagoes} vagão(ões)</small>
  </article>;
}

function TimelineFerroviaria({ visitas, segmentos, instante }) {
  const janela = useMemo(() => calcularJanelaFerroviaria(visitas), [visitas]);
  const marcadores = useMemo(() => gerarMarcadoresFerroviarios(janela), [janela]);
  const cursor = calcularPercentualInstante(instante, janela);
  const duracaoHoras = (janela.fim.getTime() - janela.inicio.getTime()) / (60 * 60 * 1000);
  const altura = Math.max(760, Math.min(1900, Math.round(duracaoHoras * 18)));

  if (!visitas.length) {
    return <EmptyState title="Nenhuma visita no line-up" description="Cadastre visitas ferroviárias com chegada e partida previstas para executar a simulação." />;
  }

  return <div className="rail-lineup-scroll">
    <div
      className="rail-lineup-board"
      style={{ gridTemplateColumns: `96px repeat(${LINHAS_FERROVIARIAS.length}, minmax(172px, 1fr))` }}
    >
      <div className="rail-lineup-corner">Horário</div>
      {LINHAS_FERROVIARIAS.map((linha) => {
        const quantidade = segmentos.filter((segmento) => segmento.linha === linha).length;
        return <header className="rail-lineup-line-header" key={linha}>
          <strong>{linha}</strong>
          <span>{quantidade} ocupação(ões)</span>
        </header>;
      })}

      <div className="rail-lineup-time-axis" style={{ height: `${altura}px` }}>
        {marcadores.map((marcador) => <span key={marcador.percentual} style={{ top: `${marcador.percentual}%` }}>
          {formatarDataHora(marcador.data)}
        </span>)}
      </div>

      {LINHAS_FERROVIARIAS.map((linha) => <div className="rail-lineup-line" key={linha} style={{ height: `${altura}px` }}>
        {marcadores.map((marcador) => <i className="rail-lineup-grid-line" key={marcador.percentual} style={{ top: `${marcador.percentual}%` }} />)}
        <i className="rail-lineup-cursor" style={{ top: `${cursor}%` }} title={`Horário simulado: ${formatarDataHora(instante)}`} />
        {segmentos.filter((segmento) => segmento.linha === linha)
          .map((segmento) => <SegmentoFerroviario key={segmento.chave} segmento={segmento} janela={janela} />)}
      </div>)}
    </div>
  </div>;
}

export function RailLineUpPage() {
  const [dias, setDias] = useState(7);
  const [dados, setDados] = useState([]);
  const [instante, setInstante] = useState(() => new Date());
  const [executando, setExecutando] = useState(false);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErro('');
    try {
      const resposta = await api.listarVisitasFerrovia(dias);
      setDados(normalizePage(resposta));
    } catch (motivo) {
      setDados([]);
      setErro(formatError(motivo, 'Não foi possível carregar o line-up ferroviário.'));
    } finally {
      setCarregando(false);
    }
  }, [dias]);

  useEffect(() => { carregar(); }, [carregar]);

  useEffect(() => {
    if (!executando) return undefined;
    const intervalo = globalThis.setInterval(() => {
      setInstante((atual) => somarHorasFerroviarias(atual, 1));
    }, 1000);
    return () => globalThis.clearInterval(intervalo);
  }, [executando]);

  const visitas = useMemo(() => normalizarVisitasFerrovia(dados), [dados]);
  const segmentos = useMemo(() => construirOcupacoesFerroviarias(visitas), [visitas]);
  const simuladas = useMemo(() => construirSimulacaoFerroviaria(visitas, segmentos, instante), [visitas, segmentos, instante]);
  const tempo = instante.getTime();
  const ocupacoesAtuais = segmentos.filter((segmento) => tempo >= segmento.inicio.getTime() && tempo < segmento.fim.getTime());
  const linhasOcupadas = new Set(ocupacoesAtuais.map((segmento) => segmento.linha)).size;
  const emTerminal = simuladas.filter((visita) => tempo >= visita.chegada.getTime() && tempo < visita.partida.getTime()).length;
  const operando = simuladas.filter((visita) => ['OPERANDO', 'EM_APOIO'].includes(visita.faseSimulada)).length;
  const conflitos = simuladas.filter((visita) => visita.conflitoLinha).length;
  const vagoes = simuladas.reduce((total, visita) => total + visita.quantidadeVagoes, 0);

  return <>
    <PageHeader
      eyebrow="Ferrovia"
      title="Line-up ferroviário"
      description="Simule verticalmente a recepção, ocupação das linhas, operação e expedição dos trens no terminal."
      actions={<button className="secondary" type="button" onClick={carregar} disabled={carregando}>Atualizar line-up</button>}
    />
    <Message type="error">{erro}</Message>

    <Section title="Controles da simulação" description="No modo automático, cada segundo avança uma hora operacional.">
      <div className="rail-lineup-controls">
        <label className="compact-field">Janela
          <select value={dias} onChange={(event) => setDias(Number(event.target.value))}>
            <option value="1">1 dia</option>
            <option value="7">7 dias</option>
            <option value="15">15 dias</option>
            <option value="30">30 dias</option>
          </select>
        </label>
        <label className="compact-field">Data e hora simulada
          <input type="datetime-local" value={paraDateTimeLocalFerroviario(instante)} onChange={(event) => {
            const novaData = new Date(event.target.value);
            if (!Number.isNaN(novaData.getTime())) setInstante(novaData);
          }} />
        </label>
        <div className="rail-lineup-control-buttons">
          <button className="secondary" type="button" onClick={() => setInstante((atual) => somarHorasFerroviarias(atual, -6))}>−6h</button>
          <button className="secondary" type="button" onClick={() => setInstante(new Date())}>Agora</button>
          <button className="secondary" type="button" onClick={() => setInstante((atual) => somarHorasFerroviarias(atual, 6))}>+6h</button>
          <button className="secondary" type="button" onClick={() => setInstante((atual) => somarHorasFerroviarias(atual, 24))}>+24h</button>
          <button type="button" onClick={() => setExecutando((valor) => !valor)}>{executando ? 'Pausar' : 'Executar simulação'}</button>
        </div>
      </div>
    </Section>

    {carregando ? <Loading label="Montando line-up ferroviário..." /> : <>
      <div className="metrics-grid">
        <MetricCard label="Visitas na janela" value={simuladas.length} detail={`${vagoes} vagão(ões) planejados`} />
        <MetricCard label="Trens no terminal" value={emTerminal} />
        <MetricCard label="Em operação" value={operando} detail={`${linhasOcupadas} linha(s) ocupada(s)`} />
        <MetricCard label="Trens em conflito" value={conflitos} detail={conflitos ? 'Replanejamento necessário' : 'Sem sobreposição de linha'} />
      </div>

      <Section title="Ocupação vertical das linhas" description={`Cenário simulado em ${formatarDataHora(instante)}. O tempo avança de cima para baixo.`}>
        <TimelineFerroviaria visitas={visitas} segmentos={segmentos} instante={instante} />
        <footer className="rail-lineup-legend">
          <span><i className="rail-lineup-legend-dot recepcao" /> Recepção</span>
          <span><i className="rail-lineup-legend-dot operacao" /> Operação</span>
          <span><i className="rail-lineup-legend-dot expedicao" /> Expedição</span>
          <span><i className="rail-lineup-legend-dot conflito" /> Conflito de linha</span>
        </footer>
      </Section>

      <Section title="Detalhamento das visitas" description="Compare a situação registrada com a fase e a linha calculadas pela simulação.">
        <DataTable rows={simuladas} rowKey="chave" emptyTitle="Nenhuma visita ferroviária disponível" columns={[
          { key: 'identificadorTrem', label: 'Trem', render: (linha) => <div className="rail-lineup-table-train"><strong>{linha.identificadorTrem}</strong><span>{linha.operadoraFerroviaria}</span></div> },
          { key: 'tipoVisita', label: 'Tipo', render: (linha) => <StatusBadge value={linha.tipoVisita} /> },
          { key: 'statusVisita', label: 'Status registrado', render: (linha) => <StatusBadge value={linha.statusVisita} /> },
          { key: 'faseSimulada', label: 'Fase simulada', render: (linha) => <StatusBadge value={linha.faseSimulada} /> },
          { key: 'linhaAtual', label: 'Linha atual' },
          { key: 'chegada', label: 'Chegada', render: (linha) => formatarDataHora(linha.chegada) },
          { key: 'partida', label: 'Partida', render: (linha) => formatarDataHora(linha.partida) },
          { key: 'quantidadeVagoes', label: 'Vagões' },
          { key: 'operacoes', label: 'Operações', render: (linha) => `${linha.quantidadeDescarga} descarga · ${linha.quantidadeCarga} carga` },
          { key: 'progressoOperacao', label: 'Progresso', render: (linha) => `${linha.progressoOperacao}%` },
          { key: 'alerta', label: 'Alerta', render: (linha) => linha.conflitoLinha ? <span className="rail-lineup-alert">Conflito de linha</span> : '—' }
        ]} />
      </Section>
    </>}
  </>;
}
