import { Location, LocationStrategy, PathLocationStrategy } from '@angular/common';
import { Component, OnInit, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { PhotoService } from 'app/photo.service';
import { RouteStateService } from 'app/route-state.service';
import { Photo } from 'app/photo.model';
import { PhotoViewerComponent } from "app/photo-viewer/photo-viewer.component";
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { Scrollable } from 'app/scrollable';
import { FlexLayoutService } from 'app/flex-layout/flex-layout.service';
import { Segment, Box } from 'app/flex-layout/flex-layout.model';


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
  styleUrls: ['./photo-justify-display.component.css'],
  providers: [Location, { provide: LocationStrategy, useClass: PathLocationStrategy }]
})
export class PhotoJustifyDisplayComponent implements OnInit {


  hasMorePage: boolean = true;
  loaded: number = 0;
  private qf: string;
  private query: string;

  showHeaderShadow: boolean = false;

  boxHeight: number = 300;
  loadedPhotos: Segment[] = [];

  private pageSize: number = 15;
  private scrollState: Map<number, number> = new Map();

  @ViewChild('flexBoxContainer', { static: true }) flexBoxContainer: ElementRef;

  constructor(private photoService: PhotoService,
    private activeRoute: ActivatedRoute,
    private location: Location,
    private el: ElementRef) {
    let path: string = location.path();
    let realPath: string = '/' + path.substring(path.indexOf('popup:') + 6, path.length - 1);
    location.replaceState(realPath);
  }



  ngOnInit() {

    let containerWidth = this.flexBoxContainer.nativeElement.clientWidth;
    let height = window.innerHeight;

    if (containerWidth <= 600) {
      if (containerWidth <= 375) {
        this.boxHeight = 124;
      } else {
        this.boxHeight = 200;
      }
    }
    console.log(`The target box height is : ${this.boxHeight}`);


    let cols = Math.floor(containerWidth / 300);
    let rows = Math.floor(height / 200);
    let size = rows * cols;
    console.log(`page size is: ${size}`);
    this.pageSize = Math.max(this.pageSize, size);

    this.qf = "albums";
    this.query = this.activeRoute.snapshot.params['id'];
    this.loadNextPage();
  }

  goBack() {
    this.location.back();
  }

  onContentScroll(event): void {
    let target = event.target;
    let scrollTop = target.scrollTop;
    this.showHeaderShadow = scrollTop > 0;

    let scrollHeight = target.scrollHeight;
    let offsetHeight = target.offsetHeight;
    let scrollRatio = (offsetHeight + scrollTop) / scrollHeight;
    if (scrollRatio > 0.9 && !this.scrollState.has(scrollHeight)) {
      this.scrollState.set(scrollHeight, scrollHeight);
      console.log(`Load next page. Scroll info: top: ${target.scrollTop}, window height: ${offsetHeight}, scroll height: ${scrollHeight}`);
      this.loadNextPage();
    }

  }

  loadNextPage(): void {
    if (this.hasMorePage) {
      const q = this.qf && this.query ? this.qf + ':' + this.query : '';
      console.log('Query:' + q);
      this.photoService.search(this.loaded, this.pageSize, q).subscribe(resp => {
        this.hasMorePage = resp.length == this.pageSize;
        this.loaded += resp.length;
        this.processPhoto(resp);
      });
    }
  }

  processPhoto(photos: Photo[]) {
    let boxes: Box[] = photos.map(photo => {
      let box: Box = new Box();
      box.height = photo.height;
      box.width = photo.width;
      box.raw = {
        width: photo.width,
        height: photo.height,
        id: photo.id,
      };
      return box;
    });
    this.loadedPhotos.push({ boxes: boxes });
  }


  openImageViewer(index: number, id: string): void {

  }

  likePhoto(index: number, id: string): void {

  }
}
