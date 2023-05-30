/* istanbul ignore file */
import queryEMX2 from "../functions/queryEMX2";
import { useFiltersStore } from "../stores/filtersStore";

/** Async so we can fire and forget for performance. */
async function cache (applyToColumn, filterOptions) {
  const { filterOptionsCache } = useFiltersStore();
  filterOptionsCache[applyToColumn] = filterOptions;
}

function retrieveFromCache (applyToColumn) {
  const { filterOptionsCache } = useFiltersStore();
  return filterOptionsCache[applyToColumn] || [];
}

/** Configurable array of values to filter out, for example 'Other, unknown' that make no sense to the user. */
function removeOptions (filterOptions, filterFacet) {
  const optionsToRemove = filterFacet.removeOptions;

  if (!optionsToRemove || !optionsToRemove.length) return filterOptions;

  optionsToRemove.map((option) => option.toLowerCase());
  return filterOptions.filter(
    (filterOption) => !optionsToRemove.includes(filterOption.text.toLowerCase())
  );
}

export const customFilterOptions = (filterFacet) => {
  const { applyToColumn, customOptions } = filterFacet;
  return () =>
    new Promise((resolve) => {
      const cachedOptions = retrieveFromCache(applyToColumn);

      if (!cachedOptions.length) {
        cache(applyToColumn, customOptions);
        resolve(customOptions);
      } else {
        resolve(customOptions);
      }
    });
};

function _mapToOptions (row, filterLabelAttribute, filterValueAttribute) {
  return {
    text: row[filterLabelAttribute],
    value: row[filterValueAttribute],
  };
}

export const genericFilterOptions = (filterFacet) => {
  const {
    sourceTable,
    applyToColumn,
    filterLabelAttribute,
    filterValueAttribute,
    sortColumn,
    sortDirection,
  } = filterFacet;

  return () =>
    new Promise((resolve) => {
      const cachedOptions = retrieveFromCache(applyToColumn);

      const selection = [filterLabelAttribute, filterValueAttribute];

      if (!cachedOptions.length) {
        new queryEMX2("graphql")
          .table(sourceTable)
          .select(selection)
          .orderBy(sourceTable, sortColumn, sortDirection)
          .execute()
          .then((response) => {
            let filterOptions = response[sourceTable].map((row) => {
              return {
                ..._mapToOptions(
                  row,
                  filterLabelAttribute,
                  filterValueAttribute
                ),
                parent: row.parent
                  ? row.parent[filterValueAttribute]
                  : undefined,
                children: row.children
                  ? row.children.map((child) => child[filterValueAttribute])
                  : undefined,
              };
            });

            /**  remove unwanted options if applicable */
            filterOptions = removeOptions(filterOptions, filterFacet);

            cache(applyToColumn, filterOptions);
            resolve(filterOptions);
          });
      } else {
        resolve(cachedOptions);
      }
    });
};

export const ontologyFilterOptions = (filterFacet) => {
  const { ontologyIdentifiers, sourceTable, applyToColumn, filterLabelAttribute, filterValueAttribute, sortColumn, sortDirection } = filterFacet

  return () => new Promise((resolve) => {

    const cachedOptions = retrieveFromCache(applyToColumn)

    const selection = [
      filterLabelAttribute,
      filterValueAttribute,
      `parent.${filterValueAttribute}`,
      `children.${filterValueAttribute}`,
      `children.children.${filterValueAttribute}`,
      `children.children.children.${filterValueAttribute}`,
      `children.children.children.children.${filterValueAttribute}`,
      `children.children.children.children.children.${filterValueAttribute}`,
      `children.${filterLabelAttribute}`,
      `children.children.${filterLabelAttribute}`,
      `children.children.children.${filterLabelAttribute}`,
      `children.children.children.children.${filterLabelAttribute}`,
      `children.children.children.children.children.${filterLabelAttribute}`
    ]

    if (!cachedOptions.length) {
      new queryEMX2('graphql')
        .table(sourceTable)
        .select(selection)
        .orderBy(sourceTable, sortColumn, sortDirection)
        .execute()
        .then(response => {

          const onlyParents = response[sourceTable].filter(row => !row.parent)
          const itemsSplitByOntology = {};

          for (const ontologyItem of onlyParents) {
            for (const ontologyId of ontologyIdentifiers) {
              if (
                ontologyItem.name.toLowerCase().includes(ontologyId.toLowerCase())
              ) {
                if (!itemsSplitByOntology[ontologyId]) {
                  itemsSplitByOntology[ontologyId] = [ontologyItem];
                } else {
                  itemsSplitByOntology[ontologyId].push(ontologyItem);
                }
              }
            }

            cache(applyToColumn, itemsSplitByOntology);
            resolve(itemsSplitByOntology);
          }
        });
    } else {
      resolve(cachedOptions);
    }
  });
};
