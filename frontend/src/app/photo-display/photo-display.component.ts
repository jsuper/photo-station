import { Component, OnInit,ElementRef} from '@angular/core';
import { Observable,fromEvent,merge,of} from 'rxjs';
import { map, filter, debounceTime, tap, switchAll, distinct, flatMap } from 'rxjs/operators';

import { Photo } from "../photo";
import { PhotoService } from "../photo.service";

@Component({
  selector: 'app-photo-display',
  templateUrl: './photo-display.component.html',
  styleUrls: ['./photo-display.component.css'],
})
export class PhotoDisplayComponent implements OnInit {

  selector=".main-container"
  photos  = [];
  sum = 12;
  throttle = 300;
  scrollDistance = 1;
  scrollUpDistance = 2;
  direction = '';
  pageSize = 12 ;

  private allPhotos:Photo[] ;

  constructor(private photoService:PhotoService) {

  }

  getRandomIndex(max:number):number {
    return Math.floor(Math.random()*Math.floor(max)) ;
  }

  ngOnInit() {
    this.getPhotos() ;
    console.log(this.allPhotos.length) ;
    console.log(this.getRandomIndex(this.allPhotos.length));
    for(let i=0;i<this.allPhotos.length;i++) {
      this.photos.push(this.allPhotos[i]) ;
    }

    this.pushRandomImages(this.allPhotos.length,this.pageSize) ;
  }

  getPhotos(): void {
    this.photoService.getPhotos().subscribe(photos=>this.allPhotos=photos) ;
  }

  pushRandomImages(start,end):void {
    for(let i=start;i<end;i++) {
      this.photos.push(this.allPhotos[this.getRandomIndex(this.allPhotos.length)]) ;
    }
  }

  onScrollDown () {
    console.log('scrolled down!!');

    if(this.sum == 24) {
      console.log("No more pictures") ;
      return;
    }

    // add another 20 items
    const start = this.sum;
    this.sum += this.pageSize;
    this.pushRandomImages(start,this.sum) ;
  }

}
