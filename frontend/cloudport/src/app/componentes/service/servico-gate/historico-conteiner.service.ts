import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export interface DetalheConteiner {
  identificador: number;
  identificacao: string;
  posicaoPatio: string;
  tipoCarga: string;
  pesoToneladas: number;
  restricoes: string | null;
  statusOperacional: string;
  ultimaAtualizacao: string;
}

export interface HistoricoConteiner {
  tipoOperacao: string;
  descricao: string;
  posicaoAnterior: string | null;
  posicaoAtual: string | null;
  responsavel: string | null;
  dataRegistro: string;
}

@Injectable({
  providedIn: 'root'
})
export class HistoricoConteinerService {
  private readonly detalhesEndpoint = '/yard/conteineres/por-codigo';
  private readonly historicoEndpoint = '/yard/conteineres/por-codigo/historico';

  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  obterDetalhePorCodigo(codigoConteiner: string): Observable<DetalheConteiner> {
    const params = this.construirParametros(codigoConteiner);
    if (!params) {
      return throwError(() => new Error('Código do contêiner inválido.'));
    }
    return this.http.get<DetalheConteiner>(
      this.configuracaoAplicacao.construirUrlApi(this.detalhesEndpoint),
      { params }
    );
  }

  obterHistoricoPorCodigo(codigoConteiner: string): Observable<HistoricoConteiner[]> {
    const params = this.construirParametros(codigoConteiner);
    if (!params) {
      return throwError(() => new Error('Código do contêiner inválido.'));
    }
    return this.http.get<HistoricoConteiner[]>(
      this.configuracaoAplicacao.construirUrlApi(this.historicoEndpoint),
      { params }
    );
  }

  private construirParametros(codigoConteiner: string): HttpParams | null {
    const codigo = (codigoConteiner ?? '').trim();
    if (!codigo) {
      return null;
    }
    return new HttpParams().set('codigo', codigo.toUpperCase());
  }
}
