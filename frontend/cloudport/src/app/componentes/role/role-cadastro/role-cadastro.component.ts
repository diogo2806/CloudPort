/* role-cadastro.component.ts */
import { Component, Input, AfterViewInit  } from '@angular/core';
import { PopupService } from '../../service/popupService';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-role-cadastro',
  templateUrl: './role-cadastro.component.html',
  styleUrls: ['./role-cadastro.component.css']
})
export class RoleCadastroComponent {
  form: FormGroup = this.formBuilder.group({}); // Initialized here

  roleName: string = ''; // Propriedade para armazenar o Role Name
  showPopup = true;
  entityType = '';
  @Input() show: boolean = true; // Certifique-se de que 'show' é uma entrada
  
  constructor(
    private formBuilder: FormBuilder,
    private popupService: PopupService) {
    this.popupService.showPopup$.subscribe(popup => {
      console.log('Recebido:', popup); // Adicione este log
      this.entityType = popup.type;
      this.showPopup = popup.show;
    });
  }



  saveRole() {
    // Lógica para salvar o Role com o nome fornecido
    console.log('Salvando Role com o nome:', this.roleName);
  }

   

  closePopup() {
    this.popupService.closePopup();
  }

  onSubmit() {}

}
