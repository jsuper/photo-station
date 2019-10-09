import { Component, OnInit, Input, ElementRef, IterableDiffers, IterableDiffer, IterableChanges } from '@angular/core';
import { FlexConfig, Box, Segment, FlexLayout } from 'app/flex-layout/flex-layout.model';
import { FlexLayoutService } from 'app/flex-layout/flex-layout.service';

// @Component({
//   selector: 'flex-box-render',
//   template:`<ng-content></ng-content>`
// })
// export class BoxRenderComponent {

// }

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
    //watch data changed
    this.differ = this.iterable.find(this.data).create();
  }

  ngOnInit() {
    const parent = this.el.nativeElement.parentElement;
    if (this.containerWidth == -1) {
      this.containerWidth = parent.clientWidth;
    }
    console.log(parent);

    console.log(`container width: ${this.containerWidth}`);

    this.config = {
      containerPadding: this.containerPadding,
      containerWidth: this.containerWidth,
      targetRowHeight: this.boxHeight,
      boxSpacing: this.boxMargin,
    };
    this.flexLayoutService.updateConfig(this.config);
  }

  ngDoCheck(): void {
    let changes: IterableChanges<Segment> = this.differ.diff(this.data);
    if (changes) {
      console.log('Detach data changed...');
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
    let layout: FlexLayout = this.flexLayoutService.layout(calcBoxes);

    let lastHeight: number = 0;
    if (lastRowBoxes.length) {
      lastHeight = lastRowBoxes[0].height;
    }
    this.totalHeight += Math.floor(layout.containerHeight) + lastHeight + this.boxMargin * 2;
    let baseTop: number = lastRowBoxes.length > 0 ? lastRowBoxes[lastRowBoxes.length - 1].top : this.containerPadding;
    if (lastRowBoxes.length > 0) {
      this.rows--;
    }
    console.log(layout.boxes);
    
    layout.boxes.forEach((box, index) => {
      if (box.left == this.containerPadding) {
        this.rows++;
      }
      let cb: Box = new Box();
      cb.height = Math.floor(box.height);
      cb.left = Math.floor(box.left);
      cb.top = Math.floor(box.top) + baseTop;
      cb.width = Math.floor(box.width);
      cb.row = this.rows;

      if (index < lastRowBoxes.length) {
        lastRowBoxes[index].width = cb.width;
        lastRowBoxes[index].height = cb.height;
        lastRowBoxes[index].left = cb.left;
        lastRowBoxes[index].top = cb.top;
        lastRowBoxes[index].row = cb.row;
      } else {
        let nb = segment.boxes[index - lastRowBoxes.length];
        cb.raw = nb.raw;
        this.boxes.push(cb);
      }
    });
  }

  private getLastRowBoxes(): Box[] {
    let lastRowBoxes: Box[] = [];
    for (let i = this.boxes.length - 1; i > 0; i--) {
      let box: Box = this.boxes[i];
      if (box.row == this.rows) {
        lastRowBoxes.unshift(box);
      } else {
        break;
      }
    }
    return lastRowBoxes;
  }

}
