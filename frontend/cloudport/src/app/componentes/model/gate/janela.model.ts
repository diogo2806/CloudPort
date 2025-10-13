import { GateEnumOption, Page } from './agendamento.model';

export interface JanelaAtendimento {
  id: number;
  data: string;
  horaInicio: string;
  horaFim: string;
  capacidade: number;
  canalEntrada: string;
  canalEntradaDescricao: string | null;
}

export interface JanelaFiltro {
  dataInicio?: string;
  dataFim?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface JanelaAtendimentoRequest {
  data: string;
  horaInicio: string;
  horaFim: string;
  capacidade: number;
  canalEntrada: string;
}

export type JanelaAtendimentoPage = Page<JanelaAtendimento>;

export type JanelaEnumOption = GateEnumOption;
