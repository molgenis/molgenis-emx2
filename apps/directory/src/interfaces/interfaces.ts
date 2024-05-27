export interface IOntologyItem {
  label: string;
  name: string;
  code: string;
  parent?: { name: string }[];
  children?: IOntologyItem[];
}
