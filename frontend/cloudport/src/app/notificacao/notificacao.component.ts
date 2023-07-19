import { Component, OnInit } from '@angular/core';
import { NotificacaoService } from './notificacao.service'; // Atualize o caminho de importação conforme necessário

@Component({
  selector: 'app-notificacao',
  templateUrl: './notificacao.component.html',
  styleUrls: ['./notificacao.component.css']
})
export class NotificacaoComponent implements OnInit {
  mensagem: string | null = null;

  constructor(private notificacaoService: NotificacaoService) { }

  ngOnInit(): void {
    this.notificacaoService.notificacao$.subscribe(mensagem => {
      this.mensagem = mensagem;
      // Reset the message after 3 seconds
      setTimeout(() => this.mensagem = null, 3000);
    });
  }
}
