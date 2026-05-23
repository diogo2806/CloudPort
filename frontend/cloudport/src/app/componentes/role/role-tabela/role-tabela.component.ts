/* role.component.ts */
import { Component, OnInit, ViewChild  } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';
import { ServicoAutenticacao } from '../../service/servico-autenticacao/servico-autenticacao.service';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
//import * as XLSX from 'xlsx';
import { ContextMenuComponent } from '../../context-menu/context-menu.component';
import { ChangeDetectorRef } from '@angular/core';
import { TabService } from '../../navbar/TabService';
import { Renderer2 } from '@angular/core';
import { AfterViewInit } from '@angular/core';
import { PopupService } from '../../service/popupService';
import { ModalComponent } from '../../modal/modal.component';
import { GridReadyEvent } from 'ag-grid-community';




@Component({
    selector: 'app-role-tabela',
    templateUrl: './role-tabela.component.html',
    styleUrls: ['./role-tabela.component.css'],
    standalone: false
})
export class RoleTabelaComponent  implements OnInit, AfterViewInit {

  // Nome do papel
  roleName: string = "";
  showPopup = false;

  // Lista dos papeis
  roles: any[] = [];
  selectedTab: string = 'role';
  @ViewChild(ModalComponent) modal!: ModalComponent; // Referência ao ModalComponent


  constructor(
    private http: HttpClient,
    private servicoAutenticacao: ServicoAutenticacao,
    private tabService: TabService,
    private renderer: Renderer2,
    private popupService: PopupService,
    private configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {

   

  }


  private boundHandleTableContextMenu: any;



  @ViewChild('gridHoleTable', { static: false }) gridTable: any;


  openPopup() {
    this.showPopup = true;
  }
  
  closePopup() {
    this.showPopup = false;
  }
  

  // Método executado quando o componente é inicializado

  ngOnInit() {

    this.loadRoles();

    
}

onGridTableReady(event: GridReadyEvent) {
  const tableElement = this.gridTable.nativeElement; // Referência direta ao elemento da tabela ag-Grid
  this.renderer.listen(tableElement, 'contextmenu', (event) => {
    event.preventDefault(); // Previne o menu de contexto padrão
    this.contextMenu.menuOptions = ['Editar', 'Deletar'];
    this.contextMenu.position = { x: event.clientX, y: event.clientY };
    this.contextMenu.isOpen = true;
    console.warn('Elemento da tabela ag-Grid encontrado');
  });
}







ngAfterViewInit() {

  if (this.gridTable && this.gridTable.nativeElement) {
    const tableElement = this.gridTable.nativeElement; // Referência direta ao elemento da tabela ag-Grid
    this.renderer.listen(tableElement, 'contextmenu', (event) => {
      event.preventDefault(); // Previne o menu de contexto padrão
      this.contextMenu.menuOptions = ['Editar', 'Deletar'];
      this.contextMenu.position = { x: event.clientX, y: event.clientY };
      this.contextMenu.isOpen = true;
      console.warn('Elemento da tabela ag-Grid encontrado');
    });
  } else {
    console.warn('Elemento da tabela ag-Grid não encontrado');
  }
}



handleDocumentContextMenu(event: MouseEvent) {
  event.preventDefault();
}

handleRoleContextMenu(event: any): void {
  // Implemente a lógica desejada aqui
  event.preventDefault();
}




handleRoleRightClick(event: any, contextMenu: ContextMenuComponent) {

  if (event === null || event.event === undefined) {
    this.contextMenu.isOpen = false; // Feche o menu se o evento for nulo
    return; // Deixe o evento de clique com o botão direito do mouse ser processado normalmente
  }

  const rowData = event.row;
  if (rowData && rowData['Role ID'] !== undefined && rowData['Role ID'] !== null) {
    const roleId = typeof rowData['Role ID'] === 'number' ? rowData['Role ID'] : Number(rowData['Role ID']);
    this.selectedRoleIds = isNaN(roleId) ? [] : [roleId];
  } else {
    this.selectedRoleIds = [];
  }

  // Evite o menu de contexto padrão do navegador e exiba o menu personalizado
  event.event.preventDefault();

  this.contextMenu.menuOptions = ['Editar', 'Deletar'];
  this.contextMenu.position = { x: event.event.clientX, y: event.event.clientY };
  this.contextMenu.isOpen = true;
}

closeContextMenu(event: MouseEvent) {
  if (this.contextMenu && !this.contextMenu.elementRef.nativeElement.contains(event.target)) {
    this.contextMenu.isOpen = false;
    event.preventDefault();
    // Deixe o evento de clique com o botão direito do mouse ser processado normalmente
  }
}



  dragging: boolean = false;

  
  mouseDown(event: MouseEvent, role: any) {
    event.preventDefault();
    this.dragging = true;
    this.toggleSelection(role);
  }

  mouseUp(event: MouseEvent) {
    this.dragging = false;
  }


  mouseover(event: MouseEvent, role: any) {
    if (this.dragging) {
      this.toggleSelection(role);
    }
  }


  toggleSelection(role: any) {
    const roleId = role && role['Role ID'] !== undefined ? role['Role ID'] : null;
    if (roleId === null) {
      return;
    }

    const normalizedRoleId = typeof roleId === 'number' ? roleId : Number(roleId);
    if (isNaN(normalizedRoleId)) {
      return;
    }

    const index = this.selectedRoleIds.indexOf(normalizedRoleId);

    if (index > -1) {
      // Se o ID já está no array, remova-o
      this.selectedRoleIds.splice(index, 1);
    } else {
      // Se o ID não está no array, adicione-o
      this.selectedRoleIds.push(normalizedRoleId);
    }
  }

  // Carrega os papeis do servidor
  loadRoles() {
    const storedRolesData = this.tabService.getTabContent(this.selectedTab);
    if (storedRolesData) {
      this.roles = storedRolesData;
      return; // Se já temos dados armazenados, não precisamos fazer a chamada HTTP
    }

    const token = this.servicoAutenticacao.obterUsuarioAtual()?.token;

    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type':  'application/json',
        'Authorization': 'Bearer ' + token
      })
    };

    const url = this.obterUrlRoles();
    this.http.get<any[]>(url, httpOptions)
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
  createRole() {
    const token = this.servicoAutenticacao.obterUsuarioAtual()?.token;

    const httpOptions = {
        headers: new HttpHeaders({
            'Content-Type':  'application/json',
            'Authorization': 'Bearer ' + token
        })
    };

    // Aplica trim(), toUpperCase() e substitui espaços por sublinhados
    const roleName = this.roleName.trim().toUpperCase().replace(' ', '_');

    const createUrl = this.obterUrlRoles();
    this.http.post(createUrl, { name: roleName }, httpOptions)
        .pipe(catchError((error: any) => {
            window.alert(error.message); // Aqui o popup é criado
            return throwError(error);
        }))
        .subscribe(response => {
            this.loadRoles();
        });
  }

  // Edita um papel existente
  editRole(roleId: number) {
    const token = this.servicoAutenticacao.obterUsuarioAtual()?.token;

    const httpOptions = {
        headers: new HttpHeaders({
            'Content-Type':  'application/json',
            'Authorization': 'Bearer ' + token
        })
    };

    // Aplica trim(), toUpperCase() e substitui espaços por sublinhados roleName
    const roleName = this.roleName.trim().toUpperCase().replace(' ', '_');

    const updateUrl = `${this.obterUrlRoles()}/${roleId}`;
    this.http.put(updateUrl, { name: roleName}, httpOptions)
        .pipe(catchError((error: any) => {
            window.alert(error.message); // Aqui o popup é criado
            return throwError(error);
        }))
        .subscribe(response => {
            this.loadRoles();
        });
  }

  // Desativa um papel
  deactivateRole(roleId: number) {
    const token = this.servicoAutenticacao.obterUsuarioAtual()?.token;

    const httpOptions = {
        headers: new HttpHeaders({
            'Content-Type':  'application/json',
            'Authorization': 'Bearer ' + token
        })
    };

    const deleteUrl = `${this.obterUrlRoles()}/${roleId}`;
    this.http.delete(deleteUrl, httpOptions)
      .pipe(catchError((error: any) => {
          if (error.status === 409) {
              window.alert('Não é possível deletar o Role pois ele ainda está sendo referenciado por um User.'); 
          } else {
              window.alert(error.message);
          }
          return throwError(error);
      }))
      .subscribe(response => {
          this.loadRoles();
      });
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


    /*
    leftClick(event: MouseEvent, role: any) {
      event.preventDefault();

      const roleId = role && role['Role ID'] !== undefined ? role['Role ID'] : null;
      if (roleId === null) {
        return;
      }

      const normalizedRoleId = typeof roleId === 'number' ? roleId : Number(roleId);
      if (isNaN(normalizedRoleId)) {
        return;
      }

      const index = this.selectedRoleIds.indexOf(normalizedRoleId);

      if (index > -1) {
        // Se o ID já está no array, remova-o
        this.selectedRoleIds.splice(index, 1);
      } else {
        // Se o ID não está no array, adicione-o
        this.selectedRoleIds.push(normalizedRoleId);
      }
    }

    */
    contextMenuOptionSelected(option: string) {
      if (this.selectedRoleIds.length === 0) {
        console.error('Nenhum role foi selecionado');
        return;
      }
      
      switch(option) {
        case 'Deletar':
          // Agora desativamos todos os roles selecionados
          for (const id of this.selectedRoleIds) {
            if (id === null || id === undefined) {
              continue;
            }

            const normalizedId = typeof id === 'number' ? id : Number(id);
            if (!isNaN(normalizedId)) {
              this.deactivateRole(normalizedId);
            }
          }
          break;
      }
      this.contextMenu.isOpen = false;
    }

    
    ngOnDestroy() {
      document.removeEventListener('click', this.closeContextMenu.bind(this));
      document.removeEventListener('contextmenu', this.boundHandleTableContextMenu);
    }

    private obterUrlRoles(): string {
      return this.configuracaoAplicacao.construirUrlApi('/api/roles');
    }

}
