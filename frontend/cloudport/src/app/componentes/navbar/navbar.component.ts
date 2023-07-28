import { Component } from '@angular/core';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { LoginComponent } from '../login/login.component';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})

export class NavbarComponent {
  mostrarMenu: boolean = false;

  constructor(
      private authenticationService: AuthenticationService,
  ) {}

  ngOnInit(): void {
      this.authenticationService.currentMenuStatus.subscribe(
          mostrar => this.mostrarMenu = mostrar
      );
  }
}
