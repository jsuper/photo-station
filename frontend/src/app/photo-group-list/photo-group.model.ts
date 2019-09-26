import { Photo, Location } from 'app/photo.model';
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

  boxes(): Array<Object> {
    return this.blocks.map(block => block.box());
  }

  length(): number {
    return this.blocks.length;
  }
}

export class Block {

  top: number = 0;
  left: number = 0;
  width: number = 0;
  height: number = 0;

  photo: Photo;
  constructor(photo: Photo) {
    this.photo = photo;
  }

  url(): string {
    let loadOrigin: boolean = this.photo.size / 1024 <= 64;
    return `/api/photo/${this.photo.id}?t=${!loadOrigin}&w=${this.width}&h=${this.height}`;
  }

  box(): object {
    return { width: this.photo.width, height: this.photo.height };
  }
}
