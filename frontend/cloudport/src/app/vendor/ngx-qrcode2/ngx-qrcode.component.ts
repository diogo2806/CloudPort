import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'ngx-qrcode',
  template: `
    <ng-container [ngSwitch]="elementType">
      <img *ngSwitchCase="'img'" [src]="qrcValue" [alt]="altText" class="ngx-qrcode__img" />
      <figure *ngSwitchDefault class="ngx-qrcode__fallback">{{ qrcValue }}</figure>
    </ng-container>
  `,
  styles: [
    `
      :host {
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }
      .ngx-qrcode__img {
        max-width: 100%;
        height: auto;
        image-rendering: pixelated;
      }
      .ngx-qrcode__fallback {
        margin: 0;
        font-family: monospace;
        font-size: 0.75rem;
        color: #ef4444;
      }
    `
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NgxQrcodeComponent {
  @Input('qrc-value') qrcValue = '';
  @Input('qrc-element-type') elementType: 'img' | 'url' | 'text' = 'img';
  @Input('alt') altText = 'QR Code';
}
