import { Component, OnInit, Inject } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
  selector: 'app-photo-viewer',
  templateUrl: './photo-viewer.component.html',
  styleUrls: ['./photo-viewer.component.css']
})
export class PhotoViewerComponent implements OnInit {

  imageHeight: number;
  imageWidth: number;
  src: string;
  constructor(public dialogRef: MatDialogRef<PhotoViewerComponent>,
    @Inject(MAT_DIALOG_DATA) public data: object) {
    let photo = data['photo'];
    let width = photo.width;
    let height = photo.height;

    let ratio: number = window.innerHeight / height;
    this.imageHeight = window.innerHeight;
    this.imageWidth = Math.floor(width * ratio);
    this.src = "/api/photo/" + photo.id;
  }

  ngOnInit() {
  }

  closeDialog() {
    this.dialogRef.close('Done');
  }

}
