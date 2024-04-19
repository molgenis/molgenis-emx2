import type {
  IFilter,
  ISearchFilter,
  IConditionsFilter,
} from "~/interfaces/types";

import { isConditionFilter } from "./filterUtils";

const buildFilterVariables = (filters: IConditionsFilter[]) => {
  const filtersVariables = filters.reduce<
    Record<string, Record<string, any | string>>
  >((accum, filter) => {
    if (filter.config.columnId && filter?.conditions?.length) {
      if (filter.config.filterTable) {
        if (!accum[filter.config.filterTable]) {
          accum[filter.config.filterTable] = {};
        }
        accum[filter.config.filterTable][filter.config.columnId] = {
          equals: filter.conditions,
        };
      } else if (filter.config.buildFilterFunction) {
        accum[filter.config.columnId] = filter.config.buildFilterFunction(
          filter.conditions
        );
      } else {
        accum[filter.config.columnId] = { equals: filter.conditions };
      }
    }

    return accum;
  }, {});

  return filtersVariables;
};

export const buildQueryFilter = (filters: IFilter[]) => {
  // build the active (non search) filters
  const conditionsFilters = filters.filter(isConditionFilter);

  let filterBuilder = buildFilterVariables(conditionsFilters);
  const searchFilter = filters.find((f) => f.config.type === "SEARCH");
  if (searchFilter?.search) {
    // add the search to the filters
    filterBuilder = {
      ...filterBuilder,
      ...{ _or: [{ _search: searchFilter.search }] },
    };

    // expand the search to the sub tables
    (searchFilter as ISearchFilter).config.searchTables?.forEach((sub) => {
      filterBuilder["_or"].push({ [sub]: { _search: searchFilter.search } });
    });
  }
  return filterBuilder;
};
