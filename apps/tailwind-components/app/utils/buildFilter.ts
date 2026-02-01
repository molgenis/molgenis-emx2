import type { IColumn } from "../../../metadata-utils/src/types";
import type { IFilterValue, IGraphQLFilter } from "../../types/filters";

function setNestedValue(obj: any, path: string[], value: any): void {
  if (path.length === 0) return;
  let current = obj;
  for (let i = 0; i < path.length - 1; i++) {
    const key = path[i] as string;
    if (!current[key]) current[key] = {};
    current = current[key];
  }
  const lastKey = path[path.length - 1] as string;
  current[lastKey] = value;
}

export function buildGraphQLFilter(
  filterStates: Map<string, IFilterValue>,
  columns: IColumn[],
  searchValue?: string
): IGraphQLFilter {
  const filter: IGraphQLFilter = {};

  if (searchValue && searchValue.trim()) {
    filter._search = searchValue.trim();
  }

  filterStates.forEach((filterValue, columnId) => {
    const pathSegments = columnId.split(".");
    const rootColumnId = pathSegments[0];
    const column = columns.find((c) => c.id === rootColumnId);
    if (!column) return;

    const { operator, value } = filterValue;

    let filterValueObj: any;
    switch (operator) {
      case "equals":
        filterValueObj = { equals: value };
        break;

      case "like":
        filterValueObj = { like: value };
        break;

      case "between": {
        const [min, max] = value as [any, any];
        const betweenFilter: Record<string, any> = {};
        if (min != null) betweenFilter.min = min;
        if (max != null) betweenFilter.max = max;
        if (Object.keys(betweenFilter).length > 0) {
          filterValueObj = { between: betweenFilter };
        }
        break;
      }

      case "in":
        if (Array.isArray(value) && value.length > 0) {
          filterValueObj = { equals: value };
        } else if (value != null) {
          filterValueObj = { equals: [value] };
        }
        break;

      case "notNull":
        filterValueObj = { notNull: true };
        break;

      case "isNull":
        filterValueObj = { isNull: true };
        break;
    }

    if (filterValueObj) {
      if (column.columnType === "FILE") {
        setNestedValue(filter, [...pathSegments, "name"], filterValueObj);
      } else {
        setNestedValue(filter, pathSegments, filterValueObj);
      }
    }
  });

  return filter;
}
