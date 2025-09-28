import { Component, HostListener, ElementRef } from '@angular/core';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { Router, ActivatedRoute } from '@angular/router';
import { TabService } from './TabService';

function logMethod(target: any, key: string, descriptor: PropertyDescriptor) {
  const originalMethod = descriptor.value;
  descriptor.value = function (...args: any[]) {
    console.log(`Classe ${target.constructor.name}: Método ${key} chamado.`);
    return originalMethod.apply(this, args);
  };
  return descriptor;
}



@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent {
  mostrarMenu: boolean = false;
  private readonly validChildRoutes = new Set(['role']);

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

    let content = { message: `Conteúdo padrão para a aba ${tabName}` };
    this.tabService.openTab(tabName);
    this.router.navigate(['/home', this.resolveChildRoute(tabName)]);
    content = this.tabService.getTabContent(tabName);
    this.tabService.openTab(tabName, content);

  }



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
    //console.log("Classe NavbarComponent: Método clickout chamado.");
    if (!this.eRef.nativeElement.contains(event.target)) {
      const submenus = this.eRef.nativeElement.querySelectorAll('.app-navbar ul li > ul');
      submenus.forEach((submenu: HTMLElement) => submenu.style.display = 'none');
    }
  }

  private resolveChildRoute(tabName: string): string {
    const normalizedTab = tabName ? tabName.toLowerCase() : '';
    return this.validChildRoutes.has(normalizedTab) ? normalizedTab : 'role';
  }
}
