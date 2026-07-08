import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';

export type FaseVisita = 'PREVISTA' | 'FUNDEADA' | 'ATRACADA' | 'OPERANDO' | 'OPERACAO_CONCLUIDA' | 'PARTIU' | 'CANCELADA';
export type TipoMovimento = 'EMBARQUE' | 'DESCARGA' | 'RESTOW';
export type TipoOperacao = 'EMBARQUE' | 'DESCARGA';
export type StatusOperacao = 'PLANEJADA' | 'EM_EXECUCAO' | 'PAUSADA' | 'CONCLUIDA' | 'CANCELADA';
export type TipoCarga = 'BOBINA' | 'CHAPA' | 'TARUGO' | 'PLACA' | 'PERFIL' | 'VERGALHAO' | 'OUTROS';
export type StatusItem = 'PLANEJADO' | 'LIBERADO' | 'EM_MOVIMENTO' | 'OPERADO' | 'BLOQUEADO' | 'CANCELADO';
export type StatusPlanoEstiva = 'RASCUNHO' | 'VALIDADO' | 'EM_EXECUCAO' | 'CONCLUIDO' | 'CANCELADO';
export type BordoEstiva = 'BB' | 'BE' | 'CENTRO';

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
}
