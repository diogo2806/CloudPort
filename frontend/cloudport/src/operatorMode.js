const STORAGE_KEY = 'cloudport:operator-mode';

function enabledByDefault() {
  return globalThis.matchMedia?.('(max-width: 720px), (pointer: coarse)')?.matches ?? false;
}

function applyMode(compact) {
  document.documentElement.classList.toggle('operator-mode', compact);
  document.documentElement.dataset.portalMode = compact ? 'operator' : 'complete';
  globalThis.localStorage?.setItem(STORAGE_KEY, compact ? 'compact' : 'complete');
}

export function installOperatorMode() {
  const stored = globalThis.localStorage?.getItem(STORAGE_KEY);
  applyMode(stored === null ? enabledByDefault() : stored === 'compact');
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'operator-mode-toggle';
  button.setAttribute('aria-label', 'Alternar entre modo operador compacto e modo completo');
  const render = () => {
    const compact = document.documentElement.classList.contains('operator-mode');
    button.textContent = compact ? 'Modo completo' : 'Modo operador';
    button.setAttribute('aria-pressed', String(compact));
  };
  button.addEventListener('click', () => {
    applyMode(!document.documentElement.classList.contains('operator-mode'));
    render();
  });
  render();
  document.body.append(button);
}
