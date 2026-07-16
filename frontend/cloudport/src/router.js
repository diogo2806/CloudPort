import { useCallback, useEffect, useState } from 'react';

function normalizePath(path) {
  const clean = String(path ?? '/').split('?')[0].split('#')[0].replace(/\/{2,}/g, '/');
  if (!clean || clean === '/') return '/home/dashboard';
  return clean.length > 1 ? clean.replace(/\/+$/, '') : clean;
}

export function usePortalRouter() {
  const [path, setPath] = useState(() => normalizePath(window.location.pathname));

  useEffect(() => {
    const handlePopState = () => setPath(normalizePath(window.location.pathname));
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const navigate = useCallback((nextPath, options = {}) => {
    const normalized = normalizePath(nextPath);
    if (options.replace) window.history.replaceState({}, '', normalized);
    else window.history.pushState({}, '', normalized);
    setPath(normalized);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, []);

  return { path, navigate };
}
