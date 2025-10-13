export type GateChartType = 'bar' | 'line';

export interface GateAnalyticsPreferences {
  inicio?: string | null;
  fim?: string | null;
  transportadoraId?: number | null;
  tipoOperacao?: string | null;
  tipoGrafico?: GateChartType;
}

const STORAGE_KEY = 'gate-analytics-preferences';

export function carregarPreferencias(): GateAnalyticsPreferences | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    return JSON.parse(raw) as GateAnalyticsPreferences;
  } catch {
    return null;
  }
}

export function salvarPreferencias(preferencias: GateAnalyticsPreferences): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(preferencias));
  } catch {
    // armazenamento indisponível, seguir sem persistir
  }
}
