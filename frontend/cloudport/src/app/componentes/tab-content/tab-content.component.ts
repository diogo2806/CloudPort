import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-tab-content',
  templateUrl: './tab-content.component.html'
})
export class TabContentComponent {
  @Input() data: any; // Marque a propriedade 'data' como uma entrada
  
  get content() {
    return this.data ? this.data.message : '';
  }
}
