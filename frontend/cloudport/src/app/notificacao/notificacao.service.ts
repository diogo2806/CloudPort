import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class NotificacaoService {
  private notificacaoSubject = new Subject<string>();

  notificacao$ = this.notificacaoSubject.asObservable();

  show(mensagem: string) {
    this.notificacaoSubject.next(mensagem);
  }
}
