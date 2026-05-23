/* popupService.ts */

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';


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


  openPopup(type: string, data?: any) {
    const newValue: PopupState = {type, show: true, data};
    this.showPopupSource.next(newValue);
  }

/*
    openPopup(type: string) {
      this.showPopupSource.next({type, show: true});
    }
    */

    closePopup() {
      this.showPopupSource.next({type: '', show: false});
    }

    openConfirmacao(data: ConfirmacaoModalData): Observable<boolean> {
      if (this.confirmacaoSubject) {
        this.confirmacaoSubject.complete();
      }

      this.confirmacaoSubject = new Subject<boolean>();
      this.openPopup('confirmacao', data);
      return this.confirmacaoSubject.asObservable();
    }

    resolveConfirmacao(confirmado: boolean) {
      if (this.confirmacaoSubject) {
        this.confirmacaoSubject.next(confirmado);
        this.confirmacaoSubject.complete();
        this.confirmacaoSubject = null;
      }
      this.closePopup();
    }
  }
  