export function isContextHelpTypingTarget(target) {
  const tagName = String(target?.tagName ?? '').toLowerCase();
  return Boolean(target?.isContentEditable) || ['input', 'select', 'textarea'].includes(tagName);
}

export function isContextHelpOpenShortcut(event) {
  if (!event || isContextHelpTypingTarget(event.target)) return false;
  return event.key === 'F1' || (event.shiftKey === true && event.key === '?');
}

export function isContextHelpCloseShortcut(event, open) {
  return Boolean(open) && event?.key === 'Escape';
}
