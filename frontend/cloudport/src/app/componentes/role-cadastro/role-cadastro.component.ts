import { Component } from '@angular/core';

@Component({
  selector: 'app-role-cadastro',
  templateUrl: './role-cadastro.component.html',
  styleUrls: ['./role-cadastro.component.css']
})
export class RoleCadastroComponent {

  roleName: string = ''; // Propriedade para armazenar o Role Name

  saveRole() {
    // Lógica para salvar o Role com o nome fornecido
    console.log('Salvando Role com o nome:', this.roleName);
  }
}
