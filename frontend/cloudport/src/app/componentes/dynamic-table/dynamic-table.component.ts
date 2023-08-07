import { Component, Input, OnInit, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { ColDef, GridApi, GridOptions ,
  GridReadyEvent,
  IDateFilterParams,
  IMultiFilterParams,
  ISetFilterParams, } from 'ag-grid-community';

@Component({
  selector: 'app-dynamic-table',
  templateUrl: './dynamic-table.component.html',
  styleUrls: ['./dynamic-table.component.css']
})
export class DynamicTableComponent implements OnInit, OnChanges {

  @Input() columns: string[] = [];
  @Input() data: any[] = [];
  @Input() selectedRoleIds: number[] = [];
  @Output() mouseDown = new EventEmitter<any>();
  @Output() mouseUp = new EventEmitter<any>();
  @Output() mouseOver = new EventEmitter<any>();
  @Output() rightClick = new EventEmitter<any>();

  filteredData: any[] = [];
  filters: { [key: string]: string } = {};

  private gridApi!: GridApi;
  columnDefinitions: ColDef[] = [];
  dragging: boolean = false;

  constructor() { }

  ngOnInit(): void {
    this.columnDefinitions = this.columns.map(column => ({
      headerName: column,
      field: column,
      filter: true,
      sortable: true
    }));
    this.filteredData = [...this.data];
  }

  onGridReady(params: any) {
    this.gridApi = params.api;
  }

  onCellClicked(event: any) {
    // Implemente a lógica que você deseja quando uma célula for clicada
    if(event.node.getSelected()==false){
      event.node.setSelected(true);
    } else {
      event.node.setSelected(false);
    }
    console.log('Célula clicada:', event);
  }

  onCellDoubleClicked(event: any) {
    // Implemente a lógica que você deseja quando uma célula for clicada duas vezes
    console.log('Célula clicada duas vezes:', event);
  }

  onCellRightClicked(event: any) {
    // Implemente a lógica que você deseja quando uma célula for clicada com o botão direito do mouse
    console.log('Célula clicada com o botão direito:', event);
  }

  public defaultColDef: ColDef = {
    flex: 1,
    minWidth: 200,
    resizable: true,
    floatingFilter: true,
    menuTabs: ['filterMenuTab'],
  };

  


  ngOnChanges(changes: SimpleChanges) {
    if (changes['data']) {
      this.filteredData = [...this.data];
      if (this.gridApi) {
        this.gridApi.setRowData(this.filteredData);
      }
    }
  }

  onRowMouseDown(event: MouseEvent, row: any) {
    if (event.button !== 0) {
      return; // If it's not the left button, return
    }
    event.preventDefault();
    this.dragging = true;
    this.toggleSelection(row);
    this.mouseDown.emit({event, row});
  }
  
  onRowMouseUp(event: MouseEvent) {
    if (event.button !== 0) {
      return; // If it's not the left button, return
    }
    this.dragging = false;
    this.mouseUp.emit(event);
  }
  
  onRowMouseOver(event: MouseEvent, row: any) {
    if (!this.dragging) {
      return; // If the user is not dragging, return
    }
    this.toggleSelection(row);
    this.mouseOver.emit({event, row});
  }
  
  onRowRightClick(event: MouseEvent, row: any) {
    event.preventDefault();
    this.rightClick.emit({event, row});
  }
  
  isRowSelected(row: any) {
    return this.selectedRoleIds.includes(row['Role ID']);
  }
  
  toggleSelection(row: any) {
    const index = this.selectedRoleIds.indexOf(row['Role ID']);
    
    if (index > -1) {
      // If the ID is already in the array, remove it
      this.selectedRoleIds.splice(index, 1);
    } else {
      // If the ID is not in the array, add it
      this.selectedRoleIds.push(row['Role ID']);
    }
  }
  
  
  

}
