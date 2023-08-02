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

  // Método executado quando o componente é inicializado
  ngOnInit() {
    this.loadRoles();
    document.addEventListener('click', this.closeContextMenu.bind(this));
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
  @ViewChild('contextMenu') contextMenu!: ContextMenuComponent;

  rightClick(event: MouseEvent, role: any) {
    event.preventDefault();
    this.selectedRoleId = role.id;
    this.contextMenu.menuOptions = ['Editar', 'Deletar']; // Define as opções aqui
    this.contextMenu.position = { x: event.clientX, y: event.clientY };
    this.contextMenu.isOpen = true;
  }
  
  contextMenuOptionSelected(option: string) {
    if (this.selectedRoleId === null) {
      console.error('Nenhum role foi selecionado');
      return;
    }

    switch(option) {
      case 'Deletar':
        this.deactivateRole(this.selectedRoleId);
        break;

      // Aqui você pode adicionar casos para outras opções do menu de contexto
      // case 'Editar':
      //   this.editRole(this.selectedRoleId);
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
  document.removeEventListener('click', this.closeContextMenu.bind(this));
}



}
