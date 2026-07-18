export function collectDatasetKeys(rows, preferred = []) {
  const preferredKeys = Array.from(new Set((Array.isArray(preferred) ? preferred : [])
    .map((key) => String(key ?? '').trim())
    .filter(Boolean)));
  const discoveredKeys = new Set();

  (Array.isArray(rows) ? rows : []).forEach((row) => {
    if (!row || typeof row !== 'object' || Array.isArray(row)) return;
    Object.keys(row).forEach((key) => {
      if (key && !preferredKeys.includes(key)) discoveredKeys.add(key);
    });
  });

  return [...preferredKeys, ...discoveredKeys];
}

export function humanizeDatasetKey(key) {
  return String(key ?? '')
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/^./, (letter) => letter.toUpperCase());
}
