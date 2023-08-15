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
  }


  
  @logMethod
  onGridReady(params: any) {
    this.gridApi = params.api;

}


@logMethod
onBtExport() {
  try {
    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(this.filteredData);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Sheet1');
    XLSX.writeFile(wb, 'data.xlsx');
    console.log("onBtExport: Exportação bem-sucedida");
  } catch (error) {
    console.error("Erro ao exportar para Excel:", error);
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
   // console.log('Célula clicada duas vezes:', event);
  }

  @logMethod
  onCellRightClicked(event: any) {
    //console.log('Célula clicada com o botão direito:', event);
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
