import { Component, Input, Output, EventEmitter, ElementRef } from '@angular/core';

@Component({
  selector: 'app-context-menu',
  templateUrl: './context-menu.component.html',
  styleUrls: ['./context-menu.component.css']
})
export class ContextMenuComponent {
  @Output() optionSelected = new EventEmitter<string>();
  @Input() position = { x: 0, y: 0 };
  @Input() isOpen = false;
  @Input() selectedRole = null;
  @Input() menuOptions: string[] = [];

  constructor(public elementRef: ElementRef) {}
}
