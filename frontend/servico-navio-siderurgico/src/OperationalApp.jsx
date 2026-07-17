import { useEffect, useRef, useState } from 'react';
import { api, clearSession, formatError, hasAnyRole, loadRuntimeConfig, readSession, saveSession } from './api.js';
import ReconciliacaoBarcodePage from './ReconciliacaoBarcodePage.jsx';
import Ui20ControlRoom from './Ui20ControlRoom.jsx';
import { ROLES, clean } from './ui20-model.js';
import './ui20.css';
import './reconciliacao-barcode.css';

const RECONCILIACAO_ROUTE = '#/reconciliacao-barcode';

function currentRoute() {
  return typeof window === 'undefined' ? '' : window.location.hash;
}

function Auth({ onAuthenticated }) {
  const [login, setLogin] = useState('');
  const [senha, setSenha] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const origins = useRef(new Set());

  useEffect(() => {
    let active = true;
    loadRuntimeConfig().then((config) => {
      if (!active) return;
      origins.current = new Set([window.location.origin, 'http://localhost:4200', ...(config.trustedParentOrigins ?? [])]);
      window.parent?.postMessage({ type: 'CLOUDPORT_CONTROL_ROOM_READY' }, '*');
    }).catch((reason) => active && setError(formatError(reason)));
    const receive = (event) => {
      if (!origins.current.has(event.origin) || event.data?.type !== 'CLOUDPORT_AUTH_SESSION') return;
      try {
        const session = saveSession(event.data.session);
        if (!hasAnyRole(session, ...ROLES)) throw new Error('Conta sem permissão operacional.');
        onAuthenticated(session);
      } catch (reason) {
        clearSession();
        setError(formatError(reason));
      }
    };
    window.addEventListener('message', receive);
    return () => { active = false; window.removeEventListener('message', receive); };
  }, [onAuthenticated]);

  async function submit(event) {
    event.preventDefault(); setBusy(true); setError('');
    try {
      const session = saveSession(await api.autenticar(clean(login), senha));
      if (!hasAnyRole(session, ...ROLES)) throw new Error('Conta sem permissão operacional.');
      onAuthenticated(session);
    } catch (reason) {
      clearSession(); setError(formatError(reason));
    } finally { setBusy(false); }
  }

  return <main className="auth-shell"><form className="auth-card" onSubmit={submit}><span className="eyebrow">CloudPort</span><h1>Control Room Navio + Pátio</h1><p>Entre com uma conta operacional autorizada.</p><label>Login<input value={login} onChange={(event) => setLogin(event.target.value)} autoComplete="username" required /></label><label>Senha<input type="password" value={senha} onChange={(event) => setSenha(event.target.value)} autoComplete="current-password" required /></label>{error && <div className="message error">{error}</div>}<button disabled={busy}>{busy ? 'Autenticando...' : 'Entrar'}</button></form></main>;
}

export default function OperationalApp() {
  const [session, setSession] = useState(() => readSession());
  const [ready, setReady] = useState(false);
  const [error, setError] = useState('');
  const [route, setRoute] = useState(currentRoute);
  useEffect(() => { loadRuntimeConfig().then(() => setReady(true)).catch((reason) => setError(formatError(reason))); }, []);
  useEffect(() => {
    const updateRoute = () => setRoute(currentRoute());
    window.addEventListener('hashchange', updateRoute);
    return () => window.removeEventListener('hashchange', updateRoute);
  }, []);
  if (error) return <main className="auth-shell"><div className="auth-card"><h1>Control Room indisponível</h1><div className="message error">{error}</div></div></main>;
  if (!ready) return <main className="auth-shell"><div className="auth-card"><h1>CloudPort</h1><p>Carregando configuração...</p></div></main>;
  if (!session || !hasAnyRole(session, ...ROLES)) return <Auth onAuthenticated={setSession} />;

  const logout = () => { clearSession(); setSession(null); window.location.hash = ''; };
  if (route === RECONCILIACAO_ROUTE && hasAnyRole(session, 'ADMIN_PORTO')) {
    return <ReconciliacaoBarcodePage session={session} onBack={() => { window.location.hash = ''; }} onLogout={logout} />;
  }

  return <><nav className="admin-navigation">{hasAnyRole(session, 'ADMIN_PORTO') && <button className="secondary" onClick={() => { window.location.hash = RECONCILIACAO_ROUTE; }}>Reconciliação de Barcode</button>}</nav><Ui20ControlRoom session={session} onLogout={logout} /></>;
}
