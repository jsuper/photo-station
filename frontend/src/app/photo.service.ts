import { Injectable } from '@angular/core';
import { Observable, of } from "rxjs";
import { Photo } from "app/photo.model";
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class PhotoService {
  private photoApi = "api/photos";
  constructor(private http: HttpClient) { }

  search(from: number, size: number, query: string): Observable<Photo[]> {
    const apiWithParam = this.photoApi + "?from=" + from + '&size=' + size + "&q=" + query;
    return this.http.get<Photo[]>(apiWithParam);
  }

  update(photo: Photo): Observable<Photo> {
    const updateApi = "api/photo/" + photo.id;
    return this.http.put<Photo>(updateApi, photo);
  }

  delete(photoId: string[]): Observable<any> {
    return this.http.put<any>("/api/trash/photos", photoId);
  }

  addToFavorite(photoId: string[]): Observable<any> {
    let partialReq = {
      'photos': photoId,
      'fields': {
        'favorite': '1',
      }
    };
    return this.http.put<any>('api/favorite/photos', partialReq);
  }
}
