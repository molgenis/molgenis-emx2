import type {
  IOntologyItem,
  IOntologyParentTreeItem,
} from "~/interfaces/types";

/*
 * Takes a list of ontology items linked via parents and returns a list of trees ( root nodes , each with their children)
 */
export const buildTree = (
  selectedOntologyItems: IOntologyParentTreeItem[]
): IOntologyItem[] => {
  if (!selectedOntologyItems) {
    return [];
  } else if (!Array.isArray(selectedOntologyItems)) {
    selectedOntologyItems = [selectedOntologyItems];
  }

  // list-of-tree to list
  const allItemsList = selectedOntologyItems.map(flattenTree).flat();

  // remove duplicates
  const uniqueItems = [
    ...new Map(allItemsList.map((v) => [v.name, v])).values(),
  ] as IOntologyItem[];

  // build tree add leaves to their parents
  for (const item of uniqueItems) {
    if (!item.parent) {
      continue;
    }
    const parent = item.parent as IOntologyItem;
    const parentIndex = uniqueItems.findIndex((i) => equals(i, parent));

    if (!uniqueItems[parentIndex].children) {
      uniqueItems[parentIndex].children = [];
    }

    if (
      !(uniqueItems[parentIndex].children as IOntologyItem[]).find((i) =>
        equals(i, item)
      )
    ) {
      (uniqueItems[parentIndex].children as IOntologyItem[]).push(item);
    }
  }

  const roots = uniqueItems.filter((item) => !item.parent);
  return roots;
};

export const flattenTree = (
  ontologyItem: IOntologyParentTreeItem
): IOntologyParentTreeItem[] => {
  if (!ontologyItem) {
    return [];
  } else if (!ontologyItem.parent) {
    return [ontologyItem];
  } else {
    return [ontologyItem, ...flattenTree(ontologyItem.parent)];
  }
};

const equals = (a: IOntologyItem, b: IOntologyItem) => a.name === b.name;
