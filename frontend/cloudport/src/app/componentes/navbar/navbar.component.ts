import { Component, ElementRef, HostListener, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ServicoAutenticacao } from '../service/servico-autenticacao/servico-autenticacao.service';
import { TabItem, TAB_REGISTRY, normalizeTabId, resolveRouteSegments, TabService } from './TabService';

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
  private readonly gateTabIds = [
    'gate/dashboard',
    'gate/agendamentos',
    'gate/janelas',
    'gate/relatorios',
    'gate/operador/console',
    'gate/operador/eventos'
  ];
  private readonly yardTabIds = ['patio/mapa', 'patio/posicoes', 'patio/movimentacoes', 'patio/movimentacao'];

  private readonly tabRoles: Record<string, string[]> = {
    role: ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR'],
    seguranca: ['ROLE_ADMIN_PORTO'],
    notificacoes: ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR'],
    privacidade: ['ROLE_ADMIN_PORTO'],
    'lista-de-usuarios': ['ROLE_ADMIN_PORTO'],
    'gate/dashboard': ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR', 'ROLE_OPERADOR_GATE'],
    'gate/agendamentos': ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR', 'ROLE_OPERADOR_GATE'],
    'gate/janelas': ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR', 'ROLE_OPERADOR_GATE'],
    'gate/relatorios': ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR', 'ROLE_OPERADOR_GATE'],
    'gate/operador/console': ['ROLE_ADMIN_PORTO', 'ROLE_OPERADOR_GATE', 'ROLE_PLANEJADOR'],
    'gate/operador/eventos': ['ROLE_ADMIN_PORTO', 'ROLE_OPERADOR_GATE', 'ROLE_PLANEJADOR'],
    'patio/mapa': ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR'],
    'patio/posicoes': ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR'],
    'patio/movimentacoes': ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR'],
    'patio/movimentacao': ['ROLE_ADMIN_PORTO', 'ROLE_PLANEJADOR']
  };

  constructor(
    private readonly servicoAutenticacao: ServicoAutenticacao,
    private readonly router: Router,
    private readonly tabService: TabService,
    private readonly eRef: ElementRef
  ) {
    this.mostrarMenu = this.servicoAutenticacao.obterStatusMenuAtual();
  }

  ngOnInit(): void {
    this.menuStatusSubscription = this.servicoAutenticacao.statusMenuObservavel.subscribe(
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

  get yardTabs(): TabItem[] {
    return this.resolveTabs(this.yardTabIds);
  }

  openTab(tab: TabItem): void {
    if (tab.disabled) {
      return;
    }
    const normalizedId = normalizeTabId(tab.id);
    if (!this.canAccess(normalizedId)) {
      return;
    }
    const canonicalTab = TAB_REGISTRY[normalizedId] ?? tab;
    const content = this.tabService.getTabContent(normalizedId) ?? {
      message: `Conteúdo padrão para a aba ${canonicalTab.label}`
    };
    this.tabService.openTab(canonicalTab, content);
    const routeCommands = ['/home', ...resolveRouteSegments(normalizedId)];
    this.router.navigate(routeCommands);
  }

  handleTabSelection(event: Event, tab: TabItem): void {
    event.preventDefault();
    event.stopPropagation();
    if (tab.disabled) {
      return;
    }
    this.openTab(tab);
  }

  toggleSubmenu(event: Event): void {
    event.preventDefault();
    event.stopPropagation();

    const host = event.currentTarget as HTMLElement;
    const submenu = host.querySelector('ul');

    if (submenu instanceof HTMLElement) {
      const isHidden = submenu.style.display === 'none' || submenu.style.display === '';
      submenu.style.display = isHidden ? 'block' : 'none';
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
      .map(id => this.resolveTabById(id))
      .filter((tab): tab is TabItem => !!tab)
      .filter(tab => tab.disabled || this.canAccess(tab.id));
  }

  private resolveTabById(id: string): TabItem | undefined {
    const registered = TAB_REGISTRY[id];
    if (registered) {
      return registered;
    }
    return {
      id,
      label: this.formatPlaceholderLabel(id),
      disabled: true,
      comingSoonMessage: 'Em breve'
    };
  }

  private formatPlaceholderLabel(value: string): string {
    return value
      .split(/[\\/\-]/)
      .filter(segment => segment.trim().length > 0)
      .map(segment => segment.charAt(0).toUpperCase() + segment.slice(1))
      .join(' ');
  }

  private canAccess(tabId: string): boolean {
    const normalized = normalizeTabId(tabId);
    const roles = this.tabRoles[normalized];
    if (!roles || roles.length === 0) {
      return true;
    }
    return this.servicoAutenticacao.possuiAlgumPapel(...roles);
  }
}
