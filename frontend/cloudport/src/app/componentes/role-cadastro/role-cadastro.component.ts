import { Component, Input } from '@angular/core';
import { PopupService } from '../service/popupService';

@Component({
  selector: 'app-role-cadastro',
  templateUrl: './role-cadastro.component.html',
  styleUrls: ['./role-cadastro.component.css']
})
export class RoleCadastroComponent {

  roleName: string = ''; // Propriedade para armazenar o Role Name
  showPopup = true;
  entityType = '';
  @Input() show: boolean = true; // Certifique-se de que 'show' é uma entrada
  
  constructor(private popupService: PopupService) {
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


}
