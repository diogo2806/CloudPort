export interface OcupacaoPorHora {
  horaInicio: string;
  totalAgendamentos: number;
  capacidadeSlot: number;
}

export interface TempoMedioPermanencia {
  dia: string;
  tempoMedioMinutos: number | null;
}

export interface DashboardResumo {
  totalAgendamentos: number;
  percentualPontualidade: number;
  percentualNoShow: number;
  percentualOcupacaoSlots: number;
  tempoMedioTurnaroundMinutos: number;
  ocupacaoPorHora: OcupacaoPorHora[];
  turnaroundPorDia: TempoMedioPermanencia[];
}

export interface DashboardFiltro {
  inicio?: string;
  fim?: string;
  transportadoraId?: number;
  tipoOperacao?: string;
}
