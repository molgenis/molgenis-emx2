import { describe, it, expect } from "vitest";
import { isCatalogueResource } from "./resourceTypeUtils";

describe("isCatalogueResource", () => {
  it("should return true for Catalogue type (e.g. testNetwork1)", () => {
    expect(isCatalogueResource([{ name: "Catalogue" }])).toBe(true);
  });

  it("should return false for Network type (e.g. OOM)", () => {
    expect(isCatalogueResource([{ name: "Network" }])).toBe(false);
  });

  it("should return false for Cohort study type (e.g. testCohort1)", () => {
    expect(isCatalogueResource([{ name: "Cohort study" }])).toBe(false);
  });

  it("should return false for Data source type", () => {
    expect(isCatalogueResource([{ name: "Data source" }])).toBe(false);
  });

  it("should return true when Catalogue is among multiple types", () => {
    expect(
      isCatalogueResource([{ name: "Network" }, { name: "Catalogue" }])
    ).toBe(true);
  });

  it("should return false for undefined types", () => {
    expect(isCatalogueResource(undefined)).toBe(false);
  });

  it("should return false for empty types array", () => {
    expect(isCatalogueResource([])).toBe(false);
  });

  it("should return false when type has no name", () => {
    expect(isCatalogueResource([{}])).toBe(false);
  });
});
