import { Component, OnInit } from '@angular/core';
import { finalize } from 'rxjs';
import { CanalNotificacao, NotificacoesService } from '../service/notificacoes/notificacoes.service';

@Component({
  selector: 'app-notificacoes',
  templateUrl: './notificacoes.component.html',
  styleUrls: ['./notificacoes.component.css']
})
export class NotificacoesComponent implements OnInit {
  canais: CanalNotificacao[] = [];
  carregando = false;
  salvandoId: number | null = null;
  mensagemErro?: string;
  mensagemSucesso?: string;

  constructor(private readonly notificacoesService: NotificacoesService) {}

  ngOnInit(): void {
    this.carregarCanais();
  }

  carregarCanais(): void {
    this.carregando = true;
    this.mensagemErro = undefined;
    this.mensagemSucesso = undefined;
    this.notificacoesService
      .listarCanais()
      .pipe(finalize(() => (this.carregando = false)))
      .subscribe({
        next: canais => {
          this.canais = canais;
        },
        error: erro => {
          this.mensagemErro = erro?.message || 'Não foi possível carregar as preferências de notificações.';
        }
      });
  }

  alternarStatus(canal: CanalNotificacao): void {
    if (this.salvandoId !== null) {
      return;
    }
    this.mensagemErro = undefined;
    this.mensagemSucesso = undefined;
    this.salvandoId = canal.identificador;
    const novoStatus = !canal.habilitado;
    this.notificacoesService
      .atualizarStatus(canal.identificador, novoStatus)
      .pipe(finalize(() => (this.salvandoId = null)))
      .subscribe({
        next: atualizado => {
          const indice = this.canais.findIndex(item => item.identificador === atualizado.identificador);
          if (indice >= 0) {
            this.canais[indice] = atualizado;
          }
          this.mensagemSucesso = `Canal ${atualizado.nomeCanal} atualizado com sucesso.`;
        },
        error: erro => {
          this.mensagemErro = erro?.message || 'Não foi possível atualizar o canal de notificação.';
        }
      });
  }
}
