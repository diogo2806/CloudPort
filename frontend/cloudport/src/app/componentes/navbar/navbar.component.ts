import { Component, ElementRef, HostListener, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ServicoAutenticacao } from '../service/servico-autenticacao/servico-autenticacao.service';
import { RegistroAba, normalizeTabId, resolveRouteSegments, TabService } from './TabService';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit, OnDestroy {
  mostrarMenu = false;
  private menuStatusSubscription?: Subscription;

  private readonly grupoConfiguracoes = 'CONFIGURACOES';
  private readonly grupoUsuarios = 'USUARIOS';
  private readonly grupoGate = 'GATE';
  private readonly grupoFerrovia = 'FERROVIA';
  private readonly grupoPatio = 'PATIO';

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
      (mostrar) => this.mostrarMenu = mostrar
    );
  }

  ngOnDestroy(): void {
    this.menuStatusSubscription?.unsubscribe();
  }

  get configurationTabs(): RegistroAba[] {
    return this.obterAbasPorGrupo(this.grupoConfiguracoes);
  }

  get userTabs(): RegistroAba[] {
    return this.obterAbasPorGrupo(this.grupoUsuarios);
  }

  get gateTabs(): RegistroAba[] {
    return this.obterAbasPorGrupo(this.grupoGate);
  }

  get ferroviaTabs(): RegistroAba[] {
    return this.obterAbasPorGrupo(this.grupoFerrovia);
  }

  get yardTabs(): RegistroAba[] {
    return this.obterAbasPorGrupo(this.grupoPatio);
  }

  openTab(tab: RegistroAba): void {
    if (tab.disabled || !this.canAccess(tab)) {
      return;
    }
    const normalizedId = normalizeTabId(tab.id);
    const registro = this.tabService.obterRegistro(tab.id) ?? tab;
    const conteudo = this.tabService.getTabContent(normalizedId) ?? {
      message: `Conteúdo padrão para a aba ${registro.label}`
    };
    this.tabService.openTab(registro, conteudo);
    const routeCommands = ['/home', ...resolveRouteSegments(normalizedId)];
    this.router.navigate(routeCommands);
  }

  handleTabSelection(event: Event, tab: RegistroAba): void {
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

  private obterAbasPorGrupo(grupo: string): RegistroAba[] {
    return this.tabService
      .obterAbasPorGrupo(grupo)
      .filter((aba) => aba.disabled || this.canAccess(aba));
  }

  private canAccess(tab: RegistroAba): boolean {
    const papeis = tab.papeisPermitidos ?? [];
    if (papeis.length === 0) {
      return true;
    }
    return this.servicoAutenticacao.possuiAlgumPapel(...papeis);
  }
}
