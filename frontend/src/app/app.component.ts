import { Component, ElementRef } from '@angular/core';

import { RouteStateService } from './route-state.service';
import { Scrollable } from './scrollable';
import { SectionService } from './sections/section.service';
import { AlbumService } from './services/album.service';
import { Router } from '@angular/router';
import { MatSidenav } from '@angular/material/sidenav';
import { MenuGroup, MENUS } from './app.model';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'photo-station';

  private lastScrollTop: number = 0;
  currentUrl: string;

  menus: MenuGroup[] = MENUS;

  constructor(
    private routeStateService: RouteStateService,
    private sectionService: SectionService,
    private albumService: AlbumService,
    private router: Router, ) {
    this.albumService.loadAllAlbums();
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

  onRouteActivate(event) {
    this.currentUrl = decodeURIComponent(this.router.url);
  }

  onToolbarClick(sideNav: MatSidenav) {
    if (sideNav.opened) {
      sideNav.toggle();
    }
  }
}
