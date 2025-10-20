export interface GateEnumOption {
  codigo: string;
  descricao: string;
}

export interface DocumentoAgendamento {
  id: number;
  tipoDocumento: string;
  numero: string;
  urlDocumento: string;
  nomeArquivo: string;
  contentType: string;
  tamanhoBytes: number;
  ultimaRevalidacao?: string | null;
  statusValidacao: string;
  statusValidacaoDescricao: string | null;
  mensagemValidacao: string | null;
}

export interface GateEvent {
  id: number;
  status: string;
  statusDescricao: string | null;
  motivoExcecao: string | null;
  motivoExcecaoDescricao: string | null;
  observacao: string | null;
  usuarioResponsavel: string | null;
  registradoEm: string;
}

export interface GatePass {
  id: number;
  codigo: string;
  status: string;
  statusDescricao: string | null;
  dataEntrada: string | null;
  dataSaida: string | null;
  eventos: GateEvent[] | null;
  token: string | null;
}

export interface Agendamento {
  id: number;
  codigo: string;
  tipoOperacao: string;
  tipoOperacaoDescricao: string | null;
  status: string;
  statusDescricao: string | null;
  transportadoraId: number | null;
  transportadoraNome: string | null;
  motoristaId: number | null;
  motoristaNome: string | null;
  veiculoId: number | null;
  placaVeiculo: string | null;
  janelaAtendimentoId: number | null;
  dataJanela: string | null;
  horaInicioJanela: string | null;
  horaFimJanela: string | null;
  horarioPrevistoChegada: string | null;
  horarioPrevistoSaida: string | null;
  horarioRealChegada: string | null;
  horarioRealSaida: string | null;
  observacoes: string | null;
  documentos: DocumentoAgendamento[] | null;
  gatePass: GatePass | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface AgendamentoFiltro {
  dataInicio?: string;
  dataFim?: string;
  page?: number;
  size?: number;
  sort?: string;
  transportadoraId?: number;
  tipoOperacao?: string;
  status?: string;
}

export interface AgendamentoRequest {
  codigo: string;
  tipoOperacao: string;
  status: string;
  transportadoraId: number;
  motoristaId: number;
  veiculoId: number;
  janelaAtendimentoId: number;
  horarioPrevistoChegada: string;
  horarioPrevistoSaida: string;
  observacoes?: string | null;
  placaVeiculo?: string | null;
  motoristaCpf?: string | null;
}

export interface UploadDocumentoStatus {
  fileName: string;
  progress: number;
  status: 'pendente' | 'enviando' | 'concluido' | 'erro';
}

export interface AgendamentoFormPayload {
  request: AgendamentoRequest;
  arquivos: File[];
}
