export interface IOntologyItem {
  label: string;
  name: string;
  code: string;
  parent?: { name: string }[];
  children?: IOntologyItem[];
}

export interface IBiobankIdentifier {
  name: string;
}

export interface IFilterOption {
  text: string;
  value: string;
}
