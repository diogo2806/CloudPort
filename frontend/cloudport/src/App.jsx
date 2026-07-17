import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, clearSession, formatError, hasAnyRole, readSession, sanitizeText, saveSession } from './api.js';
import { Message } from './components.jsx';
import { usePortalRouter } from './router.js';
import { NotificationsPage, PrivacyPage, RolesPage, SecurityPage, UsersPage } from './pages/AdminPages.jsx';
import { ContainerVesselPlannerPage } from './pages/ContainerVesselPlannerPage.jsx';
import {
  ControlRoomPage,
  DATASET_ROUTES,
  GateDashboardPage,
  GenericDatasetPage,
  HomeDashboard,
  RailImportPage,
  RailVisitsPage,
  YardMapPage
} from './pages/OperationalPages.jsx';

const FALLBACK_NAVIGATION = [
  { group: 'Visão geral', items: [{ label: 'Painel', path: '/home/dashboard', roles: [] }] },
  { group: 'Configurações', items: [
    { label: 'Papéis de acesso', path: '/home/role', roles: ['ADMIN_PORTO'] },
    { label: 'Segurança', path: '/home/seguranca', roles: [] },
    { label: 'Notificações', path: '/home/notificacoes', roles: [] },
    { label: 'Privacidade', path: '/home/privacidade', roles: [] },
    { label: 'Usuários', path: '/home/lista-de-usuarios', roles: ['ADMIN_PORTO'] }
  ] },
  { group: 'Gate', items: [
    { label: 'Agendamentos', path: '/home/gate/agendamentos', roles: [] },
    { label: 'Janelas', path: '/home/gate/janelas', roles: [] },
    { label: 'Central de ação', path: '/home/gate/dashboard', roles: [] },
    { label: 'Console do operador', path: '/home/gate/operador/console', roles: ['ADMIN_PORTO', 'OPERADOR_GATE'] },
    { label: 'Relatórios', path: '/home/gate/relatorios', roles: [] }
  ] },
  { group: 'Ferrovia', items: [
    { label: 'Visitas', path: '/home/ferrovia/visitas', roles: [] },
    { label: 'Importar manifesto', path: '/home/ferrovia/visitas/importar', roles: ['ADMIN_PORTO', 'PLANEJADOR'] },
    { label: 'Lista de trabalho', path: '/home/ferrovia/lista-trabalho', roles: [] }
  ] },
  { group: 'Pátio', items: [
    { label: 'Mapa', path: '/home/patio/mapa', roles: [] },
    { label: 'Lista de trabalho', path: '/home/patio/lista-trabalho', roles: [] },
    { label: 'Posições', path: '/home/patio/posicoes', roles: [] },
    { label: 'Movimentações', path: '/home/patio/movimentacoes', roles: [] },
    { label: 'Recursos', path: '/home/patio/recursos', roles: [] },
    { label: 'Indicadores', path: '/home/patio/dashboard-kpi', roles: [] },
    { label: 'Automação', path: '/home/patio/automacao', roles: ['ADMIN_PORTO', 'PLANEJADOR'] }
  ] },
  { group: 'Navio e embarque', items: [
    { label: 'Control Room', path: '/home/navio/control-room', roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE'] },
    { label: 'Planejamento de estiva', path: '/home/embarque/planejamento', roles: [] },
    { label: 'Steel coils', path: '/home/embarque/steel-coils', roles: [] }
  ] }
];

function normalizeBackendTabs(tabs) {
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
    groups.get(group).push({
      label: sanitizeText(tab?.rotulo) || identifier,
      path,
      roles: Array.isArray(tab?.rolesPermitidos) ? tab.rolesPermitidos : []
    });
  });
  return Array.from(groups, ([group, items]) => ({ group, items }));
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

  return <main className="login-shell">
    <section className="login-visual" aria-hidden="true"><div><span className="brand-mark">CP</span><h1>Operação portuária em uma única visão.</h1><p>Gate, ferrovia, pátio, navio e embarque conectados pelo CloudPort.</p></div></section>
    <section className="login-panel"><form className="login-card" onSubmit={submit}>
      <div className="login-brand"><span className="brand-mark small">CP</span><div><strong>CloudPort</strong><small>Portal operacional React</small></div></div>
      <div><span className="eyebrow">Acesso seguro</span><h2>Entrar</h2><p>Use uma conta autorizada para acessar os módulos.</p></div>
      <label className="field"><span>Login</span><input value={login} onChange={(event) => setLogin(event.target.value)} autoComplete="username" maxLength={120} required autoFocus /></label>
      <label className="field"><span>Senha</span><input type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" required /></label>
      <Message type="error">{error}</Message>
      <button className="large" type="submit" disabled={busy || !login || !password}>{busy ? 'Autenticando...' : 'Entrar no CloudPort'}</button>
    </form></section>
  </main>;
}

function NotFoundPage({ navigate }) {
  return <div className="not-found"><span className="eyebrow">404</span><h1>Tela não encontrada</h1><p>A rota informada não está disponível no portal React.</p><button onClick={() => navigate('/home/dashboard')}>Voltar ao painel</button></div>;
}

function RouteContent({ path, navigate, session }) {
  if (path === '/home' || path === '/home/dashboard') return <HomeDashboard navigate={navigate} />;
  if (path === '/home/role') return <RolesPage />;
  if (path === '/home/seguranca') return <SecurityPage />;
  if (path === '/home/notificacoes') return <NotificationsPage />;
  if (path === '/home/privacidade') return <PrivacyPage />;
  if (path === '/home/lista-de-usuarios') return <UsersPage />;
  if (path === '/home/navio' || path === '/home/navio/control-room') return <ControlRoomPage session={session} />;
  if (path === '/home/gate' || path === '/home/gate/dashboard') return <GateDashboardPage />;
  if (path === '/home/ferrovia' || path === '/home/ferrovia/visitas') return <RailVisitsPage />;
  if (path === '/home/ferrovia/visitas/importar') return <RailImportPage />;
  if (path === '/home/patio' || path === '/home/patio/mapa') return <YardMapPage />;
  if (path === '/home/embarque' || path === '/home/embarque/planejamento') return <ContainerVesselPlannerPage session={session} />;

  const definition = DATASET_ROUTES[path]
    ?? Object.entries(DATASET_ROUTES).find(([route]) => path.startsWith(`${route}/`))?.[1];
  if (definition) return <GenericDatasetPage {...definition} />;
  return <NotFoundPage navigate={navigate} />;
}

function PortalShell({ path, navigate, session, onLogout }) {
  const [mobileMenu, setMobileMenu] = useState(false);
  const [dynamicNavigation, setDynamicNavigation] = useState([]);

  useEffect(() => {
    let active = true;
    api.listarAbas().then((tabs) => {
      if (active) setDynamicNavigation(normalizeBackendTabs(tabs));
    }).catch(() => {
      if (active) setDynamicNavigation([]);
    });
    return () => { active = false; };
  }, []);

  const navigation = dynamicNavigation.length ? dynamicNavigation : FALLBACK_NAVIGATION;
  const visibleNavigation = useMemo(() => navigation.map((group) => ({
    ...group,
    items: group.items.filter((item) => !item.roles?.length || hasAnyRole(session, ...item.roles))
  })).filter((group) => group.items.length), [navigation, session]);

  function open(pathToOpen) {
    navigate(pathToOpen);
    setMobileMenu(false);
  }

  function logout() {
    clearSession();
    onLogout();
    navigate('/login', { replace: true });
  }

  return <div className="portal-shell">
    <aside className={`sidebar ${mobileMenu ? 'open' : ''}`}>
      <button className="sidebar-brand" onClick={() => open('/home/dashboard')}><span className="brand-mark small">CP</span><span><strong>CloudPort</strong><small>Portal operacional</small></span></button>
      <nav aria-label="Navegação principal">{visibleNavigation.map((group) => <section className="nav-group" key={group.group}><h2>{group.group}</h2>{group.items.map((item) => <button key={item.path} className={path === item.path || path.startsWith(`${item.path}/`) ? 'active' : ''} onClick={() => open(item.path)}>{item.label}</button>)}</section>)}</nav>
      <footer className="sidebar-footer"><span>React 19 · Vite 8</span></footer>
    </aside>
    {mobileMenu && <button className="sidebar-backdrop" aria-label="Fechar menu" onClick={() => setMobileMenu(false)} />}
    <div className="portal-main">
      <header className="topbar">
        <button className="menu-button" onClick={() => setMobileMenu((value) => !value)} aria-label="Abrir menu">☰</button>
        <div className="topbar-context"><strong>CloudPort</strong><span>{path.replace('/home/', '').replaceAll('/', ' / ') || 'Painel'}</span></div>
        <div className="user-menu"><div><strong>{session.nome || 'Operador'}</strong><span>{session.perfil || session.roles?.[0] || 'Usuário'}</span></div><button className="secondary" onClick={logout}>Sair</button></div>
      </header>
      <main className="content"><RouteContent path={path} navigate={navigate} session={session} /></main>
    </div>
  </div>;
}

export default function App() {
  const { path, navigate } = usePortalRouter();
  const [session, setSession] = useState(() => readSession());
  const [requestedPath, setRequestedPath] = useState('/home/dashboard');

  useEffect(() => {
    if (!session && path !== '/login') {
      setRequestedPath(path);
      navigate('/login', { replace: true });
    }
    if (session && path === '/login') navigate('/home/dashboard', { replace: true });
  }, [session, path, navigate]);

  const authenticate = useCallback((newSession) => setSession(newSession), []);
  const logout = useCallback(() => setSession(null), []);

  if (!session) return <LoginPage onAuthenticated={authenticate} navigate={navigate} returnPath={requestedPath} />;
  return <PortalShell path={path} navigate={navigate} session={session} onLogout={logout} />;
}
