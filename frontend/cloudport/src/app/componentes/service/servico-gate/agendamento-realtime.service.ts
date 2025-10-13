import { Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type AgendamentoRealtimeEventType =
  | 'snapshot'
  | 'status-atualizado'
  | 'janela-proxima'
  | 'documentos-atualizados'
  | 'documentos-revalidados'
  | 'gate-pass-atualizado'
  | 'conexao-estabelecida'
  | 'conexao-perdida'
  | 'reconectando';

export interface AgendamentoRealtimeEvent<T = unknown> {
  type: AgendamentoRealtimeEventType;
  data: T;
}

interface ReconexaoPayload {
  tentativa: number;
  delayMs: number;
}

@Injectable({
  providedIn: 'root'
})
export class AgendamentoRealtimeService {
  private readonly sources = new Map<number, EventSource>();
  private readonly retryTimers = new Map<number, number>();
  private readonly retryAttempts = new Map<number, number>();

  constructor(private readonly zone: NgZone) {}

  conectar(agendamentoId: number): Observable<AgendamentoRealtimeEvent> {
    return new Observable<AgendamentoRealtimeEvent>((subscriber) => {
      let disposed = false;

      const limparTimer = (): void => {
        const timer = this.retryTimers.get(agendamentoId);
        if (timer !== undefined) {
          clearTimeout(timer);
          this.retryTimers.delete(agendamentoId);
        }
      };

      const fecharFonte = (): void => {
        const source = this.sources.get(agendamentoId);
        if (source) {
          source.close();
          this.sources.delete(agendamentoId);
        }
      };

      const agendarReconexao = (): void => {
        if (disposed) {
          return;
        }
        const tentativaAtual = (this.retryAttempts.get(agendamentoId) ?? 0) + 1;
        this.retryAttempts.set(agendamentoId, tentativaAtual);
        const delayMs = Math.min(30000, Math.pow(2, tentativaAtual) * 1000);
        const payload: ReconexaoPayload = { tentativa: tentativaAtual, delayMs };
        this.zone.run(() => subscriber.next({ type: 'reconectando', data: payload }));
        const timer = window.setTimeout(() => {
          this.retryTimers.delete(agendamentoId);
          iniciar();
        }, delayMs);
        this.retryTimers.set(agendamentoId, timer);
      };

      const iniciar = (): void => {
        if (disposed) {
          return;
        }
        limparTimer();
        fecharFonte();

        const url = `${environment.baseApiUrl}/gate/agendamentos/${agendamentoId}/stream`;
        const eventSource = new EventSource(url, { withCredentials: true });
        this.sources.set(agendamentoId, eventSource);

        const eventos: AgendamentoRealtimeEventType[] = [
          'snapshot',
          'status-atualizado',
          'janela-proxima',
          'documentos-atualizados',
          'documentos-revalidados',
          'gate-pass-atualizado'
        ];

        eventSource.onopen = () => {
          this.retryAttempts.set(agendamentoId, 0);
          this.zone.run(() => subscriber.next({ type: 'conexao-estabelecida', data: null }));
        };

        eventos.forEach((tipo) => {
          eventSource.addEventListener(tipo, (event: MessageEvent) => {
            this.zone.run(() => {
              try {
                const data = event.data ? JSON.parse(event.data) : null;
                subscriber.next({ type: tipo, data });
              } catch (error) {
                subscriber.error(error);
              }
            });
          });
        });

        eventSource.onerror = () => {
          if (disposed) {
            return;
          }
          this.zone.run(() => subscriber.next({ type: 'conexao-perdida', data: null }));
          fecharFonte();
          agendarReconexao();
        };
      };

      iniciar();

      return () => {
        disposed = true;
        limparTimer();
        fecharFonte();
        this.retryAttempts.delete(agendamentoId);
      };
    });
  }

  desconectar(agendamentoId: number): void {
    const source = this.sources.get(agendamentoId);
    if (source) {
      source.close();
      this.sources.delete(agendamentoId);
    }
    const timer = this.retryTimers.get(agendamentoId);
    if (timer !== undefined) {
      clearTimeout(timer);
      this.retryTimers.delete(agendamentoId);
    }
    this.retryAttempts.delete(agendamentoId);
  }
}
