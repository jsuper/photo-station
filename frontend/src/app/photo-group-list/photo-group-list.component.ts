import { Component, OnInit } from '@angular/core';
import { PhotoService } from 'app/photo.service';
import * as justifiedLayout from 'justified-layout'
import { Photo } from 'app/photo.model';


class Section {
  date: string;
  boxes: object[];
  width: number = 0;
  height: number = 0;
  top: number;
  left: number;
}

@Component({
  selector: 'app-photo-group-list',
  templateUrl: './photo-group-list.component.html',
  styleUrls: ['./photo-group-list.component.css']
})
export class PhotoGroupListComponent implements OnInit {

  private photos: Photo[] = []; //store all loaded photos
  private sections: Section[] = [];

  constructor(private photoService: PhotoService) { }

  ngOnInit() {

    this.photoService.search(0, 15, '').subscribe(resp => {
      resp.forEach(photo => {
        let dateStr: string = new Date(photo.date).toLocaleDateString();
        let index: number = this.getSectionIndex(dateStr);
        if (index == -1) {
          let ns: Section = new Section();
          ns.date = dateStr;
          ns.boxes = [];
          index = this.sections.push(ns) - 1;
        }
        this.sections[index].width += photo.width;
        this.sections[index].height += photo.height;
        this.sections[index].boxes.push({ width: photo.width, height: photo.height });
      });
      console.log(this.sections);
      let v = this.sections.map(s => {
        let o = {
          width: s.width + (s.boxes.length > 1 ? s.boxes.length : 1 - 1) * 10,
          height: s.height
        };
        return o;
      });
      let just = justifiedLayout(v, {
        containerWidth: window.innerWidth
      });
      console.log(just);


    });
  }

  private getSectionIndex(dateStr: string): number {
    if (this.sections && this.sections.length) {
      for (let i = 0; i < this.sections.length; i++) {
        if (this.sections[i].date == dateStr) {
          return i;
        }
      }
    }
    return -1;
  }

}
