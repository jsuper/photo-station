import { Component, OnInit, Input, ElementRef, IterableDiffers, IterableDiffer, IterableChanges } from '@angular/core';
import { FlexConfig, Box, Segment, FlexLayout } from 'app/flex-layout/flex-layout.model';
import { FlexLayoutService } from 'app/flex-layout/flex-layout.service';

@Component({
  selector: 'app-flex-box',
  templateUrl: './flex-box.component.html',
  styleUrls: ['./flex-box.component.css']
})
export class FlexBoxComponent implements OnInit {

  @Input('data') data: Segment[] = [];
  @Input('box-margin') boxMargin: number = 4;
  @Input('padding') containerPadding: number = 0;
  @Input('box-height') boxHeight: number = 200;
  @Input('container-width') containerWidth: number = -1;

  private config: FlexConfig;
  private flexLayoutService: FlexLayoutService = new FlexLayoutService({});
  private differ: IterableDiffer<any>;

  private boxes: Box[] = [];

  totalHeight: number = 0;
  rows: number = 0;

  constructor(private el: ElementRef,
    private iterable: IterableDiffers) {

  }

  ngOnInit() {
    const parent = this.el.nativeElement.parentElement;
    if (this.containerWidth == -1) {
      this.containerWidth = parent.clientWidth;
    }

    this.config = {
      containerPadding: this.containerPadding,
      containerWidth: this.containerWidth,
      targetRowHeight: this.boxHeight,
      boxSpacing: this.boxMargin,
    };
    this.flexLayoutService.updateConfig(this.config);

    if (this.data.length > 0) {
      let box: Box[] = [];
      this.data.forEach(seg => {
        if (seg.boxes && seg.boxes.length) {
          seg.boxes.forEach(b => box.push(b));
        }
      });

      let layout: FlexLayout = this.flexLayoutService.layout(box);
      this.totalHeight += Math.floor(layout.containerHeight);

      layout.boxes.forEach(box => {
        let cb: Box = new Box();
        cb.height = Math.floor(box.height);
        cb.left = Math.floor(box.left);
        cb.top = Math.floor(box.top);
        cb.width = Math.floor(box.width);
        if (cb.left == this.containerPadding) {
          this.rows++;
        }
        cb.row = this.rows;
      });
    }
    //watch data changed
    this.differ = this.iterable.find(this.data).create();
  }

  ngDoCheck(): void {
    //Called every time that the input properties of a component or a directive are checked. Use it to extend change detection by performing a custom check.
    //Add 'implements DoCheck' to the class.
    let changes: IterableChanges<Segment> = this.differ.diff(this.data);
    if (changes) {
      changes.forEachAddedItem(record => {
        this.fireNewSegmentAdded(record.item);
      });
    }
  }

  private addBoxTo(addTo: Box[], add: Box[]) {
    add.forEach(box => {
      let w: number = box.raw.width;
      let h: number = box.raw.height;
      let t: Box = new Box();
      t.width = w;
      t.height = h;
      addTo.push(t);
    });
  }

  private fireNewSegmentAdded(segment: Segment) {
    let lastRowBoxes: Box[] = this.getLastRowBoxes();
    let calcBoxes: Box[] = [];
    this.addBoxTo(calcBoxes, lastRowBoxes);
    this.addBoxTo(calcBoxes, segment.boxes);
  }

  private getLastRowBoxes(): Box[] {
    let lastRowBoxes: Box[] = [];
    for (let i = this.boxes.length - 1; i > 0; i--) {
      let box: Box = this.boxes[i];
      if (box.row == this.rows) {
        lastRowBoxes.push(box);
      }
    }
    return lastRowBoxes;
  }

}
