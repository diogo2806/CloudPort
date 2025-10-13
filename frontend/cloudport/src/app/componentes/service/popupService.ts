/* popupService.ts */

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';


function logMethod(target: any, key: string, descriptor: PropertyDescriptor) {
  const originalMethod = descriptor.value;
  descriptor.value = function (...args: any[]) {
    console.log(`Classe ${target.constructor.name}: Método ${key} chamado.`);
    return originalMethod.apply(this, args);
  };
  return descriptor;
}



export interface PopupState<T = any> {
  type: string;
  show: boolean;
  data?: T;
}

export interface ConfirmacaoModalData {
  titulo?: string;
  mensagem: string;
  textoConfirmar?: string;
  textoCancelar?: string;
}

@Injectable({
    providedIn: 'root'
  })
  export class PopupService {
    private showPopupSource = new BehaviorSubject<PopupState>({type: '', show: false});
    showPopup$ = this.showPopupSource.asObservable();
    private confirmacaoSubject: Subject<boolean> | null = null;


    @logMethod
  openPopup(type: string, data?: any) {
    const newValue: PopupState = {type, show: true, data};
    console.log('Atualizando showPopupSource com:', newValue); // Adicione este log
    this.showPopupSource.next(newValue);
  }

/*
    @logMethod
    openPopup(type: string) {
      this.showPopupSource.next({type, show: true});
    }
    */
  
    @logMethod
    closePopup() {
      this.showPopupSource.next({type: '', show: false});
    }

    @logMethod
    openConfirmacao(data: ConfirmacaoModalData): Observable<boolean> {
      if (this.confirmacaoSubject) {
        this.confirmacaoSubject.complete();
      }

      this.confirmacaoSubject = new Subject<boolean>();
      this.openPopup('confirmacao', data);
      return this.confirmacaoSubject.asObservable();
    }

    @logMethod
    resolveConfirmacao(confirmado: boolean) {
      if (this.confirmacaoSubject) {
        this.confirmacaoSubject.next(confirmado);
        this.confirmacaoSubject.complete();
        this.confirmacaoSubject = null;
      }
      this.closePopup();
    }
  }
  