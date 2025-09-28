import { Component, OnDestroy, OnInit } from '@angular/core';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { Router, ActivatedRoute, RouteReuseStrategy } from '@angular/router';
import { CustomReuseStrategy } from '../tab-content/customreusestrategy';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit, OnDestroy {

  userToken: string = '';
  mostrarMenu: boolean = false;
  private menuStatusSubscription?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authenticationService: AuthenticationService,
    private reuseStrategy: RouteReuseStrategy 
  ) {
    let currentUser: any = this.authenticationService.currentUserValue;
    if (currentUser && currentUser.token) {
      this.userToken = currentUser.token;
    }
  }


  ngOnInit(): void {
    this.mostrarMenu = this.authenticationService.getMenuStatusValue();
    this.menuStatusSubscription = this.authenticationService.currentMenuStatus.subscribe(status => {
      this.mostrarMenu = status;
    });
  }

  ngOnDestroy(): void {
    this.menuStatusSubscription?.unsubscribe();
  }


  logout() {
    this.authenticationService.logout();
    (this.reuseStrategy as CustomReuseStrategy).markForDestruction('login'.toLowerCase());
    this.router.navigate(['login']);
  }


}
