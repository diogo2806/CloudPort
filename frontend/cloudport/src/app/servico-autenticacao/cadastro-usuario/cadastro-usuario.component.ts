import { Component } from '@angular/core';
import { UsuarioDTO } from '../models/usuario-dto'; // Atualize o caminho de importação conforme necessário
import { CadastroUsuarioService } from '../../service/cadastro-usuario-service/cadastro-usuario-service.service'; // Adicione o import para o serviço
//import { CadastroUsuarioService } from '../cadastro-usuario/cadastro-usuario-service/cadastro-usuario-service.service'; // Adicione o import para o serviço
import { NotificacaoService } from '../../notificacao/notificacao.service'; // Importe o serviço de notificação
import { Router } from '@angular/router';

@Component({
  selector: 'app-cadastro-usuario',
  templateUrl: './cadastro-usuario.component.html',
  styleUrls: ['./cadastro-usuario.component.css']
})
export class CadastroUsuarioComponent {
  usuario = new UsuarioDTO();

  // Injete o serviço no construtor
  constructor(
    private cadastroUsuarioService: CadastroUsuarioService, 
    private notificacaoService: NotificacaoService,
    private router: Router
  ) { }

  onSubmit(): void {
    // Chame o serviço para salvar o usuário
    this.cadastroUsuarioService.salvarUsuario(this.usuario).subscribe({
      next: (usuarioCadastrado) => {
        console.log(usuarioCadastrado);
        this.notificacaoService.show('Usuário cadastrado com sucesso!'); // Mostre a notificação
      },
      error: (err) => {
        console.error(err);
        this.notificacaoService.show('Ocorreu um erro ao cadastrar o usuário.'); // Mostre uma notificação de erro
      }
    });
  }

  voltarParaLogin(): void {
    this.router.navigate(['/login']);
  }
}
