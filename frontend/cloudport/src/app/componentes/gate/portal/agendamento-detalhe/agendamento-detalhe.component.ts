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
import { forkJoin, Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import {
  AgendamentoRealtimeEvent,
  AgendamentoRealtimeService
} from '../../../service/servico-gate/agendamento-realtime.service';
import { PushNotificationService } from '../../../service/servico-gate/push-notification.service';
import { AgendamentoComprovanteService } from '../../../service/servico-gate/agendamento-comprovante.service';
import { TranslateService } from '@ngx-translate/core';
import {
  DetalheConteiner,
  HistoricoConteiner,
  HistoricoConteinerService
} from '../../../service/servico-gate/historico-conteiner.service';
import { SanitizadorConteudoService } from '../../../service/sanitizacao/sanitizador-conteudo.service';

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
    this.configurarMonitoramentoConteiner(value?.codigo ?? null);
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
  conexaoEstado: 'conectando' | 'conectado' | 'reconectando' | 'desconectado' = 'desconectado';
  reconexaoEmSegundos: number | null = null;
  reconexaoTentativa: number | null = null;
  validacaoErroMensagem: string | null = null;
  historicoConteiner: HistoricoConteiner[] = [];
  detalheConteiner: DetalheConteiner | null = null;
  carregandoHistoricoConteiner = false;
  erroHistoricoConteiner: string | null = null;

  private realtimeSub?: Subscription;
  private notificacaoSolicitada = false;
  private historicoIntervalo?: ReturnType<typeof setInterval>;
  private codigoConteinerMonitorado: string | null = null;
  private readonly intervaloHistoricoMs = 15000;

  constructor(
    private readonly gateApiService: GateApiService,
    private readonly realtimeService: AgendamentoRealtimeService,
    private readonly pushNotificationService: PushNotificationService,
    private readonly comprovanteService: AgendamentoComprovanteService,
    private readonly translate: TranslateService,
    private readonly historicoConteinerService: HistoricoConteinerService,
    private readonly sanitizadorConteudo: SanitizadorConteudoService
  ) {}

  ngOnDestroy(): void {
    this.realtimeSub?.unsubscribe();
    this.liberarPreviews();
    this.conexaoEstado = 'desconectado';
    this.reconexaoEmSegundos = null;
    this.reconexaoTentativa = null;
    this.limparMonitoramentoConteiner();
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
    this.validacaoErroMensagem = null;
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
        this.recarregarHistoricoSeNecessario();
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
      case 'documentos-revalidados': {
        const documentos = evento.data as DocumentoAgendamento[];
        this.gerenciarStatusDocumentos(documentos);
        if (this.agendamento) {
          this._agendamento = {
            ...this.agendamento,
            documentos
          };
        }
        break;
      }
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

  obterClasseStatus(documento: DocumentoAgendamento): string {
    const base = 'documento__status';
    switch (documento.statusValidacao) {
      case 'VALIDADO':
        return `${base} documento__status--sucesso`;
      case 'FALHA':
        return `${base} documento__status--erro`;
      default:
        return `${base} documento__status--processando`;
    }
  }

  descricaoStatus(documento: DocumentoAgendamento): string {
    return documento.statusValidacaoDescricao ?? documento.statusValidacao;
  }

  private gerenciarStatusDocumentos(documentos: DocumentoAgendamento[]): void {
    if (!documentos) {
      return;
    }
    const anteriores = new Map<number, string>();
    (this.agendamento?.documentos ?? []).forEach((doc) => anteriores.set(doc.id, doc.statusValidacao));

    let mensagemSucesso: string | null = null;
    let mensagemErro: string | null = null;

    documentos.forEach((doc) => {
      const anterior = anteriores.get(doc.id);
      const nomeDocumento = doc.nomeArquivo || doc.tipoDocumento;
      if (doc.statusValidacao === 'PROCESSANDO' && anterior !== 'PROCESSANDO') {
        this.statusMensagem = this.translate.instant('gate.agendamentoDetalhe.documentoProcessando', {
          nome: nomeDocumento
        });
        this.validacaoErroMensagem = null;
      } else if (doc.statusValidacao === 'VALIDADO' && anterior !== 'VALIDADO') {
        mensagemSucesso = this.translate.instant('gate.agendamentoDetalhe.documentoValidado', {
          nome: nomeDocumento
        });
      } else if (doc.statusValidacao === 'FALHA' && anterior !== 'FALHA') {
        mensagemErro = this.translate.instant('gate.agendamentoDetalhe.documentoFalhou', {
          nome: nomeDocumento,
          motivo: doc.mensagemValidacao ? ` ${doc.mensagemValidacao}` : ''
        });
      }
    });

    if (mensagemSucesso) {
      this.statusMensagem = mensagemSucesso;
      this.validacaoErroMensagem = null;
    }
    if (mensagemErro) {
      this.validacaoErroMensagem = mensagemErro;
      this.statusMensagem = null;
    }
  }

  private configurarMonitoramentoConteiner(codigoConteiner: string | null): void {
    const codigoNormalizado = (codigoConteiner ?? '').trim();
    if (this.codigoConteinerMonitorado === codigoNormalizado) {
      return;
    }
    this.limparMonitoramentoConteiner();
    if (!codigoNormalizado) {
      return;
    }
    this.codigoConteinerMonitorado = codigoNormalizado;
    this.carregarHistoricoConteiner(false);
    if (typeof window !== 'undefined') {
      this.historicoIntervalo = window.setInterval(() => this.carregarHistoricoConteiner(true), this.intervaloHistoricoMs);
    }
  }

  private limparMonitoramentoConteiner(): void {
    if (this.historicoIntervalo) {
      clearInterval(this.historicoIntervalo);
      this.historicoIntervalo = undefined;
    }
    this.codigoConteinerMonitorado = null;
    this.historicoConteiner = [];
    this.detalheConteiner = null;
    this.erroHistoricoConteiner = null;
    this.carregandoHistoricoConteiner = false;
  }

  private carregarHistoricoConteiner(silencioso: boolean): void {
    const codigo = this.codigoConteinerMonitorado;
    if (!codigo) {
      return;
    }
    if (!silencioso) {
      this.carregandoHistoricoConteiner = true;
      this.erroHistoricoConteiner = null;
    }
    forkJoin({
      detalhe: this.historicoConteinerService.obterDetalhePorCodigo(codigo),
      historico: this.historicoConteinerService.obterHistoricoPorCodigo(codigo)
    })
      .pipe(finalize(() => {
        if (!silencioso) {
          this.carregandoHistoricoConteiner = false;
        }
      }))
      .subscribe({
        next: ({ detalhe, historico }) => {
          this.detalheConteiner = detalhe;
          this.historicoConteiner = historico;
          this.erroHistoricoConteiner = null;
        },
        error: () => {
          this.erroHistoricoConteiner = this.translate.instant(
            'gate.agendamentoDetalhe.historicoConteiner.erroCarregamento'
          );
          if (!silencioso) {
            this.carregandoHistoricoConteiner = false;
          }
        }
      });
  }

  private recarregarHistoricoSeNecessario(): void {
    if (this.codigoConteinerMonitorado) {
      this.carregarHistoricoConteiner(true);
    }
  }

  get codigoConteinerSelecionado(): string | null {
    return this.codigoConteinerMonitorado;
  }

  trackPorHistorico(index: number, item: HistoricoConteiner): string {
    return `${item.dataRegistro}-${item.tipoOperacao}-${index}`;
  }

  descricaoTipoOperacao(tipoOperacao: string): string {
    const mapa: Record<string, string> = {
      DESCARGA_TREM: this.translate.instant('gate.agendamentoDetalhe.historicoConteiner.descargaTrem'),
      CARGA_TREM: this.translate.instant('gate.agendamentoDetalhe.historicoConteiner.cargaTrem'),
      ALOCACAO: this.translate.instant('gate.agendamentoDetalhe.historicoConteiner.alocacao'),
      TRANSFERENCIA: this.translate.instant('gate.agendamentoDetalhe.historicoConteiner.transferencia'),
      INSPECAO: this.translate.instant('gate.agendamentoDetalhe.historicoConteiner.inspecao'),
      LIBERACAO: this.translate.instant('gate.agendamentoDetalhe.historicoConteiner.liberacao'),
      ATUALIZACAO_CADASTRAL: this.translate.instant('gate.agendamentoDetalhe.historicoConteiner.atualizacaoCadastral')
    };
    return mapa[tipoOperacao] ?? tipoOperacao;
  }

  formatarStatusConteiner(): string {
    const status = this.detalheConteiner?.statusOperacional ?? '';
    if (!status) {
      return '';
    }
    const chave = status.toLowerCase().replace(/_/g, ' ');
    return chave.charAt(0).toUpperCase() + chave.slice(1);
  }

  sanitizarTexto(valor: string | null | undefined): string {
    return this.sanitizadorConteudo.sanitizar(valor ?? '');
  }
}
