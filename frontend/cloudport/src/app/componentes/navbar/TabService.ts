import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { NavegacaoAbasService, AbaNavegacaoResposta } from '../service/navegacao/navegacao-abas.service';
import { SanitizadorConteudoService } from '../service/sanitizacao/sanitizador-conteudo.service';

export interface TabItem {
  id: string;
  label: string;
  route?: string[];
  disabled?: boolean;
  comingSoonMessage?: string;
}

export interface RegistroAba extends TabItem {
  grupo: string;
  papeisPermitidos: string[];
  padrao: boolean;
}

let registroAbasAtual: Map<string, RegistroAba> = new Map();
let idAbaPadraoAtual = '';

export function normalizeTabId(tabId: string): string {
  const normalizado = (tabId ?? '').toString().trim().toLowerCase();
  if (registroAbasAtual.has(normalizado)) {
    return normalizado;
  }
  return idAbaPadraoAtual || normalizado;
}

export function resolveRouteSegments(tabId: string): string[] {
  const normalizado = normalizeTabId(tabId);
  const registro = registroAbasAtual.get(normalizado);
  if (registro?.route && registro.route.length > 0) {
    return [...registro.route];
  }
  return normalizado.split('/').filter((segmento) => segmento.length > 0);
}

@Injectable({
  providedIn: 'root'
})
export class TabService {
  private readonly tabsSubject = new BehaviorSubject<TabItem[]>([]);
  readonly tabs$ = this.tabsSubject.asObservable();
  private readonly contentSubject = new BehaviorSubject<any>(null);
  readonly content$ = this.contentSubject.asObservable();

  private readonly registroAbasSubject = new BehaviorSubject<Map<string, RegistroAba>>(new Map());
  readonly registroAbas$ = this.registroAbasSubject.asObservable();

  private tabContents: Record<string, any> = {};
  private carregamentoAbas?: Subscription;

  constructor(
    private readonly navegacaoAbasService: NavegacaoAbasService,
    private readonly sanitizadorConteudo: SanitizadorConteudoService
  ) {
    this.recarregarAbas();
  }

  recarregarAbas(): void {
    this.carregamentoAbas?.unsubscribe();
    let requisicao: Observable<AbaNavegacaoResposta[]>;
    try {
      requisicao = this.navegacaoAbasService.listarAbas();
    } catch (erro) {
      this.processarErroCarregamento();
      return;
    }

    this.carregamentoAbas = requisicao.subscribe({
      next: (abas) => this.processarRegistroAbas(abas),
      error: () => this.processarErroCarregamento()
    });
  }

  obterRegistro(tabId: string): RegistroAba | undefined {
    return registroAbasAtual.get(normalizeTabId(tabId));
  }

  obterAbasPorGrupo(grupo: string): RegistroAba[] {
    const grupoNormalizado = this.normalizarGrupo(grupo);
    return Array.from(registroAbasAtual.values())
      .filter((aba) => aba.grupo === grupoNormalizado)
      .sort((a, b) => a.label.localeCompare(b.label, 'pt-BR', { sensitivity: 'base' }));
  }

  obterIdPadrao(): string {
    return idAbaPadraoAtual;
  }

  openTab(tab: TabItem | string, content?: any): void {
    const registro = this.resolverRegistro(tab);
    if (registro.disabled) {
      return;
    }
    const normalizedId = normalizeTabId(registro.id);
    const tabParaAbrir: TabItem = {
      id: normalizedId,
      label: registro.label,
      route: [...(registro.route ?? [])],
      disabled: registro.disabled,
      comingSoonMessage: registro.comingSoonMessage
    };
    const tabs = this.tabsSubject.value;
    if (!tabs.find((existingTab) => existingTab.id === normalizedId)) {
      this.tabsSubject.next([...tabs, tabParaAbrir]);
    }
    if (content !== undefined) {
      this.tabContents[normalizedId] = content;
    }
  }

  setContent(content: any): void {
    this.contentSubject.next(content);
  }

  closeTab(tabId: string): void {
    const normalizedId = normalizeTabId(tabId);
    const tabs = this.tabsSubject.value;
    this.tabsSubject.next(tabs.filter((t) => t.id !== normalizedId));
    delete this.tabContents[normalizedId];
  }

  getTabContent(tabId: string): any {
    return this.tabContents[normalizeTabId(tabId)];
  }

  setTabContent(tabId: string, content: any): void {
    this.tabContents[normalizeTabId(tabId)] = content;
  }

  private processarRegistroAbas(abas: AbaNavegacaoResposta[]): void {
    const novoRegistro = new Map<string, RegistroAba>();
    let idPadraoLocal = '';

    (abas ?? []).forEach((aba) => {
      const registro = this.criarRegistro(aba);
      if (!registro.id) {
        return;
      }
      novoRegistro.set(registro.id, registro);
      if (registro.padrao && !idPadraoLocal) {
        idPadraoLocal = registro.id;
      }
    });

    if (!idPadraoLocal && novoRegistro.size > 0) {
      const primeiro = novoRegistro.keys().next();
      idPadraoLocal = primeiro.value ?? '';
    }

    registroAbasAtual = novoRegistro;
    idAbaPadraoAtual = idPadraoLocal;
    this.registroAbasSubject.next(new Map(registroAbasAtual));
  }

  private processarErroCarregamento(): void {
    if (registroAbasAtual.size === 0) {
      registroAbasAtual = new Map();
      idAbaPadraoAtual = '';
      this.registroAbasSubject.next(new Map());
    }
  }

  private resolverRegistro(tab: TabItem | string): RegistroAba {
    if (typeof tab === 'string') {
      const idNormalizado = normalizeTabId(tab);
      return registroAbasAtual.get(idNormalizado) ?? this.criarRegistroPlaceholder(idNormalizado);
    }

    const idNormalizado = normalizeTabId(tab.id);
    return registroAbasAtual.get(idNormalizado) ?? {
      ...this.criarRegistroPlaceholder(idNormalizado),
      label: this.normalizarRotulo(tab.label, idNormalizado),
      route: this.normalizarSegmentos(tab.route ?? []),
      disabled: tab.disabled ?? false,
      comingSoonMessage: tab.comingSoonMessage
        ? this.sanitizadorConteudo.sanitizar(tab.comingSoonMessage)
        : undefined
    };
  }

  private criarRegistro(aba: AbaNavegacaoResposta): RegistroAba {
    const idNormalizado = this.normalizarId(aba.identificador || aba.id);
    const rotuloNormalizado = this.normalizarRotulo(aba.rotulo, idNormalizado);
    const rotaNormalizada = this.normalizarSegmentos(aba.rota);
    const rotaFinal = rotaNormalizada.length > 0 ? rotaNormalizada : this.normalizarSegmentos(idNormalizado);
    const mensagemNormalizada = aba.mensagemEmBreve
      ? this.sanitizadorConteudo.sanitizar(aba.mensagemEmBreve)
      : '';

    return {
      id: idNormalizado,
      label: rotuloNormalizado,
      route: rotaFinal,
      disabled: !!aba.desabilitado,
      comingSoonMessage: mensagemNormalizada ? mensagemNormalizada : undefined,
      grupo: this.normalizarGrupo(aba.grupo),
      papeisPermitidos: this.normalizarPapeis(aba.rolesPermitidos),
      padrao: !!aba.padrao
    };
  }

  private criarRegistroPlaceholder(id: string): RegistroAba {
    const rota = this.normalizarSegmentos(id);
    return {
      id,
      label: this.criarRotuloPlaceholder(id),
      route: rota.length > 0 ? rota : [id],
      disabled: true,
      comingSoonMessage: 'Em breve',
      grupo: 'OUTROS',
      papeisPermitidos: [],
      padrao: false
    };
  }

  private normalizarId(valor: string | undefined | null): string {
    const texto = this.sanitizadorConteudo.sanitizar(valor ?? '');
    if (!texto) {
      return '';
    }
    return texto
      .normalize('NFKC')
      .toLowerCase()
      .replace(/[^a-z0-9/\-]+/g, '-')
      .replace(/-{2,}/g, '-')
      .replace(/^-+|-+$/g, '');
  }

  private normalizarRotulo(rotulo: string | undefined | null, fallbackId: string): string {
    const rotuloSanitizado = this.sanitizadorConteudo.sanitizar(rotulo ?? '');
    if (rotuloSanitizado) {
      return rotuloSanitizado;
    }
    return this.criarRotuloPlaceholder(fallbackId);
  }

  private normalizarSegmentos(rota: string[] | string | undefined | null): string[] {
    const origem = Array.isArray(rota)
      ? rota
      : typeof rota === 'string'
        ? rota.split('/')
        : [];
    return origem
      .map((segmento) => this.normalizarId(segmento).replace(/\//g, ''))
      .filter((segmento) => segmento.length > 0);
  }

  private normalizarGrupo(grupo: string | undefined | null): string {
    const texto = this.sanitizadorConteudo.sanitizar(grupo ?? '');
    if (!texto) {
      return 'OUTROS';
    }
    return texto
      .normalize('NFKD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^A-Z0-9]/gi, '')
      .toUpperCase();
  }

  private normalizarPapeis(papeis: string[] | undefined | null): string[] {
    if (!Array.isArray(papeis)) {
      return [];
    }
    return papeis
      .map((papel) => this.sanitizadorConteudo.sanitizar(papel ?? ''))
      .map((papel) => papel
        .normalize('NFKD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[^A-Z0-9_]/gi, '')
        .toUpperCase())
      .filter((papel) => papel.length > 0);
  }

  private criarRotuloPlaceholder(valor: string): string {
    const texto = this.sanitizadorConteudo.sanitizar(valor);
    const partes = texto
      .split(/[\\/\-]/)
      .map((parte) => parte.trim())
      .filter((parte) => parte.length > 0)
      .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1));
    if (partes.length === 0) {
      return 'Recurso';
    }
    return partes.join(' ');
  }
}
