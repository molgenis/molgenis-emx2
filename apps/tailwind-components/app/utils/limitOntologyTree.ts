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

/**
 * Depth-first pre-order cut against a whole-tree node budget: a node past
 * the limit, and its whole subtree, is genuinely dropped. Item paging only
 * hides with CSS; this is the one place DOM absence is accepted.
 */
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
