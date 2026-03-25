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

export type FilterValue =
  | string
  | number
  | boolean
  | Record<string, unknown>
  | Record<string, unknown>[]
  | (string | number | null)[]
  | null;

export interface IFilterValue {
  operator: FilterOperator;
  value: FilterValue;
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

import type { Ref, ComputedRef } from "vue";
import type { IColumn } from "../../metadata-utils/src/types";
import type { ICountFetcher } from "../app/utils/createCountFetcher";

export interface ResolvedFilter {
  fullPath: string;
  column: IColumn;
  label: string;
}

export interface UseFilters {
  filterStates: Ref<Map<string, IFilterValue>>;
  searchValue: Ref<string> | ComputedRef<string>;
  gqlFilter: Ref<IGraphQLFilter>;
  activeFilters: ComputedRef<ActiveFilter[]>;
  setFilter: (columnId: string, value: IFilterValue | null) => void;
  setSearch: (value: string) => void;
  clearFilters: () => void;
  removeFilter: (columnId: string) => void;
  columns: Ref<IColumn[]>;
  visibleFilterIds: Ref<string[]>;
  toggleFilter: (columnId: string) => void;
  resetFilters: () => void;
  loadRefColumns: (path: string, column: IColumn) => Promise<void>;
  getRefColumns: (path: string) => IColumn[];
  resolvedFilters: ComputedRef<ResolvedFilter[]>;
  setFilterValue: (
    columnId: string,
    value: IFilterValue | null | undefined
  ) => Promise<void>;
  getCountFetcher: (columnPath: string) => ICountFetcher;
}
