import { describe, it, expect } from "vitest";
import {
  getSelectableTableTypes,
  DEFAULT_TABLE_TYPE,
} from "../src/tableTypeHelpers";

describe("getSelectableTableTypes", () => {
  it("returns exactly DATA and MODULE", () => {
    expect(getSelectableTableTypes()).toEqual(["DATA", "MODULE"]);
  });

  it("excludes system type ONTOLOGIES", () => {
    expect(getSelectableTableTypes()).not.toContain("ONTOLOGIES");
  });
});

describe("DEFAULT_TABLE_TYPE", () => {
  it("is DATA", () => {
    expect(DEFAULT_TABLE_TYPE).toBe("DATA");
  });
});
