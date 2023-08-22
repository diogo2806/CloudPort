import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';



function logMethod(target: any, key: string, descriptor: PropertyDescriptor) {
  const originalMethod = descriptor.value;
  descriptor.value = function (...args: any[]) {
    console.log(`Classe ${target.constructor.name}: Método ${key} chamado.`);
    return originalMethod.apply(this, args);
  };
  return descriptor;
}



@Injectable({
    providedIn: 'root'
  })
  export class PopupService {
    private showPopupSource = new BehaviorSubject<{type: string, show: boolean}>({type: '', show: false});
    showPopup$ = this.showPopupSource.asObservable();
    
    @logMethod
    openPopup(type: string) {
      this.showPopupSource.next({type, show: true});
    }
  
    @logMethod
    closePopup() {
      this.showPopupSource.next({type: '', show: false});
    }
  }
  