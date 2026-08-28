import { describe, it, expect } from "vitest";
import { resolveOntologyAncestry } from "../../../app/utils/resolveOntologyAncestry";
import type { IOntologyTerm } from "../../../app/utils/resolveOntologyAncestry";
import type { IOntologyTreeItem } from "../../../app/utils/buildOntologyTree";

describe("resolveOntologyAncestry", () => {
  it("walks a .parent chain into a linked tree", () => {
    const termsByName = new Map<string, IOntologyTerm>([
      [
        "Pediatric Cardiology",
        { name: "Pediatric Cardiology", parent: { name: "Cardiology" } },
      ],
      ["Cardiology", { name: "Cardiology", parent: { name: "Medicine" } }],
      ["Medicine", { name: "Medicine", parent: null }],
    ]);
    const values: IOntologyTreeItem[] = [{ name: "Pediatric Cardiology" }];

    const [result] = resolveOntologyAncestry(values, termsByName);

    expect(result.name).toBe("Pediatric Cardiology");
    expect(result.parent?.name).toBe("Cardiology");
    expect(result.parent?.parent?.name).toBe("Medicine");
    expect(result.parent?.parent?.parent).toBeUndefined();
  });

  it("returns the input value unchanged for a term the map does not hold", () => {
    const termsByName = new Map<string, IOntologyTerm>();
    const value: IOntologyTreeItem = { name: "Unknown term" };

    const [result] = resolveOntologyAncestry([value], termsByName);

    expect(result).toBe(value);
  });

  it("terminates on a cycle rather than recursing forever", () => {
    const termsByName = new Map<string, IOntologyTerm>([
      ["A", { name: "A", parent: { name: "B" } }],
      ["B", { name: "B", parent: { name: "A" } }],
    ]);
    const values: IOntologyTreeItem[] = [{ name: "A" }];

    const [result] = resolveOntologyAncestry(values, termsByName);

    expect(result.name).toBe("A");
    expect(result.parent?.name).toBe("B");
    expect(result.parent?.parent).toBeUndefined();
  });
});
