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
  filteredData: any[] = [];  // Adicione esta linha
  data: any[] = [];  // Adicione esta linha


  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authenticationService: AuthenticationService,
    private tabService: TabService,
    private tabStateService: TabStateService
  ) {
    let currentUser: any = this.authenticationService.currentUserValue;
    if (currentUser && currentUser.token) {
      this.userToken = currentUser.token;
    }
  }

  ngOnInit() {
    this.tabService.tabs$.subscribe(tabs => {
      this.tabs = tabs;
      if (tabs.length > 0) {
        this.selectedTab = tabs[tabs.length - 1];
      }
    });
  }

  logout() {
    this.authenticationService.logout();
    this.router.navigate(['login']);
  }

  Alert() {
    alert(this.authenticationService.currentUserValue?.token);
  }

  /*
  closeTab(tab: string) {
    this.tabService.closeTab(tab);
    this.tabService.tabs$.subscribe(tabs => {
      this.tabs = tabs;
      if (tabs.length > 0) {
        this.selectedTab = tabs[tabs.length - 1];
        this.router.navigate(['/home', this.selectedTab.toLowerCase()]);
      }
    });
  }
*/

closeTab(tab: string) {
  const tabIndex = this.tabs.indexOf(tab);
  this.tabService.closeTab(tab);
  this.tabService.tabs$.subscribe(tabs => {
      this.tabs = tabs;
      if (tabs.length > 0) {
          if (tabIndex > 0) {
              this.selectedTab = tabs[tabIndex - 1];
          } else {
              this.selectedTab = tabs[0];
          }
          this.router.navigate(['/home', this.selectedTab.toLowerCase()]);
      }
  });
}
/*
navigateTo(tab: string) {
  this.selectedTab = tab;
}
*/


navigateTo(tab: string) {
  this.selectedTab = tab;
  const tabState = this.tabStateService.getTabState(tab);
  if (tabState) {
    // Restaure o estado da aba aqui
    console.log(tabState);
    this.data = tabState.filteredData;
  } else {
    this.router.navigate(['/home', tab.toLowerCase()]);
    // Aqui, você pode fazer qualquer lógica necessária para preparar os dados para essa aba.
    // Como um exemplo, vou filtrar os dados onde algum campo (por exemplo, 'name') contém a string 'example'.
    const currentFilteredData = this.data.filter(item => item.name.includes('example'));

    // Armazene o estado da aba aqui
    this.tabStateService.setTabState(tab, { filteredData: currentFilteredData });
    this.data = currentFilteredData;
  }
}





  /*
  navigateTo(tab: string) {
    this.selectedTab = tab;
    this.router.navigate(['/home', tab.toLowerCase()]);
  }
  */
}
