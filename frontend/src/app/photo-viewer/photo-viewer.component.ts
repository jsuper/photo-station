import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { Component, OnInit, Inject, HostListener } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Observable, Subscription, fromEvent } from 'rxjs';
import { MatChipInputEvent } from '@angular/material/chips';
import { Photo } from 'app/photo.model';

@Component({
  selector: 'app-photo-viewer',
  templateUrl: './photo-viewer.component.html',
  styleUrls: ['./photo-viewer.component.css']
})
export class PhotoViewerComponent implements OnInit {

  readonly separatorKeysCodes: number[] = [ENTER, COMMA];

  imageHeight: number;
  imageWidth: number;
  src: string;

  index: number;
  max: number;
  photoReader;
  hiddenInformation = true;

  currentPhoto: Photo;
  dataChanged: boolean;
  readonly currentPhotoTitle: string;
  readonly currentPhotoNote: string;

  constructor(public dialogRef: MatDialogRef<PhotoViewerComponent>,
    @Inject(MAT_DIALOG_DATA) public data: object) {

    this.currentPhoto = data['photo'];
    this.currentPhotoTitle = this.currentPhoto.title;
    this.currentPhotoNote = this.currentPhoto.note;
    this.dataChanged = false;
    this.index = data['index'];
    this.photoReader = data['photoReader'];
    this.max = data['max']
    this.reloadPhoto(this.currentPhoto);
  }

  reloadPhoto(photo: Photo) {
    let width = photo.width;
    let height = photo.height;

    let maxWidth = window.innerWidth;
    if (!this.hiddenInformation) {
      maxWidth = maxWidth - 350;
    }
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
    this.src = "/api/photo/" + photo.id;
  }

  ngOnInit() {
  }

  checkAndSaveChangedData(): void {
    if (this.dataChanged) {
      console.log("Data has been changed.");
    }
    this.dataChanged = false;
  }

  closeDialog() {
    this.checkAndSaveChangedData();
    this.dialogRef.close('Done');
  }

  showPreviousPhoto() {
    this.checkAndSaveChangedData();
    this.index--;
    let photo: Photo = this.photoReader(this.index);
    if (photo) {
      this.currentPhoto = photo;
      this.reloadPhoto(photo);
    }
  }

  showNextPhoto() {
    this.checkAndSaveChangedData();
    this.index++;
    let photo = this.photoReader(this.index);
    if (photo) {
      this.currentPhoto = photo;
      this.reloadPhoto(photo);
    }
  }

  showPhotoInformation() {
    this.hiddenInformation = !this.hiddenInformation;
    console.log(this.currentPhoto);
    if (!this.hiddenInformation) {
      this.reloadPhoto(this.currentPhoto);
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

  addElement(event: MatChipInputEvent, target: string): void {
    const input = event.input;
    const value = event.value;

    if ((value || '').trim()) {
      this.currentPhoto[target].push(value);
      this.dataChanged = true;
    }
    input.value = '';
  }

  removeElement(ele: string, target: string): void {
    const index = this.currentPhoto[target].indexOf(ele);
    if (index >= 0) {
      this.currentPhoto[target].splice(index, 1);
      this.dataChanged = true;
    }
  }

  onInputChanged(event: KeyboardEvent, field: string): void {
    const input = event.target;
    const value: string = input['value'];
    let old: string;
    switch (field) {
      case 'title': old = this.currentPhotoTitle; break;
      case 'note': old = this.currentPhotoNote; break;
    }

    if (value != old) {
      this.dataChanged = true;
    }
  }

  clearInputValue(field: string): void {
    const oldValue = this.currentPhoto[field];
    if (oldValue && oldValue.length) {
      this.currentPhoto[field] = '';
      this.dataChanged = true;
    }
  }
}
