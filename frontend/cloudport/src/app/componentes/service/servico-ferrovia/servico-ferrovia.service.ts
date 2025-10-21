import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export type StatusOperacaoConteinerVisita = 'PENDENTE' | 'CONCLUIDO';

export interface OperacaoConteinerVisita {
  codigoConteiner: string;
  statusOperacao: StatusOperacaoConteinerVisita;
}

export interface OperacaoConteinerVisitaEnvio {
  codigoConteiner: string;
  statusOperacao?: StatusOperacaoConteinerVisita;
}

export interface AtualizacaoStatusOperacaoConteiner {
  statusOperacao: StatusOperacaoConteinerVisita;
}

export interface VisitaTrem {
  id: number;
  identificadorTrem: string;
  operadoraFerroviaria: string;
  horaChegadaPrevista: string;
  horaPartidaPrevista: string;
  statusVisita: string;
  listaDescarga: OperacaoConteinerVisita[];
  listaCarga: OperacaoConteinerVisita[];
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
  private static readonly CAMINHO_BASE = '/rail/ferrovia/visitas';

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

  adicionarConteinerDescarga(idVisita: number, payload: OperacaoConteinerVisitaEnvio): Observable<VisitaTrem> {
    return this.http.post<VisitaTrem>(this.construirUrl(`/${idVisita}/descarga`), payload);
  }

  adicionarConteinerCarga(idVisita: number, payload: OperacaoConteinerVisitaEnvio): Observable<VisitaTrem> {
    return this.http.post<VisitaTrem>(this.construirUrl(`/${idVisita}/carga`), payload);
  }

  removerConteinerDescarga(idVisita: number, codigoConteiner: string): Observable<VisitaTrem> {
    return this.http.delete<VisitaTrem>(this.construirUrl(`/${idVisita}/descarga/${encodeURIComponent(codigoConteiner)}`));
  }

  removerConteinerCarga(idVisita: number, codigoConteiner: string): Observable<VisitaTrem> {
    return this.http.delete<VisitaTrem>(this.construirUrl(`/${idVisita}/carga/${encodeURIComponent(codigoConteiner)}`));
  }

  atualizarStatusDescarga(idVisita: number,
                          codigoConteiner: string,
                          payload: AtualizacaoStatusOperacaoConteiner): Observable<VisitaTrem> {
    return this.http.patch<VisitaTrem>(
      this.construirUrl(`/${idVisita}/descarga/${encodeURIComponent(codigoConteiner)}/status`),
      payload
    );
  }

  atualizarStatusCarga(idVisita: number,
                       codigoConteiner: string,
                       payload: AtualizacaoStatusOperacaoConteiner): Observable<VisitaTrem> {
    return this.http.patch<VisitaTrem>(
      this.construirUrl(`/${idVisita}/carga/${encodeURIComponent(codigoConteiner)}/status`),
      payload
    );
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
