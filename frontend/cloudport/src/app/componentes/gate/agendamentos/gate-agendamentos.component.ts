import { Component, OnDestroy, OnInit } from '@angular/core';
import { BehaviorSubject, EMPTY, Observable, Subject, from, of } from 'rxjs';
import { catchError, concatMap, finalize, map, shareReplay, switchMap, take, takeUntil, tap, toArray } from 'rxjs/operators';
import { GateApiService } from '../../service/servico-gate/gate-api.service';
import { GateDashboardService } from '../../service/servico-gate/gate-dashboard.service';
import { Agendamento, AgendamentoFormPayload, DocumentoAgendamento, UploadDocumentoStatus } from '../../model/gate/agendamento.model';
import { JanelaAtendimento } from '../../model/gate/janela.model';
import { PopupService } from '../../service/popupService';
import { HttpEventType } from '@angular/common/http';
import { DashboardResumo } from '../../model/gate/dashboard.model';

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
  resumoUso: DashboardResumo | null = null;
  carregandoResumo = false;
  erroResumo: string | null = null;

  constructor(
    private readonly gateApi: GateApiService,
    private readonly popupService: PopupService,
    private readonly dashboardService: GateDashboardService
  ) {}

  ngOnInit(): void {
    this.carregarAgendamentos();
    this.carregarResumoUso();
  }

  ngOnDestroy(): void {
    this.destruir$.next();
    this.destruir$.complete();
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
        this.carregarResumoUso();
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

  carregarResumoUso(): void {
    if (this.carregandoResumo) {
      return;
    }

    this.carregandoResumo = true;
    this.erroResumo = null;
    this.dashboardService
      .consultarResumo()
      .pipe(
        finalize(() => (this.carregandoResumo = false)),
        takeUntil(this.destruir$)
      )
      .subscribe({
        next: (resumo) => {
          this.resumoUso = resumo;
        },
        error: () => {
          this.erroResumo = 'Não foi possível carregar as métricas de adoção.';
          this.resumoUso = null;
        }
      });
  }

  obterMensagemVariacaoAbandono(): string | null {
    if (!this.resumoUso) {
      return null;
    }
    const variacao = this.resumoUso.variacaoAbandonoPercentual;
    if (Math.abs(variacao) < 0.01) {
      return 'Taxa de abandono estável em comparação ao período anterior.';
    }
    const valorFormatado = Math.abs(variacao).toLocaleString('pt-BR', {
      maximumFractionDigits: 0,
      minimumFractionDigits: 0
    });
    return variacao >= 0
      ? `Queda de ${valorFormatado}% no abandono do fluxo em comparação ao período anterior.`
      : `Aumento de ${valorFormatado}% no abandono do fluxo em comparação ao período anterior.`;
  }

  variacaoAbandonoEhPositiva(): boolean {
    return (this.resumoUso?.variacaoAbandonoPercentual ?? 0) >= 0;
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
