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

  // Armazenamento para o conteúdo de cada aba
  private tabContents: { [tabName: string]: any } = {};


  openTab(tab: string, content?: any) {
    const tabs = this.tabsSubject.value;
    if (!tabs.includes(tab)) {
      this.tabsSubject.next([...tabs, tab]);
      if (content) {
        this.tabContents[tab] = content;
      }
    }
  }

  
    setContent(content: any) {
      this.contentSubject.next(content);
    }
    
    
  closeTab(tab: string) {
    const tabs = this.tabsSubject.value;
    this.tabsSubject.next(tabs.filter(t => t !== tab));
    // Remova o conteúdo da aba quando ela for fechada
    delete this.tabContents[tab];
  }

  getTabContent(tab: string): any {
    const content = this.tabContents[tab];
    if (content) {
    } else {
    }
    return content;
  }

  setTabContent(tab: string, content: any) {
    this.tabContents[tab] = content;
  }
}
