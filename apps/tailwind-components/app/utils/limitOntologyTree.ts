import type { IOntologyTreeItem } from "../../types/types";

export function countOntologyNodes(nodes: IOntologyTreeItem[]): number {
  let total = 0;
  for (const node of nodes) {
    total += 1;
    if (node.children?.length) {
      total += countOntologyNodes(node.children);
    }
  }
  return total;
}

export function limitOntologyTree(
  nodes: IOntologyTreeItem[],
  limit: number
): IOntologyTreeItem[] {
  let remaining = limit;

  function limitLevel(level: IOntologyTreeItem[]): IOntologyTreeItem[] {
    const result: IOntologyTreeItem[] = [];
    for (const node of level) {
      if (remaining <= 0) break;
      remaining -= 1;
      result.push({
        ...node,
        children: node.children?.length
          ? limitLevel(node.children)
          : node.children,
      });
    }
    return result;
  }

  return limitLevel(nodes);
}
