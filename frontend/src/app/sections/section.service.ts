import { Injectable } from '@angular/core';
import { Section, Block } from './section.model';
import { Photo } from 'app/photo.model';
import { FlexLayoutService } from 'app/flex-layout/flex-layout.service';
import { FlexLayout, Box } from 'app/flex-layout/flex-layout.model';
import { merge } from 'rxjs/operators';
import { Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SectionService {

  private selected: Map<string, string> = new Map();

  private selectedSections: number = 0;
  private selectedBlocks: number = 0;

  private containerWidth: number = 0;
  private targetRowHeight: number = 0;
  private blockSpace: number = 4;

  private sections: Section[] = [];//all sections
  private totalPhotos: number = 0;//loaded photo size
  private lastMergeIndex: number = 0;
  private layoutService: FlexLayoutService = new FlexLayoutService({});
  constructor() {
  }

  public setLayoutConfig(containerWidth: number, targetRowHeight: number, blockSpace: number): void {
    this.containerWidth = containerWidth;
    this.targetRowHeight = targetRowHeight;
    this.blockSpace = blockSpace;
    this.layoutService.updateConfig({
      containerWidth: this.containerWidth,
      targetRowHeight: this.targetRowHeight,
      boxSpacing: this.blockSpace,
      containerPadding: this.blockSpace,
    });
  }

  /**
   * 将新加载的图片添加到块中, 并返回当前已加载的图片的数量
   * @param photos 新加载的图片
   */
  public addNewLoadedPhotos(photos: Photo[]): number {
    this.totalPhotos += photos.length;
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
      let boxes: Box[] = lastSection.boxes();
      let flexLayout: FlexLayout = this.layoutService.layout(boxes);
      lastSection.height = Math.floor(flexLayout.containerHeight);
      lastSection.updateBlockBox(flexLayout.boxes);
      lastSection.calculateWidth(this.blockSpace);
      lastSection.updateRows(this.blockSpace);
    }
    //calc new sections
    newSections.forEach((section, index) => {
      let calc: Box[] = section.blocks.map(block => block.box());

      let layout: FlexLayout = this.layoutService.layout(calc);
      let boxes = layout.boxes;

      let baseTop = 0;
      if (this.sections.length > 0) {
        baseTop = this.sections[this.sections.length - 1].top + this.sections[this.sections.length - 1].height;
      }
      section.height = Math.ceil(layout.containerHeight);
      section.top = baseTop + 28;
      section.left = this.blockSpace;
      section.show = true;

      section.updateBlockBox(boxes);
      section.calculateWidth(this.blockSpace);
      section.updateRows(this.blockSpace);

      this.sections.push(section);
    });
    // this.mergeSections();
    return this.totalPhotos;
  }

  public mergeSections(hasMore: boolean): void {
    if (this.lastMergeIndex < this.sections.length) {
      let mergeSize: number = hasMore ? this.sections.length - 2 : this.sections.length - 1;
      let maxContainer = this.containerWidth;
      for (let i = this.lastMergeIndex; i < mergeSize; i++) {
        let startSection: Section = this.sections[i];
        let nextSection: Section = this.sections[i + 1];
        // let logMessage: string = 'Merge section from ' + startSection.title + ', check next section: ' + nextSection.title + ' can be merged -> ';
        if (startSection.width < maxContainer && nextSection.width < maxContainer &&
          (startSection.width + nextSection.width + 48 - this.blockSpace) < maxContainer) {

          // let scaledWidth: number = this.getScaledSectionWidth(startSection, nextSection, false);
          let scaledWidth: number = nextSection.scaling(startSection.height, this.blockSpace, false);
          let newRowWidth: number = startSection.left + startSection.width + scaledWidth + 48 - this.blockSpace;
          if (newRowWidth <= maxContainer) {
            // logMessage += ' Section ' + nextSection.title + ' can be merge up, it\'s width is: ' + nextSection.width;
            let moveUpHeight: number = nextSection.height;
            let left = startSection.left + startSection.width + 48 - this.blockSpace;
            nextSection.top = startSection.top;
            nextSection.left = left;
            // this.getScaledSectionWidth(startSection, nextSection, true);
            nextSection.scaling(startSection.height, this.blockSpace, true);
            this.moveUpNextSections(i + 2, moveUpHeight + 24);
          }
        }
      }
      this.lastMergeIndex = mergeSize;
    }
  }

  private moveUpNextSections(index: number, height: number) {
    for (let i = index; i < this.sections.length; i++) {
      this.sections[i].top -= height;
    }
  }

  public reset(): void {
    this.sections = [];
    this.totalPhotos = 0;
    this.selectedSections = 0;
    this.selectedBlocks = 0;
    this.lastMergeIndex = 0 ;
    this.selected.clear();
  }

  /**
   * 返回当前选中的section数量
   * @param index 选中的section下标
   * @param state 选中状态
   */
  public selectSection(sectionIndex: number): number {
    let section: Section = this.sections[sectionIndex];
    if (section) {
      if (section.selected) {
        section.selected = 0;
        this.selectedSections--;
        this.selectedBlocks -= section.blocks.length;
      } else {
        section.selected = section.blocks.length;
        this.selectedSections++;
        this.selectedBlocks += section.blocks.length;
      }
      let blockSelect = section.selected > 0;
      section.blocks.forEach((val, index) => {
        val.selected = blockSelect;
        let key: string = sectionIndex + '-' + index;
        if (blockSelect) {
          this.selected.set(key, val.photo.id);
        } else {
          this.selected.delete(key);
        }
      });
    }
    return this.selectedSections;
  }

  public selectBlock(sectionIndex: number, blockIndex: number): number {

    let section: Section = this.sections[sectionIndex];
    if (section) {
      let block: Block = section.blocks[blockIndex];
      block.selected = !block.selected;
      let key = sectionIndex + '-' + blockIndex;
      if (block.selected) {
        this.selectedBlocks++;
        section.selected++;
        this.selected.set(key, block.photo.id);
      } else {
        this.selectedBlocks--;
        section.selected--;
        this.selected.delete(key);
      }
    }
    return this.selectedBlocks;
  }

  public clearSelections(): void {
    console.log(this.selected);

    this.selected.forEach((value, key, map) => {
      let ks: string[] = key.split('-');
      console.log('clear select: ' + key);
      this.selectBlock(parseInt(ks[0]), parseInt(ks[1]));
    });
    this.selectedBlocks = 0;
    this.selectedSections = 0;

  }

  public getSections(): Section[] {
    return this.sections;
  }

  public sectionLength(): number {
    return this.sections.length;
  }

  public numberOfSelectedSections(): number {
    return this.selectedSections;
  }

  public numberOfSelectedBlocks(): number {
    return this.selectedBlocks;
  }
}
