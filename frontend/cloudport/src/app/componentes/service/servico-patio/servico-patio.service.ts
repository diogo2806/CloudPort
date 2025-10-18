import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';
import { ClienteStompBasico, ManipuladorErro, ManipuladorMensagem } from './cliente-stomp-basico';

export interface ConteinerMapa {
  id?: number;
  codigo: string;
  linha: number;
  coluna: number;
  status: string;
  tipoCarga: string;
  destino: string;
  camadaOperacional: string;
}

export interface EquipamentoMapa {
  id?: number;
  identificador: string;
  tipoEquipamento: string;
  linha: number;
  coluna: number;
  statusOperacional: string;
}

export interface PosicaoPatio {
  id: number;
  linha: number;
  coluna: number;
  camadaOperacional: string;
  ocupada: boolean;
  codigoConteiner?: string;
  statusConteiner?: string;
}

export interface MovimentoPatio {
  id: number;
  codigoConteiner?: string;
  tipoMovimento: string;
  descricao: string;
  destino?: string;
  linha?: number;
  coluna?: number;
  camadaOperacional?: string;
  registradoEm: string;
}

export interface OpcoesCadastroPatio {
  statusConteiner: string[];
  tiposEquipamento: string[];
  statusEquipamento: string[];
}

export interface MapaPatioResposta {
  conteineres: ConteinerMapa[];
  equipamentos: EquipamentoMapa[];
  totalLinhas: number;
  totalColunas: number;
  atualizadoEm: string;
}

export interface FiltrosMapaPatio {
  statusDisponiveis: string[];
  tiposCargaDisponiveis: string[];
  destinosDisponiveis: string[];
  camadasOperacionaisDisponiveis: string[];
  tiposEquipamentoDisponiveis: string[];
}

export interface FiltroConsultaMapa {
  status?: string[];
  tiposCarga?: string[];
  destinos?: string[];
  camadas?: string[];
  tiposEquipamento?: string[];
}

export interface EventoTempoRealMapa {
  tipoEvento: string;
  mapa: MapaPatioResposta;
}

@Injectable({
  providedIn: 'root'
})
export class ServicoPatioService implements OnDestroy {
  private clienteStomp?: ClienteStompBasico;
  private sujeitoAtualizacoes = new Subject<EventoTempoRealMapa>();
  private conectado = false;

  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  obterMapa(filtro: FiltroConsultaMapa): Observable<MapaPatioResposta> {
    const params = this.construirParametros(filtro);
    return this.http.get<MapaPatioResposta>(this.construirUrl('/mapa'), { params });
  }

  obterFiltros(): Observable<FiltrosMapaPatio> {
    return this.http.get<FiltrosMapaPatio>(this.construirUrl('/filtros'));
  }

  listarPosicoes(): Observable<PosicaoPatio[]> {
    return this.http.get<PosicaoPatio[]>(this.construirUrl('/posicoes'));
  }

  listarConteineres(): Observable<ConteinerMapa[]> {
    return this.http.get<ConteinerMapa[]>(this.construirUrl('/conteineres'));
  }

  listarMovimentacoes(): Observable<MovimentoPatio[]> {
    return this.http.get<MovimentoPatio[]>(this.construirUrl('/movimentacoes'));
  }

  obterOpcoesCadastro(): Observable<OpcoesCadastroPatio> {
    return this.http.get<OpcoesCadastroPatio>(this.construirUrl('/opcoes'));
  }

  salvarConteiner(payload: ConteinerMapa): Observable<ConteinerMapa> {
    return this.http.post<ConteinerMapa>(this.construirUrl('/conteineres'), payload);
  }

  salvarEquipamento(payload: EquipamentoMapa): Observable<EquipamentoMapa> {
    return this.http.post<EquipamentoMapa>(this.construirUrl('/equipamentos'), payload);
  }

  iniciarMonitoramentoTempoReal(): Observable<EventoTempoRealMapa> {
    if (!this.conectado) {
      this.conectarTempoReal();
    }
    return this.sujeitoAtualizacoes.asObservable();
  }

  ngOnDestroy(): void {
    this.clienteStomp?.desconectar();
    this.sujeitoAtualizacoes.complete();
  }

  private conectarTempoReal(): void {
    this.clienteStomp = new ClienteStompBasico(this.obterUrlWebsocket());
    const manipuladorMensagem: ManipuladorMensagem = (corpo) => {
      try {
        const evento: EventoTempoRealMapa = JSON.parse(corpo);
        this.sujeitoAtualizacoes.next(evento);
      } catch (erro) {
        console.error('Falha ao interpretar mensagem do mapa do pátio', erro);
      }
    };
    const manipuladorErro: ManipuladorErro = (erro) => {
      console.error('Erro no canal de tempo real do pátio', erro);
      this.conectado = false;
    };

    this.clienteStomp.conectar('/topico/patio', manipuladorMensagem, manipuladorErro);
    this.conectado = true;
  }

  private construirUrl(caminho: string): string {
    return this.configuracaoAplicacao.construirUrlApi(`/yard/patio${caminho}`);
  }

  private obterUrlWebsocket(): string {
    return this.configuracaoAplicacao.construirUrlWebsocket('/ws/patio');
  }

  private construirParametros(filtro: FiltroConsultaMapa): HttpParams {
    let params = new HttpParams();

    if (filtro.status) {
      filtro.status.filter((valor) => valor).forEach((valor) => {
        params = params.append('status', valor);
      });
    }
    if (filtro.tiposCarga) {
      filtro.tiposCarga.filter((valor) => valor).forEach((valor) => {
        params = params.append('tipoCarga', valor);
      });
    }
    if (filtro.destinos) {
      filtro.destinos.filter((valor) => valor).forEach((valor) => {
        params = params.append('destino', valor);
      });
    }
    if (filtro.camadas) {
      filtro.camadas.filter((valor) => valor).forEach((valor) => {
        params = params.append('camada', valor);
      });
    }
    if (filtro.tiposEquipamento) {
      filtro.tiposEquipamento.filter((valor) => valor).forEach((valor) => {
        params = params.append('tipoEquipamento', valor);
      });
    }

    return params;
  }
}
