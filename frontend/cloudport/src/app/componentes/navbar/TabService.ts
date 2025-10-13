import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TabService {
  private tabsSubject = new BehaviorSubject<string[]>([]);
  tabs$ = this.tabsSubject.asObservable();
  private contentSubject = new BehaviorSubject<any>(null);
  content$ = this.contentSubject.asObservable();

  private tabContents: { [tabName: string]: any } = {};

  openTab(tab: string, content?: any) {
    const tabs = this.tabsSubject.value;
    if (!tabs.includes(tab)) {
      this.tabsSubject.next([...tabs, tab]);
    }
    if (content) {
      this.tabContents[tab] = content;
    }
  }

  setContent(content: any) {
    this.contentSubject.next(content);
  }

  closeTab(tab: string) {
    const tabs = this.tabsSubject.value;
    this.tabsSubject.next(tabs.filter(t => t !== tab));
    delete this.tabContents[tab];
  }

  getTabContent(tab: string): any {
    return this.tabContents[tab];
  }

  setTabContent(tab: string, content: any) {
    this.tabContents[tab] = content;
  }
}
