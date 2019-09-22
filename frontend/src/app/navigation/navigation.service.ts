import { Injectable } from '@angular/core';
import { NavigationNode, AggregationItem } from './navigation.model';
import { Observable, of, merge } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { mapTo, map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class NavigationService {

  private nav_agg = 'api/nav-agg';
  private homeNode: NavigationNode = {
    url: '/photos',
    title: 'Photos',
    tooltip: 'Show all photos',
    hidden: false,
    params: { 'q': '' }
  };
  private favoriteNode: NavigationNode = {
    url: '/photos/favorite',
    title: 'Favorites',
    tooltip: 'All your favorite photos',
    hidden: false,
    params: { 'q': '1' }
  };

  private groupList: NavigationNode = {
    url: '/groups',
    title: 'Group List',
    tooltip: 'Show photos group by day',
    hidden: false,
    params: { 'q': '1' }
  };

  private aggMenus: Map<String, NavigationNode> = new Map([
    ['albums', {
      title: 'Albums',
      tooltip: 'Photo albums',
      hidden: false,
    }], [
      'tags', {
        title: 'Tags',
        tooltip: 'All tags in photos',
        hidden: false,
      }], [
      "date", {
        title: 'Date & Time',
        tooltip: 'Shooting date',
        hidden: false,
        field: 'year'
      }]
  ]);
  constructor(private http: HttpClient) {
  }

  getNavMenus(): Observable<NavigationNode[]> {
    const menu = this.getBaseAggregation().pipe(map(val => {
      let dynamicNodes: NavigationNode[] = [];
      dynamicNodes.push(this.homeNode);
      Object.keys(val).forEach(aggName => {
        const aggNode: NavigationNode = this.aggMenus.get(aggName);
        const aggregations: Array<AggregationItem> = val[aggName];
        const children: NavigationNode[] = [];
        aggregations.forEach(aggregation => {
          children.push({
            url: '/photos/' + (aggNode.field || aggName),
            title: aggregation.value + '(' + aggregation.counter + ')',
            tooltip: aggNode.tooltip + ' of ' + aggregation.counter,
            hidden: false,
            params: { "q": aggregation.value }
          });
        });
        aggNode.children = children;
        dynamicNodes.push(aggNode);
      });
      dynamicNodes.push(this.favoriteNode);
      dynamicNodes.push(this.groupList);
      return dynamicNodes;
    }));
    return menu;
  }

  getBaseAggregation(): Observable<Object> {
    return this.http.get<Object>(this.nav_agg);
  }
}
