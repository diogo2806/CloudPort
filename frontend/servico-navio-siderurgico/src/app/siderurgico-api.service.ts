import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';

export type FaseVisita = 'PREVISTA' | 'FUNDEADA' | 'ATRACADA' | 'OPERANDO' | 'OPERACAO_CONCLUIDA' | 'PARTIU' | 'CANCELADA';
export type TipoMovimento = 'EMBARQUE' | 'DESCARGA' | 'RESTOW';
export type TipoOperacao = 'EMBARQUE' | 'DESCARGA';
export type StatusOperacao = 'PLANEJADA' | 'EM_EXECUCAO' | 'PAUSADA' | 'CONCLUIDA' | 'CANCELADA';
export type TipoCarga = 'BOBINA' | 'CHAPA' | 'TARUGO' | 'PLACA' | 'PERFIL' | 'VERGALHAO' | 'OUTROS';
export type StatusItem = 'PLANEJADO' | 'LIBERADO' | 'EM_MOVIMENTO' | 'OPERADO' | 'BLOQUEADO' | 'CANCELADO';
export type StatusIntegracaoPatio = 'NAO_GERADO' | 'RESERVADO' | 'ORDEM_GERADA' | 'EM_EXECUCAO' | 'SINCRONIZADO' | 'ERRO' | 'CANCELADO';
export type StatusPlanoEstiva = 'RASCUNHO' | 'VALIDADO' | 'EM_EXECUCAO' | 'CONCLUIDO' | 'CANCELADO';
export type BordoEstiva = 'BB' | 'BE' | 'CENTRO';
export type TipoReservaPatio = 'TENTATIVA' | 'DEFINITIVA';
export type StatusReservaPatio = 'ATIVA' | 'CONSUMIDA' | 'CANCELADA' | 'EXPIRADA';
export type StatusOrdemPatio = 'PENDENTE' | 'EM_EXECUCAO' | 'BLOQUEADA' | 'SUSPENSA' | 'CONCLUIDA' | 'CANCELADA';

export interface ConfiguracaoRuntime {
  baseApiUrl: string;
}

export interface NavioSiderurgico {
  id?: number;
  nome: string;
  codigoImo: string;
  paisBandeira: string;
  empresaArmadora: string;
  tipoNavio: string;
  loaMetros?: number | null;
  dwtToneladas?: number | null;
  quantidadePoroes: number;
  status?: string;
}

export interface OperacaoSiderurgica {
  id?: number;
  navioId: number;
  navioNome?: string;
  tipoOperacao: TipoOperacao;
  status?: StatusOperacao;
  berco?: string;
  viagem?: string;
  eta?: string;
  origem?: string;
  destino?: string;
  observacoes?: string;
}

export interface ItemCargaSiderurgica {
  id?: number;
  operacaoId?: number;
  codigoLote: string;
  tipoCarga: TipoCarga;
  produto: string;
  quantidade: number;
  pesoUnitarioToneladas?: number | null;
  pesoTotalToneladas: number;
  porao?: number | null;
  posicaoBordo?: string;
  origemPatio?: string;
  destinoPatio?: string;
  sequenciaOperacional?: number | null;
  status?: StatusItem;
}

export interface VisitaNavio {
  id?: number;
  navioId: number;
  navioNome?: string;
  codigoVisita: string;
  viagemEntrada?: string | null;
  viagemSaida?: string | null;
  linhaOperadora?: string | null;
  terminalFacility?: string | null;
  bercoPrevisto?: string | null;
  bercoAtual?: string | null;
  eta?: string | null;
  ata?: string | null;
  etb?: string | null;
  atb?: string | null;
  inicioOperacao?: string | null;
  fimOperacao?: string | null;
  etd?: string | null;
  atd?: string | null;
  janelaRecebimentoInicio?: string | null;
  janelaRecebimentoFim?: string | null;
  cutoffOperacional?: string | null;
  fase?: FaseVisita;
  observacoes?: string | null;
}

export interface ItemOperacaoNavio {
  id?: number;
  visitaNavioId?: number;
  tipoMovimento: TipoMovimento;
  codigoLote: string;
  produto: string;
  tipoCarga: TipoCarga;
  quantidade: number;
  pesoUnitarioToneladas?: number | null;
  pesoTotalToneladas: number;
  poraoPlanejado?: number | null;
  poraoReal?: number | null;
  posicaoPlanejada?: string | null;
  posicaoReal?: string | null;
  origemPatio?: string | null;
  destinoPatio?: string | null;
  conteinerPatioId?: number | null;
  cargaPatioId?: number | null;
  ordemTrabalhoPatioId?: number | null;
  movimentoPatioId?: number | null;
  posicaoPatioPlanejada?: string | null;
  posicaoPatioReal?: string | null;
  statusIntegracaoPatio?: StatusIntegracaoPatio;
  sequenciaOperacional?: number | null;
  status?: StatusItem;
  motivoBloqueio?: string | null;
  observacoes?: string | null;
}

export interface PosicaoEstivaNavio {
  id?: number;
  planoEstivaId?: number;
  itemOperacaoId: number;
  codigoLote?: string;
  porao: number;
  camada: number;
  coluna: number;
  bordo: BordoEstiva;
  sequencia: number;
  pesoToneladas: number;
  status?: string;
}

export interface PlanoEstivaNavio {
  id?: number;
  visitaNavioId?: number;
  versao?: number;
  status?: StatusPlanoEstiva;
  pesoTotalPlanejado?: number;
  pesoTotalRealizado?: number;
  criadoEm?: string;
  validadoEm?: string;
  posicoes: PosicaoEstivaNavio[];
}

export interface ResumoOperacionalNavio {
  totalItensPlanejados: number;
  totalItensOperados: number;
  pesoPlanejado: number;
  pesoOperado: number;
  percentualProgresso: number;
  divergenciasPoraoPosicao: number;
  itensBloqueados: number;
  tempoOperacaoMinutos?: number | null;
}

export interface EventoVisitaNavio {
  id: number;
  visitaNavioId: number;
  itemOperacaoId?: number | null;
  tipoEvento: string;
  descricao: string;
  usuario: string;
  criadoEm: string;
}

export interface ValidacaoPlanoEstiva {
  plano: PlanoEstivaNavio;
  erros: string[];
  alertas: string[];
}

export interface ReservaPatioNavio {
  id?: number | null;
  visitaNavioId: number;
  itemOperacaoNavioId: number;
  posicaoPatioId: string;
  bloco?: string | null;
  linha?: number | null;
  coluna?: number | null;
  camada?: string | null;
  tipoReserva: TipoReservaPatio;
  status: StatusReservaPatio;
  motivoCancelamento?: string | null;
  criadoEm?: string | null;
  atualizadoEm?: string | null;
}

export interface OrdemPatioDaVisita {
  id?: number | null;
  visitaNavioId: number;
  itemOperacaoNavioId: number;
  codigoLote: string;
  tipoMovimento: TipoMovimento;
  statusOrdem: string;
  origem?: string | null;
  destino?: string | null;
  posicaoPlanejada?: string | null;
  posicaoReal?: string | null;
  sequenciaNavio?: number | null;
  prioridadeOperacional?: number | null;
}

export interface FilaPatioDaVisita {
  identificador: string;
  agrupamento: string;
  visitaNavioId: number;
  berco?: string | null;
  blocoZona?: string | null;
  sequenciaInicial?: number | null;
  status: string;
  totalOrdens: number;
  ordens: OrdemPatioDaVisita[];
}

export interface WorkQueuePatioDaVisita {
  id?: number | null;
  identificador: string;
  agrupamento: string;
  visitaNavioId: number;
  berco?: string | null;
  porao?: number | null;
  blocoZona?: string | null;
  sequenciaInicial?: number | null;
  pow?: string | null;
  poolOperacional?: string | null;
  equipamento?: string | null;
  status: string;
  prioridadeOperacional?: number | null;
  totalOrdens: number;
  jobList: OrdemPatioDaVisita[];
  criadoEm?: string | null;
  atualizadoEm?: string | null;
}

export interface AtualizacaoWorkQueuePow {
  pow?: string | null;
  poolOperacional?: string | null;
}

export interface AtualizacaoWorkQueueEquipamento {
  equipamento?: string | null;
}

export interface DispatchWorkQueue {
  limiteOrdens?: number | null;
  operador?: string | null;
  observacao?: string | null;
}

export interface ResultadoDispatchWorkQueue {
  workQueueId: number;
  totalOrdensDespachadas: number;
  ordens: OrdemPatioDaVisita[];
  mensagem?: string | null;
}

export interface AlertaIntegracaoNavioPatio {
  tipo: string;
  severidade: string;
  visitaNavioId: number;
  itemOperacaoNavioId?: number | null;
  ordemTrabalhoPatioId?: number | null;
  mensagem: string;
}

export interface ResumoIntegracaoNavioPatio {
  visitaNavioId: number;
  totalItens: number;
  itensComReserva: number;
  itensComOrdem: number;
  itensSemReserva: number;
  itensSemOrdem: number;
  ordensEmExecucao: number;
  ordensConcluidas: number;
  totalAlertas: number;
  statusPredominante: StatusIntegracaoPatio;
}

export interface ResultadoGeracaoOrdensPatio {
  totalOrdensCriadas: number;
  totalItensIgnorados: number;
  totalItensComErro: number;
  errosPorItem: string[];
  alertas: string[];
}

export interface ResultadoReplanejamentoPatioNavio {
  reservasSugeridas: ReservaPatioNavio[];
  ordensReordenadas: OrdemPatioDaVisita[];
  economiaEstimadaDistanciaPercentual: number;
  riscoRehandle: string;
  alertasImpeditivos: string[];
  itensNaoReplanejados: number[];
}

export interface RelatorioOperacionalIntegrado {
  visita: VisitaNavio;
  resumoOperacional: ResumoOperacionalNavio;
  resumoIntegracao: ResumoIntegracaoNavioPatio;
  planoEstiva?: PlanoEstivaNavio | null;
  itens: ItemOperacaoNavio[];
  reservasPatio: ReservaPatioNavio[];
  ordensPatio: OrdemPatioDaVisita[];
  divergenciasAlertas: AlertaIntegracaoNavioPatio[];
  eventosRelevantes: EventoVisitaNavio[];
}

@Injectable({ providedIn: 'root' })
export class SiderurgicoApiService {
  private baseApiUrl = '';

  constructor(private readonly http: HttpClient) {}

  async carregarConfiguracao(): Promise<void> {
    const configuracao = await firstValueFrom(this.http.get<ConfiguracaoRuntime>('assets/configuracao.json'));
    this.baseApiUrl = configuracao.baseApiUrl.replace(/\/+$/, '');
  }

  listarNavios(): Promise<NavioSiderurgico[]> {
    return firstValueFrom(this.http.get<NavioSiderurgico[]>(`${this.baseApiUrl}/navios-siderurgicos`).pipe(timeout(5000)));
  }

  criarNavio(navio: NavioSiderurgico): Promise<NavioSiderurgico> {
    return firstValueFrom(this.http.post<NavioSiderurgico>(`${this.baseApiUrl}/navios-siderurgicos`, navio).pipe(timeout(5000)));
  }

  listarVisitas(): Promise<VisitaNavio[]> {
    return firstValueFrom(this.http.get<VisitaNavio[]>(`${this.baseApiUrl}/visitas-navio`).pipe(timeout(5000)));
  }

  criarVisita(visita: VisitaNavio): Promise<VisitaNavio> {
    return firstValueFrom(this.http.post<VisitaNavio>(`${this.baseApiUrl}/visitas-navio`, visita).pipe(timeout(5000)));
  }

  alterarFaseVisita(visitaId: number, fase: FaseVisita): Promise<VisitaNavio> {
    return firstValueFrom(this.http.patch<VisitaNavio>(`${this.baseApiUrl}/visitas-navio/${visitaId}/fase`, { fase }).pipe(timeout(5000)));
  }

  listarItensVisita(visitaId: number): Promise<ItemOperacaoNavio[]> {
    return firstValueFrom(this.http.get<ItemOperacaoNavio[]>(`${this.baseApiUrl}/visitas-navio/${visitaId}/itens`).pipe(timeout(5000)));
  }

  criarItemVisita(visitaId: number, item: ItemOperacaoNavio): Promise<ItemOperacaoNavio> {
    return firstValueFrom(this.http.post<ItemOperacaoNavio>(`${this.baseApiUrl}/visitas-navio/${visitaId}/itens`, item).pipe(timeout(5000)));
  }

  alterarStatusItem(visitaId: number, itemId: number, status: StatusItem): Promise<ItemOperacaoNavio> {
    return firstValueFrom(this.http.patch<ItemOperacaoNavio>(`${this.baseApiUrl}/visitas-navio/${visitaId}/itens/${itemId}/status`, { status }).pipe(timeout(5000)));
  }

  alterarBloqueioItem(visitaId: number, itemId: number, bloqueado: boolean, motivo?: string): Promise<ItemOperacaoNavio> {
    return firstValueFrom(this.http.patch<ItemOperacaoNavio>(`${this.baseApiUrl}/visitas-navio/${visitaId}/itens/${itemId}/bloqueio`, { bloqueado, motivo }).pipe(timeout(5000)));
  }

  obterResumo(visitaId: number): Promise<ResumoOperacionalNavio> {
    return firstValueFrom(this.http.get<ResumoOperacionalNavio>(`${this.baseApiUrl}/visitas-navio/${visitaId}/resumo-operacional`).pipe(timeout(5000)));
  }

  listarEventos(visitaId: number): Promise<EventoVisitaNavio[]> {
    return firstValueFrom(this.http.get<EventoVisitaNavio[]>(`${this.baseApiUrl}/visitas-navio/${visitaId}/eventos`).pipe(timeout(5000)));
  }

  obterPlanoEstiva(visitaId: number): Promise<PlanoEstivaNavio> {
    return firstValueFrom(this.http.get<PlanoEstivaNavio>(`${this.baseApiUrl}/visitas-navio/${visitaId}/plano-estiva`).pipe(timeout(5000)));
  }

  criarPlanoEstiva(visitaId: number): Promise<PlanoEstivaNavio> {
    return firstValueFrom(this.http.post<PlanoEstivaNavio>(`${this.baseApiUrl}/visitas-navio/${visitaId}/plano-estiva`, { posicoes: [] }).pipe(timeout(5000)));
  }

  salvarPosicoesPlano(visitaId: number, planoId: number, posicoes: PosicaoEstivaNavio[]): Promise<PlanoEstivaNavio> {
    return firstValueFrom(this.http.put<PlanoEstivaNavio>(`${this.baseApiUrl}/visitas-navio/${visitaId}/plano-estiva/${planoId}/posicoes`, posicoes).pipe(timeout(5000)));
  }

  validarPlano(visitaId: number, planoId: number): Promise<ValidacaoPlanoEstiva> {
    return firstValueFrom(this.http.post<ValidacaoPlanoEstiva>(`${this.baseApiUrl}/visitas-navio/${visitaId}/plano-estiva/${planoId}/validar`, {}).pipe(timeout(5000)));
  }

  obterResumoIntegracaoPatio(visitaId: number): Promise<ResumoIntegracaoNavioPatio> {
    return firstValueFrom(this.http.get<ResumoIntegracaoNavioPatio>(`${this.baseApiUrl}/visitas-navio/${visitaId}/integracao-patio`).pipe(timeout(5000)));
  }

  gerarReservasPatio(visitaId: number, tipoReserva: TipoReservaPatio = 'TENTATIVA'): Promise<ReservaPatioNavio[]> {
    return firstValueFrom(this.http.post<ReservaPatioNavio[]>(`${this.baseApiUrl}/visitas-navio/${visitaId}/integracao-patio/reservas`, { tipoReserva, somentePendentes: true }).pipe(timeout(5000)));
  }

  listarReservasPatio(visitaId: number): Promise<ReservaPatioNavio[]> {
    return firstValueFrom(this.http.get<ReservaPatioNavio[]>(`${this.baseApiUrl}/visitas-navio/${visitaId}/integracao-patio/reservas`).pipe(timeout(5000)));
  }

  gerarOrdensPatio(visitaId: number, tipoMovimento?: TipoMovimento): Promise<ResultadoGeracaoOrdensPatio> {
    return firstValueFrom(this.http.post<ResultadoGeracaoOrdensPatio>(`${this.baseApiUrl}/visitas-navio/${visitaId}/integracao-patio/gerar-ordens`, { tipoMovimento, modo: 'SOMENTE_PENDENTES', gerarReservasAutomaticas: true }).pipe(timeout(5000)));
  }

  listarOrdensPatio(visitaId: number): Promise<OrdemPatioDaVisita[]> {
    return firstValueFrom(this.http.get<OrdemPatioDaVisita[]>(`${this.baseApiUrl}/visitas-navio/${visitaId}/integracao-patio/ordens`).pipe(timeout(5000)));
  }

  atualizarPrioridadeOrdemPatio(visitaId: number, ordemId: number, prioridadeOperacional: number, prioridadeBusca = false): Promise<OrdemPatioDaVisita> {
    return firstValueFrom(this.http.patch<OrdemPatioDaVisita>(`${this.baseApiUrl}/visitas-navio/${visitaId}/integracao-patio/ordens/${ordemId}/prioridade`, { prioridadeOperacional, prioridadeBusca }).pipe(timeout(5000)));
  }

  suspenderOrdemPatio(visitaId: number, ordemId: number): Promise<OrdemPatioDaVisita> {
    return firstValueFrom(this.http.patch<OrdemPatioDaVisita>(`${this.baseApiUrl}/visitas-navio/${visitaId}/integracao-patio/ordens/${ordemId}/suspender`, {}).pipe(timeout(5000)));
  }

  retomarOrdemPatio(visitaId: number, ordemId: number): Promise<OrdemPatioDaVisita> {
    return firstValueFrom(this.http.patch<OrdemPatioDaVisita>(`${this.baseApiUrl}/visitas-navio/${visitaId}/integracao-patio/ordens/${ordemId}/retomar`, {}).pipe(timeout(5000)));
  }

  listarFilasPatio(visitaId: number): Promise<FilaPatioDaVisita[]> {
    return firstValueFrom(this.http.get<FilaPatioDaVisita[]>(`${this.baseApiUrl}/visitas-navio/${visitaId}/integracao-patio/filas`).pipe(timeout(5000)));
  }

  listarWorkQueuesPatio(visitaId: number): Promise<WorkQueuePatioDaVisita[]> {
    return firstValueFrom(this.http.get<WorkQueuePatioDaVisita[]>(`${this.baseApiUrl}/visitas-navio/${visitaId}/integracao-patio/work-queues`).pipe(timeout(5000)));
  }

  ativarWorkQueuePatio(workQueueId: number): Promise<WorkQueuePatioDaVisita> {
    return firstValueFrom(this.http.patch<WorkQueuePatioDaVisita>(`${this.baseApiUrl}/yard/patio/work-queues/${workQueueId}/ativar`, {}).pipe(timeout(5000)));
  }

  desativarWorkQueuePatio(workQueueId: number): Promise<WorkQueuePatioDaVisita> {
    return firstValueFrom(this.http.patch<WorkQueuePatioDaVisita>(`${this.baseApiUrl}/yard/patio/work-queues/${workQueueId}/desativar`, {}).pipe(timeout(5000)));
  }

  atualizarPowWorkQueuePatio(workQueueId: number, atualizacao: AtualizacaoWorkQueuePow): Promise<WorkQueuePatioDaVisita> {
    return firstValueFrom(this.http.patch<WorkQueuePatioDaVisita>(`${this.baseApiUrl}/yard/patio/work-queues/${workQueueId}/pow`, atualizacao).pipe(timeout(5000)));
  }

  atualizarEquipamentoWorkQueuePatio(workQueueId: number, atualizacao: AtualizacaoWorkQueueEquipamento): Promise<WorkQueuePatioDaVisita> {
    return firstValueFrom(this.http.patch<WorkQueuePatioDaVisita>(`${this.baseApiUrl}/yard/patio/work-queues/${workQueueId}/equipamento`, atualizacao).pipe(timeout(5000)));
  }

  despacharWorkQueuePatio(workQueueId: number, dispatch: DispatchWorkQueue = {}): Promise<ResultadoDispatchWorkQueue> {
    return firstValueFrom(this.http.post<ResultadoDispatchWorkQueue>(`${this.baseApiUrl}/yard/patio/work-queues/${workQueueId}/dispatch`, dispatch).pipe(timeout(5000)));
  }

  resetarWorkInstructionPatio(workInstructionId: number): Promise<OrdemPatioDaVisita> {
    return firstValueFrom(this.http.post<OrdemPatioDaVisita>(`${this.baseApiUrl}/yard/patio/work-instructions/${workInstructionId}/reset`, {}).pipe(timeout(5000)));
  }

  cancelarWorkInstructionPatio(workInstructionId: number): Promise<OrdemPatioDaVisita> {
    return firstValueFrom(this.http.post<OrdemPatioDaVisita>(`${this.baseApiUrl}/yard/patio/work-instructions/${workInstructionId}/cancelar`, {}).pipe(timeout(5000)));
  }

  listarOrdensSemCoberturaPatio(visitaId: number): Promise<OrdemPatioDaVisita[]> {
    return firstValueFrom(this.http.get<OrdemPatioDaVisita[]>(`${this.baseApiUrl}/visitas-navio/${visitaId}/integracao-patio/sem-cobertura`).pipe(timeout(5000)));
  }

  listarAlertasIntegracaoPatio(visitaId: number): Promise<AlertaIntegracaoNavioPatio[]> {
    return firstValueFrom(this.http.get<AlertaIntegracaoNavioPatio[]>(`${this.baseApiUrl}/visitas-navio/${visitaId}/integracao-patio/alertas`).pipe(timeout(5000)));
  }

  sincronizarStatusPatio(visitaId: number): Promise<ResumoIntegracaoNavioPatio> {
    return firstValueFrom(this.http.post<ResumoIntegracaoNavioPatio>(`${this.baseApiUrl}/visitas-navio/${visitaId}/integracao-patio/sincronizar-status`, {}).pipe(timeout(5000)));
  }

  replanejarPatioVisita(visitaId: number, aplicar = false): Promise<ResultadoReplanejamentoPatioNavio> {
    return firstValueFrom(this.http.post<ResultadoReplanejamentoPatioNavio>(`${this.baseApiUrl}/visitas-navio/${visitaId}/integracao-patio/replanejar`, { aplicar }).pipe(timeout(5000)));
  }

  obterRelatorioOperacionalIntegrado(visitaId: number): Promise<RelatorioOperacionalIntegrado> {
    return firstValueFrom(this.http.get<RelatorioOperacionalIntegrado>(`${this.baseApiUrl}/visitas-navio/${visitaId}/relatorio-operacional-integrado`).pipe(timeout(5000)));
  }
}
