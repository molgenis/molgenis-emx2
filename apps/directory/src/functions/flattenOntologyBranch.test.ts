import { describe, expect, it } from "vitest";
import { IOntologyItem } from "../interfaces/interfaces";
import flattenOntologyBranch from "./flattenOntologyBranch";

describe("flattenOntologyBranch", () => {
  it("should return an just the root for a node without children", () => {
    const branch: IOntologyItem = {
      name: "Empty",
      label: "Empty Label",
      code: "empty_code",
    };

    const result = flattenOntologyBranch(branch);

    expect(result).toEqual([branch]);
  });

  it("should flatten an ontology branch", () => {
    const grandChild1: IOntologyItem = {
      name: "Grandchild 1",
      label: "Grandchild 1 Label",
      code: "grandchild_1_code",
    };
    const grandChild2: IOntologyItem = {
      name: "Grandchild 2",
      label: "Grandchild 2 Label",
      code: "grandchild_2_code",
    };
    const child1: IOntologyItem = {
      name: "Child 1",
      label: "Child 1 Label",
      code: "child_1_code",
      children: [grandChild1, grandChild2],
    };
    const child2: IOntologyItem = {
      name: "Child 2",
      label: "Child 2 Label",
      code: "child_2_code",
    };
    const branch: IOntologyItem = {
      name: "Root",
      label: "Root Label",
      code: "root_code",
      children: [child1, child2],
    };

    const result = flattenOntologyBranch(branch);

    const expectedResult = [branch, child2, child1, grandChild2, grandChild1];
    expect(result).toEqual(expectedResult);
  });
});
