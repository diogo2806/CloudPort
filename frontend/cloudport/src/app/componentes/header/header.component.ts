import { Component } from '@angular/core';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { Router, ActivatedRoute } from '@angular/router';
@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {

  userToken: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authenticationService: AuthenticationService
  ) {
    let currentUser: any = this.authenticationService.currentUserValue;
    if (currentUser && currentUser.token) {
      this.userToken = currentUser.token;
    }
  }


  logout() {
    this.authenticationService.logout();
    this.router.navigate(['login']);
  }


}
