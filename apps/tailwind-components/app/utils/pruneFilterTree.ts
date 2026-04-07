import type { ITreeNodeState } from "../../types/types";

export function setAllNodesVisible(node: ITreeNodeState): ITreeNodeState {
  return {
    ...node,
    visible: true,
    children: (node.children as ITreeNodeState[]).map(setAllNodesVisible),
  };
}

export function pruneTree(
  nodes: ITreeNodeState[],
  query: string
): ITreeNodeState[] {
  if (!query.trim()) return nodes;

  const words = query.toLowerCase().split(/\s+/).filter(Boolean);

  function matchesNode(node: ITreeNodeState): boolean {
    const label = (node.label || node.name).toLowerCase();
    return words.every((word) => label.includes(word));
  }

  function prune(nodes: ITreeNodeState[]): ITreeNodeState[] {
    const result: ITreeNodeState[] = [];
    for (const node of nodes) {
      if (matchesNode(node)) {
        result.push({ ...setAllNodesVisible(node), expanded: true });
      } else if (node.children?.length) {
        const prunedChildren = prune(node.children as ITreeNodeState[]);
        if (prunedChildren.length > 0) {
          result.push({
            ...node,
            visible: true,
            children: prunedChildren,
            expanded: true,
          });
        }
      }
    }
    return result;
  }

  return prune(nodes);
}

export function pruneStringNodes(nodes: ITreeNodeState[]): ITreeNodeState[] {
  const result: ITreeNodeState[] = [];
  for (const node of nodes) {
    if (node.children?.length) {
      const prunedChildren = pruneStringNodes(
        node.children as ITreeNodeState[]
      );
      if (prunedChildren.length > 0) {
        result.push({ ...node, children: prunedChildren });
      }
    } else if (node.visible !== false) {
      result.push(node);
    }
  }
  return result;
}
