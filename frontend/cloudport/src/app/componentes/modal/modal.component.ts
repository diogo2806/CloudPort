import { Component, Input, ViewChild, ViewContainerRef, ComponentRef, AfterViewInit, Type, Injector, ChangeDetectorRef } from '@angular/core';
import { PopupService, PopupState } from '../service/popupService';
import { RoleCadastroComponent } from '../role/role-cadastro/role-cadastro.component'; // Importe todos os componentes que você pode querer carregar dinamicamente
import { ConfirmacaoModalComponent } from './confirmacao-modal/confirmacao-modal.component';


function logMethod(target: any, key: string, descriptor: PropertyDescriptor) {
  const originalMethod = descriptor.value;
  descriptor.value = function (...args: any[]) {
    console.log(`Classe ${target.constructor.name}: Método ${key} chamado.`);
    return originalMethod.apply(this, args);
  };
  return descriptor;
}


const componentMapping: Record<string, Type<any>> = {
  'role': RoleCadastroComponent,
  'confirmacao': ConfirmacaoModalComponent,
  // Adicione outros mapeamentos aqui
};
@Component({
  selector: 'app-modal',
  templateUrl: './modal.component.html',
  styleUrls: ['./modal.component.css']
})
export class ModalComponent implements AfterViewInit {

  @ViewChild('dynamicComponentContainer', { read: ViewContainerRef }) container!: ViewContainerRef;
  private componentRef: ComponentRef<any> | null = null;

  @Input() arg:any;  //<--you pass as arg any object


  showPopup = false;
  entityType = '';
  @Input() show: boolean = false; // Certifique-se de que 'show' é uma entrada



  constructor(private viewContainerRef: ViewContainerRef, private popupService: PopupService, private cdRef: ChangeDetectorRef) {
    // Removido a subscrição aqui
  }

  @logMethod
  ngAfterViewInit() {
    this.popupService.showPopup$.subscribe((popup) => {
      console.log('Recebido:', popup);
      this.entityType = popup.type;
      this.showPopup = popup.show;
      if (popup.show) {
        this.loadComponent(popup);
      } else if (this.componentRef) {
        this.componentRef.destroy();
        this.componentRef = null;
      }
    });
  }

  @logMethod
  loadComponent(popup: PopupState) {
    if (this.componentRef) {
      this.componentRef.destroy();
      this.componentRef = null;
    }

    const componentType = componentMapping[popup.type as keyof typeof componentMapping];

    if (!componentType) {
      console.error(`Componente não encontrado para o tipo: ${popup.type}`);
      return;
    }


    console.log('componentTypeString: ' + popup.type);

    const injector = Injector.create({ providers: [], parent: this.viewContainerRef.injector });

    this.componentRef = this.viewContainerRef.createComponent(componentType, {
      injector: injector
    });

    if (this.componentRef && popup.data !== undefined) {
      Object.assign(this.componentRef.instance, { data: popup.data });
    }

    this.cdRef.detectChanges();
  }




  @logMethod
  closePopup() {
    this.popupService.closePopup();
  }
}
