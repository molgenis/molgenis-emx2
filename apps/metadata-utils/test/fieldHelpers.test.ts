import { describe, it, expect } from "vitest";
import { isArrayType, isRefType } from "../src/fieldHelpers";
import type { IColumn } from "../src/types";

function makeColumn(columnType: IColumn["columnType"]): IColumn {
  return { id: "col", label: "col", columnType };
}

describe("isArrayType", () => {
  it("returns true for MODULE_ARRAY", () => {
    expect(isArrayType(makeColumn("MODULE_ARRAY"))).toBe(true);
  });

  it("returns true for ENUM_ARRAY", () => {
    expect(isArrayType(makeColumn("ENUM_ARRAY"))).toBe(true);
  });

  it("returns false for MODULE", () => {
    expect(isArrayType(makeColumn("MODULE"))).toBe(false);
  });

  it("returns false for ENUM", () => {
    expect(isArrayType(makeColumn("ENUM"))).toBe(false);
  });
});

describe("isRefType", () => {
  it("returns false for MODULE_ARRAY", () => {
    expect(isRefType(makeColumn("MODULE_ARRAY"))).toBe(false);
  });

  it("returns false for MODULE", () => {
    expect(isRefType(makeColumn("MODULE"))).toBe(false);
  });

  it("returns false for ENUM", () => {
    expect(isRefType(makeColumn("ENUM"))).toBe(false);
  });

  it("returns false for ENUM_ARRAY", () => {
    expect(isRefType(makeColumn("ENUM_ARRAY"))).toBe(false);
  });
});
