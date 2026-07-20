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
import { railApi } from '../railApi.js';
import { RailOperationsPanels } from './RailOperationsPanels.jsx';
import {
  LINHAS_FERROVIARIAS,
  LINHAS_PLANEJAMENTO,
  calcularJanelaFerroviaria,
  calcularPercentualInstante,
  calcularPosicaoVertical,
  construirComposicaoFerroviaria,
  construirOcupacaoLinhasComposicao,
  construirOcupacoesFerroviarias,
  construirSimulacaoFerroviaria,
  criarPlanejamentoVagoes,
  gerarMarcadoresFerroviarios,
  moverVagaoNoPlanejamento,
  normalizarVisitasFerrovia,
  paraDateTimeLocalFerroviario,
  somarHorasFerroviarias
} from '../railLineUp.js';

const TIPO_DADO_CONTEINER = 'application/x-cloudport-container';

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

function operacaoConcluida(status) {
  return ['CONCLUIDO', 'CONCLUIDA'].includes(String(status || '').toUpperCase());
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

function BarraProgressoVagao({ valor }) {
  const progresso = Math.max(0, Math.min(100, Number(valor) || 0));
  return <div className="rail-wagon-progress" aria-label={`Progresso operacional ${progresso}%`}>
    <span style={{ width: `${progresso}%` }} />
  </div>;
}

function VagaoComposicao({ vagao, linha, bloqueado, onMover, onAlternarBloqueio, onSolicitarReplanejamento }) {
  const indisponivel = bloqueado || vagao.bloqueadoOrigem;
  const conteineres = vagao.operacoes.slice(0, 4);
  const capacidade = Number(vagao.capacidadeConteineres) || 2;
  return <article
    className={`rail-wagon-card${indisponivel ? ' blocked' : ''}${vagao.incompativel ? ' incompatible' : ''}`}
    draggable={!indisponivel}
    onDragStart={(event) => {
      if (event.target !== event.currentTarget) return;
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/plain', vagao.identificadorVagao);
    }}
    onDragOver={(event) => {
      if (!indisponivel && Array.from(event.dataTransfer.types || []).includes(TIPO_DADO_CONTEINER)) {
        event.preventDefault();
        event.dataTransfer.dropEffect = 'move';
      }
    }}
    onDrop={(event) => {
      const conteudo = event.dataTransfer.getData(TIPO_DADO_CONTEINER);
      if (!conteudo || indisponivel) return;
      event.preventDefault();
      event.stopPropagation();
      try {
        const movimento = JSON.parse(conteudo);
        if (movimento.vagaoOrigem !== vagao.identificadorVagao) {
          onSolicitarReplanejamento({ ...movimento, vagaoDestino: vagao.identificadorVagao });
        }
      } catch {
        // Dados externos de drag-and-drop são ignorados.
      }
    }}
    title={indisponivel ? 'Vagão bloqueado para replanejamento' : 'Arraste o vagão para outra linha ou mova um contêiner entre vagões'}
  >
    <div className="rail-wagon-topline">
      <span>#{vagao.posicaoNoTrem}</span>
      <strong>{vagao.identificadorVagao}</strong>
      <small>{vagao.tipoVagao}</small>
    </div>
    <div className="rail-wagon-containers">
      {conteineres.map((operacao) => {
        const concluida = operacaoConcluida(operacao.statusOperacao);
        return <span
          key={`${operacao.tipoOperacao}-${operacao.codigoConteiner}`}
          className={String(operacao.statusOperacao).toLowerCase()}
          draggable={!indisponivel && !concluida}
          onDragStart={(event) => {
            event.stopPropagation();
            event.dataTransfer.effectAllowed = 'move';
            event.dataTransfer.setData(TIPO_DADO_CONTEINER, JSON.stringify({
              codigoConteiner: operacao.codigoConteiner,
              tipoOperacao: operacao.tipoOperacao,
              vagaoOrigem: vagao.identificadorVagao
            }));
          }}
          title={concluida ? 'Operação concluída' : 'Arraste para outro vagão'}
        >
          {operacao.codigoConteiner}
        </span>;
      })}
      {vagao.operacoes.length > conteineres.length && <span>+{vagao.operacoes.length - conteineres.length}</span>}
      {!vagao.operacoes.length && <em>Sem contêiner associado</em>}
    </div>
    <div className="rail-wagon-stats">
      <span>{vagao.quantidadeDescarga} descarga</span>
      <span>{vagao.quantidadeCarga} carga</span>
      <span>{vagao.operacoes.length}/{capacidade} ocupação</span>
      <strong>{vagao.progresso}%</strong>
    </div>
    <BarraProgressoVagao valor={vagao.progresso} />
    <label className="rail-wagon-line-select">Linha
      <select value={linha ?? ''} disabled={indisponivel} onChange={(event) => onMover(vagao.identificadorVagao, event.target.value)}>
        {LINHAS_PLANEJAMENTO.map((item) => <option key={item} value={item}>{item}</option>)}
      </select>
    </label>
    <div className="rail-wagon-flags">
      {indisponivel && <span className="blocked">Bloqueado</span>}
      {vagao.incompativel && <span className="incompatible">Incompatível: {vagao.motivoIncompatibilidade}</span>}
    </div>
    {!vagao.bloqueadoOrigem && <button className="secondary small" type="button" onClick={() => onAlternarBloqueio(vagao.identificadorVagao)}>
      {bloqueado ? 'Desbloquear' : 'Bloquear'}
    </button>}
  </article>;
}

function ComposicaoVisual({ composicao, planejamento, bloqueios, onMover, onAlternarBloqueio, onSolicitarReplanejamento }) {
  if (!composicao.locomotiva) return null;
  return <div className="rail-composition-scroll">
    <div className="rail-composition-sequence">
      <article className="rail-locomotive-card">
        <span className="rail-locomotive-cab" aria-hidden="true" />
        <div><strong>{composicao.locomotiva.identificador}</strong><small>{composicao.locomotiva.operadora}</small></div>
      </article>
      {composicao.vagoes.map((vagao) => <VagaoComposicao
        key={vagao.identificadorVagao}
        vagao={vagao}
        linha={planejamento[vagao.identificadorVagao]}
        bloqueado={Boolean(bloqueios[vagao.identificadorVagao])}
        onMover={onMover}
        onAlternarBloqueio={onAlternarBloqueio}
        onSolicitarReplanejamento={onSolicitarReplanejamento}
      />)}
      {!composicao.vagoes.length && !composicao.locomotiva.isolada && <EmptyState title="Composição sem vagões" description="A visita selecionada não possui vagões cadastrados." />}
    </div>
  </div>;
}

function PatioFerroviario({ ocupacoes, bloqueios, onMover }) {
  return <div className="rail-track-yard">
    {ocupacoes.map((ocupacao) => <section
      className="rail-track-row"
      key={ocupacao.linha}
      onDragOver={(event) => {
        if (!Array.from(event.dataTransfer.types || []).includes('text/plain')) return;
        event.preventDefault();
        event.dataTransfer.dropEffect = 'move';
      }}
      onDrop={(event) => {
        event.preventDefault();
        const identificador = event.dataTransfer.getData('text/plain');
        if (identificador) onMover(identificador, ocupacao.linha);
      }}
    >
      <header><strong>{ocupacao.linha}</strong><span>{ocupacao.vagoes.length} vagão(ões)</span></header>
      <div className="rail-track-bed">
        <div className="rail-track-vehicles">
          {ocupacao.vagoes.map((vagao) => <span
            className={`rail-track-wagon${bloqueios[vagao.identificadorVagao] || vagao.bloqueadoOrigem ? ' blocked' : ''}${vagao.incompativel ? ' incompatible' : ''}`}
            draggable={!bloqueios[vagao.identificadorVagao] && !vagao.bloqueadoOrigem}
            key={vagao.identificadorVagao}
            onDragStart={(event) => {
              event.dataTransfer.effectAllowed = 'move';
              event.dataTransfer.setData('text/plain', vagao.identificadorVagao);
            }}
            title={`${vagao.identificadorVagao} · ${vagao.progresso}% concluído`}
          >
            <strong>{vagao.identificadorVagao}</strong>
            <small>{vagao.operacoes.length} contêiner(es)</small>
          </span>)}
          {!ocupacao.vagoes.length && <em>Solte vagões nesta linha</em>}
        </div>
      </div>
    </section>)}
  </div>;
}

export function RailLineUpPage() {
  const [dias, setDias] = useState(7);
  const [dados, setDados] = useState([]);
  const [instante, setInstante] = useState(() => new Date());
  const [executando, setExecutando] = useState(false);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [visitaSelecionadaId, setVisitaSelecionadaId] = useState('');
  const [detalheVisita, setDetalheVisita] = useState(null);
  const [carregandoDetalhe, setCarregandoDetalhe] = useState(false);
  const [erroDetalhe, setErroDetalhe] = useState('');
  const [planejamento, setPlanejamento] = useState({});
  const [bloqueios, setBloqueios] = useState({});
  const [replanejamentoPendente, setReplanejamentoPendente] = useState(null);
  const [motivoReplanejamento, setMotivoReplanejamento] = useState('');
  const [salvandoReplanejamento, setSalvandoReplanejamento] = useState(false);
  const [mensagemReplanejamento, setMensagemReplanejamento] = useState('');

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
  const simuladas = useMemo(
    () => construirSimulacaoFerroviaria(visitas, segmentos, instante),
    [visitas, segmentos, instante]
  );
  const composicao = useMemo(() => construirComposicaoFerroviaria(detalheVisita), [detalheVisita]);
  const ocupacoesComposicao = useMemo(
    () => construirOcupacaoLinhasComposicao(composicao, planejamento),
    [composicao, planejamento]
  );

  useEffect(() => {
    setVisitaSelecionadaId((atual) => {
      if (visitas.some((visita) => String(visita.id) === String(atual))) return atual;
      const preferida = visitas.find((visita) => ['CHEGOU', 'PROCESSANDO', 'CONCLUIDO'].includes(visita.statusVisita)) ?? visitas[0];
      return preferida?.id ? String(preferida.id) : '';
    });
  }, [visitas]);

  useEffect(() => {
    setReplanejamentoPendente(null);
    setMotivoReplanejamento('');
    setMensagemReplanejamento('');
    if (!visitaSelecionadaId) {
      setDetalheVisita(null);
      setPlanejamento({});
      setBloqueios({});
      return undefined;
    }
    let ativo = true;
    setCarregandoDetalhe(true);
    setErroDetalhe('');
    railApi.consultarVisita(visitaSelecionadaId)
      .then((detalhe) => {
        if (!ativo) return;
        const resumo = visitas.find((visita) => String(visita.id) === String(visitaSelecionadaId));
        const segmentoOperacao = segmentos.find((segmento) => segmento.visitaChave === resumo?.chave && segmento.etapa === 'OPERACAO');
        const novaComposicao = construirComposicaoFerroviaria(detalhe);
        setDetalheVisita(detalhe);
        setPlanejamento(criarPlanejamentoVagoes(detalhe, segmentoOperacao?.linha));
        setBloqueios(Object.fromEntries(novaComposicao.vagoes
          .filter((vagao) => vagao.bloqueadoOrigem)
          .map((vagao) => [vagao.identificadorVagao, true])));
      })
      .catch((motivo) => {
        if (!ativo) return;
        setDetalheVisita(null);
        setPlanejamento({});
        setBloqueios({});
        setErroDetalhe(formatError(motivo, 'Não foi possível carregar a composição ferroviária.'));
      })
      .finally(() => {
        if (ativo) setCarregandoDetalhe(false);
      });
    return () => { ativo = false; };
  }, [segmentos, visitaSelecionadaId, visitas]);

  const tempo = instante.getTime();
  const ocupacoesAtuais = segmentos.filter((segmento) => tempo >= segmento.inicio.getTime() && tempo < segmento.fim.getTime());
  const linhasOcupadas = new Set(ocupacoesAtuais.map((segmento) => segmento.linha)).size;
  const emTerminal = simuladas.filter((visita) => tempo >= visita.chegada.getTime() && tempo < visita.partida.getTime()).length;
  const operando = simuladas.filter((visita) => ['OPERANDO', 'EM_APOIO'].includes(visita.faseSimulada)).length;
  const conflitos = simuladas.filter((visita) => visita.conflitoLinha).length;
  const vagoes = simuladas.reduce((total, visita) => total + visita.quantidadeVagoes, 0);
  const conflitosOperacionais = segmentos.filter((segmento) => segmento.conflito && segmento.etapa === 'OPERACAO');

  function moverVagao(identificadorVagao, linhaDestino) {
    setPlanejamento((atual) => moverVagaoNoPlanejamento(atual, identificadorVagao, linhaDestino, bloqueios));
  }

  function alternarBloqueio(identificadorVagao) {
    setBloqueios((atual) => ({ ...atual, [identificadorVagao]: !atual[identificadorVagao] }));
  }

  function solicitarReplanejamento(movimento) {
    setErroDetalhe('');
    setMensagemReplanejamento('');
    setMotivoReplanejamento('');
    setReplanejamentoPendente(movimento);
  }

  async function confirmarReplanejamento() {
    const motivo = motivoReplanejamento.trim();
    if (!replanejamentoPendente || !motivo) {
      setErroDetalhe('Informe o motivo para confirmar o replanejamento.');
      return;
    }
    setSalvandoReplanejamento(true);
    setErroDetalhe('');
    try {
      const detalheAtualizado = await railApi.replanejarConteiner(visitaSelecionadaId, {
        codigoConteiner: replanejamentoPendente.codigoConteiner,
        tipoMovimentacao: replanejamentoPendente.tipoOperacao === 'DESCARGA' ? 'DESCARGA_TREM' : 'CARGA_TREM',
        vagaoOrigem: replanejamentoPendente.vagaoOrigem,
        vagaoDestino: replanejamentoPendente.vagaoDestino,
        versaoComposicao: Number(detalheVisita?.versao ?? 0),
        motivo
      });
      setDetalheVisita(detalheAtualizado);
      setReplanejamentoPendente(null);
      setMotivoReplanejamento('');
      setMensagemReplanejamento(`Contêiner ${replanejamentoPendente.codigoConteiner} replanejado para ${replanejamentoPendente.vagaoDestino}.`);
    } catch (motivoErro) {
      setErroDetalhe(formatError(motivoErro, 'Não foi possível confirmar o replanejamento.'));
    } finally {
      setSalvandoReplanejamento(false);
    }
  }

  function cancelarReplanejamento() {
    setReplanejamentoPendente(null);
    setMotivoReplanejamento('');
    setErroDetalhe('');
  }

  function resetarPlano() {
    const resumo = visitas.find((visita) => String(visita.id) === String(visitaSelecionadaId));
    const segmentoOperacao = segmentos.find((segmento) => segmento.visitaChave === resumo?.chave && segmento.etapa === 'OPERACAO');
    setPlanejamento(criarPlanejamentoVagoes(detalheVisita, segmentoOperacao?.linha));
    setBloqueios(Object.fromEntries(composicao.vagoes
      .filter((vagao) => vagao.bloqueadoOrigem)
      .map((vagao) => [vagao.identificadorVagao, true])));
  }

  return <>
    <PageHeader
      eyebrow="Ferrovia"
      title="Ferrovia visual"
      description="Planeje a composição, acompanhe a ocupação das linhas e simule a carga e a descarga de cada vagão."
      actions={<button className="secondary" type="button" onClick={carregar} disabled={carregando}>Atualizar ferrovia</button>}
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

    {carregando ? <Loading label="Montando ferrovia visual..." /> : <>
      <div className="metrics-grid">
        <MetricCard label="Visitas na janela" value={simuladas.length} detail={`${vagoes} vagão(ões) planejados`} />
        <MetricCard label="Trens no terminal" value={emTerminal} />
        <MetricCard label="Em operação" value={operando} detail={`${linhasOcupadas} linha(s) ocupada(s)`} />
        <MetricCard label="Trens em conflito" value={conflitos} detail={conflitos ? 'Replanejamento necessário' : 'Sem sobreposição de linha'} />
      </div>

      <Section title="Composição gráfica e planejamento" description="Arraste contêineres entre vagões e confirme o motivo. O movimento é validado e persistido no manifesto e na ordem ferroviária.">
        <div className="rail-composition-toolbar">
          <label className="compact-field">Visita de trem
            <select value={visitaSelecionadaId} onChange={(event) => setVisitaSelecionadaId(event.target.value)} disabled={!visitas.length}>
              <option value="">Selecione</option>
              {visitas.map((visita) => <option key={visita.chave} value={visita.id}>{visita.identificadorTrem} · {visita.operadoraFerroviaria} · {visita.statusVisita}</option>)}
            </select>
          </label>
          <button className="secondary" type="button" disabled={!detalheVisita || carregandoDetalhe} onClick={resetarPlano}>Resetar plano de linhas</button>
        </div>
        <Message type="error">{erroDetalhe}</Message>
        <Message type="success">{mensagemReplanejamento}</Message>
        {carregandoDetalhe ? <Loading label="Carregando locomotiva, vagões e contêineres..." /> : !detalheVisita ? <EmptyState title="Selecione uma visita" description="A composição e a ocupação das linhas serão exibidas aqui." /> : <>
          <ComposicaoVisual
            composicao={composicao}
            planejamento={planejamento}
            bloqueios={bloqueios}
            onMover={moverVagao}
            onAlternarBloqueio={alternarBloqueio}
            onSolicitarReplanejamento={solicitarReplanejamento}
          />
          {replanejamentoPendente && <div className="rail-composition-toolbar">
            <div>
              <strong>Confirmar replanejamento</strong>
              <p>{replanejamentoPendente.codigoConteiner}: {replanejamentoPendente.vagaoOrigem} → {replanejamentoPendente.vagaoDestino}</p>
            </div>
            <label className="compact-field">Motivo
              <textarea
                maxLength="500"
                value={motivoReplanejamento}
                onChange={(event) => setMotivoReplanejamento(event.target.value)}
                placeholder="Informe o motivo operacional"
              />
            </label>
            <div className="rail-lineup-control-buttons">
              <button type="button" disabled={salvandoReplanejamento} onClick={confirmarReplanejamento}>
                {salvandoReplanejamento ? 'Confirmando...' : 'Confirmar movimento'}
              </button>
              <button className="secondary" type="button" disabled={salvandoReplanejamento} onClick={cancelarReplanejamento}>Cancelar</button>
            </div>
          </div>}
          {composicao.operacoesSemVagao.length > 0 && <Message type="warning">Há {composicao.operacoesSemVagao.length} operação(ões) com vagão ausente ou incompatível na composição.</Message>}
          <PatioFerroviario ocupacoes={ocupacoesComposicao} bloqueios={bloqueios} onMover={moverVagao} />
        </>}
      </Section>

      <RailOperationsPanels idVisita={visitaSelecionadaId} composicao={composicao} />

      <Section title="Cronograma e ocupação das linhas" description={`Cenário simulado em ${formatarDataHora(instante)}. O tempo avança de cima para baixo.`}>
        <TimelineFerroviaria visitas={visitas} segmentos={segmentos} instante={instante} />
        <footer className="rail-lineup-legend">
          <span><i className="rail-lineup-legend-dot recepcao" /> Recepção</span>
          <span><i className="rail-lineup-legend-dot operacao" /> Operação</span>
          <span><i className="rail-lineup-legend-dot expedicao" /> Expedição</span>
          <span><i className="rail-lineup-legend-dot conflito" /> Conflito de linha ou recurso</span>
        </footer>
      </Section>

      {conflitosOperacionais.length > 0 && <Section title="Conflitos operacionais" description="Sobreposições de trens na mesma linha durante a janela planejada.">
        <div className="rail-conflict-grid">
          {conflitosOperacionais.map((segmento) => <article key={segmento.chave}>
            <strong>{segmento.visita.identificadorTrem}</strong>
            <span>{segmento.linha}</span>
            <small>{formatarDataHora(segmento.inicio)} até {formatarDataHora(segmento.fim)}</small>
          </article>)}
        </div>
      </Section>}

      <Section title="Detalhamento das visitas" description="Clique em uma visita para abrir sua composição gráfica.">
        <DataTable rows={simuladas} rowKey="chave" onRowClick={(linha) => setVisitaSelecionadaId(String(linha.id))} emptyTitle="Nenhuma visita ferroviária disponível" columns={[
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
