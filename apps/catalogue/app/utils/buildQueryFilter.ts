import type {
  IFilter,
  ISearchFilter,
  IConditionsFilter,
  IRefArrayFilterCustomConfig,
} from "~/interfaces/types";

import { isConditionFilter } from "./filterUtils";

const buildFilterVariables = (filters: IConditionsFilter[]) => {
  const filtersVariables = filters.reduce<
    Record<string, Record<string, any | string> | string>
  >((accum, filter) => {
    if (
      // @ts-ignore
      (filter.config.columnId ||
        (filter.config as IRefArrayFilterCustomConfig).buildFilterFunction) &&
      filter?.conditions?.length
    ) {
      if (filter.config.filterTable) {
        if (!accum[filter.config.filterTable]) {
          accum[filter.config.filterTable] = {};
        }
        // @ts-ignore
        accum[filter.config.filterTable][filter.config.columnId] = {
          equals: filter.conditions,
        };
      } else if (
        (filter.config as IRefArrayFilterCustomConfig).buildFilterFunction
      ) {
        const buildFilterFunction = (
          filter.config as IRefArrayFilterCustomConfig
        ).buildFilterFunction;
        if (buildFilterFunction) {
          accum = buildFilterFunction(accum, filter.conditions);
        }
      } else {
        // @ts-ignore
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
    if ((searchFilter as ISearchFilter).config.searchTables) {
      // add the search to the filters
      filterBuilder = {
        ...filterBuilder,
        ...{ _or: [{ _search: searchFilter.search }] },
      };
    } else {
      // add the search to the filters
      filterBuilder = {
        ...filterBuilder,
        ...{ _search: searchFilter.search },
      };
    }

    // expand the search to the sub tables
    (searchFilter as ISearchFilter).config.searchTables?.forEach((sub) => {
      // @ts-ignore
      filterBuilder["_or"].push({ [sub]: { _search: searchFilter.search } });
    });
  }
  return filterBuilder;
};
