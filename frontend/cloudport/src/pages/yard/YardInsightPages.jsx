import { api, hasAnyRole } from '../../api.js';
import { DataTable, EmptyState, JsonDetails, Loading, Message, MetricCard, Section, StatusBadge } from '../../components.jsx';
import { CommandPanel, DetailGrid, displayValue, FINAL_ORDER_STATUSES, normalized, useCommand, useRemote, YardPageHeader } from './YardShared.jsx';
import { YardCustodyPanel } from './YardCustodyPanel.jsx';

export function YardKpiPage({ navigate }) {
  const remote = useRemote(async () => {
    const [map, positions, queues, orders, heatmap, level, optimization] = await Promise.all([
      api.obterMapaPatio({}),
      api.listarPosicoesReservaveisPatio(),
      api.listarWorkQueuesPatio(),
      api.listarOrdensPatio(),
      api.obterHeatmapPatio(),
      api.obterNivelOcupacaoPatio(),
      api.obterEstatisticasOtimizacaoPatio()
    ]);
    return {
      map: map ?? {},
      positions: positions ?? [],
      queues: queues ?? [],
      orders: orders ?? [],
      heatmap: heatmap ?? {},
      level,
      optimization: optimization ?? {}
    };
  }, []);
  const data = remote.data ?? {};
  const positions = data.positions ?? [];
  const orders = data.orders ?? [];
  const occupied = positions.filter((position) => position.ocupada).length;
  const restricted = positions.filter((position) => position.bloqueada || position.interditada || !position.areaPermitida).length;
  const activeOrders = orders.filter((order) => !FINAL_ORDER_STATUSES.has(order.statusOrdem)).length;
  const uncovered = orders.filter((order) => !order.workQueueId && !FINAL_ORDER_STATUSES.has(order.statusOrdem)).length;
  const statusCounts = Array.from(orders.reduce((accumulator, order) => {
    const key = order.statusOrdem ?? 'INDEFINIDO';
    accumulator.set(key, (accumulator.get(key) ?? 0) + 1);
    return accumulator;
  }, new Map()), ([status, total]) => ({ status, total }));

  return <>
    <YardPageHeader path="/home/patio/dashboard-kpi" navigate={navigate} title="Indicadores do pátio" description="Indicadores derivados exclusivamente das posições, filas, instruções e análises persistidas pelo backend." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error}</Message>
    {remote.loading ? <Loading label="Calculando indicadores com dados do Yard..." /> : <>
      <div className="metrics-grid">
        <MetricCard label="Ocupação" value={`${Number(data.heatmap?.percentualOcupacaoGeral ?? (positions.length ? occupied / positions.length * 100 : 0)).toFixed(1)}%`} detail={displayValue(data.level)} />
        <MetricCard label="Posições restritas" value={restricted} detail={`${positions.length} posições reais`} />
        <MetricCard label="Work instructions ativas" value={activeOrders} detail={`${uncovered} sem cobertura`} />
        <MetricCard label="Work queues" value={data.queues?.length ?? 0} detail={`${data.queues?.filter((queue) => queue.status === 'ATIVA').length ?? 0} ativas`} />
      </div>
      <div className="split-grid">
        <Section title="Distribuição por status"><DataTable rows={statusCounts} rowKey="status" columns={[
          { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
          { key: 'total', label: 'Total' }
        ]} emptyTitle="Nenhuma work instruction" /></Section>
        <Section title="Otimização de rota"><DetailGrid value={data.optimization} fields={[
          ['distanciaTotalOriginal', 'Distância original'],
          ['distanciaTotalOtimizada', 'Distância otimizada'],
          ['economiaDistancia', 'Economia'],
          ['percentualMelhoria', 'Melhoria (%)'],
          ['tempoEstimadoOriginal', 'Tempo original'],
          ['tempoEstimadoOtimizado', 'Tempo otimizado']
        ]} /><JsonDetails value={data.optimization} title="Resultado completo do backend" /></Section>
      </div>
      <Section title="Rotas prioritárias para o gate"><DataTable rows={data.heatmap?.rotasEscape ?? []} rowKey={(row) => row.codigoConteiner} columns={[
        { key: 'codigoConteiner', label: 'Contêiner' },
        { key: 'linhaAtual', label: 'Linha' },
        { key: 'colunaAtual', label: 'Coluna' },
        { key: 'distanciaParaGate', label: 'Distância' },
        { key: 'prioridade', label: 'Prioridade', render: (row) => <StatusBadge value={row.prioridade} /> }
      ]} emptyTitle="Nenhuma rota prioritária" /></Section>
    </>}
    <YardCustodyPanel />
  </>;
}

function HeatmapPreview({ heatmap }) {
  const zones = [
    ...(heatmap?.zonasAlta ?? []),
    ...(heatmap?.zonasMedia ?? []),
    ...(heatmap?.zonasBaixa ?? [])
  ].slice(0, 100);
  if (!zones.length) return <EmptyState title="Heatmap sem zonas ocupadas" />;
  return <div className="heatmap-preview">{zones.map((zone) => <button type="button" key={`${zone.linha}-${zone.coluna}`} className={`heatmap-cell level-${normalized(zone.nivel)}`} title={`Linha ${zone.linha}, coluna ${zone.coluna}: ${zone.ocupacao}`}><strong>{zone.ocupacao}</strong><span>L{zone.linha}/C{zone.coluna}</span></button>)}</div>;
}

export function YardAutomationPage({ navigate, session }) {
  const canAutomate = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR');
  const remote = useRemote(async () => {
    const [heatmap, level, conflicts, reshuffling, optimizedOrders, optimization] = await Promise.all([
      api.obterHeatmapPatio(),
      api.obterNivelOcupacaoPatio(),
      api.listarConflitosRtgPatio(),
      api.analisarReshufflingPatio(),
      api.listarOrdensOtimizadasPatio(),
      api.obterEstatisticasOtimizacaoPatio()
    ]);
    return {
      heatmap: heatmap ?? {},
      level,
      conflicts: conflicts ?? [],
      reshuffling: reshuffling ?? {},
      optimizedOrders: optimizedOrders ?? [],
      optimization: optimization ?? {}
    };
  }, []);
  const commands = useCommand(remote);
  const data = remote.data ?? {};

  function executeReshuffling() {
    commands.setCommand({
      title: 'Executar reshuffling recomendado',
      description: 'O backend recalculará o plano e criará ou reutilizará as ordens persistidas aplicáveis. Nenhuma coordenada será produzida pelo navegador.',
      success: 'Reshuffling confirmado e estado persistido recarregado.',
      run: (reason) => api.executarReshufflingPatio(reason)
    });
  }

  return <>
    <YardPageHeader path="/home/patio/automacao" navigate={navigate} title="Automação do pátio" description="Heatmap, otimização, conflitos RTG e reshuffling calculados pelo backend a partir do mapa real." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error || commands.error}</Message>
    <Message type="success">{commands.success}</Message>
    {!canAutomate && <Message type="warning">Seu perfil pode consultar as análises, mas não executar comandos de automação.</Message>}
    <CommandPanel command={commands.command} busy={commands.busy} onCancel={() => commands.setCommand(null)} onConfirm={commands.confirm} />
    {remote.loading ? <Loading label="Carregando análises de automação..." /> : <>
      <div className="metrics-grid">
        <MetricCard label="Nível de ocupação" value={displayValue(data.level)} detail={`${Number(data.heatmap?.percentualOcupacaoGeral ?? 0).toFixed(1)}%`} />
        <MetricCard label="Conflitos RTG" value={data.conflicts?.length ?? 0} />
        <MetricCard label="Reshuffling" value={data.reshuffling?.recomendado ? 'RECOMENDADO' : 'NÃO RECOMENDADO'} detail={`${data.reshuffling?.conteinersParaReshuffling?.length ?? 0} candidato(s)`} />
        <MetricCard label="Sequência otimizada" value={data.optimizedOrders?.length ?? 0} detail="work instructions" />
      </div>
      <Section title="Heatmap de ocupação" description="A prévia usa apenas as zonas retornadas pelo backend."><HeatmapPreview heatmap={data.heatmap} /></Section>
      <div className="split-grid">
        <Section title="Plano de reshuffling" actions={canAutomate && data.reshuffling?.recomendado ? <button onClick={executeReshuffling}>Executar plano</button> : null}>
          <DetailGrid value={data.reshuffling} fields={[
            ['recomendado', 'Recomendado'],
            ['motivo', 'Motivo'],
            ['totalConteiners', 'Total de contêineres'],
            ['conteinersParaReshuffling', 'Candidatos', (value) => value.conteinersParaReshuffling?.length ?? 0]
          ]} />
          <JsonDetails value={data.reshuffling} title="Plano calculado pelo backend" />
        </Section>
        <Section title="Conflitos RTG"><DataTable rows={data.conflicts ?? []} rowKey={(row, index) => row.id ?? `${row.identificadorRtg}-${row.fila}-${index}`} columns={[
          { key: 'identificadorRtg', label: 'RTG' },
          { key: 'fila', label: 'Fila' },
          { key: 'tipoConflito', label: 'Conflito', render: (row) => <StatusBadge value={row.tipoConflito ?? row.status} /> },
          { key: 'descricao', label: 'Descrição' }
        ]} emptyTitle="Nenhum conflito RTG" /></Section>
      </div>
      <Section title="Sequência de work instructions otimizada"><DataTable rows={data.optimizedOrders ?? []} rowKey={(row) => row.id} columns={[
        { key: 'codigoConteiner', label: 'Contêiner' },
        { key: 'tipoMovimento', label: 'Movimento' },
        { key: 'statusOrdem', label: 'Status', render: (row) => <StatusBadge value={row.statusOrdem} /> },
        { key: 'linhaDestino', label: 'Linha' },
        { key: 'colunaDestino', label: 'Coluna' },
        { key: 'camadaDestino', label: 'Camada' },
        { key: 'prioridadeOperacional', label: 'Prioridade' }
      ]} emptyTitle="Nenhuma ordem pendente para otimização" /></Section>
    </>}
  </>;
}
