import { useEffect, useMemo, useState } from 'react';
import { api, hasAnyRole } from '../../api.js';
import { DataTable, EmptyState, JsonDetails, Loading, Message, MetricCard, Section, StatusBadge } from '../../components.jsx';
import { CommandPanel, DetailGrid, displayValue, FINAL_ORDER_STATUSES, normalized, useCommand, useRemote, YardPageHeader } from './YardShared.jsx';
import { YardCustodyPanel } from './YardCustodyPanel.jsx';

const HISTORY_KEY = 'cloudport.yard-planning.history.v1';

function readHistory() {
  try {
    const value = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]');
    return Array.isArray(value) ? value : [];
  } catch {
    return [];
  }
}

function writeHistory(history) {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(history.slice(0, 50)));
}

function numberValue(value) {
  const number = Number(value ?? 0);
  return Number.isFinite(number) ? number : 0;
}

function createSnapshot(data) {
  const optimization = data.optimization ?? {};
  const positions = data.positions ?? [];
  const orders = data.orders ?? [];
  const occupied = positions.filter((position) => position.ocupada).length;
  const rejected = orders.filter((order) => normalized(order.statusOrdem).includes('rejeit')).length;
  return {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    capturedAt: new Date().toISOString(),
    occupancy: numberValue(data.heatmap?.percentualOcupacaoGeral ?? (positions.length ? occupied / positions.length * 100 : 0)),
    totalPositions: positions.length,
    activeOrders: orders.filter((order) => !FINAL_ORDER_STATUSES.has(order.statusOrdem)).length,
    rejected,
    reshuffles: numberValue(data.reshuffling?.conteinersParaReshuffling?.length),
    originalDistance: numberValue(optimization.distanciaTotalOriginal),
    optimizedDistance: numberValue(optimization.distanciaTotalOtimizada),
    originalTime: numberValue(optimization.tempoEstimadoOriginal),
    optimizedTime: numberValue(optimization.tempoEstimadoOtimizado),
    improvement: numberValue(optimization.percentualMelhoria),
    inputs: { positions, orders },
    result: optimization,
    rejectionDetails: orders.filter((order) => normalized(order.statusOrdem).includes('rejeit'))
  };
}

function exportHistory(history) {
  const header = ['capturado_em', 'ocupacao_percentual', 'ordens_ativas', 'rejeicoes', 'reshuffles', 'distancia_original', 'distancia_otimizada', 'tempo_original', 'tempo_otimizado', 'melhoria_percentual'];
  const rows = history.map((item) => [item.capturedAt, item.occupancy, item.activeOrders, item.rejected, item.reshuffles, item.originalDistance, item.optimizedDistance, item.originalTime, item.optimizedTime, item.improvement]);
  const csv = [header, ...rows].map((row) => row.map((value) => `"${String(value ?? '').replaceAll('"', '""')}"`).join(';')).join('\n');
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = `yard-planning-${new Date().toISOString().slice(0, 10)}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

function YardPlanningManual() {
  return <details className="json-details">
    <summary>ⓘ Manual</summary>
    <div className="content-card">
      <h3>Finalidade da tela</h3><p>Acompanhar ocupação, otimizações, rejeições e re-shuffles em uma visão única, com comparação entre o cenário original e o otimizado.</p>
      <h3>Fluxo operacional</h3><ol><li>Atualize os indicadores para consultar o estado atual do backend.</li><li>Use os filtros para localizar execuções históricas.</li><li>Abra uma execução para conferir entradas, resultados e rejeições.</li><li>Exporte o histórico quando precisar analisar os dados fora do sistema.</li></ol>
      <h3>Explicação dos campos</h3><ul><li>Ocupação: percentual de posições ocupadas no mapa retornado pelo backend.</li><li>Distância e tempo: valores antes e depois da otimização.</li><li>Rejeições: ordens cujo status indica rejeição.</li><li>Re-shuffles: quantidade de contêineres candidatos ao reposicionamento.</li></ul>
      <h3>Permissões necessárias</h3><p>Perfis com acesso de consulta ao pátio visualizam o dashboard. ADMIN_PORTO e PLANEJADOR também podem executar comandos na tela de Automação.</p>
      <h3>Estados possíveis</h3><ul><li>Carregando, disponível, sem dados e falha de consulta.</li><li>Otimização com ganho, sem ganho ou sem referência comparável.</li></ul>
      <h3>Motivos de bloqueio</h3><ul><li>Sessão expirada, perfil sem acesso ao pátio ou indisponibilidade de uma API obrigatória.</li><li>Ausência de posições ou ordens suficientes para calcular a comparação.</li></ul>
      <h3>Exemplo</h3><p>Uma execução com distância original 1.200 e otimizada 900 apresenta economia de 300 unidades e permite inspecionar as ordens usadas no cálculo.</p>
      <h3>Atalhos</h3><ul><li>Atualizar: captura uma nova leitura.</li><li>Exportar CSV: baixa o histórico filtrado.</li><li>Ver detalhes: abre entradas, resultados e rejeições.</li></ul>
      <p><a href="https://github.com/diogo2806/CloudPort/issues/726" target="_blank" rel="noreferrer">Abrir processo completo</a></p>
    </div>
  </details>;
}

export function YardKpiPage({ navigate }) {
  const [history, setHistory] = useState(readHistory);
  const [selected, setSelected] = useState(null);
  const [query, setQuery] = useState('');
  const remote = useRemote(async () => {
    const [map, positions, queues, orders, heatmap, level, optimization, reshuffling] = await Promise.all([
      api.obterMapaPatio({}), api.listarPosicoesReservaveisPatio(), api.listarWorkQueuesPatio(), api.listarOrdensPatio(),
      api.obterHeatmapPatio(), api.obterNivelOcupacaoPatio(), api.obterEstatisticasOtimizacaoPatio(), api.analisarReshufflingPatio()
    ]);
    return { map: map ?? {}, positions: positions ?? [], queues: queues ?? [], orders: orders ?? [], heatmap: heatmap ?? {}, level, optimization: optimization ?? {}, reshuffling: reshuffling ?? {} };
  }, []);
  const data = remote.data ?? {};
  const positions = data.positions ?? [];
  const orders = data.orders ?? [];
  const occupied = positions.filter((position) => position.ocupada).length;
  const restricted = positions.filter((position) => position.bloqueada || position.interditada || !position.areaPermitida).length;
  const activeOrders = orders.filter((order) => !FINAL_ORDER_STATUSES.has(order.statusOrdem)).length;
  const uncovered = orders.filter((order) => !order.workQueueId && !FINAL_ORDER_STATUSES.has(order.statusOrdem)).length;
  const filteredHistory = useMemo(() => history.filter((item) => !query || new Date(item.capturedAt).toLocaleString('pt-BR').includes(query) || String(item.improvement).includes(query)), [history, query]);

  useEffect(() => {
    if (!remote.data) return;
    const snapshot = createSnapshot(remote.data);
    setHistory((current) => {
      const next = [snapshot, ...current].slice(0, 50);
      writeHistory(next);
      return next;
    });
  }, [remote.data]);

  return <>
    <YardPageHeader path="/home/patio/dashboard-kpi" navigate={navigate} title="Yard Planning" description="Dashboard unificado de ocupação, otimizações, rejeições, re-shuffles e histórico de leituras." actions={<><button className="secondary" onClick={() => exportHistory(filteredHistory)} disabled={!filteredHistory.length}>Exportar CSV</button><button className="secondary" onClick={remote.reload}>Atualizar</button></>} />
    <YardPlanningManual />
    <Message type="error">{remote.error}</Message>
    {remote.loading ? <Loading label="Calculando indicadores com dados do Yard..." /> : <>
      <div className="metrics-grid">
        <MetricCard label="Ocupação" value={`${numberValue(data.heatmap?.percentualOcupacaoGeral ?? (positions.length ? occupied / positions.length * 100 : 0)).toFixed(1)}%`} detail={displayValue(data.level)} />
        <MetricCard label="Posições restritas" value={restricted} detail={`${positions.length} posições reais`} />
        <MetricCard label="Work instructions ativas" value={activeOrders} detail={`${uncovered} sem cobertura`} />
        <MetricCard label="Re-shuffles candidatos" value={data.reshuffling?.conteinersParaReshuffling?.length ?? 0} detail={data.reshuffling?.recomendado ? 'Plano recomendado' : 'Sem recomendação'} />
      </div>
      <Section title="Comparação da otimização" description="Fórmulas: economia = original − otimizado; melhoria (%) = economia ÷ original × 100."><DataTable rows={[data.optimization]} rowKey={() => 'comparison'} columns={[
        { key: 'distanciaTotalOriginal', label: 'Distância original' }, { key: 'distanciaTotalOtimizada', label: 'Distância otimizada' },
        { key: 'economiaDistancia', label: 'Economia' }, { key: 'percentualMelhoria', label: 'Melhoria (%)' },
        { key: 'tempoEstimadoOriginal', label: 'Tempo original' }, { key: 'tempoEstimadoOtimizado', label: 'Tempo otimizado' }
      ]} /></Section>
      <Section title="Histórico de execuções" description="Mantém as 50 leituras mais recentes neste navegador." actions={<input aria-label="Filtrar histórico" placeholder="Filtrar por data ou melhoria" value={query} onChange={(event) => setQuery(event.target.value)} />}>
        <DataTable rows={filteredHistory} rowKey="id" columns={[
          { key: 'capturedAt', label: 'Capturado em', render: (row) => displayValue(row.capturedAt) },
          { key: 'occupancy', label: 'Ocupação', render: (row) => `${row.occupancy.toFixed(1)}%` },
          { key: 'activeOrders', label: 'Ordens ativas' }, { key: 'rejected', label: 'Rejeições' }, { key: 'reshuffles', label: 'Re-shuffles' },
          { key: 'improvement', label: 'Melhoria', render: (row) => `${row.improvement.toFixed(1)}%` },
          { key: 'actions', label: 'Ações', render: (row) => <button className="secondary small" onClick={() => setSelected(row)}>Ver detalhes</button> }
        ]} emptyTitle="Nenhuma execução capturada" />
      </Section>
      {selected && <Section title={`Detalhes da execução de ${displayValue(selected.capturedAt)}`} actions={<button className="secondary small" onClick={() => setSelected(null)}>Fechar</button>}>
        <div className="split-grid"><JsonDetails value={selected.inputs} title="Entradas" /><JsonDetails value={selected.result} title="Resultado otimizado" /></div>
        <JsonDetails value={selected.rejectionDetails} title="Rejeições" />
      </Section>}
    </>}
    <YardCustodyPanel />
  </>;
}

function HeatmapPreview({ heatmap }) {
  const zones = [...(heatmap?.zonasAlta ?? []), ...(heatmap?.zonasMedia ?? []), ...(heatmap?.zonasBaixa ?? [])].slice(0, 100);
  if (!zones.length) return <EmptyState title="Heatmap sem zonas ocupadas" />;
  return <div className="heatmap-preview">{zones.map((zone) => <button type="button" key={`${zone.linha}-${zone.coluna}`} className={`heatmap-cell level-${normalized(zone.nivel)}`} title={`Linha ${zone.linha}, coluna ${zone.coluna}: ${zone.ocupacao}`}><strong>{zone.ocupacao}</strong><span>L{zone.linha}/C{zone.coluna}</span></button>)}</div>;
}

export function YardAutomationPage({ navigate, session }) {
  const canAutomate = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR');
  const remote = useRemote(async () => {
    const [heatmap, level, conflicts, reshuffling, optimizedOrders, optimization] = await Promise.all([
      api.obterHeatmapPatio(), api.obterNivelOcupacaoPatio(), api.listarConflitosRtgPatio(), api.analisarReshufflingPatio(), api.listarOrdensOtimizadasPatio(), api.obterEstatisticasOtimizacaoPatio()
    ]);
    return { heatmap: heatmap ?? {}, level, conflicts: conflicts ?? [], reshuffling: reshuffling ?? {}, optimizedOrders: optimizedOrders ?? [], optimization: optimization ?? {} };
  }, []);
  const commands = useCommand(remote);
  const data = remote.data ?? {};
  function executeReshuffling() { commands.setCommand({ title: 'Executar reshuffling recomendado', description: 'O backend recalculará o plano e criará ou reutilizará as ordens persistidas aplicáveis. Nenhuma coordenada será produzida pelo navegador.', success: 'Reshuffling confirmado e estado persistido recarregado.', run: (reason) => api.executarReshufflingPatio(reason) }); }
  return <>
    <YardPageHeader path="/home/patio/automacao" navigate={navigate} title="Automação do pátio" description="Heatmap, otimização, conflitos RTG e reshuffling calculados pelo backend a partir do mapa real." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error || commands.error}</Message><Message type="success">{commands.success}</Message>
    {!canAutomate && <Message type="warning">Seu perfil pode consultar as análises, mas não executar comandos de automação.</Message>}
    <CommandPanel command={commands.command} busy={commands.busy} onCancel={() => commands.setCommand(null)} onConfirm={commands.confirm} />
    {remote.loading ? <Loading label="Carregando análises de automação..." /> : <>
      <div className="metrics-grid"><MetricCard label="Nível de ocupação" value={displayValue(data.level)} detail={`${numberValue(data.heatmap?.percentualOcupacaoGeral).toFixed(1)}%`} /><MetricCard label="Conflitos RTG" value={data.conflicts?.length ?? 0} /><MetricCard label="Reshuffling" value={data.reshuffling?.recomendado ? 'RECOMENDADO' : 'NÃO RECOMENDADO'} detail={`${data.reshuffling?.conteinersParaReshuffling?.length ?? 0} candidato(s)`} /><MetricCard label="Sequência otimizada" value={data.optimizedOrders?.length ?? 0} detail="work instructions" /></div>
      <Section title="Heatmap de ocupação" description="A prévia usa apenas as zonas retornadas pelo backend."><HeatmapPreview heatmap={data.heatmap} /></Section>
      <div className="split-grid"><Section title="Plano de reshuffling" actions={canAutomate && data.reshuffling?.recomendado ? <button onClick={executeReshuffling}>Executar plano</button> : null}><DetailGrid value={data.reshuffling} fields={[[ 'recomendado', 'Recomendado' ], [ 'motivo', 'Motivo' ], [ 'totalConteiners', 'Total de contêineres' ], [ 'conteinersParaReshuffling', 'Candidatos', (value) => value.conteinersParaReshuffling?.length ?? 0 ]]} /><JsonDetails value={data.reshuffling} title="Plano calculado pelo backend" /></Section><Section title="Conflitos RTG"><DataTable rows={data.conflicts ?? []} rowKey={(row, index) => row.id ?? `${row.identificadorRtg}-${row.fila}-${index}`} columns={[{ key: 'identificadorRtg', label: 'RTG' }, { key: 'fila', label: 'Fila' }, { key: 'tipoConflito', label: 'Conflito', render: (row) => <StatusBadge value={row.tipoConflito ?? row.status} /> }, { key: 'descricao', label: 'Descrição' }]} emptyTitle="Nenhum conflito RTG" /></Section></div>
      <Section title="Sequência de work instructions otimizada"><DataTable rows={data.optimizedOrders ?? []} rowKey={(row) => row.id} columns={[{ key: 'codigoConteiner', label: 'Contêiner' }, { key: 'tipoMovimento', label: 'Movimento' }, { key: 'statusOrdem', label: 'Status', render: (row) => <StatusBadge value={row.statusOrdem} /> }, { key: 'linhaDestino', label: 'Linha' }, { key: 'colunaDestino', label: 'Coluna' }, { key: 'camadaDestino', label: 'Camada' }, { key: 'prioridadeOperacional', label: 'Prioridade' }]} emptyTitle="Nenhuma ordem pendente para otimização" /></Section>
    </>}
  </>;
}