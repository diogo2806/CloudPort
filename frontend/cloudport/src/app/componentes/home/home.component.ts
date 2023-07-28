import { Component } from '@angular/core';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {
  userToken: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authenticationService: AuthenticationService
  ) {
    // aqui estamos obtendo o valor do token do currentUserValue no serviço de autenticação
   let currentUser: any = this.authenticationService.currentUserValue;
    if (currentUser && currentUser.token) {
      this.userToken = currentUser.token;
    }
  }

  logout() {
    this.authenticationService.logout()
    this.router.navigate(['login']);
  }

  Alert(){
    alert(this.authenticationService.currentUserValue?.token)
  }
}
