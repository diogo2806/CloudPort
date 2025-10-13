import { Component, ElementRef, HostListener, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { TabItem, TAB_REGISTRY, normalizeTabId, TabService } from './TabService';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit, OnDestroy {
  mostrarMenu = false;
  private menuStatusSubscription?: Subscription;

  private readonly configurationTabIds = ['role', 'seguranca', 'notificacoes', 'privacidade'];
  private readonly userTabIds = ['lista-de-usuarios'];
  private readonly gateTabIds = ['gate/agendamentos', 'gate/janelas', 'gate/dashboard'];

  private readonly tabRoles: Record<string, string[]> = {
    role: ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR'],
    seguranca: ['ROLE_ADMIN_PORTO'],
    notificacoes: ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR'],
    privacidade: ['ROLE_ADMIN_PORTO'],
    'lista-de-usuarios': ['ROLE_ADMIN_PORTO'],
    'gate/agendamentos': ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR', 'ROLE_OPERADOR_GATE'],
    'gate/janelas': ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR', 'ROLE_OPERADOR_GATE'],
    'gate/dashboard': ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR', 'ROLE_OPERADOR_GATE']
  };

  constructor(
    private readonly authenticationService: AuthenticationService,
    private readonly router: Router,
    private readonly tabService: TabService,
    private readonly eRef: ElementRef
  ) {
    this.mostrarMenu = this.authenticationService.getMenuStatusValue();
  }

  ngOnInit(): void {
    this.menuStatusSubscription = this.authenticationService.currentMenuStatus.subscribe(
      mostrar => this.mostrarMenu = mostrar
    );
  }

  ngOnDestroy(): void {
    this.menuStatusSubscription?.unsubscribe();
  }

  get configurationTabs(): TabItem[] {
    return this.resolveTabs(this.configurationTabIds);
  }

  get userTabs(): TabItem[] {
    return this.resolveTabs(this.userTabIds);
  }

  get gateTabs(): TabItem[] {
    return this.resolveTabs(this.gateTabIds);
  }

  openTab(tab: TabItem): void {
    const normalizedId = normalizeTabId(tab.id);
    if (!this.canAccess(normalizedId)) {
      return;
    }
    const canonicalTab = TAB_REGISTRY[normalizedId] ?? tab;
    const content = this.tabService.getTabContent(normalizedId) ?? {
      message: `Conteúdo padrão para a aba ${canonicalTab.label}`
    };
    this.tabService.openTab(canonicalTab, content);
    const routeCommands = ['/home', ...normalizedId.split('/')];
    this.router.navigate(routeCommands);
  }

  toggleSubmenu(event: Event): void {
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
  clickout(event: MouseEvent): void {
    if (!this.eRef.nativeElement.contains(event.target)) {
      const submenus = this.eRef.nativeElement.querySelectorAll('.app-navbar ul li > ul');
      submenus.forEach((submenu: HTMLElement) => submenu.style.display = 'none');
    }
  }

  private resolveTabs(ids: string[]): TabItem[] {
    return ids
      .map(id => TAB_REGISTRY[id])
      .filter((tab): tab is TabItem => !!tab)
      .filter(tab => this.canAccess(tab.id));
  }

  private canAccess(tabId: string): boolean {
    const normalized = normalizeTabId(tabId);
    const roles = this.tabRoles[normalized];
    if (!roles || roles.length === 0) {
      return true;
    }
    return this.authenticationService.hasAnyRole(...roles);
  }
}
