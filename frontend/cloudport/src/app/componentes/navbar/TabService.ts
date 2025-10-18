import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface TabItem {
  id: string;
  label: string;
  route?: string[];
  disabled?: boolean;
  comingSoonMessage?: string;
}

export const DEFAULT_TAB_ID = 'role';

export const TAB_REGISTRY: Readonly<Record<string, TabItem>> = {
  role: { id: 'role', label: 'Role', route: ['role'] },
  seguranca: { id: 'seguranca', label: 'Segurança', route: ['seguranca'] },
  notificacoes: { id: 'notificacoes', label: 'Notificações', route: ['notificacoes'] },
  privacidade: { id: 'privacidade', label: 'Privacidade', route: ['privacidade'] },
  'lista-de-usuarios': { id: 'lista-de-usuarios', label: 'Lista de usuários', route: ['lista-de-usuarios'] },
  'gate/agendamentos': { id: 'gate/agendamentos', label: 'Agendamentos do Gate', route: ['gate', 'agendamentos'] },
  'gate/janelas': { id: 'gate/janelas', label: 'Janelas de Atendimento', route: ['gate', 'janelas'] }
};

export const VALID_TAB_IDS = new Set<string>(Object.keys(TAB_REGISTRY));

export function normalizeTabId(tabId: string): string {
  const normalized = tabId?.trim().toLowerCase() ?? '';
  return VALID_TAB_IDS.has(normalized) ? normalized : DEFAULT_TAB_ID;
}

export function resolveRouteSegments(tabId: string): string[] {
  const normalized = normalizeTabId(tabId);
  const canonical = TAB_REGISTRY[normalized];
  if (canonical?.route && canonical.route.length > 0) {
    return canonical.route;
  }
  return normalized.split('/');
}

@Injectable({
  providedIn: 'root'
})
export class TabService {
  private tabsSubject = new BehaviorSubject<TabItem[]>([]);
  tabs$ = this.tabsSubject.asObservable();
  private contentSubject = new BehaviorSubject<any>(null);
  content$ = this.contentSubject.asObservable();

  private tabContents: { [tabId: string]: any } = {};

  openTab(tab: TabItem | string, content?: any): void {
    const tabToRegister = this.resolveTab(tab);
    if (tabToRegister.disabled) {
      return;
    }
    const normalizedId = normalizeTabId(tabToRegister.id);
    const registeredTab = TAB_REGISTRY[normalizedId] ?? {
      id: normalizedId,
      label: tabToRegister.label ?? normalizedId
    };
    const tabToOpen: TabItem = { ...registeredTab };
    const tabs = this.tabsSubject.value;
    if (!tabs.find(existingTab => existingTab.id === normalizedId)) {
      this.tabsSubject.next([...tabs, tabToOpen]);
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
    this.tabsSubject.next(tabs.filter(t => t.id !== normalizedId));
    delete this.tabContents[normalizedId];
  }

  getTabContent(tabId: string): any {
    return this.tabContents[normalizeTabId(tabId)];
  }

  setTabContent(tabId: string, content: any): void {
    this.tabContents[normalizeTabId(tabId)] = content;
  }

  private resolveTab(tab: TabItem | string): TabItem {
    if (typeof tab !== 'string') {
      return tab;
    }

    const normalizedId = normalizeTabId(tab);
    const registryById = TAB_REGISTRY[normalizedId];
    if (registryById) {
      return registryById;
    }

    const registryByLabel = Object.values(TAB_REGISTRY).find(
      registeredTab => registeredTab.label.toLowerCase() === tab.toLowerCase()
    );
    if (registryByLabel) {
      return registryByLabel;
    }

    return {
      id: normalizedId,
      label: tab
    };
  }
}
