import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxQrcodeComponent } from './ngx-qrcode.component';

@NgModule({
  declarations: [NgxQrcodeComponent],
  imports: [CommonModule],
  exports: [NgxQrcodeComponent]
})
export class QRCodeModule {}
