import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class RouteStateService {

  private component: object;
  constructor() { }

  setComponent(component: object): void {
    this.component = component;
  }

  getComponent(): object {
    return this.component;
  }
}
