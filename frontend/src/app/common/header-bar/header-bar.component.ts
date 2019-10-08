import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-header-bar',
  templateUrl: './header-bar.component.html',
  styleUrls: ['./header-bar.component.css']
})
export class HeaderBarComponent implements OnInit {

  @Input('show-shadow') showShadow: boolean = true;
  @Input('visiable') visiable: boolean = true;
  constructor() { }

  ngOnInit() {
  }

}
