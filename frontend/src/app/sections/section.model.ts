import { Photo, Location } from 'app/photo.model';
import { Box } from 'app/flex-layout/flex-layout.model';

/**
 * Grouped sections
 */
export class Section {
  top: number = 0;
  left: number = 0;
  width: number = 0;
  height: number = 0;

  title: string;
  key: string;

  blocks: Block[] = [];
  show: boolean;

  rows: number = 0;
  location: string;
  checked: number;

  constructor(key: string) {
    this.key = key;
    this.title = key;
  }

  addBlock(block: Block): number {
    if (!this.location || !this.location.length) {
      if (block.photo.location && block.photo.location.address) {
        this.location = block.photo.location.province == block.photo.location.city ?
          block.photo.location.nation + block.photo.location.province + block.photo.location.district :
          block.photo.location.nation + block.photo.location.province + block.photo.location.city + block.photo.location.district;
      }
    }
    return this.blocks.push(block);
  }

  lastBlock(): Block {
    return this.blocks[this.blocks.length - 1];
  }


  updateRows(blockSpacing: number): void {
    this.rows = this.blocks.filter(val => val.left == blockSpacing).length;
  }

  boxes(): Box[] {
    return this.blocks.map(block => block.box());
  }

  length(): number {
    return this.blocks.length;
  }

  checkAll(state: boolean): void {
    if (state) {
      this.checked = this.blocks.length;
    } else {
      this.checked = 0;
    }
    this.blocks.forEach(b => b.check(state));
  }

  checkBlock(index: number): boolean {
    let block: Block = this.blocks[index];
    if (block.checked) {
      this.checked--;
      block.check(false);
    } else {
      this.checked++;
      block.check(true);
    }
    return block.checked;
  }

  updateBlockBox(boxes: Array<any>): void {
    this.blocks.forEach((block, i) => {
      block.width = Math.floor(boxes[i].width);
      block.height = Math.floor(boxes[i].height);
      block.top = Math.floor(boxes[i].top);
      block.left = Math.floor(boxes[i].left);
    });
  }

  calculateWidth(blockSpacing: number): number {
    let firstBreakRowIndex: number = -1;
    let foundFirstBreakRowIndex = false;
    this.blocks.forEach((block, i) => {
      if (i != 0 && block.left == blockSpacing && !foundFirstBreakRowIndex) {
        firstBreakRowIndex = i - 1;
        foundFirstBreakRowIndex = true;
      }
    });

    if (firstBreakRowIndex < 0) {
      firstBreakRowIndex = this.blocks.length - 1;
    }
    let sb: Block = this.blocks[firstBreakRowIndex];
    this.width = sb.left + sb.width + blockSpacing;
    return this.width;
  }

  /**
   * 缩放section内部块的高度到目标高度，宽度按比例缩放
   * @param targetHeight 缩放的目标高度
   * @param blockSpace 块之间的间隔宽度
   * @param applyScale 是否更新自身、子模块的宽高度
   */
  scaling(targetHeight: number, blockSpace: number, applyScale: boolean): number {
    let newWidth: number = 0;
    this.blocks.forEach((bl, index) => {
      let ratio = targetHeight / bl.height;
      let width = Math.ceil(ratio * bl.width);
      newWidth += width;
      if (applyScale) {
        bl.width = width;
        bl.height = targetHeight;
        if (index == 0) {
          bl.left = blockSpace;
        } else {
          let pre: Block = this.blocks[index - 1];
          bl.left = pre.left + pre.width + blockSpace;
        }
      }
    });

    newWidth += ((this.blocks.length - 1) * blockSpace) + (2 * blockSpace);
    if (applyScale) {
      this.width = newWidth;
    }
    return newWidth;
  }


  public hasChecked(): boolean {
    return this.checked == this.blocks.length;
  }
}

export class Block {

  top: number = 0;
  left: number = 0;
  width: number = 0;
  height: number = 0;

  checked: boolean;
  photo: Photo;
  constructor(photo: Photo) {
    this.photo = photo;
  }

  url(): string {
    let loadOrigin: boolean = this.photo.size / 1024 <= 64;
    return `/api/photo/${this.photo.id}?t=${!loadOrigin}&w=${this.width}&h=${this.height}`;
  }

  box(): Box {
    let box: Box = new Box();
    box.width = this.photo.width;
    box.height = this.photo.height;
    return box;
  }

  check(state: boolean): void {
    this.checked = state;
  }
}
