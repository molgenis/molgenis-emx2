import { describe, it, expect } from "vitest";
import { buildOntologyTree } from "../../../app/utils/buildOntologyTree";
import type { IOntologyTreeItem } from "../../../app/utils/buildOntologyTree";

describe("buildOntologyTree", () => {
  it("returns empty array for null input", () => {
    expect(buildOntologyTree(null)).toEqual([]);
  });

  it("returns empty array for undefined input", () => {
    expect(buildOntologyTree(undefined)).toEqual([]);
  });

  it("returns single item wrapped in array", () => {
    const item: IOntologyTreeItem = { name: "Biology" };
    const result = buildOntologyTree(item);
    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("Biology");
  });

  it("returns flat list as-is when no parents", () => {
    const items: IOntologyTreeItem[] = [
      { name: "Genomics" },
      { name: "Proteomics" },
      { name: "Metabolomics" },
    ];
    const result = buildOntologyTree(items);
    expect(result).toHaveLength(3);
    expect(result.every((item) => !item.children)).toBe(true);
  });

  it("groups items with parents into tree", () => {
    const items: IOntologyTreeItem[] = [
      {
        name: "Cardiology",
        parent: { name: "Medicine" },
      },
      {
        name: "Neurology",
        parent: { name: "Medicine" },
      },
    ];
    const result = buildOntologyTree(items);
    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("Medicine");
    expect(result[0].children).toHaveLength(2);
    const childNames = result[0].children!.map((c) => c.name);
    expect(childNames).toContain("Cardiology");
    expect(childNames).toContain("Neurology");
  });

  it("builds multi-level tree from parent chain", () => {
    const items: IOntologyTreeItem[] = [
      {
        name: "Pediatric Cardiology",
        parent: {
          name: "Cardiology",
          parent: { name: "Medicine" },
        },
      },
    ];
    const result = buildOntologyTree(items);
    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("Medicine");
    expect(result[0].children).toHaveLength(1);
    expect(result[0].children![0].name).toBe("Cardiology");
    expect(result[0].children![0].children).toHaveLength(1);
    expect(result[0].children![0].children![0].name).toBe(
      "Pediatric Cardiology"
    );
  });

  it("sorts by order field when all items have order", () => {
    const items: IOntologyTreeItem[] = [
      { name: "C", order: 3 },
      { name: "A", order: 1 },
      { name: "B", order: 2 },
    ];
    const result = buildOntologyTree(items);
    expect(result.map((item) => item.name)).toEqual(["A", "B", "C"]);
  });

  it("leaves the items it was given untouched", () => {
    const items: IOntologyTreeItem[] = [
      { name: "Cardiology", parent: { name: "Medicine" } },
      { name: "Anatomy", children: [{ name: "Thorax" }, { name: "Abdomen" }] },
    ];
    const itemsBeforeCall = structuredClone(items);

    buildOntologyTree(items);

    expect(items).toEqual(itemsBeforeCall);
  });

  it("sorts alphabetically when no order field", () => {
    const items: IOntologyTreeItem[] = [
      { name: "Zebra" },
      { name: "Antelope" },
      { name: "Mouse" },
    ];
    const result = buildOntologyTree(items);
    expect(result.map((item) => item.name)).toEqual([
      "Antelope",
      "Mouse",
      "Zebra",
    ]);
  });

  // The four cases below carry rules that catalogue's own deleted buildTree
  // suite held and this one did not: repeated array entries, root nodes
  // coexisting with a child of one of them in the same input, the
  // list-of-trees shape, and unique children under a repeated parent+child
  // pair.
  it("deduplicates the same item occurring multiple times in the input", () => {
    const itemA: IOntologyTreeItem = { name: "A" };
    const itemB: IOntologyTreeItem = { name: "B" };
    const result = buildOntologyTree([itemA, itemB, itemA, itemB]);
    expect(result.map((item) => item.name)).toEqual(["A", "B"]);
  });

  it("returns only root nodes at the top level when a child of a root is also in the input", () => {
    const itemA: IOntologyTreeItem = { name: "A" };
    const itemB: IOntologyTreeItem = { name: "B" };
    const itemC: IOntologyTreeItem = { name: "C", parent: itemA };
    const result = buildOntologyTree([itemA, itemB, itemC]);
    expect(result.map((item) => item.name)).toEqual(["A", "B"]);
  });

  it("returns a list of trees, one per root", () => {
    const itemA: IOntologyTreeItem = { name: "A" };
    const itemB: IOntologyTreeItem = { name: "B" };
    const itemC: IOntologyTreeItem = { name: "C", parent: itemA };
    const trees = buildOntologyTree([itemA, itemB, itemC]);
    expect(trees).toHaveLength(2);
    expect(trees[0].name).toBe("A");
    expect(trees[0].children![0].name).toBe("C");
    expect(trees[1].name).toBe("B");
  });

  it("keeps children unique when the same child+parent pair repeats", () => {
    const items: IOntologyTreeItem[] = [
      { name: "C1", parent: { name: "A" } },
      { name: "C1", parent: { name: "A" } },
    ];
    const result = buildOntologyTree(items);
    expect(result[0].children).toHaveLength(1);
    expect(result[0].children![0].name).toBe("C1");
  });

  // The Map keyed by name already collapses two flat array entries for the
  // same child before the linking loop runs, so the case above never
  // exercises the .some() guard in the loop itself. This one does: the
  // parent item arrives with `children` already populated, and a second
  // item links the same child by name through `.parent`.
  it("keeps children unique when an item arrives with children already populated", () => {
    const items: IOntologyTreeItem[] = [
      { name: "C1", parent: { name: "A" } },
      { name: "A", children: [{ name: "C1" }] },
    ];
    const result = buildOntologyTree(items);
    expect(result).toHaveLength(1);
    expect(result[0].children).toHaveLength(1);
  });
});
