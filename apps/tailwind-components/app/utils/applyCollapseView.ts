import type { CountedOption } from "./fetchCounts";

export interface CollapseViewOptions {
  hideZero: boolean;
  limit: number | null;
}

function hasNonZeroDescendant(node: CountedOption): boolean {
  if (!node.children || node.children.length === 0) return false;
  return node.children.some(
    (child) => child.count > 0 || hasNonZeroDescendant(child)
  );
}

function pruneZeros(nodes: CountedOption[]): CountedOption[] {
  return nodes
    .filter((node) => node.count > 0 || hasNonZeroDescendant(node))
    .map((node) => ({
      ...node,
      children:
        node.children && node.children.length > 0
          ? pruneZeros(node.children)
          : node.children,
    }));
}

export function countAllNodes(nodes: CountedOption[]): number {
  let total = 0;
  for (const node of nodes) {
    total += 1;
    if (node.children && node.children.length > 0) {
      total += countAllNodes(node.children);
    }
  }
  return total;
}

export function applyCollapseView(
  options: CountedOption[],
  { hideZero, limit }: CollapseViewOptions
): CountedOption[] {
  const afterZeroFilter = hideZero ? pruneZeros(options) : options;

  if (limit === null) return afterZeroFilter;

  if (afterZeroFilter.length <= limit) return afterZeroFilter;

  return afterZeroFilter.slice(0, limit);
}

function nodeMatchesQuery(node: CountedOption, query: string): boolean {
  const lower = query.toLowerCase();
  const label = (node.label ?? node.name).toLowerCase();
  return label.includes(lower);
}

function filterNode(node: CountedOption, query: string): CountedOption | null {
  const selfMatches = nodeMatchesQuery(node, query);
  const filteredChildren = (node.children ?? [])
    .map((child) => filterNode(child, query))
    .filter((child): child is CountedOption => child !== null);

  if (selfMatches) {
    return { ...node, children: node.children ? node.children : undefined };
  }
  if (filteredChildren.length > 0) {
    return { ...node, children: filteredChildren };
  }
  return null;
}

export function filterOptionsBySearch(
  options: CountedOption[],
  query: string
): CountedOption[] {
  if (!query) return options;
  return options
    .map((node) => filterNode(node, query))
    .filter((node): node is CountedOption => node !== null);
}
