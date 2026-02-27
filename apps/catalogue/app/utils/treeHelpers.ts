interface TreeNodeWithParent {
  code?: string;
  parent?: { code: string };
}

export const removeChildIfParentSelected = (nodes: TreeNodeWithParent[]) => {
  const selectedCodes = nodes.map((node) => node.code);
  return nodes.filter(
    (node) => !(node.parent && selectedCodes.includes(node.parent.code))
  );
};
