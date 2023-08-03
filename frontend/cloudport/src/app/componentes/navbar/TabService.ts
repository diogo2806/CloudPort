import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TabService {
  private tabsSubject = new BehaviorSubject<string[]>([]);
  tabs$ = this.tabsSubject.asObservable();

  openTab(tab: string) {
    const tabs = this.tabsSubject.value;
    if (!tabs.includes(tab)) {
      this.tabsSubject.next([...tabs, tab]);
    }
  }

  closeTab(tab: string) {
    const tabs = this.tabsSubject.value;
    this.tabsSubject.next(tabs.filter(t => t !== tab));
    
  }
}
