import { Component, OnInit, ElementRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, fromEvent, merge, of } from 'rxjs';
import { map, filter, debounceTime, tap, switchAll, distinct, flatMap, switchMap } from 'rxjs/operators';

import { Photo } from "app/photo";
import { PhotoService } from "app/photo.service";
import { PhotoViewerComponent } from "app/photo-viewer/photo-viewer.component";
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'app-photo-display',
  templateUrl: './photo-display.component.html',
  styleUrls: ['./photo-display.component.css'],
})
export class PhotoDisplayComponent implements OnInit {

  selector = ".main-container"
  total = 0;
  throttle = 300;
  scrollDistance = 1;
  scrollUpDistance = 2;
  direction = '';

  pageSize = 12;
  lastReturn = -1;
  from = 0;
  photos = [];

  queryField: string;
  queryVal: string;

  constructor(private photoService: PhotoService,
    private route: ActivatedRoute,
    private router: Router,
    public dialog: MatDialog) {
    this.route.queryParams.subscribe(qp => {
      let q = qp['q'];
      let field = this.route.snapshot.params['field'];
      if (field == this.queryField && q == this.queryVal) {
        console.log("Url not changed...");
      } else {
        this.queryField = field;
        this.queryVal = q;
        this.photos = [];
        this.lastReturn = -1;
        this.from = 0;
        this.onScrollDown();
      }
    });
  }

  ngOnInit() {

  }

  onScrollDown() {
    if (this.lastReturn == 0) {
      console.log("There is no more pictures.");
      return;
    }
    const q = this.queryField && this.queryVal ? this.queryField + ":" + this.queryVal : "";
    this.photoService.search(this.from, q).subscribe(photos => {
      this.lastReturn = this.photos.length;
      if (photos && photos.length) {
        this.from += photos.length;
        for (let i = 0; i < photos.length; i++) {
          this.photos.push(photos[i]);
        }
      }
    });
  }

  openImageViewer() {
    console.log("opened image");
    const dialogRef = this.dialog.open(PhotoViewerComponent, {
      minWidth: '100%',
      minHeight: '100%',
      height:'100%',
      width:'100%',
      panelClass:'photo-viewer-dialog',
      data: { name: 'demo' }
    });
  }

}
