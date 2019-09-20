import { Component, OnInit, ElementRef } from '@angular/core';
import * as justifiedLayout from 'justified-layout'
import { PhotoService } from 'app/photo.service';
import { ActivatedRoute } from '@angular/router';
import { RouteStateService } from 'app/route-state.service';
import { start } from 'repl';
import { Photo } from 'app/photo.model';
import { ThrowStmt } from '@angular/compiler';

class BoxModel {
  width: number;
  height: number;
  id: string;
  top: number;
  left: number;
  imgWidth: number;
  imgHeight: number;

  constructor(width: number, height: number, top: number, left: number) {
    this.width = width;
    this.height = height;
    this.left = left;
    this.top = top;
  }
}


@Component({
  selector: 'app-photo-justify-display',
  templateUrl: './photo-justify-display.component.html',
  styleUrls: ['./photo-justify-display.component.css']
})
export class PhotoJustifyDisplayComponent implements OnInit {
  readonly pageSize: number = 15;

  boxes: Array<BoxModel>;
  lastMaxScrollTop: number = 0;
  lastScrollTop: number = 0;
  scrollDirection: number = 0; //滚动方向，默认向下，1为向上

  hasMorePage: boolean = true;
  loaded: number = 0;
  justifyOption = {
    containerWidth: window.innerWidth - 10,
    targetRowHeight: 220
  };
  loading: boolean = false;
  constructor(private photoService: PhotoService,
    private routeStateService: RouteStateService) { }

  ngOnInit() {

    this.routeStateService.setComponent(this);
    this.loading = true;
    this.photoService.search(0, this.pageSize, '').subscribe(resp => {
      this.loading = false;
      this.hasMorePage = resp.length === this.pageSize;
      this.loaded = resp.length;
      let wh = resp.map(photo => {
        return { width: photo.width, height: photo.height };
      });
      let justify = justifiedLayout(wh, this.justifyOption);
      this.boxes = justify.boxes.map(box => {
        return new BoxModel(Math.floor(box.width), Math.floor(box.height), Math.floor(box.top), Math.floor(box.left));
      });
      this.boxes.forEach((val, idx) => {
        val.id = resp[idx].id;
        val.imgWidth = resp[idx].width;
        val.imgHeight = resp[idx].height;
      });
    });
  }

  getLastNotNotBox(): BoxModel[] {
    const containerWidth = window.innerWidth - 10;
    let calcBoxes: BoxModel[] = [];
    let boxTotalWidth: number = 0;
    for (let i = this.boxes.length - 1; i > 0; i--) {
      let box: BoxModel = this.boxes[i];
      boxTotalWidth += box.width;
      calcBoxes.unshift(box);
      if (this.boxes[i].left == 0) {
        break;
      }
    }
    boxTotalWidth += ((calcBoxes.length - 1) * 10);
    if((boxTotalWidth/containerWidth)>0.8) {
      return [] ;
    }
    return calcBoxes;
  }

  loadNextPage(): void {
    if (this.hasMorePage) {
      this.loading = true;
      let lastTop: number = this.boxes[this.boxes.length - 1].top;
      let calcBoxes: Array<Object> = [];

      let numOfRecalcEles: number = 0;
      let boxIndex: number = 0;
      for (let i = this.boxes.length - 1; i > 0; i--) {
        let box: BoxModel = this.boxes[i];
        calcBoxes.unshift({ width: box.imgWidth, height: box.imgHeight });
        numOfRecalcEles++;
        if (box.left === 10) {
          boxIndex = i;
          break;
        }
      };


      this.photoService.search(this.loaded, this.pageSize, '').subscribe(resp => {
        this.loading = false;
        this.hasMorePage = resp.length == this.pageSize;
        this.loaded += resp.length;
        resp.forEach(photo => {
          calcBoxes.push({
            width: photo.width,
            height: photo.height,
          })
        });
        let justify = justifiedLayout(calcBoxes, this.justifyOption).boxes;
        console.log(justify);

        let addTop = 0;
        for (let i = 0; i < justify.length; i++) {

          let nb = justify[i];
          let width = Math.floor(nb.width);
          let height = Math.floor(nb.height);
          let top = Math.floor(nb.top);
          let left = Math.floor(nb.left);
          if (i < numOfRecalcEles) {
            let box: BoxModel = this.boxes[boxIndex];
            box.height = height;
            box.width = width;
          } else {
            if (left == 10) {
              let box: BoxModel = this.boxes[this.boxes.length - 1];
              console.log(box);
              addTop = box.top;
            }
            let box: BoxModel = new BoxModel(width, height, top + addTop, left);
            let photo: Photo = resp[i - numOfRecalcEles];
            box.id = photo.id;
            box.imgHeight = photo.height;
            box.imgWidth = photo.width;
            this.boxes.push(box);
          }
        }
      });
    }
    console.log("loading next page of data.");
  }

  /**
   * Called once parent container scrolled.
   *
   * @param el Scroll container element
   */
  onParentScroll(el: ElementRef): void {
    const nativeEl = el.nativeElement;
    const scrollTop: number = nativeEl.scrollTop;
    const scrollHeight: number = nativeEl.scrollHeight;
    const offsetHeight: number = nativeEl.offsetHeight;

    this.lastMaxScrollTop = Math.max(scrollTop, this.lastMaxScrollTop);
    this.scrollDirection = scrollTop > this.lastScrollTop ? 0 : 1;
    this.lastScrollTop = scrollTop;

    if (scrollTop < this.lastMaxScrollTop) {
      return;
    }

    const scrollPercent: number = Math.ceil(offsetHeight / (scrollHeight - this.lastMaxScrollTop) * 100);
    if (scrollPercent == 80 && this.scrollDirection == 0 && this.hasMorePage) {
      //scroll down
      this.loadNextPage();
    }
  }
}
