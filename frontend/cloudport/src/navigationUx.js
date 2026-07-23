const GROUP_ICONS = {
  'Visão geral': '⌂',
  Cadastros: '▦',
  'Control Room': '◉',
  Configurações: '⚙',
  'Carga geral': '▤',
  Gate: '⇥',
  Faturamento: '¤',
  'Portal do cliente': '◎',
  Ferrovia: '▰',
  Pátio: '▥',
  Integrações: '⇄',
  'Navio e embarque': '◒'
};

const PATH_ICONS = [
  ['/dashboard', '⌂'],
  ['/alertas', '!'],
  ['/role', '♙'],
  ['/usuarios', '♙'],
  ['/lista-de-usuarios', '♙'],
  ['/seguranca', '◆'],
  ['/notificacoes', '●'],
  ['/privacidade', '◇'],
  ['/gate', '⇥'],
  ['/ferrovia', '▰'],
  ['/patio/mapa', '▦'],
  ['/patio/inventario', '▤'],
  ['/patio', '▥'],
  ['/navio', '◒'],
  ['/embarque', '▲'],
  ['/billing', '¤'],
  ['/cap', '◎'],
  ['/carga-geral', '▤'],
  ['/integracoes', '⇄'],
  ['/control-room', '◉']
];

const SEARCH_SYNONYMS = {
  '/home/dashboard': ['inicio', 'home', 'visao geral', 'kpi'],
  '/home/alertas': ['ocorrencias', 'avisos', 'pendencias'],
  '/home/gate/dashboard': ['portaria', 'entrada', 'saida', 'fila'],
  '/home/gate/operacao': ['truck visit', 'caminhao', 'motorista'],
  '/home/ferrovia/visitas': ['trem', 'composicao', 'vagao'],
  '/home/ferrovia/line-up': ['linha', 'timeline', 'programacao ferroviaria'],
  '/home/ferrovia/lista-trabalho': ['ordens', 'carga', 'descarga'],
  '/home/patio/mapa': ['yard', 'bloco', 'pilha', 'posicao'],
  '/home/patio/inventario': ['container', 'conteiner', 'chassis', 'carreta'],
  '/home/navio/line-up': ['escala', 'berco', 'vessel'],
  '/home/navio/control-room': ['operacao navio', 'guindaste', 'produtividade'],
  '/home/embarque/planejamento': ['estiva', 'vessel planner', 'slot'],
  '/home/billing': ['cobranca', 'fatura', 'tarifa'],
  '/home/integracoes/edi': ['mensagem', 'interchange'],
  '/home/integracoes/api-publica': ['diagnostico', 'api', 'integracao']
};

export function normalizeNavigationText(value) {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLocaleLowerCase('pt-BR')
    .trim();
}

export function navigationGroupIcon(group) {
  return GROUP_ICONS[group] ?? '□';
}

export function navigationItemIcon(item) {
  const path = String(item?.path ?? '');
  return PATH_ICONS.find(([fragment]) => path.includes(fragment))?.[1] ?? '·';
}

export function flattenNavigation(navigation = []) {
  return navigation.flatMap((group) => group.items.map((item) => ({ ...item, group: group.group })));
}

export function filterNavigation(navigation = [], query = '') {
  const term = normalizeNavigationText(query);
  if (!term) return navigation.map((group) => ({ ...group, items: [...group.items] }));

  return navigation.map((group) => {
    const groupText = normalizeNavigationText(group.group);
    const groupMatches = groupText.includes(term);
    const items = group.items.filter((item) => {
      const synonyms = SEARCH_SYNONYMS[item.path] ?? [];
      const haystack = normalizeNavigationText([
        group.group,
        item.label,
        item.path,
        ...synonyms
      ].join(' '));
      return groupMatches || haystack.includes(term);
    });
    return { ...group, items };
  }).filter((group) => group.items.length);
}

export function allowedNavigationPaths(navigation = []) {
  return new Set(flattenNavigation(navigation).map((item) => item.path));
}

export function sanitizeStoredPaths(paths, navigation = []) {
  const allowed = allowedNavigationPaths(navigation);
  return [...new Set(Array.isArray(paths) ? paths.map(String) : [])].filter((path) => allowed.has(path));
}

export function toggleNavigationPath(paths, path) {
  const current = new Set(Array.isArray(paths) ? paths : []);
  if (current.has(path)) current.delete(path);
  else current.add(path);
  return [...current];
}

export function updateRecentNavigation(paths, path, navigation = [], limit = 6) {
  const allowed = allowedNavigationPaths(navigation);
  if (!allowed.has(path)) return sanitizeStoredPaths(paths, navigation).slice(0, limit);
  return [path, ...sanitizeStoredPaths(paths, navigation).filter((item) => item !== path)].slice(0, limit);
}

export function itemsForPaths(paths, navigation = []) {
  const itemByPath = new Map(flattenNavigation(navigation).map((item) => [item.path, item]));
  return (Array.isArray(paths) ? paths : []).map((path) => itemByPath.get(path)).filter(Boolean);
}

export function activeNavigationItem(path, navigation = []) {
  const candidates = flattenNavigation(navigation)
    .filter((item) => path === item.path || path.startsWith(`${item.path}/`))
    .sort((a, b) => b.path.length - a.path.length);
  return candidates[0] ?? null;
}

export function navigationBreadcrumb(path, navigation = []) {
  const active = activeNavigationItem(path, navigation);
  if (!active) return path.replace('/home/', '').replaceAll('/', ' / ') || 'Painel';
  return `${active.group} / ${active.label}`;
}

export function navigationStorageKey(session = {}, scope) {
  const identity = session.id ?? session.usuarioId ?? session.email ?? session.login ?? session.nome ?? 'anonimo';
  const safeIdentity = normalizeNavigationText(identity).replace(/[^a-z0-9_-]+/g, '-');
  return `cloudport:navigation:${safeIdentity || 'anonimo'}:${scope}`;
}

export function readNavigationStorage(storage, key, fallback = []) {
  try {
    const parsed = JSON.parse(storage?.getItem(key) ?? 'null');
    return Array.isArray(parsed) ? parsed : fallback;
  } catch {
    return fallback;
  }
}

export function writeNavigationStorage(storage, key, value) {
  try {
    storage?.setItem(key, JSON.stringify(value));
    return true;
  } catch {
    return false;
  }
}
