import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';

export type TipoOperacao = 'EMBARQUE' | 'DESCARGA';
export type StatusOperacao = 'PLANEJADA' | 'EM_EXECUCAO' | 'PAUSADA' | 'CONCLUIDA' | 'CANCELADA';
export type TipoCarga = 'BOBINA' | 'CHAPA' | 'TARUGO' | 'PLACA' | 'PERFIL' | 'VERGALHAO' | 'OUTROS';
export type StatusItem = 'PLANEJADO' | 'LIBERADO' | 'EM_MOVIMENTO' | 'OPERADO' | 'BLOQUEADO' | 'CANCELADO';

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

  listarOperacoes(navioId?: number): Promise<OperacaoSiderurgica[]> {
    const query = navioId ? `?navioId=${navioId}` : '';
    return firstValueFrom(this.http.get<OperacaoSiderurgica[]>(`${this.baseApiUrl}/operacoes-siderurgicas${query}`).pipe(timeout(5000)));
  }

  criarOperacao(operacao: OperacaoSiderurgica): Promise<OperacaoSiderurgica> {
    return firstValueFrom(this.http.post<OperacaoSiderurgica>(`${this.baseApiUrl}/operacoes-siderurgicas`, operacao).pipe(timeout(5000)));
  }

  listarItens(operacaoId: number): Promise<ItemCargaSiderurgica[]> {
    return firstValueFrom(this.http.get<ItemCargaSiderurgica[]>(`${this.baseApiUrl}/operacoes-siderurgicas/${operacaoId}/itens`).pipe(timeout(5000)));
  }

  criarItem(operacaoId: number, item: ItemCargaSiderurgica): Promise<ItemCargaSiderurgica> {
    return firstValueFrom(this.http.post<ItemCargaSiderurgica>(`${this.baseApiUrl}/operacoes-siderurgicas/${operacaoId}/itens`, item).pipe(timeout(5000)));
  }
}
