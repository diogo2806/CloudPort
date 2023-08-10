// tab-content.component.ts
import { Component, OnInit } from '@angular/core';
import { TabService } from '../navbar/TabService';

@Component({
  selector: 'app-tab-content',
  template: '<div>{{ data }}</div>'
})
export class TabContentComponent implements OnInit {
  data: any;

  constructor(private tabService: TabService) {}

  ngOnInit() {
    this.tabService.content$.subscribe(content => {
      this.data = content;
    });
  }
}
