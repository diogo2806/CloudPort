import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export interface VisitaTrem {
  id: number;
  identificadorTrem: string;
  operadoraFerroviaria: string;
  horaChegadaPrevista: string;
  horaPartidaPrevista: string;
  statusVisita: string;
}

export interface VisitaTremRequisicao {
  identificadorTrem: string;
  operadoraFerroviaria: string;
  horaChegadaPrevista: string;
  horaPartidaPrevista: string;
  statusVisita: string;
}

@Injectable({
  providedIn: 'root'
})
export class ServicoFerroviaService {
  private static readonly CAMINHO_BASE = '/yard/ferrovia/visitas';

  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  listarVisitasProximosDias(dias: number): Observable<VisitaTrem[]> {
    const diasAjustado = this.normalizarJanela(dias);
    const params = new HttpParams().set('dias', diasAjustado.toString());
    return this.http.get<VisitaTrem[]>(this.construirUrl(''), { params });
  }

  obterVisita(id: number): Observable<VisitaTrem> {
    return this.http.get<VisitaTrem>(this.construirUrl(`/${id}`));
  }

  registrarVisita(payload: VisitaTremRequisicao): Observable<VisitaTrem> {
    return this.http.post<VisitaTrem>(this.construirUrl(''), payload);
  }

  atualizarVisita(id: number, payload: VisitaTremRequisicao): Observable<VisitaTrem> {
    return this.http.put<VisitaTrem>(this.construirUrl(`/${id}`), payload);
  }

  private construirUrl(caminho: string): string {
    return this.configuracaoAplicacao.construirUrlApi(`${ServicoFerroviaService.CAMINHO_BASE}${caminho}`);
  }

  private normalizarJanela(dias: number): number {
    if (!Number.isFinite(dias)) {
      return 7;
    }
    const inteiro = Math.floor(Math.abs(dias));
    if (inteiro < 1) {
      return 1;
    }
    if (inteiro > 30) {
      return 30;
    }
    return inteiro;
  }
}
