import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subject, map, tap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { GateApiService } from './gate-api.service';
import {
  GateBloqueioRequest,
  GateLiberacaoManualRequest,
  GateNivelEvento,
  GateOcorrenciaRequest,
  GateOperadorEvento,
  GateOperadorPainel
} from '../../model/gate/operador.model';
import { GateEnumOption } from '../../model/gate/agendamento.model';

@Injectable({
  providedIn: 'root'
})
export class GateOperadorService implements OnDestroy {
  private readonly operadorUrl = `${environment.baseApiUrl}/gate/operador`;
  private readonly painelUrl = `${this.operadorUrl}/painel`;
  private readonly eventosUrl = `${this.operadorUrl}/eventos`;

  private readonly painelSubject = new BehaviorSubject<GateOperadorPainel | null>(null);
  private readonly eventosSubject = new BehaviorSubject<GateOperadorEvento[]>([]);
  private readonly alertasSubject = new Subject<GateOperadorEvento>();
  private readonly conexaoStatusSubject = new BehaviorSubject<'conectado' | 'desconectado' | 'conectando'>('desconectado');

  readonly painel$ = this.painelSubject.asObservable().pipe(map((painel) => painel ?? {
    filasEntrada: [],
    filasSaida: [],
    veiculosAtendimento: [],
    historico: [],
    ultimaAtualizacao: new Date().toISOString()
  }));
  readonly eventos$ = this.eventosSubject.asObservable();
  readonly alertas$ = this.alertasSubject.asObservable();
  readonly statusConexao$ = this.conexaoStatusSubject.asObservable();

  private eventSource: EventSource | null = null;
  private reconexaoTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(private readonly http: HttpClient, private readonly gateApi: GateApiService) {}

  carregarPainel(): Observable<GateOperadorPainel> {
    return this.http.get<GateOperadorPainel>(this.painelUrl).pipe(
      tap((painel) => {
        this.painelSubject.next(painel);
        if (painel.historico) {
          this.eventosSubject.next(painel.historico);
        }
      })
    );
  }

  atualizarHistorico(): Observable<GateOperadorEvento[]> {
    return this.http.get<GateOperadorEvento[]>(this.eventosUrl).pipe(
      tap((eventos) => this.eventosSubject.next(eventos))
    );
  }

  conectarEventos(): void {
    if (this.eventSource || typeof window === 'undefined') {
      return;
    }

    this.conexaoStatusSubject.next('conectando');
    const streamUrl = `${this.eventosUrl}/stream`;

    try {
      this.eventSource = new EventSource(streamUrl);
      this.eventSource.onopen = () => this.conexaoStatusSubject.next('conectado');
      this.eventSource.onmessage = (event) => {
        try {
          const payload: GateOperadorEvento = JSON.parse(event.data);
          this.alertasSubject.next(payload);
          const historicoAtual = [payload, ...this.eventosSubject.value];
          this.eventosSubject.next(historicoAtual.slice(0, 100));
          this.recarregarPainelComEvento();
          if (this.deveEmitirAlertaSonoro(payload.nivel)) {
            this.emitirAlertaSonoro();
          }
        } catch (erro) {
          console.warn('Não foi possível interpretar o evento recebido do Gate.', erro);
        }
      };
      this.eventSource.onerror = () => {
        this.conexaoStatusSubject.next('desconectado');
        this.desconectarEventos();
        this.agendarReconexao();
      };
    } catch (erro) {
      console.error('Falha ao conectar no canal de eventos do Gate.', erro);
      this.conexaoStatusSubject.next('desconectado');
      this.agendarReconexao();
    }
  }

  desconectarEventos(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    if (this.reconexaoTimer) {
      clearTimeout(this.reconexaoTimer);
      this.reconexaoTimer = null;
    }
    this.conexaoStatusSubject.next('desconectado');
  }

  liberarVeiculo(veiculoId: number, payload: GateLiberacaoManualRequest): Observable<void> {
    return this.http.post<void>(`${this.operadorUrl}/veiculos/${veiculoId}/liberacao`, payload).pipe(
      tap(() => this.recarregarPainelComEvento())
    );
  }

  bloquearVeiculo(veiculoId: number, payload: GateBloqueioRequest): Observable<void> {
    return this.http.post<void>(`${this.operadorUrl}/veiculos/${veiculoId}/bloqueio`, payload).pipe(
      tap(() => this.recarregarPainelComEvento())
    );
  }

  registrarOcorrencia(payload: GateOcorrenciaRequest): Observable<void> {
    return this.http.post<void>(`${this.operadorUrl}/ocorrencias`, payload).pipe(
      tap(() => this.recarregarPainelComEvento())
    );
  }

  imprimirComprovante(veiculoId: number): Observable<void> {
    return this.http.get(`${this.operadorUrl}/veiculos/${veiculoId}/comprovante`, {
      responseType: 'blob'
    }).pipe(
      tap((blob) => {
        if (typeof window === 'undefined') {
          return;
        }
        const fileUrl = window.URL.createObjectURL(blob);
        const printWindow = window.open(fileUrl);
        if (printWindow) {
          printWindow.addEventListener('load', () => {
            printWindow.focus();
            printWindow.print();
          });
        }
        setTimeout(() => {
          window.URL.revokeObjectURL(fileUrl);
          printWindow?.close();
        }, 5000);
      }),
      map(() => void 0)
    );
  }

  listarMotivosExcecao(): Observable<GateEnumOption[]> {
    return this.gateApi.listarMotivosExcecao();
  }

  listarCanaisEntrada(): Observable<GateEnumOption[]> {
    return this.gateApi.listarCanaisEntrada();
  }

  listarTiposOcorrencia(): Observable<GateEnumOption[]> {
    return this.gateApi.listarTiposOcorrencia();
  }

  listarNiveisEvento(): Observable<GateEnumOption[]> {
    return this.gateApi.listarNiveisEvento();
  }

  ngOnDestroy(): void {
    this.desconectarEventos();
    this.painelSubject.complete();
    this.eventosSubject.complete();
    this.alertasSubject.complete();
    this.conexaoStatusSubject.complete();
  }

  private recarregarPainelComEvento(): void {
    this.carregarPainel().subscribe({
      error: (erro) => console.warn('Não foi possível atualizar o painel do Gate.', erro)
    });
  }

  private agendarReconexao(): void {
    if (this.reconexaoTimer) {
      return;
    }
    this.reconexaoTimer = setTimeout(() => {
      this.reconexaoTimer = null;
      this.conectarEventos();
    }, 5000);
  }

  private deveEmitirAlertaSonoro(nivel: GateNivelEvento): boolean {
    const nivelNormalizado = (nivel || '').toUpperCase();
    return nivelNormalizado === 'CRITICA' || nivelNormalizado === 'ALTA' || nivelNormalizado === 'ALERTA';
  }

  private emitirAlertaSonoro(): void {
    if (typeof window === 'undefined' || typeof AudioContext === 'undefined') {
      return;
    }

    try {
      const contexto = new AudioContext();
      const oscilador = contexto.createOscillator();
      const ganho = contexto.createGain();
      oscilador.type = 'sine';
      oscilador.frequency.value = 880;
      ganho.gain.setValueAtTime(0.0001, contexto.currentTime);
      ganho.gain.exponentialRampToValueAtTime(0.4, contexto.currentTime + 0.05);
      ganho.gain.exponentialRampToValueAtTime(0.0001, contexto.currentTime + 0.8);
      oscilador.connect(ganho);
      ganho.connect(contexto.destination);
      oscilador.start();
      oscilador.stop(contexto.currentTime + 0.8);
    } catch (erro) {
      console.warn('Não foi possível reproduzir o alerta sonoro.', erro);
    }
  }
}
