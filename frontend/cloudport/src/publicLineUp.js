const PHASE_LABELS = {
  PREVISTA: 'Previsto',
  INBOUND: 'Em aproximação',
  ATRACADO: 'Atracado',
  OPERANDO: 'Em operação',
  PARTIU: 'Partiu'
};

export function lineUpStatusLabel(fase) {
  return PHASE_LABELS[String(fase ?? '').toUpperCase()] ?? 'Não informado';
}

export function normalizeLineUp(payload) {
  if (!Array.isArray(payload)) return [];
  return payload.filter((item) => item && typeof item === 'object').map((item) => ({
    nomeNavio: String(item.nomeNavio ?? '').trim(),
    codigoImo: String(item.codigoImo ?? '').trim(),
    viagemEntrada: String(item.viagemEntrada ?? '').trim(),
    viagemSaida: String(item.viagemSaida ?? '').trim(),
    fase: String(item.fase ?? '').trim().toUpperCase(),
    chegadaPrevista: item.chegadaPrevista ?? null,
    atracacaoPrevista: item.atracacaoPrevista ?? null,
    partidaPrevista: item.partidaPrevista ?? null,
    chegadaEfetiva: item.chegadaEfetiva ?? null,
    atracacaoEfetiva: item.atracacaoEfetiva ?? null,
    partidaEfetiva: item.partidaEfetiva ?? null,
    berco: String(item.berco ?? '').trim()
  })).sort((left, right) => (Date.parse(left.chegadaPrevista ?? '') || Number.MAX_SAFE_INTEGER)
    - (Date.parse(right.chegadaPrevista ?? '') || Number.MAX_SAFE_INTEGER));
}

export function filterLineUp(items, search, phase) {
  const term = String(search ?? '').trim().toLocaleLowerCase('pt-BR');
  const selectedPhase = String(phase ?? '').trim().toUpperCase();
  return items.filter((item) => {
    if (selectedPhase && item.fase !== selectedPhase) return false;
    if (!term) return true;
    return [item.nomeNavio, item.codigoImo, item.viagemEntrada, item.viagemSaida, item.berco]
      .some((value) => String(value ?? '').toLocaleLowerCase('pt-BR').includes(term));
  });
}

export function formatLineUpDate(value) {
  if (!value) return 'A confirmar';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'A confirmar';
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
  }).format(date);
}

export function scheduleMoment(actual, expected) {
  return { value: actual || expected || null, actual: Boolean(actual) };
}
