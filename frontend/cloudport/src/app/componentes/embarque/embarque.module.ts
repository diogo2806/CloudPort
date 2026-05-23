import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EmbarqueRoutingModule } from './embarque-routing.module';
import { PlanejamentoEmbarqueComponent } from './planejamento-embarque/planejamento-embarque.component';

@NgModule({
  declarations: [PlanejamentoEmbarqueComponent],
  imports: [CommonModule, FormsModule, EmbarqueRoutingModule]
})
export class EmbarqueModule { }
