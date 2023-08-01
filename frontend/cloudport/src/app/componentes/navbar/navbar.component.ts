import { Component } from '@angular/core';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { LoginComponent } from '../login/login.component';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})

export class NavbarComponent {
  mostrarMenu: boolean = false;

  constructor(
      private authenticationService: AuthenticationService,
      private router: Router
  ) {}

  ngOnInit(): void {
      this.authenticationService.currentMenuStatus.subscribe(
          mostrar => this.mostrarMenu = mostrar
      );
  }

  goToRole(): void {
    this.router.navigate(['/role']); // navega para a rota 'role'
  }
}
