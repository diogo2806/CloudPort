export const DEFAULT_PAGE_SIZE_OPTIONS = [10, 25, 50];
export const FILTER_OPERATORS = [
  { value: 'contains', label: 'contém' },
  { value: 'equals', label: 'é igual a' },
  { value: 'notContains', label: 'não contém' },
  { value: 'startsWith', label: 'começa com' },
  { value: 'isEmpty', label: 'está vazio' },
  { value: 'isNotEmpty', label: 'não está vazio' }
];

export function normalizeGridValue(value) {
  if (value === undefined || value === null) return '';
  if (typeof value === 'boolean') return value ? 'Sim' : 'Não';
  if (Array.isArray(value)) return value.map(normalizeGridValue).filter(Boolean).join(', ');
  if (typeof value === 'object') {
    try { return JSON.stringify(value); }
    catch { return String(value); }
  }
  return String(value);
}

export function normalizeSearchText(value) {
  return normalizeGridValue(value)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase('pt-BR')
    .trim();
}

export function resolveRowKey(row, index, rowKey = 'id') {
  const configured = typeof rowKey === 'function' ? rowKey(row, index) : row?.[rowKey];
  const fallback = row?.id ?? row?.codigo ?? row?.identificador ?? row?.chave;
  const resolved = configured ?? fallback;
  if (resolved !== undefined && resolved !== null && resolved !== '') return String(resolved);
  return `${normalizeGridValue(row)}::${index}`;
}

function columnRawValue(row, column) {
  return column?.value ? column.value(row) : row?.[column?.key];
}

function matchesFilter(row, column, filter) {
  const rawValue = columnRawValue(row, column);
  const normalizedValue = normalizeSearchText(rawValue);
  const expected = normalizeSearchText(filter.value);
  switch (filter.operator) {
    case 'equals': return normalizedValue === expected;
    case 'notContains': return !normalizedValue.includes(expected);
    case 'startsWith': return normalizedValue.startsWith(expected);
    case 'isEmpty': return normalizedValue.length === 0;
    case 'isNotEmpty': return normalizedValue.length > 0;
    case 'contains':
    default: return normalizedValue.includes(expected);
  }
}

export function applyGridFilters(rows, columns, query = '', filters = []) {
  const safeRows = Array.isArray(rows) ? rows : [];
  const safeColumns = Array.isArray(columns) ? columns : [];
  const searchableColumns = safeColumns.filter((column) => column.searchable !== false);
  const normalizedQuery = normalizeSearchText(query);
  const activeFilters = (Array.isArray(filters) ? filters : []).filter((filter) => safeColumns.some((column) => column.key === filter.columnKey));

  return safeRows.filter((row) => {
    const matchesQuery = !normalizedQuery || searchableColumns.some((column) => normalizeSearchText(columnRawValue(row, column)).includes(normalizedQuery));
    if (!matchesQuery) return false;
    return activeFilters.every((filter) => {
      const column = safeColumns.find((candidate) => candidate.key === filter.columnKey);
      return column ? matchesFilter(row, column, filter) : true;
    });
  });
}

function comparableValue(value) {
  if (value === undefined || value === null || value === '') return { empty: true, value: '' };
  if (typeof value === 'number') return { empty: false, value };
  if (typeof value === 'boolean') return { empty: false, value: value ? 1 : 0 };
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}(?:T|$)/.test(value)) {
    const timestamp = Date.parse(value);
    if (!Number.isNaN(timestamp)) return { empty: false, value: timestamp };
  }
  const numeric = Number(value);
  if (typeof value !== 'string' && Number.isFinite(numeric)) return { empty: false, value: numeric };
  return { empty: false, value: normalizeSearchText(value) };
}

export function sortGridRows(rows, columns, sort) {
  const safeRows = Array.isArray(rows) ? rows : [];
  if (!sort?.columnKey || !sort?.direction) return [...safeRows];
  const column = (Array.isArray(columns) ? columns : []).find((candidate) => candidate.key === sort.columnKey);
  if (!column) return [...safeRows];
  const multiplier = sort.direction === 'desc' ? -1 : 1;

  return safeRows.map((row, index) => ({ row, index })).sort((left, right) => {
    if (column.compare) {
      const compared = column.compare(left.row, right.row);
      return compared === 0 ? left.index - right.index : compared * multiplier;
    }
    const leftValue = comparableValue(column.sortValue ? column.sortValue(left.row) : columnRawValue(left.row, column));
    const rightValue = comparableValue(column.sortValue ? column.sortValue(right.row) : columnRawValue(right.row, column));
    if (leftValue.empty && rightValue.empty) return left.index - right.index;
    if (leftValue.empty) return 1;
    if (rightValue.empty) return -1;
    if (leftValue.value < rightValue.value) return -1 * multiplier;
    if (leftValue.value > rightValue.value) return 1 * multiplier;
    return left.index - right.index;
  }).map(({ row }) => row);
}

export function paginateGridRows(rows, page, pageSize) {
  const safeRows = Array.isArray(rows) ? rows : [];
  const safePageSize = Math.max(1, Number(pageSize) || DEFAULT_PAGE_SIZE_OPTIONS[0]);
  const totalPages = Math.max(1, Math.ceil(safeRows.length / safePageSize));
  const safePage = Math.min(Math.max(0, Number(page) || 0), totalPages - 1);
  return {
    page: safePage,
    pageSize: safePageSize,
    totalPages,
    totalRows: safeRows.length,
    rows: safeRows.slice(safePage * safePageSize, safePage * safePageSize + safePageSize)
  };
}

export function reconcileColumnLayout(columns, storedLayout = {}) {
  const keys = (Array.isArray(columns) ? columns : []).map((column) => column.key);
  const storedOrder = Array.isArray(storedLayout.order) ? storedLayout.order.filter((key) => keys.includes(key)) : [];
  const order = [...storedOrder, ...keys.filter((key) => !storedOrder.includes(key))];
  const hidden = (Array.isArray(storedLayout.hidden) ? storedLayout.hidden : []).filter((key) => keys.includes(key));
  return { order, hidden, frozenFirst: Boolean(storedLayout.frozenFirst) };
}

export function moveColumn(order, columnKey, direction) {
  const safeOrder = Array.isArray(order) ? [...order] : [];
  const index = safeOrder.indexOf(columnKey);
  const target = direction === 'left' ? index - 1 : index + 1;
  if (index < 0 || target < 0 || target >= safeOrder.length) return safeOrder;
  [safeOrder[index], safeOrder[target]] = [safeOrder[target], safeOrder[index]];
  return safeOrder;
}

function escapeCsv(value) {
  const text = normalizeGridValue(value).replace(/"/g, '""');
  return `"${text}"`;
}

export function buildGridCsv(rows, columns) {
  const safeColumns = (Array.isArray(columns) ? columns : []).filter((column) => column.exportable !== false);
  const header = safeColumns.map((column) => escapeCsv(column.label ?? column.key)).join(';');
  const body = (Array.isArray(rows) ? rows : []).map((row) => safeColumns.map((column) => {
    const value = column.exportValue ? column.exportValue(row) : columnRawValue(row, column);
    return escapeCsv(value);
  }).join(';'));
  return ['\uFEFF' + header, ...body].join('\r\n');
}
