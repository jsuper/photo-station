import { Component, ElementRef } from '@angular/core';

import { NavigationService } from "app/navigation/navigation.service";
import { NavigationNode } from './navigation/navigation.model';
import { PhotoDisplayComponent } from './photo-display/photo-display.component';
import { PhotoJustifyDisplayComponent } from './photo-justify-display/photo-justify-display.component';
import { RouteStateService } from './route-state.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'photo-station';

  currentNavNode: NavigationNode[];

  constructor(private navigationService: NavigationService,
    private routeStateService: RouteStateService,
    private el: ElementRef) {
    navigationService.getNavMenus().subscribe(nodes => this.currentNavNode = nodes);
  }

  onScroll(container: any): void {
    let scrollElementRef:ElementRef = container.elementRef;

    // let scrollTop = navContent.scrollTop;
    // let scrollHeight = navContent.scrollHeight;
    // let offsetHeight = navContent.offsetHeight;

    // console.log(scrollHeight - scrollTop);

    // console.log(Math.ceil(offsetHeight / (scrollHeight - scrollTop) * 100));



    let componentInstance = this.routeStateService.getComponent();
    if (componentInstance) {
      let onParentScroll = componentInstance['onParentScroll'];
      if (typeof onParentScroll === 'function') {
        componentInstance['onParentScroll'](scrollElementRef);
      }
    }
  }
}
