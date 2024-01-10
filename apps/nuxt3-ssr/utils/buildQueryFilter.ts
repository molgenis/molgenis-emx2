import type {
  IFilter,
  ISearchFilter,
  IConditionsFilter,
} from "~/interfaces/types";

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
  const conditionsFilters = filters.filter(
    (f) => f.conditions?.length
  ) as IConditionsFilter[];

  let filterBuilder = buildFilterVariables(conditionsFilters);
  const searchFilter = filters.find(
    (f) => f.config.type === "SEARCH"
  ) as ISearchFilter;
  if (searchFilter.search) {
    // add the search to the filters
    filterBuilder = {
      ...filterBuilder,
      ...{ _or: [{ _search: searchFilter.search }] },
    };

    // expand the search to the sub tables
    searchFilter.config.searchTables?.forEach((sub) => {
      filterBuilder["_or"].push({ [sub]: { _search: searchFilter.search } });
    });
  }
  return filterBuilder;
};
