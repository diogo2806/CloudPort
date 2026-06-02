import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export interface RespostaAutoplanejamento {
  totalConteineresPlanificados: number;
  totalConteineresSucesso: number;
  totalConteineresFalha: number;
  percentualSucesso: number;
  containersPlanificados: string[];
  containersException: string[];
  mensagemResumo: string;
  temExcecoes: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ServicoAutomacaoPatio {
  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  executarAutoplanejamento(): Observable<RespostaAutoplanejamento> {
    const url = this.configuracaoAplicacao.construirUrlApi('/yard/patio/automacao/executar-autoplanejamento');
    return this.http.post<RespostaAutoplanejamento>(url, {});
  }
}
