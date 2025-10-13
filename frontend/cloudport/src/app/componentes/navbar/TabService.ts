import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface TabItem {
  id: string;
  label: string;
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

  openTab(tab: TabItem, content?: any) {
    const tabs = this.tabsSubject.value;
    if (!tabs.find(existingTab => existingTab.id === tab.id)) {
      this.tabsSubject.next([...tabs, tab]);
    }
    if (content) {
      this.tabContents[tab.id] = content;
    }
  }

  setContent(content: any) {
    this.contentSubject.next(content);
  }

  closeTab(tabId: string) {
    const tabs = this.tabsSubject.value;
    this.tabsSubject.next(tabs.filter(t => t.id !== tabId));
    delete this.tabContents[tabId];
  }

  getTabContent(tabId: string): any {
    return this.tabContents[tabId];
  }

  setTabContent(tabId: string, content: any) {
    this.tabContents[tabId] = content;
  }
}
