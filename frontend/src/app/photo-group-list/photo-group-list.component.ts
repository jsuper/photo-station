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
import { SectionService } from 'app/sections/section.service';

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

  allSections: Section[] = [];
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
    @Inject(AppComponent) private app: AppComponent,
    private sectionService: SectionService) {

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

    let maxRows: number = Math.ceil((window.innerHeight - 72) / this.targetHeight);
    let rowCols: number = Math.ceil(this.containerWidth / 300);
    this.pageSize = maxRows * rowCols;

    this.sectionService.setLayoutConfig(container.clientWidth, this.targetHeight, this.blockSpacing);
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
    let sec: Section = this.allSections[sectionIndex];
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
          sections: this.allSections.length,
          photos: this.total,
          photo: photo,
          indexes: [sectionIndex, photoIndex],
          photoReader: (section, block) => {
            return this.allSections[section].blocks[block].photo;
          },
          blocksOfSection: sectionIndex => { return this.allSections[sectionIndex].blocks.length }
        }
      });
    }

  }

  checkSection(sectionIndex: number): void {
    let section: Section = this.allSections[sectionIndex];
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
    let sec: Section = this.allSections[sectionIndex];
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
    this.allSections.forEach(sec => {
      sec.checkAll(false);
    });
    this.app.setSelectedBlocks(this.checkedBlocks);
  }

  sections(): Section[] {
    return this.sectionService.getSections();
  }

  private reset(): void {
    this.hasMore = true;
    this.total = 0;
    this.lastMergedIndex = 0;
    this.allSections = [];
    this.scrollState.clear();
  }

  private loadNextPagePhotos() {
    if (this.hasMore) {
      this.photoService.search(this.total, this.pageSize, this.query ? this.query.queryStr() : '')
        .subscribe(resp => {
          this.hasMore = resp.length == this.pageSize;
          this.total += resp.length;
          // this.handlePageResult(resp);
          this.sectionService.addNewLoadedPhotos(resp);
          this.sectionService.mergeSections(this.hasMore);
        });
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
