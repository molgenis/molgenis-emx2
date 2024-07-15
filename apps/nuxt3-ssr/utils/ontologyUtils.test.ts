import { describe, it, expect } from "vitest";
import type { IOntologyParentTreeItem } from "../interfaces/types";
import { buildTree, flattenTree } from "./ontologyUtils";

describe("flattenTree", () => {
  it("passing an instance without parent should return a list containing only the item", () => {
    const item: IOntologyParentTreeItem = {
      name: "item",
    };
    expect(flattenTree(item)).toEqual([item]);
  });

  it("passing an instance with a parent should return a list containing the item followed by its parent", () => {
    const item: IOntologyParentTreeItem = {
      name: "item",
      parent: {
        name: "parent",
      },
    };
    expect(flattenTree(item)).toEqual([item, item.parent]);
  });

  it("should add al the parents  ", () => {
    const item: IOntologyParentTreeItem = {
      name: "item",
      parent: {
        name: "parent",
        parent: {
          name: "grandparent",
        },
      },
    };
    expect(flattenTree(item)).toEqual([item, item.parent, item.parent?.parent]);
  });
});

describe("buildTree", () => {
  const itemA: IOntologyParentTreeItem = { name: "A" };
  const itemB: IOntologyParentTreeItem = { name: "B" };
  const itemC: IOntologyParentTreeItem = { name: "C", parent: itemA };

  it("if items occur multiple times in the input they should be deduplicated", () => {
    expect(buildTree([itemA, itemB, itemA, itemB])).toEqual([itemA, itemB]);
  });

  it("should only return root nodes at the top level ", () => {
    expect(buildTree([itemA, itemB, itemC])).toEqual([itemA, itemB]);
  });

  it("should return a list of trees ", () => {
    const trees = buildTree([itemA, itemB, itemC]);
    expect(trees.length).toEqual(2);
    expect(trees[0].name).toEqual("A");
    //@ts-ignore
    expect(trees[0].children[0].name).toEqual("C");
    expect(trees[1].name).toEqual("B");
  });

  it("should return a tree ", () => {
    const trees = buildTree([
      { name: "A", parent: { name: "B", parent: { name: "C" } } },
    ]);
    expect(trees.length).toEqual(1);
    expect(trees[0].name).toEqual("C");
    //@ts-ignore
    expect(trees[0].children[0].name).toEqual("B");
    //@ts-ignore
    expect(trees[0].children[0].children[0].name).toEqual("A");
  });

  it("children should be unique", () => {
    const tree = buildTree([
      { name: "C1", parent: { name: "A" } },
      { name: "C1", parent: { name: "A" } },
    ]);
    //@ts-ignore
    expect(tree[0].children.length).toEqual(1);
    //@ts-ignore
    expect(tree[0].children[0].name).toEqual("C1");
  });
});
