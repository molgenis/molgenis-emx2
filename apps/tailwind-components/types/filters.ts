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
