import { Component, OnInit, Input, ElementRef, IterableDiffers, IterableDiffer, IterableChanges } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { Photo } from 'app/photo.model';
import { PhotoService } from 'app/photo.service';
import { Segment, Box } from 'app/flex-layout/flex-layout.model';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-flex-photo',
  templateUrl: './flex-photo.component.html',
  styleUrls: ['./flex-photo.component.css']
})
export class FlexPhotoComponent implements OnInit {

  segment: Segment[] = [];
  total: number = 0;
  hasMore: boolean = true;

  private field: string;
  private query: string;
  constructor(private el: ElementRef,
    private photoService: PhotoService,
    private activatedRoute: ActivatedRoute) {
    this.activatedRoute.data.subscribe(data => {
      this.field = data.field;
      this.query = data.query;
    });
  };

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
    this.segment.push({ boxes: boxes });
  }

  ngOnInit() {
    console.log(`Query: ${this.field}:${this.query}`);
    this.loadNextPage();
  }

  loadNextPage() {
    if (!this.field || !this.query) {
      this.hasMore = false;
      return;
    }
    if (this.hasMore) {
      let q = `${this.field}:${this.query}`;
      this.photoService.search(this.total, 20, q).subscribe(data => {
        this.total += data.length;
        this.hasMore = data.length == 20;
        this.processPhoto(data);
      });
    }
  }
}
