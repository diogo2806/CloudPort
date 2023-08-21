/* role.component.ts */
import { Component, OnInit, ViewChild  } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../service/endpoint';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
//import * as XLSX from 'xlsx';
import { ContextMenuComponent } from '../context-menu/context-menu.component';
import { ChangeDetectorRef } from '@angular/core';
import { TabService } from '../navbar/TabService';


function logMethod(target: any, key: string, descriptor: PropertyDescriptor) {
  const originalMethod = descriptor.value;
  descriptor.value = function (...args: any[]) {
    console.log(`Classe ${target.constructor.name}: Método ${key} chamado.`);
    return originalMethod.apply(this, args);
  };
  return descriptor;
}




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
  selectedTab: string = 'defaultTab'; // ou qualquer valor padrão que faça sentido para o seu caso


  constructor(
    private http: HttpClient, 
    private authenticationService: AuthenticationService,
    private changeDetector: ChangeDetectorRef,
    private tabService: TabService
  ) { }

  private selectionStarted: boolean = false;
  private target: HTMLElement | null = null;
  private boundHandleTableContextMenu: any;
  private boundHandleRoleContextMenu: any;


  // Método executado quando o componente é inicializado
  @logMethod
  ngOnInit() {
    this.loadRoles();
    document.addEventListener('click', this.closeContextMenu.bind(this));
     this.boundHandleRoleContextMenu = this.handleRoleContextMenu.bind(this);
  //  document.addEventListener('contextmenu', this.boundHandleRoleContextMenu);
 
 
  //document.addEventListener('contextmenu', this.handleDocumentContextMenu.bind(this));
   
    //this.gridTable.nativeElement.addEventListener('contextmenu', this.handleTableContextMenu.bind(this));
}

handleDocumentContextMenu(event: MouseEvent) {
  event.preventDefault();
}

handleRoleContextMenu(event: any): void {
  // Implemente a lógica desejada aqui
  event.preventDefault();
}
  dragging: boolean = false;

  
  @logMethod
  mouseDown(event: MouseEvent, role: any) {
    event.preventDefault();
    this.dragging = true;
    this.toggleSelection(role);
  }

  @logMethod
  mouseUp(event: MouseEvent) {
    this.dragging = false;
  }


  @logMethod
  mouseover(event: MouseEvent, role: any) {
    if (this.dragging) {
      this.toggleSelection(role);
    }
  }


  @logMethod
  toggleSelection(role: any) {
    const index = this.selectedRoleIds.indexOf(role.id);
    
    if (index > -1) {
      // Se o ID já está no array, remova-o
      this.selectedRoleIds.splice(index, 1);
    } else {
      // Se o ID não está no array, adicione-o
      this.selectedRoleIds.push(role.id);
    }
  }

  // Carrega os papeis do servidor
  @logMethod
  loadRoles() {
    const storedRolesData = this.tabService.getTabContent(this.selectedTab);
    if (storedRolesData && storedRolesData.content) {
      this.roles = storedRolesData.content;
      return; // Se já temos dados armazenados, não precisamos fazer a chamada HTTP
    }

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
        this.roles = response.map(role => {
          return {
            'Role ID': role.id,
            'Role Name': role.name
          }
        });
         // Defina o conteúdo da aba após carregar os papéis
      this.tabService.setTabContent(this.selectedTab, this.roles);
      
      });
}


  // Cria um novo papel
  @logMethod
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
  @logMethod
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
  @logMethod
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
    
    
    selectedRoleId: number | null = null;
    selectedRoleIds: number[] = [];
    @ViewChild('contextMenu') contextMenu!: ContextMenuComponent;

    @logMethod
    rightClick(event: MouseEvent, role: any) {
      console.log("RoleComponent rightClick")
      event.preventDefault();
      
      this.contextMenu.menuOptions = ['Editar', 'Deletar']; // Define as opções aqui
      this.contextMenu.position = { x: event.clientX, y: event.clientY };
      this.contextMenu.isOpen = true;
    }


    @logMethod
    handleRoleRightClick(event: any, contextMenu: ContextMenuComponent) {

      console.log("Evento recebido:", event);
      document.addEventListener('contextmenu', this.handleDocumentContextMenu.bind(this));

      //event.preventDefault();

      if (event === null || event.event === undefined) {
        this.contextMenu.isOpen = false; // Feche o menu se o evento for nulo
        return;
      }
      event.event.preventDefault();
      
      console.log("RoleComponent handleRoleRightClick: Manipulando clique com o botão direito do mouse", event); // Depuração
      this.contextMenu.menuOptions = ['Editar', 'Deletar'];
      this.contextMenu.position = { x: event.event.clientX, y: event.event.clientY };
      this.contextMenu.isOpen = true;
    }
    
    
    

    /*
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

    */
    @logMethod
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
      }
      
      this.contextMenu.isOpen = false;
    }

    @logMethod
    closeContextMenu(event: MouseEvent) {
      if (this.contextMenu && !this.contextMenu.elementRef.nativeElement.contains(event.target)) {
        this.contextMenu.isOpen = false;
        console.log(this.contextMenu.isOpen);
        document.removeEventListener('contextmenu', this.handleDocumentContextMenu.bind(this));

      }
    }
    
    @logMethod
    ngOnDestroy() {
      document.removeEventListener('click', this.closeContextMenu.bind(this));
      document.removeEventListener('contextmenu', this.boundHandleTableContextMenu);
 
    }
}
