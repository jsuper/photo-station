import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { Component, OnInit, Inject, HostListener } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Observable, Subscription, fromEvent } from 'rxjs';
import { MatChipInputEvent } from '@angular/material/chips';
import { Photo } from 'app/photo.model';
import { PhotoService } from 'app/photo.service';
import { Album } from 'app/model/album.model';
import { AlbumService } from 'app/services/album.service';

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

  photoReader;
  hiddenInformation = true;

  currentPhoto: Photo;
  dataChanged: boolean;
  readonly currentPhotoTitle: string;
  readonly currentPhotoNote: string;

  private totalSections: number = 0;
  private totalPhotos: number = 0;
  private sectionIndex: number = 0;
  private blockIndex: number = 0;

  private sectionLengthReader;
  private canMoveNext: boolean;
  private canMovePrevious: boolean;
  private albums: Map<string, Album> = new Map();

  constructor(public dialogRef: MatDialogRef<PhotoViewerComponent>,
    @Inject(MAT_DIALOG_DATA) public data: object,
    private photoService: PhotoService,
    private albumService: AlbumService) {
    dialogRef.afterClosed().subscribe(close => {
      this.checkAndSaveChangedData();
    });
    this.currentPhoto = data['photo'];
    this.currentPhotoTitle = this.currentPhoto.title;
    this.currentPhotoNote = this.currentPhoto.note;
    this.dataChanged = false;
    this.totalSections = data['sections'];
    this.totalPhotos = data['photos'];
    this.photoReader = data['photoReader'];
    let indexes: number[] = data['indexes'];

    this.sectionIndex = indexes[0];
    this.blockIndex = indexes[1];

    this.sectionLengthReader = data['blocksOfSection'];
    this.reloadPhoto(this.currentPhoto);

  }

  allAlbums(photo: Photo): Album[] {
    if (photo.albums) {
      return photo.albums.map(id => this.albumService.getAlbumById(id))
        .filter(ele => ele && ele.id);
    }
    return [];
  }

  reloadPhoto(photo: Photo) {
    this.albums.clear();

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
    this.canMovePrevious = this.blockIndex <= 0 && this.sectionIndex <= 0;
    let sb: number = this.sectionLengthReader(this.sectionIndex);
    this.canMoveNext = (this.sectionIndex < this.totalSections - 1) ||
      (this.sectionIndex == this.totalSections - 1 && this.blockIndex < sb - 1);
  }

  checkAndSaveChangedData(): void {
    if (this.dataChanged) {
      console.log("Data has been changed.");
      this.photoService.update(this.currentPhoto)
        .subscribe(resp => console.log(resp));
    }
    this.dataChanged = false;
  }

  closeDialog() {
    this.dialogRef.close('Done');
  }
  showPreviousPhoto() {
    this.checkAndSaveChangedData();
    if (this.blockIndex <= 0 && this.sectionIndex <= 0) {
      this.canMovePrevious = false;
      return;
    }

    if (this.blockIndex == 0) {
      this.sectionIndex--;
      this.blockIndex = this.sectionLengthReader(this.sectionIndex);
      console.log(`Switch to previous section: ${this.sectionIndex}: ${this.blockIndex}`);
    }

    this.blockIndex--;
    let photo: Photo = this.photoReader(this.sectionIndex, this.blockIndex);
    if (photo) {
      this.currentPhoto = photo;
      this.reloadPhoto(photo);
    }
    this.canMoveNext = true;
    if (this.blockIndex <= 0 && this.sectionIndex <= 0) {
      this.canMovePrevious = false;
    }
  }

  showNextPhoto() {
    this.checkAndSaveChangedData();
    let sectionBlocks: number = this.sectionLengthReader(this.sectionIndex);
    if (this.blockIndex == sectionBlocks - 1 && this.sectionIndex == this.totalSections - 1) {
      console.log(`No more photos: ${this.sectionIndex}:${sectionBlocks},${this.totalSections},${this.blockIndex}`);
      return;
    }

    this.canMovePrevious = true;
    if (this.blockIndex == sectionBlocks - 1) {
      this.sectionIndex++;
      this.blockIndex = -1;
      console.log(`Move to next sections: ${this.sectionIndex}`);
    }

    this.blockIndex++;
    let photo = this.photoReader(this.sectionIndex, this.blockIndex);
    if (photo) {
      this.currentPhoto = photo;
      this.reloadPhoto(photo);
    }
    if (this.blockIndex == this.sectionLengthReader(this.sectionIndex) - 1 && this.sectionIndex == this.totalSections - 1) {
      this.canMoveNext = false;
    }
  }

  showPhotoInformation() {
    this.hiddenInformation = !this.hiddenInformation;
    if (!this.hiddenInformation) {
      this.reloadPhoto(this.currentPhoto);
    } else {
      console.log("Close information panel, check and save snapshot metadata: " + this.dataChanged);
      this.checkAndSaveChangedData();
    }
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent) {
    if (event.keyCode == 37) {
      //arrow left
      this.showPreviousPhoto();
    } else if (event.keyCode == 39) {
      //arrow right
      this.showNextPhoto();
    }
  }

  addElement(event: MatChipInputEvent, target: string): void {
    const input = event.input;
    const value = event.value;

    if ((value || '').trim()) {
      if (!this.currentPhoto[target]) {
        this.currentPhoto[target] = [];
      }
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

  renderExposureTime(exposure: string): string {
    if (exposure && exposure.length) {
      let ep = parseFloat(exposure);
      return '1/' + (Math.ceil(1 / ep));
    }
    return '';
  }
}
