import { Component, ElementRef, Input, ViewChild, OnInit } from '@angular/core';

import { RouteStateService } from './route-state.service';
import { Scrollable } from './scrollable';
import { SectionService } from './sections/section.service';
import { AlbumService } from './services/album.service';
import { Router } from '@angular/router';
import { MatSidenav } from '@angular/material/sidenav';
import { MenuGroup, MENUS } from './app.model';
import { MatButton } from '@angular/material/button';
import { PhotoUploaderComponent } from './photo-uploader/photo-uploader.component';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  ngOnInit(): void {
  }
  title = 'photo-station';

  private lastScrollTop: number = 0;
  currentUrl: string;

  menus: MenuGroup[] = MENUS;
  @ViewChild('sidenav', { static: false }) nav: MatSidenav;
  @ViewChild('menuToggle', { static: false }) menuToggle: MatButton;

  showToolbarShadow: boolean = false;

  constructor(
    private routeStateService: RouteStateService,
    private sectionService: SectionService,
    private albumService: AlbumService,
    private router: Router,
    private _el: ElementRef) {
    let redirect = this._el.nativeElement.getAttribute('redirect');
    console.log(`Redirect to uri: ${redirect}`);


    this.albumService.loadAllAlbums();
  }

  onScroll(container: any): void {
    let scrollElementRef: ElementRef = container.elementRef;
    let scrollTop: number = scrollElementRef.nativeElement.scrollTop;
    this.showToolbarShadow = scrollTop > 0;
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
    this.onToolbarClick(this.nav);
    this.menuToggle._elementRef.nativeElement.blur();
  }

  onMenuItemClick(sideNav: MatSidenav) {
    sideNav.toggle();
  }

  onToolbarClick(sideNav: MatSidenav) {
    if (sideNav && sideNav.opened) {
      sideNav.toggle();
    }
  }

  toggleSideNav(event): void {
    let button = event.target;
    if (this.nav) {
      this.nav.toggle();
    }
    button.blur();
    event.stopPropagation();
  }

  firePhotoChosen(fileInput: any, uploader: PhotoUploaderComponent) {
    let uploadQueue: File[] = [];
    let files = <FileList>fileInput.target.files;
    for (let i = 0; i < files.length; i++) {
      uploadQueue.push(files[i]);
    }
    if (uploadQueue.length) {
      uploader.uploadFiles(uploadQueue);
    }
  }
}
