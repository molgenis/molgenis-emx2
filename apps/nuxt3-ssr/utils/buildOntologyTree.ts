export const buildOntologyTree = (
  data: { name: string; parent?: { name: string } }[]
) => {
  if (!data || !data.length) {
    return [];
  }
  const nodes = new Map();

  function flattenTree(item: {
    name: string;
    parent?: { name: string };
    code?: string;
    order?: number;
    definition?: string;
    ontologyTermURI?: string;
  }) {
    const newParentName = item?.parent?.name;
    const oldItem = nodes.get(item.name);
    const oldParentName = oldItem?.parent;
    if (!oldItem || (newParentName && !oldParentName)) {
      nodes.set(
        item.name,
        (({ name, code, order, parent, definition, ontologyTermURI }) => ({
          name,
          code,
          order,
          parent: parent?.name,
          definition,
          ontologyTermURI,
        }))(item)
      );
    }

    if (item.parent) {
      flattenTree(item.parent);
    }
  }

  // depth first find item in tree by name
  function findInTree(tree: { name: string; children: [] }[], name: string) {
    return tree.reduce((accum: any, elem) => {
      if (elem.name === name) {
        accum = elem;
      } else {
        const result = findInTree(elem.children, name);
        if (result) {
          accum = result;
        }
      }
      return accum;
    }, undefined);
  }

  // flatten raw response to list of nodes
  data.forEach((element: { name: string; parent?: { name: string } }) => {
    flattenTree(element);
  });

  // list of all unique tree nodes
  const nodeList = Array.from(nodes.values());

  // prepare the list to make the flipped tree
  const treeItems = nodeList.map(node => {
    node.children = [];
    return node;
  });

  // build the tree by adding each node to its parents child list ( if it had a parent)
  const dirtyTree = nodeList.reduce((treeItems, node) => {
    if (!node.parent) {
      return treeItems;
    }

    const parentName = node.parent;
    const parentTreeItem = findInTree(treeItems, parentName);
    parentTreeItem.children.push(node);

    return treeItems;
  }, treeItems);

  const tree = dirtyTree.filter(
    (node: { name: string; parent: {}[] }) => !node.parent
  );

  return ontologyTreeSortOrder(tree);
};

const ontologyTreeSortOrder = (tree: any) => {
  return tree.sort((a: any, b: any) => {
    if (a.order !== undefined && b.order !== undefined) {
      return a.order > b.order;
    } else {
      return a.name.localeCompare(b.name);
    }
  });
};
