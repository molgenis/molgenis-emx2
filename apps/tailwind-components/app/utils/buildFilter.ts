import type { IColumn } from "../../../metadata-utils/src/types";
import type { IFilterValue, IGraphQLFilter } from "../../types/filters";

export function buildGraphQLFilter(
  filterStates: Map<string, IFilterValue>,
  columns: IColumn[],
  searchValue?: string
): IGraphQLFilter {
  const filter: IGraphQLFilter = {};

  // add search if present
  if (searchValue && searchValue.trim()) {
    filter._search = searchValue.trim();
  }

  // convert each filter state to GraphQL format
  filterStates.forEach((filterValue, columnId) => {
    const column = columns.find((c) => c.id === columnId);
    if (!column) return;

    const { operator, value } = filterValue;

    switch (operator) {
      case "equals":
        filter[columnId] = { equals: value };
        break;

      case "like":
        filter[columnId] = { like: value };
        break;

      case "between": {
        const [min, max] = value as [any, any];
        const betweenFilter: Record<string, any> = {};
        if (min != null) betweenFilter.min = min;
        if (max != null) betweenFilter.max = max;
        if (Object.keys(betweenFilter).length > 0) {
          filter[columnId] = { between: betweenFilter };
        }
        break;
      }

      case "in":
        if (Array.isArray(value) && value.length > 0) {
          filter[columnId] = { equals: value };
        } else if (value != null) {
          filter[columnId] = { equals: [value] };
        }
        break;

      case "notNull":
        filter[columnId] = { notNull: true };
        break;

      case "isNull":
        filter[columnId] = { isNull: true };
        break;
    }
  });

  return filter;
}
