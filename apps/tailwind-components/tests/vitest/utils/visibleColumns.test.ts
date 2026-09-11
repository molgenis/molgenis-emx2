import { describe, expect, test } from "vitest";
import type { IColumn, ColumnType } from "../../../../metadata-utils/src/types";
import { visibleColumns } from "../../../app/utils/visibleColumns";

function column(id: string, columnType: ColumnType, label: string): IColumn {
  return { id, columnType, label, position: 0, tableId: "Pet" } as IColumn;
}

const columns = [
  column("about", "SECTION", "About"),
  column("name", "STRING", "Name"),
  column("size", "HEADING", "Size"),
  column("weight", "DECIMAL", "Weight"),
  column("mg_draft", "BOOL", "Mg draft"),
];

const idsOf = (result: IColumn[]) => result.map((c) => c.id);

describe("visibleColumns", () => {
  test("hides the mg_ columns unless asked", () => {
    expect(idsOf(visibleColumns(columns))).toEqual([
      "about",
      "name",
      "size",
      "weight",
    ]);
    expect(idsOf(visibleColumns(columns, { showMgColumns: true }))).toContain(
      "mg_draft"
    );
  });

  test("keeps only the fields whose label matches the term", () => {
    expect(idsOf(visibleColumns(columns, { term: "weig" }))).toEqual([
      "about",
      "size",
      "weight",
    ]);
  });

  test("matches the label case-insensitively and ignores surrounding space", () => {
    expect(idsOf(visibleColumns(columns, { term: "  WEIGHT " }))).toContain(
      "weight"
    );
  });

  test("keeps every heading and section, so a filter cannot flatten the structure", () => {
    // A term matching no field at all must still leave the structure intact,
    // or a section disappears while its fields still match somewhere else.
    expect(idsOf(visibleColumns(columns, { term: "nothing matches" }))).toEqual(
      ["about", "size"]
    );
  });

  test("an empty or blank term filters nothing", () => {
    expect(idsOf(visibleColumns(columns, { term: "" }))).toEqual(
      idsOf(visibleColumns(columns))
    );
    expect(idsOf(visibleColumns(columns, { term: "   " }))).toEqual(
      idsOf(visibleColumns(columns))
    );
  });
});
