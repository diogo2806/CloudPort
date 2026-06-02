import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';
import { ClienteStompBasico, ManipuladorErro, ManipuladorMensagem } from '../servico-patio/cliente-stomp-basico';

export interface BercoResumo {
  id?: number;
  codigo: string;
  nome: string;
  comprimentoMetros: number;
  caladoMetros: number;
  guinchesPermanentes: number;
  capacidadeToneladasDia: number;
  voltagem: string;
  aguaPotavel: boolean;
  energiaGenerica: boolean;
  iluminacaoNoturna: boolean;
  sistemaSeguranca: boolean;
  cobertura: boolean;
  zonaPrimaria: string;
  zonaSecundaria?: string | null;
  distanciaZonaMetros: number;
  tempoTransporteMinutos: number;
  diasOperacao: string;
  ultimaManutencao?: string | null;
  proximaManutencao?: string | null;
  status: string;
  observacoes?: string | null;
  scoreAtual: number;
  recomendado: boolean;
  motivoRecomendacao?: string | null;
}

export interface DiaCalendarioBerco {
  data: string;
  status: string;
  rotulo: string;
  navioCodigo?: string | null;
  navioNome?: string | null;
  reservaId?: number | null;
}

export interface CalendarioBerco {
  codigoBerco: string;
  nomeBerco: string;
  dias: DiaCalendarioBerco[];
}

export interface ZonaArmazenagem {
  codigo: string;
  nome: string;
  capacidadeTotal: number;
  ocupacaoAtual: number;
  percentualOcupacao: number;
  bloqueada: boolean;
  atualizadoEm: string;
  observacao?: string | null;
}

export interface EquipamentoBerco {
  identificador: string;
  tipo: string;
  bercoCodigo: string;
  status: string;
  ultimaVerificacao: string;
}

export interface ReservaBerco {
  id: number;
  bercoCodigo: string;
  navioCodigo: string;
  navioNome: string;
  chegadaPrevista: string;
  saidaPrevista: string;
  comprimentoNavio: number;
  caladoNavio: number;
  guinchesRequeridos: number;
  tipoCarga: string;
  zonaArmazenagem: string;
  tipoReserva: string;
  status: string;
  score: number;
  motivo: string;
  criadoEm: string;
}

export interface ResumoRecursos {
  totalBercos: number;
  bercosOperacionais: number;
  bercosEmManutencao: number;
  bercosBloqueados: number;
  reservasConfirmadas: number;
  reservasPropostas: number;
  zonas: ZonaArmazenagem[];
  equipamentos: EquipamentoBerco[];
  alertas: string[];
}

export interface SolicitacaoAlocacao {
  navioCodigo: string;
  navioNome: string;
  chegadaPrevista: string;
  saidaPrevista: string;
  comprimentoNavio: number;
  caladoNavio: number;
  guinchesRequeridos: number;
  tipoCarga: string;
  zonaArmazenagem: string;
  toneladasPrevistas?: number;
  bercoPreferido?: string;
  confirmar: boolean;
}

export interface SolicitacaoManutencao {
  bercoCodigo: string;
  inicio: string;
  fim: string;
  observacao?: string;
}

export interface RespostaAlocacao {
  bercoRecomendado: BercoResumo;
  reservaConfirmada?: ReservaBerco | null;
  ranking: BercoResumo[];
  alertas: string[];
}

export interface EventoRecursosTempoReal {
  tipoEvento: string;
  resumo: ResumoRecursos;
  alocacao?: RespostaAlocacao | null;
}

@Injectable({
  providedIn: 'root'
})
export class ServicoRecursosService implements OnDestroy {
  private clienteStomp?: ClienteStompBasico;
  private sujeitoAtualizacoes = new Subject<EventoRecursosTempoReal>();
  private conectado = false;

  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  listarBercos(): Observable<BercoResumo[]> {
    return this.http.get<BercoResumo[]>(this.construirUrl('/bercos'));
  }

  consultarCalendario(inicio?: string, dias = 14): Observable<CalendarioBerco[]> {
    let params = new HttpParams().set('dias', String(dias));
    if (inicio) {
      params = params.set('inicio', inicio);
    }
    return this.http.get<CalendarioBerco[]>(this.construirUrl('/calendario'), { params });
  }

  consultarResumo(): Observable<ResumoRecursos> {
    return this.http.get<ResumoRecursos>(this.construirUrl('/resumo'));
  }

  listarReservas(): Observable<ReservaBerco[]> {
    return this.http.get<ReservaBerco[]>(this.construirUrl('/reservas'));
  }

  listarEquipamentos(): Observable<EquipamentoBerco[]> {
    return this.http.get<EquipamentoBerco[]>(this.construirUrl('/equipamentos'));
  }

  recomendarOuConfirmarAlocacao(payload: SolicitacaoAlocacao): Observable<RespostaAlocacao> {
    return this.http.post<RespostaAlocacao>(this.construirUrl('/alocacoes'), payload);
  }

  agendarManutencao(payload: SolicitacaoManutencao): Observable<ReservaBerco> {
    return this.http.post<ReservaBerco>(this.construirUrl('/manutencoes'), payload);
  }

  iniciarMonitoramentoTempoReal(): Observable<EventoRecursosTempoReal> {
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
    this.clienteStomp = new ClienteStompBasico(this.construirUrlWebsocket());
    const manipuladorMensagem: ManipuladorMensagem = (corpo) => {
      try {
        const evento: EventoRecursosTempoReal = JSON.parse(corpo);
        this.sujeitoAtualizacoes.next(evento);
      } catch (erro) {
        console.error('Falha ao interpretar mensagem de recursos', erro);
      }
    };
    const manipuladorErro: ManipuladorErro = (erro) => {
      console.error('Erro no canal de tempo real de recursos', erro);
      this.conectado = false;
    };

    this.clienteStomp.conectar('/topico/recursos', manipuladorMensagem, manipuladorErro);
    this.conectado = true;
  }

  private construirUrl(caminho: string): string {
    return this.configuracaoAplicacao.construirUrlApi(`/yard/recursos${caminho}`);
  }

  private construirUrlWebsocket(): string {
    return this.configuracaoAplicacao.construirUrlWebsocket('/ws/recursos');
  }
}
