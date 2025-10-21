import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export type StatusOrdemMovimentacao = 'PENDENTE' | 'EM_EXECUCAO' | 'CONCLUIDA';
export type TipoMovimentacaoOrdem = 'DESCARGA_TREM' | 'CARGA_TREM';

export interface OrdemMovimentacao {
  id: number;
  idVisitaTrem: number;
  codigoConteiner: string;
  tipoMovimentacao: TipoMovimentacaoOrdem;
  statusMovimentacao: StatusOrdemMovimentacao;
  criadoEm: string;
  atualizadoEm: string;
}

export interface AtualizacaoStatusOrdemMovimentacao {
  statusMovimentacao: StatusOrdemMovimentacao;
}

@Injectable({ providedIn: 'root' })
export class ServicoListaTrabalhoTremService {
  private static readonly CAMINHO_BASE = '/rail/ferrovia/lista-trabalho';

  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  listarOrdens(visitaId: number, status?: StatusOrdemMovimentacao): Observable<OrdemMovimentacao[]> {
    const params = this.criarParametros(status);
    return this.http.get<OrdemMovimentacao[]>(
      this.construirUrl(`/visitas/${encodeURIComponent(visitaId)}/ordens`),
      { params }
    );
  }

  atualizarStatus(visitaId: number,
                  ordemId: number,
                  status: StatusOrdemMovimentacao): Observable<OrdemMovimentacao> {
    const payload: AtualizacaoStatusOrdemMovimentacao = { statusMovimentacao: status };
    return this.http.patch<OrdemMovimentacao>(
      this.construirUrl(`/visitas/${encodeURIComponent(visitaId)}/ordens/${encodeURIComponent(ordemId)}/status`),
      payload
    );
  }

  private construirUrl(caminho: string): string {
    return this.configuracaoAplicacao.construirUrlApi(`${ServicoListaTrabalhoTremService.CAMINHO_BASE}${caminho}`);
  }

  private criarParametros(status?: StatusOrdemMovimentacao): HttpParams {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return params;
  }
}
