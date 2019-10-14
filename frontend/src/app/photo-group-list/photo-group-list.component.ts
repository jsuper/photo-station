import { Component, OnInit, ElementRef, Inject, LOCALE_ID } from '@angular/core';
import { PhotoService } from 'app/photo.service';
import { Photo } from 'app/photo.model';
import { Section, Block } from 'app/sections/section.model';
import { ActivatedRoute, Params, Router } from '@angular/router';
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

  private query: Query;

  private scrollState: Map<Number, Number> = new Map();


  constructor(private photoService: PhotoService,
    private activeRoute: ActivatedRoute,
    private el: ElementRef,
    private routeStateService: RouteStateService,
    @Inject(LOCALE_ID) private locale: string,
    private dialog: MatDialog,
    private sectionService: SectionService,
    private router: Router) {
    this.reset();
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
    let currentUrl: string = decodeURIComponent(this.router.url);
    let uriIndex = currentUrl.indexOf('?');
    let uri = uriIndex > 0 ? currentUrl.substring(0, uriIndex) : currentUrl;
    console.log('Called:' + uri);

    this.routeStateService.setComponent(this);
    let container = this.el.nativeElement.getElementsByClassName('group-list')[0];
    this.containerWidth = container.clientWidth;

    let maxRows: number = Math.ceil((window.innerHeight - 72) / this.targetHeight);
    let rowCols: number = Math.ceil(this.containerWidth / 300);
    this.pageSize = maxRows * rowCols;

    this.sectionService.setLayoutConfig(container.clientWidth, this.targetHeight, this.blockSpacing, this.blockSpacing);
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
      this.loadNextPagePhotos();
    }
  }

  openImageViewer(sectionIndex: number, photoIndex: number): void {
    let sec: Section = this.sections()[sectionIndex];
    let block: Block = sec.blocks[photoIndex];

    if (block.selected || this.selectedBlocks() > 0) {
      this.checkBlock(sectionIndex, photoIndex);
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
          sections: this.sections().length,
          photos: this.total,
          photo: photo,
          indexes: [sectionIndex, photoIndex],
          photoReader: (section, block) => {
            return this.sections()[section].blocks[block].photo;
            // return this.allSections[section].blocks[block].photo;
          },
          blocksOfSection: sectionIndex => { return this.sections()[sectionIndex].blocks.length }
        }
      });
    }

  }

  checkSection(sectionIndex: number): void {
    this.sectionService.selectSection(sectionIndex);

  }

  checkBlock(sectionIndex: number, blockIndex: number): void {
    this.sectionService.selectBlock(sectionIndex, blockIndex);
  }

  sections(): Section[] {
    return this.sectionService.getSections();
  }
  selectedSections(): number {
    return this.sectionService.numberOfSelectedSections();
  }

  selectedBlocks(): number {
    return this.sectionService.numberOfSelectedBlocks();
  }

  private reset(): void {
    this.hasMore = true;
    this.total = 0;
    this.scrollState.clear();
    this.sectionService.reset();
  }

  private loadNextPagePhotos() {
    if (this.hasMore) {
      let q = this.query ? this.query.queryStr() : '';
      if (!q.length) {
        q = 'deleted:0';
      } else {
        q += ',-deleted:0';
      }
      this.photoService.search(this.total, this.pageSize, q)
        .subscribe(resp => {
          this.hasMore = resp.length == this.pageSize;
          this.total += resp.length;
          this.sectionService.addNewLoadedPhotos(resp);
          this.sectionService.mergeSections(this.hasMore);

          if (!this.hasMore) {
            console.log(`Total loaded photos: ${this.total}`);

          }
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
    return 'translateZ(0px) scale3d(' + widthScale + ',' + heightScale + ', 1)';
  }

}
