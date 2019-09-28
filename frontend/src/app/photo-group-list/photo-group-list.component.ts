import { Component, OnInit, ElementRef, Inject, LOCALE_ID } from '@angular/core';
import { PhotoService } from 'app/photo.service';
import * as justifiedLayout from 'justified-layout'
import { Photo } from 'app/photo.model';
import { Section, Block } from 'app/sections/section.model';
import { ActivatedRoute, Params } from '@angular/router';
import { Scrollable } from 'app/scrollable';
import { RouteStateService } from 'app/route-state.service';
import { formatDate } from '@angular/common';
import { PhotoViewerComponent } from 'app/photo-viewer/photo-viewer.component';
import { MatDialog } from '@angular/material/dialog';
import { AppComponent } from 'app/app.component';

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
  readonly blockSpacing: number = 4;

  sections: Section[] = [];
  total: number = 0;
  pageSize: number = 15;
  hasMore: boolean = true;
  targetHeight: number = 200;
  containerWidth: number;
  lastMergedIndex = 0;
  checkedSections = 0;
  checkedBlocks = 0;

  private query: Query;

  private scrollState: Map<Number, Number> = new Map();


  constructor(private photoService: PhotoService,
    private activeRoute: ActivatedRoute,
    private el: ElementRef,
    private routeStateService: RouteStateService,
    @Inject(LOCALE_ID) private locale: string,
    private dialog: MatDialog,
    @Inject(AppComponent) private app: AppComponent) {

    this.activeRoute.queryParams.subscribe(qp => {
      let q = qp['q'] || '';
      let field = this.activeRoute.snapshot.params['field'];

      let temp: Query = new Query(field, q);
      if (this.query == null) {
        this.query = temp;
      } else {
        if (!this.query.equals(temp)) {
          this.query = temp;
          this.reset();
          this.loadNextPagePhotos();
        }
      }
    });
  }


  ngOnInit() {
    this.routeStateService.setComponent(this);
    let container = this.el.nativeElement.getElementsByClassName('group-list')[0];
    this.containerWidth = container.clientWidth;
    console.log(this.containerWidth);



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
      // console.log(`Load next page: ${scrollPercent}, state: ${JSON.stringify(this.scrollState)}`);
      this.loadNextPagePhotos();
    }
  }

  openImageViewer(sectionIndex: number, photoIndex: number): void {
    let sec: Section = this.sections[sectionIndex];
    let block: Block = sec.blocks[photoIndex];

    if (this.checkedBlocks > 0 || block.checked) {
      this.checkBlock(sectionIndex, photoIndex);
      console.log(`There are ${this.checkedBlocks} blocks were checked.`);
      return;
    }

    if (sec && block) {
      let photo: Photo = block.photo;
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

  checkSection(sectionIndex: number): void {
    let section: Section = this.sections[sectionIndex];
    if (section.checked) {
      this.checkedSections--;
      section.checkAll(false);
      this.checkedBlocks -= section.blocks.length;
    } else {
      this.checkedSections++;
      section.checkAll(true);
      this.checkedBlocks += section.blocks.length;
    }
    this.app.setSelectedBlocks(this.checkedBlocks);

  }

  checkBlock(sectionIndex: number, blockIndex: number): void {
    let sec: Section = this.sections[sectionIndex];
    if (sec.checkBlock(blockIndex)) {
      this.checkedBlocks++;
    } else {
      this.checkedBlocks--;
    }
    this.app.setSelectedBlocks(this.checkedBlocks);
  }

  unselectAll(): void {
    this.checkedSections = 0;
    this.checkedBlocks = 0;
    this.sections.forEach(sec => {
      sec.checkAll(false);
    });
    this.app.setSelectedBlocks(this.checkedBlocks);
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
      let flexLayout = justifiedLayout(boxes, {
        containerWidth: this.containerWidth,
        targetRowHeight: this.targetHeight,
        boxSpacing: this.blockSpacing,
        containerPadding: this.blockSpacing,
      });
      lastSection.height = Math.floor(flexLayout.containerHeight);
      lastSection.updateBlockBox(flexLayout.boxes);
      lastSection.calculateWidth(this.blockSpacing);
      lastSection.updateRows(this.blockSpacing);
    }
    //calc new sections
    newSections.forEach((section, index) => {
      let calc: Box[] = section.blocks.map(block => {
        return new Box(block.photo.width, block.photo.height);
      });

      let layout = justifiedLayout(calc, {
        containerWidth: this.containerWidth,
        targetRowHeight: this.targetHeight,
        boxSpacing: this.blockSpacing,
        containerPadding: this.blockSpacing,
      })
      let boxes = layout.boxes;

      let baseTop = 0;
      if (this.sections.length > 0) {
        baseTop = this.sections[this.sections.length - 1].top + this.sections[this.sections.length - 1].height;
      }
      section.height = Math.ceil(layout.containerHeight);
      section.top = baseTop + 28;
      section.left = this.blockSpacing;
      section.show = true;

      section.updateBlockBox(boxes);
      section.calculateWidth(this.blockSpacing);
      section.updateRows(this.blockSpacing);

      this.sections.push(section);
    });
    this.mergeSection();
  }

  private mergeSection(): void {
    if (this.lastMergedIndex < this.sections.length) {
      let maxContainer = this.containerWidth;
      let mergeSize: number = this.hasMore ? this.sections.length - 2 : this.sections.length - 1;
      for (let i = this.lastMergedIndex; i < mergeSize; i++) {
        let startSection: Section = this.sections[i];
        let nextSection: Section = this.sections[i + 1];
        // let logMessage: string = 'Merge section from ' + startSection.title + ', check next section: ' + nextSection.title + ' can be merged -> ';
        if (startSection.width < maxContainer && nextSection.width < maxContainer &&
          (startSection.width + nextSection.width + 48 - this.blockSpacing) < maxContainer) {

          // let scaledWidth: number = this.getScaledSectionWidth(startSection, nextSection, false);
          let scaledWidth: number = nextSection.scaling(startSection.height, this.blockSpacing, false);
          let newRowWidth: number = startSection.left + startSection.width + scaledWidth + 48 - this.blockSpacing;
          if (newRowWidth <= maxContainer) {
            // logMessage += ' Section ' + nextSection.title + ' can be merge up, it\'s width is: ' + nextSection.width;
            let moveUpHeight: number = nextSection.height;
            let left = startSection.left + startSection.width + 48 - this.blockSpacing;
            nextSection.top = startSection.top;
            nextSection.left = left;
            // this.getScaledSectionWidth(startSection, nextSection, true);
            nextSection.scaling(startSection.height, this.blockSpacing, true);
            this.moveUpNextSections(i + 2, moveUpHeight + 24);
          }
        }
      }
      this.lastMergedIndex = mergeSize;
    }
  }

  private moveUpNextSections(index: number, height: number) {
    for (let i = index; i < this.sections.length; i++) {
      this.sections[i].top -= height;
    }
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

  getTransform(width: number, height: number): string {
    let widthScale = Math.ceil((width - 42) / width * 100) / 100;
    let heightScale = Math.ceil((height - 42) / height * 100) / 100;
    return `translateZ(0px) scale3d(${widthScale}, ${heightScale}, 1)`
  }

}
