export interface FlexConfig {
  containerWidth?: number,
  targetRowHeight?: number,
  boxSpacing?: number,
  containerPadding?: number,
}

export interface FlexLayout {
  containerHeight?: number,
  boxes?: Box[],
}

export interface Segment {
  boxes?: Box[],
}

export class Box {
  top: number;
  left: number;
  width: number;
  height: number;
  row: number ; //row number
}
