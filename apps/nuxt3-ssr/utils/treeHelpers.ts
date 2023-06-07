export const removeChildIfParentSelected = (nodes: INode[]) => {
  const selectedCodes = nodes.map(node => node.code);
  return nodes.filter(
    node => !(node.parent && selectedCodes.includes(node.parent.code))
  );
};
