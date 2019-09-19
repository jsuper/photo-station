import { Injectable } from '@angular/core';
import { NavigationNode } from './navigation.model';
import { Observable, of, merge } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { mapTo, map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class NavigationService {

  private nav_agg = "api/nav-agg";
  private homeNode: NavigationNode = {
    url: '/photos',
    title: 'Photos',
    tooltip: 'Show all photos',
    hidden: false,
    params: { 'q': '' }
  };
  private favoriteNode: NavigationNode = {
    url: '/photos/favorite',
    title: 'Favorite',
    tooltip: 'Show all photos',
    hidden: false,
    params: { 'q': '1' }
  };

  private aggMenus = {
    'albums': {
      title: 'Albums',
      tooltip: 'Photo albums.',
      hidden: false,
    },
    'tags': {
      title: 'Tags',
      tooltip: 'All tags in photos',
      hidden: false,
    },
    "date": {
      title: 'Date & Time',
      tooltip: 'Shooting date',
      hidden: false,
      field: 'year'
    }
  };
  private aggregations: Object;
  constructor(private http: HttpClient) {

  }



  getNavMenus(): Observable<NavigationNode[]> {
    const menu = this.getBaseAggregation().pipe(map(val => {
      let dynamicNodes: NavigationNode[] = [];
      dynamicNodes.push(this.homeNode);

      for (let agg in val) {
        let node: NavigationNode = this.aggMenus[agg];
        let aggVal = Object.keys(val[agg]);
        if (node && aggVal.length) {
          let children: NavigationNode[] = [];
          let aggregation = val[agg];

          for (let idx in aggregation) {
            let item = aggregation[idx];
            children.push({
              url: '/photos/' + (node.field || agg),
              title: item['value'] + '(' + item['counter'] + ')',
              tooltip: item['value'],
              hidden: false,
              params: { "q": item['value'] }
            })
          }
          node.children = children;
          dynamicNodes.push(node);
        }
      }
      dynamicNodes.push(this.favoriteNode);
      return dynamicNodes;
    }));
    return menu;
  }

  getBaseAggregation(): Observable<Object> {
    return this.http.get<Object>(this.nav_agg);
  }
}
