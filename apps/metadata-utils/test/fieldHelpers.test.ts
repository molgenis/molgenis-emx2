import { describe, expect, it } from "vitest";
import { fieldTypes, isArrayLikeDetail } from "../src/fieldHelpers";
import type { IColumn } from "../src/types";

const createColumn = (columnType?: string) =>
  ({ columnType } as unknown as IColumn);

describe("isArrayLikeDetail", () => {
  it("returns true for array-like column types", () => {
    expect(isArrayLikeDetail(createColumn("STRING_ARRAY"))).toBe(true);
    expect(isArrayLikeDetail(createColumn("INT_ARRAY"))).toBe(true);
    expect(isArrayLikeDetail(createColumn("MULTISELECT"))).toBe(true);
    expect(isArrayLikeDetail(createColumn("CHECKBOX"))).toBe(true);
  });

  it("returns false for non array-like column types", () => {
    expect(isArrayLikeDetail(createColumn("REF"))).toBe(false);
    expect(isArrayLikeDetail(createColumn(undefined))).toBe(false);
  });
});

describe("enum column types", () => {
  it("lists both enum types as field types", () => {
    expect(fieldTypes()).toEqual(
      expect.arrayContaining(["ENUM", "ENUM_ARRAY"])
    );
  });
});
