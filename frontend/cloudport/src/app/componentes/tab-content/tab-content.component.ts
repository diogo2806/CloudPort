import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-tab-content',
  template: './tab-content.component.html'
})
export class TabContentComponent {
  @Input() data: any; // Marque a propriedade 'data' como uma entrada
}
