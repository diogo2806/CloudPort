import { Component } from '@angular/core';
import { CadastroUsuarioService } from './cadastro-usuario-service/cadastro-usuario-service.service';
import { UsuarioDTO } from '../models/usuario-dto';

@Component({
  selector: 'app-cadastro-usuario',
  templateUrl: './cadastro-usuario.component.html',
  styleUrls: ['./cadastro-usuario.component.css']
})
export class CadastroUsuarioComponent {
  usuario: UsuarioDTO = new UsuarioDTO();

  constructor(private cadastroUsuarioService: CadastroUsuarioService) { }

  onSubmit() {
    this.cadastroUsuarioService.salvarUsuario(this.usuario)
      .subscribe(result => {
        // Tratar resultado
        console.log(result);
      }, error => {
        // Tratar erro
        console.log(error);
      });
  }
}
