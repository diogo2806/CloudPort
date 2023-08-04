import { Component, Input, OnInit, Output, EventEmitter } from '@angular/core';

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

  constructor() { }

  ngOnInit(): void {
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
}
