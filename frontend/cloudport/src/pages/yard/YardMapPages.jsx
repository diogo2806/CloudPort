import { useMemo, useState } from 'react';
import { api, sanitizeText } from '../../api.js';
import { DataTable, EmptyState, JsonDetails, Loading, Message, MetricCard, Section, StatusBadge } from '../../components.jsx';
import { buildStacks, DetailGrid, displayValue, FilterField, normalized, Pagination, positionKey, stackClass, usePagination, useRemote, YardPageHeader } from './YardShared.jsx';

export function YardMapPage({ navigate }) {
  const [filters, setFilters] = useState({ status: '', tipoCarga: '', destino: '', camada: '', tipoEquipamento: '' });
  const [selectedStack, setSelectedStack] = useState(null);
  const remote = useRemote(async () => {
    const query = Object.fromEntries(Object.entries(filters).filter(([, value]) => value));
    const [map, positions, availableFilters, orders] = await Promise.all([
      api.obterMapaPatio(query),
      api.listarPosicoesReservaveisPatio(),
      api.obterFiltrosMapaPatio(),
      api.listarOrdensPatio()
    ]);
    return { map: map ?? {}, positions: positions ?? [], filters: availableFilters ?? {}, orders: orders ?? [] };
  }, [filters.status, filters.tipoCarga, filters.destino, filters.camada, filters.tipoEquipamento]);
  const stacks = useMemo(() => buildStacks(remote.data?.positions, remote.data?.orders), [remote.data]);
  const map = remote.data?.map ?? {};
  const containers = map.conteineres ?? [];
  const equipment = map.equipamentos ?? [];
  const selectedContainer = selectedStack?.layers.map((layer) => containers.find((container) => container.codigo === layer.codigoConteiner)).find(Boolean);
  const selectedEquipment = selectedStack ? equipment.filter((item) => item.linha === selectedStack.linha && item.coluna === selectedStack.coluna) : [];

  function optionList(key) {
    return remote.data?.filters?.[key] ?? [];
  }

  return <>
    <YardPageHeader path="/home/patio/mapa" navigate={navigate} title="Mapa operacional" description="Estrutura real do pátio por bloco, linha, coluna e camada, com ocupação, restrições, destinos planejados e equipamentos persistidos." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error}</Message>
    <Section title="Filtros do mapa" description="Os valores são fornecidos pelo backend do Yard."><div className="filter-grid">
      <FilterField label="Status do contêiner"><select value={filters.status} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))}><option value="">Todos</option>{optionList('statusDisponiveis').map((value) => <option key={value}>{value}</option>)}</select></FilterField>
      <FilterField label="Tipo de carga"><select value={filters.tipoCarga} onChange={(event) => setFilters((current) => ({ ...current, tipoCarga: event.target.value }))}><option value="">Todos</option>{optionList('tiposCargaDisponiveis').map((value) => <option key={value}>{value}</option>)}</select></FilterField>
      <FilterField label="Destino"><select value={filters.destino} onChange={(event) => setFilters((current) => ({ ...current, destino: event.target.value }))}><option value="">Todos</option>{optionList('destinosDisponiveis').map((value) => <option key={value}>{value}</option>)}</select></FilterField>
      <FilterField label="Camada"><select value={filters.camada} onChange={(event) => setFilters((current) => ({ ...current, camada: event.target.value }))}><option value="">Todas</option>{optionList('camadasOperacionaisDisponiveis').map((value) => <option key={value}>{value}</option>)}</select></FilterField>
      <FilterField label="Equipamento"><select value={filters.tipoEquipamento} onChange={(event) => setFilters((current) => ({ ...current, tipoEquipamento: event.target.value }))}><option value="">Todos</option>{optionList('tiposEquipamentoDisponiveis').map((value) => <option key={value}>{value}</option>)}</select></FilterField>
    </div></Section>
    {remote.loading ? <Loading label="Carregando mapa e restrições..." /> : <>
      <div className="metrics-grid"><MetricCard label="Blocos" value={stacks.length} /><MetricCard label="Posições reais" value={remote.data?.positions.length ?? 0} /><MetricCard label="Contêineres" value={containers.length} /><MetricCard label="Equipamentos" value={equipment.length} /></div>
      <div className="yard-map-layout">
        <Section title="Blocos e pilhas" description="Clique em uma pilha para abrir suas camadas, restrições, unidade e equipamentos.">
          {!stacks.length ? <EmptyState title="Nenhuma posição real disponível" /> : <div className="yard-block-grid">{stacks.map((block) => <article className="yard-block" key={block.bloco}><header><strong>{block.bloco}</strong><span>{block.stacks.length} pilha(s)</span></header><div className="yard-stack-grid">{block.stacks.map((stack) => <button type="button" className={`yard-stack ${stackClass(stack)} ${selectedStack?.bloco === stack.bloco && selectedStack?.linha === stack.linha && selectedStack?.coluna === stack.coluna ? 'selected' : ''}`} key={`${stack.bloco}-${stack.linha}-${stack.coluna}`} onClick={() => setSelectedStack(stack)}><strong>L{stack.linha} · C{stack.coluna}</strong><span>{stack.layers.filter((layer) => layer.ocupada).length}/{stack.layers.length} ocupadas</span><div className="yard-layers">{stack.layers.map((layer) => <i key={layer.id ?? positionKey(layer)} className={layer.interditada ? 'interdicted' : layer.bloqueada || !layer.areaPermitida ? 'blocked' : layer.plannedOrder ? 'reserved' : layer.ocupada ? 'occupied' : 'available'} title={`${layer.camadaOperacional}: ${layer.codigoConteiner || 'livre'}`}>{layer.camadaOperacional}</i>)}</div></button>)}</div></article>)}</div>}
        </Section>
        <aside className="yard-detail-column"><Section title="Detalhe da pilha">
          {!selectedStack ? <EmptyState title="Selecione uma pilha" /> : <><DetailGrid value={selectedStack} fields={[["bloco", "Bloco"], ["linha", "Linha"], ["coluna", "Coluna"], ["layers", "Camadas", (value) => value.layers.length]]} /><div className="layer-detail-list">{selectedStack.layers.map((layer) => <article key={layer.id ?? positionKey(layer)}><div><strong>{layer.camadaOperacional}</strong><StatusBadge value={layer.interditada ? 'INTERDITADA' : layer.bloqueada ? 'BLOQUEADA' : layer.ocupada ? 'OCUPADA' : layer.plannedOrder ? 'DESTINO_PLANEJADO' : 'DISPONIVEL'} /></div><small>{layer.codigoConteiner || 'Sem contêiner'} · capacidade {layer.ocupacaoPilha ?? 0}/{layer.capacidadePilha ?? '—'}</small><small>Cargas: {displayValue(layer.tiposCargaPermitidos)} · peso máx. {displayValue(layer.pesoMaximoToneladas)} t</small></article>)}</div><JsonDetails value={{ pilha: selectedStack, conteiner: selectedContainer, equipamentos: selectedEquipment }} title="Contrato persistido completo" /></>}
        </Section></aside>
      </div>
      {!!map.alertas?.length && <Section title="Alertas do mapa"><div className="card-list">{map.alertas.map((alert, index) => <article className="content-card" key={`${alert.tipoAlerta}-${index}`}><div className="card-meta"><StatusBadge value={alert.nivelSeveridade} /><span>{sanitizeText(alert.tipoAlerta)}</span></div><h3>{sanitizeText(alert.mensagem)}</h3><p>{sanitizeText(alert.recomendacao)}</p></article>)}</div></Section>}
    </>}
  </>;
}

export function YardPositionsPage({ navigate }) {
  const [query, setQuery] = useState('');
  const [block, setBlock] = useState('');
  const [availability, setAvailability] = useState('');
  const [selected, setSelected] = useState(null);
  const remote = useRemote(() => api.listarPosicoesReservaveisPatio(), []);
  const rows = useMemo(() => (remote.data ?? []).filter((position) => {
    const matchesQuery = !query || normalized(`${position.codigoConteiner} ${position.linha} ${position.coluna} ${position.camadaOperacional}`).includes(normalized(query));
    const matchesBlock = !block || position.bloco === block;
    const state = position.interditada ? 'INTERDITADA' : position.bloqueada || !position.areaPermitida ? 'BLOQUEADA' : position.ocupada ? 'OCUPADA' : 'DISPONIVEL';
    return matchesQuery && matchesBlock && (!availability || state === availability);
  }), [remote.data, query, block, availability]);
  const blocks = useMemo(() => Array.from(new Set((remote.data ?? []).map((position) => position.bloco).filter(Boolean))).sort(), [remote.data]);
  const paged = usePagination(rows);
  return <>
    <YardPageHeader path="/home/patio/posicoes" navigate={navigate} title="Posições do pátio" description="Consulta navegável das posições reais, capacidade da pilha e restrições de placement usadas pelo Yard." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error}</Message>
    <Section title="Filtros"><div className="filter-grid"><FilterField label="Busca"><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Contêiner, linha, coluna ou camada" /></FilterField><FilterField label="Bloco"><select value={block} onChange={(event) => setBlock(event.target.value)}><option value="">Todos</option>{blocks.map((value) => <option key={value}>{value}</option>)}</select></FilterField><FilterField label="Disponibilidade"><select value={availability} onChange={(event) => setAvailability(event.target.value)}><option value="">Todas</option>{['DISPONIVEL', 'OCUPADA', 'BLOQUEADA', 'INTERDITADA'].map((value) => <option key={value}>{value}</option>)}</select></FilterField></div></Section>
    <div className="split-grid"><Section title={`Posições (${rows.length})`}>{remote.loading ? <Loading /> : <><DataTable rows={paged.rows} rowKey={(row) => row.id} onRowClick={setSelected} columns={[
      { key: 'bloco', label: 'Bloco' }, { key: 'linha', label: 'Linha' }, { key: 'coluna', label: 'Coluna' }, { key: 'camadaOperacional', label: 'Camada' }, { key: 'ocupada', label: 'Situação', render: (row) => <StatusBadge value={row.interditada ? 'INTERDITADA' : row.bloqueada || !row.areaPermitida ? 'BLOQUEADA' : row.ocupada ? 'OCUPADA' : 'DISPONIVEL'} /> }, { key: 'codigoConteiner', label: 'Contêiner' }, { key: 'ocupacaoPilha', label: 'Pilha', render: (row) => `${row.ocupacaoPilha ?? 0}/${row.capacidadePilha ?? '—'}` }
    ]} emptyTitle="Nenhuma posição corresponde aos filtros" /><Pagination page={paged.page} totalPages={paged.totalPages} totalRows={rows.length} onChange={paged.setPage} /></>}</Section><Section title="Detalhes e restrições"><DetailGrid value={selected} fields={[["id", "ID"], ["bloco", "Bloco"], ["linha", "Linha"], ["coluna", "Coluna"], ["camadaOperacional", "Camada"], ["codigoConteiner", "Contêiner"], ["statusConteiner", "Status do contêiner"], ["tiposCargaPermitidos", "Cargas permitidas"], ["pesoMaximoToneladas", "Peso máximo (t)"], ["alturaMaximaMetros", "Altura máxima (m)"], ["camadaMaxima", "Camada máxima"], ["capacidadePilha", "Capacidade da pilha"], ["ocupacaoPilha", "Ocupação da pilha"]]} /></Section></div>
  </>;
}
