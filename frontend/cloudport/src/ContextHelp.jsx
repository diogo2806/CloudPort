import { useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { readSession } from './api.js';
import { resolveCockpitContextHelp } from './cockpitContextHelp.js';
import { buildHelpSections, filterHelpSections, resolveContextHelp } from './contextHelp.js';
import { isContextHelpCloseShortcut, isContextHelpOpenShortcut } from './contextHelpKeyboard.js';
import { resolveFleetContextHelp } from './fleetContextHelp.js';
import { applyPredictiveContextHelp } from './predictiveContextHelp.js';
import { resolveRailContextHelp } from './railContextHelp.js';
import './context-help.css';

function HelpSection({ section }) {
  const List = section.id === 'flow' ? 'ol' : 'ul';
  return <section className={`context-help-section context-help-section-${section.id}`}>
    <h3>{section.title}</h3>
    <List>{section.items.map((item, index) => <li key={`${section.id}-${index}`}>{item}</li>)}</List>
  </section>;
}

export function ContextHelp({ path, navigate, session, shortcuts = true }) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const triggerRef = useRef(null);
  const closeRef = useRef(null);
  const activePath = path ?? globalThis.location?.pathname ?? '/home/dashboard';
  const activeSession = session ?? readSession() ?? {};
  const help = useMemo(() => {
    const baseHelp = resolveContextHelp(activePath, activeSession);
    const domainHelp = resolveFleetContextHelp(activePath, baseHelp)
      ?? resolveCockpitContextHelp(activePath, baseHelp)
      ?? resolveRailContextHelp(activePath, baseHelp)
      ?? baseHelp;
    const predictiveHelp = applyPredictiveContextHelp(activePath, domainHelp);
    return {
      ...predictiveHelp,
      shortcuts: [...new Set([
        ...(predictiveHelp.shortcuts ?? []),
        'Ctrl + K ou Command + K: abrir o menu e posicionar o foco na busca global de telas e comandos.'
      ])]
    };
  }, [activePath, activeSession]);
  const sections = useMemo(() => buildHelpSections(help), [help]);
  const visibleSections = useMemo(() => filterHelpSections(sections, query), [sections, query]);
  const portalTarget = globalThis.document?.body;

  useEffect(() => {
    setOpen(false);
    setQuery('');
  }, [activePath]);

  useEffect(() => {
    if (!shortcuts) return undefined;

    function handleKeyDown(event) {
      if (isContextHelpCloseShortcut(event, open)) {
        event.preventDefault();
        setOpen(false);
        triggerRef.current?.focus();
        return;
      }
      if (!isContextHelpOpenShortcut(event)) return;
      event.preventDefault();
      setOpen(true);
    }

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [open, shortcuts]);

  useEffect(() => {
    function handleEscape(event) {
      if (!shortcuts && isContextHelpCloseShortcut(event, open)) {
        event.preventDefault();
        setOpen(false);
        triggerRef.current?.focus();
      }
    }
    if (!shortcuts) window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [open, shortcuts]);

  useEffect(() => {
    if (open) closeRef.current?.focus();
  }, [open]);

  useEffect(() => {
    if (!open || !portalTarget) return undefined;
    const previousOverflow = portalTarget.style.overflow;
    portalTarget.style.overflow = 'hidden';
    return () => {
      portalTarget.style.overflow = previousOverflow;
    };
  }, [open, portalTarget]);

  function close() {
    setOpen(false);
    triggerRef.current?.focus();
  }

  function openProcess() {
    close();
    if (typeof navigate === 'function') {
      navigate(help.processPath);
      return;
    }
    globalThis.history?.pushState({}, '', help.processPath);
    globalThis.dispatchEvent?.(new PopStateEvent('popstate'));
    globalThis.scrollTo?.({ top: 0, behavior: 'smooth' });
  }

  const overlay = open && portalTarget
    ? createPortal(
      <div className="context-help-layer">
        <button type="button" className="context-help-backdrop" aria-label="Fechar ajuda contextual" onClick={close} />
        <aside className="context-help-drawer" role="dialog" aria-modal="true" aria-labelledby="context-help-title">
          <header>
            <div><span>{help.module}</span><h2 id="context-help-title">{help.title}</h2><p>{help.path}</p></div>
            <button ref={closeRef} type="button" className="icon-button" aria-label="Fechar ajuda" onClick={close}>×</button>
          </header>

          <div className="context-help-search">
            <label htmlFor="context-help-query">Pesquisar nesta ajuda</label>
            <input id="context-help-query" type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Ex.: permissão, status, bloqueio" />
          </div>

          <div className="context-help-access">
            <span>Perfil atual</span>
            <strong title={help.currentRoles.join(' · ')}>{help.currentRoles.length ? help.currentRoles.join(' · ') : 'Perfil não informado'}</strong>
          </div>

          <div className="context-help-content">
            {visibleSections.length
              ? visibleSections.map((section) => <HelpSection key={section.id} section={section} />)
              : <div className="context-help-empty"><strong>Nenhum conteúdo encontrado</strong><span>Altere o termo pesquisado.</span></div>}
          </div>

          <footer>
            <div>
              <button type="button" className="secondary small" onClick={openProcess}>{help.processLabel}</button>
              <a href={help.documentationUrl} target="_blank" rel="noreferrer">Documentação técnica</a>
            </div>
            <span>F1 ou Shift + ? abre a ajuda · Esc fecha</span>
          </footer>
        </aside>
      </div>,
      portalTarget,
    )
    : null;

  return <div className="context-help">
    <button
      ref={triggerRef}
      type="button"
      className="context-help-trigger secondary"
      aria-label={`Abrir ajuda contextual de ${help.title}`}
      aria-expanded={open}
      aria-haspopup="dialog"
      onClick={() => setOpen((value) => !value)}
    >
      <span aria-hidden="true">?</span><strong>Ajuda</strong>
    </button>
    {overlay}
  </div>;
}
