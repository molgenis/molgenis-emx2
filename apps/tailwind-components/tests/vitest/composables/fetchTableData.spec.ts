import { describe, it, expect } from "vitest";
import { buildColumnGql } from "../../../app/composables/fetchTableData";
import type { IColumn } from "../../../../metadata-utils/src/types";

function col(id: string, columnType: string, refTableId?: string): IColumn {
  return {
    id,
    columnType,
    refTableId,
    position: 0,
  } as IColumn;
}

const refTableColumnsMap: Record<string, IColumn[]> = {
  CollectionEvents: [col("name", "STRING"), col("description", "STRING")],
  Subpopulations: [col("name", "STRING")],
  Publications: [col("title", "STRING")],
  Cohorts: [col("id", "STRING"), col("name", "STRING")],
};

describe("buildColumnGql — scalar columns", () => {
  it("emits scalar field name", () => {
    const result = buildColumnGql(
      [col("name", "STRING")],
      () => [],
      true,
      2,
      undefined
    );
    expect(result).toContain("name");
  });

  it("does not emit HEADING or SECTION columns", () => {
    const result = buildColumnGql(
      [col("sect", "HEADING"), col("sec2", "SECTION")],
      () => [],
      true,
      2,
      undefined
    );
    expect(result).not.toContain("sect");
    expect(result).not.toContain("sec2");
  });
});

describe("buildColumnGql — REFBACK at root level", () => {
  it("always emits _agg { count } for REFBACK even without nestedLimit", () => {
    const result = buildColumnGql(
      [col("collectionEvents", "REFBACK", "CollectionEvents")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      true,
      2,
      undefined
    );
    expect(result).toContain("collectionEvents_agg { count }");
  });

  it("emits nested fields for REFBACK at root level", () => {
    const result = buildColumnGql(
      [col("collectionEvents", "REFBACK", "CollectionEvents")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      true,
      2,
      undefined
    );
    expect(result).toContain("collectionEvents {");
    expect(result).toContain("name");
  });

  it("adds (limit: N) when nestedLimit is set for REFBACK", () => {
    const result = buildColumnGql(
      [col("collectionEvents", "REFBACK", "CollectionEvents")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      true,
      2,
      5
    );
    expect(result).toContain("collectionEvents(limit: 5) {");
    expect(result).toContain("collectionEvents_agg { count }");
  });

  it("does not add (limit: N) when nestedLimit is undefined", () => {
    const result = buildColumnGql(
      [col("collectionEvents", "REFBACK", "CollectionEvents")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      true,
      2,
      undefined
    );
    expect(result).not.toContain("(limit:");
  });
});

describe("buildColumnGql — REF_ARRAY at root level", () => {
  it("always emits _agg { count } for REF_ARRAY even without nestedLimit", () => {
    const result = buildColumnGql(
      [col("publications", "REF_ARRAY", "Publications")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      true,
      2,
      undefined
    );
    expect(result).toContain("publications_agg { count }");
  });

  it("adds (limit: N) when nestedLimit is set for REF_ARRAY", () => {
    const result = buildColumnGql(
      [col("publications", "REF_ARRAY", "Publications")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      true,
      2,
      5
    );
    expect(result).toContain("publications(limit: 5) {");
    expect(result).toContain("publications_agg { count }");
  });
});

describe("buildColumnGql — nested depth > 1 (non-root)", () => {
  it("skips REFBACK at non-root level (no expansion)", () => {
    const result = buildColumnGql(
      [col("subItems", "REFBACK", "Subpopulations")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      false,
      1,
      5
    );
    expect(result).not.toContain("subItems");
  });

  it("does not emit _agg { count } for REF_ARRAY at non-root level", () => {
    const result = buildColumnGql(
      [col("publications", "REF_ARRAY", "Publications")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      false,
      1,
      5
    );
    expect(result).not.toContain("publications_agg");
  });
});

describe("buildColumnGql — REF column unaffected by nestedLimit", () => {
  it("emits REF field without limit or _agg", () => {
    const result = buildColumnGql(
      [col("cohort", "REF", "Cohorts")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      true,
      2,
      5
    );
    expect(result).toContain("cohort {");
    expect(result).not.toContain("cohort_agg");
    expect(result).not.toContain("(limit:");
  });
});

describe("buildColumnGql — MULTISELECT and CHECKBOX at root level", () => {
  it("emits _agg { count } for MULTISELECT at root level", () => {
    const result = buildColumnGql(
      [col("tags", "MULTISELECT", "Tags")],
      () => [col("name", "STRING")],
      true,
      2,
      5
    );
    expect(result).toContain("tags_agg { count }");
    expect(result).toContain("tags(limit: 5) {");
  });

  it("emits _agg { count } for CHECKBOX at root level", () => {
    const result = buildColumnGql(
      [col("options", "CHECKBOX", "Options")],
      () => [col("name", "STRING")],
      true,
      2,
      5
    );
    expect(result).toContain("options_agg { count }");
    expect(result).toContain("options(limit: 5) {");
  });
});
