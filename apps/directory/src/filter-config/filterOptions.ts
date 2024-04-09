//@ts-ignore
import { QueryEMX2 } from "molgenis-components";
import { useFiltersStore } from "../stores/filtersStore";

/** Async so we can fire and forget for performance. */
async function cache(facetIdentifier: any, filterOptions: any) {
  const { filterOptionsCache } = useFiltersStore() as any;
  filterOptionsCache[facetIdentifier] = filterOptions;
}

function retrieveFromCache(facetIdentifier: any) {
  const { filterOptionsCache } = useFiltersStore() as any;
  return filterOptionsCache[facetIdentifier] || [];
}

/** Configurable array of values to filter out, for example 'Other, unknown' that make no sense to the user. */
function removeOptions(filterOptions: any, filterFacet: any) {
  const optionsToRemove = filterFacet.removeOptions;

  if (!optionsToRemove || !optionsToRemove.length) return filterOptions;

  optionsToRemove.map((option: any) => option.toLowerCase());
  return filterOptions.filter(
    (filterOption: any) =>
      !optionsToRemove.includes(filterOption.text.toLowerCase())
  );
}

export const customFilterOptions = (filterFacet: any) => {
  const { facetIdentifier, customOptions } = filterFacet;
  return () =>
    new Promise((resolve) => {
      const cachedOptions = retrieveFromCache(facetIdentifier);

      if (!cachedOptions.length) {
        cache(facetIdentifier, customOptions);
        resolve(customOptions);
      } else {
        resolve(customOptions);
      }
    });
};

function _mapToOptions(
  row: any,
  filterLabelAttribute: any,
  filterValueAttribute: any
) {
  return {
    text: row[filterLabelAttribute],
    value: row[filterValueAttribute],
  };
}

export const genericFilterOptions = (filterFacet: any) => {
  const {
    sourceTable,
    sourceSchema,
    facetIdentifier,
    filterLabelAttribute,
    filterValueAttribute,
    sortColumn,
    sortDirection,
  } = filterFacet;

  return () =>
    new Promise((resolve) => {
      const cachedOptions = retrieveFromCache(facetIdentifier);

      const selection = [filterLabelAttribute, filterValueAttribute];

      if (!cachedOptions.length) {
        new QueryEMX2(getSchema(sourceSchema))
          .table(sourceTable)
          .select(selection)
          .orderBy(sourceTable, sortColumn, sortDirection)
          .execute()
          .then((response: any) => {
            let filterOptions = response[sourceTable].map((row: any) => {
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
                  ? row.children.map(
                      (child: any) => child[filterValueAttribute]
                    )
                  : undefined,
              };
            });

            /**  remove unwanted options if applicable */
            filterOptions = removeOptions(filterOptions, filterFacet);

            cache(facetIdentifier, filterOptions);
            resolve(filterOptions);
          });
      } else {
        resolve(cachedOptions);
      }
    });
};

export const ontologyFilterOptions = (filterFacet: any) => {
  const {
    ontologyIdentifiers,
    sourceSchema,
    sourceTable,
    facetIdentifier,
    filterLabelAttribute,
    filterValueAttribute,
    sortColumn,
    sortDirection,
  } = filterFacet;

  return () =>
    new Promise((resolve) => {
      const cachedOptions = retrieveFromCache(facetIdentifier);

      const selection = [
        filterLabelAttribute,
        filterValueAttribute,
        "code",
        `parent.${filterValueAttribute}`,
      ];

      if (!cachedOptions.length) {
        /** make it query after all the others, saves 50% of initial load */
        const waitAfterBiobanks = setTimeout(() => {
          new QueryEMX2(getSchema(sourceSchema))
            .table(sourceTable)
            .select(selection)
            .orderBy(sourceTable, sortColumn, sortDirection)
            .execute()
            .then((response: any) => {
              const itemsSplitByOntology = getItemsSplitByOntology(
                response[sourceTable],
                ontologyIdentifiers
              );

              cache(facetIdentifier, itemsSplitByOntology);
              resolve(itemsSplitByOntology);
            });
          clearTimeout(waitAfterBiobanks);
        }, 1000);
      } else {
        resolve(cachedOptions);
      }
    });
};
interface OntologyItem {
  label: string;
  name: string;
  code: string;
  parent?: { name: string }[];
  children?: OntologyItem[];
}

function getItemsSplitByOntology(
  ontologyItems: OntologyItem[],
  ontologyIdentifiers: string[]
): Record<string, OntologyItem[]> {
  const childrenPerParent = getChildrenPerParent(ontologyItems);
  const itemsWithChildren = getItemsWithChildren(
    ontologyItems,
    childrenPerParent
  );

  const rootNodes = itemsWithChildren.filter(
    (item: any) => !item.parent?.length
  );

  const itemsSplitByOntology = splitItemsByOntology(
    rootNodes,
    ontologyIdentifiers
  );
  return itemsSplitByOntology;
}

function getItemsWithChildren(
  items: OntologyItem[],
  childrenPerParent: Record<string, OntologyItem[]>
) {
  return items.map((item) => {
    if (childrenPerParent[item.name]) {
      item.children = childrenPerParent[item.name];
    }
    return item;
  });
}

function splitItemsByOntology(
  rootNodes: OntologyItem[],
  ontologyIdentifiers: string[]
): Record<string, OntologyItem[]> {
  const itemsSplitByOntology: Record<string, OntologyItem[]> = {};
  for (const ontologyItem of rootNodes) {
    for (const ontologyId of ontologyIdentifiers) {
      if (ontologyItem.name.toLowerCase().includes(ontologyId.toLowerCase())) {
        if (!itemsSplitByOntology[ontologyId]) {
          itemsSplitByOntology[ontologyId] = [ontologyItem];
        } else {
          itemsSplitByOntology[ontologyId].push(ontologyItem);
        }
      }
    }
  }
  return itemsSplitByOntology;
}

function getChildrenPerParent(
  items: OntologyItem[]
): Record<string, OntologyItem[]> {
  let childrenPerParent: Record<string, OntologyItem[]> = {};
  items.forEach((item) => {
    item.parent?.forEach((parent) => {
      if (childrenPerParent[parent.name]) {
        childrenPerParent[parent.name].push(item);
      } else {
        childrenPerParent[parent.name] = [item];
      }
    });
  });
  return childrenPerParent;
}

export function getSchema(sourceSchema: string | undefined) {
  return sourceSchema
    ? `${window.location.protocol}//${window.location.host}/${sourceSchema}`
    : "graphql";
}
