import { describe, it, expect } from "vitest";
import {
  pruneTree,
  pruneStringNodes,
} from "../../../app/utils/pruneFilterTree";
import type { ITreeNodeState } from "../../../types/types";

function makeNode(
  name: string,
  label: string,
  children: ITreeNodeState[] = [],
  visible = true
): ITreeNodeState {
  return {
    name,
    label,
    children,
    selectable: true,
    expanded: false,
    visible,
  };
}

describe("pruneTree", () => {
  it("returns all nodes when query is empty", () => {
    const nodes = [makeNode("age", "Age"), makeNode("name", "Name")];
    expect(pruneTree(nodes, "")).toEqual(nodes);
    expect(pruneTree(nodes, "   ")).toEqual(nodes);
  });

  it("returns matching leaf nodes", () => {
    const nodes = [makeNode("age", "Age"), makeNode("name", "Name")];
    const result = pruneTree(nodes, "age");
    expect(result).toHaveLength(1);
    expect(result[0]!.name).toBe("age");
  });

  it("returns empty array when nothing matches", () => {
    const nodes = [makeNode("age", "Age"), makeNode("name", "Name")];
    expect(pruneTree(nodes, "nonexistent")).toEqual([]);
  });

  it("matches label case-insensitively", () => {
    const nodes = [makeNode("age", "Age")];
    expect(pruneTree(nodes, "AGE")).toHaveLength(1);
    expect(pruneTree(nodes, "age")).toHaveLength(1);
  });

  it("requires all words to match (AND logic)", () => {
    const nodes = [makeNode("a", "Admission Date"), makeNode("b", "Age")];
    const result = pruneTree(nodes, "admission date");
    expect(result).toHaveLength(1);
    expect(result[0]!.name).toBe("a");
  });

  it("includes parent when child matches, expands it", () => {
    const child = makeNode("hospital.name", "Name");
    const parent = makeNode("hospital", "Hospital", [child]);
    const nodes = [parent];
    const result = pruneTree(nodes, "name");
    expect(result).toHaveLength(1);
    expect(result[0]!.name).toBe("hospital");
    expect(result[0]!.expanded).toBe(true);
    expect(result[0]!.children).toHaveLength(1);
    expect((result[0]!.children[0] as ITreeNodeState).name).toBe(
      "hospital.name"
    );
  });

  it("excludes parent when no child matches", () => {
    const child = makeNode("hospital.name", "Name");
    const parent = makeNode("hospital", "Hospital", [child]);
    const nodes = [makeNode("age", "Age"), parent];
    const result = pruneTree(nodes, "age");
    expect(result).toHaveLength(1);
    expect(result[0]!.name).toBe("age");
  });

  it("includes matching parent with all its children", () => {
    const child1 = makeNode("hospital.name", "Name");
    const child2 = makeNode("hospital.city", "City");
    const parent = makeNode("hospital", "Hospital", [child1, child2]);
    const result = pruneTree([parent], "hospital");
    expect(result).toHaveLength(1);
    expect(result[0]!.children).toHaveLength(2);
  });

  it("sets expanded=true on matching node", () => {
    const nodes = [makeNode("age", "Age")];
    const result = pruneTree(nodes, "age");
    expect(result[0]!.expanded).toBe(true);
  });

  it("preserves non-matching siblings of matching children", () => {
    const child1 = makeNode("h.name", "Name");
    const child2 = makeNode("h.city", "City");
    const parent = makeNode("h", "Hospital", [child1, child2]);
    const result = pruneTree([parent], "name");
    expect(result[0]!.children).toHaveLength(1);
    expect((result[0]!.children[0] as ITreeNodeState).name).toBe("h.name");
  });

  it("handles deeply nested matching", () => {
    const grandchild = makeNode("a.b.zip", "Zip Code");
    const child = makeNode("a.b", "Address", [grandchild]);
    const parent = makeNode("a", "Hospital", [child]);
    const result = pruneTree([parent], "zip");
    expect(result).toHaveLength(1);
    expect(result[0]!.children).toHaveLength(1);
    expect((result[0]!.children[0] as ITreeNodeState).children).toHaveLength(1);
  });
});

describe("pruneStringNodes", () => {
  it("keeps nodes with visible !== false", () => {
    const nodes = [makeNode("age", "Age", [], true)];
    expect(pruneStringNodes(nodes)).toHaveLength(1);
  });

  it("removes nodes with visible === false", () => {
    const nodes = [makeNode("name", "Name", [], false)];
    expect(pruneStringNodes(nodes)).toHaveLength(0);
  });

  it("keeps parent node when it has visible children", () => {
    const child1 = makeNode("h.name", "Name", [], false);
    const child2 = makeNode("h.id", "ID", [], true);
    const parent = makeNode("h", "Hospital", [child1, child2]);
    const result = pruneStringNodes([parent]);
    expect(result).toHaveLength(1);
    expect(result[0]!.children).toHaveLength(1);
  });

  it("removes parent when all children have visible === false", () => {
    const child = makeNode("h.name", "Name", [], false);
    const parent = makeNode("h", "Hospital", [child]);
    const result = pruneStringNodes([parent]);
    expect(result).toHaveLength(0);
  });

  it("keeps parent with no children when it has visible !== false", () => {
    const parent = makeNode("h", "Hospital", [], true);
    const result = pruneStringNodes([parent]);
    expect(result).toHaveLength(1);
    expect(result[0]!.children).toHaveLength(0);
  });
});
