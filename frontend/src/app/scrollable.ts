import { ElementRef } from '@angular/core';

export class ScrollEvent {

}

/**
 * 滚动事件
 */
export interface Scrollable {

    scrollUp(scrollEl: ElementRef);

    scrollDown(scrollEl: ElementRef);
}