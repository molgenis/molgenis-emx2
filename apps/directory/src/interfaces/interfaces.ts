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

export interface IFilterFacet {
  sourceTable: string;
  sourceSchema: string;
  facetIdentifier: string;
  filterLabelAttribute: string;
  filterValueAttribute: string;
  extraAttributes?: string[];
  sortColumn: string;
  sortDirection: "ASC" | "DESC";
  ontologyIdentifiers?: string[];
  customOptions?: any;
}
