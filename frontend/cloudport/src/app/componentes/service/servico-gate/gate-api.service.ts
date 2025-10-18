import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';
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
  private readonly agendamentosSegmento = '/agendamentos';
  private readonly janelasSegmento = '/janelas';
  private readonly configSegmento = '/config';

  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  private get baseUrl(): string {
    return this.configuracaoAplicacao.construirUrlApi('/gate');
  }

  listarAgendamentos(filtro?: AgendamentoFiltro): Observable<Page<Agendamento>> {
    const params = this.buildParams(filtro);
    return this.http.get<Page<Agendamento>>(this.construirUrlAgendamentos(), { params });
  }

  obterAgendamentoPorId(id: number): Observable<Agendamento> {
    return this.http.get<Agendamento>(`${this.construirUrlAgendamentos()}/${id}`);
  }

  confirmarChegadaAntecipada(id: number): Observable<Agendamento> {
    return this.http.post<Agendamento>(`${this.baseUrl}/agendamentos/${id}/confirmar-chegada`, {});
  }

  revalidarDocumentos(id: number): Observable<Agendamento> {
    return this.http.post<Agendamento>(`${this.construirUrlAgendamentos()}/${id}/documentos/revalidar`, {});
  }

  criarAgendamento(request: AgendamentoRequest): Observable<Agendamento> {
    return this.http.post<Agendamento>(this.construirUrlAgendamentos(), request);
  }

  atualizarAgendamento(id: number, request: AgendamentoRequest): Observable<Agendamento> {
    return this.http.put<Agendamento>(`${this.construirUrlAgendamentos()}/${id}`, request);
  }

  cancelarAgendamento(id: number): Observable<void> {
    return this.http.delete<void>(`${this.construirUrlAgendamentos()}/${id}`);
  }

  listarJanelas(filtro?: JanelaFiltro): Observable<Page<JanelaAtendimento>> {
    const params = this.buildParams(filtro);
    return this.http.get<Page<JanelaAtendimento>>(this.construirUrlJanelas(), { params });
  }

  obterJanelaPorId(id: number): Observable<JanelaAtendimento> {
    return this.http.get<JanelaAtendimento>(`${this.construirUrlJanelas()}/${id}`);
  }

  criarJanela(request: JanelaAtendimentoRequest): Observable<JanelaAtendimento> {
    return this.http.post<JanelaAtendimento>(this.construirUrlJanelas(), request);
  }

  atualizarJanela(id: number, request: JanelaAtendimentoRequest): Observable<JanelaAtendimento> {
    return this.http.put<JanelaAtendimento>(`${this.construirUrlJanelas()}/${id}`, request);
  }

  removerJanela(id: number): Observable<void> {
    return this.http.delete<void>(`${this.construirUrlJanelas()}/${id}`);
  }

  listarTiposOperacao(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.construirUrlConfig()}/tipos-operacao`);
  }

  listarStatusAgendamento(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.construirUrlConfig()}/status-agendamento`);
  }

  listarStatusGate(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.construirUrlConfig()}/status-gate`);
  }

  listarMotivosExcecao(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.construirUrlConfig()}/motivos-excecao`);
  }

  listarCanaisEntrada(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.construirUrlConfig()}/canais-entrada`);
  }

  listarTiposOcorrencia(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.construirUrlConfig()}/tipos-ocorrencia`);
  }

  listarNiveisEvento(): Observable<GateEnumOption[]> {
    return this.http.get<GateEnumOption[]>(`${this.construirUrlConfig()}/niveis-evento`);
  }

  uploadDocumentoAgendamento(id: number, arquivo: File): Observable<HttpEvent<DocumentoAgendamento>> {
    const formData = new FormData();
    const metadata = {
      tipoDocumento: this.inferirTipoDocumento(arquivo),
      numero: null
    };
    formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
    formData.append('arquivo', arquivo, arquivo.name);
    return this.http.post<DocumentoAgendamento>(`${this.construirUrlAgendamentos()}/${id}/documentos`, formData, {
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

  private construirUrlAgendamentos(): string {
    return `${this.baseUrl}${this.agendamentosSegmento}`;
  }

  private construirUrlJanelas(): string {
    return `${this.baseUrl}${this.janelasSegmento}`;
  }

  private construirUrlConfig(): string {
    return `${this.baseUrl}${this.configSegmento}`;
  }
}
