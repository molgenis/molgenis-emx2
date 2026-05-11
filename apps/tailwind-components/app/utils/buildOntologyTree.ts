export interface IOntologyTreeItem {
  name: string;
  label?: string;
  definition?: string;
  order?: number;
  code?: string;
  ontologyTermURI?: string;
  parent?: IOntologyTreeItem;
  children?: IOntologyTreeItem[];
}

export const buildOntologyTree = (
  items: IOntologyTreeItem | IOntologyTreeItem[] | null | undefined
): IOntologyTreeItem[] => {
  if (!items) {
    return [];
  }

  const itemsArray = Array.isArray(items) ? items : [items];

  const allItems = itemsArray.map(flattenParentChain).flat();

  const uniqueItems = [
    ...new Map(allItems.map((item) => [item.name, item])).values(),
  ];

  for (const item of uniqueItems) {
    if (!item.parent) {
      continue;
    }
    const parent = item.parent;
    const parentIndex = uniqueItems.findIndex(
      (candidate) => candidate.name === parent.name
    );

    if (parentIndex !== -1 && uniqueItems[parentIndex]) {
      if (!uniqueItems[parentIndex].children) {
        uniqueItems[parentIndex].children = [];
      }
      const children = uniqueItems[parentIndex].children!;
      if (!children.find((child) => child.name === item.name)) {
        children.push(item);
      }
    }
  }

  const roots = uniqueItems.filter((item) => !item.parent);
  return sortOntologyTree(roots);
};

export const flattenParentChain = (
  item: IOntologyTreeItem
): IOntologyTreeItem[] => {
  if (!item) {
    return [];
  }
  if (!item.parent) {
    return [item];
  }
  return [item, ...flattenParentChain(item.parent)];
};

export const sortOntologyTree = (
  tree: IOntologyTreeItem[]
): IOntologyTreeItem[] => {
  const sortBy = tree.every((item) => item.order !== undefined)
    ? "order"
    : "name";
  tree.sort((a, b) => {
    return sortBy === "order" && a.order !== undefined && b.order !== undefined
      ? a.order - b.order
      : a.name.localeCompare(b.name);
  });

  for (const item of tree) {
    if (item.children) {
      item.children = sortOntologyTree(item.children);
    }
  }

  return tree;
};
