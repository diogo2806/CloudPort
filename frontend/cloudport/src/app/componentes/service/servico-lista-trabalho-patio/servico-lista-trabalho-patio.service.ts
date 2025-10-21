import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export type StatusOrdemTrabalhoPatio = 'PENDENTE' | 'EM_EXECUCAO' | 'CONCLUIDA';
export type TipoMovimentoPatio = 'ALOCACAO' | 'ATUALIZACAO' | 'REMOCAO';

export interface OrdemTrabalhoPatio {
  id: number;
  codigoConteiner: string;
  tipoCarga: string;
  destino: string;
  linhaDestino: number;
  colunaDestino: number;
  camadaDestino: string;
  tipoMovimento: TipoMovimentoPatio;
  statusOrdem: StatusOrdemTrabalhoPatio;
  statusConteinerDestino: string;
  criadoEm: string;
  atualizadoEm: string;
  concluidoEm?: string;
}

export interface NovaOrdemTrabalhoPatio {
  codigoConteiner: string;
  tipoCarga: string;
  destino: string;
  linhaDestino: number;
  colunaDestino: number;
  camadaDestino: string;
  tipoMovimento: TipoMovimentoPatio;
  statusConteinerDestino: string;
}

export interface AtualizacaoStatusOrdemTrabalhoPatio {
  statusOrdem: StatusOrdemTrabalhoPatio;
}

@Injectable({ providedIn: 'root' })
export class ServicoListaTrabalhoPatioService {
  private static readonly CAMINHO_BASE = '/yard/patio/ordens';

  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  listarOrdens(status?: StatusOrdemTrabalhoPatio): Observable<OrdemTrabalhoPatio[]> {
    const params = this.criarParametros(status);
    return this.http.get<OrdemTrabalhoPatio[]>(this.construirUrl(''), { params });
  }

  registrarOrdem(payload: NovaOrdemTrabalhoPatio): Observable<OrdemTrabalhoPatio> {
    return this.http.post<OrdemTrabalhoPatio>(this.construirUrl(''), payload);
  }

  atualizarStatus(id: number, status: StatusOrdemTrabalhoPatio): Observable<OrdemTrabalhoPatio> {
    const corpo: AtualizacaoStatusOrdemTrabalhoPatio = { statusOrdem: status };
    return this.http.patch<OrdemTrabalhoPatio>(this.construirUrl(`/${encodeURIComponent(id)}/status`), corpo);
  }

  private construirUrl(caminho: string): string {
    return this.configuracaoAplicacao.construirUrlApi(`${ServicoListaTrabalhoPatioService.CAMINHO_BASE}${caminho}`);
  }

  private criarParametros(status?: StatusOrdemTrabalhoPatio): HttpParams {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return params;
  }
}
