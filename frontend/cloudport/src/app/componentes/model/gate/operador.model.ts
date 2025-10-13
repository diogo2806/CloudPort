export type GateNivelEvento = 'INFO' | 'ALERTA' | 'CRITICA' | 'OPERACIONAL' | string;

export interface GateOperadorExcecao {
  codigo: string;
  descricao: string;
  nivel: GateNivelEvento;
}

export interface GateOperadorContato {
  tipo: 'TELEFONE' | 'EMAIL' | 'WHATSAPP' | string;
  valor: string;
  descricao?: string | null;
}

export interface GateOperadorVeiculo {
  id: number;
  placa: string;
  documento?: string | null;
  motorista?: string | null;
  status: string;
  statusDescricao?: string | null;
  tempoFilaMinutos?: number | null;
  canalEntrada?: string | null;
  transportadora?: string | null;
  contatos?: GateOperadorContato[] | null;
  excecoes?: GateOperadorExcecao[] | null;
  podeImprimirComprovante?: boolean;
}

export interface GateOperadorFila {
  id: number;
  nome: string;
  quantidade: number;
  tempoMedioEsperaMinutos?: number | null;
  veiculos: GateOperadorVeiculo[];
}

export interface GateOperadorEvento {
  id: number;
  tipo: string;
  descricao: string;
  nivel: GateNivelEvento;
  registradoEm: string;
  veiculoId?: number | null;
  placaVeiculo?: string | null;
  transportadora?: string | null;
  usuario?: string | null;
}

export interface GateOperadorPainel {
  filasEntrada: GateOperadorFila[];
  filasSaida: GateOperadorFila[];
  veiculosAtendimento: GateOperadorVeiculo[];
  historico: GateOperadorEvento[];
  ultimaAtualizacao: string;
}

export interface GateLiberacaoManualRequest {
  canalEntrada: string;
  justificativa: string;
  notificarTransportadora: boolean;
}

export interface GateBloqueioRequest {
  motivoCodigo: string;
  justificativa: string;
  bloqueioAte: string;
}

export interface GateOcorrenciaRequest {
  tipoCodigo: string;
  descricao: string;
  veiculoId?: number | null;
}
