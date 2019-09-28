export interface FlexLayout {
    containerHeight?: number,
    boxes?: Box[],
}

export class Box {
    top: number;
    left: number;
    width: number;
    height: number;
}