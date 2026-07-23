import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AlertCenterPage, GlobalAlertCenter } from './AlertCenter.jsx';
import { api, clearSession, formatError, hasAnyRole, readSession, sanitizeText, saveSession, subscribeSessionExpired } from './api.js';
import { Message } from './components.jsx';
import {
  activeNavigationItem,
  filterNavigation,
  itemsForPaths,
  navigationBreadcrumb,
  navigationGroupIcon,
  navigationItemIcon,
  navigationStorageKey,
  readNavigationStorage,
  sanitizeStoredPaths,
  toggleNavigationPath,
  updateRecentNavigation,
  writeNavigationStorage
} from './navigationUx.js';
import { usePortalRouter } from './router.js';
import { NotificationsPage, PrivacyPage, RolesPage, SecurityPage, UsersPage } from './pages/AdminPages.jsx';
import { BillingPage, CapPage } from './pages/BillingCapPages.jsx';
import { CompaniesPage } from './pages/CompaniesPage.jsx';
import { ContainerVesselPlannerPage } from './pages/ContainerVesselPlannerPage.jsx';
import { ControlRoomEquipamentosPage } from './pages/ControlRoomEquipamentosPage.jsx';
import { EdiMonitorPage } from './pages/EdiMonitorPage.jsx';
import { GateDirectVesselPage } from './pages/GateDirectVesselPage.jsx';
import { GateDirectVesselReleasePage } from './pages/GateDirectVesselReleasePage.jsx';
import { GateOperationsPage } from './pages/GateOperationsPage.jsx';
import { GatePeopleAccessPage } from './pages/GatePeopleAccessPage.jsx';
import { GeneralCargoPage } from './pages/GeneralCargoPage.jsx';
import { GateReportsPage, YardInventoryPage } from './pages/InventoryReportsPages.jsx';
import { LostFoundPage } from './pages/LostFoundPage.jsx';
import { BerthRegistrationsPage, EquipmentReferencesPage, GateInfrastructurePage, VesselRegistrationsPage } from './pages/MasterDataPages.jsx';
import { PublicApiDiagnosticsPage } from './pages/PublicApiDiagnosticsPage.jsx';
import { RailLineUpPage } from './pages/RailLineUpPage.jsx';
import { RailLocomotiveTransfersPage } from './pages/RailLocomotiveTransfersPage.jsx';
import { RailWorkListPage } from './pages/RailWorkListPage.jsx';
import { ShipModelsPage } from './pages/ShipModelsPage.jsx';
import { SteelCoilPlannerPage } from './pages/SteelCoilPlannerPage.jsx';
import { StuffUnstuffPage } from './pages/StuffUnstuffPage.jsx';
import { VesselLineUpPage } from './pages/VesselLineUpPage.jsx';
import { ControlRoomPage, DATASET_ROUTES, GateDashboardPage, GenericDatasetPage, HomeDashboard, RailImportPage, RailVisitsPage } from './pages/OperationalPages.jsx';
import { DispatchEquipamentosPage } from './pages/yard/DispatchEquipamentosPage.jsx';
import { YardAutomationPage, YardImpactPage, YardInstructionsPage, YardKpiPage, YardMapPage, YardMovementsPage, YardPositionsPage, YardReceivingPlanPage, YardResourcesPage, YardWorkListPage } from './pages/YardPages.jsx';
import './navigation.css';

export const FALLBACK_NAVIGATION = [
  { group: 'Visão geral', items: [
    { label: 'Painel', path: '/home/dashboard', roles: [] },
    { label: 'Central de alertas', path: '/home/alertas', roles: [] }
  ] },
  { group: 'Cadastros', items: [
    { label: 'Papéis de acesso', path: '/home/role', roles: ['ADMIN_PORTO'] },
    { label: 'Usuários', path: '/home/lista-de-usuarios', roles: ['ADMIN_PORTO'] },
    { label: 'Empresas e clientes', path: '/home/cadastros/empresas', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE'] },
    { label: 'Berços portuários', path: '/home/patio/bercos', roles: ['ADMIN_PORTO', 'PLANEJADOR'] },
    { label: 'Instalações, gates e pistas', path: '/home/gate/configuracao', roles: ['ADMIN_PORTO'] },
    { label: 'Tipos e prefixos de equipamentos', path: '/home/patio/tipos-equipamentos', roles: ['ADMIN_PORTO', 'PLANEJADOR'] },
    { label: 'Contêineres, chassis e carretas', path: '/home/patio/inventario', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO'] },
    { label: 'Pátios e posições', path: '/home/patio/mapa', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO'] },
    { label: 'Trens e composições', path: '/home/ferrovia/visitas', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO'] },
    { label: 'Modelos de navio', path: '/home/cadastros/modelos-navio', roles: ['ADMIN_PORTO', 'PLANEJADOR'] },
    { label: 'Janelas de atendimento', path: '/home/gate/janelas', roles: [] },
    { label: 'Recursos do pátio', path: '/home/patio/recursos', roles: [] }
  ] },
  { group: 'Control Room', items: [
    { label: 'Equipamentos e telemetria', path: '/home/control-room', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO', 'OPERADOR_GATE'] }
  ] },
  { group: 'Configurações', items: [
    { label: 'Segurança', path: '/home/seguranca', roles: [] },
    { label: 'Notificações', path: '/home/notificacoes', roles: [] },
    { label: 'Privacidade', path: '/home/privacidade', roles: [] }
  ] },
  { group: 'Carga geral', items: [
    { label: 'Bill of Lading e cargo lots', path: '/home/carga-geral', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE'] },
    { label: 'Stuff e unstuff', path: '/home/carga-geral/stuff-unstuff', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE'] }
  ] },
  { group: 'Gate', items: [
    { label: 'Gate visual', path: '/home/gate/dashboard', roles: [] },
    { label: 'Operação completa', path: '/home/gate/operacao', roles: ['ADMIN_PORTO', 'OPERADOR_GATE', 'PLANEJADOR'] },
    { label: 'Agendamentos', path: '/home/gate/agendamentos', roles: [] },
    { label: 'Controle de pessoas', path: '/home/gate/pessoas', roles: ['ADMIN_PORTO', 'OPERADOR_GATE', 'PLANEJADOR'] },
    { label: 'Embarque direto', path: '/home/gate/embarque-direto', roles: ['ADMIN_PORTO', 'OPERADOR_GATE'] },
    { label: 'Saída direta do navio', path: '/home/gate/saida-direta-navio', roles: ['ADMIN_PORTO', 'OPERADOR_GATE'] },
    { label: 'Relatórios', path: '/home/gate/relatorios', roles: [] }
  ] },
  { group: 'Faturamento', items: [{ label: 'Billing', path: '/home/billing', roles: ['ADMIN_PORTO', 'PLANEJADOR'] }] },
  { group: 'Portal do cliente', items: [{ label: 'Portal da transportadora', path: '/home/cap', roles: ['TRANSPORTADORA'] }] },
  { group: 'Ferrovia', items: [
    { label: 'Visitas', path: '/home/ferrovia/visitas', roles: [] },
    { label: 'Line-up ferroviário', path: '/home/ferrovia/line-up', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO'] },
    { label: 'Importar manifesto', path: '/home/ferrovia/visitas/importar', roles: ['ADMIN_PORTO', 'PLANEJADOR'] },
    { label: 'Lista de trabalho', path: '/home/ferrovia/lista-trabalho', roles: [] },
    { label: 'Locomotivas para navio', path: '/home/ferrovia/locomotivas', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO'] }
  ] },
  { group: 'Pátio', items: [
    { label: 'Mapa', path: '/home/patio/mapa', roles: [] },
    { label: 'Inventário', path: '/home/patio/inventario', roles: [] },
    { label: 'Unidades não localizadas', path: '/home/patio/lost-found', roles: ['ADMIN_PORTO', 'OPERADOR_PATIO', 'PLANEJADOR'] },
    { label: 'Planejamento de recebimento', path: '/home/patio/planejamento-recebimento', roles: ['ADMIN_PORTO', 'PLANEJADOR'] },
    { label: 'Yard Impact', path: '/home/patio/yard-impact', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO'] },
    { label: 'Lista de trabalho', path: '/home/patio/lista-trabalho', roles: [] },
    { label: 'Instruções de trabalho', path: '/home/patio/instrucoes', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO'] },
    { label: 'Movimentações', path: '/home/patio/movimentacoes', roles: [] },
    { label: 'Indicadores', path: '/home/patio/dashboard-kpi', roles: [] },
    { label: 'Automação', path: '/home/patio/automacao', roles: ['ADMIN_PORTO', 'PLANEJADOR'] },
    { label: 'Dispatch e equipamentos', path: '/home/patio/dispatch-equipamentos', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_PATIO'] }
  ] },
  { group: 'Integrações', items: [
    { label: 'Painel EDI', path: '/home/integracoes/edi', roles: ['ADMIN_PORTO', 'PLANEJADOR'] },
    { label: 'Diagnóstico da API pública', path: '/home/integracoes/api-publica', roles: ['ADMIN_PORTO'] }
  ] },
  { group: 'Navio e embarque', items: [
    { label: 'Navios', path: '/home/navio/cadastros', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE'] },
    { label: 'Line-up de navios', path: '/home/navio/line-up', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE'] },
    { label: 'Control Room', path: '/home/navio/control-room', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE'] },
    { label: 'Planejamento de estiva', path: '/home/embarque/planejamento', roles: [] },
    { label: 'Steel coils', path: '/home/embarque/steel-coils', roles: [] }
  ] }
];

export function normalizeBackendTabs(tabs) {
  if (!Array.isArray(tabs)) return [];
  const groups = new Map();
  tabs.forEach((tab) => {
    if (tab?.desabilitado) return;
    const route = Array.isArray(tab?.rota) ? tab.rota.map((part) => sanitizeText(part)).filter(Boolean) : [];
    const identifier = sanitizeText(tab?.identificador || tab?.id).toLowerCase();
    const path = route.length ? `/home/${route.join('/')}` : identifier ? `/home/${identifier}` : '';
    if (!path) return;
    const group = sanitizeText(tab?.grupo) || 'Outros';
    if (!groups.has(group)) groups.set(group, []);
    groups.get(group).push({ label: sanitizeText(tab?.rotulo) || identifier, path, roles: Array.isArray(tab?.rolesPermitidos) ? tab.rolesPermitidos : [] });
  });
  return Array.from(groups, ([group, items]) => ({ group, items }));
}

export function mergeNavigation(fallbackNavigation, dynamicNavigation) {
  const dynamicPaths = new Set(dynamicNavigation.flatMap((group) => group.items.map((item) => item.path)));
  const groups = new Map();
  fallbackNavigation.forEach((group) => {
    const items = group.items.filter((item) => !dynamicPaths.has(item.path));
    if (items.length) groups.set(group.group, items);
  });
  dynamicNavigation.forEach((group) => {
    const current = groups.get(group.group) ?? [];
    groups.set(group.group, [...current, ...group.items]);
  });
  return Array.from(groups, ([group, items]) => ({ group, items }));
}

function safeReturnPath(path) {
  const normalized = String(path ?? '');
  return normalized === '/home' || normalized.startsWith('/home/') ? normalized : '/home/dashboard';
}

function LoginPage({ onAuthenticated, navigate, returnPath }) {
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(event) {
    event.preventDefault();
    if (busy || !login || !password) return;
    setBusy(true); setError('');
    try {
      const session = saveSession(await api.autenticar(login, password));
      setPassword('');
      onAuthenticated(session);
      navigate(returnPath && returnPath !== '/login' ? returnPath : '/home/dashboard', { replace: true });
    } catch (reason) {
      clearSession();
      setError(formatError(reason, 'Não foi possível autenticar.'));
    } finally { setBusy(false); }
  }
  return <main className="login-shell"><section className="login-visual" aria-hidden="true"><div><span className="brand-mark">CP</span><h1>Operação portuária em uma única visão.</h1><p>Gate, ferrovia, pátio, navio e embarque conectados pelo CloudPort.</p></div></section><section className="login-panel"><form className="login-card" onSubmit={submit}><div className="login-brand"><span className="brand-mark small">CP</span><div><strong>CloudPort</strong><small>Portal operacional</small></div></div><div><span className="eyebrow">Acesso seguro</span><h2>Entrar</h2><p>Use uma conta autorizada para acessar os módulos.</p></div><label className="field"><span>Login</span><input value={login} onChange={(event) => setLogin(event.target.value)} autoComplete="username" maxLength={120} required autoFocus /></label><label className="field"><span>Senha</span><input type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" required /></label><Message type="error">{error}</Message><button className="large" type="submit" disabled={busy || !login || !password}>{busy ? 'Autenticando...' : 'Entrar no CloudPort'}</button></form></section></main>;
}

function NotFoundPage({ navigate }) {
  return <div className="not-found"><span className="eyebrow">404</span><h1>Tela não encontrada</h1><p>A rota informada não está disponível no portal React.</p><button onClick={() => navigate('/home/dashboard')}>Voltar ao painel</button></div>;
}

function RouteContent({ path, navigate, session }) {
  if (path === '/home' || path === '/home/dashboard') return <HomeDashboard navigate={navigate} />;
  if (path === '/home/alertas') return <AlertCenterPage navigate={navigate} session={session} />;
  if (path === '/home/control-room' || path === '/home/patio/control-room') return <ControlRoomEquipamentosPage session={session} />;
  if (path === '/home/role') return <RolesPage />;
  if (path === '/home/seguranca') return <SecurityPage />;
  if (path === '/home/notificacoes') return <NotificationsPage />;
  if (path === '/home/privacidade') return <PrivacyPage />;
  if (path === '/home/lista-de-usuarios') return <UsersPage />;
  if (path === '/home/cadastros/empresas') return <CompaniesPage session={session} />;
  if (path === '/home/navio/cadastros') return <VesselRegistrationsPage session={session} />;
  if (path === '/home/patio/bercos') return <BerthRegistrationsPage session={session} />;
  if (path === '/home/gate/configuracao') return <GateInfrastructurePage session={session} />;
  if (path === '/home/patio/tipos-equipamentos') return <EquipmentReferencesPage session={session} />;
  if (path === '/home/cadastros/modelos-navio') return <ShipModelsPage />;
  if (path === '/home/carga-geral') return <GeneralCargoPage />;
  if (path === '/home/carga-geral/stuff-unstuff') return <StuffUnstuffPage />;
  if (path === '/home/billing') return <BillingPage />;
  if (path === '/home/cap') return <CapPage />;
  if (path === '/home/navio' || path === '/home/navio/line-up') return <VesselLineUpPage navigate={navigate} />;
  if (path === '/home/navio/control-room') return <ControlRoomPage session={session} />;
  if (path === '/home/gate/operacao') return <GateOperationsPage session={session} />;
  if (path === '/home/gate' || path === '/home/gate/dashboard' || path === '/home/gate/operador' || path === '/home/gate/operador/console') return <GateDashboardPage />;
  if (path === '/home/gate/pessoas') return <GatePeopleAccessPage />;
  if (path === '/home/gate/embarque-direto') return <GateDirectVesselPage session={session} />;
  if (path === '/home/gate/saida-direta-navio') return <GateDirectVesselReleasePage />;
  if (path === '/home/gate/relatorios') return <GateReportsPage />;
  if (path === '/home/ferrovia' || path === '/home/ferrovia/visitas') return <RailVisitsPage session={session} />;
  if (path === '/home/ferrovia/line-up') return <RailLineUpPage />;
  if (path === '/home/ferrovia/visitas/importar') return <RailImportPage />;
  if (path === '/home/ferrovia/lista-trabalho') return <RailWorkListPage session={session} />;
  if (path === '/home/ferrovia/locomotivas') return <RailLocomotiveTransfersPage />;
  if (path === '/home/patio' || path === '/home/patio/mapa') return <YardMapPage navigate={navigate} />;
  if (path === '/home/patio/inventario') return <YardInventoryPage />;
  if (path === '/home/patio/lost-found') return <LostFoundPage />;
  if (path === '/home/patio/planejamento-recebimento') return <YardReceivingPlanPage navigate={navigate} />;
  if (path === '/home/patio/yard-impact') return <YardImpactPage navigate={navigate} />;
  if (path === '/home/patio/posicoes') return <YardPositionsPage navigate={navigate} />;
  if (path === '/home/patio/lista-trabalho') return <YardWorkListPage navigate={navigate} session={session} />;
  if (path === '/home/patio/instrucoes') return <YardInstructionsPage navigate={navigate} session={session} />;
  if (path === '/home/patio/movimentacoes' || path === '/home/patio/movimentacao') return <YardMovementsPage navigate={navigate} />;
  if (path === '/home/patio/recursos') return <YardResourcesPage navigate={navigate} />;
  if (path === '/home/patio/dashboard-kpi') return <YardKpiPage navigate={navigate} />;
  if (path === '/home/patio/automacao' || path === '/home/patio/simulador') return <YardAutomationPage navigate={navigate} session={session} />;
  if (path === '/home/patio/dispatch-equipamentos') return <DispatchEquipamentosPage navigate={navigate} session={session} />;
  if (path === '/home/integracoes' || path === '/home/integracoes/edi') return <EdiMonitorPage />;
  if (path === '/home/integracoes/api-publica') return <PublicApiDiagnosticsPage />;
  if (path === '/home/embarque' || path === '/home/embarque/planejamento') return <ContainerVesselPlannerPage session={session} />;
  if (path === '/home/embarque/steel-coils') return <SteelCoilPlannerPage />;
  const definition = DATASET_ROUTES[path] ?? Object.entries(DATASET_ROUTES).find(([route]) => path.startsWith(`${route}/`))?.[1];
  if (definition) return <GenericDatasetPage {...definition} />;
  return <NotFoundPage navigate={navigate} />;
}

function NavigationItem({ item, active, favorite, onOpen, onToggleFavorite }) {
  return <div className={`navigation-item${active ? ' active' : ''}`}>
    <button
      type="button"
      className="navigation-link"
      aria-current={active ? 'page' : undefined}
      aria-label={`Abrir ${item.label}`}
      onClick={() => onOpen(item.path)}
    >
      <span className="navigation-icon" aria-hidden="true">{navigationItemIcon(item)}</span>
      <span>{item.label}</span>
    </button>
    <button
      type="button"
      className={`navigation-favorite${favorite ? ' selected' : ''}`}
      aria-label={`${favorite ? 'Desafixar' : 'Fixar'} ${item.label} nos favoritos`}
      aria-pressed={favorite}
      onClick={() => onToggleFavorite(item.path)}
      title={favorite ? 'Remover dos favoritos' : 'Adicionar aos favoritos'}
    >
      {favorite ? '★' : '☆'}
    </button>
  </div>;
}

function PortalShell({ path, navigate, session, onLogout }) {
  const [mobileMenu, setMobileMenu] = useState(false);
  const [dynamicNavigation, setDynamicNavigation] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const searchRef = useRef(null);
  const storage = globalThis.localStorage;
  const favoritesKey = useMemo(() => navigationStorageKey(session, 'favorites'), [session]);
  const recentKey = useMemo(() => navigationStorageKey(session, 'recent'), [session]);
  const groupsKey = useMemo(() => navigationStorageKey(session, 'groups'), [session]);
  const [favoritePaths, setFavoritePaths] = useState(() => readNavigationStorage(storage, favoritesKey));
  const [recentPaths, setRecentPaths] = useState(() => readNavigationStorage(storage, recentKey));
  const [openGroups, setOpenGroups] = useState(() => readNavigationStorage(storage, groupsKey));

  useEffect(() => {
    let active = true;
    api.listarAbas().then((tabs) => { if (active) setDynamicNavigation(normalizeBackendTabs(tabs)); }).catch(() => { if (active) setDynamicNavigation([]); });
    return () => { active = false; };
  }, []);

  const navigation = useMemo(() => mergeNavigation(FALLBACK_NAVIGATION, dynamicNavigation), [dynamicNavigation]);
  const visibleNavigation = useMemo(() => navigation
    .map((group) => ({ ...group, items: group.items.filter((item) => !item.roles?.length || hasAnyRole(session, ...item.roles)) }))
    .filter((group) => group.items.length), [navigation, session]);
  const activeItem = useMemo(() => activeNavigationItem(path, visibleNavigation), [path, visibleNavigation]);
  const breadcrumb = useMemo(() => navigationBreadcrumb(path, visibleNavigation), [path, visibleNavigation]);
  const filteredNavigation = useMemo(() => filterNavigation(visibleNavigation, searchQuery), [visibleNavigation, searchQuery]);
  const favorites = useMemo(() => itemsForPaths(favoritePaths, visibleNavigation), [favoritePaths, visibleNavigation]);
  const recent = useMemo(() => itemsForPaths(recentPaths, visibleNavigation).filter((item) => !favoritePaths.includes(item.path)), [recentPaths, favoritePaths, visibleNavigation]);

  useEffect(() => {
    setFavoritePaths(readNavigationStorage(storage, favoritesKey));
    setRecentPaths(readNavigationStorage(storage, recentKey));
    setOpenGroups(readNavigationStorage(storage, groupsKey));
  }, [favoritesKey, recentKey, groupsKey, storage]);

  useEffect(() => {
    setFavoritePaths((current) => {
      const next = sanitizeStoredPaths(current, visibleNavigation);
      writeNavigationStorage(storage, favoritesKey, next);
      return next;
    });
    setRecentPaths((current) => {
      const next = sanitizeStoredPaths(current, visibleNavigation).slice(0, 6);
      writeNavigationStorage(storage, recentKey, next);
      return next;
    });
  }, [visibleNavigation, favoritesKey, recentKey, storage]);

  useEffect(() => {
    if (!activeItem) return;
    setRecentPaths((current) => {
      const next = updateRecentNavigation(current, activeItem.path, visibleNavigation);
      writeNavigationStorage(storage, recentKey, next);
      return next;
    });
    setOpenGroups((current) => {
      if (current.includes(activeItem.group)) return current;
      const next = [...current, activeItem.group];
      writeNavigationStorage(storage, groupsKey, next);
      return next;
    });
  }, [activeItem, visibleNavigation, recentKey, groupsKey, storage]);

  useEffect(() => {
    function focusSearch(event) {
      const tag = String(event.target?.tagName ?? '').toLowerCase();
      const typing = event.target?.isContentEditable || ['input', 'select', 'textarea'].includes(tag);
      if (typing || !(event.ctrlKey || event.metaKey) || event.key.toLowerCase() !== 'k') return;
      event.preventDefault();
      setMobileMenu(true);
      globalThis.setTimeout(() => searchRef.current?.focus(), 0);
    }
    window.addEventListener('keydown', focusSearch);
    return () => window.removeEventListener('keydown', focusSearch);
  }, []);

  function open(pathToOpen) {
    navigate(pathToOpen);
    setMobileMenu(false);
    setSearchQuery('');
  }

  function toggleFavorite(pathToToggle) {
    setFavoritePaths((current) => {
      const next = sanitizeStoredPaths(toggleNavigationPath(current, pathToToggle), visibleNavigation);
      writeNavigationStorage(storage, favoritesKey, next);
      return next;
    });
  }

  function toggleGroup(group) {
    setOpenGroups((current) => {
      const next = current.includes(group) ? current.filter((item) => item !== group) : [...current, group];
      writeNavigationStorage(storage, groupsKey, next);
      return next;
    });
  }

  function logout() {
    clearSession();
    onLogout();
    navigate('/login', { replace: true });
  }

  function renderQuickGroup(title, items, icon) {
    if (!items.length || searchQuery) return null;
    return <section className="nav-group nav-quick-group">
      <h2><span className="navigation-icon" aria-hidden="true">{icon}</span>{title}</h2>
      <div className="nav-group-items">{items.map((item) => <NavigationItem
        key={`${title}-${item.path}`}
        item={item}
        active={path === item.path || path.startsWith(`${item.path}/`)}
        favorite={favoritePaths.includes(item.path)}
        onOpen={open}
        onToggleFavorite={toggleFavorite}
      />)}</div>
    </section>;
  }

  return <div className="portal-shell">
    <aside className={`sidebar navigation-sidebar ${mobileMenu ? 'open' : ''}`}>
      <button className="sidebar-brand" onClick={() => open('/home/dashboard')}>
        <span className="brand-mark small">CP</span>
        <span><strong>CloudPort</strong><small>Portal operacional</small></span>
      </button>

      <label className="navigation-search">
        <span>Buscar telas e comandos</span>
        <div><span aria-hidden="true">⌕</span><input ref={searchRef} type="search" value={searchQuery} onChange={(event) => setSearchQuery(event.target.value)} placeholder="Ex.: trem, gate, estiva" /><kbd>Ctrl K</kbd></div>
      </label>

      <nav aria-label="Navegação principal">
        {renderQuickGroup('Favoritos', favorites, '★')}
        {renderQuickGroup('Recentes', recent, '◷')}

        {filteredNavigation.map((group) => {
          const expanded = Boolean(searchQuery) || openGroups.includes(group.group) || activeItem?.group === group.group;
          const groupId = `navigation-group-${group.group.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`;
          return <section className={`nav-group${expanded ? ' expanded' : ''}`} key={group.group}>
            <button type="button" className="nav-group-toggle" aria-expanded={expanded} aria-controls={groupId} onClick={() => toggleGroup(group.group)}>
              <span className="navigation-icon" aria-hidden="true">{navigationGroupIcon(group.group)}</span>
              <span>{group.group}</span>
              <span className="nav-group-count">{group.items.length}</span>
              <span className="nav-group-chevron" aria-hidden="true">›</span>
            </button>
            <div id={groupId} className="nav-group-items" hidden={!expanded}>
              {group.items.map((item) => <NavigationItem
                key={item.path}
                item={item}
                active={path === item.path || path.startsWith(`${item.path}/`)}
                favorite={favoritePaths.includes(item.path)}
                onOpen={open}
                onToggleFavorite={toggleFavorite}
              />)}
            </div>
          </section>;
        })}
        {!filteredNavigation.length && <div className="navigation-empty"><strong>Nenhuma tela encontrada</strong><span>Tente outro nome, módulo, sinônimo ou rota.</span></div>}
      </nav>
      <footer className="sidebar-footer"><span>Busca: Ctrl + K · Ajuda: F1</span></footer>
    </aside>

    {mobileMenu && <button className="sidebar-backdrop" aria-label="Fechar menu" onClick={() => setMobileMenu(false)} />}

    <div className="portal-main">
      <header className="topbar">
        <button className="menu-button" onClick={() => setMobileMenu((value) => !value)} aria-label={mobileMenu ? 'Fechar menu' : 'Abrir menu'} aria-expanded={mobileMenu}>☰</button>
        <div className="topbar-context"><strong>{activeItem?.label ?? 'CloudPort'}</strong><span>{breadcrumb}</span></div>
        <div className="topbar-actions"><GlobalAlertCenter navigate={navigate} session={session} /><div className="user-menu"><div><strong>{session.nome || 'Operador'}</strong><span>{session.perfil || session.roles?.[0] || 'Usuário'}</span></div><button className="secondary" onClick={logout}>Sair</button></div></div>
      </header>
      <main className="content"><RouteContent path={path} navigate={navigate} session={session} /></main>
    </div>
  </div>;
}

export default function App() {
  const { path, navigate } = usePortalRouter();
  const [session, setSession] = useState(() => readSession());
  const [requestedPath, setRequestedPath] = useState('/home/dashboard');
  useEffect(() => subscribeSessionExpired(() => { setRequestedPath(safeReturnPath(path)); setSession(null); navigate('/login', { replace: true }); }), [path, navigate]);
  useEffect(() => {
    if (!session && path !== '/login') { setRequestedPath(safeReturnPath(path)); navigate('/login', { replace: true }); }
    if (session && path === '/login') navigate('/home/dashboard', { replace: true });
  }, [session, path, navigate]);
  const authenticate = useCallback((newSession) => setSession(newSession), []);
  const logout = useCallback(() => setSession(null), []);
  if (!session) return <LoginPage onAuthenticated={authenticate} navigate={navigate} returnPath={requestedPath} />;
  return <PortalShell path={path} navigate={navigate} session={session} onLogout={logout} />;
}
