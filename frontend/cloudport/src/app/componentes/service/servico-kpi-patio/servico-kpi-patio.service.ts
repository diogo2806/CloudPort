import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export interface KpiPatio {
  yardDensity: number;
  rehandleRatio: number;
  equipmentUtilization: number;
  gateThroghput: number;
  atualizadoEm: string;
  statusYardDensity: string;
  statusRehandleRatio: string;
  statusEquipmentUtilization: string;
  statusGateThroghput: string;
}

@Injectable({
  providedIn: 'root'
})
export class ServicoKpiPatio {
  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  calcularKpis(): Observable<KpiPatio> {
    const url = this.configuracaoAplicacao.construirUrlApi('/yard/patio/kpi/calcular');
    return this.http.get<KpiPatio>(url);
  }
}
