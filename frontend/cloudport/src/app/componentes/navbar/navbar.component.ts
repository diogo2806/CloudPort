import { Component, HostListener, ElementRef, OnDestroy, OnInit } from '@angular/core';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { Router } from '@angular/router';
import { TabItem, TabService } from './TabService';
import { Subscription } from 'rxjs';

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
export class NavbarComponent implements OnInit, OnDestroy {
  mostrarMenu: boolean = false;
  private readonly defaultChildRoute = 'role';
  private readonly validChildRoutes = new Set([
    this.defaultChildRoute,
    'seguranca',
    'notificacoes',
    'privacidade',
    'lista-de-usuarios'
  ]);
  readonly tabs: Record<string, TabItem> = {
    role: { id: 'role', label: 'Role' },
    seguranca: { id: 'seguranca', label: 'Segurança' },
    notificacoes: { id: 'notificacoes', label: 'Notificações' },
    privacidade: { id: 'privacidade', label: 'Privacidade' },
    listaDeUsuarios: { id: 'lista-de-usuarios', label: 'Lista de usuários' }
  };
  private menuStatusSubscription?: Subscription;

  constructor(
    private authenticationService: AuthenticationService,
    private router: Router,
    private tabService: TabService,
    private eRef: ElementRef
  ) {
    console.log('Classe NavbarComponent: Método construtor chamado.');
    this.mostrarMenu = this.authenticationService.getMenuStatusValue();
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

  openTab(tab: TabItem) {
    const route = this.resolveChildRoute(tab.id);
    const content = this.tabService.getTabContent(tab.id) ?? { message: `Conteúdo padrão para a aba ${tab.label}` };
    this.tabService.openTab(tab, content);
    this.router.navigate(['/home', route]);
  }

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

  private resolveChildRoute(tabId: string): string {
    const normalizedTab = tabId?.toLowerCase() ?? '';
    return this.validChildRoutes.has(normalizedTab) ? normalizedTab : this.defaultChildRoute;
  }
}
