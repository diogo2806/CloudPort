import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export interface CanalNotificacao {
  identificador: number;
  nomeCanal: string;
  habilitado: boolean;
}

interface CanalNotificacaoResposta {
  identificador: number;
  nomeCanal: string;
  habilitado: boolean;
}

@Injectable({ providedIn: 'root' })
export class NotificacoesService {
  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  listarCanais(): Observable<CanalNotificacao[]> {
    const url = this.configuracaoAplicacao.construirUrlApi('/api/configuracoes/notificacoes');
    return this.http.get<CanalNotificacaoResposta[]>(url).pipe(
      map(canais => canais.map(canal => this.sanitizarCanal(canal))),
      catchError(erro => this.tratarErro(erro, 'Não foi possível carregar as preferências de notificações.'))
    );
  }

  atualizarStatus(identificador: number, habilitado: boolean): Observable<CanalNotificacao> {
    const url = this.configuracaoAplicacao.construirUrlApi(`/api/configuracoes/notificacoes/${identificador}`);
    return this.http.patch<CanalNotificacaoResposta>(url, { habilitado }).pipe(
      map(resposta => this.sanitizarCanal(resposta)),
      catchError(erro => this.tratarErro(erro, 'Não foi possível atualizar o canal de notificação.'))
    );
  }

  private sanitizarCanal(canal: CanalNotificacaoResposta): CanalNotificacao {
    return {
      identificador: canal.identificador,
      nomeCanal: this.sanitizarTexto(canal.nomeCanal),
      habilitado: canal.habilitado
    };
  }

  private tratarErro(erro: unknown, mensagemPadrao: string): Observable<never> {
    let mensagem = mensagemPadrao;
    if (erro instanceof HttpErrorResponse) {
      if (typeof erro.error === 'string' && erro.error.trim().length > 0) {
        mensagem = erro.error;
      } else if (erro.error && typeof erro.error === 'object') {
        const corpoErro = erro.error as { mensagem?: unknown; message?: unknown };
        const mensagemErro = typeof corpoErro.mensagem === 'string' ? corpoErro.mensagem : corpoErro.message;
        if (typeof mensagemErro === 'string' && mensagemErro.trim().length > 0) {
          mensagem = mensagemErro;
        }
      } else if (erro.message) {
        mensagem = erro.message;
      }
    }
    const mensagemSanitizada = this.sanitizarTexto(mensagem) || mensagemPadrao;
    return throwError(() => new Error(mensagemSanitizada));
  }

  private sanitizarTexto(valor: unknown): string {
    if (typeof valor !== 'string') {
      return '';
    }
    const normalizado = valor.normalize('NFKC').trim();
    return normalizado.replace(/[\u0000-\u001F\u007F<>]/g, '');
  }
}
