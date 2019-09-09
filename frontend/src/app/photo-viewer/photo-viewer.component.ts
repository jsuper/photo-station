import { Component, OnInit, Inject, HostListener } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Observable, Subscription, fromEvent } from 'rxjs';

@Component({
  selector: 'app-photo-viewer',
  templateUrl: './photo-viewer.component.html',
  styleUrls: ['./photo-viewer.component.css']
})
export class PhotoViewerComponent implements OnInit {

  imageHeight: number;
  imageWidth: number;
  src: string;

  index: number;
  max: number;
  photoReader;
  constructor(public dialogRef: MatDialogRef<PhotoViewerComponent>,
    @Inject(MAT_DIALOG_DATA) public data: object) {
    let photo = data['photo'];
    this.index = data['index'];
    this.photoReader = data['photoReader'];
    this.max = data['max']
    this.reloadPhoto(photo);
  }

  reloadPhoto(photo: any) {
    this.src = "/api/photo/" + photo.id;

    let width = photo.width;
    let height = photo.height;
    console.log("photo: " + width + "," + height);

    let maxWidth = window.innerWidth;
    let maxHeight = window.innerHeight;

    if (width > maxWidth || height > maxHeight) {
      if (width > maxWidth && height < maxHeight) {
        //以x缩放
        let ratio: number = maxWidth / width;
        this.imageWidth = maxWidth;
        this.imageHeight = Math.floor(height * ratio);
      } else if (width < maxWidth && height > maxHeight) {
        //以y缩放
        let ratio: number = maxHeight / height;
        this.imageHeight = maxHeight;
        this.imageWidth = Math.floor(ratio * width);
      } else {
        let ratio: number = window.innerHeight / height;
        this.imageHeight = window.innerHeight;
        this.imageWidth = Math.floor(width * ratio);
      }
    } else {
      this.imageHeight = height;
      this.imageWidth = width;
    }

  }

  ngOnInit() {
  }

  closeDialog() {
    this.dialogRef.close('Done');
  }

  showPreviousPhoto() {
    this.index--;
    let photo = this.photoReader(this.index);
    if (photo) {
      this.reloadPhoto(photo);
    }
  }

  showNextPhoto() {
    this.index++;
    let photo = this.photoReader(this.index);
    if (photo) {
      this.reloadPhoto(photo);
    }
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent) {
    if (event.keyCode == 37 && this.index > 0) {
      //arrow left
      this.showPreviousPhoto();
    } else if (event.keyCode == 39 && this.index < this.max - 1) {
      //arrow right
      this.showNextPhoto();
    }
  }
}
