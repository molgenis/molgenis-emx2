export interface ActiveFilter {
  columnId: string;
  label: string;
  displayValue: string;
  values: string[];
}

export type FilterOperator =
  | "equals"
  | "like"
  | "between"
  | "notNull"
  | "isNull";

export interface IFilterValue {
  operator: FilterOperator;
  value: any;
}

// GraphQL filter types - matches EMX2 GraphQL API
export interface IGraphQLFilterEquals {
  equals: unknown;
}

export interface IGraphQLFilterLike {
  like: string;
}

export interface IGraphQLFilterBetween {
  between: { min?: unknown; max?: unknown };
}

export interface IGraphQLFilterNull {
  notNull?: boolean;
  isNull?: boolean;
}

export type IGraphQLFilterValue =
  | IGraphQLFilterEquals
  | IGraphQLFilterLike
  | IGraphQLFilterBetween
  | IGraphQLFilterNull;

export interface IGraphQLFilter {
  _search?: string;
  _or?: IGraphQLFilter[];
  _and?: IGraphQLFilter[];
  [columnId: string]:
    | IGraphQLFilterValue
    | IGraphQLFilter[]
    | string
    | undefined;
}
