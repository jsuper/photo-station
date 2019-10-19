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
  preview: string = '';
  total: number;
  current: number;
  progress: number;

  uploadQueue: File[] = [];

  constructor(private http: HttpClient) { }

  ngOnInit() {
  }

  public uploadFiles(input: File[]): void {
    if (input && input.length) {
      this.uploadQueue = input;
      this.current = -1;
      this.total = this.uploadQueue.length;
      this.progress = 0;
      this.uploading = true;
      this.doNextSubmit();
    }
  }

  doUploadFile() {
    this.current = -1;
    this.uploading = true;
    this.doNextSubmit();
  }

  reset() {
    this.uploading = false;
    this.uploadQueue = [];
    this.total = 0;
    this.progress = 0;
    this.preview = '';
  }

  doNextSubmit() {
    this.current++;
    this.progress = Math.floor((this.current + 1) / this.total * 100);
    if (this.current > this.total) {
      console.log("No more files");
      // this.reset();
      return;
    }
    let file = this.uploadQueue[this.current];

    if (file == null) {
      console.log("no file");
      this.reset();
      return;
    }
    let previewReader = new FileReader();
    previewReader.readAsDataURL(file);
    previewReader.onload = e => {
      this.preview = previewReader.result.toString();
    }
    let formData = new FormData();
    formData.append('file', file);
    this.doHttpSubmit(formData, () => this.doNextSubmit());
  }

  doHttpSubmit(data: FormData, callback: Function) {
    this.http.post('/api/photos/upload', data).subscribe(resp => {
      callback();
    });
  }
}
