import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Router, RouteReuseStrategy, RouterOutlet } from '@angular/router';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { TabItem, TabService } from '../navbar/TabService';
import { CustomReuseStrategy } from '../tab-content/customreusestrategy';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit, OnDestroy {

  @ViewChild('outlet', { read: RouterOutlet }) outlet!: RouterOutlet;
  userToken: string = '';
  tabs: TabItem[] = [];
  selectedTabId = '';
  filteredData: any[] = [];
  data: { [key: string]: any } = {};
  tabContent: { [key: string]: any } = {};
  private tabSubscription?: Subscription;
  private readonly defaultChildRoute = 'role';
  private readonly validChildRoutes = new Set([
    this.defaultChildRoute,
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
    this.tabSubscription = this.tabService.tabs$.subscribe(tabs => {
      this.tabs = tabs;
      if (tabs.length > 0) {
        const lastTab = tabs[tabs.length - 1];
        const route = this.resolveChildRoute(lastTab.id);
        this.selectedTabId = this.validChildRoutes.has(lastTab.id) ? lastTab.id : this.defaultChildRoute;
        this.router.navigate(['/home', route]);
      } else {
        this.selectedTabId = this.defaultChildRoute;
        this.router.navigate(['/home', this.defaultChildRoute]);
      }
    });
    (this.reuseStrategy as CustomReuseStrategy).markForDestruction('login'.toLowerCase());
  }

  ngOnDestroy(): void {
    this.tabSubscription?.unsubscribe();
  }

  navigateTo(tabId: string) {
    const route = this.resolveChildRoute(tabId);
    const normalizedTabId = this.validChildRoutes.has(tabId) ? tabId : this.defaultChildRoute;
    this.selectedTabId = normalizedTabId;
    this.tabContent[normalizedTabId] = this.tabService.getTabContent(normalizedTabId);
    this.router.navigate(['/home', route]);
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
    if (route !== tabId) {
      delete this.tabContent[route];
    }
  }

  objectKeys(obj: any): string[] {
    return Object.keys(obj);
  }

  private resolveChildRoute(tabId: string): string {
    const normalizedTab = tabId?.toLowerCase() ?? '';
    return this.validChildRoutes.has(normalizedTab) ? normalizedTab : this.defaultChildRoute;
  }
}
