import { Component, Input, OnInit } from '@angular/core';
import { CurrentNode, NavigationNode } from "app/navigation/navigation.model";
import { MatSidenav } from '@angular/material/sidenav';

@Component({
  selector: 'app-nav-menu',
  template: `
  <app-nav-item [nav]="nav" *ngFor="let node of filteredNodes" [node]="node" [selectedNodes]="currentNode?.nodes" [isWide]="isWide">
  </app-nav-item>`,
})
export class NavMenuComponent implements OnInit {


  @Input() currentNode: CurrentNode;
  @Input() isWide = false;
  @Input() nodes: NavigationNode[];
  @Input() nav: MatSidenav;

  ngOnInit(): void {
    console.log(this.nav);

  }

  get filteredNodes() { return this.nodes ? this.nodes.filter(n => !n.hidden) : []; }

}
