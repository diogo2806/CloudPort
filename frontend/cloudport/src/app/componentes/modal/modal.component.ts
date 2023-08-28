import { Component, Input, ViewChild, ViewContainerRef, ComponentFactoryResolver, ComponentRef, AfterViewInit } from '@angular/core';
import { PopupService } from '../service/popupService';
import { RoleCadastroComponent } from '../role-cadastro/role-cadastro.component'; // Importe todos os componentes que você pode querer carregar dinamicamente
import { ChangeDetectorRef } from '@angular/core';

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
export class ModalComponent implements AfterViewInit  {

  @ViewChild('dynamicComponentContainer', { read: ViewContainerRef }) container!: ViewContainerRef;
  private componentRef: ComponentRef<any> | null = null;
  @Input() arg:any;  //<--you pass as arg any object


  showPopup = false;
  entityType = '';
  @Input() show: boolean = false; // Certifique-se de que 'show' é uma entrada

  /*
  constructor(private componentFactoryResolver: ComponentFactoryResolver, private popupService: PopupService, private cdRef: ChangeDetectorRef) {
    this.popupService.showPopup$.subscribe(popup => {
      console.log('Recebido:', popup); // Adicione este log
      this.entityType = popup.type;
      this.showPopup = popup.show;
      this.loadComponent(popup.type);
    });
  }
  */
  constructor(private viewContainerRef: ViewContainerRef, private componentFactoryResolver: ComponentFactoryResolver, private popupService: PopupService, private cdRef: ChangeDetectorRef) {
    // Removido a subscrição aqui
  }

  @logMethod
  ngAfterViewInit() {
    this.popupService.showPopup$.subscribe(popup => {
      console.log('Recebido:', popup);
      this.entityType = popup.type;
      this.showPopup = popup.show;

      if (this.showPopup) {
        const component = this.componentFactoryResolver.resolveComponentFactory(RoleCadastroComponent);
        const componentRef = this.container.createComponent(component);
        
        // Usando asserção de tipo para evitar erro de compilação
        Object.keys(this.arg).forEach(x => {
          (componentRef.instance as any)[x] = this.arg[x];
        });
      }
      this.cdRef.detectChanges();
      console.log('fim ngAfterViewInit:');
    });
  }

  @logMethod
loadComponent(type: string) {
  if (this.componentRef) {
    this.componentRef.destroy();
    this.componentRef = null;
  }

  let component: any;
  if (type === 'role') {
    component = RoleCadastroComponent;
  }



  console.log('this.container: '+this.container);

  if (this.container) {
    const componentFactory = this.componentFactoryResolver.resolveComponentFactory(RoleCadastroComponent);
    this.componentRef = this.container.createComponent(componentFactory);
    console.log('componentRef: '  + this.componentRef);
  } else {
    console.error('this.container é undefined');
  }
}



  @logMethod
  closePopup() {
    this.popupService.closePopup();
  }
}
