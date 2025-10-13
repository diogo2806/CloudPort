import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { Agendamento, DocumentoAgendamento, UploadDocumentoStatus } from '../../../model/gate/agendamento.model';

interface DocumentoPreview {
  nome: string;
  tamanho: string;
  tipo: string;
  url?: string;
  arquivo: File;
}

@Component({
  selector: 'app-agendamento-detalhe',
  templateUrl: './agendamento-detalhe.component.html',
  styleUrls: ['./agendamento-detalhe.component.css']
})
export class AgendamentoDetalheComponent implements OnChanges, OnDestroy {
  @Input() agendamento: Agendamento | null = null;
  @Input() uploadStatus: UploadDocumentoStatus[] = [];
  @Output() anexarDocumentos = new EventEmitter<File[]>();

  documentosSelecionados: DocumentoPreview[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['agendamento']) {
      this.limparSelecao();
    }
  }

  ngOnDestroy(): void {
    this.liberarPreviews();
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

  trackPorDocumento(_: number, documento: DocumentoAgendamento): number {
    return documento.id;
  }

  trackPorPreview(_: number, preview: DocumentoPreview): string {
    return preview.nome;
  }
}

