import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  Agendamento,
  AgendamentoFiltro,
  AgendamentoRequest,
  DocumentoAgendamento,
  GateEnumOption,
  Page
} from '../../model/gate/agendamento.model';
import {
  JanelaAtendimento,
  JanelaAtendimentoRequest,
  JanelaFiltro
} from '../../model/gate/janela.model';

@Injectable({
  providedIn: 'root'
})
export class GateApiService {
  private readonly baseUrl = `${environment.baseApiUrl}/gate`;
  private readonly agendamentosUrl = `${this.baseUrl}/agendamentos`;
  private readonly janelasUrl = `${this.baseUrl}/janelas`;
  private readonly configUrl = `${this.baseUrl}/config`;

  constructor(private readonly http: HttpClient) {}

  listarAgendamentos(filtro?: AgendamentoFiltro): Observable<Page<Agendamento>> {
    const params = this.buildParams(filtro);
    return this.http.get<Page<Agendamento>>(this.agendamentosUrl, { params });
  }

  obterAgendamentoPorId(id: number): Observable<Agendamento> {
    return this.http.get<Agendamento>(`${this.agendamentosUrl}/${id}`);
  }

  confirmarChegadaAntecipada(id: number): Observable<Agendamento> {
    return this.http.post<Agendamento>(`${this.baseUrl}/agendamentos/${id}/confirmar-chegada`, {});
  }

  revalidarDocumentos(id: number): Observable<Agendamento> {
    return this.http.post<Agendamento>(`${this.agendamentosUrl}/${id}/documentos/revalidar`, {});
  }

  criarAgendamento(request: AgendamentoRequest): Observable<Agendamento> {
    return this.http.post<Agendamento>(this.agendamentosUrl, request);
  }

  atualizarAgendamento(id: number, request: AgendamentoRequest): Observable<Agendamento> {
    return this.http.put<Agendamento>(`${this.agendamentosUrl}/${id}`, request);
  }

  cancelarAgendamento(id: number): Observable<void> {
    return this.http.delete<void>(`${this.agendamentosUrl}/${id}`);
  }

  listarJanelas(filtro?: JanelaFiltro): Observable<Page<JanelaAtendimento>> {
    const params = this.buildParams(filtro);
    return this.http.get<Page<JanelaAtendimento>>(this.janelasUrl, { params });
  }

  obterJanelaPorId(id: number): Observable<JanelaAtendimento> {
    return this.http.get<JanelaAtendimento>(`${this.janelasUrl}/${id}`);
  }

  criarJanela(request: JanelaAtendimentoRequest): Observable<JanelaAtendimento> {
    return this.http.post<JanelaAtendimento>(this.janelasUrl, request);
  }

  atualizarJanela(id: number, request: JanelaAtendimentoRequest): Observable<JanelaAtendimento> {
    return this.http.put<JanelaAtendimento>(`${this.janelasUrl}/${id}`, request);
  }

  removerJanela(id: number): Observable<void> {
    return this.http.delete<void>(`${this.janelasUrl}/${id}`);
  }

  listarTiposOperacao(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.configUrl}/tipos-operacao`);
  }

  listarStatusAgendamento(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.configUrl}/status-agendamento`);
  }

  listarStatusGate(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.configUrl}/status-gate`);
  }

  listarMotivosExcecao(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.configUrl}/motivos-excecao`);
  }

  listarCanaisEntrada(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.configUrl}/canais-entrada`);
  }

  listarTiposOcorrencia(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.configUrl}/tipos-ocorrencia`);
  }

  listarNiveisEvento(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.configUrl}/niveis-evento`);
  }

  uploadDocumentoAgendamento(id: number, arquivo: File): Observable<HttpEvent<DocumentoAgendamento>> {
    const formData = new FormData();
    const metadata = {
      tipoDocumento: this.inferirTipoDocumento(arquivo),
      numero: null
    };
    formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
    formData.append('arquivo', arquivo, arquivo.name);
    return this.http.post<DocumentoAgendamento>(`${this.agendamentosUrl}/${id}/documentos`, formData, {
      reportProgress: true,
      observe: 'events'
    });
  }

  private inferirTipoDocumento(arquivo: File): string {
    if (arquivo.type) {
      return arquivo.type;
    }
    const partesNome = arquivo.name?.split('.') ?? [];
    if (partesNome.length > 1) {
      return partesNome.pop()!.toUpperCase();
    }
    return 'ARQUIVO';
  }

  private buildParams(filters?: Record<string, string | number | boolean | undefined | null>): HttpParams {
    let params = new HttpParams();
    if (!filters) {
      return params;
    }

    Object.entries(filters)
      .filter(([, value]) => value !== undefined && value !== null)
      .forEach(([key, value]) => {
        params = params.set(key, String(value));
      });

    return params;
  }
}
