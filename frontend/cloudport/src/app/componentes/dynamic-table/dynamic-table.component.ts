import { Component, Input, OnInit, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';

import { ColDef, GridApi, GridOptions, GridReadyEvent, IDateFilterParams, IMultiFilterParams, ISetFilterParams } from 'ag-grid-community';
import { TabStateService } from './tab-state.service';

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
  @Input() selectedTab: string = '';


  filteredData: any[] = [];
  filters: { [key: string]: string } = {};

  private gridApi!: GridApi;
  columnDefinitions: ColDef[] = [];
  dragging: boolean = false;

  constructor(private tabStateService: TabStateService) { }

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
    if (!event.node.isSelected()) {
      event.node.setSelected(true);
    } else {
      event.node.setSelected(false);
    }
    console.log('Célula clicada:', event);
  }

  onCellDoubleClicked(event: any) {
    console.log('Célula clicada duas vezes:', event);
  }

  onCellRightClicked(event: any) {
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
        // Update the current tab state here
        this.tabStateService.setTabState(this.selectedTab, { filteredData: this.filteredData });
    }
}


  /*
  ngOnChanges(changes: SimpleChanges) {
    if (changes['data']) {
      this.filteredData = [...this.data];
      if (this.gridApi) {
        this.gridApi.setRowData(this.filteredData);
      }
      // Atualize o estado da aba atual aqui
      this.tabStateService.setTabState(this.selectedTab, { filteredData: this.filteredData });
    }
}


/*
  ngOnChanges(changes: SimpleChanges) {
    if (changes['data']) {
      this.filteredData = [...this.data];
      if (this.gridApi) {
        this.gridApi.setRowData(this.filteredData);
      }
    }


  }
*/
  onRowMouseDown(event: MouseEvent, row: any) {
    if (event.button !== 0) return;
    event.preventDefault();
    this.dragging = true;
    this.toggleSelection(row);
    this.mouseDown.emit({ event, row });
  }

  onRowMouseUp(event: MouseEvent) {
    if (event.button !== 0) return;
    this.dragging = false;
    this.mouseUp.emit(event);
  }

  onRowMouseOver(event: MouseEvent, row: any) {
    if (!this.dragging) return;
    this.toggleSelection(row);
    this.mouseOver.emit({ event, row });
  }

  onRowRightClick(event: MouseEvent, row: any) {
    event.preventDefault();
    this.rightClick.emit({ event, row });
  }

  isRowSelected(row: any): boolean {
    return this.selectedRoleIds.includes(row['Role ID']);
  }

  toggleSelection(row: any): void {
    const index = this.selectedRoleIds.indexOf(row['Role ID']);
    if (index > -1) {
      this.selectedRoleIds.splice(index, 1);
    } else {
      this.selectedRoleIds.push(row['Role ID']);
    }
  }
}
