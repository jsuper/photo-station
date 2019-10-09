import { Component, OnInit, Input, ElementRef, IterableDiffers, IterableDiffer, IterableChanges } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { Photo } from 'app/photo.model';

@Component({
  selector: 'app-flex-photo',
  templateUrl: './flex-photo.component.html',
  styleUrls: ['./flex-photo.component.css']
})
export class FlexPhotoComponent implements OnInit {
  items: number[] = [1,2,3,4,5];

  @Input('photos') photos: Photo[];
  @Input('box-space') boxSpace: number = 4;
  @Input('container-padding') containerPadding: number = 0;
  @Input('height') targetHeight: number = 200;

  private iterableDiffer: IterableDiffer<number>;
  private layoutConfig;
  constructor(private el: ElementRef,
    private _iterableDiffers: IterableDiffers) {
    this.iterableDiffer = this._iterableDiffers.find(this.items).create();
  };

  ngDoCheck(): void {
    let changes:IterableChanges<number> = this.iterableDiffer.diff(this.items);
    if (changes) {
      changes.forEachAddedItem(rc => {
        console.log(rc.item);
      }) ;
    }
  }

  ngOnInit() {
    const parent = this.el.nativeElement.parentElement;
    this.layoutConfig = {
      containerWidth: parent.clientWidth,
      targetRowHeight: this.targetHeight,
      boxSpacing: this.boxSpace,
      containerPadding: this.containerPadding,
    }
  }

}
