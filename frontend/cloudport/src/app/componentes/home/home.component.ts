import { Component, ViewChild, AfterViewInit, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router'; // Importação correta para RouterOutlet
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { Router, ActivatedRoute, RouteReuseStrategy } from '@angular/router';
import { TabService } from '../navbar/TabService';
import { CustomReuseStrategy } from '../tab-content/customreusestrategy';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit  {

  @ViewChild('outlet', { read: RouterOutlet }) outlet!: RouterOutlet; // Adicionado o modificador '!' aqui
  userToken: string = '';
  tabs: string[] = [];
  selectedTab = '';
  filteredData: any[] = [];
  data: { [key: string]: any } = {};
  tabContent: { [key: string]: any } = {};

  constructor(
    private router: Router,
    private authenticationService: AuthenticationService,
    private tabService: TabService,
    private reuseStrategy: RouteReuseStrategy // Injete a estratégia de reutilização de rota aqui
  ) {
    console.log("Classe HomeComponent: Método construtor chamado.");
    let currentUser: any = this.authenticationService.currentUserValue;
    if (currentUser && currentUser.token) {
      this.userToken = currentUser.token;
    }
  }


  ngOnInit() {
    console.log("Classe HomeComponent: Método ngOnInit iniciado.");
    this.tabService.tabs$.subscribe(tabs => {
      this.tabs = tabs;
      if (tabs.length > 0) {
        this.selectedTab = tabs[tabs.length - 1];
        this.router.navigate(['/home', this.selectedTab.toLowerCase()]);
      }
    });
    (this.reuseStrategy as CustomReuseStrategy).markForDestruction('login'.toLowerCase());

  }


  navigateTo(tabName: string) {
    this.selectedTab = tabName;
    this.tabContent[tabName] = this.tabService.getTabContent(tabName);
    this.router.navigate(['/home', tabName.toLowerCase()]);
  }

  
  logout() {
    console.log("Classe HomeComponent: Método logout chamado.");
    this.authenticationService.logout();
   
    (this.reuseStrategy as CustomReuseStrategy).markForDestruction('login'.toLowerCase());
    this.router.navigate(['login']);
  }

  Alert() {
    console.log("Classe HomeComponent: Método Alert chamado.");
    alert(this.authenticationService.currentUserValue?.token);
  }

  closeTab(tab: string) {
    console.log(`Classe HomeComponent: Método closeTab chamado com o parâmetro clearHandlers=${tab}.`);
     (this.reuseStrategy as CustomReuseStrategy).markForDestruction(tab.toLowerCase());
    console.log(`Classe HomeComponent: Método closeTab chamado com o parâmetro tab=${tab}.`);
    this.tabService.closeTab(tab);
  }

  objectKeys(obj: any): string[] {
    return Object.keys(obj);
  }


}
