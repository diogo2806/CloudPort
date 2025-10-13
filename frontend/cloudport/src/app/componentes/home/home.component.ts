import { Component, ViewChild, OnInit } from '@angular/core';
import { Router, RouteReuseStrategy, RouterOutlet } from '@angular/router';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { TabService } from '../navbar/TabService';
import { CustomReuseStrategy } from '../tab-content/customreusestrategy';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  @ViewChild('outlet', { read: RouterOutlet }) outlet!: RouterOutlet;
  userToken: string = '';
  tabs: string[] = [];
  selectedTab = '';
  filteredData: any[] = [];
  data: { [key: string]: any } = {};
  tabContent: { [key: string]: any } = {};
  private readonly validChildRoutes = new Set([
    'role',
    'seguranca',
    'notificacoes',
    'privacidade',
    'lista-de-usuarios'
  ]);

  constructor(
    private router: Router,
    private authenticationService: AuthenticationService,
    private tabService: TabService,
    private reuseStrategy: RouteReuseStrategy
  ) {
    console.log('Classe HomeComponent: Método construtor chamado.');
    const currentUser: any = this.authenticationService.currentUserValue;
    if (currentUser && currentUser.token) {
      this.userToken = currentUser.token;
    }
  }

  ngOnInit() {
    console.log('Classe HomeComponent: Método ngOnInit iniciado.');
    this.tabService.tabs$.subscribe(tabs => {
      this.tabs = tabs;
      if (tabs.length > 0) {
        this.selectedTab = tabs[tabs.length - 1];
        this.router.navigate(['/home', this.resolveChildRoute(this.selectedTab)]);
      }
    });
    (this.reuseStrategy as CustomReuseStrategy).markForDestruction('login'.toLowerCase());
  }

  navigateTo(tabName: string) {
    this.selectedTab = tabName;
    this.tabContent[tabName] = this.tabService.getTabContent(tabName);
    this.router.navigate(['/home', this.resolveChildRoute(tabName)]);
  }

  logout() {
    console.log('Classe HomeComponent: Método logout chamado.');
    this.authenticationService.logout();
    (this.reuseStrategy as CustomReuseStrategy).markForDestruction('login'.toLowerCase());
    this.router.navigate(['login']);
  }

  Alert() {
    console.log('Classe HomeComponent: Método Alert chamado.');
    alert(this.authenticationService.currentUserValue?.token);
  }

  closeTab(tab: string) {
    console.log(`Classe HomeComponent: Método closeTab chamado com o parâmetro clearHandlers=${tab}.`);
    const route = this.resolveChildRoute(tab);
    (this.reuseStrategy as CustomReuseStrategy).markForDestruction(route);
    console.log(`Classe HomeComponent: Método closeTab chamado com o parâmetro tab=${tab}.`);
    this.tabService.closeTab(tab);
  }

  objectKeys(obj: any): string[] {
    return Object.keys(obj);
  }

  private resolveChildRoute(tabName: string): string {
    const normalizedTab = this.normalizeTabName(tabName);
    return this.validChildRoutes.has(normalizedTab) ? normalizedTab : 'role';
  }

  private normalizeTabName(tabName: string): string {
    return tabName
      ? tabName
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .replace(/\s+/g, '-')
      : '';
  }
}
