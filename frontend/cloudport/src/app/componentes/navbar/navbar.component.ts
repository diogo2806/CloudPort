import { Component, ElementRef, HostListener, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { TabService } from '../service/tab.service';

function logMethod(target: any, key: string, descriptor: PropertyDescriptor) {
  const originalMethod = descriptor.value;
  descriptor.value = function (...args: any[]) {
    console.log(`Classe ${target.constructor.name}: Método ${key} chamado.`);
    return originalMethod.apply(this, args);
  };
  return descriptor;
}

interface NavbarChildItem {
  label: string;
  tab: string;
  roles?: string[];
}

interface NavbarItem {
  label: string;
  roles?: string[];
  children: NavbarChildItem[];
}

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit, OnDestroy {
  mostrarMenu: boolean = false;
  private readonly defaultChildRoute = 'role';
  private readonly validChildRoutes = new Set(['role', 'login']);
  private menuStatusSubscription?: Subscription;

  menuItems: NavbarItem[] = [
    {
      label: 'Configurações',
      roles: ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR', 'ROLE_OPERADOR_GATE'],
      children: [
        { label: 'Geral', tab: 'Role', roles: ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR'] },
        { label: 'Perfil', tab: 'Role', roles: ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR', 'ROLE_OPERADOR_GATE'] },
        { label: 'Segurança', tab: 'login', roles: ['ROLE_ADMIN_PORTO'] },
        { label: 'Notificações', tab: 'login', roles: ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR'] },
        { label: 'Privacidade', tab: 'login', roles: ['ROLE_ADMIN_PORTO'] }
      ]
    },
    {
      label: 'Usuários',
      roles: ['ROLE_ADMIN_PORTO'],
      children: [
        { label: 'Lista de usuários', tab: 'login', roles: ['ROLE_ADMIN_PORTO'] },
        { label: 'Adicionar usuário', tab: 'Role', roles: ['ROLE_ADMIN_PORTO'] },
        { label: 'Gerenciar usuários', tab: 'Role', roles: ['ROLE_ADMIN_PORTO'] },
        { label: 'Role', tab: 'Role', roles: ['ROLE_ADMIN_PORTO'] }
      ]
    },
    {
      label: 'Transportadora',
      roles: ['ROLE_TRANSPORTADORA'],
      children: [
        { label: 'Meus agendamentos', tab: 'Role', roles: ['ROLE_TRANSPORTADORA'] }
      ]
    }
  ];

  constructor(
      private authenticationService: AuthenticationService,
      private router: Router,
      private tabService: TabService,
      private eRef: ElementRef
  ) {
    console.log('Classe NavbarComponent: Método construtor chamado.');
  }

  ngOnInit(): void {
      console.log('Classe NavbarComponent: Método ngOnInit iniciado.');
      this.menuStatusSubscription = this.authenticationService.currentMenuStatus.subscribe(
          mostrar => this.mostrarMenu = mostrar
      );
      console.log('Classe NavbarComponent: Método ngOnInit finalizado.');
  }

  ngOnDestroy(): void {
      this.menuStatusSubscription?.unsubscribe();
  }

  @logMethod
  openTab(tabName: string) {
    if (!tabName) {
      return;
    }
    let content = { message: `Conteúdo padrão para a aba ${tabName}` };
    this.tabService.openTab(tabName);
    this.router.navigate(['/home', this.resolveChildRoute(tabName)]);
    content = this.tabService.getTabContent(tabName);
    this.tabService.openTab(tabName, content);
  }

  @logMethod
  toggleSubmenu(event: Event) {
    console.log('Classe NavbarComponent: Método toggleSubmenu chamado.');
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
    if (!this.eRef.nativeElement.contains(event.target)) {
      const submenus = this.eRef.nativeElement.querySelectorAll('.app-navbar ul li > ul');
      submenus.forEach((submenu: HTMLElement) => submenu.style.display = 'none');
    }
  }

  canDisplay(roles?: string[]): boolean {
    if (!roles || roles.length === 0) {
      return true;
    }
    return this.authenticationService.hasAnyRole(...roles);
  }

  private resolveChildRoute(tabName: string): string {
    const normalizedTab = tabName ? tabName.toLowerCase() : '';
    return this.validChildRoutes.has(normalizedTab) ? normalizedTab : this.defaultChildRoute;
  }
}
