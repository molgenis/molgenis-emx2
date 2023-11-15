import { IFilter } from "~/interfaces/types";

const buildFilterVariables = (filters: IFilter[]) => {
  const filtersVariables = filters.reduce<
    Record<string, Record<string, any | string>>
  >((accum, filter) => {
    if (filter.columnId && filter?.conditions?.length) {
      if (filter.filterTable) {
        if (!accum[filter.filterTable]) {
          accum[filter.filterTable] = {};
        }
        accum[filter.filterTable][filter.columnId] = {
          equals: filter.conditions,
        };
      } else {
        accum[filter.columnId] = { equals: filter.conditions };
      }
    }

    return accum;
  }, {});

  return filtersVariables;
};

export const buildQueryFilter = (filters: IFilter[], searchString?: string) => {
  // build the active (non search) filters
  let filterBuilder = buildFilterVariables(filters);
  if (searchString) {
    // add the search to the filters
    // @ts-ignore (dynamic object)
    filterBuilder = {
      ...filterBuilder,
      ...{ _or: [{ _search: searchString }] },
    };
    // expand the search to the subtabels
    // @ts-ignore (dynamic object)
    filters
      .find((f) => f.columnType === "_SEARCH")
      ?.searchTables?.forEach((sub) => {
        filterBuilder["_or"].push({ [sub]: { _search: searchString } });
      });
  }
  return { _and: filterBuilder };
};
