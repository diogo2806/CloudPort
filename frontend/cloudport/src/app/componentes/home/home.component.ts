import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Router, RouteReuseStrategy, RouterOutlet } from '@angular/router';
import { ServicoAutenticacao } from '../service/servico-autenticacao/servico-autenticacao.service';
import { TabItem, TabService, normalizeTabId, resolveRouteSegments, RegistroAba } from '../navbar/TabService';
import { CustomReuseStrategy } from '../tab-content/customreusestrategy';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit, OnDestroy {

  @ViewChild('outlet', { read: RouterOutlet }) outlet!: RouterOutlet;
  userToken: string = '';
  tabs: TabItem[] = [];
  selectedTabId = '';
  filteredData: any[] = [];
  data: { [key: string]: any } = {};
  tabContent: { [key: string]: any } = {};
  private tabSubscription?: Subscription;
  private registroSubscription?: Subscription;
  private defaultChildRoute = '';
  private defaultTabOpened = false;

  constructor(
    private router: Router,
    private servicoAutenticacao: ServicoAutenticacao,
    private tabService: TabService,
    private reuseStrategy: RouteReuseStrategy
  ) {
    console.log('Classe HomeComponent: Método construtor chamado.');
    const usuarioAtual: any = this.servicoAutenticacao.obterUsuarioAtual();
    if (usuarioAtual && usuarioAtual.token) {
      this.userToken = usuarioAtual.token;
    }
  }

  ngOnInit() {
    console.log('Classe HomeComponent: Método ngOnInit iniciado.');
    this.registroSubscription = this.tabService.registroAbas$.subscribe(() => {
      const idPadrao = this.tabService.obterIdPadrao();
      if (idPadrao) {
        this.defaultChildRoute = idPadrao;
        if (!this.defaultTabOpened && this.tabs.length === 0) {
          this.openDefaultTab();
        }
      }
    });

    this.tabSubscription = this.tabService.tabs$.subscribe(tabs => {
      this.tabs = tabs;
      if (tabs.length === 0) {
        const rotaPadrao = this.defaultChildRoute || this.tabService.obterIdPadrao();
        if (!rotaPadrao) {
          return;
        }
        this.selectedTabId = rotaPadrao;
        if (!this.defaultTabOpened) {
          this.openDefaultTab();
        }
        return;
      }
      const lastTab = tabs[tabs.length - 1];
      const route = this.resolveChildRoute(lastTab.id);
      this.selectedTabId = route;
      this.tabContent[route] = this.tabService.getTabContent(route);
      this.navigateToChild(route);
    });
    (this.reuseStrategy as CustomReuseStrategy).markForDestruction('login'.toLowerCase());
  }

  ngOnDestroy(): void {
    this.tabSubscription?.unsubscribe();
    this.registroSubscription?.unsubscribe();
  }

  navigateTo(tabId: string) {
    const route = this.resolveChildRoute(tabId);
    this.selectedTabId = route;
    this.tabContent[route] = this.tabService.getTabContent(route);
    this.navigateToChild(route);
  }

  logout() {
    console.log('Classe HomeComponent: Método logout chamado.');
    this.servicoAutenticacao.encerrarSessao();
    (this.reuseStrategy as CustomReuseStrategy).markForDestruction('login'.toLowerCase());
    this.router.navigate(['login']);
  }

  Alert() {
    console.log('Classe HomeComponent: Método Alert chamado.');
    alert(this.servicoAutenticacao.obterUsuarioAtual()?.token);
  }

  closeTab(tabId: string) {
    console.log(`Classe HomeComponent: Método closeTab chamado com o parâmetro clearHandlers=${tabId}.`);
    const route = this.resolveChildRoute(tabId);
    (this.reuseStrategy as CustomReuseStrategy).markForDestruction(route);
    console.log(`Classe HomeComponent: Método closeTab chamado com o parâmetro tab=${tabId}.`);
    this.tabService.closeTab(route);
    delete this.tabContent[route];
  }

  objectKeys(obj: any): string[] {
    return Object.keys(obj);
  }

  private resolveChildRoute(tabId: string): string {
    return normalizeTabId(tabId);
  }

  private openDefaultTab(): void {
    const idPadrao = this.defaultChildRoute || this.tabService.obterIdPadrao();
    if (!idPadrao) {
      return;
    }
    const registroPadrao = this.tabService.obterRegistro(idPadrao);
    if (registroPadrao && !this.possuiPermissaoAba(registroPadrao)) {
      return;
    }
    if (registroPadrao?.disabled) {
      return;
    }
    const rotuloPadrao = registroPadrao?.label ?? 'Aba inicial';
    const conteudoInicial = this.tabService.getTabContent(idPadrao) ?? {
      message: `Conteúdo padrão para a aba ${rotuloPadrao}`
    };
    this.tabService.openTab(registroPadrao ?? idPadrao, conteudoInicial);
    this.tabService.setTabContent(idPadrao, conteudoInicial);
    this.tabContent[idPadrao] = conteudoInicial;
    this.selectedTabId = idPadrao;
    this.defaultTabOpened = true;
    this.navigateToChild(idPadrao);
  }

  private navigateToChild(route: string): void {
    const commands = ['/home', ...resolveRouteSegments(route)];
    this.router.navigate(commands);
  }

  private possuiPermissaoAba(aba: RegistroAba): boolean {
    const papeis = aba.papeisPermitidos ?? [];
    if (papeis.length === 0) {
      return true;
    }
    return this.servicoAutenticacao.possuiAlgumPapel(...papeis);
  }
}
