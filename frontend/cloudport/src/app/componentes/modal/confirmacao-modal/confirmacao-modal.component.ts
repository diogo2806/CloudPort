import { Component, Input } from '@angular/core';
import { PopupService, ConfirmacaoModalData } from '../../service/popupService';

@Component({
  selector: 'app-confirmacao-modal',
  templateUrl: './confirmacao-modal.component.html',
  styleUrls: ['./confirmacao-modal.component.css']
})
export class ConfirmacaoModalComponent {
  @Input() data: ConfirmacaoModalData | null = null;

  constructor(private readonly popupService: PopupService) {}

  confirmar(): void {
    this.popupService.resolveConfirmacao(true);
  }

  cancelar(): void {
    this.popupService.resolveConfirmacao(false);
  }
}

