import { describe, it, expect } from "vitest";
import {
  buildColumnGql,
  DEFAULT_NESTED_LIMIT,
} from "../../../app/composables/fetchTableData";
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

describe("DEFAULT_NESTED_LIMIT", () => {
  it("is 5", () => {
    expect(DEFAULT_NESTED_LIMIT).toBe(5);
  });
});

describe("buildColumnGql — scalar columns", () => {
  it("emits scalar field name", () => {
    const result = buildColumnGql([col("name", "STRING")], () => [], true, 2);
    expect(result).toContain("name");
  });

  it("does not emit HEADING or SECTION columns", () => {
    const result = buildColumnGql(
      [col("sect", "HEADING"), col("sec2", "SECTION")],
      () => [],
      true,
      2
    );
    expect(result).not.toContain("sect");
    expect(result).not.toContain("sec2");
  });
});

describe("buildColumnGql — REFBACK at root level", () => {
  it("always emits _agg { count } for REFBACK", () => {
    const result = buildColumnGql(
      [col("collectionEvents", "REFBACK", "CollectionEvents")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      true,
      2
    );
    expect(result).toContain("collectionEvents_agg { count }");
  });

  it("emits nested fields for REFBACK at root level", () => {
    const result = buildColumnGql(
      [col("collectionEvents", "REFBACK", "CollectionEvents")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      true,
      2
    );
    expect(result).toContain("collectionEvents");
    expect(result).toContain("name");
  });

  it("always emits (limit: 5) from DEFAULT_NESTED_LIMIT for REFBACK at root level", () => {
    const result = buildColumnGql(
      [col("collectionEvents", "REFBACK", "CollectionEvents")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      true,
      2
    );
    expect(result).toContain(
      `collectionEvents(limit: ${DEFAULT_NESTED_LIMIT}) {`
    );
    expect(result).toContain("collectionEvents_agg { count }");
  });
});

describe("buildColumnGql — REF_ARRAY at root level", () => {
  it("always emits _agg { count } for REF_ARRAY", () => {
    const result = buildColumnGql(
      [col("publications", "REF_ARRAY", "Publications")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      true,
      2
    );
    expect(result).toContain("publications_agg { count }");
  });

  it("always emits (limit: 5) from DEFAULT_NESTED_LIMIT for REF_ARRAY at root level", () => {
    const result = buildColumnGql(
      [col("publications", "REF_ARRAY", "Publications")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      true,
      2
    );
    expect(result).toContain(`publications(limit: ${DEFAULT_NESTED_LIMIT}) {`);
    expect(result).toContain("publications_agg { count }");
  });
});

describe("buildColumnGql — nested depth > 1 (non-root)", () => {
  it("skips REFBACK at non-root level (no expansion)", () => {
    const result = buildColumnGql(
      [col("subItems", "REFBACK", "Subpopulations")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      false,
      1
    );
    expect(result).not.toContain("subItems");
  });

  it("does not emit _agg { count } for REF_ARRAY at non-root level", () => {
    const result = buildColumnGql(
      [col("publications", "REF_ARRAY", "Publications")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      false,
      1
    );
    expect(result).not.toContain("publications_agg");
  });

  it("does not emit (limit:) at non-root level", () => {
    const result = buildColumnGql(
      [col("publications", "REF_ARRAY", "Publications")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      false,
      1
    );
    expect(result).not.toContain("(limit:");
  });
});

describe("buildColumnGql — REF column unaffected by DEFAULT_NESTED_LIMIT", () => {
  it("emits REF field without limit or _agg", () => {
    const result = buildColumnGql(
      [col("cohort", "REF", "Cohorts")],
      (tableId) => refTableColumnsMap[tableId] ?? [],
      true,
      2
    );
    expect(result).toContain("cohort {");
    expect(result).not.toContain("cohort_agg");
    expect(result).not.toContain("(limit:");
  });
});

describe("buildColumnGql — MULTISELECT and CHECKBOX at root level", () => {
  it("emits _agg { count } and (limit: 5) for MULTISELECT at root level", () => {
    const result = buildColumnGql(
      [col("tags", "MULTISELECT", "Tags")],
      () => [col("name", "STRING")],
      true,
      2
    );
    expect(result).toContain("tags_agg { count }");
    expect(result).toContain(`tags(limit: ${DEFAULT_NESTED_LIMIT}) {`);
  });

  it("emits _agg { count } and (limit: 5) for CHECKBOX at root level", () => {
    const result = buildColumnGql(
      [col("options", "CHECKBOX", "Options")],
      () => [col("name", "STRING")],
      true,
      2
    );
    expect(result).toContain("options_agg { count }");
    expect(result).toContain(`options(limit: ${DEFAULT_NESTED_LIMIT}) {`);
  });
});
