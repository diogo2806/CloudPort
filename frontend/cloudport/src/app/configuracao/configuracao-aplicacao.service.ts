import { Inject, Injectable, InjectionToken, Optional } from '@angular/core';

export interface ConfiguracaoAplicacao {
  baseApiUrl: string;
}

export const CONFIGURACAO_APLICACAO_TOKEN = new InjectionToken<ConfiguracaoAplicacao>('CONFIGURACAO_APLICACAO_TOKEN');

@Injectable({
  providedIn: 'root'
})
export class ConfiguracaoAplicacaoService {
  private configuracao?: ConfiguracaoAplicacao;

  constructor(
    @Optional()
    @Inject(CONFIGURACAO_APLICACAO_TOKEN)
    configuracaoInicial?: ConfiguracaoAplicacao
  ) {
    if (configuracaoInicial) {
      this.definirConfiguracao(configuracaoInicial);
    }
  }

  definirConfiguracao(configuracao: ConfiguracaoAplicacao): void {
    if (!configuracao || typeof configuracao.baseApiUrl !== 'string') {
      throw new Error('Configuração inválida: "baseApiUrl" deve ser informada.');
    }
    const baseNormalizada = configuracao.baseApiUrl.trim();
    if (!baseNormalizada) {
      throw new Error('Configuração inválida: "baseApiUrl" deve ser informada.');
    }
    this.configuracao = {
      baseApiUrl: baseNormalizada.replace(/\/+$/, '')
    };
  }

  obterConfiguracao(): ConfiguracaoAplicacao {
    if (!this.configuracao) {
      throw new Error('A configuração da aplicação não foi carregada.');
    }
    return this.configuracao;
  }

  obterUrlBaseApi(): string {
    return this.obterConfiguracao().baseApiUrl;
  }

  construirUrlApi(caminhoRelativo: string): string {
    const base = this.obterUrlBaseApi();
    const caminhoNormalizado = caminhoRelativo.startsWith('/')
      ? caminhoRelativo
      : `/${caminhoRelativo}`;
    const urlConcatenada = `${base}${caminhoNormalizado}`;
    return urlConcatenada.replace(/([^:]\/)\/+/g, '$1');
  }

  construirUrlWebsocket(caminhoRelativo: string): string {
    const urlHttp = this.construirUrlApi(caminhoRelativo);
    if (urlHttp.startsWith('https://')) {
      return `wss://${urlHttp.substring('https://'.length)}`;
    }
    if (urlHttp.startsWith('http://')) {
      return `ws://${urlHttp.substring('http://'.length)}`;
    }
    return urlHttp;
  }
}
