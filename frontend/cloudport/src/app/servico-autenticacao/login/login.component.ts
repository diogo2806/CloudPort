import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../service/login-service/AuthService';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  username: string;
  password: string;
  errorMessage = 'Invalid Credentials';
  successMessage: string;
  invalidLogin = false;
  loginSuccess = false;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
  }

  handleLogin() {
     console.log(`Username: ${this.username}, Password: ${this.password}`);
     
    this.authService.login(this.username, this.password).subscribe((result) => {
      console.log('Resposta de login recebida:', result);
      this.invalidLogin = false;
      this.loginSuccess = true;
      this.successMessage = 'Login Successful';
      // redirect to main page
    }, (error) => {
      console.error('Erro durante o login:', error);
      this.invalidLogin = true;
      this.loginSuccess = false;
    });
  }

  limparCampos() {
    this.username = '';
    this.password = '';
  }
}
