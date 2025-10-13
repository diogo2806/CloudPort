import { Injectable, NgZone } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { AgendamentoRealtimeEvent } from '../../model/gate/agendamento.model';
import { Observable, Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AgendamentoRealtimeService {
  private readonly emissores = new Map<number, EventSource>();
  private readonly canais = new Map<number, Subject<AgendamentoRealtimeEvent>>();
  private readonly tentativas = new Map<number, number>();
  private readonly timeouts = new Map<number, number>();

  constructor(private readonly zone: NgZone) {}

  conectar(agendamentoId: number): Observable<AgendamentoRealtimeEvent> {
    const existente = this.canais.get(agendamentoId);
    if (existente) {
      return existente.asObservable();
    }

    const canal = new Subject<AgendamentoRealtimeEvent>();
    this.canais.set(agendamentoId, canal);
    this.tentativas.set(agendamentoId, 0);
    this.abrirFonte(agendamentoId, canal);

    return canal.asObservable();
  }

  desconectar(agendamentoId: number): void {
    const emissor = this.emissores.get(agendamentoId);
    if (emissor) {
      emissor.close();
      this.emissores.delete(agendamentoId);
    }

    const timeoutId = this.timeouts.get(agendamentoId);
    if (timeoutId != null && typeof window !== 'undefined') {
      window.clearTimeout(timeoutId);
      this.timeouts.delete(agendamentoId);
    }

    this.tentativas.delete(agendamentoId);

    const canal = this.canais.get(agendamentoId);
    if (canal) {
      canal.complete();
      this.canais.delete(agendamentoId);
    }
  }

  limparTudo(): void {
    Array.from(this.canais.keys()).forEach((id) => this.desconectar(id));
  }

  private abrirFonte(agendamentoId: number, canal: Subject<AgendamentoRealtimeEvent>): void {
    const emissor = new EventSource(`${environment.baseApiUrl}/gate/agendamentos/${agendamentoId}/status-stream`);
    this.emissores.set(agendamentoId, emissor);

    const emitir = (evento: AgendamentoRealtimeEvent): void => {
      this.zone.run(() => canal.next(evento));
    };

    emissor.onopen = () => {
      const timeoutId = this.timeouts.get(agendamentoId);
      if (timeoutId != null && typeof window !== 'undefined') {
        window.clearTimeout(timeoutId);
        this.timeouts.delete(agendamentoId);
      }

      const houveTentativas = (this.tentativas.get(agendamentoId) ?? 0) > 0;
      this.tentativas.set(agendamentoId, 0);
      emitir({
        type: 'connection',
        payload: { state: 'connected', attempt: houveTentativas ? 0 : undefined }
      });
    };

    const registrarEvento = <T>(tipo: AgendamentoRealtimeEvent['type']) => (event: MessageEvent<string>) => {
      try {
        const dados = JSON.parse(event.data) as T;
        emitir({ type: tipo, payload: dados } as AgendamentoRealtimeEvent);
      } catch (error) {
        console.warn('Falha ao interpretar evento SSE', error);
      }
    };

    emissor.addEventListener('status', registrarEvento('status'));
    emissor.addEventListener('window-reminder', registrarEvento('window-reminder'));
    emissor.addEventListener('documentos-revalidados', registrarEvento('documentos-revalidados'));
    emissor.addEventListener('snapshot', registrarEvento('snapshot'));

    emissor.onerror = () => {
      this.emissores.delete(agendamentoId);
      emissor.close();
      emitir({ type: 'connection', payload: { state: 'disconnected' } });
      this.agendarReconexao(agendamentoId, canal);
    };
  }

  private agendarReconexao(agendamentoId: number, canal: Subject<AgendamentoRealtimeEvent>): void {
    if (!this.canais.has(agendamentoId)) {
      return;
    }

    const tentativaAtual = (this.tentativas.get(agendamentoId) ?? 0) + 1;
    this.tentativas.set(agendamentoId, tentativaAtual);

    const delay = Math.min(30000, 1000 * Math.pow(2, tentativaAtual - 1));
    const delaySeconds = Math.max(1, Math.ceil(delay / 1000));

    this.zone.run(() =>
      canal.next({
        type: 'connection',
        payload: { state: 'reconnecting', attempt: tentativaAtual, delayMs: delay, delaySeconds }
      })
    );

    if (typeof window === 'undefined') {
      return;
    }

    this.zone.runOutsideAngular(() => {
      const timeoutId = window.setTimeout(() => {
        this.timeouts.delete(agendamentoId);
        if (!this.canais.has(agendamentoId)) {
          return;
        }
        this.abrirFonte(agendamentoId, canal);
      }, delay);

      this.timeouts.set(agendamentoId, timeoutId);
    });
  }
}
