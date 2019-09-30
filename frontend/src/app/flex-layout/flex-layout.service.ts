
import * as justifiedLayout from 'justified-layout'
import { Box, FlexLayout } from './flex-layout.model';

export class FlexLayoutService {

  private configuration: any;

  constructor(config: any) {
    this.configuration = config;
  }

  updateConfig(latest: any) {
    this.configuration = latest;
  }

  public layout(boxes: Box[]): FlexLayout {
    return justifiedLayout(boxes, this.configuration);
  }

  public static flex(boxes: Box[], config): FlexLayout {
    return justifiedLayout(boxes, config);
  }
}
