import { Component, ElementRef } from '@angular/core';

import { NavigationService } from "app/navigation/navigation.service";
import { NavigationNode } from './navigation/navigation.model';
import { RouteStateService } from './route-state.service';
import { Scrollable } from './scrollable';
import { SectionService } from './sections/section.service';
import { AlbumService } from './services/album.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'photo-station';

  private lastScrollTop: number = 0;
  currentNavNode: NavigationNode[];

  constructor(private navigationService: NavigationService,
    private routeStateService: RouteStateService,
    private el: ElementRef,
    private sectionService: SectionService,
    private albumService: AlbumService) {
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

  hasBlockSelected(): boolean {
    return this.sectionService.numberOfSelectedBlocks() > 0;
  }
}
