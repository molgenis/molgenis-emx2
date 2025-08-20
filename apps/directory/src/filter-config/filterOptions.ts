//@ts-ignore
import { QueryEMX2 } from "molgenis-components";
import { IFilterFacet, IOntologyItem } from "../interfaces/interfaces";

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

function _mapToOptions(
  row: Record<string, any>,
  filterLabelAttribute: string,
  filterValueAttribute: string,
  extraAttributes?: string[]
): Record<string, any> {
  if (extraAttributes?.length) {
    return {
      text: row[filterLabelAttribute],
      value: row[filterValueAttribute],
      extraAttributes: getExtraAttributes(row, extraAttributes),
    };
  } else {
    return {
      text: row[filterLabelAttribute],
      value: row[filterValueAttribute],
    };
  }
}

function getExtraAttributes(row: Record<string, any>, attributes: string[]) {
  return attributes.reduce((accum: Record<string, any>, attribute: string) => {
    if (attribute.includes(".")) {
      const [parent, child] = attribute.split(".");
      accum[attribute] = row[parent][child];
    } else {
      accum[attribute] = row[attribute];
    }
    return accum;
  }, {});
}

export async function genericFilterOptions(filterFacet: IFilterFacet) {
  const {
    sourceTable,
    sourceSchema,
    filterLabelAttribute,
    filterValueAttribute,
    extraAttributes,
    sortColumn,
    sortDirection,
  } = filterFacet;

  const selection = getAttributeSelection(
    filterLabelAttribute,
    filterValueAttribute,
    extraAttributes
  );

  return new QueryEMX2(getSchema(sourceSchema))
    .table(sourceTable)
    .select(selection)
    .orderBy(sourceTable, sortColumn, sortDirection)
    .execute()
    .then((response: Record<string, Record<string, any>>) => {
      let filterOptions = response[sourceTable].map(
        (row: Record<string, any>) => {
          let result = _mapToOptions(
            row,
            filterLabelAttribute,
            filterValueAttribute,
            extraAttributes
          );
          const parent = row.parent
            ? row.parent[filterValueAttribute]
            : undefined;
          if (parent) {
            result[parent] = parent;
          }
          const children = row.children?.map(
            (child: any) => child[filterValueAttribute]
          );
          if (children) {
            result[children] = children;
          }
          return result;
        }
      );

      /**  remove unwanted options if applicable */
      filterOptions = removeOptions(filterOptions, filterFacet);

      return filterOptions;
    });
}

function getAttributeSelection(
  filterLabelAttribute: string,
  filterValueAttribute: string,
  extraAttributes?: string[]
) {
  if (extraAttributes?.length) {
    return [filterLabelAttribute, filterValueAttribute, ...extraAttributes];
  } else {
    return [filterLabelAttribute, filterValueAttribute];
  }
}

export function ontologyFilterOptions(filterFacet: IFilterFacet) {
  const {
    ontologyIdentifiers,
    sourceSchema,
    sourceTable,
    filterLabelAttribute,
    filterValueAttribute,
    sortColumn,
    sortDirection,
  } = filterFacet;

  const selection = [
    filterLabelAttribute,
    filterValueAttribute,
    "code",
    `parent.${filterValueAttribute}`,
  ];

  /** make it query after all the others, saves 50% of initial load */
  setTimeout(() => {}, 1000);
  return new QueryEMX2(getSchema(sourceSchema))
    .table(sourceTable)
    .select(selection)
    .orderBy(sourceTable, sortColumn, sortDirection)
    .execute()
    .then((response: any) => {
      const itemsSplitByOntology = getItemsSplitByOntology(
        response[sourceTable],
        ontologyIdentifiers!
      );

      return itemsSplitByOntology;
    });
}

function getItemsSplitByOntology(
  ontologyItems: IOntologyItem[],
  ontologyIdentifiers: string[]
): Record<string, IOntologyItem[] | Record<string, IOntologyItem>> {
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

  return { ...itemsSplitByOntology };
}

function getItemsWithChildren(
  items: IOntologyItem[],
  childrenPerParent: Record<string, IOntologyItem[]>
) {
  return items.map((item) => {
    if (childrenPerParent[item.name]) {
      item.children = childrenPerParent[item.name];
    }
    return item;
  });
}

function splitItemsByOntology(
  rootNodes: IOntologyItem[],
  ontologyIdentifiers: string[]
): Record<string, IOntologyItem[]> {
  const itemsSplitByOntology: Record<string, IOntologyItem[]> = {};
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
  items: IOntologyItem[]
): Record<string, IOntologyItem[]> {
  let childrenPerParent: Record<string, IOntologyItem[]> = {};
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
