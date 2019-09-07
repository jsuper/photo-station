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

  pageSize = 12;
  from = 0;
  photos = [];

  queryField: string;
  queryVal: string;

  //cache photos
  photosCache = [];

  constructor(private photoService: PhotoService,
    private route: ActivatedRoute,
    private router: Router,
    public dialog: MatDialog) {
    this.route.queryParams.subscribe(qp => {
      let q = qp['q'];
      let field = this.route.snapshot.params['field'];
      if (this.queryVal === q && this.queryField == field) {
        console.log("Page query not changed.");
      } else {
        this.queryField = field;
        this.queryVal = q;
        this.photosCache = [];
        this.from = 0;
        this.photos = [];
        this.loadPhotos(0, () => {
          this.loadedCallback();
        });
      }
    });
  }

  ngOnInit() {

  }

  loadedCallback() {
    let loadedIndex = 0;
    for (let i = 0; i < this.pageSize && i < this.photosCache.length; i++) {
      this.photos.push(this.photosCache[i]);
      loadedIndex++;
    }
    this.photosCache = this.photosCache.slice(loadedIndex);
  }

  loadPhotos(from: number, callack) {
    const q = this.queryField && this.queryVal ? this.queryField + ":" + this.queryVal : "";
    this.photoService.search(from, this.pageSize * 3, q).subscribe(resp => {
      this.from += resp.length;
      resp.forEach(val => {
        this.photosCache.push(val);
      });
      if (callack && typeof callack == "function") {
        callack();
      }
    });
  }

  onScrollDown() {
    if (this.photosCache.length == 0) {
      console.log("There is no more photos.");
      return;
    }
    let hasMorePage = true;
    if (this.photosCache.length < this.pageSize) {
      console.log("There is no more pages...");
      hasMorePage = false;
    }

    let len = hasMorePage ? this.pageSize : this.photosCache.length;

    for (let i = 0; i < len; i++) {
      this.photos.push(this.photosCache[i]);
    }
    this.photosCache = this.photosCache.slice(len);
    if (hasMorePage && this.photosCache.length < this.pageSize) {
      this.loadPhotos(this.from, () => {
        this.loadedCallback();
      });
    }
  }

  openImageViewer(index: number) {
    const dialogRef = this.dialog.open(PhotoViewerComponent, {
      minWidth: '100%',
      minHeight: '100%',
      height: '100%',
      width: '100%',
      panelClass: 'photo-viewer-dialog',
      data: {
        max:this.photos.length,
        photo: this.photos[index], 
        index: index, 
        photoReader: index => {
          return this.photos[index];
        }
      }
    });
  }

}
