import { Component, Input, OnInit, Output, EventEmitter } from '@angular/core';
import {TableModule} from 'primeng/table';

@Component({
  selector: 'app-dynamic-table',
  templateUrl: './dynamic-table.component.html',
  styleUrls: ['./dynamic-table.component.css']
})
export class DynamicTableComponent implements OnInit {

  @Input() columns: string[] = [];
  @Input() data: any[] = [];
  @Input() selectedRoleIds: number[] = [];
  @Output() mouseDown = new EventEmitter<any>();
  @Output() mouseUp = new EventEmitter<any>();
  @Output() mouseOver = new EventEmitter<any>();
  @Output() rightClick = new EventEmitter<any>();

  filteredData: any[] = [];

  constructor() { }

  ngOnInit(): void {
    this.filteredData = this.data;
  }

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


  handleKeyUp(event: KeyboardEvent, column: string) {
    const target = event.target as HTMLInputElement;
    console.log("handleKeyUp: "+target.value +"column"+column);
    this.applyFilter(target.value, column);
  }

  
  applyFilter(filterValue: string, column: string) {
    if (!filterValue) {
      this.filteredData = this.data;
    } else {
      filterValue = filterValue.trim(); // Remove whitespace
      filterValue = filterValue.toLowerCase(); // MatTableDataSource defaults to lowercase matches
      this.filteredData = this.data.filter(row => {
        let cellValue = row[column];
        // Check if cellValue is a string, if not convert it to a string
        if (typeof cellValue !== 'string') {
          cellValue = cellValue.toString();
        }
        return cellValue.toLowerCase().includes(filterValue);
      });
    }
    console.log("filterValue: "+filterValue +"column"+column);
  }
  
  

}
