import { Component, OnInit, ElementRef } from '@angular/core';
import { Observable, fromEvent, merge, of } from 'rxjs';
import { map, filter, debounceTime, tap, switchAll, distinct, flatMap } from 'rxjs/operators';

import { Photo } from "../photo";
import { PhotoService } from "../photo.service";

@Component({
  selector: 'app-photo-display',
  templateUrl: './photo-display.component.html',
  styleUrls: ['./photo-display.component.css'],
})
export class PhotoDisplayComponent implements OnInit {

  selector = ".main-container"
  photos = [];
  total = 0;
  throttle = 300;
  scrollDistance = 1;
  scrollUpDistance = 2;
  direction = '';
  pageSize = 12;
  lastReturn = -1;
  from = 0;

  constructor(private photoService: PhotoService) {

  }

  ngOnInit() {
    this.onScrollDown();
  }

  onScrollDown() {
    if (this.lastReturn == 0) {
      console.log("There is no more pictures.");
      return;
    }

    this.photoService.search(this.from).subscribe(photos => {
      if (photos && photos.length) {
        this.from += photos.length;
        for (let i = 0; i < photos.length; i++) {
          this.photos.push(photos[i]);
        }
      }
    });
  }

}
