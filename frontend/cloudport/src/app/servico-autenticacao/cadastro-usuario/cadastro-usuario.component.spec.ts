import { Component } from '@angular/core';
import { CadastroUsuarioService } from './cadastro-usuario-service/cadastro-usuario-service.service';
import { UsuarioDTO } from '../models/usuario-dto';
import { NotificacaoService } from '../../notificacao/notificacao.service'; // Importe o serviço de notificação

@Component({
  selector: 'app-cadastro-usuario',
  templateUrl: './cadastro-usuario.component.html',
  styleUrls: ['./cadastro-usuario.component.css']
})
export class CadastroUsuarioComponent {
  usuario: UsuarioDTO = new UsuarioDTO();

  constructor(
    private cadastroUsuarioService: CadastroUsuarioService,
    private notificacaoService: NotificacaoService
    ) { }

  onSubmit() {
    this.cadastroUsuarioService.salvarUsuario(this.usuario)
      .subscribe(result => {
        // Tratar resultado
        console.log(result);
        this.notificacaoService.show('Usuário cadastrado com sucesso!'); // Mostre a notificação
      }, error => {
        // Tratar erro
        console.log(error);
        this.notificacaoService.show('Ocorreu um erro ao cadastrar o usuário.'); // Mostre uma notificação de erro
      });
  }
}
