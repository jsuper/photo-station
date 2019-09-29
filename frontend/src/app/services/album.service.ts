import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Album } from 'app/model/album.model';
import { HttpClient } from '@angular/common/http';


@Injectable({
  providedIn: 'root'
})
export class AlbumService {

  readonly api: string = '/api/album';

  constructor(private http: HttpClient) {


  }

  public addAlbum(name: string): Observable<Album> {
    let req = this.api + '/' + name;
    return this.http.put(req, '');
  }

  public listAlbums(): Observable<Album[]> {
    return this.http.get<Album[]>('/api/albums');
  }

  public addPhotoToAlbum(albumId:string,photoId:string[]):Observable<string> {
    let url = `/api/album/${albumId}/photos` ;
    return this.http.post<string>(url,photoId);
  }
}
