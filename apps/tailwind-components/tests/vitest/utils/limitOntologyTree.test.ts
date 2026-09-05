import { describe, expect, it } from "vitest";
import {
  countOntologyNodes,
  limitOntologyTree,
} from "../../../app/utils/limitOntologyTree";
import type { IOntologyTreeItem } from "../../../types/types";

describe("countOntologyNodes", () => {
  it("counts every node across every level", () => {
    const tree: IOntologyTreeItem[] = [
      {
        name: "A",
        children: [{ name: "A1" }, { name: "A2", children: [{ name: "A2a" }] }],
      },
      { name: "B" },
    ];
    expect(countOntologyNodes(tree)).toBe(5);
  });
});

describe("limitOntologyTree", () => {
  const tree: IOntologyTreeItem[] = [
    { name: "A", children: [{ name: "A1" }, { name: "A2" }] },
    { name: "B", children: [{ name: "B1" }] },
  ];

  it("keeps the whole tree when the limit is not reached", () => {
    expect(limitOntologyTree(tree, 10)).toEqual(tree);
  });

  it("cuts depth-first: a node's own subtree is consumed before its sibling", () => {
    expect(limitOntologyTree(tree, 2)).toEqual([
      { name: "A", children: [{ name: "A1" }] },
    ]);
  });

  it("drops a whole sibling once the budget hits zero mid-level", () => {
    expect(limitOntologyTree(tree, 3)).toEqual([
      { name: "A", children: [{ name: "A1" }, { name: "A2" }] },
    ]);
  });

  it("returns an empty tree for a zero budget", () => {
    expect(limitOntologyTree(tree, 0)).toEqual([]);
  });
});
