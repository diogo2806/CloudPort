import { Component, ViewChild, OnInit } from '@angular/core';
import { Router, RouteReuseStrategy, RouterOutlet } from '@angular/router';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { TabItem, TabService } from '../navbar/TabService';
import { CustomReuseStrategy } from '../tab-content/customreusestrategy';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  @ViewChild('outlet', { read: RouterOutlet }) outlet!: RouterOutlet;
  userToken: string = '';
  tabs: TabItem[] = [];
  selectedTabId = '';
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
        const lastTab = tabs[tabs.length - 1];
        this.selectedTabId = lastTab.id;
        this.router.navigate(['/home', this.resolveChildRoute(this.selectedTabId)]);
      } else {
        this.selectedTabId = '';
      }
    });
    (this.reuseStrategy as CustomReuseStrategy).markForDestruction('login'.toLowerCase());
  }

  navigateTo(tabId: string) {
    this.selectedTabId = tabId;
    this.tabContent[tabId] = this.tabService.getTabContent(tabId);
    this.router.navigate(['/home', this.resolveChildRoute(tabId)]);
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

  closeTab(tabId: string) {
    console.log(`Classe HomeComponent: Método closeTab chamado com o parâmetro clearHandlers=${tabId}.`);
    const route = this.resolveChildRoute(tabId);
    (this.reuseStrategy as CustomReuseStrategy).markForDestruction(route);
    console.log(`Classe HomeComponent: Método closeTab chamado com o parâmetro tab=${tabId}.`);
    this.tabService.closeTab(tabId);
    delete this.tabContent[tabId];
  }

  objectKeys(obj: any): string[] {
    return Object.keys(obj);
  }

  private resolveChildRoute(tabId: string): string {
    return this.validChildRoutes.has(tabId) ? tabId : 'role';
  }
}
