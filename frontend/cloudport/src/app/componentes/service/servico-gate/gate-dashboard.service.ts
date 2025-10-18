import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';
import { DashboardFiltro, DashboardResumo } from '../../model/gate/dashboard.model';
import { GateEnumOption } from '../../model/gate/agendamento.model';

@Injectable({
  providedIn: 'root'
})
export class GateDashboardService {
  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  consultarResumo(filtro?: DashboardFiltro): Observable<DashboardResumo> {
    const params = this.buildParams(filtro);
    return this.http.get<DashboardResumo>(this.obterDashboardUrl(), { params });
  }

  exportarResumo(formato: 'csv' | 'xlsx', filtro?: DashboardFiltro): Observable<Blob> {
    const params = this.buildParams(filtro);
    const endpoint = `${this.obterDashboardUrl()}/relatorios/${formato}`;
    const headers = formato === 'csv'
      ? { Accept: 'text/csv' }
      : { Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' };
    return this.http.get(endpoint, {
      params,
      headers,
      responseType: 'blob'
    });
  }

  listarTiposOperacao(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.obterConfigUrl()}/tipos-operacao`);
  }

  listarTransportadoras(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.obterConfigUrl()}/transportadoras`);
  }

  registrarStream(filtro?: DashboardFiltro): EventSource {
    const params = this.buildParams(filtro);
    const query = params.toString();
    const urlBase = this.obterDashboardUrl();
    const url = query ? `${urlBase}/stream?${query}` : `${urlBase}/stream`;
    return new EventSource(url, { withCredentials: true });
  }

  private buildParams(filters?: DashboardFiltro): HttpParams {
    let params = new HttpParams();
    if (!filters) {
      return params;
    }

    Object.entries(filters)
      .filter(([, value]) => value !== undefined && value !== null && value !== '')
      .forEach(([key, value]) => {
        params = params.set(key, String(value));
      });

    return params;
  }
  private obterDashboardUrl(): string {
    return this.configuracaoAplicacao.construirUrlApi('/gate/dashboard');
  }

  private obterConfigUrl(): string {
    return this.configuracaoAplicacao.construirUrlApi('/gate/config');
  }
}
