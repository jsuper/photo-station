import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-photo-uploader',
  templateUrl: './photo-uploader.component.html',
  styleUrls: ['./photo-uploader.component.css']
})
export class PhotoUploaderComponent implements OnInit {

  uploading: boolean = false;

  preview;

  total: number;
  current: number;

  uploadQueue: File[] = [];

  constructor(private http: HttpClient) { }

  ngOnInit() {
  }

  firePhotoChosen(fileInput: any) {
    let files = <FileList>fileInput.target.files;

    for (let i = 0; i < files.length; i++) {
      this.uploadQueue.push(files[i]);
    }
    this.total = files.length;
    this.doUploadFile();
  }

  doUploadFile() {
    this.uploading = true;
    this.current = 0;
    this.doNextSubmit();
  }

  doNextSubmit() {
    if (this.current > this.total) {
      return;
    }
    let file = this.uploadQueue[this.current];

    if (file == null) {
      return;
    }
    let previewReader = new FileReader();
    previewReader.readAsDataURL(file);
    previewReader.onload = e => {
      this.preview = previewReader.result;
    }
    let formData = new FormData();
    formData.append('file', file);
    this.doHttpSubmit(formData, () => this.doNextSubmit());
  }

  doHttpSubmit(data: FormData, callback: Function) {
    this.http.post('/api/photos/upload', data).subscribe(resp => {
      console.log("Upload result: " + resp);
      this.current++;
      if (this.current >= this.total) {
        console.log('上传完毕');
        return;
      } else {
        callback();
      }
    });
  }
}
