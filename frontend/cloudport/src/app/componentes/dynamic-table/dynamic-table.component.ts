/* dynamic-table.component.ts */
import { ViewChild, ElementRef } from '@angular/core';
import { Component, Input, OnInit, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { AfterViewInit } from '@angular/core';
import * as XLSX from 'xlsx';
import { ColDef, GridApi, GridOptions, GridReadyEvent, IDateFilterParams, IMultiFilterParams, ISetFilterParams } from 'ag-grid-community';
import { TabStateService } from './tab-state.service';
import { PopupService } from '../service/popupService';


function logMethod(target: any, key: string, descriptor: PropertyDescriptor) {
  const originalMethod = descriptor.value;
  descriptor.value = function (...args: any[]) {
    console.log(`Classe ${target.constructor.name}: Método ${key} chamado.`);
    return originalMethod.apply(this, args);
  };
  return descriptor;
}



@Component({
  selector: 'app-dynamic-table',
  templateUrl: './dynamic-table.component.html',
  styleUrls: ['./dynamic-table.component.css']
})
export class DynamicTableComponent implements OnInit, AfterViewInit {

  private _data: any[] = [];
  private gridApi!: GridApi;

  @Input() columns: string[] = [];
  //@Input() data: any[] = [];
  
  @Input() 
  set data(value: any[]) {
    this._data = value;
    this.filteredData = [...this._data];
    if (this.gridApi) {
      this.gridApi.setRowData(this.filteredData);
    }
  }
  @Input() selectedRoleIds: number[] = [];
  @Output() mouseDown = new EventEmitter<any>();
  @Output() mouseUp = new EventEmitter<any>();
  @Output() mouseOver = new EventEmitter<any>();
  @Output() rightClick = new EventEmitter<any>();
  @Input() selectedTab: string = '';
  @ViewChild('gridTable') gridTable!: ElementRef;
  @Output() gridReady = new EventEmitter<void>();

  @Output() createRole = new EventEmitter<void>();

  filteredData: any[] = [];
  filters: { [key: string]: string } = {};
  private boundHandleTableContextMenu: any;
 
  columnDefinitions: ColDef[] = [];
  dragging: boolean = false;

  tabela: boolean = false;
  constructor(
    private tabStateService: TabStateService,
    private popupService: PopupService
  ) {}

  


  ngOnInit(): void {
    console.log('Classe DynamicTableComponent: : Método ngOnInit iniciado.');
   
    this.columnDefinitions = this.columns.map(column => ({
      headerName: column,
      field: column,
      filter: true,
      sortable: true
    }));
    this.filteredData = [...this.data];
  
   // this.gridTable.nativeElement.addEventListener('contextmenu', this.handleTableContextMenu.bind(this));

    console.log('Classe DynamicTableComponent: : Método ngOnInit finalizado.');
   // document.addEventListener('click', this.closeContextMenu.bind(this));
   //this.gridTable.nativeElement.removeEventListener('contextmenu', this.handleTableContextMenu.bind(this));
   //this.boundHandleTableContextMenu = this.handleTableContextMenu.bind(this);
   //document.addEventListener('contextmenu', this.boundHandleTableContextMenu);


  }
  
  onContextMenu(event: MouseEvent): void {
    event.preventDefault(); // Previne o menu de contexto padrão
  }
  

  ngAfterViewInit(): void {
    console.log('Classe DynamicTableComponent: : Método ngAfterViewInit iniciado.');
    // Adicione o manipulador de eventos para o evento 'contextmenu'
   // this.gridTable.nativeElement.addEventListener('contextmenu', this.handleTableContextMenu.bind(this));
    console.log('Classe DynamicTableComponent: : Método ngAfterViewInit finalizado.');
  }


  handleTableContextMenu(event: any): void {
    
    //document.addEventListener('contextmenu', this.handleTableContextMenu.bind(this));
    //document.removeEventListener('contextmenu', this.handleTableContextMenu.bind(this));
    event.preventDefault();
    //console.log("DynamicTableComponent handleTableContextMenu event: ",event);
    const row = 0; // Acessa os dados da linha clicada
    console.log('DynamicTableComponent handleTableContextMenu event row: Emitindo evento de clique com o botão direito do mouse', { event: event, row }); // Depuração

/*

      console.log(event)

    
    if(event==null){
      console.log("handleTableContextMenu: event.row==null")

    }
   if (this.gridTable && this.gridTable.nativeElement) {
     console.log("handleTableContextMenu: oi")
  }

  if (this.gridTable) {
    console.log("handleTableContextMenu: this.gridTable")
 }
 
 if (this.gridTable==null) {
  console.log("handleTableContextMenu: this.gridTable==null")
}
console.log("handleTableContextMenu: ", event)
      event.preventDefault(); // Previne o menu de contexto padrão dentro da tabela

      */
  }



 // @logMethod
 // ngAfterViewInit(): void {
   // if (this.gridTable && this.gridTable.nativeElement) {
   //   this.gridTable.nativeElement.addEventListener('contextmenu', this.handleTableContextMenu.bind(this));
  // }
  //}
  
  

  


  ngOnDestroy() {
   // if (this.gridTable && this.gridTable.nativeElement) {
   // document.removeEventListener('contextmenu', this.handleTableContextMenu.bind(this));
    // this.gridTable.nativeElement.removeEventListener('contextmenu', this.handleTableContextMenu.bind(this));
    //}
    this.gridTable.nativeElement.removeEventListener('contextmenu', this.handleTableContextMenu.bind(this));

  }
  

  onCellContextMenu(event: any): void {
    const mouseEvent = event.event as MouseEvent;
    mouseEvent.preventDefault(); // Previne o menu de contexto padrão dentro da tabela

    if (mouseEvent.button !== 2) {
      return; // Ignora se não for o botão direito do mouse
    }

    mouseEvent.stopPropagation();

    const row = event.data; // Acessa os dados da linha clicada
    console.log('DynamicTableComponent onCellContextMenu: Emitindo evento de clique com o botão direito do mouse', { event: mouseEvent, row }); // Depuração
    this.rightClick.emit({ event: mouseEvent, row });
  }






  getContextMenuItems(params: any): any[] {
    return []; // Desativa o menu de contexto
  }
  

 // @logMethod
//preventRightClickDefault(event: MouseEvent): void {
  //console.log("preventRightClickDefault: "+event.target?.dispatchEvent)
  /*
  if (this.gridTable && this.gridTable.nativeElement.contains(event.target as Node)) {
    console.log('CLIQUE DENTRO DA TABELA');
    event.preventDefault();
    //this.rightClick.emit(null); // Emita um evento nulo para fechar o menu
  } else {
    console.log('CLIQUE FORA DA TABELA');
    //event.preventDefault();
    //this.rightClick.emit(null); // Emita um evento nulo para fechar o menu
  }
  */
//}



  closeContextMenu(event: MouseEvent): void {
    const target = event?.target as Node | null;
    const clickedInsideGrid = !!(target && this.gridTable?.nativeElement?.contains(target));

    const contextMenuElement = document.querySelector('.context-menu');
    const clickedInsideMenu = !!(target && contextMenuElement?.contains(target));

    if (!clickedInsideGrid && !clickedInsideMenu) {
      this.rightClick.emit(null);
    }
  }
  

  

  onGridReady(params: any) {
    this.gridApi = params.api;
   // this.gridTable.nativeElement.addEventListener('contextmenu', this.handleTableContextMenu.bind(this));
  
    //this.gridReady.emit();

}




onBtExport() {
  try {
    // Obter os dados das linhas filtradas percorrendo todos os nós disponíveis
    const dados: any[] = [];
    this.gridApi.forEachNodeAfterFilter((node) => {
      if (node.data) {
        dados.push(node.data);
      }
    });

    // Converter os dados coletados para JSON antes da exportação
    const jsonData = JSON.parse(JSON.stringify(dados));

    // Preparar as colunas para exportação
    const columns = this.columns.map(column => ({ [column]: '' }));
    const exportData = jsonData.length ? jsonData : columns;

    // Usar a biblioteca XLSX para exportar os dados
    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(exportData);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Sheet1');
    XLSX.writeFile(wb, 'data.xlsx');
    console.log("DynamicTableComponent onBtExport: Exportação bem-sucedida");
  } catch (error) {
    // Tratar err
  }
}






  onCellClicked(event: any) {
    if (!event.node.isSelected()) {
     // event.node.setSelected(true);
    } else {
      //event.node.setSelected(false);
    }
   // console.log('Célula clicada:', event);
   this.rightClick.emit(null); // Emita um evento nulo para fechar o menu
  // console.log('Célula clicada:', event);
  }

   /*
     @logMethod
  onCellClicked(event: any) {
    if (!event.node.isSelected()) {
      event.node.setSelected(true);
    } else {
      event.node.setSelected(false);
    }
   // console.log('Célula clicada:', event);
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

    */


  onCellDoubleClicked(event: any) {
    console.log('DynamicTableComponent onCellDoubleClicked: Célula clicada com o botão direito:', event);
  }
/*
  @logMethod
  onCellRightClicked(event: any) {
    console.log('onCellRightClicked: ', event);
    const mouseEvent = event.event as MouseEvent;
    const row = event.data; // Acessa os dados da linha clicada
    this.rightClick.emit({ event: mouseEvent, row });

  }
*/
  
  onRowClicked(event: any) {
    console.log('onRowClicked: ', event);
    //this.rightClick.emit({event});
  }
  



  public defaultColDef: ColDef = {
    flex: 1,
    minWidth: 200,
    resizable: true,
    floatingFilter: true,
    menuTabs: ['filterMenuTab'],
  };


  
  get data(): any[] {
    return this._data;
  }
  

  onRowMouseDown(event: MouseEvent, row: any) {
    if (event.button !== 0) return;
    event.preventDefault();
    this.dragging = true;
    this.toggleSelection(row);
    this.mouseDown.emit({ event, row });
    console.log('onRowMouseDown: ', event);
  }


  onRowMouseUp(event: MouseEvent) {
    if (event.button !== 0) return;
    this.dragging = false;
    this.mouseUp.emit(event);
  }

  @logMethod
  onRowMouseOver(event: MouseEvent, row: any) {
    if (!this.dragging) return;
    this.toggleSelection(row);
    this.mouseOver.emit({ event, row });
  }

  @logMethod
  onRowRightClick(event: MouseEvent, row: any) {
    event.preventDefault();
    this.rightClick.emit({ event, row });
  }



  @logMethod
  isRowSelected(row: any): boolean {
    return this.selectedRoleIds.includes(row['Role ID']);
  }

  @logMethod
  toggleSelection(row: any): void {
    const index = this.selectedRoleIds.indexOf(row['Role ID']);
    if (index > -1) {
      this.selectedRoleIds.splice(index, 1);
    } else {
      this.selectedRoleIds.push(row['Role ID']);
    }
  }

  
  showModal = false;

  @logMethod
  createEntity(entityType: string) {
    if (entityType === 'role') {
      this.popupService.openPopup('role'); // Abre o modal
    }
  }

  @logMethod
  closePopup() {
    this.showModal = false; // Fecha o modal
  }

  
}
