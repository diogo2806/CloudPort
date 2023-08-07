// tab-state.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TabStateService {
  private tabState = new BehaviorSubject<{ [key: string]: any }>({});

  get state() {
    return this.tabState.asObservable();
  }

  setTabState(tabName: string, state: any) {
    const currentState = this.tabState.value;
    currentState[tabName] = state;
    this.tabState.next(currentState);
  }

  getTabState(tabName: string) {
    return this.tabState.value[tabName];
  }
}
