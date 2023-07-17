import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  username: string;
  password: string;

  constructor(private router: Router) {
    this.username = '';
    this.password = '';
  }

  onSubmit() {
    // Implemente a lógica do login aqui.
    console.log(`Username: ${this.username}, Password: ${this.password}`);
    
    // Se o login for bem-sucedido, navegue até a tela de solicitar acesso:
    this.router.navigate(['/solicitar-acesso']);
  }

  limparCampos() {
    this.username = '';
    this.password = '';
  }
}
