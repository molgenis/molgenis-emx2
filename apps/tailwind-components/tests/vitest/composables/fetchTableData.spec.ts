import { describe, it, expect } from "vitest";
import {
  buildColumnGqlAsync,
  DEFAULT_NESTED_LIMIT,
} from "../../../app/composables/fetchTableData";
import type { IColumn } from "../../../../metadata-utils/src/types";

const ROOT_SCHEMA_ID = "MySchema";
const ROOT_TABLE_ID = "MyTable";

function col(
  id: string,
  columnType: string,
  refTableId?: string,
  refSchemaId?: string
): IColumn {
  return {
    id,
    columnType,
    refTableId,
    refSchemaId,
    position: 0,
  } as IColumn;
}

const columnsBySchemaAndTable: Record<string, Record<string, IColumn[]>> = {
  [ROOT_SCHEMA_ID]: {
    CollectionEvents: [col("name", "STRING"), col("description", "STRING")],
    Subpopulations: [col("name", "STRING")],
    Publications: [col("title", "STRING")],
    Cohorts: [col("id", "STRING"), col("name", "STRING")],
    Tags: [col("name", "STRING")],
    Options: [col("name", "STRING")],
    SharedTable: [col("localName", "STRING")],
  },
  OtherSchema: {
    SharedTable: [col("foreignName", "STRING")],
  },
};

async function columnsForTable(
  schemaId: string,
  tableId: string
): Promise<IColumn[]> {
  return columnsBySchemaAndTable[schemaId]?.[tableId] ?? [];
}

async function noColumnsForTable(): Promise<IColumn[]> {
  return [];
}

describe("DEFAULT_NESTED_LIMIT", () => {
  it("is 5", () => {
    expect(DEFAULT_NESTED_LIMIT).toBe(5);
  });
});

describe("buildColumnGqlAsync — scalar columns", () => {
  it("emits scalar field name", async () => {
    const result = await buildColumnGqlAsync(
      [col("name", "STRING")],
      noColumnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      true,
      2
    );
    expect(result).toContain("name");
  });

  it("does not emit HEADING or SECTION columns", async () => {
    const result = await buildColumnGqlAsync(
      [col("sect", "HEADING"), col("sec2", "SECTION")],
      noColumnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      true,
      2
    );
    expect(result).not.toContain("sect");
    expect(result).not.toContain("sec2");
  });
});

describe("buildColumnGqlAsync — ontology columns", () => {
  it("selects only the term's own fields, never an ancestor chain, so ancestry is not fetched on every row", async () => {
    const result = await buildColumnGqlAsync(
      [col("keywords", "ONTOLOGY_ARRAY", "Keywords", "CatalogueOntologies")],
      noColumnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      true,
      2
    );
    expect(result).toBe(" keywords {name, label, definition, order}");
  });

  it("selects the same fields for a single ONTOLOGY column as for an ONTOLOGY_ARRAY column", async () => {
    const singleResult = await buildColumnGqlAsync(
      [col("status", "ONTOLOGY", "Statuses")],
      noColumnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      true,
      2
    );
    expect(singleResult).toBe(" status {name, label, definition, order}");
  });
});

describe("buildColumnGqlAsync — REFBACK at root level", () => {
  it("always emits _agg { count } for REFBACK", async () => {
    const result = await buildColumnGqlAsync(
      [col("collectionEvents", "REFBACK", "CollectionEvents")],
      columnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      true,
      2
    );
    expect(result).toContain("collectionEvents_agg { count }");
  });

  it("emits nested fields for REFBACK at root level", async () => {
    const result = await buildColumnGqlAsync(
      [col("collectionEvents", "REFBACK", "CollectionEvents")],
      columnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      true,
      2
    );
    expect(result).toContain("collectionEvents");
    expect(result).toContain("name");
  });

  it("always emits (limit: 5) from DEFAULT_NESTED_LIMIT for REFBACK at root level", async () => {
    const result = await buildColumnGqlAsync(
      [col("collectionEvents", "REFBACK", "CollectionEvents")],
      columnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      true,
      2
    );
    expect(result).toContain(
      `collectionEvents(limit: ${DEFAULT_NESTED_LIMIT}) {`
    );
    expect(result).toContain("collectionEvents_agg { count }");
  });
});

describe("buildColumnGqlAsync — REF_ARRAY at root level", () => {
  it("always emits _agg { count } for REF_ARRAY", async () => {
    const result = await buildColumnGqlAsync(
      [col("publications", "REF_ARRAY", "Publications")],
      columnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      true,
      2
    );
    expect(result).toContain("publications_agg { count }");
  });

  it("always emits (limit: 5) from DEFAULT_NESTED_LIMIT for REF_ARRAY at root level", async () => {
    const result = await buildColumnGqlAsync(
      [col("publications", "REF_ARRAY", "Publications")],
      columnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      true,
      2
    );
    expect(result).toContain(`publications(limit: ${DEFAULT_NESTED_LIMIT}) {`);
    expect(result).toContain("publications_agg { count }");
  });
});

describe("buildColumnGqlAsync — nested depth > 1 (non-root)", () => {
  it("skips REFBACK at non-root level (no expansion)", async () => {
    const result = await buildColumnGqlAsync(
      [col("subItems", "REFBACK", "Subpopulations")],
      columnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      false,
      1
    );
    expect(result).not.toContain("subItems");
  });

  it("does not emit _agg { count } for REF_ARRAY at non-root level", async () => {
    const result = await buildColumnGqlAsync(
      [col("publications", "REF_ARRAY", "Publications")],
      columnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      false,
      1
    );
    expect(result).not.toContain("publications_agg");
  });

  it("does not emit (limit:) at non-root level", async () => {
    const result = await buildColumnGqlAsync(
      [col("publications", "REF_ARRAY", "Publications")],
      columnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      false,
      1
    );
    expect(result).not.toContain("(limit:");
  });
});

describe("buildColumnGqlAsync — mg_tableclass beyond the expand depth", () => {
  it("emits mg_tableclass where only key columns survive, so an ancestor row still names its own subclass", async () => {
    const result = await buildColumnGqlAsync(
      [
        col("mg_tableclass", "STRING"),
        col("id", "STRING"),
        col("description", "STRING"),
      ],
      noColumnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      false,
      0
    );
    expect(result.trim().split(/\s+/)).toEqual(["mg_tableclass"]);
  });

  it("keeps emitting key columns alongside mg_tableclass beyond the expand depth", async () => {
    const result = await buildColumnGqlAsync(
      [
        col("mg_tableclass", "STRING"),
        { ...col("id", "STRING"), key: 1 },
        col("description", "STRING"),
      ],
      noColumnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      false,
      0
    );
    expect(result.trim().split(/\s+/)).toEqual(["mg_tableclass", "id"]);
  });
});

describe("buildColumnGqlAsync — REF column unaffected by DEFAULT_NESTED_LIMIT", () => {
  it("emits REF field without limit or _agg", async () => {
    const result = await buildColumnGqlAsync(
      [col("cohort", "REF", "Cohorts")],
      columnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      true,
      2
    );
    expect(result).toContain("cohort {");
    expect(result).not.toContain("cohort_agg");
    expect(result).not.toContain("(limit:");
  });
});

describe("buildColumnGqlAsync — cross-schema refs", () => {
  it("expands a ref into the columns of its own refSchemaId, not the schema being queried", async () => {
    const result = await buildColumnGqlAsync(
      [col("shared", "REF", "SharedTable", "OtherSchema")],
      columnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      true,
      2
    );
    expect(result).toBe(" shared { foreignName }");
  });

  it("expands a ref without refSchemaId into the columns of the schema being queried", async () => {
    const result = await buildColumnGqlAsync(
      [col("shared", "REF", "SharedTable")],
      columnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      true,
      2
    );
    expect(result).toBe(" shared { localName }");
  });
});

describe("buildColumnGqlAsync — MULTISELECT and CHECKBOX at root level", () => {
  it("emits _agg { count } and (limit: 5) for MULTISELECT at root level", async () => {
    const result = await buildColumnGqlAsync(
      [col("tags", "MULTISELECT", "Tags")],
      columnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      true,
      2
    );
    expect(result).toContain("tags_agg { count }");
    expect(result).toContain(`tags(limit: ${DEFAULT_NESTED_LIMIT}) {`);
  });

  it("emits _agg { count } and (limit: 5) for CHECKBOX at root level", async () => {
    const result = await buildColumnGqlAsync(
      [col("options", "CHECKBOX", "Options")],
      columnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      true,
      2
    );
    expect(result).toContain("options_agg { count }");
    expect(result).toContain(`options(limit: ${DEFAULT_NESTED_LIMIT}) {`);
  });
});

describe("buildColumnGqlAsync — PARTS", () => {
  it("queries a PARTS column as a collection of its child rows, like its REFBACK flavor", async () => {
    const result = await buildColumnGqlAsync(
      [col("subpopulations", "PARTS", "Subpopulations")],
      columnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      true,
      2
    );
    expect(result).toBe(
      await buildColumnGqlAsync(
        [col("subpopulations", "REFBACK", "Subpopulations")],
        columnsForTable,
        ROOT_SCHEMA_ID,
        ROOT_TABLE_ID,
        true,
        2
      )
    );
    expect(result).toContain(
      `subpopulations(limit: ${DEFAULT_NESTED_LIMIT}) { name }`
    );
    expect(result).toContain("subpopulations_agg { count }");
  });

  it("skips a PARTS column at non-root level, like its REFBACK flavor", async () => {
    const result = await buildColumnGqlAsync(
      [col("subpopulations", "PARTS", "Subpopulations")],
      columnsForTable,
      ROOT_SCHEMA_ID,
      ROOT_TABLE_ID,
      false,
      1
    );
    expect(result).toBe("");
  });
});
