import { Component, OnInit } from '@angular/core';
import { finalize } from 'rxjs/operators';

import { UsuariosService, UsuarioResumo } from '../service/servico-autenticacao/usuarios.service';

@Component({
  selector: 'app-usuarios-lista',
  templateUrl: './usuarios-lista.component.html',
  styleUrls: ['./usuarios-lista.component.css']
})
export class UsuariosListaComponent implements OnInit {
  usuarios: UsuarioResumo[] = [];
  estaCarregando = false;
  erroCarregamento: string | null = null;

  constructor(private readonly usuariosService: UsuariosService) {}

  ngOnInit(): void {
    this.carregarUsuarios();
  }

  private carregarUsuarios(): void {
    this.estaCarregando = true;
    this.erroCarregamento = null;
    this.usuariosService
      .listarUsuarios()
      .pipe(finalize(() => (this.estaCarregando = false)))
      .subscribe({
        next: (usuarios) => {
          this.usuarios = usuarios;
        },
        error: (erro) => {
          this.erroCarregamento = this.resolverMensagemErro(erro);
          this.usuarios = [];
        }
      });
  }

  private resolverMensagemErro(erro: any): string {
    const mensagem = erro?.error?.message ?? erro?.message;
    return mensagem && typeof mensagem === 'string'
      ? mensagem
      : 'Não foi possível carregar os usuários. Tente novamente em instantes.';
  }
}
