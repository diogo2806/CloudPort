import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { AgGridModule } from 'ag-grid-angular';
import { DynamicTableComponent } from './dynamic-table.component';
import { ModalComponent } from '../modal/modal.component';
import { ConfirmacaoModalComponent } from '../modal/confirmacao-modal/confirmacao-modal.component';
import { RoleCadastroComponent } from '../role/role-cadastro/role-cadastro.component';

@NgModule({
  declarations: [
    DynamicTableComponent,
    ModalComponent,
    ConfirmacaoModalComponent,
    RoleCadastroComponent
  ],
  imports: [CommonModule, ReactiveFormsModule, FormsModule, AgGridModule],
  exports: [DynamicTableComponent, ModalComponent]
})
export class DynamicTableModule {}
