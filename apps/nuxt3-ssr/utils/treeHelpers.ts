export const removeChildIfParentSelected = (nodes: any[]) => {
  const selectedCodes = nodes.map((node: { code: any; }) => node.code);
  return nodes.filter(
    (node: { parent: { code: string; }; }) => !(node.parent && selectedCodes.includes(node.parent.code))
  );
};
