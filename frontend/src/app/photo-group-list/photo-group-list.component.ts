import { Component, OnInit, ElementRef, Inject, LOCALE_ID } from '@angular/core';
import { PhotoService } from 'app/photo.service';
import * as justifiedLayout from 'justified-layout'
import { Photo } from 'app/photo.model';
import { Section, Block } from './photo-group.model';
import { ActivatedRoute, Params } from '@angular/router';
import { Scrollable } from 'app/scrollable';
import { RouteStateService } from 'app/route-state.service';
import { formatDate } from '@angular/common';
import { PhotoViewerComponent } from 'app/photo-viewer/photo-viewer.component';
import { MatDialog } from '@angular/material/dialog';

class Box {
  width: number;
  height: number;
  constructor(width: number, height: number) {
    this.width = width;
    this.height = height;
  }
}

class Query {
  field: string;
  value: string;

  constructor(field: string, value: string) {
    this.field = field;
    this.value = value;
  }

  equals(query: Query): boolean {
    return this == query || (this.field == query.field && this.value == query.value);
  }

  queryStr(): string {
    return this.field && this.value ? this.field + ':' + this.value : '';
  }
}

@Component({
  selector: 'app-photo-group-list',
  templateUrl: './photo-group-list.component.html',
  styleUrls: ['./photo-group-list.component.css']
})
export class PhotoGroupListComponent implements OnInit, Scrollable {

  readonly today: Date = new Date();
  readonly blockSpacing: number = 10;

  sections: Section[] = [];
  total: number = 0;
  pageSize: number = 15;
  hasMore: boolean = true;
  targetHeight: number = 240;
  containerWidth: number = window.innerWidth - 40;
  lastMergedIndex = 0;

  private query: Query;
  private maxScrollTop: number;

  private scrollState: Map<Number, Number> = new Map();

  constructor(private photoService: PhotoService,
    private activeRoute: ActivatedRoute,
    private el: ElementRef,
    private routeStateService: RouteStateService,
    @Inject(LOCALE_ID) private locale: string,
    private dialog: MatDialog, ) {
    console.log(`Current locale: ${this.locale}`);


    this.activeRoute.queryParams.subscribe(qp => {
      let q = qp['q'] || '';
      let field = this.activeRoute.snapshot.params['field'];

      let temp: Query = new Query(field, q);
      if (this.query == null) {
        //first loaded.
        this.query = temp;
      } else {
        if (!this.query.equals(temp)) {
          console.log("Query changed...");

          this.query = temp;
          this.reset();
          this.loadNextPagePhotos();
        }
      }
    });
  }

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
    this.routeStateService.setComponent(this);
    let container = this.el.nativeElement.getElementsByClassName('container');
    this.containerWidth = container[0].offsetWidth - 20;
    console.log(`Container width is : ${this.containerWidth}`);


    let maxRows: number = Math.ceil((window.innerHeight - 72) / this.targetHeight);
    let rowCols: number = Math.ceil(this.containerWidth / 300);
    this.pageSize = maxRows * rowCols;

    this.loadNextPagePhotos();
  }

  scrollUp(scrollEl: ElementRef) {
    // console.log();
  }
  scrollDown(scrollEl: ElementRef) {

    let currentScrollTop: number = scrollEl.nativeElement.scrollTop; //滚动条位置
    let clientHeight: number = scrollEl.nativeElement.clientHeight; //元素实际高度
    let scrollHeight: number = scrollEl.nativeElement.scrollHeight; // 滚动条高度

    const scrollPercent: number = Math.ceil((clientHeight + currentScrollTop) / scrollHeight * 100);

    if (scrollPercent >= 80 && this.hasMore && !this.scrollState.has(scrollHeight)) {
      this.scrollState.set(scrollHeight, 1);
      console.log(`Load next page: ${scrollPercent}, state: ${JSON.stringify(this.scrollState)}`);
      this.loadNextPagePhotos();
    }
  }

  openImageViewer(sectionIndex: number, photoIndex: number): void {
    if (sectionIndex < this.sections.length && photoIndex < this.sections[sectionIndex].blocks.length) {
      let photo: Photo = this.sections[sectionIndex].blocks[photoIndex].photo;
      this.dialog.open(PhotoViewerComponent, {
        minWidth: '100%',
        minHeight: '100%',
        height: '100%',
        width: '100%',
        panelClass: 'photo-viewer-dialog',
        data: {
          sections: this.sections.length,
          photos: this.total,
          photo: photo,
          indexes: [sectionIndex, photoIndex],
          photoReader: (section, block) => {
            return this.sections[section].blocks[block].photo;
          },
          blocksOfSection: sectionIndex => { return this.sections[sectionIndex].blocks.length }
        }
      });
    }

  }

  private reset(): void {
    this.hasMore = true;
    this.total = 0;
    this.lastMergedIndex = 0;
    this.sections = [];
    this.scrollState.clear();
  }

  private loadNextPagePhotos() {
    if (this.hasMore) {
      this.photoService.search(this.total, this.pageSize, this.query ? this.query.queryStr() : '')
        .subscribe(resp => {
          this.hasMore = resp.length == this.pageSize;
          this.handlePageResult(resp);
        });
    }
  }

  private updateBlock(block: Block, box) {
    block.top = Math.floor(box.top);
    block.left = Math.floor(box.left);
    block.width = Math.floor(box.width);
    block.height = Math.floor(box.height);
  }

  private handlePageResult(photos: Photo[]): void {
    this.total += photos.length;
    let newSections: Section[] = [];
    let adjustExistsSection: boolean;
    photos.forEach((photo, index) => {
      let key: string = new Date(photo.date).toLocaleDateString();
      let section: Section;
      if (this.sections.length > 0 && (this.sections[this.sections.length - 1].key === key)) {
        section = this.sections[this.sections.length - 1];
        adjustExistsSection = true;
      } else {
        if (newSections.length && newSections[newSections.length - 1].key === key) {
          section = newSections[newSections.length - 1];
        } else {
          section = new Section(key);
          newSections.push(section);
        }
      }
      let block: Block = new Block(photo);
      section.addBlock(block);
    });

    if (adjustExistsSection && this.sections.length) {
      let lastSection: Section = this.sections[this.sections.length - 1];
      let boxes = lastSection.boxes();
      let flexLayout = justifiedLayout(boxes, { containerWidth: this.containerWidth, targetRowHeight: this.targetHeight });
      lastSection.height = Math.floor(flexLayout.containerHeight);
      lastSection.blocks.forEach((block, index) => {
        this.updateBlock(block, flexLayout.boxes[index]);
      });
      lastSection.updateRows(this.blockSpacing);
    }
    //calc new sections
    newSections.forEach((section, index) => {
      let calc: Box[] = section.blocks.map(block => {
        return new Box(block.photo.width, block.photo.height);
      });

      let layout = justifiedLayout(calc, { containerWidth: this.containerWidth, targetRowHeight: this.targetHeight })
      let boxes = layout.boxes;
      let baseTop = 0;
      if (this.sections.length > 0) {
        baseTop = this.sections[this.sections.length - 1].top + this.sections[this.sections.length - 1].height;
      }
      section.height = Math.floor(layout.containerHeight);
      section.top = baseTop + 28;
      section.left = this.blockSpacing;
      section.show = true;

      let firstBreakRowIndex: number = -1;
      let foundFirstBreakRowIndex = false;
      section.blocks.forEach((block, i) => {
        block.width = Math.floor(boxes[i].width);
        block.height = Math.floor(boxes[i].height);
        block.top = Math.floor(boxes[i].top);
        block.left = Math.floor(boxes[i].left);
        if (i != 0 && block.left == this.blockSpacing && !foundFirstBreakRowIndex) {
          firstBreakRowIndex = i - 1;
          foundFirstBreakRowIndex = true;
        }
      });

      if (firstBreakRowIndex < 0) {
        firstBreakRowIndex = section.blocks.length - 1;
      }
      let sb: Block = section.blocks[firstBreakRowIndex];
      section.width = sb.left + sb.width + this.blockSpacing;
      section.updateRows(this.blockSpacing);
      this.sections.push(section);
    });
    this.mergeSection();
  }

  private mergeSection(): void {
    if (this.lastMergedIndex < this.sections.length) {
      let maxContainer = this.containerWidth - 30;
      for (let i = this.lastMergedIndex; i < this.sections.length - 2; i++) {
        let startSection: Section = this.sections[i];
        if (startSection.width < maxContainer) {
          let tempWidth = startSection.width;
          let nextSection: Section = this.sections[i + 1];
          if (nextSection.width < maxContainer && (tempWidth + nextSection.width) < maxContainer &&
            (tempWidth + this.getScaledSectionWidth(startSection, nextSection, false)) < maxContainer) {
            nextSection.top = startSection.top;
            nextSection.left = startSection.left + startSection.width + 28;
            this.getScaledSectionWidth(startSection, nextSection, true);
            this.moveUpNextSections(i + 2, startSection.height + 28);
          }
        }
      }
      this.lastMergedIndex = this.sections.length - 2;
    }
  }

  private moveUpNextSections(index: number, height: number) {
    for (let i = index; i < this.sections.length; i++) {
      this.sections[i].top -= height;
    }
  }

  // (20,10)->(?,5)
  private getScaledSectionWidth(startSection: Section, mayMergeSection: Section, changeWidth: boolean): number {
    let scaleToHeight: number = startSection.lastBlock().height;
    let newWidth: number = 0;
    mayMergeSection.blocks.forEach((bl, index) => {
      let ratio = scaleToHeight / bl.height;
      let width = Math.floor(ratio * bl.width);
      newWidth += width;
      if (changeWidth) {
        bl.width = width;
        bl.height = scaleToHeight;
        if (index == 0) {
          bl.left = this.blockSpacing;
        } else {
          let pre: Block = mayMergeSection.blocks[index - 1];
          bl.left = pre.left + pre.width + this.blockSpacing;
        }
      }
    });
    newWidth += ((mayMergeSection.blocks.length - 1) * this.blockSpacing);
    if (changeWidth) {
      mayMergeSection.width = newWidth + 20;
    }
    return newWidth;
  }

  /**
   * Converting given number pixel to css pixel format.
   * @param pixel Given pixel number value
   */
  public pixel(pixel: number): string {
    return pixel + 'px';
  }

  public titleRender(date: string): string {
    let todayYear: number = this.today.getFullYear();
    let curYear: number = new Date(date).getFullYear();
    let fmt: string = todayYear == curYear ? 'M月d日EEE' : 'yyyy年M月d日EEE';
    return formatDate(date, fmt, this.locale);
  }

}
