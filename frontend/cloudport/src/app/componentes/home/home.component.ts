import { Component, ViewChild, AfterViewInit, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router'; // Importação correta para RouterOutlet
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { Router, ActivatedRoute } from '@angular/router';
import { TabService } from '../navbar/TabService';
import { TabStateService } from '../dynamic-table/tab-state.service';
import { ChangeDetectorRef } from '@angular/core';
import { TabContentComponent } from '../tab-content/tab-content.component';

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
    private route: ActivatedRoute,
    private router: Router,
    private authenticationService: AuthenticationService,
    private tabService: TabService,
    private tabStateService: TabStateService,
    private cdr: ChangeDetectorRef
  ) {
    console.log("Classe HomeComponent: Método construtor chamado.");
    let currentUser: any = this.authenticationService.currentUserValue;
    if (currentUser && currentUser.token) {
      this.userToken = currentUser.token;
    }
  }

  
  ngAfterViewInit() {
    // Capturar o conteúdo renderizado pelo <router-outlet>
    const content = this.outlet.component;
    this.tabService.setContent(content);
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
    const content = this.outlet.component;
    this.tabService.setContent(content);
    console.log("Classe HomeComponent: Método ngOnInit finalizado.");
  }

  
  navigateTo(tabName: string) {
    this.selectedTab = tabName;
    this.tabContent = this.tabService.getTabContent(tabName);
    this.router.navigate(['/home', tabName.toLowerCase()]);
  }

  /*
  ngOnInit() {
    console.log("Classe HomeComponent: Método ngOnInit iniciado.");
    this.tabService.tabs$.subscribe(tabs => {
      this.tabs = tabs;
      if (tabs.length > 0) {
        this.selectedTab = tabs[tabs.length - 1];
        this.router.navigate(['/home', this.selectedTab.toLowerCase()]);
      }
    });
    console.log("Classe HomeComponent: Método ngOnInit finalizado.");
  }
*/
  logout() {
    console.log("Classe HomeComponent: Método logout chamado.");
    this.authenticationService.logout();
    this.router.navigate(['login']);
  }

  Alert() {
    console.log("Classe HomeComponent: Método Alert chamado.");
    alert(this.authenticationService.currentUserValue?.token);
  }

  closeTab(tab: string) {
    console.log(`Classe HomeComponent: Método closeTab chamado com o parâmetro tab=${tab}.`);
    this.tabService.closeTab(tab);
  }

  /*
  navigateTo(tabName: string) {
    console.log(`Classe HomeComponent: Método navigateTo chamado com o parâmetro tab=${tabName}.`);
    this.selectedTab = tabName;
    this.router.navigate(['/home', tabName.toLowerCase()]);
  }
  */

/*
  navigateTo(tabName: string) {
    console.log(`Classe HomeComponent: Método navigateTo chamado com o parâmetro tab=${tabName}.`);
    
    // Obter o conteúdo da aba
    const tabContent = this.tabService.getTabContent(tabName);
    console.log(`Classe HomeComponent: Conteúdo obtido para a aba ${tabName}:`, tabContent);
    
    if (tabContent !== undefined) {
        this.data = tabContent;
        console.log(`Classe HomeComponent: Atualizando a aba selecionada para: ${tabName}`);
        this.selectedTab = tabName;
        this.cdr.markForCheck(); // Marque o componente para verificação de mudanças
        console.log(`Classe HomeComponent: A aba selecionada foi atualizada para: ${this.selectedTab}`);
    } else {
        console.error(`Classe HomeComponent Erro: Não foi possível obter o conteúdo da aba ${tabName}.`);
    }
}

*/

objectKeys(obj: any): string[] {
  return Object.keys(obj);
}


}
