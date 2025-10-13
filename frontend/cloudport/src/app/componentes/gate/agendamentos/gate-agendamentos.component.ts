import { Component, OnDestroy, OnInit } from '@angular/core';
import { BehaviorSubject, EMPTY, Observable, Subject, Subscription, from, of } from 'rxjs';
import { catchError, concatMap, finalize, map, shareReplay, switchMap, take, takeUntil, tap, toArray } from 'rxjs/operators';
import { GateApiService } from '../../service/servico-gate/gate-api.service';
import {
  Agendamento,
  AgendamentoFormPayload,
  AgendamentoRealtimeConnection,
  AgendamentoRealtimeEvent,
  DocumentoAgendamento,
  DocumentoRevalidacaoResultado,
  UploadDocumentoStatus
} from '../../model/gate/agendamento.model';
import { JanelaAtendimento } from '../../model/gate/janela.model';
import { PopupService } from '../../service/popupService';
import { HttpEventType } from '@angular/common/http';
import { AgendamentoRealtimeService } from '../../service/servico-gate/agendamento-realtime.service';
import { NotificationBridgeService } from '../../service/notification-bridge.service';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-gate-agendamentos',
  templateUrl: './gate-agendamentos.component.html',
  styleUrls: ['./gate-agendamentos.component.css']
})
export class GateAgendamentosComponent implements OnInit, OnDestroy {
  readonly titulo = 'Agendamentos do Gate';

  private readonly destruir$ = new Subject<void>();
  private readonly agendamentosSubject = new BehaviorSubject<Agendamento[]>([]);
  readonly agendamentos$ = this.agendamentosSubject.asObservable();

  readonly tiposOperacao$ = this.gateApi.listarTiposOperacao().pipe(shareReplay(1));
  readonly status$ = this.gateApi.listarStatusAgendamento().pipe(shareReplay(1));
  readonly janelas$: Observable<JanelaAtendimento[]> = this.gateApi
    .listarJanelas({ size: 200 })
    .pipe(map((pagina) => pagina.content), shareReplay(1));

  selecionadoId: number | null = null;
  selecionado: Agendamento | null = null;
  emEdicao: Agendamento | null = null;
  exibirFormulario = false;
  carregandoLista = false;
  carregandoDetalhe = false;
  uploadStatus: UploadDocumentoStatus[] = [];
  documentosExistentes: DocumentoAgendamento[] | null = [];

  realtimeStatus: AgendamentoRealtimeConnection | null = null;

  private realtimeSubscription?: Subscription;
  private realtimeAgendamentoId: number | null = null;

  constructor(
    private readonly gateApi: GateApiService,
    private readonly popupService: PopupService,
    private readonly realtimeService: AgendamentoRealtimeService,
    private readonly notificationBridge: NotificationBridgeService,
    private readonly translate: TranslateService
  ) {}

  ngOnInit(): void {
    this.carregarAgendamentos();
  }

  ngOnDestroy(): void {
    this.destruir$.next();
    this.destruir$.complete();
    this.desconectarRealtime();
    this.realtimeService.limparTudo();
  }

  carregarAgendamentos(): void {
    this.carregandoLista = true;
    this.gateApi
      .listarAgendamentos({ size: 100 })
      .pipe(
        finalize(() => (this.carregandoLista = false)),
        takeUntil(this.destruir$)
      )
      .subscribe((pagina) => {
        this.agendamentosSubject.next(pagina.content);
        if (this.selecionadoId) {
          const existe = pagina.content.some((item) => item.id === this.selecionadoId);
          if (!existe) {
            this.selecionadoId = null;
            this.selecionado = null;
          }
        }
      });
  }

  selecionarAgendamento(id: number): void {
    this.selecionadoId = id;
    this.carregandoDetalhe = true;
    this.gateApi
      .obterAgendamentoPorId(id)
      .pipe(
        finalize(() => (this.carregandoDetalhe = false)),
        takeUntil(this.destruir$)
      )
      .subscribe((agendamento) => {
        this.selecionado = agendamento;
        this.documentosExistentes = agendamento.documentos ?? [];
        if (!this.exibirFormulario) {
          this.emEdicao = null;
        }
        this.conectarRealtime(id);
      });
  }

  criarAgendamento(): void {
    this.emEdicao = null;
    this.documentosExistentes = [];
    this.exibirFormulario = true;
  }

  editarAgendamento(id: number): void {
    this.popupService
      .openConfirmacao({
        titulo: 'Editar agendamento',
        mensagem: 'Deseja editar o agendamento selecionado?',
        textoConfirmar: 'Editar'
      })
      .pipe(take(1))
      .subscribe((confirmado) => {
        if (confirmado) {
          this.iniciarEdicao(id);
        }
      });
  }

  cancelarAgendamento(id: number): void {
    this.popupService
      .openConfirmacao({
        titulo: 'Cancelar agendamento',
        mensagem: 'Confirma o cancelamento deste agendamento?',
        textoConfirmar: 'Cancelar',
        textoCancelar: 'Manter'
      })
      .pipe(take(1), switchMap((confirmado) => (confirmado ? this.gateApi.cancelarAgendamento(id) : EMPTY)))
      .subscribe({
        next: () => {
          if (this.selecionadoId === id) {
            this.selecionadoId = null;
            this.selecionado = null;
          }
          this.carregarAgendamentos();
        }
      });
  }

  salvar(payload: AgendamentoFormPayload): void {
    const acao$ = this.emEdicao
      ? this.gateApi.atualizarAgendamento(this.emEdicao.id, payload.request)
      : this.gateApi.criarAgendamento(payload.request);

    acao$
      .pipe(
        switchMap((agendamento) =>
          this.processarUploads(agendamento.id, payload.arquivos).pipe(map(() => agendamento))
        ),
        finalize(() => (this.uploadStatus = [])),
        takeUntil(this.destruir$)
      )
      .subscribe((agendamento) => {
        this.exibirFormulario = false;
        this.emEdicao = null;
        this.documentosExistentes = agendamento.documentos ?? [];
        this.carregarAgendamentos();
        this.selecionarAgendamento(agendamento.id);
      });
  }

  cancelarFormulario(): void {
    this.exibirFormulario = false;
    this.emEdicao = null;
    this.documentosExistentes = this.selecionado?.documentos ?? [];
  }

  atualizarLista(): void {
    this.carregarAgendamentos();
  }

  anexarDocumentos(files: File[]): void {
    if (!this.selecionadoId || !files.length) {
      return;
    }
    this.processarUploads(this.selecionadoId, files)
      .pipe(takeUntil(this.destruir$))
      .subscribe(() => {
        if (this.selecionadoId) {
          this.selecionarAgendamento(this.selecionadoId);
        }
        this.carregarAgendamentos();
      });
  }

  aoAtualizarAgendamento(agendamento: Agendamento): void {
    this.selecionado = agendamento;
    this.documentosExistentes = agendamento.documentos ?? [];
    this.carregarAgendamentos();
  }

  aoDocumentosRevalidados(resultados: DocumentoRevalidacaoResultado[]): void {
    if (resultados.length) {
      const validos = resultados.filter((resultado) => resultado.valido).length;
      const mensagem = `${validos}/${resultados.length} ${this.selecionado?.codigo}`;
      this.notificationBridge.notify(this.translate.instant('gate.detail.actions.revalidateSuccess'), { body: mensagem });
    }
  }

  private conectarRealtime(id: number): void {
    if (this.realtimeAgendamentoId === id) {
      return;
    }
    this.desconectarRealtime();
    this.realtimeAgendamentoId = id;
    this.realtimeStatus = null;
    this.realtimeSubscription = this.realtimeService.conectar(id).subscribe((evento) =>
      this.processarEventoRealtime(evento)
    );
  }

  private desconectarRealtime(): void {
    if (this.realtimeAgendamentoId != null) {
      this.realtimeService.desconectar(this.realtimeAgendamentoId);
      this.realtimeAgendamentoId = null;
    }
    this.realtimeSubscription?.unsubscribe();
    this.realtimeSubscription = undefined;
    this.realtimeStatus = null;
  }

  private processarEventoRealtime(evento: AgendamentoRealtimeEvent): void {
    if (!this.selecionado || this.selecionado.id !== this.realtimeAgendamentoId) {
      return;
    }
    switch (evento.type) {
      case 'connection':
        this.realtimeStatus = evento.payload;
        if (evento.payload.state === 'reconnecting') {
          this.notificationBridge.notify(
            this.translate.instant('gate.detail.realtime.reconnecting', {
              seconds: evento.payload.delaySeconds ?? 0,
              attempt: evento.payload.attempt ?? 0
            }),
            { body: this.selecionado.codigo }
          );
        }
        if (evento.payload.state === 'connected' && evento.payload.attempt === 0) {
          this.notificationBridge.notify(this.translate.instant('gate.detail.realtime.connected'), {
            body: this.selecionado.codigo
          });
        }
        if (evento.payload.state === 'disconnected') {
          this.notificationBridge.notify(this.translate.instant('gate.detail.realtime.disconnected'), {
            body: this.selecionado.codigo
          });
        }
        break;
      case 'status':
        this.selecionado = {
          ...this.selecionado,
          status: evento.payload.status,
          statusDescricao: evento.payload.statusDescricao,
          horarioRealChegada: evento.payload.horarioRealChegada,
          horarioRealSaida: evento.payload.horarioRealSaida,
          observacoes: evento.payload.observacao
        };
        this.carregarAgendamentos();
        break;
      case 'snapshot':
        this.selecionado = evento.payload;
        this.documentosExistentes = evento.payload.documentos ?? [];
        this.carregarAgendamentos();
        break;
      case 'documentos-revalidados':
        this.notificationBridge.notify(
          this.translate.instant('gate.detail.actions.revalidateSuccess'),
          { body: `${evento.payload.length} ${this.translate.instant('gate.detail.instructions.header')}` }
        );
        break;
      case 'window-reminder':
        this.notificationBridge.notify(
          this.translate.instant('gate.detail.status.windowReminder', { minutes: evento.payload.minutosRestantes }),
          { body: evento.payload.codigoAgendamento }
        );
        break;
    }
  }

  private iniciarEdicao(id: number): void {
    this.gateApi
      .obterAgendamentoPorId(id)
      .pipe(take(1))
      .subscribe((agendamento) => {
        this.emEdicao = agendamento;
        this.documentosExistentes = agendamento.documentos ?? [];
        this.exibirFormulario = true;
      });
  }

  private processarUploads(id: number, arquivos: File[]): Observable<DocumentoAgendamento[]> {
    if (!arquivos.length) {
      return of([]);
    }

    this.uploadStatus = arquivos.map((arquivo) => ({ fileName: arquivo.name, progress: 0, status: 'pendente' }));

    return from(arquivos).pipe(
      concatMap((arquivo) =>
        this.gateApi.uploadDocumentoAgendamento(id, arquivo).pipe(
          tap((evento) => this.atualizarStatusUpload(arquivo.name, evento)),
          catchError(() => {
            this.atualizarStatusUpload(arquivo.name, null, true);
            return EMPTY;
          }),
          switchMap((evento) =>
            evento.type === HttpEventType.Response ? of(evento.body as DocumentoAgendamento) : EMPTY
          )
        )
      ),
      toArray(),
      finalize(() => {
        setTimeout(() => (this.uploadStatus = []), 800);
      })
    );
  }

  private atualizarStatusUpload(nome: string, evento: any, erro = false): void {
    this.uploadStatus = this.uploadStatus.map((status) => {
      if (status.fileName !== nome) {
        return status;
      }

      if (erro) {
        return { ...status, status: 'erro' };
      }

      if (!evento) {
        return status;
      }

      if (evento.type === HttpEventType.Sent) {
        return { ...status, status: 'enviando', progress: 0 };
      }

      if (evento.type === HttpEventType.UploadProgress) {
        const total = evento.total ?? evento.loaded ?? 1;
        const progresso = total ? Math.round((evento.loaded * 100) / total) : status.progress;
        return { ...status, status: 'enviando', progress: progresso };
      }

      if (evento.type === HttpEventType.Response) {
        return { ...status, status: 'concluido', progress: 100 };
      }

      return status;
    });
  }
}
