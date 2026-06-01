import { describe, expect, it } from "vitest";
import {
  isArrayLikeDetail,
  isRefLikeDetail,
} from "../../../app/utils/refUtils";
import type { IColumn } from "../../../../metadata-utils/src/types";

const createColumn = (columnType?: string) =>
  ({ columnType } as unknown as IColumn);

describe("isRefLikeDetail", () => {
  it("returns true for reference-like column types", () => {
    const refLikeTypes = [
      "REF",
      "RADIO",
      "CHECKBOX",
      "SELECT",
      "ONTOLOGY",
      "REFBACK",
      "MULTISELECT",
    ];

    for (const type of refLikeTypes) {
      expect(isRefLikeDetail(createColumn(type))).toBe(true);
    }
  });

  it("returns false for non reference-like column types", () => {
    expect(isRefLikeDetail(createColumn("STRING"))).toBe(false);
    expect(isRefLikeDetail(createColumn(undefined))).toBe(false);
  });
});

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
