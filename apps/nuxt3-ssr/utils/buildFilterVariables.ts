export const buildFilterVariables = (filters: IFilter[]) => {
  const filtersVariables = filters.reduce<
    Record<string, Record<string, object | string>>
  >((accum, filter) => {
    if (filter.columnName && filter?.conditions?.length) {
      if (filter.filterTable) {
        if (!accum[filter.filterTable]) {
          accum[filter.filterTable] = {};
        }
        accum[filter.filterTable][filter.columnName] = {
          equals: filter.conditions,
        };
      } else {
        accum[filter.columnName] = { equals: filter.conditions };
      }
    }

    return accum;
  }, {});

  return filtersVariables;
};

export const buildQueryFilter = (filters: IFilter[], searchString: string) => {
}

