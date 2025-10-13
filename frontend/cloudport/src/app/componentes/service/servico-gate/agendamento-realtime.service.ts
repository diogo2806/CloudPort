import { Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type AgendamentoRealtimeEventType =
  | 'snapshot'
  | 'status-atualizado'
  | 'janela-proxima'
  | 'documentos-atualizados'
  | 'documentos-revalidados'
  | 'gate-pass-atualizado';

export interface AgendamentoRealtimeEvent<T = unknown> {
  type: AgendamentoRealtimeEventType;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class AgendamentoRealtimeService {
  private readonly sources = new Map<number, EventSource>();

  constructor(private readonly zone: NgZone) {}

  conectar(agendamentoId: number): Observable<AgendamentoRealtimeEvent> {
    return new Observable<AgendamentoRealtimeEvent>((subscriber) => {
      if (this.sources.has(agendamentoId)) {
        this.sources.get(agendamentoId)?.close();
      }
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

      eventSource.onerror = (error) => {
        this.zone.run(() => subscriber.error(error));
      };

      return () => {
        eventSource.close();
        this.sources.delete(agendamentoId);
      };
    });
  }

  desconectar(agendamentoId: number): void {
    const source = this.sources.get(agendamentoId);
    if (source) {
      source.close();
      this.sources.delete(agendamentoId);
    }
  }
}
