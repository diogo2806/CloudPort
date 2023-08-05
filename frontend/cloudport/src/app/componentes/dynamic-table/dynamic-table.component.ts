import { Component, Input, OnInit, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';


@Component({
  selector: 'app-dynamic-table',
  templateUrl: './dynamic-table.component.html',
  styleUrls: ['./dynamic-table.component.css']
})
export class DynamicTableComponent implements OnInit, OnChanges  {

 
  @Input() columns: string[] = [];
  @Input() data: any[] = [];
  @Input() selectedRoleIds: number[] = [];
  @Output() mouseDown = new EventEmitter<any>();
  @Output() mouseUp = new EventEmitter<any>();
  @Output() mouseOver = new EventEmitter<any>();
  @Output() rightClick = new EventEmitter<any>();

  filteredData: any[] = [];
  filters: { [key: string]: string } = {}; // New property to keep track of filters

  dragging: boolean = false; // New property to track if the user is dragging the mouse

  constructor() { }

  ngOnInit(): void {
    this.filteredData = [...this.data];
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['data']) {
      this.filteredData = [...this.data];
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

  /*
  onRowMouseDown(event: MouseEvent, row: any) {
    this.mouseDown.emit({event, row});
  }

  onRowMouseUp(event: MouseEvent) {
    this.mouseUp.emit(event);
  }

  onRowMouseOver(event: MouseEvent, row: any) {
    this.mouseOver.emit({event, row});
  }

  onRowRightClick(event: MouseEvent, row: any) {
    event.preventDefault();
    this.rightClick.emit({event, row});
  }

  isRowSelected(row: any) {
    return this.selectedRoleIds.includes(row['Role ID']);
  }
*/

  handleKeyUp(event: KeyboardEvent, column: string) {
    const target = event.target as HTMLInputElement;
    this.filters[column] = target.value; // Save the filter value
    this.applyFilters(); // Apply all filters
  }

  applyFilters() {
    let filteredData = [...this.data];
    for (const column of this.columns) {
      const filterValue = this.filters[column];
      if (filterValue) {
        filteredData = filteredData.filter(row => {
          let cellValue = row[column];
          if (typeof cellValue !== 'string') {
            cellValue = cellValue.toString();
          }
          return cellValue.toLowerCase().includes(filterValue.toLowerCase());
        });
      }
    }
    this.filteredData = filteredData;
  }
  
  

}
