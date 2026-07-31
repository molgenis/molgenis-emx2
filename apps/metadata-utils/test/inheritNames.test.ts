import { describe, it, expect } from "vitest";
import {
  normalizeInheritNames,
  getPrimaryInheritName,
  getInheritanceEdges,
} from "../src/inheritNames";

describe("normalizeInheritNames", () => {
  it("returns the plural list when present", () => {
    expect(
      normalizeInheritNames({
        inheritNames: ["ClinicalSubject", "ResearchSubject"],
      })
    ).toEqual(["ClinicalSubject", "ResearchSubject"]);
  });

  it("falls back to the legacy singular field when plural is empty", () => {
    expect(
      normalizeInheritNames({ inheritName: "Subject", inheritNames: [] })
    ).toEqual(["Subject"]);
  });

  it("prefers the plural list over the legacy singular field", () => {
    expect(
      normalizeInheritNames({
        inheritName: "Subject",
        inheritNames: ["ParentB", "ParentC"],
      })
    ).toEqual(["ParentB", "ParentC"]);
  });

  it("returns an empty list for a root table without parents", () => {
    expect(normalizeInheritNames({})).toEqual([]);
    expect(
      normalizeInheritNames({ inheritName: null, inheritNames: null })
    ).toEqual([]);
  });

  it("removes duplicates while preserving first-seen order", () => {
    expect(
      normalizeInheritNames({ inheritNames: ["ParentB", "ParentC", "ParentB"] })
    ).toEqual(["ParentB", "ParentC"]);
  });

  it("drops empty and whitespace-only names", () => {
    expect(
      normalizeInheritNames({ inheritNames: ["ParentB", "", "  ", "ParentC"] })
    ).toEqual(["ParentB", "ParentC"]);
  });
});

describe("getInheritanceEdges", () => {
  it("emits one edge per parent for a diamond child", () => {
    expect(
      getInheritanceEdges({
        name: "ClinicalResearchSubject",
        tableType: "DATA",
        inheritNames: ["ClinicalSubject", "ResearchSubject"],
      })
    ).toEqual([
      {
        parent: "ClinicalSubject",
        child: "ClinicalResearchSubject",
        isModule: false,
      },
      {
        parent: "ResearchSubject",
        child: "ClinicalResearchSubject",
        isModule: false,
      },
    ]);
  });

  it("flags module-binding edges via the subclass tableType", () => {
    expect(
      getInheritanceEdges({
        name: "CockayneSyndrome",
        tableType: "MODULE",
        inheritNames: ["Subject"],
      })
    ).toEqual([
      { parent: "Subject", child: "CockayneSyndrome", isModule: true },
    ]);
  });

  it("uses the legacy singular parent when the plural list is absent", () => {
    expect(
      getInheritanceEdges({
        name: "RNA",
        tableType: "MODULE",
        inheritName: "Experiment",
      })
    ).toEqual([{ parent: "Experiment", child: "RNA", isModule: true }]);
  });

  it("returns no edges for a root table without parents", () => {
    expect(getInheritanceEdges({ name: "Subject", tableType: "DATA" })).toEqual(
      []
    );
  });

  it("returns no edges when the child name is missing", () => {
    expect(getInheritanceEdges({ inheritNames: ["Subject"] })).toEqual([]);
  });
});

describe("getPrimaryInheritName", () => {
  it("returns the first normalized parent", () => {
    expect(
      getPrimaryInheritName({
        inheritNames: ["ClinicalSubject", "ResearchSubject"],
      })
    ).toBe("ClinicalSubject");
  });

  it("returns undefined for a root table", () => {
    expect(getPrimaryInheritName({})).toBeUndefined();
  });

  it("uses the legacy singular field when plural is absent", () => {
    expect(getPrimaryInheritName({ inheritName: "Subject" })).toBe("Subject");
  });
});
