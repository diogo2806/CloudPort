/* dynamic-table.component.ts */
import { ViewChild, ElementRef } from '@angular/core';
import { Component, Input, OnInit, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import * as XLSX from 'xlsx';
import { ColDef, GridApi, GridOptions, GridReadyEvent, IDateFilterParams, IMultiFilterParams, ISetFilterParams } from 'ag-grid-community';
import { TabStateService } from './tab-state.service';


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
export class DynamicTableComponent implements OnInit {

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

  filteredData: any[] = [];
  filters: { [key: string]: string } = {};

 
  columnDefinitions: ColDef[] = [];
  dragging: boolean = false;

  constructor(private tabStateService: TabStateService) {}

  @logMethod
  ngOnInit(): void {
    this.columnDefinitions = this.columns.map(column => ({
      headerName: column,
      field: column,
      filter: true,
      sortable: true
    }));
    this.filteredData = [...this.data];


   document.addEventListener('contextmenu', this.preventRightClickDefault.bind(this));
    document.addEventListener('click', this.closeContextMenu.bind(this));

  }

  @logMethod
  preventRightClickDefault(event: MouseEvent): void {
       // Verifique se o clique foi fora da tabela
       if (!this.gridTable || !this.gridTable.nativeElement || !this.gridTable.nativeElement.contains(event.target)) {
        return;
        //this.rightClick.emit(null); // Emita um evento nulo para fechar o menu
      }
      
      event.preventDefault();
  }

  @logMethod
  ngOnDestroy() {
    document.removeEventListener('contextmenu', this.preventRightClickDefault.bind(this));
  }
  
  @logMethod
  closeContextMenu(event: MouseEvent): void {
    // Verifique se o clique foi fora do menu de contexto e feche-o
    // Você pode adicionar lógica adicional aqui para determinar quando fechar o menu
    this.rightClick.emit(null); // Emita um evento nulo para fechar o menu
  }
  
  
  @logMethod
  onGridReady(params: any) {
    this.gridApi = params.api;

}



@logMethod
onBtExport() {
  try {
    // Obter os nós da linha após a filtragem
    const nodes = this.gridApi.getRenderedNodes();

    // Extrair os dados de cada nó
    const dados = nodes.map(node => node.data);

    // Preparar as colunas para exportação
    const columns = this.columns.map(column => ({ [column]: '' }));
    const exportData = dados.length ? dados : columns;

    // Usar a biblioteca XLSX para exportar os dados
    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(exportData);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Sheet1');
    XLSX.writeFile(wb, 'data.xlsx');
    console.log("onBtExport: Exportação bem-sucedida");
  } catch (error) {
    // Tratar erro
  }
}





  @logMethod
  onCellClicked(event: any) {
    if (!event.node.isSelected()) {
     // event.node.setSelected(true);
    } else {
      //event.node.setSelected(false);
    }
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

  @logMethod
  onCellDoubleClicked(event: any) {
    console.log('onCellDoubleClicked: Célula clicada com o botão direito:', event);
  }

  @logMethod
  onCellRightClicked(event: any) {


    console.log('onCellRightClicked: Emitindo evento de clique com o botão direito do mouse')
    const mouseEvent = event.event as MouseEvent;
    mouseEvent.preventDefault(); // Previne o menu de contexto padrão dentro da tabela
    if (mouseEvent.button !== 2) return; // Ignora se não for o botão direito do mouse
    const row = event.data; // Acessa os dados da linha clicada
    console.log('Emitindo evento de clique com o botão direito do mouse', { event: mouseEvent, row }); // Depuração
    this.rightClick.emit({ event: mouseEvent, row });

    /*
    const mouseEvent = event.event as MouseEvent;
    mouseEvent.preventDefault(); // Previne o menu de contexto padrão dentro da tabela
    if (mouseEvent.button !== 2) return; // Ignora se não for o botão direito do mouse
    const row = event.data; // Acessa os dados da linha clicada
    console.log('Emitindo evento de clique com o botão direito do mouse', { event: mouseEvent, row }); // Depuração
    this.rightClick.emit({ event: mouseEvent, row });

    */
    /*
    const mouseEvent = event.event as MouseEvent;
    if (mouseEvent.button !== 2) return; // Ignora se não for o botão direito do mouse
    const row = event.data; // Acessa os dados da linha clicada
    console.log('Emitindo evento de clique com o botão direito do mouse', { event: mouseEvent, row }); // Depuração
    this.rightClick.emit({ event: mouseEvent, row });*/
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
  @logMethod
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
  
  @logMethod
  onRowMouseDown(event: MouseEvent, row: any) {
    if (event.button !== 0) return;
    event.preventDefault();
    this.dragging = true;
    this.toggleSelection(row);
    this.mouseDown.emit({ event, row });
    console.log('onRowMouseDown: ', event);
  }

  @logMethod
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
}
