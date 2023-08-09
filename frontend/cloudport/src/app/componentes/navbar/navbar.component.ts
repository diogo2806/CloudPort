import { Component, HostListener, ElementRef } from '@angular/core';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { Router, ActivatedRoute } from '@angular/router';
import { TabService } from './TabService';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent {
  mostrarMenu: boolean = false;

  constructor(
      private authenticationService: AuthenticationService,
      private router: Router,
      private tabService: TabService,
      private eRef: ElementRef
  ) {
    console.log("Classe NavbarComponent: Método construtor chamado.");
  }

  ngOnInit(): void {
      console.log("Classe NavbarComponent: Método ngOnInit iniciado.");
      this.authenticationService.currentMenuStatus.subscribe(
          mostrar => this.mostrarMenu = mostrar
      );

      this.mostrarMenu = true;
      console.log("Classe NavbarComponent: Método ngOnInit finalizado.");
  }
// ... (resto do código)

openTab(tabName: string) {
  console.log(`Classe NavbarComponent: Método openTab chamado com o parâmetro tabName=${tabName}.`);
  
 // let tabContent = this.tabService.getTabContent(tabName);
  
  // Se o conteúdo da aba não existir, defina um conteúdo padrão
  //if (!tabContent) {
  //  tabContent = { message: `Conteúdo padrão para a aba ${tabName}` };
 // }
  
  this.tabService.openTab(tabName);
  this.router.navigate(['/home' , tabName.toLowerCase()]);

}


// ... (resto do código)

  /*
  openTab(tabName: string) {
    console.log(`Classe NavbarComponent: Método openTab chamado com o parâmetro tabName=${tabName}.`);
    this.tabService.openTab(tabName);
    this.router.navigate(['/home' , tabName.toLowerCase()]);
    const content = this.tabService.getTabContent(tabName);
    this.tabService.setTabContent(tabName, content);
  }

/*
  navigateTo(tab: string) {
    console.log(`Classe NavbarComponent: Método navigateTo chamado com o parâmetro tab=${tab}.`);
    this.tabService.openTab(tab);
    this.router.navigate(['/home' , tab.toLowerCase()]);
  }
  
*/

  toggleSubmenu(event: Event) {
    console.log("Classe NavbarComponent: Método toggleSubmenu chamado.");
    event.preventDefault();
    event.stopPropagation();
  
    const target = event.target as HTMLElement;
    const submenu = target.nextElementSibling as HTMLElement;
  
    if (submenu) {
      if (submenu.style.display === 'none' || submenu.style.display === '') {
        submenu.style.display = 'block';
      } else {
        submenu.style.display = 'none';
      }
    }
  }
  
  @HostListener('document:click', ['$event'])
  clickout(event: MouseEvent) {
    console.log("Classe NavbarComponent: Método clickout chamado.");
    if (!this.eRef.nativeElement.contains(event.target)) {
      const submenus = this.eRef.nativeElement.querySelectorAll('.app-navbar ul li > ul');
      submenus.forEach((submenu: HTMLElement) => submenu.style.display = 'none');
    }
  }
}
