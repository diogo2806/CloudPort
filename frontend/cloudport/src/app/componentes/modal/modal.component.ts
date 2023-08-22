/* modal.component.ts */

import { Component, Input  } from '@angular/core';
import { PopupService } from '../service/popupService';
import { BehaviorSubject } from 'rxjs';


function logMethod(target: any, key: string, descriptor: PropertyDescriptor) {
  const originalMethod = descriptor.value;
  descriptor.value = function (...args: any[]) {
    console.log(`Classe ${target.constructor.name}: Método ${key} chamado.`);
    return originalMethod.apply(this, args);
  };
  return descriptor;
}



@Component({
  selector: 'app-modal',
  templateUrl: './modal.component.html',
  styleUrls: ['./modal.component.css']
})
export class ModalComponent {
  
  showPopup = false;
  entityType = '';
  @Input() show: boolean = false; // Certifique-se de que 'show' é uma entrada
  
  constructor(private popupService: PopupService) {
    this.popupService.showPopup$.subscribe(popup => {
      this.entityType = popup.type;
      this.showPopup = popup.show;
    });
  }

  closePopup() {
    this.popupService.closePopup();
  }


}
