import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Album } from 'app/model/album.model';
import { HttpClient } from '@angular/common/http';


@Injectable({
  providedIn: 'root'
})
export class AlbumService {

  readonly api: string = '/api/album';
  private album: Album[] = [];

  constructor(private http: HttpClient) {
  }

  public addAlbum(name: string): Observable<Album> {
    let req = this.api + '/' + name;
    let ob: Observable<Album> = this.http.put(req, '');
    ob.subscribe(res => this.album.push(res));
    return ob;
  }

  public listAlbums(): Observable<Album[]> {
    return this.http.get<Album[]>('/api/albums');
  }

  public loadAllAlbums(): void {
    console.log('Load all albums.');

    this.listAlbums().subscribe(resp => this.album = resp);
  }

  public getAlbums(): Album[] {
    return this.album;
  }

  public getAlbumById(id: string): Album {
    let albumCached: Album = this.album.find(ele => {
      if (ele.id == id) {
        return ele;
      }
    });
    if (albumCached) {
      return albumCached;
    }
    return null;
  }

  public addPhotoToAlbum(albumId: string, photoId: string[]): Observable<any> {
    let url = `/api/album/${albumId}/photos`;
    return this.http.post<any>(url, photoId);
  }
}
