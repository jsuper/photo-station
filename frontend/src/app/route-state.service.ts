import { Injectable } from '@angular/core';
import { Scrollable } from './scrollable';

@Injectable({
  providedIn: 'root'
})
export class RouteStateService {

  private component: Scrollable;
  constructor() { }

  setComponent(component: Scrollable): void {
    this.component = component;
  }

  getComponent(): Scrollable {
    return this.component;
  }
}
