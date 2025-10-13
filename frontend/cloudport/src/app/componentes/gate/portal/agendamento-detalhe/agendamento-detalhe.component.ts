import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  Output
} from '@angular/core';
import {
  Agendamento,
  DocumentoAgendamento,
  GatePass,
  UploadDocumentoStatus
} from '../../../model/gate/agendamento.model';
import { GateApiService } from '../../../service/servico-gate/gate-api.service';
import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import {
  AgendamentoRealtimeEvent,
  AgendamentoRealtimeService
} from '../../../service/servico-gate/agendamento-realtime.service';
import { PushNotificationService } from '../../../service/servico-gate/push-notification.service';
import { AgendamentoComprovanteService } from '../../../service/servico-gate/agendamento-comprovante.service';
import { TranslateService } from '@ngx-translate/core';

interface DocumentoPreview {
  nome: string;
  tamanho: string;
  tipo: string;
  url?: string;
  arquivo: File;
}

interface JanelaProximaPayload {
  minutosRestantes: number;
}

interface RealtimeReconexaoPayload {
  tentativa: number;
  delayMs: number;
}

@Component({
  selector: 'app-agendamento-detalhe',
  templateUrl: './agendamento-detalhe.component.html',
  styleUrls: ['./agendamento-detalhe.component.css']
})
export class AgendamentoDetalheComponent implements OnDestroy {
  private _agendamento: Agendamento | null = null;
  @Input()
  set agendamento(value: Agendamento | null) {
    if (value?.id !== this._agendamento?.id) {
      this.conectarRealtime(value);
    }
    this._agendamento = value;
    this.limparSelecao();
  }
  get agendamento(): Agendamento | null {
    return this._agendamento;
  }

  @Input() uploadStatus: UploadDocumentoStatus[] = [];
  @Output() anexarDocumentos = new EventEmitter<File[]>();

  documentosSelecionados: DocumentoPreview[] = [];
  janelaMensagem: string | null = null;
  statusMensagem: string | null = null;
  notificacaoErro: string | null = null;
  carregandoConfirmacao = false;
  carregandoRevalidacao = false;
  conexaoEstado: 'conectando' | 'conectado' | 'reconectando' | 'desconectado' = 'desconectado';
  reconexaoEmSegundos: number | null = null;
  reconexaoTentativa: number | null = null;

  private realtimeSub?: Subscription;
  private notificacaoSolicitada = false;

  constructor(
    private readonly gateApiService: GateApiService,
    private readonly realtimeService: AgendamentoRealtimeService,
    private readonly pushNotificationService: PushNotificationService,
    private readonly comprovanteService: AgendamentoComprovanteService,
    private readonly translate: TranslateService
  ) {}

  ngOnDestroy(): void {
    this.realtimeSub?.unsubscribe();
    this.liberarPreviews();
    this.conexaoEstado = 'desconectado';
    this.reconexaoEmSegundos = null;
    this.reconexaoTentativa = null;
  }

  aoSelecionarArquivos(evento: Event): void {
    const input = evento.target as HTMLInputElement | null;
    const arquivos = Array.from(input?.files ?? []);
    this.liberarPreviews();
    this.documentosSelecionados = arquivos.map((arquivo) => ({
      nome: arquivo.name,
      tamanho: this.formatarTamanho(arquivo.size),
      tipo: arquivo.type || 'application/octet-stream',
      url: arquivo.type.startsWith('image/') ? URL.createObjectURL(arquivo) : undefined,
      arquivo
    }));
  }

  enviarDocumentos(): void {
    if (!this.documentosSelecionados.length) {
      return;
    }
    this.anexarDocumentos.emit(this.documentosSelecionados.map((item) => item.arquivo));
    this.limparSelecao();
  }

  acompanharProgresso(nomeArquivo: string): UploadDocumentoStatus | undefined {
    return this.uploadStatus.find((status) => status.fileName === nomeArquivo);
  }

  existeUploadEmAndamento(): boolean {
    return this.uploadStatus.some((status) => status.status === 'enviando');
  }

  confirmarChegadaAntecipada(): void {
    if (!this.agendamento) {
      return;
    }
    this.carregandoConfirmacao = true;
    this.gateApiService
      .confirmarChegadaAntecipada(this.agendamento.id)
      .pipe(finalize(() => (this.carregandoConfirmacao = false)))
      .subscribe((agendamento) => {
        this._agendamento = agendamento;
        this.statusMensagem = this.translate.instant('gate.agendamentoDetalhe.statusAtualizado', {
          status: agendamento.statusDescricao ?? agendamento.status
        });
      });
  }

  revalidarDocumentos(): void {
    if (!this.agendamento) {
      return;
    }
    this.carregandoRevalidacao = true;
    this.gateApiService
      .revalidarDocumentos(this.agendamento.id)
      .pipe(finalize(() => (this.carregandoRevalidacao = false)))
      .subscribe((agendamento) => {
        this._agendamento = agendamento;
        this.statusMensagem = this.translate.instant('gate.agendamentoDetalhe.statusAtualizado', {
          status: agendamento.statusDescricao ?? agendamento.status
        });
      });
  }

  baixarComprovante(): void {
    if (!this.agendamento) {
      return;
    }
    const blob = this.comprovanteService.gerar(this.agendamento);
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${this.agendamento.codigo}-comprovante.txt`;
    link.click();
    URL.revokeObjectURL(url);
  }

  imprimir(): void {
    if (!this.agendamento) {
      return;
    }
    const blob = this.comprovanteService.gerar(this.agendamento);
    const url = URL.createObjectURL(blob);
    const janela = window.open(url);
    if (janela) {
      janela.onload = () => janela.print();
    } else {
      window.print();
    }
  }

  baixarInstrucoes(): void {
    if (!this.agendamento) {
      return;
    }
    const instrucoes = this.instrucoes;
    if (!instrucoes.length) {
      return;
    }
    const titulo = this.translate.instant('gate.agendamentoDetalhe.instrucoesArquivoTitulo', {
      codigo: this.agendamento.codigo
    });
    const conteudo = [
      titulo,
      '='.repeat(50),
      ...instrucoes.map((instrucao, indice) => `${indice + 1}. ${instrucao}`)
    ].join('\n');
    const blob = new Blob([conteudo], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${this.agendamento.codigo}-instrucoes.txt`;
    link.click();
    URL.revokeObjectURL(url);
  }

  trackPorDocumento(_: number, documento: DocumentoAgendamento): number {
    return documento.id;
  }

  trackPorPreview(_: number, preview: DocumentoPreview): string {
    return preview.nome;
  }

  get instrucoes(): string[] {
    const valor = this.translate.instant('gate.agendamentoDetalhe.instrucoes');
    return Array.isArray(valor) ? valor : [valor];
  }

  get conexaoParametros(): Record<string, string | number> {
    return {
      segundos: this.reconexaoEmSegundos ?? '',
      tentativa: this.reconexaoTentativa ?? ''
    };
  }

  private conectarRealtime(agendamento: Agendamento | null): void {
    this.realtimeSub?.unsubscribe();
    this.janelaMensagem = null;
    this.statusMensagem = null;
    this.notificacaoErro = null;
    this.conexaoEstado = agendamento ? 'conectando' : 'desconectado';
    this.reconexaoEmSegundos = null;
    this.reconexaoTentativa = null;
    if (!agendamento) {
      return;
    }
    if (!this.notificacaoSolicitada) {
      this.notificacaoSolicitada = true;
      this.pushNotificationService.requestPermission().then((granted) => {
        if (!granted) {
          this.notificacaoErro = this.translate.instant('gate.agendamentoDetalhe.pushPermissionDenied');
        }
      });
    }
    this.realtimeSub = this.realtimeService.conectar(agendamento.id).subscribe({
      next: (event) => this.processarEvento(event),
      error: () => {
        this.notificacaoErro = this.translate.instant('gate.agendamentoDetalhe.realtimeErro');
        this.conexaoEstado = 'desconectado';
      }
    });
  }

  private processarEvento(evento: AgendamentoRealtimeEvent): void {
    switch (evento.type) {
      case 'snapshot':
      case 'status-atualizado':
        this._agendamento = evento.data as Agendamento;
        if (evento.type === 'status-atualizado' && this.agendamento) {
          this.statusMensagem = this.translate.instant('gate.agendamentoDetalhe.statusAtualizado', {
            status: this.agendamento.statusDescricao ?? this.agendamento.status
          });
        }
        break;
      case 'conexao-estabelecida':
        this.conexaoEstado = 'conectado';
        this.reconexaoEmSegundos = null;
        this.reconexaoTentativa = null;
        break;
      case 'conexao-perdida':
        this.conexaoEstado = 'desconectado';
        break;
      case 'reconectando': {
        const payload = evento.data as RealtimeReconexaoPayload | null;
        this.reconexaoTentativa = payload?.tentativa ?? null;
        this.reconexaoEmSegundos = payload ? Math.max(1, Math.ceil(payload.delayMs / 1000)) : null;
        this.conexaoEstado = 'reconectando';
        break;
      }
      case 'janela-proxima':
        this.exibirJanelaProxima(evento.data as JanelaProximaPayload);
        break;
      case 'documentos-atualizados':
      case 'documentos-revalidados':
        if (this.agendamento) {
          this._agendamento = {
            ...this.agendamento,
            documentos: evento.data as DocumentoAgendamento[]
          };
        }
        break;
      case 'gate-pass-atualizado':
        if (this.agendamento) {
          this._agendamento = {
            ...this.agendamento,
            gatePass: evento.data as GatePass
          };
        }
        break;
    }
  }

  private async exibirJanelaProxima(payload: JanelaProximaPayload | null): Promise<void> {
    if (!payload) {
      return;
    }
    const minutos = Math.max(0, Math.round(payload.minutosRestantes));
    this.janelaMensagem = this.translate.instant('gate.agendamentoDetalhe.janelaProxima', {
      minutos
    });
    try {
      await this.pushNotificationService.showNotification(
        this.translate.instant('gate.agendamentoDetalhe.notificacaoJanelaTitulo'),
        {
          body: this.janelaMensagem,
          icon: 'assets/icons/bell.svg'
        }
      );
    } catch (error) {
      if (error instanceof Error && error.message === 'notification-permission-denied') {
        this.notificacaoErro = this.translate.instant('gate.agendamentoDetalhe.pushPermissionDenied');
      }
    }
  }

  private limparSelecao(): void {
    this.liberarPreviews();
    this.documentosSelecionados = [];
  }

  private liberarPreviews(): void {
    this.documentosSelecionados
      .filter((preview) => preview.url)
      .forEach((preview) => URL.revokeObjectURL(preview.url!));
  }

  private formatarTamanho(bytes: number): string {
    if (bytes === 0) {
      return '0 B';
    }
    const unidades = ['B', 'KB', 'MB', 'GB'];
    const indice = Math.floor(Math.log(bytes) / Math.log(1024));
    const tamanho = bytes / Math.pow(1024, indice);
    return `${tamanho.toFixed(1)} ${unidades[indice]}`;
  }
}
