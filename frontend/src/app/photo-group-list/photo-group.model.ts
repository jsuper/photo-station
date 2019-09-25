import { Photo } from 'app/photo.model';
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
  constructor(key: string) {
    this.key = key;
    this.title = key;
  }

  addBlock(block: Block): number {
    return this.blocks.push(block);
  }

  lastBlock(): Block {
    return this.blocks[this.blocks.length - 1];
  }


  updateRows(blockSpacing: number): void {
    this.rows = this.blocks.filter(val => val.left == blockSpacing).length;
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
    console.log(`t=${this.height < this.photo.height}`);

    return '/api/photo/' + this.photo.id + '?t=true&w=' + this.width + '&h=' + this.height;
  }
}
