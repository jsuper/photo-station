import { Component, OnInit } from '@angular/core';
import { SectionService } from 'app/sections/section.service';

@Component({
  selector: 'app-selected-bar',
  templateUrl: './selected-bar.component.html',
  styleUrls: ['./selected-bar.component.css']
})
export class SelectedBarComponent implements OnInit {

  constructor(private sectionService: SectionService) { }

  ngOnInit() {
  }

  hasSelectedBlocks(): boolean {
    return this.sectionService.numberOfSelectedBlocks() > 0;
  }

  selectedBlocks(): number {
    return this.sectionService.numberOfSelectedBlocks();
  }

  clearSelections():void {
    this.sectionService.clearSelections();
  }

}
