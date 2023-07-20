import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../login/login-service/AuthService';


@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  username: string;
  //username: string;
  password: string;
  errorMessage = 'Invalid Credentials';
  successMessage: string;
  invalidLogin = false;
  loginSuccess = false;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
  }

  handleLogin() {
    this.authService.login(this.username, this.password).subscribe((result) => {
      this.invalidLogin = false;
      this.loginSuccess = true;
      this.successMessage = 'Login Successful';
      // redirect to main page
    }, () => {
      this.invalidLogin = true;
      this.loginSuccess = false;
    });
  }

  limparCampos() {
    this.username = '';
    this.password = '';
  }

}

/*  onSubmit() {
    // Implemente a lógica do login aqui.
    console.log(`Username: ${this.username}, Password: ${this.password}`);
    
    // Se o login for bem-sucedido, navegue até a tela de solicitar acesso:
    this.router.navigate(['/cadastro-usuario']);
  }

  limparCampos() {
    this.username = '';
    this.password = '';
  }
  */