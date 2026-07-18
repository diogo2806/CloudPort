import { useEffect, useMemo, useRef, useState } from 'react';
import {
  applyGridFilters,
  buildGridCsv,
  DEFAULT_PAGE_SIZE_OPTIONS,
  FILTER_OPERATORS,
  moveColumn,
  normalizeGridValue,
  paginateGridRows,
  reconcileColumnLayout,
  resolveRowKey,
  sortGridRows
} from './operationalDataGrid.js';
import './operational-data-grid.css';

function readStorage(key, fallback) {
  if (typeof window === 'undefined') return fallback;
  try {
    const value = window.localStorage.getItem(key);
    return value ? JSON.parse(value) : fallback;
  } catch {
    return fallback;
  }
}

function writeStorage(key, value) {
  if (typeof window === 'undefined') return;
  try { window.localStorage.setItem(key, JSON.stringify(value)); }
  catch { /* Preferências locais são opcionais. */ }
}

function SelectionCheckbox({ checked, indeterminate = false, onChange, label }) {
  const ref = useRef(null);
  useEffect(() => {
    if (ref.current) ref.current.indeterminate = indeterminate;
  }, [indeterminate]);
  return <input ref={ref} className="grid-checkbox" type="checkbox" checked={checked} onChange={onChange} aria-label={label} />;
}

function operatorNeedsValue(operator) {
  return !['isEmpty', 'isNotEmpty'].includes(operator);
}

function safeFileName(value) {
  const normalized = String(value || 'registros-operacionais').normalize('NFD').replace(/[\u0300-\u036f]/g, '');
  return normalized.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'registros-operacionais';
}

function downloadCsv(csv, fileName) {
  if (typeof document === 'undefined') return;
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${safeFileName(fileName)}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function Inspector({ row, columns, onClose }) {
  if (!row) return null;
  return <div className="grid-inspector-backdrop" onMouseDown={onClose}>
    <aside className="grid-inspector" role="dialog" aria-modal="true" aria-labelledby="grid-inspector-title" onMouseDown={(event) => event.stopPropagation()}>
      <header><div><span>Inspector operacional</span><h2 id="grid-inspector-title">Detalhes do registro</h2></div><button className="icon-button" type="button" aria-label="Fechar inspector" onClick={onClose}>×</button></header>
      <div className="grid-inspector-fields">
        {columns.map((column) => <div key={column.key}><span>{column.label}</span><strong>{normalizeGridValue(column.inspectorValue ? column.inspectorValue(row) : column.value ? column.value(row) : row?.[column.key]) || '—'}</strong></div>)}
      </div>
    </aside>
  </div>;
}

export function OperationalDataGrid({
  rows,
  columns,
  rowKey = 'id',
  emptyTitle = 'Nenhum registro encontrado',
  onRowClick,
  inspectable,
  selectable = true,
  bulkActions = [],
  gridId,
  exportFileName,
  pageSizeOptions = DEFAULT_PAGE_SIZE_OPTIONS,
  defaultPageSize = DEFAULT_PAGE_SIZE_OPTIONS[0],
  serverState
}) {
  const safeRows = Array.isArray(rows) ? rows : [];
  const safeColumns = Array.isArray(columns) ? columns : [];
  const columnSignature = safeColumns.map((column) => column.key).join('|');
  const storageKey = useMemo(() => {
    const route = typeof window !== 'undefined' ? window.location.pathname : 'global';
    return `cloudport:operational-grid:${gridId || route}:${columnSignature}`;
  }, [gridId, columnSignature]);
  const viewsStorageKey = `${storageKey}:views`;

  const [query, setQuery] = useState('');
  const [filters, setFilters] = useState([]);
  const [filterDraft, setFilterDraft] = useState({ columnKey: safeColumns[0]?.key ?? '', operator: 'contains', value: '' });
  const [sort, setSort] = useState(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(defaultPageSize);
  const [selectedRowsByKey, setSelectedRowsByKey] = useState(() => new Map());
  const [inspectorRow, setInspectorRow] = useState(null);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [layout, setLayout] = useState(() => reconcileColumnLayout(safeColumns, readStorage(storageKey, {})));
  const [savedViews, setSavedViews] = useState(() => readStorage(viewsStorageKey, []));
  const [viewName, setViewName] = useState('');
  const [selectedViewName, setSelectedViewName] = useState('');

  const inspectorEnabled = inspectable ?? !onRowClick;
  const serverMode = Boolean(serverState?.onChange);

  useEffect(() => {
    setLayout(reconcileColumnLayout(safeColumns, readStorage(storageKey, {})));
    setSavedViews(readStorage(viewsStorageKey, []));
  }, [storageKey, columnSignature]);

  useEffect(() => { writeStorage(storageKey, layout); }, [storageKey, layout]);
  useEffect(() => { writeStorage(viewsStorageKey, savedViews); }, [viewsStorageKey, savedViews]);

  useEffect(() => {
    if (!safeColumns.some((column) => column.key === filterDraft.columnKey)) {
      setFilterDraft((current) => ({ ...current, columnKey: safeColumns[0]?.key ?? '' }));
    }
  }, [columnSignature, filterDraft.columnKey]);

  const orderedColumns = useMemo(() => layout.order
    .map((key) => safeColumns.find((column) => column.key === key))
    .filter(Boolean), [layout.order, safeColumns]);
  const visibleColumns = useMemo(() => orderedColumns.filter((column) => !layout.hidden.includes(column.key)), [orderedColumns, layout.hidden]);
  const filterableColumns = useMemo(() => orderedColumns.filter((column) => column.filterable !== false), [orderedColumns]);

  const filteredRows = useMemo(() => serverMode ? safeRows : applyGridFilters(safeRows, orderedColumns, query, filters), [serverMode, safeRows, orderedColumns, query, filters]);
  const sortedRows = useMemo(() => serverMode ? filteredRows : sortGridRows(filteredRows, orderedColumns, sort), [serverMode, filteredRows, orderedColumns, sort]);
  const clientPage = useMemo(() => paginateGridRows(sortedRows, page, pageSize), [sortedRows, page, pageSize]);
  const effectivePage = serverMode ? Math.max(0, Number(serverState.page) || 0) : clientPage.page;
  const effectivePageSize = serverMode ? Math.max(1, Number(serverState.pageSize) || pageSize) : clientPage.pageSize;
  const totalRows = serverMode ? Math.max(0, Number(serverState.totalRows) || 0) : clientPage.totalRows;
  const totalPages = serverMode ? Math.max(1, Math.ceil(totalRows / effectivePageSize)) : clientPage.totalPages;
  const displayedRows = serverMode ? safeRows : clientPage.rows;
  const displayedEntries = displayedRows.map((row, index) => ({ row, key: resolveRowKey(row, effectivePage * effectivePageSize + index, rowKey) }));
  const selectedRows = Array.from(selectedRowsByKey.values());
  const selectedOnPage = displayedEntries.filter(({ key }) => selectedRowsByKey.has(key));
  const allPageSelected = displayedEntries.length > 0 && selectedOnPage.length === displayedEntries.length;
  const somePageSelected = selectedOnPage.length > 0 && !allPageSelected;

  function publishServerState(partial) {
    if (!serverMode) return;
    serverState.onChange({
      query,
      filters,
      sort,
      page: effectivePage,
      pageSize: effectivePageSize,
      ...partial
    });
  }

  function changeQuery(value) {
    setQuery(value);
    setPage(0);
    publishServerState({ query: value, page: 0 });
  }

  function changeSort(column) {
    if (column.sortable === false) return;
    const nextSort = sort?.columnKey !== column.key
      ? { columnKey: column.key, direction: 'asc' }
      : sort.direction === 'asc'
        ? { columnKey: column.key, direction: 'desc' }
        : null;
    setSort(nextSort);
    setPage(0);
    publishServerState({ sort: nextSort, page: 0 });
  }

  function addFilter() {
    if (!filterDraft.columnKey || (operatorNeedsValue(filterDraft.operator) && !filterDraft.value.trim())) return;
    const nextFilters = [...filters, { ...filterDraft, id: `${Date.now()}-${filters.length}` }];
    setFilters(nextFilters);
    setFilterDraft((current) => ({ ...current, value: '' }));
    setPage(0);
    publishServerState({ filters: nextFilters, page: 0 });
  }

  function removeFilter(id) {
    const nextFilters = filters.filter((filter) => filter.id !== id);
    setFilters(nextFilters);
    setPage(0);
    publishServerState({ filters: nextFilters, page: 0 });
  }

  function toggleSelection(key, row) {
    setSelectedRowsByKey((current) => {
      const next = new Map(current);
      if (next.has(key)) next.delete(key);
      else next.set(key, row);
      return next;
    });
  }

  function togglePageSelection() {
    setSelectedRowsByKey((current) => {
      const next = new Map(current);
      if (allPageSelected) displayedEntries.forEach(({ key }) => next.delete(key));
      else displayedEntries.forEach(({ key, row }) => next.set(key, row));
      return next;
    });
  }

  function activateRow(row) {
    onRowClick?.(row);
    if (inspectorEnabled) setInspectorRow(row);
  }

  function exportRows(rowsToExport = selectedRows.length ? selectedRows : sortedRows) {
    downloadCsv(buildGridCsv(rowsToExport, visibleColumns), exportFileName || gridId || 'registros-operacionais');
  }

  function toggleColumn(columnKey) {
    setLayout((current) => {
      const isHidden = current.hidden.includes(columnKey);
      if (!isHidden && visibleColumns.length <= 1) return current;
      return { ...current, hidden: isHidden ? current.hidden.filter((key) => key !== columnKey) : [...current.hidden, columnKey] };
    });
  }

  function resetLayout() {
    const reset = reconcileColumnLayout(safeColumns, {});
    setLayout(reset);
    setQuery('');
    setFilters([]);
    setSort(null);
    setPage(0);
    setPageSize(defaultPageSize);
    setSelectedRowsByKey(new Map());
    publishServerState({ query: '', filters: [], sort: null, page: 0, pageSize: defaultPageSize });
  }

  function saveView() {
    const name = viewName.trim();
    if (!name) return;
    const view = { name, query, filters, sort, pageSize: effectivePageSize, layout };
    setSavedViews((current) => [...current.filter((item) => item.name !== name), view].sort((left, right) => left.name.localeCompare(right.name, 'pt-BR')));
    setSelectedViewName(name);
    setViewName('');
  }

  function applyView() {
    const view = savedViews.find((item) => item.name === selectedViewName);
    if (!view) return;
    const nextLayout = reconcileColumnLayout(safeColumns, view.layout);
    setQuery(view.query ?? '');
    setFilters(Array.isArray(view.filters) ? view.filters : []);
    setSort(view.sort ?? null);
    setPage(0);
    setPageSize(Number(view.pageSize) || defaultPageSize);
    setLayout(nextLayout);
    publishServerState({ query: view.query ?? '', filters: view.filters ?? [], sort: view.sort ?? null, page: 0, pageSize: Number(view.pageSize) || defaultPageSize });
  }

  function deleteView() {
    if (!selectedViewName) return;
    setSavedViews((current) => current.filter((item) => item.name !== selectedViewName));
    setSelectedViewName('');
  }

  function changePage(nextPage) {
    const normalizedPage = Math.min(Math.max(0, nextPage), totalPages - 1);
    if (serverMode) publishServerState({ page: normalizedPage });
    else setPage(normalizedPage);
  }

  function changePageSize(nextPageSize) {
    const normalizedSize = Number(nextPageSize) || defaultPageSize;
    setPageSize(normalizedSize);
    setPage(0);
    publishServerState({ page: 0, pageSize: normalizedSize });
  }

  const firstVisibleKey = visibleColumns[0]?.key;

  return <div className="operational-grid">
    <div className="operational-grid-toolbar">
      <label className="operational-grid-search"><span>Busca rápida</span><input type="search" value={query} onChange={(event) => changeQuery(event.target.value)} placeholder="Buscar em todas as colunas" /></label>
      <div className="operational-grid-toolbar-actions">
        <span>{totalRows} registro(s)</span>
        <button type="button" className="secondary small" onClick={() => exportRows()} disabled={!sortedRows.length}>Exportar CSV</button>
        <button type="button" className="secondary small" aria-expanded={settingsOpen} onClick={() => setSettingsOpen((value) => !value)}>Colunas e visões</button>
      </div>
    </div>

    <div className="operational-grid-filter-builder">
      <label><span>Coluna</span><select value={filterDraft.columnKey} onChange={(event) => setFilterDraft((current) => ({ ...current, columnKey: event.target.value }))}>{filterableColumns.map((column) => <option key={column.key} value={column.key}>{column.label}</option>)}</select></label>
      <label><span>Condição</span><select value={filterDraft.operator} onChange={(event) => setFilterDraft((current) => ({ ...current, operator: event.target.value }))}>{FILTER_OPERATORS.map((operator) => <option key={operator.value} value={operator.value}>{operator.label}</option>)}</select></label>
      {operatorNeedsValue(filterDraft.operator) && <label className="operational-grid-filter-value"><span>Valor</span><input value={filterDraft.value} onChange={(event) => setFilterDraft((current) => ({ ...current, value: event.target.value }))} onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); addFilter(); } }} /></label>}
      <button type="button" className="small" onClick={addFilter} disabled={!filterDraft.columnKey || (operatorNeedsValue(filterDraft.operator) && !filterDraft.value.trim())}>Adicionar filtro</button>
    </div>

    {!!filters.length && <div className="operational-grid-filter-chips" aria-label="Filtros ativos">{filters.map((filter) => {
      const column = orderedColumns.find((candidate) => candidate.key === filter.columnKey);
      const operator = FILTER_OPERATORS.find((candidate) => candidate.value === filter.operator);
      return <button type="button" className="secondary small" key={filter.id} onClick={() => removeFilter(filter.id)} title="Remover filtro">{column?.label || filter.columnKey} {operator?.label || filter.operator}{operatorNeedsValue(filter.operator) ? ` “${filter.value}”` : ''} ×</button>;
    })}</div>}

    {settingsOpen && <section className="operational-grid-settings" aria-label="Configuração da grade">
      <div className="operational-grid-settings-columns"><h3>Colunas</h3>{orderedColumns.map((column, index) => <div key={column.key}>
        <label><input type="checkbox" checked={!layout.hidden.includes(column.key)} onChange={() => toggleColumn(column.key)} /> <span>{column.label}</span></label>
        <div><button type="button" className="secondary small" aria-label={`Mover ${column.label} para a esquerda`} disabled={index === 0} onClick={() => setLayout((current) => ({ ...current, order: moveColumn(current.order, column.key, 'left') }))}>←</button><button type="button" className="secondary small" aria-label={`Mover ${column.label} para a direita`} disabled={index + 1 === orderedColumns.length} onClick={() => setLayout((current) => ({ ...current, order: moveColumn(current.order, column.key, 'right') }))}>→</button></div>
      </div>)}<label className="operational-grid-freeze"><input type="checkbox" checked={layout.frozenFirst} onChange={(event) => setLayout((current) => ({ ...current, frozenFirst: event.target.checked }))} /> Fixar a primeira coluna visível</label></div>
      <div className="operational-grid-saved-views"><h3>Visões salvas</h3><label><span>Nome da visão</span><input value={viewName} onChange={(event) => setViewName(event.target.value)} maxLength={80} placeholder="Ex.: Pendências do turno" /></label><button type="button" className="small" disabled={!viewName.trim()} onClick={saveView}>Salvar visão atual</button><label><span>Visões disponíveis</span><select value={selectedViewName} onChange={(event) => setSelectedViewName(event.target.value)}><option value="">Selecione</option>{savedViews.map((view) => <option key={view.name} value={view.name}>{view.name}</option>)}</select></label><div className="actions"><button type="button" className="secondary small" disabled={!selectedViewName} onClick={applyView}>Aplicar</button><button type="button" className="danger small" disabled={!selectedViewName} onClick={deleteView}>Excluir</button><button type="button" className="secondary small" onClick={resetLayout}>Restaurar padrão</button></div></div>
    </section>}

    {!!selectedRows.length && <div className="operational-grid-selection-bar" role="status"><strong>{selectedRows.length} selecionado(s)</strong><div className="actions"><button type="button" className="secondary small" onClick={() => exportRows(selectedRows)}>Exportar seleção</button>{bulkActions.map((action) => <button type="button" className={`${action.className || 'secondary'} small`} key={action.label} onClick={() => action.onClick(selectedRows)}>{action.label}</button>)}<button type="button" className="secondary small" onClick={() => setSelectedRowsByKey(new Map())}>Limpar seleção</button></div></div>}

    <div className="table-wrap operational-grid-table-wrap">
      <table>
        <thead><tr>
          {selectable && <th className="operational-grid-selection-column"><SelectionCheckbox checked={allPageSelected} indeterminate={somePageSelected} onChange={togglePageSelection} label="Selecionar todos os registros desta página" /></th>}
          {visibleColumns.map((column) => {
            const direction = sort?.columnKey === column.key ? sort.direction : null;
            const frozen = layout.frozenFirst && column.key === firstVisibleKey;
            return <th key={column.key} className={frozen ? 'operational-grid-frozen' : ''} style={frozen ? { left: selectable ? '43px' : 0 } : undefined} aria-sort={direction === 'asc' ? 'ascending' : direction === 'desc' ? 'descending' : 'none'}><button type="button" className="operational-grid-sort" onClick={() => changeSort(column)} disabled={column.sortable === false}><span>{column.label}</span><i aria-hidden="true">{direction === 'asc' ? '↑' : direction === 'desc' ? '↓' : '↕'}</i></button></th>;
          })}
        </tr></thead>
        <tbody>{displayedEntries.map(({ row, key }, index) => {
          const clickable = Boolean(onRowClick || inspectorEnabled);
          return <tr key={key} className={`${clickable ? 'clickable' : ''} ${selectedRowsByKey.has(key) ? 'selected' : ''}`} tabIndex={clickable ? 0 : undefined} onClick={(event) => { if (!event.target.closest('button,input,a,select,textarea')) activateRow(row); }} onKeyDown={(event) => { if (clickable && ['Enter', ' '].includes(event.key)) { event.preventDefault(); activateRow(row); } }}>
            {selectable && <td className="operational-grid-selection-column" data-label="Selecionar"><SelectionCheckbox checked={selectedRowsByKey.has(key)} onChange={() => toggleSelection(key, row)} label={`Selecionar registro ${key}`} /></td>}
            {visibleColumns.map((column) => {
              const frozen = layout.frozenFirst && column.key === firstVisibleKey;
              return <td key={column.key} className={frozen ? 'operational-grid-frozen' : ''} style={frozen ? { left: selectable ? '43px' : 0 } : undefined} data-label={column.label}>{column.render ? column.render(row, index) : normalizeGridValue(column.value ? column.value(row) : row?.[column.key]) || '—'}</td>;
            })}
          </tr>;
        })}</tbody>
      </table>
      {!displayedEntries.length && <div className="empty-state"><strong>{safeRows.length ? 'Nenhum registro corresponde aos filtros' : emptyTitle}</strong>{safeRows.length > 0 && <span>Remova filtros ou altere a busca para visualizar os registros.</span>}</div>}
    </div>

    <footer className="operational-grid-pagination" aria-label="Paginação da grade">
      <span>{totalRows ? `${effectivePage * effectivePageSize + 1}–${Math.min((effectivePage + 1) * effectivePageSize, totalRows)} de ${totalRows}` : '0 registros'}</span>
      <label>Linhas por página<select value={effectivePageSize} onChange={(event) => changePageSize(event.target.value)}>{pageSizeOptions.map((size) => <option key={size} value={size}>{size}</option>)}</select></label>
      <div><button type="button" className="secondary small" disabled={effectivePage === 0} onClick={() => changePage(effectivePage - 1)}>Anterior</button><span>Página {effectivePage + 1} de {totalPages}</span><button type="button" className="secondary small" disabled={effectivePage + 1 >= totalPages} onClick={() => changePage(effectivePage + 1)}>Próxima</button></div>
    </footer>

    <Inspector row={inspectorRow} columns={visibleColumns} onClose={() => setInspectorRow(null)} />
  </div>;
}
