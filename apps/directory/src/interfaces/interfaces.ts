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
  value: string | boolean;
}

export interface IFilterFacet {
  component: string;
  facetTitle: string;
  negotiatorRequestString: string;
  applyToColumn?: string;
  showFacet?: boolean;
  adaptive?: boolean;
  builtIn?: boolean;
  customOptions?: any;
  extraAttributes?: string[];
  facetIdentifier?: string;
  filterLabelAttribute?: string;
  filterValueAttribute?: string;
  name?: string;
  ontologyIdentifiers?: string[];
  removeOptions?: string[];
  showMatchTypeSelector?: boolean;
  sourceSchema?: string;
  sourceTable?: string;
  sortColumn?: string;
  sortDirection?: "ASC" | "DESC" | "asc" | "desc";
  trueOption?: IFilterOption;
}

export interface IFilterDetails {
  adaptive: boolean;
  builtIn: boolean;
  component: string; // filter components.
  facetIdentifier: string;
  facetTitle: string;
  filterLabelAttribute: string;
  filterValueAttribute: string;
  matchTypeForFilter: string;
  negotiatorRequestString: string;
  ontologyIdentifiers: any[];
  options: Function;
  sortColumn: string;
  sortDirection: SortDirection;
  showFacet: boolean;
  showMatchTypeSelector: boolean;
  applyToColumn?: string;
  sourceTable?: string;
  trueOption?: IFilterOption;
}

export type SortDirection = "asc" | "desc";
