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

  const nodesByName = new Map<string, IOntologyTreeItem>();
  for (const item of allItems) {
    nodesByName.set(item.name, copyOntologyItem(item));
  }

  for (const node of nodesByName.values()) {
    if (!node.parent) {
      continue;
    }
    const parent = nodesByName.get(node.parent.name);
    if (!parent) {
      continue;
    }
    if (!parent.children) {
      parent.children = [];
    }
    if (!parent.children.some((child) => child.name === node.name)) {
      parent.children.push(node);
    }
  }

  const roots = [...nodesByName.values()].filter((node) => !node.parent);
  return sortOntologyTree(roots);
};

const copyOntologyItem = (item: IOntologyTreeItem): IOntologyTreeItem =>
  item.children ? { ...item, children: [...item.children] } : { ...item };

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
  return [...tree]
    .sort((a, b) => {
      return sortBy === "order" &&
        a.order !== undefined &&
        b.order !== undefined
        ? a.order - b.order
        : a.name.localeCompare(b.name);
    })
    .map((item) =>
      item.children
        ? { ...item, children: sortOntologyTree(item.children) }
        : item
    );
};
