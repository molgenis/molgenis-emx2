import { describe, it, expect } from "vitest";
import {
  getSelectableColumnTypes,
  getColumnTypesWithEditableValues,
} from "../src/columnTypeHelpers";

describe("getSelectableColumnTypes", () => {
  it("includes the diamond/module column types ENUM, ENUM_ARRAY, MODULE, MODULE_ARRAY", () => {
    const types = getSelectableColumnTypes();
    expect(types).toContain("ENUM");
    expect(types).toContain("ENUM_ARRAY");
    expect(types).toContain("MODULE");
    expect(types).toContain("MODULE_ARRAY");
  });

  it("retains existing scalar types such as STRING and INT", () => {
    const types = getSelectableColumnTypes();
    expect(types).toContain("STRING");
    expect(types).toContain("INT");
  });
});

describe("getColumnTypesWithEditableValues", () => {
  it("returns exactly the value-bearing types ENUM, ENUM_ARRAY, MODULE, MODULE_ARRAY", () => {
    expect(getColumnTypesWithEditableValues()).toEqual([
      "ENUM",
      "ENUM_ARRAY",
      "MODULE",
      "MODULE_ARRAY",
    ]);
  });
});
