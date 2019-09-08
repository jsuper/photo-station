import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-photo-uploader',
  templateUrl: './photo-uploader.component.html',
  styleUrls: ['./photo-uploader.component.css']
})
export class PhotoUploaderComponent implements OnInit {

  uploading: boolean = false;

  files: File;
  preview;

  constructor() { }

  ngOnInit() {
  }

  firePhotoChosen(fileInput: any) {
    let files = <File>fileInput.target.files;
    console.log(files);
    this.uploading = true;
    this.files = files;

    let imageReader = new FileReader();
    imageReader.readAsDataURL(files[0]);
    imageReader.onload = (_event) => {
      this.preview = imageReader.result;
    }
  }
}
