import { describe, it, expect } from "vitest";
import {
  getSelectableColumnTypes,
  getColumnTypesWithEditableValues,
} from "../src/columnTypeHelpers";

describe("getSelectableColumnTypes", () => {
  it("offers ENUM and ENUM_ARRAY next to the existing types, each once", () => {
    const types = getSelectableColumnTypes();
    expect(types).toContain("ENUM");
    expect(types).toContain("ENUM_ARRAY");
    expect(types).toContain("STRING");
    expect(types).toContain("ONTOLOGY_ARRAY");
    expect(new Set(types).size).toEqual(types.length);
  });
});

describe("getColumnTypesWithEditableValues", () => {
  it("names exactly the enum types", () => {
    expect(getColumnTypesWithEditableValues()).toEqual(["ENUM", "ENUM_ARRAY"]);
  });
});
