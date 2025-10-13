import { Component } from '@angular/core';

@Component({
  selector: 'app-seguranca',
  templateUrl: './seguranca.component.html',
  styleUrls: ['./seguranca.component.css']
})
export class SegurancaComponent {
  readonly dicas: string[] = [
    'Atualize sua senha periodicamente.',
    'Utilize autenticação multifator sempre que disponível.',
    'Revise os acessos concedidos à sua conta com frequência.'
  ];
}
