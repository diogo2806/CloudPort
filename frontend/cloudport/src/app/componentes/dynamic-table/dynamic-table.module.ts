import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgGridModule } from 'ag-grid-angular';
import { DynamicTableComponent } from './dynamic-table.component';

@NgModule({
  declarations: [DynamicTableComponent],
  imports: [CommonModule, AgGridModule],
  exports: [DynamicTableComponent]
})
export class DynamicTableModule {}

