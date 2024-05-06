export interface IOntologyItem {
  label: string;
  name: string;
  code: string;
  parent?: { name: string }[];
  children?: IOntologyItem[];
}

export interface IFilter {
  label?: string;
  name?: string;
  code?: string;
  value: any;
  text: string;
  parent?: { name: string }[];
  children?: any[];
}
