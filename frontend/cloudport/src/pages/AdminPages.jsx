import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, formatError, sanitizeText } from '../api.js';
import { DataTable, EmptyState, Field, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';

function useRemote(loader, dependencies = []) {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await loader();
      setData(response ?? []);
      return response;
    } catch (reason) {
      setData([]);
      setError(formatError(reason));
      return null;
    } finally {
      setLoading(false);
    }
  }, dependencies);

  useEffect(() => { reload(); }, [reload]);
  return { data, setData, loading, error, setError, reload };
}

export function RolesPage() {
  const remote = useRemote(() => api.listarRoles(), []);
  const [name, setName] = useState('');
  const [selected, setSelected] = useState(null);
  const [busy, setBusy] = useState(false);
  const [success, setSuccess] = useState('');
  const normalizedName = sanitizeText(name).toUpperCase().replace(/\s+/g, '_').replace(/[^A-Z0-9_]/g, '');

  async function submit(event) {
    event.preventDefault();
    if (!normalizedName || busy) return;
    setBusy(true); remote.setError(''); setSuccess('');
    try {
      if (selected) await api.atualizarRole(selected.id, normalizedName);
      else await api.criarRole(normalizedName);
      setSuccess(selected ? 'Papel atualizado.' : 'Papel criado.');
      setName(''); setSelected(null);
      await remote.reload();
    } catch (reason) {
      remote.setError(formatError(reason));
    } finally { setBusy(false); }
  }

  async function remove(role) {
    if (!window.confirm(`Excluir o papel ${role.name}?`)) return;
    setBusy(true); remote.setError(''); setSuccess('');
    try {
      await api.excluirRole(role.id);
      setSuccess('Papel excluído.');
      await remote.reload();
    } catch (reason) {
      remote.setError(formatError(reason, 'Não foi possível excluir o papel.'));
    } finally { setBusy(false); }
  }

  const rows = useMemo(() => (remote.data ?? []).map((role) => ({ id: role.id, name: sanitizeText(role.name) })), [remote.data]);

  return <>
    <PageHeader eyebrow="Administração" title="Papéis de acesso" description="Gerencie os papéis usados na autorização do portal." />
    <Message type="error">{remote.error}</Message><Message type="success">{success}</Message>
    <Section title={selected ? 'Editar papel' : 'Novo papel'}>
      <form className="inline-form" onSubmit={submit}>
        <Field label="Nome do papel"><input value={name} onChange={(event) => setName(event.target.value)} maxLength={80} required /></Field>
        <button type="submit" disabled={!normalizedName || busy}>{busy ? 'Salvando...' : selected ? 'Atualizar' : 'Criar'}</button>
        {selected && <button className="secondary" type="button" onClick={() => { setSelected(null); setName(''); }}>Cancelar</button>}
      </form>
    </Section>
    <Section title="Papéis cadastrados" actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>}>
      {remote.loading ? <Loading /> : <DataTable rows={rows} columns={[
        { key: 'id', label: 'ID' },
        { key: 'name', label: 'Papel' },
        { key: 'actions', label: 'Ações', render: (role) => <div className="actions compact"><button className="small secondary" onClick={() => { setSelected(role); setName(role.name); }}>Editar</button><button className="small danger" onClick={() => remove(role)}>Excluir</button></div> }
      ]} emptyTitle="Nenhum papel cadastrado" />}
    </Section>
  </>;
}

export function UsersPage() {
  const remote = useRemote(() => api.listarUsuarios(), []);
  const rows = useMemo(() => [...(remote.data ?? [])]
    .map((user) => ({ ...user, nome: sanitizeText(user.nome), email: sanitizeText(user.email), status: sanitizeText(user.status) || 'Ativo' }))
    .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR', { sensitivity: 'base' })), [remote.data]);
  return <>
    <PageHeader eyebrow="Administração" title="Usuários" description="Consulta de usuários autorizados no CloudPort." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error}</Message>
    <Section title="Usuários cadastrados">
      {remote.loading ? <Loading /> : <DataTable rows={rows} columns={[
        { key: 'id', label: 'ID' }, { key: 'nome', label: 'Nome' }, { key: 'email', label: 'E-mail' },
        { key: 'status', label: 'Status', render: (user) => <StatusBadge value={user.status} /> }
      ]} emptyTitle="Nenhum usuário encontrado" />}
    </Section>
  </>;
}

export function SecurityPage() {
  const [version, setVersion] = useState('');
  const [order, setOrder] = useState('ordem');
  const [query, setQuery] = useState({ versao: '', ordenacao: 'ordem' });
  const remote = useRemote(() => api.listarSeguranca(query), [query.versao, query.ordenacao]);
  return <>
    <PageHeader eyebrow="Configurações" title="Política de segurança" description="Diretrizes vigentes disponibilizadas pelo backend." />
    <Message type="error">{remote.error}</Message>
    <Section title="Filtros"><form className="inline-form" onSubmit={(event) => { event.preventDefault(); setQuery({ versao: sanitizeText(version), ordenacao: sanitizeText(order) }); }}>
      <Field label="Versão"><input value={version} onChange={(event) => setVersion(event.target.value)} /></Field>
      <Field label="Ordenação"><select value={order} onChange={(event) => setOrder(event.target.value)}><option value="ordem">Ordem</option><option value="titulo">Título</option><option value="versao">Versão</option></select></Field>
      <button type="submit">Aplicar</button>
    </form></Section>
    <Section title="Diretrizes">{remote.loading ? <Loading /> : remote.data.length ? <div className="card-list">{remote.data.map((item) => <article className="content-card" key={item.id}><div className="card-meta"><span>Ordem {item.ordem ?? '—'}</span><StatusBadge value={item.versao} /></div><h3>{sanitizeText(item.titulo)}</h3><p>{sanitizeText(item.descricao)}</p></article>)}</div> : <EmptyState title="Nenhuma diretriz encontrada" />}</Section>
  </>;
}

export function NotificationsPage() {
  const remote = useRemote(() => api.listarNotificacoes(), []);
  const [busyId, setBusyId] = useState(null);
  const [success, setSuccess] = useState('');
  async function toggle(channel) {
    setBusyId(channel.identificador); remote.setError(''); setSuccess('');
    try {
      const updated = await api.atualizarNotificacao(channel.identificador, !channel.habilitado);
      remote.setData((current) => current.map((item) => item.identificador === updated.identificador ? updated : item));
      setSuccess('Preferência atualizada.');
    } catch (reason) { remote.setError(formatError(reason)); }
    finally { setBusyId(null); }
  }
  return <>
    <PageHeader eyebrow="Configurações" title="Notificações" description="Ative ou desative os canais operacionais disponíveis." />
    <Message type="error">{remote.error}</Message><Message type="success">{success}</Message>
    <Section title="Canais">{remote.loading ? <Loading /> : remote.data.length ? <div className="settings-list">{remote.data.map((channel) => <article className="setting-row" key={channel.identificador}><div><strong>{sanitizeText(channel.nomeCanal)}</strong><span>{channel.habilitado ? 'Canal ativo' : 'Canal desativado'}</span></div><button className={channel.habilitado ? 'warning' : ''} disabled={busyId === channel.identificador} onClick={() => toggle(channel)}>{busyId === channel.identificador ? 'Atualizando...' : channel.habilitado ? 'Desativar' : 'Ativar'}</button></article>)}</div> : <EmptyState title="Nenhum canal disponível" />}</Section>
  </>;
}

export function PrivacyPage() {
  const remote = useRemote(() => api.listarPrivacidade(), []);
  return <>
    <PageHeader eyebrow="Configurações" title="Privacidade" description="Opções de privacidade publicadas para a conta autenticada." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error}</Message>
    <Section title="Preferências">{remote.loading ? <Loading /> : remote.data.length ? <div className="settings-list">{remote.data.map((item) => <article className="setting-row" key={item.id}><div><strong>{sanitizeText(item.descricao)}</strong><span>Identificador: {sanitizeText(item.id)}</span></div><StatusBadge value={item.ativo ? 'ATIVO' : 'INATIVO'} /></article>)}</div> : <EmptyState title="Nenhuma opção encontrada" />}</Section>
  </>;
}
