export type FilterOperator =
  | "equals"
  | "like"
  | "between"
  | "in"
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
  [columnId: string]: IGraphQLFilterValue | string | undefined;
}
