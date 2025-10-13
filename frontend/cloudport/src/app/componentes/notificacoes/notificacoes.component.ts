import { Component } from '@angular/core';

interface CanalNotificacao {
  canal: string;
  habilitado: boolean;
}

@Component({
  selector: 'app-notificacoes',
  templateUrl: './notificacoes.component.html',
  styleUrls: ['./notificacoes.component.css']
})
export class NotificacoesComponent {
  canais: CanalNotificacao[] = [
    { canal: 'E-mail', habilitado: true },
    { canal: 'SMS', habilitado: false },
    { canal: 'Aplicativo móvel', habilitado: true }
  ];
}
