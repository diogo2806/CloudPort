import { Component, EventEmitter, OnInit, Output, ViewChild  } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../service/endpoint';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import * as XLSX from 'xlsx';
import { ContextMenuComponent } from '../context-menu/context-menu.component';


@Component({
  selector: 'app-role',
  templateUrl: './role.component.html',
  styleUrls: ['./role.component.css']
})
export class RoleComponent implements OnInit {

  // Nome do papel
  roleName: string = "";

  // Lista dos papeis
  roles: any[] = [];

  constructor(
    private http: HttpClient, 
    private authenticationService: AuthenticationService

  ) { }

  private mouseDown: boolean = false;
  // Método executado quando o componente é inicializado
  ngOnInit() {
    this.loadRoles();

    // Adicionamos alguns listeners ao document para rastrear o estado do botão esquerdo do mouse
    document.addEventListener('mousedown', this.onMouseDown.bind(this));
    document.addEventListener('mouseup', this.onMouseUp.bind(this));
    document.addEventListener('mousemove', this.onMouseMove.bind(this));
    document.addEventListener('click', this.closeContextMenu.bind(this));
  }

  onMouseDown(event: MouseEvent) {
    if (event.button === 0) {
      console.log('Botão esquerdo do mouse pressionado.');
      this.mouseDown = true;
  
      // Inicialmente, o target é o elemento onde o mouse foi pressionado (pode ser a célula da tabela)
      let target = event.target as HTMLElement;
  
      // Se o elemento alvo não é uma linha da tabela, procuramos o elemento da linha da tabela entre os pais do alvo
      while (target && !target.classList.contains('table-row')) {
        target = target.parentElement as HTMLElement;
      }
  
      // Se encontramos um elemento com a classe 'table-row', então processamos o clique na linha da tabela
      if (target) {
        console.log('Linha da tabela clicada.');
        const roleId = Number(target.getAttribute('data-role-id'));
        console.log('ID do role obtido:', roleId);
        if (this.selectedRoleIds.indexOf(roleId) === -1) {
          this.selectedRoleIds.push(roleId);
          console.log('ID do role adicionado à lista de seleção:', roleId);
        } else {
          console.log('ID do role já estava na lista de seleção.');
        }
      } else {
        console.log('Clique não foi em uma linha da tabela.');
      }
    } else {
      console.log('Botão direito do mouse pressionado.');
    }
  }
  



  onMouseUp(event: MouseEvent) {
    // Verifica se o botão esquerdo do mouse foi solto
    if (event.button === 0) {
      this.mouseDown = false;
    }
  }

  onMouseMove(event: MouseEvent) {
    if (this.mouseDown) {
      let target = event.target as HTMLElement;
  
      // Subindo na árvore DOM até encontrar um elemento com a classe 'table-row'.
      while (target && !target.classList.contains('table-row')) {
        if (target.parentElement) {
          target = target.parentElement;
        } else {
          // Não há mais ancestrais na árvore DOM, portanto, saia do loop.
          return;
        }
      }
  
      if (target.classList.contains('table-row')) {
        console.log('Movimento do mouse detectado. Target:', target);
        const roleId = Number(target.getAttribute('data-role-id'));
        if (this.selectedRoleIds.indexOf(roleId) === -1) {
          this.selectedRoleIds.push(roleId);
          console.log('Role ID adicionado à lista de seleção:', roleId);
        } else {
          console.log('Role ID já está na lista de seleção:', roleId);
        }
      } else {
        console.log('Não foi possível encontrar um elemento com a classe \'table-row\'.');
      }
    } else {
      console.log('Botão do mouse não está pressionado.');
    }
  }
  
  
  
  

  // Carrega os papeis do servidor
  loadRoles() {
    const token = this.authenticationService.currentUserValue?.token;

    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type':  'application/json',
        'Authorization': 'Bearer ' + token
      })
    };

    this.http.get<any[]>(`${environment.role.getAll}`, httpOptions)
      .pipe(catchError((error: any) => {
        window.alert(error.message); // Aqui o popup é criado
        return throwError(error);
      }))
      .subscribe(response => {
        this.roles = response;
      });
  }

 // Cria um novo papel
createRole() {
  const token = this.authenticationService.currentUserValue?.token;

  const httpOptions = {
      headers: new HttpHeaders({
          'Content-Type':  'application/json',
          'Authorization': 'Bearer ' + token
      })
  };

  // Aplica trim(), toUpperCase() e substitui espaços por sublinhados
  const roleName = this.roleName.trim().toUpperCase().replace(' ', '_');

  this.http.post(`${environment.role.create}`, { name: roleName }, httpOptions)
      .pipe(catchError((error: any) => {
          window.alert(error.message); // Aqui o popup é criado
          return throwError(error);
      }))
      .subscribe(response => {
          console.log('Papel criado:', response);
          this.loadRoles();
      });
}

// Edita um papel existente
editRole(roleId: number) {
  const token = this.authenticationService.currentUserValue?.token;

  const httpOptions = {
      headers: new HttpHeaders({
          'Content-Type':  'application/json',
          'Authorization': 'Bearer ' + token
      })
  };

  // Aplica trim(), toUpperCase() e substitui espaços por sublinhados roleName
  const roleName = this.roleName.trim().toUpperCase().replace(' ', '_');

  this.http.put(`${environment.role.update(roleId)}`, { name: roleName}, httpOptions)
      .pipe(catchError((error: any) => {
          window.alert(error.message); // Aqui o popup é criado
          return throwError(error);
      }))
      .subscribe(response => {
          console.log('Papel atualizado:', response);
          this.loadRoles();
      });
}


// Desativa um papel
deactivateRole(roleId: number) {
  const token = this.authenticationService.currentUserValue?.token;

  const httpOptions = {
      headers: new HttpHeaders({
          'Content-Type':  'application/json',
          'Authorization': 'Bearer ' + token
      })
  };

  this.http.delete(`${environment.role.delete(roleId)}`, httpOptions)
    .pipe(catchError((error: any) => {
        if (error.status === 409) {
            window.alert('Não é possível deletar o Role pois ele ainda está sendo referenciado por um User.'); 
        } else {
            window.alert(error.message);
        }
        return throwError(error);
    }))
    .subscribe(response => {
        console.log('Role deletado:', response);
        this.loadRoles();
    });
  }

  exportToExcel() {
    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(this.roles);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Roles');
    
    /* save to file */
    XLSX.writeFile(wb, 'roles.xlsx');
  }

  selectedRoleId: number | null = null;
  selectedRoleIds: number[] = [];
  @ViewChild('contextMenu') contextMenu!: ContextMenuComponent;

  rightClick(event: MouseEvent, role: any) {
    event.preventDefault();
    
    this.contextMenu.menuOptions = ['Editar', 'Deletar']; // Define as opções aqui
    this.contextMenu.position = { x: event.clientX, y: event.clientY };
    this.contextMenu.isOpen = true;
  }



  leftClick(event: MouseEvent, role: any) {
    event.preventDefault();
    
    const index = this.selectedRoleIds.indexOf(role.id);
    
    if (index > -1) {
      // Se o ID já está no array, remova-o
      this.selectedRoleIds.splice(index, 1);
    } else {
      // Se o ID não está no array, adicione-o
      this.selectedRoleIds.push(role.id);
    }
  }

  
  
  contextMenuOptionSelected(option: string) {
    if (this.selectedRoleIds.length === 0) {
      console.error('Nenhum role foi selecionado');
      return;
    }
    
    switch(option) {
      case 'Deletar':
        // Agora desativamos todos os roles selecionados
        for (const id of this.selectedRoleIds) {
          this.deactivateRole(id);
        }
        break;
        
      // Aqui você pode adicionar casos para outras opções do menu de contexto
      // case 'Editar':
      //   for (const id of this.selectedRoleIds) {
      //     this.editRole(id);
      //   }
      //   break;
    }
    
    this.contextMenu.isOpen = false;
  }
  

  closeContextMenu(event: MouseEvent) {
    if (!this.contextMenu.elementRef.nativeElement.contains(event.target)) {
        this.contextMenu.isOpen = false;
    }
}

ngOnDestroy() {
    document.removeEventListener('mousedown', this.onMouseDown.bind(this));
    document.removeEventListener('mouseup', this.onMouseUp.bind(this));
    document.removeEventListener('mousemove', this.onMouseMove.bind(this));
    document.removeEventListener('click', this.closeContextMenu.bind(this));
  }



}
