import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../service/endpoint';
import { ClienteStompBasico, ManipuladorErro, ManipuladorMensagem } from './cliente-stomp-basico';

export interface ConteinerMapa {
  id: number;
  codigo: string;
  linha: number;
  coluna: number;
  status: string;
  tipoCarga: string;
  destino: string;
  camadaOperacional: string;
}

export interface EquipamentoMapa {
  id: number;
  identificador: string;
  tipoEquipamento: string;
  linha: number;
  coluna: number;
  statusOperacional: string;
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

  constructor(private readonly http: HttpClient) {}

  obterMapa(filtro: FiltroConsultaMapa): Observable<MapaPatioResposta> {
    const params = this.construirParametros(filtro);
    return this.http.get<MapaPatioResposta>(environment.patio.mapa, { params });
  }

  obterFiltros(): Observable<FiltrosMapaPatio> {
    return this.http.get<FiltrosMapaPatio>(environment.patio.filtros);
  }

  salvarConteiner(payload: ConteinerMapa): Observable<ConteinerMapa> {
    return this.http.post<ConteinerMapa>(environment.patio.conteineres, payload);
  }

  salvarEquipamento(payload: EquipamentoMapa): Observable<EquipamentoMapa> {
    return this.http.post<EquipamentoMapa>(environment.patio.equipamentos, payload);
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
    this.clienteStomp = new ClienteStompBasico(environment.patio.websocket);
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
