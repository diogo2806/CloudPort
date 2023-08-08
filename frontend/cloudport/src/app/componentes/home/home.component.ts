import { Component, OnInit } from '@angular/core';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { Router, ActivatedRoute } from '@angular/router';
import { TabService } from '../navbar/TabService';
import { TabStateService } from '../dynamic-table/tab-state.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  userToken: string = '';
  tabs: string[] = [];
  selectedTab = '';
  filteredData: any[] = [];
  data: any[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authenticationService: AuthenticationService,
    private tabService: TabService,
    private tabStateService: TabStateService
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
    console.log("Classe HomeComponent: Método ngOnInit finalizado.");
  }

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

  navigateTo(tabName: string) {
    console.log(`Classe HomeComponent: Método navigateTo chamado com o parâmetro tab=${tabName}.`);
    const content = this.tabService.getTabContent(tabName);
    this.tabService.setTabContent(tabName, content);
  }

  openTab(tabName: string) {
    console.log(`Classe HomeComponent: Método openTab chamado com o parâmetro tabName=${tabName}.`);
    this.tabService.openTab(tabName);
  }



}
