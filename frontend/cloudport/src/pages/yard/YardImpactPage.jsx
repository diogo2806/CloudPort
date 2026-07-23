import { useMemo, useState } from 'react';
import { sanitizeText } from '../../api.js';
import { DataTable, EmptyState, Loading, Message, MetricCard, Section, StatusBadge } from '../../components.jsx';
import { yardPredictiveApi } from '../../yardPredictiveApi.js';
import { displayValue, YardPageHeader, useRemote } from './YardShared.jsx';

export function YardImpactPage({ navigate }) {
  const [horizonteHoras, setHorizonteHoras] = useState(6);
  const [blocoSelecionado, setBlocoSelecionado] = useState('');
  const [powSelecionado, setPowSelecionado] = useState('');
  const remote = useRemote(() => yardPredictiveApi.obterYardImpact(horizonteHoras), [horizonteHoras]);
  const previsaoRemote = useRemote(() => yardPredictiveApi.obterPrevisaoDemanda(horizonteHoras), [horizonteHoras]);
  const impact = remote.data ?? {};
  const previsao = previsaoRemote.data ?? {};
  const blocos = impact.blocos ?? [];
  const pows = impact.pows ?? [];
  const unidades = impact.unidades ?? [];
  const alertas = impact.alertas ?? [];

  const unidadesFiltradas = useMemo(() => unidades.filter((unidade) => {
    if (blocoSelecionado && unidade.bloco !== blocoSelecionado) return false;
    if (powSelecionado && unidade.pow !== powSelecionado) return false;
    return true;
  }), [blocoSelecionado, powSelecionado, unidades]);

  const atualizar = () => {
    remote.reload();
    previsaoRemote.reload();
  };

  return <>
    <YardPageHeader
      path="/home/patio/yard-impact"
      navigate={navigate}
      title="Yard Impact"
      description="Projeta entradas, saídas, rehandles, reservas, work instructions, saturação e demanda de CHE no horizonte operacional."
      actions={<button className="secondary" type="button" onClick={atualizar}>Atualizar projeção</button>}
    />
    <Message type="error">{remote.error || previsaoRemote.error}</Message>
    <Section title="Horizonte da projeção" description="O horizonte mínimo operacional é de seis horas e pode ser ampliado até vinte e quatro horas.">
      <label className="field">
        <span>Horas projetadas: {horizonteHoras}</span>
        <input type="range" min="6" max="24" step="1" value={horizonteHoras} onChange={(event) => setHorizonteHoras(Number(event.target.value))} />
      </label>
    </Section>

    {previsaoRemote.loading ? <Loading label="Calculando previsão de demanda..." /> : <Section
      title="Previsão de demanda operacional"
      description="A previsão apoia a decisão, mas nenhuma posição é persistida sem aprovação das regras determinísticas."
    >
      <div className="metrics-grid">
        <MetricCard label="Demanda prevista" value={previsao.demandaPrevista ?? 0} detail={`Próximas ${previsao.horizonteHoras ?? horizonteHoras}h`} />
        <MetricCard label="Duração prevista" value={`${previsao.duracaoPrevistaMinutos ?? 0} min`} />
        <MetricCard label="Confiança" value={`${((previsao.confianca ?? 0) * 100).toFixed(0)}%`} />
        <MetricCard label="Baseline determinístico" value={previsao.baselineDeterministico ?? 0} detail={`Diferença ${previsao.diferencaBaseline ?? 0}`} />
      </div>
      <Message type={previsao.fallbackDeterministico ? 'warning' : 'info'}>
        {previsao.fallbackDeterministico
          ? `Fallback determinístico ativo. ${sanitizeText(previsao.explicacao)}`
          : `Sugestão gerada pelo modelo ${sanitizeText(previsao.versaoModelo)}. ${sanitizeText(previsao.explicacao)}`}
      </Message>
      <div className="card-meta">
        <StatusBadge value={previsao.fallbackDeterministico ? 'DETERMINISTICO' : 'MODELO'} />
        <small>Gerado em {displayValue(previsao.geradoEm)}</small>
      </div>
      {(previsao.validacoesObrigatorias ?? []).length > 0 && <div className="card-list">
        {(previsao.validacoesObrigatorias ?? []).map((validacao) => <article className="content-card" key={validacao}>
          <strong>Validação obrigatória</strong>
          <p>{sanitizeText(validacao)}</p>
        </article>)}
      </div>}
    </Section>}

    {remote.loading ? <Loading label="Calculando Yard Impact..." /> : <>
      <div className="metrics-grid">
        <MetricCard label="Entradas" value={impact.totalEntradas ?? 0} detail={`Próximas ${impact.horizonteHoras ?? horizonteHoras}h`} />
        <MetricCard label="Saídas" value={impact.totalSaidas ?? 0} />
        <MetricCard label="Rehandles" value={impact.totalRehandles ?? 0} />
        <MetricCard label="Reservas" value={impact.totalReservas ?? 0} />
        <MetricCard label="Work instructions" value={impact.totalWorkInstructions ?? 0} />
        <MetricCard label="Déficit de CHE" value={impact.deficitChe ?? 0} detail={`${impact.cheDisponiveis ?? 0} disponível(is) para demanda ${impact.demandaChe ?? 0}`} />
      </div>

      <Message type={alertas.length ? 'warning' : 'success'}>
        {alertas.length ? `${alertas.length} alerta(s) operacional(is) na projeção.` : 'Nenhum risco de saturação ou déficit de CHE identificado.'}
      </Message>

      <div className="split-grid">
        <Section title="Mapa de calor por bloco" description="Selecione um bloco para restringir o drill-down às unidades que geram o impacto.">
          {!blocos.length ? <EmptyState title="Sem blocos para projetar" /> : <div className="card-list">
            {blocos.map((bloco) => <button
              type="button"
              className={`content-card ${blocoSelecionado === bloco.bloco ? 'selected' : ''}`}
              key={bloco.bloco}
              onClick={() => setBlocoSelecionado((current) => current === bloco.bloco ? '' : bloco.bloco)}
            >
              <div className="card-meta"><strong>{sanitizeText(bloco.bloco)}</strong><StatusBadge value={bloco.saturado ? 'SATURADO' : 'CONTROLADO'} /></div>
              <progress max="100" value={Math.min(100, Number(bloco.ocupacaoProjetadaPercentual ?? 0))} aria-label={`Ocupação projetada do bloco ${bloco.bloco}`} />
              <p><strong>{Number(bloco.ocupacaoProjetadaPercentual ?? 0).toFixed(1)}%</strong> · {bloco.movimentosPrevistos ?? 0} movimentos · {bloco.reservasAtivas ?? 0} reservas</p>
              <small>Entradas {bloco.entradas ?? 0} · Saídas {bloco.saidas ?? 0} · Rehandles {bloco.rehandles ?? 0}</small>
            </button>)}
          </div>}
        </Section>

        <Section title="Impacto por POW" description="A cobertura relaciona filas, work instructions, CHE associados e motivos de bloqueio.">
          <DataTable
            rows={pows}
            rowKey="pow"
            onRowClick={(row) => setPowSelecionado((current) => current === row.pow ? '' : row.pow)}
            columns={[
              { key: 'pow', label: 'POW' },
              { key: 'workQueues', label: 'Filas' },
              { key: 'workInstructions', label: 'Instruções' },
              { key: 'demandaChe', label: 'Demanda CHE' },
              { key: 'cheAssociados', label: 'CHE associados' },
              { key: 'deficitChe', label: 'Déficit' },
              { key: 'bloqueado', label: 'Estado', render: (row) => <StatusBadge value={row.bloqueado ? 'BLOQUEADO' : 'COBERTO'} /> },
              { key: 'motivosBloqueio', label: 'Motivos', render: (row) => (row.motivosBloqueio ?? []).join(' ') || '—' }
            ]}
            emptyTitle="Nenhum POW projetado"
          />
        </Section>
      </div>

      <Section title="Drill-down das unidades" description="Lista as unidades e posições planejadas que compõem a projeção selecionada.">
        <DataTable
          rows={unidadesFiltradas}
          rowKey="planoId"
          columns={[
            { key: 'codigoContainer', label: 'Unidade' },
            { key: 'bloco', label: 'Bloco' },
            { key: 'pow', label: 'POW' },
            { key: 'posicao', label: 'Posição', render: (row) => `${row.linha ?? '—'} / ${row.coluna ?? '—'} / ${sanitizeText(row.camada) || '—'}` },
            { key: 'estado', label: 'Plano', render: (row) => <StatusBadge value={row.estado} /> },
            { key: 'equipamentoId', label: 'CHE' },
            { key: 'horizonteInicio', label: 'Início', render: (row) => displayValue(row.horizonteInicio) },
            { key: 'horizonteFim', label: 'Fim', render: (row) => displayValue(row.horizonteFim) },
            { key: 'validoAte', label: 'Validade', render: (row) => displayValue(row.validoAte) },
            { key: 'motivo', label: 'Motivo' }
          ]}
          emptyTitle="Nenhuma unidade corresponde aos filtros"
        />
      </Section>

      {alertas.length > 0 && <Section title="Alertas projetados">
        <div className="card-list">{alertas.map((alerta, index) => <article className="content-card" key={`${alerta}-${index}`}><StatusBadge value="ALERTA" /><p>{sanitizeText(alerta)}</p></article>)}</div>
      </Section>}
    </>}
  </>;
}
