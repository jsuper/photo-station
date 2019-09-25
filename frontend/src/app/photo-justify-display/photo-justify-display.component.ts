import { Component, OnInit, ElementRef, ViewChild } from '@angular/core';
import * as justifiedLayout from 'justified-layout'
import { PhotoService } from 'app/photo.service';
import { RouteStateService } from 'app/route-state.service';
import { Photo } from 'app/photo.model';
import { PhotoViewerComponent } from "app/photo-viewer/photo-viewer.component";
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { Scrollable } from 'app/scrollable';

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

  copy(): BoxModel {
    let cop: BoxModel = new BoxModel(this.width, this.height, this.top, this.left);
    cop.imgHeight = this.imgHeight;
    cop.imgWidth = this.imgWidth;
    cop.id = this.id;
    return cop;
  }
}


@Component({
  selector: 'app-photo-justify-display',
  templateUrl: './photo-justify-display.component.html',
  styleUrls: ['./photo-justify-display.component.css']
})
export class PhotoJustifyDisplayComponent implements OnInit,Scrollable {
  scrollUp(scrollEl: ElementRef<any>) {
    throw new Error("Method not implemented.");
  }
  scrollDown(scrollEl: ElementRef<any>) {
    throw new Error("Method not implemented.");
  }
 
  pageSize: number = 15;

  photos: Photo[] = [];
  boxes: BoxModel[] = [];
  lastMaxScrollTop: number = 0;
  lastScrollTop: number = 0;
  scrollDirection: number = 0; //滚动方向，默认向下，1为向上
  lastScrollHeight: number = 0;
  hasMorePage: boolean = true;
  loaded: number = 0;
  justifyOption = {
    containerWidth: window.innerWidth - 10,
    targetRowHeight: 220,
  };
  loading: boolean = false;

  private qf: string;
  private query: string;


  constructor(private photoService: PhotoService,
    private routeStateService: RouteStateService,
    private dialog: MatDialog,
    private activedRoute: ActivatedRoute) {

    let maxRows = Math.ceil(window.innerHeight / 240);
    let cols = Math.floor(window.innerWidth / 300);
    this.pageSize = cols * maxRows + 1;

    this.activedRoute.queryParams.subscribe(qp => {
      let q = qp['q'] || '';
      let field = this.activedRoute.snapshot.params['field'];


      console.log('qf:' + this.qf + ', query: ' + this.query + ',field: ' + field + ', val: ' + q);
      let loadPage = q != this.query && field == this.qf && this.qf;

      if (this.qf != field || this.query != q) {
        this.reset();
        this.qf = field;
        this.query = q;
      }
      if (loadPage) {
        this.loadNextPage();
      }
    });

  }

  private reset(): void {
    console.log('reset values');

    this.photos = [];
    this.boxes = [];
    this.lastMaxScrollTop = 0;
    this.lastScrollHeight = 0;
    this.lastScrollTop = 0;
    this.loaded = 0;
    this.hasMorePage = true;
    this.loading = false;
  }

  ngOnInit() {
    console.log("Called on init...");
    console.log(this.activedRoute.snapshot.params);
    console.log(this.activedRoute.snapshot.queryParams);



    this.routeStateService.setComponent(this);
    this.loadNextPage();
    // this.loading = true;
    // this.photoService.search(0, this.pageSize, '').subscribe(resp => {
    //   this.loading = false;
    //   this.hasMorePage = resp.length === this.pageSize;
    //   this.loaded = resp.length;
    //   let wh = [];

    //   resp.forEach(photo => {
    //     this.photos.push(photo);
    //     wh.push({ width: photo.width, height: photo.height });
    //   });
    //   let justify = justifiedLayout(wh, this.justifyOption);
    //   this.boxes = justify.boxes.map(box => {
    //     return new BoxModel(Math.floor(box.width), Math.floor(box.height), Math.floor(box.top), Math.floor(box.left));
    //   });
    //   this.boxes.forEach((val, idx) => {
    //     val.id = resp[idx].id;
    //     val.imgWidth = resp[idx].width;
    //     val.imgHeight = resp[idx].height;
    //   });
    // });
  }

  getBoxesOfLastRow(): BoxModel[] {
    let calcBoxes: BoxModel[] = [];
    if (this.boxes && this.boxes.length) {
      for (let i = this.boxes.length - 1; i > 0; i--) {
        let box: BoxModel = this.boxes[i];
        calcBoxes.unshift(box.copy());
        if (this.boxes[i].left == 10) {
          break;
        }
      }
    }
    return calcBoxes;
  }

  loadNextPage(): void {
    if (this.hasMorePage) {
      console.log("Starting loading next pages...");
      this.loading = true;
      let calcBoxes: Array<Object> = [];
      let boxesOfLastRow = this.getBoxesOfLastRow();
      // console.log(boxesOfLastRow);

      boxesOfLastRow.forEach(box => {
        calcBoxes.push({ width: box.imgWidth, height: box.imgHeight });
      });

      const q = this.qf && this.query ? this.qf + ':' + this.query : '';
      console.log('Query:' + q);
      this.photoService.search(this.loaded, this.pageSize, q).subscribe(resp => {
        this.loading = false;
        this.hasMorePage = resp.length == this.pageSize;
        this.loaded += resp.length;
        resp.forEach(photo => {
          this.photos.push(photo);
          calcBoxes.push({
            width: photo.width,
            height: photo.height,
          })
        });
        let justify = justifiedLayout(calcBoxes, this.justifyOption).boxes;
        // console.log(justify);

        let baseTop = this.boxes && this.boxes.length ? this.boxes[this.boxes.length - 1].top : 10;
        let previousBox = [];
        let newPagebox = [];


        for (let i = 0; i < justify.length; i++) {
          if (i < boxesOfLastRow.length) {
            previousBox.push(justify[i]);
          } else {
            newPagebox.push(justify[i]);
          }
        }
        if (previousBox.length) {
          for (let i = this.boxes.length - boxesOfLastRow.length; i < this.boxes.length; i++) {
            let b: BoxModel = this.boxes[i];
            let jf = previousBox[i % boxesOfLastRow.length];
            b.width = jf.width;
            b.height = jf.height;
            b.left = jf.left;
          }
        }
        let rows = 0;
        for (let i = 0; i < newPagebox.length; i++) {
          let jf = newPagebox[i];
          let photo: Photo = resp[i];
          if (jf.left == 10) {
            rows++;
          }
          let width = Math.floor(jf.width);
          let height = Math.floor(jf.height);
          let top = Math.floor(jf.top);
          let left = Math.floor(jf.left);

          let boxTop = rows == 0 ? baseTop : baseTop + top - 10;
          let box: BoxModel = new BoxModel(width, height, boxTop, left);
          box.id = photo.id;
          box.imgHeight = photo.height;
          box.imgWidth = photo.width;
          this.boxes.push(box);
        }
      });
    }
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

    this.scrollDirection = scrollTop > this.lastScrollTop ? 0 : 1;
    this.lastScrollTop = scrollTop;
    this.lastMaxScrollTop = Math.max(scrollTop, this.lastMaxScrollTop);
    const scrollPercent: number = Math.ceil(offsetHeight / (scrollHeight - this.lastMaxScrollTop) * 100);
    // console.log("Scroll percent: " + scrollPercent);
    // console.log(this.lastScrollHeight + ":" + scrollHeight + ":" + scrollPercent);

    if (scrollPercent >= 80 && this.scrollDirection == 0 && this.hasMorePage &&
      (this.lastScrollHeight == 0 || this.lastScrollHeight < scrollHeight)) {
      this.lastScrollHeight = scrollHeight;
      this.loadNextPage();
    }
  }

  openImageViewer(index: number, id: string): void {
    console.log('idx:' + index + ", id:" + id);
    const dialogRef = this.dialog.open(PhotoViewerComponent, {
      minWidth: '100%',
      minHeight: '100%',
      height: '100%',
      width: '100%',
      panelClass: 'photo-viewer-dialog',
      data: {
        max: this.photos.length,
        photo: this.photos[index],
        index: index,
        photoReader: index => {
          return this.photos[index];
        }
      }
    });
  }

  likePhoto(index: number, id: string): void {
    let photo = this.photos[index];
    if (photo.id === id) {
      photo.favorite = photo.favorite ? 0 : 1;
      this.photoService.update(photo).subscribe(resp => {
        console.log("Update photo favorite state: " + resp);
      });
    }
  }
}
