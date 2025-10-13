import { Component } from '@angular/core';

@Component({
  selector: 'app-privacidade',
  templateUrl: './privacidade.component.html',
  styleUrls: ['./privacidade.component.css']
})
export class PrivacidadeComponent {
  opcoes: { descricao: string; ativo: boolean }[] = [
    { descricao: 'Permitir compartilhamento de dados com parceiros', ativo: false },
    { descricao: 'Mostrar minha atividade para outros usuários', ativo: false },
    { descricao: 'Receber relatórios de auditoria mensais', ativo: true }
  ];
}
