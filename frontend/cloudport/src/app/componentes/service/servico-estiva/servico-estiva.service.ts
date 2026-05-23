import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export type TipoCargaConteiner = 'SECO' | 'REFRIGERADO' | 'PERIGOSO' | 'GRANELEIRO' | 'OUTRO';
export type StatusPlanoEstiva = 'RASCUNHO' | 'CONFIRMADO' | 'EM_EXECUCAO' | 'CONCLUIDO';

export interface EscalaResumo {
  id: number;
  navioId: number;
  nomeNavio: string;
  codigoImo: string;
  viagemEntrada: string;
  fase: string;
  chegadaPrevista: string;
  bercoPrevisto?: string | null;
}

export interface AtribuicaoEstiva {
  id: number;
  codigoConteiner: string;
  tipoCarga: TipoCargaConteiner;
  pesoToneladas?: number | null;
  baia: number;
  fileira: number;
  camada: number;
  posicaoPatioOrigem?: string | null;
  sequenciaEmbarque?: number | null;
  embarcado: boolean;
  embarcadoEm?: string | null;
}

export interface PlanoEstivaDetalhe {
  id: number;
  escalaId: number;
  nomeNavio: string;
  viagemEntrada: string;
  status: StatusPlanoEstiva;
  baias: number;
  fileiras: number;
  camadas: number;
  capacidadeCelulas: number;
  totalPlanejado: number;
  totalEmbarcado: number;
  totalPendente: number;
  ocupacaoPercentual: number;
  atribuicoes: AtribuicaoEstiva[];
}

export interface NovoPlanoEstiva {
  baias: number;
  fileiras: number;
  camadas: number;
}

export interface NovaAtribuicaoEstiva {
  codigoConteiner: string;
  tipoCarga: TipoCargaConteiner;
  pesoToneladas?: number | null;
  baia: number;
  fileira: number;
  camada: number;
  posicaoPatioOrigem?: string | null;
  sequenciaEmbarque?: number | null;
}

@Injectable({ providedIn: 'root' })
export class ServicoEstivaService {

  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  listarEscalas(dias = 30): Observable<EscalaResumo[]> {
    const params = new HttpParams().set('dias', String(dias));
    return this.http.get<EscalaResumo[]>(this.construirUrl('/escalas'), { params });
  }

  obterPlano(escalaId: number): Observable<PlanoEstivaDetalhe> {
    return this.http.get<PlanoEstivaDetalhe>(this.construirUrl(`/escalas/${escalaId}/plano-estiva`));
  }

  criarPlano(escalaId: number, payload: NovoPlanoEstiva): Observable<PlanoEstivaDetalhe> {
    return this.http.post<PlanoEstivaDetalhe>(this.construirUrl(`/escalas/${escalaId}/plano-estiva`), payload);
  }

  adicionarAtribuicao(escalaId: number, payload: NovaAtribuicaoEstiva): Observable<PlanoEstivaDetalhe> {
    return this.http.post<PlanoEstivaDetalhe>(
      this.construirUrl(`/escalas/${escalaId}/plano-estiva/atribuicoes`),
      payload
    );
  }

  embarcar(atribuicaoId: number): Observable<PlanoEstivaDetalhe> {
    return this.http.patch<PlanoEstivaDetalhe>(
      this.construirUrl(`/plano-estiva/atribuicoes/${atribuicaoId}/embarcar`),
      {}
    );
  }

  removerAtribuicao(atribuicaoId: number): Observable<PlanoEstivaDetalhe> {
    return this.http.delete<PlanoEstivaDetalhe>(
      this.construirUrl(`/plano-estiva/atribuicoes/${atribuicaoId}`)
    );
  }

  private construirUrl(caminho: string): string {
    return this.configuracaoAplicacao.construirUrlApi(caminho);
  }
}
