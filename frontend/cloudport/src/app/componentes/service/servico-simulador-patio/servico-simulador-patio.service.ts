import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export interface CenarioSimulacao {
  tipoCenario: 'ATRASO_NAVIO' | 'MANUTENCAO_EQUIPAMENTO' | 'AUMENTO_VOLUME';
  descricao: string;
  horasAtraso?: number;
  codigoEquipamento?: string;
  quantidadeConteinoresAdicionais?: number;
}

export interface ResultadoSimulacao {
  cenarioDescricao: string;
  totalConteineresCenarioCurrent: number;
  ocupacaoPatioCurrent: number;
  rehandleRatioCurrent: number;
  equipamentosDisponiveisCurrent: number;
  totalConteineresSimulado: number;
  ocupacaoPatioSimulado: number;
  rehandleRatioSimulado: number;
  equipamentosDisponiveisSimulado: number;
  deltaConteineres: number;
  deltaOcupacao: number;
  deltaRehandleRatio: number;
  deltaEquipamentos: number;
  alertaPrincipal: string;
  recomendacao: string;
  impactoProducao: string;
}

@Injectable({
  providedIn: 'root'
})
export class ServicoSimuladorPatio {
  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  simularCenario(cenario: CenarioSimulacao): Observable<ResultadoSimulacao> {
    const url = this.configuracaoAplicacao.construirUrlApi('/yard/patio/simulador/simular');
    return this.http.post<ResultadoSimulacao>(url, cenario);
  }
}
