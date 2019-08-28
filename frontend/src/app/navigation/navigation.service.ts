import { Injectable } from '@angular/core';
import { NavigationNode } from './navigation.model';
import { Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class NavigationService {

  private nav: NavigationNode[] = [{
    url: '/',
    title: 'Photos',
    tooltip: 'Show all photos',
    hidden: false
  }, {
    title: 'Albums',
    tooltip: '相册',
    hidden: false,
    children: [{
      url: '/',
      title: 'My Son',
      tooltip: 'All photos of my son',
      hidden: false
    }]
  },
  {
    title: 'Tags',
    tooltip: 'Show all tags',
    hidden: false,
    children: [{
      url: '/',
      title: '铁山坪(40)',
      tooltip: 'Show all photos',
      hidden: false
    }]
  }];

  getNavMenus(): Observable<NavigationNode[]> {
    return of(this.nav);
  }

}
