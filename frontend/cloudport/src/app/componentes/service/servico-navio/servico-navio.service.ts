import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export type FaseEscala =
  | 'PREVISTA'
  | 'INBOUND'
  | 'ATRACADO'
  | 'OPERANDO'
  | 'PARTIU'
  | 'ENCERRADA'
  | 'CANCELADA';

export interface NavioResumo {
  identificador: number;
  nome: string;
  codigoImo: string;
  empresaArmadora: string;
  capacidadeTeu: number;
}

export interface NavioDetalhe extends NavioResumo {
  paisBandeira: string;
  loaMetros?: number | null;
  caladoMaximoMetros?: number | null;
  callSign?: string | null;
}

export interface CadastroNavio {
  nome: string;
  codigoImo: string;
  paisBandeira: string;
  empresaArmadora: string;
  capacidadeTeu: number;
  loaMetros?: number | null;
  caladoMaximoMetros?: number | null;
  callSign?: string | null;
}

export interface EscalaResumo {
  id: number;
  navioId: number;
  nomeNavio: string;
  codigoImo: string;
  viagemEntrada: string;
  fase: FaseEscala;
  chegadaPrevista: string;
  bercoPrevisto?: string | null;
}

export interface EscalaDetalhe {
  id: number;
  navioId: number;
  nomeNavio: string;
  codigoImo: string;
  viagemEntrada: string;
  viagemSaida?: string | null;
  fase: FaseEscala;
  chegadaPrevista: string;
  atracacaoPrevista?: string | null;
  partidaPrevista?: string | null;
  chegadaEfetiva?: string | null;
  atracacaoEfetiva?: string | null;
  partidaEfetiva?: string | null;
  bercoPrevisto?: string | null;
  bercoAtual?: string | null;
  observacoes?: string | null;
}

export interface CadastroEscala {
  viagemEntrada: string;
  viagemSaida?: string | null;
  chegadaPrevista: string;
  atracacaoPrevista?: string | null;
  partidaPrevista?: string | null;
  bercoPrevisto?: string | null;
  observacoes?: string | null;
}

export interface AtualizacaoEscala {
  viagemEntrada?: string | null;
  viagemSaida?: string | null;
  chegadaPrevista?: string | null;
  atracacaoPrevista?: string | null;
  partidaPrevista?: string | null;
  bercoPrevisto?: string | null;
  bercoAtual?: string | null;
  observacoes?: string | null;
}

/**
 * Transições de fase permitidas (espelham as regras do EscalaServico no backend).
 * Inspiradas nas visit phases do Navis N4.
 */
export const TRANSICOES_FASE: Record<FaseEscala, FaseEscala[]> = {
  PREVISTA: ['INBOUND', 'ATRACADO', 'CANCELADA'],
  INBOUND: ['ATRACADO', 'CANCELADA'],
  ATRACADO: ['OPERANDO', 'PARTIU', 'CANCELADA'],
  OPERANDO: ['PARTIU'],
  PARTIU: ['ENCERRADA'],
  ENCERRADA: [],
  CANCELADA: []
};

export const ROTULOS_FASE: Record<FaseEscala, string> = {
  PREVISTA: 'Prevista',
  INBOUND: 'Em aproximação',
  ATRACADO: 'Atracado',
  OPERANDO: 'Em operação',
  PARTIU: 'Partiu',
  ENCERRADA: 'Encerrada',
  CANCELADA: 'Cancelada'
};

@Injectable({
  providedIn: 'root'
})
export class ServicoNavioService {
  private static readonly CAMINHO_NAVIOS = '/navios';
  private static readonly CAMINHO_ESCALAS = '/escalas';

  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  listarNavios(): Observable<NavioResumo[]> {
    return this.http.get<NavioResumo[]>(this.urlNavios(''));
  }

  obterNavio(id: number): Observable<NavioDetalhe> {
    return this.http.get<NavioDetalhe>(this.urlNavios(`/${id}`));
  }

  registrarNavio(payload: CadastroNavio): Observable<NavioDetalhe> {
    return this.http.post<NavioDetalhe>(this.urlNavios(''), payload);
  }

  removerNavio(id: number): Observable<void> {
    return this.http.delete<void>(this.urlNavios(`/${id}`));
  }

  listarCronograma(dias: number): Observable<EscalaResumo[]> {
    const params = new HttpParams().set('dias', this.normalizarJanela(dias).toString());
    return this.http.get<EscalaResumo[]>(this.urlEscalas(''), { params });
  }

  listarEscalasPorNavio(navioId: number): Observable<EscalaResumo[]> {
    return this.http.get<EscalaResumo[]>(this.urlNavios(`/${navioId}/escalas`));
  }

  obterEscala(id: number): Observable<EscalaDetalhe> {
    return this.http.get<EscalaDetalhe>(this.urlEscalas(`/${id}`));
  }

  registrarEscala(navioId: number, payload: CadastroEscala): Observable<EscalaDetalhe> {
    return this.http.post<EscalaDetalhe>(this.urlNavios(`/${navioId}/escalas`), payload);
  }

  atualizarEscala(id: number, payload: AtualizacaoEscala): Observable<EscalaDetalhe> {
    return this.http.put<EscalaDetalhe>(this.urlEscalas(`/${id}`), payload);
  }

  avancarFase(id: number, fase: FaseEscala): Observable<EscalaDetalhe> {
    return this.http.patch<EscalaDetalhe>(this.urlEscalas(`/${id}/fase`), { fase });
  }

  removerEscala(id: number): Observable<void> {
    return this.http.delete<void>(this.urlEscalas(`/${id}`));
  }

  private urlNavios(caminho: string): string {
    return this.configuracaoAplicacao.construirUrlApi(`${ServicoNavioService.CAMINHO_NAVIOS}${caminho}`);
  }

  private urlEscalas(caminho: string): string {
    return this.configuracaoAplicacao.construirUrlApi(`${ServicoNavioService.CAMINHO_ESCALAS}${caminho}`);
  }

  private normalizarJanela(dias: number): number {
    if (!Number.isFinite(dias)) {
      return 7;
    }
    const inteiro = Math.floor(Math.abs(dias));
    if (inteiro < 1) {
      return 1;
    }
    if (inteiro > 60) {
      return 60;
    }
    return inteiro;
  }
}
