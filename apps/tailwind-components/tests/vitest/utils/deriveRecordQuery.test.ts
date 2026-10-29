import { describe, expect, test } from "vitest";
import type {
  ColumnType,
  IColumn,
  ISchemaMetaData,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import {
  deriveRecordQuery,
  NESTED_LIST_LIMIT,
} from "../../../app/utils/deriveRecordQuery";

function column(
  id: string,
  columnType: ColumnType,
  extra: Partial<IColumn> = {}
): IColumn {
  return { id, label: id, columnType, ...extra };
}

function ref(
  id: string,
  columnType: ColumnType,
  refTableId: string,
  extra: Partial<IColumn> = {}
): IColumn {
  return column(id, columnType, {
    refTableId,
    refSchemaId: "pets",
    ...extra,
  });
}

function table(id: string, columns: IColumn[]): ITableMetaData {
  return {
    id,
    name: id,
    label: id,
    schemaId: "pets",
    tableType: "DATA",
    columns,
  };
}

const schema: ISchemaMetaData = {
  id: "pets",
  label: "pets",
  tables: [
    table("Pet", [
      column("mg_insertedOn", "STRING"),
      column("about", "SECTION"),
      column("name", "STRING", { key: 1 }),
      column("photo", "FILE"),
      ref("owner", "REF", "Owner", { refLabel: "${name} ${city.name}" }),
      ref("category", "ONTOLOGY", "Categories", { refLabelDefault: "${name}" }),
      ref("plain", "REF", "Owner"),
      ref("toys", "REFBACK", "Toy", { refBackId: "pet" }),
      ref("friends", "REF_ARRAY", "Pet", { refLabelDefault: "${name}" }),
    ]),
    table("Owner", [
      column("name", "STRING", { key: 1 }),
      ref("city", "REF", "City"),
    ]),
    table("City", [
      column("name", "STRING", { key: 1 }),
      column("mg_draft", "BOOL"),
    ]),
    table("Categories", [
      column("name", "STRING", { key: 1 }),
      column("label", "STRING"),
    ]),
    table("Toy", [
      column("name", "STRING", { key: 1 }),
      ref("pet", "REF", "Pet"),
      column("color", "STRING"),
      column("notes", "TEXT"),
      column("mg_draft", "BOOL"),
    ]),
  ],
};

function query(lists = {}) {
  return deriveRecordQuery(schema, "Pet", lists).replace(/\s+/g, " ");
}

describe("deriveRecordQuery", () => {
  test("asks one record by filter, with the table's own filter type", () => {
    expect(query()).toMatch(
      /^query \(\$filter: PetFilter\) \{ Pet\(filter: \$filter, limit: 1\) \{ .* \} \}$/
    );
  });

  test("selects no mg_ column and no section or heading, at the root or nested", () => {
    const text = query();
    expect(text).not.toContain("mg_");
    expect(text).not.toContain("about");
  });

  test("selects the file fragment for a file column", () => {
    expect(query()).toContain("photo { id, size, filename, extension, url }");
  });

  test("selects the paths an unconfigured ref's refLabel reads, nested ones included, plus its key", () => {
    expect(query()).toContain("owner { name city { name } }");
  });

  test("selects only the key for a ref with no refLabel", () => {
    expect(query()).toContain("plain { name }");
  });

  test("selects label as well as the refLabelDefault paths for an ontology column", () => {
    expect(query()).toContain("category { name label }");
  });

  test("selects limit and a sibling _agg count for a configured refback", () => {
    const text = query({ toys: { layout: "LIST" } });
    expect(text).toContain(
      `toys(limit: ${NESTED_LIST_LIMIT}) { name pet { name } color notes } toys_agg { count }`
    );
  });

  test("selects the columns a configured list names, besides the keys", () => {
    const text = query({
      friends: { layout: "LINKS", titleColumn: "name", detailColumnIds: [] },
    });
    expect(text).toContain(
      `friends(limit: ${NESTED_LIST_LIMIT}) { name } friends_agg { count }`
    );
  });

  test("asks no limit and no count of an unconfigured refback", () => {
    const text = query();
    expect(text).toContain("toys { name }");
    expect(text).not.toContain("_agg");
  });
});
