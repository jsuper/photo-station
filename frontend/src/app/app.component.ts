import { Component, ElementRef } from '@angular/core';

import { NavigationService } from "app/navigation/navigation.service";
import { NavigationNode } from './navigation/navigation.model';
import { PhotoDisplayComponent } from './photo-display/photo-display.component';
import { PhotoJustifyDisplayComponent } from './photo-justify-display/photo-justify-display.component';
import { RouteStateService } from './route-state.service';
import { Scrollable } from './scrollable';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'photo-station';

  private lastScrollTop: number = 0;
  currentNavNode: NavigationNode[];
  selectedBlocks: number = 0;

  constructor(private navigationService: NavigationService,
    private routeStateService: RouteStateService,
    private el: ElementRef) {
    navigationService.getNavMenus().subscribe(nodes => this.currentNavNode = nodes);
  }

  onScroll(container: any): void {
    let scrollElementRef: ElementRef = container.elementRef;
    let scrollTop: number = scrollElementRef.nativeElement.scrollTop;
    let direction: string = scrollTop > this.lastScrollTop ? 'DOWN' : 'UP';
    this.lastScrollTop = scrollTop;
    let scrollable: Scrollable = this.routeStateService.getComponent();
    if (scrollable) {
      if (direction === 'DOWN') {
        scrollable.scrollDown(scrollElementRef);
      } else {
        scrollable.scrollUp(scrollElementRef);
      }
    }
  }

  setSelectedBlocks(count: number) {
    this.selectedBlocks = count;
  }
}
