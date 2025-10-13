import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { DashboardFiltro, DashboardResumo } from '../../model/gate/dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class GateDashboardService {
  private readonly dashboardUrl = `${environment.baseApiUrl}/gate/dashboard`;

  constructor(private readonly http: HttpClient) {}

  consultarResumo(filtro?: DashboardFiltro): Observable<DashboardResumo> {
    const params = this.buildParams(filtro);
    return this.http.get<DashboardResumo>(this.dashboardUrl, { params });
  }

  registrarStream(filtro?: DashboardFiltro): EventSource {
    const params = this.buildParams(filtro);
    const query = params.toString();
    const url = query ? `${this.dashboardUrl}/stream?${query}` : `${this.dashboardUrl}/stream`;
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
}
