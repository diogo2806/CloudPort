import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import {
  Agendamento,
  DocumentoRevalidacaoResultado,
  GatePassQrCode
} from '../../../model/gate/agendamento.model';
import { GateApiService } from '../../../service/servico-gate/gate-api.service';
import { finalize } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { NotificationBridgeService } from '../../../service/notification-bridge.service';

@Component({
  selector: 'app-motorista-pass',
  templateUrl: './motorista-pass.component.html',
  styleUrls: ['./motorista-pass.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MotoristaPassComponent implements OnChanges {
  @Input() agendamento: Agendamento | null = null;
  @Output() atualizado = new EventEmitter<Agendamento>();
  @Output() documentosRevalidados = new EventEmitter<DocumentoRevalidacaoResultado[]>();

  qrCode: GatePassQrCode | null = null;
  carregandoQr = false;
  revalidando = false;
  confirmando = false;
  erro?: string;

  constructor(
    private readonly gateApi: GateApiService,
    private readonly translate: TranslateService,
    private readonly notificationBridge: NotificationBridgeService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['agendamento']) {
      this.qrCode = null;
      if (this.agendamento?.id) {
        this.carregarQrCode();
      }
    }
  }

  carregarQrCode(): void {
    if (!this.agendamento?.id) {
      return;
    }
    this.carregandoQr = true;
    this.gateApi
      .gerarQrCode(this.agendamento.id)
      .pipe(finalize(() => (this.carregandoQr = false)))
      .subscribe({
        next: (qr) => {
          this.qrCode = qr;
        },
        error: () => {
          this.erro = this.translate.instant('gate.detail.qr.error');
        }
      });
  }

  confirmarChegada(): void {
    if (!this.agendamento?.id) {
      return;
    }
    this.confirmando = true;
    this.gateApi
      .confirmarChegadaAntecipada(this.agendamento.id, {
        antecipada: true,
        dataHoraChegada: new Date().toISOString()
      })
      .pipe(finalize(() => (this.confirmando = false)))
      .subscribe((agendamento) => {
        this.atualizado.emit(agendamento);
        const mensagem = this.translate.instant('gate.detail.actions.confirmArrivalSuccess');
        this.notificationBridge.notify(mensagem, { body: agendamento.codigo });
      });
  }

  revalidarDocumentos(): void {
    if (!this.agendamento?.id) {
      return;
    }
    this.revalidando = true;
    this.gateApi
      .revalidarDocumentos(this.agendamento.id)
      .pipe(finalize(() => (this.revalidando = false)))
      .subscribe((response) => {
        this.atualizado.emit(response.agendamento);
        this.documentosRevalidados.emit(response.resultados);
        const mensagem = this.translate.instant('gate.detail.actions.revalidateSuccess');
        this.notificationBridge.notify(mensagem, { body: response.agendamento.codigo });
      });
  }

  gerarLinkDownload(): void {
    if (!this.agendamento || !this.qrCode) {
      return;
    }
    const conteudo = [
      `${this.translate.instant('gate.detail.fields.code')}: ${this.agendamento.codigo}`,
      `${this.translate.instant('gate.detail.fields.carrier')}: ${this.agendamento.transportadoraNome ?? '—'}`,
      `${this.translate.instant('gate.detail.fields.driver')}: ${this.agendamento.motoristaNome ?? '—'}`,
      `${this.translate.instant('gate.detail.fields.window')}: ${this.agendamento.dataJanela ?? '—'} ${
        this.agendamento.horaInicioJanela ?? ''
      } - ${this.agendamento.horaFimJanela ?? ''}`
    ].join('\n');
    const blob = new Blob([conteudo], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `${this.agendamento.codigo}-comprovante.txt`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  imprimir(): void {
    if (!this.qrCode) {
      return;
    }
    const printWindow = window.open('', '_blank', 'noopener');
    if (!printWindow) {
      return;
    }
    printWindow.document.write(`
      <html>
        <head>
          <title>${this.translate.instant('gate.detail.title')}</title>
          <style>
            body { font-family: Arial, sans-serif; text-align: center; padding: 24px; }
            img { width: 240px; image-rendering: pixelated; }
          </style>
        </head>
        <body>
          <h1>${this.agendamento?.codigo ?? ''}</h1>
          <img src="data:${this.qrCode.mimeType};base64,${this.qrCode.base64}" alt="QR" />
          <p>${this.translate.instant('gate.detail.qr.subtitle')}</p>
        </body>
      </html>
    `);
    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
    printWindow.close();
  }

  get qrCodeDataUrl(): string | null {
    if (!this.qrCode) {
      return null;
    }
    return `data:${this.qrCode.mimeType};base64,${this.qrCode.base64}`;
  }
}
