import { Component } from '@angular/core';

import { NavigationService } from "app/navigation/navigation.service";
import { NavigationNode } from './navigation/navigation.model';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'photo-station';

  currentNavNode: NavigationNode[];

  constructor(private navigationService: NavigationService) {
    navigationService.getNavMenus().subscribe(nodes => this.currentNavNode = nodes);
    console.log(this.currentNavNode);
  }
}
