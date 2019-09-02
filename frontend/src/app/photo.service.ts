import { Injectable } from '@angular/core';
import { Observable, of } from "rxjs";
import { Photo } from "app/photo";
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class PhotoService {
  private photoApi = "api/photos";
  constructor(private http:HttpClient) { }

  search(from:number,query:string):Observable<Object[]> {
    const apiWithParam = this.photoApi+"?from="+from+"&q="+query;
    return this.http.get<Object[]>(apiWithParam) ;
  }
}
