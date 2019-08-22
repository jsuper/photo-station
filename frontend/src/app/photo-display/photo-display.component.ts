import { Component, OnInit,ElementRef} from '@angular/core';
import { Observable,fromEvent,merge,of} from 'rxjs';
import { map, filter, debounceTime, tap, switchAll, distinct, flatMap } from 'rxjs/operators';

@Component({
  selector: 'app-photo-display',
  templateUrl: './photo-display.component.html',
  styleUrls: ['./photo-display.component.css'],
})
export class PhotoDisplayComponent implements OnInit {

  selector=".main-container"
  photos  = [];
  sum = 24;
  throttle = 300;
  scrollDistance = 1;
  scrollUpDistance = 2;
  direction = '';
  pageSize = 24 ;

  constructor() {
    for(let i=0;i<this.pageSize;i++) {
      this.photos.push("Photo "+i) ;
    }
  }

  ngOnInit() {

  }

  onScrollDown () {
    console.log('scrolled down!!');

    // add another 20 items
    const start = this.sum;
    this.sum += this.pageSize;
    for(let i=start;i<this.sum;i++) {
      this.photos.push("Photo "+i) ;
    }
  }

}
