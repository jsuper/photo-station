import { Injectable } from '@angular/core';
import { Observable, of } from "rxjs";
import { Photo } from "./photo";
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class PhotoService {
  private photoApi = "api/photos";
  private photos = [
    { 'id': 1, 'thumbnail': '/assets/examples/IMG_4074_500.jpg', 'raw': '/assets/examples/IMG_4074.JPG' },
    { 'id': 2, 'thumbnail': '/assets/examples/IMG_4075_500.jpg', 'raw': '/assets/examples/IMG_4075.JPG' },
    { 'id': 3, 'thumbnail': '/assets/examples/IMG_4097_500.jpg', 'raw': '/assets/examples/IMG_4097.JPG' },
    { 'id': 4, 'thumbnail': '/assets/examples/IMG_4103_500.jpg', 'raw': '/assets/examples/IMG_4103.JPG' },
    { 'id': 5, 'thumbnail': '/assets/examples/IMG_4110_500.jpg', 'raw': '/assets/examples/IMG_4110.JPG' },
    { 'id': 6, 'thumbnail': '/assets/examples/IMG_4111_500.jpg', 'raw': '/assets/examples/IMG_4111.JPG' },
    { 'id': 7, 'thumbnail': '/assets/examples/IMG_4112_500.jpg', 'raw': '/assets/examples/IMG_4112.JPG' },
  ]
  constructor(private http:HttpClient) { }

  getPhotos(): Observable<Photo[]> {
    return of(this.photos);
  }

  search(page:number):Observable<Object[]> {
    return this.http.get<Object[]>(this.photoApi) ;
  }
}
