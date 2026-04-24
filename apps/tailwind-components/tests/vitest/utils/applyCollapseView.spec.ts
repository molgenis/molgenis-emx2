import { describe, it, expect } from "vitest";
import {
  applyCollapseView,
  countAllNodes,
  filterOptionsBySearch,
} from "../../../app/utils/applyCollapseView";
import type { CountedOption } from "../../../app/utils/fetchCounts";

function flat(names: string[], counts?: number[]): CountedOption[] {
  return names.map((name, i) => ({ name, count: counts?.[i] ?? 1 }));
}

function node(
  name: string,
  count: number,
  children?: CountedOption[]
): CountedOption {
  return { name, count, ...(children !== undefined ? { children } : {}) };
}

describe("countAllNodes", () => {
  it("counts flat list", () => {
    expect(countAllNodes(flat(["a", "b", "c"]))).toBe(3);
  });

  it("counts nested nodes", () => {
    const tree = [node("root", 5, [node("child1", 3), node("child2", 2)])];
    expect(countAllNodes(tree)).toBe(3);
  });
});

describe("applyCollapseView - pass-through when ≤25 options", () => {
  it("returns all options unchanged when count is exactly 25", () => {
    const opts = flat(Array.from({ length: 25 }, (_, i) => `opt${i}`));
    const result = applyCollapseView(opts, { hideZero: false, limit: 25 });
    expect(result).toHaveLength(25);
  });

  it("returns all options unchanged when count is below 25", () => {
    const opts = flat(["a", "b", "c"]);
    const result = applyCollapseView(opts, { hideZero: false, limit: 25 });
    expect(result).toHaveLength(3);
  });
});

describe("applyCollapseView - root-slice truncation at limit (>25 options)", () => {
  it("root-slices to first N roots from a flat list", () => {
    const opts = flat(Array.from({ length: 30 }, (_, i) => `opt${i}`));
    const result = applyCollapseView(opts, { hideZero: false, limit: 25 });
    expect(result).toHaveLength(25);
    expect(result[0].name).toBe("opt0");
    expect(result[24].name).toBe("opt24");
  });

  it("root-slices to limit roots, preserving all descendants of each included root", () => {
    const opts = [
      node("root1", 5, [node("c1", 3), node("c2", 2)]),
      node("root2", 4),
      node("root3", 1),
    ];
    const result = applyCollapseView(opts, { hideZero: false, limit: 2 });
    expect(result).toHaveLength(2);
    expect(result[0].name).toBe("root1");
    expect(result[0].children).toHaveLength(2);
    expect(result[1].name).toBe("root2");
  });

  it("included root keeps all its descendants intact regardless of subtree size", () => {
    const opts = [
      node("root1", 5, [node("c1", 3), node("c2", 2), node("c3", 1)]),
      node("root2", 4),
    ];
    const result = applyCollapseView(opts, { hideZero: false, limit: 1 });
    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("root1");
    expect(result[0].children).toHaveLength(3);
  });

  it("30 roots each with 10 children: root-slice to 25 returns 25 roots with all descendants", () => {
    const opts = Array.from({ length: 30 }, (_, i) =>
      node(
        `root${i}`,
        10,
        Array.from({ length: 10 }, (__, j) => node(`child${i}_${j}`, 1))
      )
    );
    const result = applyCollapseView(opts, { hideZero: false, limit: 25 });
    expect(result).toHaveLength(25);
    result.forEach((root) => {
      expect(root.children).toHaveLength(10);
    });
  });

  it("5 roots with 100 total descendants: no truncation when root count ≤ limit", () => {
    const opts = Array.from({ length: 5 }, (_, i) =>
      node(
        `root${i}`,
        20,
        Array.from({ length: 19 }, (__, j) => node(`child${i}_${j}`, 1))
      )
    );
    const result = applyCollapseView(opts, { hideZero: false, limit: 25 });
    expect(result).toHaveLength(5);
    result.forEach((root) => {
      expect(root.children).toHaveLength(19);
    });
  });

  it("includes full subtree of included root", () => {
    const opts = [
      node("root1", 5, [node("c1", 3), node("c2", 2)]),
      node("root2", 4),
    ];
    const result = applyCollapseView(opts, { hideZero: false, limit: 2 });
    expect(result[0].children).toHaveLength(2);
    expect(result[0].children![0].name).toBe("c1");
  });
});

describe("applyCollapseView - hideZero pruning", () => {
  it("drops options with count 0 when hideZero is true", () => {
    const opts = flat(["a", "b", "c", "d"], [5, 0, 3, 0]);
    const result = applyCollapseView(opts, { hideZero: true, limit: null });
    expect(result.map((o) => o.name)).toEqual(["a", "c"]);
  });

  it("keeps parent with count 0 when it has non-zero descendants", () => {
    const opts = [node("parent", 0, [node("child", 3)]), node("other", 0)];
    const result = applyCollapseView(opts, { hideZero: true, limit: null });
    expect(result.map((o) => o.name)).toEqual(["parent"]);
    expect(result[0].children![0].name).toBe("child");
  });

  it("drops parent with count 0 and all-zero descendants", () => {
    const opts = [
      node("parent", 0, [node("child1", 0), node("child2", 0)]),
      node("visible", 5),
    ];
    const result = applyCollapseView(opts, { hideZero: true, limit: null });
    expect(result.map((o) => o.name)).toEqual(["visible"]);
  });

  it("preserves hierarchy when pruning children but parent has count", () => {
    const opts = [
      node("parent", 3, [node("zero-child", 0), node("live-child", 2)]),
    ];
    const result = applyCollapseView(opts, { hideZero: true, limit: null });
    expect(result).toHaveLength(1);
    expect(result[0].children).toHaveLength(1);
    expect(result[0].children![0].name).toBe("live-child");
  });

  it("does not prune when hideZero is false", () => {
    const opts = flat(["a", "b", "c"], [5, 0, 3]);
    const result = applyCollapseView(opts, { hideZero: false, limit: null });
    expect(result).toHaveLength(3);
  });
});

describe("filterOptionsBySearch", () => {
  it("empty query returns all options unchanged", () => {
    const opts = flat(["a", "b", "c"]);
    expect(filterOptionsBySearch(opts, "")).toEqual(opts);
  });

  it("flat list: matches substring case-insensitively", () => {
    const opts = [
      { name: "dogs", label: "Dogs", count: 5 },
      { name: "cats", label: "Cats", count: 3 },
      { name: "bird", label: "Bird", count: 1 },
    ];
    const result = filterOptionsBySearch(opts, "og");
    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("dogs");
  });

  it("case-insensitive: uppercase query matches lowercase label", () => {
    const opts = [{ name: "dogs", label: "Dogs", count: 5 }];
    expect(filterOptionsBySearch(opts, "DOG")).toHaveLength(1);
  });

  it("zero matches returns empty array", () => {
    const opts = flat(["a", "b", "c"]);
    expect(filterOptionsBySearch(opts, "xyz")).toHaveLength(0);
  });

  it("hierarchy: child match keeps parent chain visible", () => {
    const opts = [
      node("animal", 10, [
        node("mammal", 5, [
          { name: "dogs", label: "Dogs", count: 3 },
          { name: "cats", label: "Cats", count: 2 },
        ]),
      ]),
    ];
    const result = filterOptionsBySearch(opts, "dogs");
    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("animal");
    expect(result[0].children).toHaveLength(1);
    expect(result[0].children![0].name).toBe("mammal");
    expect(result[0].children![0].children).toHaveLength(1);
    expect(result[0].children![0].children![0].name).toBe("dogs");
  });

  it("hierarchy: parent match keeps all descendants", () => {
    const opts = [
      node("animal", 10, [
        { name: "dogs", label: "Dogs", count: 3 },
        { name: "cats", label: "Cats", count: 2 },
      ]),
    ];
    const result = filterOptionsBySearch(opts, "animal");
    expect(result).toHaveLength(1);
    expect(result[0].name).toBe("animal");
    expect(result[0].children).toHaveLength(2);
  });

  it("matches on name when label is absent", () => {
    const opts = [{ name: "feline", count: 2 }];
    expect(filterOptionsBySearch(opts, "feli")).toHaveLength(1);
  });
});

describe("applyCollapseView - combined hideZero + limit", () => {
  it("applies zero-pruning before limit, then truncates", () => {
    const opts = [
      node("a", 5),
      node("b", 0),
      node("c", 3),
      node("d", 0),
      node("e", 1),
    ];
    const result = applyCollapseView(opts, { hideZero: true, limit: 2 });
    expect(result.map((o) => o.name)).toEqual(["a", "c"]);
  });

  it("pass-through when after zero-prune count is ≤ limit", () => {
    const opts = flat(
      Array.from({ length: 30 }, (_, i) => `opt${i}`),
      Array.from({ length: 30 }, (_, i) => (i < 20 ? 1 : 0))
    );
    const result = applyCollapseView(opts, { hideZero: true, limit: 25 });
    expect(result).toHaveLength(20);
  });
});
