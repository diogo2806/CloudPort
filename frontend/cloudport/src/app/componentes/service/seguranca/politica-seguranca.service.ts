import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, throwError } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';
import { SanitizadorConteudoService } from '../sanitizacao/sanitizador-conteudo.service';

export interface DiretrizSeguranca {
  id: string;
  titulo: string;
  descricao: string;
  versao: string;
  ordem: number;
}

export interface ConsultaDiretrizesSeguranca {
  versao?: string;
  ordenacao?: string;
}

@Injectable({ providedIn: 'root' })
export class PoliticaSegurancaService {
  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService,
    private readonly sanitizadorConteudo: SanitizadorConteudoService
  ) {}

  listarDiretrizes(parametros?: ConsultaDiretrizesSeguranca): Observable<DiretrizSeguranca[]> {
    const url = this.configuracaoAplicacao.construirUrlApi('/api/configuracoes/seguranca');
    let parametrosRequisicao = new HttpParams();

    if (parametros?.versao) {
      const versaoSanitizada = this.sanitizadorConteudo.sanitizar(parametros.versao);
      if (versaoSanitizada !== parametros.versao) {
        return throwError(() => new Error('Parâmetro de versão contém caracteres não permitidos.'));
      }
      parametrosRequisicao = parametrosRequisicao.set('versao', versaoSanitizada);
    }

    if (parametros?.ordenacao) {
      const ordenacaoSanitizada = this.sanitizadorConteudo.sanitizar(parametros.ordenacao);
      if (ordenacaoSanitizada !== parametros.ordenacao) {
        return throwError(() => new Error('Parâmetro de ordenação contém caracteres não permitidos.'));
      }
      parametrosRequisicao = parametrosRequisicao.set('ordenacao', ordenacaoSanitizada.toLowerCase());
    }

    return this.http.get<DiretrizSeguranca[]>(url, { params: parametrosRequisicao }).pipe(
      map((diretrizes) =>
        diretrizes.map((diretriz) => ({
          ...diretriz,
          titulo: this.sanitizadorConteudo.sanitizar(diretriz.titulo),
          descricao: this.sanitizadorConteudo.sanitizar(diretriz.descricao),
          versao: this.sanitizadorConteudo.sanitizar(diretriz.versao)
        }))
      )
    );
  }
}
