import { useEffect, useState } from 'react';
import { api, clearSession, formatError, hasAnyRole, loadRuntimeConfig, readSession, saveSession } from './api.js';
import Ui20ControlRoom from './Ui20ControlRoom.jsx';
import { ROLES, clean } from './ui20-model.js';
import './ui20.css';

function Auth({ onAuthenticated }) {
  const [login, setLogin] = useState(''); const [senha, setSenha] = useState(''); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError('');
    try { const session = saveSession(await api.autenticar(clean(login), senha)); if (!hasAnyRole(session, ...ROLES)) throw new Error('Conta sem permissão operacional.'); onAuthenticated(session); }
    catch (reason) { clearSession(); setError(formatError(reason)); } finally { setBusy(false); }
  }
  return <main className="auth-shell"><form className="auth-card" onSubmit={submit}><span className="eyebrow">CloudPort</span><h1>Control Room Navio + Pátio</h1><p>Entre com uma conta operacional autorizada.</p><label>Login<input value={login} onChange={(event) => setLogin(event.target.value)} required /></label><label>Senha<input type="password" value={senha} onChange={(event) => setSenha(event.target.value)} required /></label>{error && <div className="message error">{error}</div>}<button disabled={busy}>{busy ? 'Autenticando...' : 'Entrar'}</button></form></main>;
}

export default function OperationalApp() {
  const [session, setSession] = useState(() => readSession()); const [ready, setReady] = useState(false); const [error, setError] = useState('');
  useEffect(() => { loadRuntimeConfig().then(() => setReady(true)).catch((reason) => setError(formatError(reason))); }, []);
  if (error) return <main className="auth-shell"><div className="auth-card"><h1>Control Room indisponível</h1><div className="message error">{error}</div></div></main>;
  if (!ready) return <main className="auth-shell"><div className="auth-card"><h1>CloudPort</h1><p>Carregando configuração...</p></div></main>;
  return session && hasAnyRole(session, ...ROLES) ? <Ui20ControlRoom session={session} onLogout={() => { clearSession(); setSession(null); }} /> : <Auth onAuthenticated={setSession} />;
}
