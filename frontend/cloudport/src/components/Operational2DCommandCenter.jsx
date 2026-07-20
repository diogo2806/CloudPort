import { useMemo, useState } from 'react';
import {
  FILL_DIRECTIONS,
  FLOW_PATTERNS,
  applyOperationalFilter,
  buildFlowPreview,
  buildQuayQueueModel,
  createWorkspacePayload,
  normalizeCheMap,
  planRailYardAssignments,
  shortestOperationalRoute,
  summarizeEcConsole,
  transferQuayQueue,
  validateGeometryDraft,
  validateWorkspacePayload
} from '../operational-2d-tools.js';
import '../operational-2d-tools.css';

const TABS = Object.freeze([
  { value: 'FLOW', label: 'Flow tools', bus: 'BUS1630' },
  { value: 'QUAY', label: 'Navio e cais', bus: 'BUS1640–1650' },
  { value: 'EC', label: 'EC Console e CHE', bus: 'BUS1660–1680' },
  { value: 'FILTERS', label: 'Filtros e recaps', bus: 'BUS1690' },
  { value: 'WORKSPACE', label: 'Workspace', bus: 'BUS1700' },
  { value: 'GEOMETRY', label: 'Geometria e rotas', bus: 'BUS1710–1720' },
  { value: 'RAIL', label: 'Rail × Yard', bus: 'BUS1730' }
]);

function array(value) {
  return Array.isArray(value) ? value : [];
}

function text(value) {
  return String(value ?? '').trim();
}

function identifier(value, fallback) {
  return text(value?.id ?? value?.codigo ?? value?.code ?? value?.codigoConteiner ?? value?.containerCode) || fallback;
}

function uniqueById(items) {
  const seen = new Set();
  return array(items).filter((item, index) => {
    const id = identifier(item, `ITEM-${index + 1}`);
    if (seen.has(id)) return false;
    seen.add(id);
    return true;
  });
}

function collectUnits(data) {
  const planSlots = array(data?.plan?.slots ?? data?.plan?.posicoes).filter((slot) => text(slot?.codigoContainer ?? slot?.containerCode));
  const yardLayers = array(data?.blocks).flatMap((block) => array(block?.stacks).flatMap((stack) => array(stack?.layers)
    .filter((layer) => text(layer?.codigoConteiner))
    .map((layer) => ({ ...layer, bloco: block?.bloco, linha: stack?.linha, coluna: stack?.coluna }))));
  const movements = array(data?.movements ?? data?.movimentos).filter((movement) => text(movement?.codigoConteiner ?? movement?.containerCode));
  const selected = data?.selectedSlot || data?.selectedStack || data?.selectedUnit;
  return uniqueById([selected, ...planSlots, ...yardLayers, ...movements].filter(Boolean));
}

function collectDestinations(data) {
  const planSlots = array(data?.plan?.slots ?? data?.plan?.posicoes).map((slot) => ({
    ...slot,
    id: identifier(slot, `B${slot?.bay}-R${slot?.rowBay ?? slot?.row}-T${slot?.tier}`)
  }));
  const yardLayers = array(data?.blocks).flatMap((block) => array(block?.stacks).flatMap((stack) => array(stack?.layers).map((layer) => ({
    ...layer,
    id: identifier(layer, `${block?.bloco}-${stack?.linha}-${stack?.coluna}-${layer?.camadaOperacional ?? layer?.camada}`),
    bloco: block?.bloco,
    linha: stack?.linha,
    coluna: stack?.coluna
  }))));
  return uniqueById([...planSlots, ...yardLayers]);
}

function collectOperations(data) {
  return array(data?.sequencing?.sequencia ?? data?.sequencia ?? data?.movements ?? data?.movimentos);
}

function collectQueues(data) {
  const explicit = array(data?.workQueues ?? data?.filas ?? data?.queues);
  if (explicit.length > 0) return explicit;
  return buildQuayQueueModel(collectOperations(data)).map((queue) => ({ ...queue, jobs: queue.operations }));
}

function collectTelemetry(data) {
  return array(data?.telemetry ?? data?.telemetria ?? data?.equipamentos);
}

function collectWagons(data) {
  return array(data?.wagons ?? data?.vagoes ?? data?.rail?.vagoes ?? data?.composicao?.vagoes);
}

function defaultGeometry(data) {
  const elements = array(data?.blocks).map((block, index) => ({
    id: text(block?.bloco) || `BLOCO-${index + 1}`,
    type: 'BLOCK',
    x: 8 + (index % 4) * 22,
    y: 10 + Math.floor(index / 4) * 24,
    width: 18,
    height: 16
  }));
  const railLines = array(data?.rail?.linhas ?? data?.linhasFerroviarias).map((line, index) => ({
    id: identifier(line, `LINHA-${index + 1}`),
    type: 'RAIL',
    x: 5,
    y: 78 + index * 5,
    width: 88,
    height: 2
  }));
  const nodes = [...elements, ...railLines];
  const edges = nodes.slice(1).map((node, index) => ({
    id: `ROTA-${index + 1}`,
    from: nodes[index].id,
    to: node.id,
    distance: 5 + index,
    congestion: 0,
    oneWay: false
  }));
  return { version: 1, state: 'RASCUNHO', elements: nodes, edges };
}

function parseIdentifiers(value, source) {
  const ids = text(value).split(/[\n,;]+/).map((item) => item.trim()).filter(Boolean);
  const index = new Map(array(source).map((item, position) => [identifier(item, `ITEM-${position + 1}`), item]));
  return ids.map((id) => index.get(id) ?? { id, codigoConteiner: id });
}

function formatInstant(value) {
  const timestamp = Date.parse(value);
  return Number.isFinite(timestamp) ? new Date(timestamp).toLocaleString('pt-BR') : 'Não calculado';
}

function Manual() {
  return <details className="operational-2d-manual">
    <summary aria-label="Abrir manual das ferramentas operacionais 2D">ⓘ Manual</summary>
    <div className="content-card">
      <h3>Finalidade da tela</h3>
      <p>Planejar, simular e operar Navio, Yard, Rail, work queues e CHEs no mesmo workspace gráfico 2D.</p>
      <h3>Fluxo operacional</h3>
      <ol><li>Selecione unidades, destinos, filas, equipamentos ou elementos físicos.</li><li>Configure o padrão, horizonte, filtros ou rota.</li><li>Revise a proposta numerada, conflitos, ETA, produtividade e estados.</li><li>Confirme o comando em lote ou cancele o rascunho sem efeitos parciais.</li></ol>
      <h3>Explicação dos campos</h3>
      <ul><li>Padrão e sentido definem a sequência stack-wise ou tier-wise.</li><li>Paired 20 agrupa pares de unidades de 20 pés.</li><li>Fila, bay, guindaste e ponto de divisão controlam a execução de cais.</li><li>Range representa a área alcançável do CHE.</li><li>Escopo define se o workspace é individual, de equipe, papel ou padrão.</li><li>Nós, ligações, bloqueios e congestionamento formam a rede de rotas.</li></ul>
      <h3>Permissões necessárias</h3>
      <p>Consulta: perfis operacionais. Planejamento e publicação: PLANEJADOR ou ADMIN_PORTO. Dispatch, transferência de filas e alteração de alcance exigem as permissões específicas do domínio.</p>
      <h3>Estados possíveis</h3>
      <p>Rascunho, proposta, tentativo, definitivo, reservado, atribuído, despachado, em execução, bloqueado, falha, concluído e publicado.</p>
      <h3>Motivos de bloqueio</h3>
      <p>Destino insuficiente, paired 20 incompatível, fila inexistente, telemetria stale, CHE fora do alcance, geometria inválida, rota interrompida, conflito ferroviário, permissão insuficiente ou versão concorrente.</p>
      <h3>Exemplo</h3>
      <p>Selecione seis unidades, aplique stack-wise com alternância de bays, confira a projeção das filas e envie um único comando transacional após validar CHE, rota e vagão.</p>
      <h3>Atalhos</h3>
      <ul><li>Ctrl ou Cmd + Enter: confirmar a proposta da aba atual.</li><li>Esc: cancelar rascunho ou seleção.</li><li>F1 ou Shift + ?: abrir a ajuda contextual da página.</li></ul>
      <p><a href="https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/operacao-grafica-2d-xps.md" target="_blank" rel="noreferrer">Abrir processo completo</a></p>
    </div>
  </details>;
}

function FlowPanel({ units, destinations, onCommand }) {
  const [unitText, setUnitText] = useState(() => units.slice(0, 8).map((item, index) => identifier(item, `UNIDADE-${index + 1}`)).join('\n'));
  const [destinationText, setDestinationText] = useState(() => destinations.slice(0, 8).map((item, index) => identifier(item, `DESTINO-${index + 1}`)).join('\n'));
  const [pattern, setPattern] = useState('STACK_WISE');
  const [direction, setDirection] = useState('FORWARD');
  const [paired20, setPaired20] = useState(false);
  const [alternateBays, setAlternateBays] = useState(false);
  const [status, setStatus] = useState('RASCUNHO');
  const preview = useMemo(() => buildFlowPreview({
    units: parseIdentifiers(unitText, units),
    destinations: parseIdentifiers(destinationText, destinations),
    pattern,
    direction,
    paired20,
    alternateBays
  }), [unitText, destinationText, units, destinations, pattern, direction, paired20, alternateBays]);

  function confirm() {
    if (!preview.valid) return;
    onCommand({ type: 'APLICAR_FLUXO_PLANEJAMENTO', version: 1, commandId: crypto.randomUUID(), ...preview.summary, moves: preview.moves });
    setStatus('ENCAMINHADO');
  }

  return <section className="operational-2d-panel">
    <header><div><strong>Planejamento em lote</strong><small>Sequência numerada e confirmação única</small></div><span>{status}</span></header>
    <div className="operational-2d-form-grid">
      <label>Unidades<textarea value={unitText} onChange={(event) => { setUnitText(event.target.value); setStatus('RASCUNHO'); }} /></label>
      <label>Destinos<textarea value={destinationText} onChange={(event) => { setDestinationText(event.target.value); setStatus('RASCUNHO'); }} /></label>
      <label>Padrão<select value={pattern} onChange={(event) => setPattern(event.target.value)}>{FLOW_PATTERNS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
      <label>Sentido<select value={direction} onChange={(event) => setDirection(event.target.value)}>{FILL_DIRECTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
      <label className="operational-2d-check"><input type="checkbox" checked={paired20} onChange={(event) => setPaired20(event.target.checked)} />Paired 20</label>
      <label className="operational-2d-check"><input type="checkbox" checked={alternateBays} onChange={(event) => setAlternateBays(event.target.checked)} />Alternar bays</label>
    </div>
    {preview.errors.length > 0 && <ul className="operational-2d-errors">{preview.errors.map((error) => <li key={error}>{error}</li>)}</ul>}
    <div className="operational-flow-preview">{preview.moves.map((move) => <article key={`${move.sequence}-${move.unitId}`}><span>{move.sequence}</span><strong>{move.unitId}</strong><small>→ {move.destinationId}</small>{move.pairGroup && <em>{move.pairGroup}</em>}</article>)}</div>
    <footer><span>{preview.moves.length} movimento(s)</span><div><button type="button" className="secondary" onClick={() => setStatus('CANCELADO')}>Cancelar lote</button><button type="button" disabled={!preview.valid} onClick={confirm}>Confirmar lote atomicamente</button></div></footer>
  </section>;
}

function QuayPanel({ operations, onCommand }) {
  const initial = useMemo(() => buildQuayQueueModel(operations), [operations]);
  const [queues, setQueues] = useState(initial);
  const [source, setSource] = useState(initial[0]?.id ?? '');
  const [target, setTarget] = useState('QC-02');
  const [splitAt, setSplitAt] = useState(0);
  const [message, setMessage] = useState('');

  function transfer() {
    const result = transferQuayQueue(queues, source, target, splitAt);
    if (!result.valid) {
      setMessage(result.error);
      return;
    }
    setQueues(result.queues);
    setSource(result.queues[0]?.id ?? '');
    setMessage(`${result.moved} operação(ões) transferida(s) na simulação.`);
  }

  function confirm() {
    onCommand({ type: 'REPROGRAMAR_QUAY_COMMANDER', commandId: crypto.randomUUID(), queues });
    setMessage('Programação encaminhada para confirmação transacional.');
  }

  return <section className="operational-2d-panel">
    <header><div><strong>Perfil do navio e Quay Commander</strong><small>Work queues, bays, guindastes e término previsto</small></div><span>{queues.length} fila(s)</span></header>
    <div className="operational-quay-grid">{queues.map((queue) => <article key={queue.id} draggable onDragStart={(event) => event.dataTransfer.setData('text/plain', queue.id)} onDrop={(event) => { event.preventDefault(); setSource(event.dataTransfer.getData('text/plain')); setTarget(queue.crane); }} onDragOver={(event) => event.preventDefault()}>
      <header><strong>Bay {queue.bay}</strong><span>{queue.crane}</span></header>
      <dl><div><dt>Planejado</dt><dd>{queue.planned}</dd></div><div><dt>Restante</dt><dd>{queue.remaining}</dd></div><div><dt>Término</dt><dd>{formatInstant(queue.projectedEnd)}</dd></div></dl>
      {queue.blockedReason && <p>{queue.blockedReason}</p>}
      <ol>{queue.operations.slice(0, 12).map((operation) => <li key={operation.id}>{operation.sequence}. {identifier(operation, operation.id)}</li>)}</ol>
    </article>)}</div>
    <div className="operational-quay-actions"><label>Fila<select value={source} onChange={(event) => setSource(event.target.value)}>{queues.map((queue) => <option key={queue.id} value={queue.id}>{queue.id}</option>)}</select></label><label>Guindaste destino<input value={target} onChange={(event) => setTarget(event.target.value)} /></label><label>Dividir após<input type="number" min="0" value={splitAt} onChange={(event) => setSplitAt(event.target.value)} /></label><button type="button" className="secondary" onClick={transfer}>Simular divisão/transferência</button><button type="button" onClick={confirm}>Persistir programação</button></div>
    {message && <p>{message}</p>}
  </section>;
}

function EcPanel({ queues, telemetry, onCommand }) {
  const che = useMemo(() => normalizeCheMap(telemetry), [telemetry]);
  const recap = useMemo(() => summarizeEcConsole(queues, telemetry), [queues, telemetry]);
  const [ranges, setRanges] = useState(() => Object.fromEntries(che.map((item) => [item.id, item.range])));
  const [selected, setSelected] = useState(che[0]?.id ?? '');
  const active = che.find((item) => item.id === selected);

  return <section className="operational-2d-panel">
    <header><div><strong>EC Console gráfico e mapa de CHE</strong><small>POWs, pools, jobs, telemetria, trilha, rota e alcance</small></div><span>{recap.activeChe} ativo(s)</span></header>
    <div className="operational-recap-grid">{Object.entries(recap).map(([key, value]) => <article key={key}><span>{key}</span><strong>{value}</strong></article>)}</div>
    <div className="operational-che-map" role="img" aria-label="Mapa 2D dos equipamentos de movimentação">
      {che.map((item) => <button type="button" key={item.id} className={`operational-che ${item.stale ? 'stale' : ''} ${selected === item.id ? 'selected' : ''}`} style={{ left: `${Math.abs(item.x) % 92}%`, top: `${Math.abs(item.y) % 86}%`, transform: `rotate(${item.heading}deg)` }} onClick={() => setSelected(item.id)} title={`${item.label} · ${item.state} · ${item.stale ? 'telemetria atrasada' : 'posição atual'}`}>
        <span className="operational-che-range" style={{ width: `${Math.min(180, ranges[item.id] ?? item.range)}px`, height: `${Math.min(180, ranges[item.id] ?? item.range)}px` }} />
        <strong>▲</strong><small>{item.label}</small>
      </button>)}
    </div>
    {active && <div className="operational-che-inspector"><dl><div><dt>Equipamento</dt><dd>{active.label}</dd></div><div><dt>Estado</dt><dd>{active.state}</dd></div><div><dt>Conectividade</dt><dd>{active.connected ? 'Conectado' : 'Desconectado'}</dd></div><div><dt>Telemetria</dt><dd>{active.stale ? `Atrasada ${active.ageSeconds}s` : `Atual ${active.ageSeconds}s`}</dd></div><div><dt>Unidade</dt><dd>{active.carriedUnit || '—'}</dd></div><div><dt>Job atual</dt><dd>{active.currentJob || '—'}</dd></div></dl><label>Alcance operacional<input type="range" min="10" max="180" value={ranges[active.id] ?? active.range} onChange={(event) => setRanges((current) => ({ ...current, [active.id]: Number(event.target.value) }))} /></label><button type="button" disabled={active.stale} onClick={() => onCommand({ type: 'ALTERAR_ALCANCE_CHE', commandId: crypto.randomUUID(), equipmentId: active.id, rangeMeters: ranges[active.id] ?? active.range })}>Salvar alcance e recalcular cobertura</button></div>}
  </section>;
}

function FiltersPanel({ elements }) {
  const [filters, setFilters] = useState({ query: '', state: '', domain: '', equipment: '' });
  const filtered = useMemo(() => applyOperationalFilter(elements, filters), [elements, filters]);
  const highlighted = filtered.filter((item) => item.highlighted);
  return <section className="operational-2d-panel">
    <header><div><strong>Filtros, recaps e desenho bidirecional</strong><small>A seleção acinzenta o restante sem remover o contexto</small></div><span>{highlighted.length}/{filtered.length}</span></header>
    <div className="operational-filter-bar"><label>Busca<input value={filters.query} onChange={(event) => setFilters((current) => ({ ...current, query: event.target.value }))} /></label><label>Estado<input value={filters.state} onChange={(event) => setFilters((current) => ({ ...current, state: event.target.value }))} /></label><label>Domínio<input value={filters.domain} onChange={(event) => setFilters((current) => ({ ...current, domain: event.target.value }))} /></label><label>CHE<input value={filters.equipment} onChange={(event) => setFilters((current) => ({ ...current, equipment: event.target.value }))} /></label></div>
    <div className="operational-filter-results">{filtered.slice(0, 100).map((item, index) => <button type="button" key={identifier(item, `FILTER-${index}`)} className={item.dimmed ? 'dimmed' : 'highlighted'}><strong>{identifier(item, `Elemento ${index + 1}`)}</strong><small>{text(item.domain) || 'operacional'} · {text(item.state ?? item.status) || 'sem estado'}</small></button>)}</div>
  </section>;
}

function WorkspacePanel({ scope, onCommand }) {
  const [workspace, setWorkspace] = useState(() => createWorkspacePayload({ name: `Operação 2D · ${scope}`, scope: 'INDIVIDUAL', panels: TABS.map((tab, index) => ({ id: tab.value, x: index % 3, y: Math.floor(index / 3), width: 4, height: 3 })) }));
  const [message, setMessage] = useState('');
  const validation = validateWorkspacePayload(workspace);

  async function save() {
    if (!validation.valid) return;
    const command = { type: 'SALVAR_WORKSPACE_2D', commandId: crypto.randomUUID(), workspace: validation.payload };
    onCommand(command);
    setWorkspace((current) => ({ ...current, version: current.version + 1, updatedAt: new Date().toISOString() }));
    setMessage('Workspace versionado encaminhado ao servidor.');
  }

  function exportWorkspace() {
    const blob = new Blob([JSON.stringify(validation.payload, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `${validation.payload.name.replace(/[^a-z0-9]+/gi, '-').toLowerCase()}.json`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  function importWorkspace(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    file.text().then((content) => {
      const imported = validateWorkspacePayload(JSON.parse(content));
      if (!imported.valid) throw new Error(imported.errors.join(' '));
      setWorkspace(imported.payload);
      setMessage('Workspace importado. Salve para criar uma nova versão no servidor.');
    }).catch((error) => setMessage(error.message));
  }

  return <section className="operational-2d-panel">
    <header><div><strong>Paletas, layouts e workspaces</strong><small>Versionado, compartilhável, importável e exportável</small></div><span>v{workspace.version}</span></header>
    <div className="operational-workspace-form"><label>Nome<input value={workspace.name} onChange={(event) => setWorkspace((current) => ({ ...current, name: event.target.value }))} /></label><label>Escopo<select value={workspace.scope} onChange={(event) => setWorkspace((current) => ({ ...current, scope: event.target.value }))}><option value="INDIVIDUAL">Individual</option><option value="EQUIPE">Equipe</option><option value="PAPEL">Papel</option><option value="PADRAO">Padrão administrativo</option></select></label>{workspace.scope === 'PAPEL' && <label>Papel<input value={workspace.role ?? ''} onChange={(event) => setWorkspace((current) => ({ ...current, role: event.target.value }))} /></label>}<label>Cor principal<input type="color" value={workspace.palette?.primary ?? '#1b5faa'} onChange={(event) => setWorkspace((current) => ({ ...current, palette: { ...current.palette, primary: event.target.value } }))} /></label></div>
    {validation.errors.length > 0 && <ul className="operational-2d-errors">{validation.errors.map((error) => <li key={error}>{error}</li>)}</ul>}
    <div className="operational-workspace-layout">{workspace.panels.map((panel) => <article key={panel.id}><strong>{TABS.find((tab) => tab.value === panel.id)?.label ?? panel.id}</strong><label><input type="checkbox" checked={panel.visible} onChange={(event) => setWorkspace((current) => ({ ...current, panels: current.panels.map((item) => item.id === panel.id ? { ...item, visible: event.target.checked } : item) }))} />Visível</label></article>)}</div>
    <footer><span>{message}</span><div><label className="secondary file-button">Importar<input type="file" accept="application/json" onChange={importWorkspace} /></label><button type="button" className="secondary" onClick={exportWorkspace}>Exportar</button><button type="button" disabled={!validation.valid} onClick={save}>Salvar no servidor</button></div></footer>
  </section>;
}

function GeometryPanel({ initialGeometry, onCommand }) {
  const [geometry, setGeometry] = useState(initialGeometry);
  const [newId, setNewId] = useState('');
  const [newType, setNewType] = useState('BLOCK');
  const [start, setStart] = useState(initialGeometry.elements[0]?.id ?? '');
  const [end, setEnd] = useState(initialGeometry.elements.at(-1)?.id ?? '');
  const [blockedEdges, setBlockedEdges] = useState([]);
  const validation = validateGeometryDraft(geometry);
  const route = shortestOperationalRoute(geometry.elements, geometry.edges, start, end, blockedEdges);

  function addElement() {
    const id = text(newId);
    if (!id) return;
    setGeometry((current) => ({ ...current, elements: [...current.elements, { id, type: newType, x: 12 + current.elements.length * 8, y: 18 + current.elements.length * 5, width: 12, height: 8 }] }));
    setNewId('');
  }

  function publish() {
    if (!validation.valid) return;
    onCommand({ type: 'PUBLICAR_GEOMETRIA_2D', commandId: crypto.randomUUID(), geometry: { ...geometry, version: geometry.version + 1, state: 'PUBLICADA' } });
    setGeometry((current) => ({ ...current, version: current.version + 1, state: 'PUBLICADA' }));
  }

  return <section className="operational-2d-panel">
    <header><div><strong>Editor físico e rede de rotas</strong><small>Blocos, trilhos, vias, sentidos, bloqueios, congestionamento e ETA</small></div><span>v{geometry.version} · {geometry.state}</span></header>
    <div className="operational-geometry-canvas">{geometry.edges.map((edge) => <button type="button" key={edge.id} className={blockedEdges.includes(edge.id) ? 'route-edge blocked' : 'route-edge'} onClick={() => setBlockedEdges((current) => current.includes(edge.id) ? current.filter((id) => id !== edge.id) : [...current, edge.id])}>{edge.id}<small>{edge.oneWay ? '→' : '↔'} · {edge.congestion ?? 0}</small></button>)}{geometry.elements.map((element) => <article key={element.id} className={`geometry-${element.type.toLowerCase()}`} style={{ left: `${element.x}%`, top: `${element.y}%`, width: `${element.width}%`, minHeight: `${element.height * 3}px` }}><strong>{element.id}</strong><small>{element.type}</small></article>)}</div>
    <div className="operational-geometry-actions"><label>ID<input value={newId} onChange={(event) => setNewId(event.target.value)} /></label><label>Tipo<select value={newType} onChange={(event) => setNewType(event.target.value)}><option>BLOCK</option><option>ROAD</option><option>RAIL</option><option>EXCHANGE_AREA</option><option>TRANSFER_POINT</option><option>REEFER</option><option>ZONE</option></select></label><button type="button" className="secondary" onClick={addElement}>Adicionar elemento</button><label>Origem<select value={start} onChange={(event) => setStart(event.target.value)}>{geometry.elements.map((element) => <option key={element.id}>{element.id}</option>)}</select></label><label>Destino<select value={end} onChange={(event) => setEnd(event.target.value)}>{geometry.elements.map((element) => <option key={element.id}>{element.id}</option>)}</select></label></div>
    {validation.errors.length > 0 && <ul className="operational-2d-errors">{validation.errors.map((error) => <li key={error}>{error}</li>)}</ul>}
    <div className={route.reachable ? 'operational-route-result' : 'operational-route-result blocked'}>{route.reachable ? <><strong>Rota prevista: {route.path.join(' → ')}</strong><span>ETA {route.etaMinutes} min · custo {route.cost.toFixed(2)}</span></> : <strong>{route.reason}</strong>}</div>
    <footer><span>{blockedEdges.length} interdição(ões) simulada(s)</span><button type="button" disabled={!validation.valid} onClick={publish}>Validar e publicar versão</button></footer>
  </section>;
}

function RailPanel({ units, wagons, onCommand }) {
  const plan = useMemo(() => planRailYardAssignments(units, wagons), [units, wagons]);
  return <section className="operational-2d-panel">
    <header><div><strong>Canvas Rail × Yard × Dispatch</strong><small>Unidade, vagão, WI, CHE, sequência e conflitos</small></div><span>{plan.valid ? 'PRONTO' : 'BLOQUEADO'}</span></header>
    <div className="operational-rail-canvas"><div><h4>Pátio</h4>{units.slice(0, 30).map((unit, index) => <article key={identifier(unit, `UNIT-${index}`)} draggable><strong>{identifier(unit, `Unidade ${index + 1}`)}</strong><small>{text(unit?.workInstructionId ?? unit?.ordemId) || 'Sem WI'} · {text(unit?.equipamento ?? unit?.che) || 'Sem CHE'}</small></article>)}</div><div><h4>Composição ferroviária</h4>{wagons.map((wagon, index) => <article key={identifier(wagon, `WAGON-${index}`)}><strong>{identifier(wagon, `Vagão ${index + 1}`)}</strong><small>{text(wagon?.linha ?? wagon?.line) || 'Linha não informada'} · posição {text(wagon?.posicao ?? wagon?.position) || '—'}</small>{plan.assignments.filter((assignment) => assignment.wagonId === identifier(wagon, '')).map((assignment) => <span key={assignment.unitId}>{assignment.sequence}. {assignment.unitId}</span>)}</article>)}</div></div>
    {plan.conflicts.length > 0 && <ul className="operational-2d-errors">{plan.conflicts.map((conflict) => <li key={conflict}>{conflict}</li>)}</ul>}
    <footer><span>{plan.assignments.length} atribuição(ões)</span><button type="button" disabled={!plan.valid} onClick={() => onCommand({ type: 'CONFIRMAR_PLANO_RAIL_YARD', commandId: crypto.randomUUID(), assignments: plan.assignments })}>Confirmar plano integrado</button></footer>
  </section>;
}

export function Operational2DCommandCenter({ data = {}, scope = 'Operação integrada', onCommand }) {
  const [tab, setTab] = useState('FLOW');
  const [lastCommand, setLastCommand] = useState(null);
  const units = useMemo(() => collectUnits(data), [data]);
  const destinations = useMemo(() => collectDestinations(data), [data]);
  const operations = useMemo(() => collectOperations(data), [data]);
  const queues = useMemo(() => collectQueues(data), [data]);
  const telemetry = useMemo(() => collectTelemetry(data), [data]);
  const wagons = useMemo(() => collectWagons(data), [data]);
  const geometry = useMemo(() => defaultGeometry(data), [data]);
  const filterElements = useMemo(() => [
    ...units.map((item) => ({ ...item, domain: 'yard', state: item?.estado ?? item?.status, equipment: item?.equipamento ?? item?.che })),
    ...operations.map((item) => ({ ...item, domain: 'vessel', state: item?.estado ?? item?.status, equipment: item?.guindasteId ?? item?.equipamento })),
    ...normalizeCheMap(telemetry).map((item) => ({ ...item, domain: 'equipment', equipment: item.id })),
    ...wagons.map((item) => ({ ...item, domain: 'rail', state: item?.estado ?? item?.status }))
  ], [units, operations, telemetry, wagons]);

  function command(value) {
    setLastCommand(value);
    onCommand?.(value);
    data?.onOperational2DCommand?.(value);
  }

  return <section className="operational-2d-command-center" onKeyDown={(event) => {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') event.currentTarget.querySelector('.operational-2d-panel footer button:not(:disabled):last-child')?.click();
    if (event.key === 'Escape') setLastCommand(null);
  }} tabIndex={0}>
    <header className="operational-2d-command-header"><div><span>BUS1630–BUS1730</span><h2>Interface gráfica 2D operacional</h2><p>{scope}</p></div><Manual /></header>
    <nav className="operational-2d-tabs" aria-label="Ferramentas operacionais 2D">{TABS.map((item) => <button type="button" key={item.value} className={tab === item.value ? 'active' : 'secondary'} onClick={() => setTab(item.value)}><span>{item.bus}</span>{item.label}</button>)}</nav>
    {tab === 'FLOW' && <FlowPanel units={units} destinations={destinations} onCommand={command} />}
    {tab === 'QUAY' && <QuayPanel operations={operations} onCommand={command} />}
    {tab === 'EC' && <EcPanel queues={queues} telemetry={telemetry} onCommand={command} />}
    {tab === 'FILTERS' && <FiltersPanel elements={filterElements} />}
    {tab === 'WORKSPACE' && <WorkspacePanel scope={scope} onCommand={command} />}
    {tab === 'GEOMETRY' && <GeometryPanel initialGeometry={geometry} onCommand={command} />}
    {tab === 'RAIL' && <RailPanel units={units} wagons={wagons} onCommand={command} />}
    {lastCommand && <aside className="operational-command-receipt" aria-live="polite"><strong>Comando preparado</strong><span>{lastCommand.type}</span><small>{lastCommand.commandId}</small></aside>}
  </section>;
}
