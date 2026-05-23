import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export type StatusBerco = 'DISPONIVEL' | 'OCUPADO' | 'INATIVO';
export type StatusVisitaNavio =
  | 'PLANEJADA'
  | 'PROGRAMADA'
  | 'CHEGADA_CONFIRMADA'
  | 'ATRACADA'
  | 'EM_OPERACAO'
  | 'OPERACAO_CONCLUIDA'
  | 'DESATRACADA'
  | 'CANCELADA';
export type TipoOperacaoNavioConteiner = 'EMBARQUE' | 'DESCARGA';
export type StatusOperacaoNavioConteiner = 'PLANEJADA' | 'EM_ANDAMENTO' | 'CONCLUIDA';

export interface NavioResumo {
  identificador: number;
  nome: string;
  codigoImo: string;
  statusOperacao: string;
  dataPrevistaAtracacao: string;
  bercoPrevisto?: string | null;
}

export interface Berco {
  identificador: number;
  nome: string;
  comprimentoMetros: number;
  caladoMaximoMetros: number;
  status: StatusBerco;
}

export interface BercoRequest {
  nome: string;
  comprimentoMetros: number;
  caladoMaximoMetros: number;
  status?: StatusBerco;
}

export interface OperacaoNavioConteiner {
  identificador: number;
  tipoOperacao: TipoOperacaoNavioConteiner;
  identificacaoConteiner: string;
  bay?: number | null;
  fileira?: number | null;
  altura?: number | null;
  pesoToneladas?: number | null;
  status: StatusOperacaoNavioConteiner;
}

export interface OperacaoRequest {
  tipoOperacao: TipoOperacaoNavioConteiner;
  identificacaoConteiner: string;
  bay?: number | null;
  fileira?: number | null;
  altura?: number | null;
  pesoToneladas?: number | null;
}

export interface PortoRotacao {
  identificador?: number;
  sequencia: number;
  portoUnloc: string;
  nomePorto?: string | null;
}

export interface ServicoLinha {
  identificador: number;
  codigo: string;
  nome: string;
  armador?: string | null;
  rotacao: PortoRotacao[];
}

export interface ServicoLinhaRequest {
  codigo: string;
  nome: string;
  armador?: string;
  rotacao: PortoRotacao[];
}

export interface VisitaNavioResumo {
  identificador: number;
  navioNome: string;
  codigoImo: string;
  numeroViagem: string;
  bercoNome?: string | null;
  atracacaoPrevista: string;
  desatracacaoPrevista: string;
  status: StatusVisitaNavio;
  servicoCodigo?: string | null;
}

export interface VisitaNavioDetalhe {
  identificador: number;
  navioId: number;
  navioNome: string;
  codigoImo: string;
  numeroViagem: string;
  bercoId?: number | null;
  bercoNome?: string | null;
  atracacaoPrevista: string;
  atracacaoEfetiva?: string | null;
  desatracacaoPrevista: string;
  desatracacaoEfetiva?: string | null;
  status: StatusVisitaNavio;
  observacoes?: string | null;
  servicoId?: number | null;
  servicoCodigo?: string | null;
  chegadaPrevista?: string | null;
  chegadaEfetiva?: string | null;
  operacoes: OperacaoNavioConteiner[];
}

export interface CadastroVisitaNavio {
  navioId: number;
  numeroViagem: string;
  atracacaoPrevista: string;
  desatracacaoPrevista: string;
  observacoes?: string;
  servicoId?: number | null;
  chegadaPrevista?: string | null;
}

export interface PlanejamentoAtracacao {
  bercoId: number;
  atracacaoPrevista: string;
  desatracacaoPrevista: string;
}

@Injectable({
  providedIn: 'root'
})
export class ServicoNavioService {
  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  listarNavios(): Observable<NavioResumo[]> {
    return this.http.get<NavioResumo[]>(this.url('/navios'));
  }

  listarBercos(): Observable<Berco[]> {
    return this.http.get<Berco[]>(this.url('/bercos'));
  }

  criarBerco(payload: BercoRequest): Observable<Berco> {
    return this.http.post<Berco>(this.url('/bercos'), payload);
  }

  atualizarBerco(id: number, payload: BercoRequest): Observable<Berco> {
    return this.http.put<Berco>(this.url(`/bercos/${id}`), payload);
  }

  removerBerco(id: number): Observable<void> {
    return this.http.delete<void>(this.url(`/bercos/${id}`));
  }

  listarServicos(): Observable<ServicoLinha[]> {
    return this.http.get<ServicoLinha[]>(this.url('/servicos-linha'));
  }

  criarServico(payload: ServicoLinhaRequest): Observable<ServicoLinha> {
    return this.http.post<ServicoLinha>(this.url('/servicos-linha'), payload);
  }

  atualizarServico(id: number, payload: ServicoLinhaRequest): Observable<ServicoLinha> {
    return this.http.put<ServicoLinha>(this.url(`/servicos-linha/${id}`), payload);
  }

  removerServico(id: number): Observable<void> {
    return this.http.delete<void>(this.url(`/servicos-linha/${id}`));
  }

  listarVisitas(): Observable<VisitaNavioResumo[]> {
    return this.http.get<VisitaNavioResumo[]>(this.url('/visitas'));
  }

  listarAgendaAtracacao(): Observable<VisitaNavioResumo[]> {
    return this.http.get<VisitaNavioResumo[]>(this.url('/visitas/agenda-atracacao'));
  }

  obterVisita(id: number): Observable<VisitaNavioDetalhe> {
    return this.http.get<VisitaNavioDetalhe>(this.url(`/visitas/${id}`));
  }

  criarVisita(payload: CadastroVisitaNavio): Observable<VisitaNavioDetalhe> {
    return this.http.post<VisitaNavioDetalhe>(this.url('/visitas'), payload);
  }

  planejarAtracacao(id: number, payload: PlanejamentoAtracacao): Observable<VisitaNavioDetalhe> {
    return this.http.put<VisitaNavioDetalhe>(this.url(`/visitas/${id}/atracacao`), payload);
  }

  registrarChegada(id: number): Observable<VisitaNavioDetalhe> {
    return this.http.post<VisitaNavioDetalhe>(this.url(`/visitas/${id}/chegada`), {});
  }

  atracar(id: number): Observable<VisitaNavioDetalhe> {
    return this.http.post<VisitaNavioDetalhe>(this.url(`/visitas/${id}/atracar`), {});
  }

  iniciarOperacao(id: number): Observable<VisitaNavioDetalhe> {
    return this.http.post<VisitaNavioDetalhe>(this.url(`/visitas/${id}/iniciar-operacao`), {});
  }

  concluirOperacao(id: number): Observable<VisitaNavioDetalhe> {
    return this.http.post<VisitaNavioDetalhe>(this.url(`/visitas/${id}/concluir-operacao`), {});
  }

  desatracar(id: number): Observable<VisitaNavioDetalhe> {
    return this.http.post<VisitaNavioDetalhe>(this.url(`/visitas/${id}/desatracar`), {});
  }

  cancelar(id: number): Observable<VisitaNavioDetalhe> {
    return this.http.post<VisitaNavioDetalhe>(this.url(`/visitas/${id}/cancelar`), {});
  }

  adicionarOperacao(id: number, payload: OperacaoRequest): Observable<OperacaoNavioConteiner> {
    return this.http.post<OperacaoNavioConteiner>(this.url(`/visitas/${id}/operacoes`), payload);
  }

  atualizarStatusOperacao(
    id: number,
    operacaoId: number,
    status: StatusOperacaoNavioConteiner
  ): Observable<OperacaoNavioConteiner> {
    return this.http.patch<OperacaoNavioConteiner>(
      this.url(`/visitas/${id}/operacoes/${operacaoId}/status`),
      { status }
    );
  }

  removerOperacao(id: number, operacaoId: number): Observable<void> {
    return this.http.delete<void>(this.url(`/visitas/${id}/operacoes/${operacaoId}`));
  }

  private url(caminho: string): string {
    return this.configuracaoAplicacao.construirUrlApi(caminho);
  }
}
