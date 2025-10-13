import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { GatePass } from '../../../model/gate/agendamento.model';

@Component({
  selector: 'app-motorista-pass',
  templateUrl: './motorista-pass.component.html',
  styleUrls: ['./motorista-pass.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MotoristaPassComponent {
  @Input() gatePass: GatePass | null = null;
  @Input() agendamentoCodigo: string | null = null;

  get qrData(): string | null {
    if (!this.gatePass) {
      return null;
    }
    return this.gatePass.token || this.gatePass.codigo;
  }

  get statusDescricao(): string | null {
    if (!this.gatePass) {
      return null;
    }
    return this.gatePass.statusDescricao || this.gatePass.status;
  }

  get atualizadoEm(): string | null {
    if (!this.gatePass) {
      return null;
    }
    const eventos = this.gatePass.eventos ?? [];
    if (eventos.length > 0) {
      return eventos[eventos.length - 1].registradoEm;
    }
    return this.gatePass.dataEntrada || this.gatePass.dataSaida;
  }
}
