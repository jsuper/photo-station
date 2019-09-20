export interface NavigationNode {
  url?: string;
  title?: string;
  tooltip?: string;
  hidden?: boolean;
  children?: NavigationNode[];
  params?: object;
  field?: string;
}

export interface CurrentNode {
  url: string;
  view: string;
  nodes: NavigationNode[];
}

export interface CurrentNodes {
  [view: string]: CurrentNode;
}

export interface AggregationItem {
  value?: string,
  counter?: number,
}
