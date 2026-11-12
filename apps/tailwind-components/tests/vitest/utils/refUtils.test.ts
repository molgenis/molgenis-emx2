import { describe, expect, it } from "vitest";
import { isRefLikeDetail } from "../../../app/utils/refUtils";
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
