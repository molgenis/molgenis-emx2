import { describe, it, expect } from "vitest";
import {
  buildOntologyTree,
  flattenParentChain,
  sortOntologyTree,
} from "../../../app/utils/buildOntologyTree";
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

  it("deduplicates items with the same name", () => {
    const items: IOntologyTreeItem[] = [
      { name: "Cardiology", parent: { name: "Medicine" } },
      { name: "Neurology", parent: { name: "Medicine" } },
    ];
    const result = buildOntologyTree(items);
    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("Medicine");
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
});

describe("flattenParentChain", () => {
  it("returns single item when no parent", () => {
    const item: IOntologyTreeItem = { name: "Root" };
    expect(flattenParentChain(item)).toHaveLength(1);
  });

  it("returns item and all ancestors", () => {
    const item: IOntologyTreeItem = {
      name: "Leaf",
      parent: { name: "Middle", parent: { name: "Root" } },
    };
    const result = flattenParentChain(item);
    expect(result).toHaveLength(3);
    expect(result.map((i) => i.name)).toEqual(["Leaf", "Middle", "Root"]);
  });
});

describe("sortOntologyTree", () => {
  it("sorts by order when all items have order defined", () => {
    const tree: IOntologyTreeItem[] = [
      { name: "B", order: 2 },
      { name: "A", order: 1 },
    ];
    const result = sortOntologyTree(tree);
    expect(result[0].name).toBe("A");
    expect(result[1].name).toBe("B");
  });

  it("sorts alphabetically when not all items have order", () => {
    const tree: IOntologyTreeItem[] = [{ name: "B", order: 2 }, { name: "A" }];
    const result = sortOntologyTree(tree);
    expect(result[0].name).toBe("A");
    expect(result[1].name).toBe("B");
  });

  it("recursively sorts children", () => {
    const tree: IOntologyTreeItem[] = [
      {
        name: "Root",
        children: [{ name: "Z" }, { name: "A" }],
      },
    ];
    const result = sortOntologyTree(tree);
    expect(result[0].children![0].name).toBe("A");
    expect(result[0].children![1].name).toBe("Z");
  });
});
