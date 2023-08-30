import { Component, Input, ViewChild, ViewContainerRef, ComponentFactoryResolver, ComponentRef, AfterViewInit, OnInit, Type, Injector } from '@angular/core';
import { PopupService } from '../service/popupService';
import { RoleCadastroComponent } from '../role/role-cadastro/role-cadastro.component'; // Importe todos os componentes que você pode querer carregar dinamicamente
import { ChangeDetectorRef } from '@angular/core';
import { Renderer2, ElementRef } from '@angular/core';


function logMethod(target: any, key: string, descriptor: PropertyDescriptor) {
  const originalMethod = descriptor.value;
  descriptor.value = function (...args: any[]) {
    console.log(`Classe ${target.constructor.name}: Método ${key} chamado.`);
    return originalMethod.apply(this, args);
  };
  return descriptor;
}


const componentMapping = {
  'role': RoleCadastroComponent,
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



  constructor(private viewContainerRef: ViewContainerRef, private componentFactoryResolver: ComponentFactoryResolver, private popupService: PopupService, private cdRef: ChangeDetectorRef) {
    // Removido a subscrição aqui
  }

  @logMethod
  ngAfterViewInit() {
    this.popupService.showPopup$.subscribe(popup => {
      console.log('Recebido:', popup);
      this.entityType = popup.type;
      this.showPopup = popup.show;
      this.loadComponent(popup.type);
    });
  }

  @logMethod
loadComponent(componentTypeString: string) {
  if (this.componentRef) {
    this.componentRef.destroy();
    this.componentRef = null;
  }

  const componentType = componentMapping[componentTypeString as keyof typeof componentMapping];

  if (!componentType) {
    console.error(`Componente não encontrado para o tipo: ${componentTypeString}`);
    return;
  }


  console.log('componentTypeString: '+componentTypeString);
  
  const injector = Injector.create({providers: [], parent: this.viewContainerRef.injector});
  
  // Correção aqui: passar componentType como primeiro argumento e o objeto de opções como segundo argumento
  this.componentRef = this.viewContainerRef.createComponent(componentType, {
    injector: injector
  });
}




  @logMethod
  closePopup() {
    this.popupService.closePopup();
  }
}
