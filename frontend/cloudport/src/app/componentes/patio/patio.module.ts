import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MapaPatioComponent } from './mapa-patio/mapa-patio.component';
import { PatioRoutingModule } from './patio-routing.module';

@NgModule({
  declarations: [MapaPatioComponent],
  imports: [CommonModule, ReactiveFormsModule, PatioRoutingModule]
})
export class PatioModule { }
