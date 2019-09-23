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
  breakRow: boolean;
}


@Component({
  selector: 'app-photo-group-list',
  templateUrl: './photo-group-list.component.html',
  styleUrls: ['./photo-group-list.component.css']
})
export class PhotoGroupListComponent implements OnInit {

  private photos: Photo[] = []; //store all loaded photos
  sections: Section[] = [];
  loaded: boolean = false;

  constructor(private photoService: PhotoService) { }

  /**
   * 基于Segment的图片展示
   *
   * 基于日期进行分段，先按每个段一行来展示
   * 段内使用justified layout来进行布局。
   * 当展示完成后，重新遍历所有的段，判断当前段和下一个段是否可以合并，（从当前段开始找所有可以合并的段）
   * 可以合并的段找到后，向上移动该段（段内元素使用上一个段的高度进行等比缩放），剩余的段依次想上移动当前段的height高度像素。
   *
   * 分页加载的时候，将当前元素归到对应的段，如果当前元素的段已经加载，则直接将元素插入段的最后一行，并重新计算其布局
   */
  ngOnInit() {

    this.photoService.search(0, 50, '').subscribe(resp => {
      resp.forEach(photo => {
        let dateStr: string = new Date(photo.date).toLocaleDateString();
        let index: number = this.getSectionIndex(dateStr);
        if (index == -1) {
          let ns: Section = new Section();
          ns.date = dateStr;
          ns.boxes = [];
          index = this.sections.push(ns) - 1;
        }
        this.sections[index].width += 300;
        this.sections[index].boxes.push({ width: photo.width, height: photo.height });
      });

      let maxWidth: number = window.innerWidth - 30;
      this.sections.forEach((val, index) => {
        let rows = Math.ceil(((val.boxes.length * 300) + (val.boxes.length - 1) * 10) / maxWidth);
        val.height = rows * 200 + (rows - 1) * 10;
        let nr: boolean = false;
        if (index == 0) {
          val.width = Math.min(val.width, maxWidth);
          val.top = 10;
          val.left = 10;
        } else {
          let pre: Section = this.sections[index - 1];
          if (!nr) {
            nr = pre.width + 20 + val.width > maxWidth;
          }
          if (nr) {
            val.top = pre.height + pre.top + 10;
            val.left = 10;
          } else {
            val.top = pre.top;
            val.left = pre.width + 20;
          }
        }
        val.width = Math.min(val.width, maxWidth);
        let config;
        if (!nr && index > 0) {
          //force heigh to same as previous section
          let preBox = this.sections[index - 1].boxes;
          let height: number = preBox[preBox.length - 1]['height'];
          let containerWidth = maxWidth - preBox[preBox.length - 1]['width'] - 20;
          config = { containerWidth: containerWidth, targetRowHeight: height, targetRowHeightTolerance: 0 };
        } else {
          config = { containerWidth: val.width - 10, targetRowHeight: 240 }
        }

        console.log("Calc section " + val.date + " with config: " + JSON.stringify(config));
        let boxes = justifiedLayout(val.boxes, config).boxes;
        console.log(boxes);

        val.boxes.splice(0, val.boxes.length);
        let row = 0;
        let rh = 0;
        let vw = 0;
        boxes.forEach(box => {
          let h = Math.floor(box.height);
          let l = Math.floor(box.left);
          let w = Math.floor(box.width);
          if (l == 10) {
            row++;
            rh += h;
          }
          val.boxes.push({
            width: w,
            height: h,
            top: Math.floor(box.top),
            left: l
          });
        });
        rh = (row - 1) * 10 + rh;
        val.height = rh + 10;
      });
      this.loaded = true;

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
