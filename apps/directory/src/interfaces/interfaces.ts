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
