import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface TabItem {
  id: string;
  label: string;
}

export const DEFAULT_TAB_ID = 'role';

export const TAB_REGISTRY: Readonly<Record<string, TabItem>> = {
  role: { id: 'role', label: 'Role' },
  seguranca: { id: 'seguranca', label: 'Segurança' },
  notificacoes: { id: 'notificacoes', label: 'Notificações' },
  privacidade: { id: 'privacidade', label: 'Privacidade' },
  'lista-de-usuarios': { id: 'lista-de-usuarios', label: 'Lista de usuários' },
  'gate/agendamentos': { id: 'gate/agendamentos', label: 'Agendamentos do Gate' },
  'gate/janelas': { id: 'gate/janelas', label: 'Janelas de Atendimento' },
  'gate/dashboard': { id: 'gate/dashboard', label: 'Dashboard do Gate' }
};

export const VALID_TAB_IDS = new Set<string>(Object.keys(TAB_REGISTRY));

export function normalizeTabId(tabId: string): string {
  const normalized = tabId?.trim().toLowerCase() ?? '';
  return VALID_TAB_IDS.has(normalized) ? normalized : DEFAULT_TAB_ID;
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

  openTab(tab: TabItem, content?: any): void {
    const normalizedId = normalizeTabId(tab.id);
    const registeredTab = TAB_REGISTRY[normalizedId] ?? {
      id: normalizedId,
      label: tab.label ?? normalizedId
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
}
