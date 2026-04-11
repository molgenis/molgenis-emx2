import type { Ref, ComputedRef } from "vue";
import type { columnValue, IColumn } from "../../metadata-utils/src/types";
import type { CountedOption } from "../app/utils/fetchCounts";

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

export type IFilterValue =
  | {
      operator: "equals" | "like" | "notNull" | "isNull";
      value: columnValue;
    }
  | { operator: "between"; value: [columnValue, columnValue] };

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

export interface IGraphQLFilterMatchAnyIncludingChildren {
  _match_any_including_children: string[];
}

export type IGraphQLFilterValue =
  | IGraphQLFilterEquals
  | IGraphQLFilterLike
  | IGraphQLFilterBetween
  | IGraphQLFilterNull
  | IGraphQLFilterMatchAnyIncludingChildren;

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

export interface UseFilters {
  filterStates: Ref<Map<string, IFilterValue>>;
  searchValue: Ref<string> | ComputedRef<string>;
  gqlFilter: ComputedRef<IGraphQLFilter>;
  activeFilters: ComputedRef<ActiveFilter[]>;
  setFilter: (columnId: string, value: IFilterValue | null) => void;
  setSearch: (value: string) => void;
  clearFilters: () => void;
  removeFilter: (columnId: string) => void;
  columns: Ref<IColumn[]>;
  visibleFilterIds: Ref<string[]>;
  toggleFilter: (columnId: string) => void;
  resetFilters: () => void;
  getCountedOptions: (columnId: string) => ComputedRef<CountedOption[]>;
  isCountLoading: (columnId: string) => ComputedRef<boolean>;
  nestedColumnMeta: Ref<
    Map<
      string,
      {
        label: string;
        columnType: string;
        refTableId?: string | null;
        refSchemaId?: string | null;
      }
    >
  >;
  registerNestedColumn: (
    id: string,
    meta: {
      label: string;
      columnType: string;
      refTableId?: string | null;
      refSchemaId?: string | null;
    }
  ) => void;
  schemaId: string;
  tableId: string;
}
