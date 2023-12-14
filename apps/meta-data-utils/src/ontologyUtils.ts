import type { IOntologyItem, IOntologyParentTreeItem } from "./types";

/*
 * Takes a list of ontology items linked via parents and returns a list of trees ( root nodes , each with their children)
 */
export const buildTree = (
  selectedOntologyItems: IOntologyParentTreeItem[]
): IOntologyItem[] => {
  if (!selectedOntologyItems) {
    return [];
  }

  // list-of-tree to list
  const allItemsList = selectedOntologyItems.map(flattenTree).flat();

  // remove duplicates
  const uniqueItems = allItemsList.filter(
    (item, index, self) => self.findIndex((i) => equals(i, item)) === index
  ) as IOntologyItem[];

  // build tree add leaves to their parents
  for (const item of uniqueItems) {
    if (!item.parent) {
      continue;
    }
    const parentIndex = uniqueItems.findIndex((i) => equals(i, item.parent));

    if (!uniqueItems[parentIndex].children) {
      uniqueItems[parentIndex].children = [];
    }

    if (!uniqueItems[parentIndex].children.find((i) => equals(i, item))) {
      uniqueItems[parentIndex].children.push(item);
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
