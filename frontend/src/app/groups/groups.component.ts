import { Component, OnInit, ElementRef } from '@angular/core';
import { Box } from 'app/flex-layout/flex-layout.model';
import { FlexLayoutService } from 'app/flex-layout/flex-layout.service';

@Component({
  selector: 'app-groups',
  templateUrl: './groups.component.html',
  styleUrls: ['./groups.component.css']
})
export class GroupsComponent implements OnInit {

  constructor(private el: ElementRef) { }

  ngOnInit() {


  }

}
