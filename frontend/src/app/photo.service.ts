import { Injectable } from '@angular/core';
import { Observable, of } from "rxjs";
import { Photo } from "./photo";
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class PhotoService {
  private photoApi = "api/photos";
  constructor(private http:HttpClient) { }

  search(from:number):Observable<Object[]> {
    const apiWithParam = this.photoApi+"?from="+from;
    return this.http.get<Object[]>(apiWithParam) ;
  }
}
