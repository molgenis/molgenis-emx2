import { describe, expect, test } from "vitest";
import type {
  ColumnType,
  IColumn,
  IRow,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import { groupRecordSections } from "../../../app/utils/groupRecordSections";

function column(id: string, columnType: ColumnType, label?: string): IColumn {
  return { id, label: label ?? id, columnType };
}

function table(columns: IColumn[]): ITableMetaData {
  return {
    id: "Pet",
    schemaId: "pet store",
    name: "Pet",
    label: "Pet",
    tableType: "DATA",
    columns,
  };
}

describe("groupRecordSections", () => {
  test("groups on SECTION first and HEADING second", () => {
    const metadata = table([
      column("about", "SECTION", "About"),
      column("name", "STRING"),
      column("size", "HEADING", "Size"),
      column("weight", "DECIMAL"),
      column("care", "SECTION", "Care"),
      column("diet", "STRING"),
    ]);
    const rowData: IRow = {
      name: "spike",
      weight: 15.7,
      diet: "insects",
    };

    const sections = groupRecordSections(metadata, rowData);

    expect(
      sections.map((section) => ({
        id: section.id,
        label: section.label,
        fields: section.fields.map((field) => field.id),
        headings: section.headings.map((heading) => ({
          id: heading.id,
          label: heading.label,
          fields: heading.fields.map((field) => field.id),
        })),
      }))
    ).toEqual([
      {
        id: "about",
        label: "About",
        fields: ["name"],
        headings: [{ id: "size", label: "Size", fields: ["weight"] }],
      },
      { id: "care", label: "Care", fields: ["diet"], headings: [] },
    ]);
  });

  test("carries each field's label, metadata and value", () => {
    const nameColumn = column("name", "STRING", "Name");
    const sections = groupRecordSections(
      table([column("about", "SECTION", "About"), nameColumn]),
      { name: "spike" }
    );

    expect(sections[0]?.fields).toEqual([
      { id: "name", label: "Name", metadata: nameColumn, value: "spike" },
    ]);
  });

  test("renders the synthetic top section unnamed and never prints _top", () => {
    const metadata = table([
      column("mg_top_of_form", "SECTION", "_top"),
      column("name", "STRING"),
    ]);

    const sections = groupRecordSections(metadata, { name: "spike" });

    expect(sections).toHaveLength(1);
    expect(sections[0]?.id).toBe("mg_top_of_form");
    expect(sections[0]?.label).toBe(null);
  });

  test("keeps the synthetic top section unnamed for an admin seeing mg_ columns", () => {
    const metadata = table([
      column("mg_top_of_form", "SECTION", "_top"),
      column("name", "STRING"),
    ]);

    const sections = groupRecordSections(
      metadata,
      { name: "spike" },
      { showMgColumns: true }
    );

    expect(sections[0]?.label).toBe(null);
  });

  test("drops a section and a heading that end up empty", () => {
    const metadata = table([
      column("about", "SECTION", "About"),
      column("name", "STRING"),
      column("size", "HEADING", "Size"),
      column("empty", "SECTION", "Empty"),
      column("alsoEmpty", "HEADING", "Also empty"),
    ]);

    const sections = groupRecordSections(metadata, { name: "spike" });

    expect(sections.map((section) => section.id)).toEqual(["about"]);
    expect(sections[0]?.headings).toEqual([]);
  });

  test("drops a field whose value is empty, in every shape empty takes", () => {
    const metadata = table([
      column("about", "SECTION", "About"),
      column("name", "STRING"),
      column("nothing", "STRING"),
      column("blank", "STRING"),
      column("nadaList", "STRING_ARRAY"),
      column("zero", "INT"),
      column("no", "BOOL"),
    ]);

    const sections = groupRecordSections(metadata, {
      name: "spike",
      nothing: null,
      blank: "",
      nadaList: [],
      zero: 0,
      no: false,
    });

    expect(sections[0]?.fields.map((field) => field.id)).toEqual([
      "name",
      "zero",
      "no",
    ]);
  });

  test("drops a heading whose fields are all empty, and the section with it", () => {
    const metadata = table([
      column("about", "SECTION", "About"),
      column("size", "HEADING", "Size"),
      column("weight", "DECIMAL"),
      column("height", "DECIMAL"),
    ]);

    const sections = groupRecordSections(metadata, {
      weight: null,
      height: "",
    });

    expect(sections).toEqual([]);
  });

  test("drops a column the row data does not carry", () => {
    const metadata = table([
      column("about", "SECTION", "About"),
      column("name", "STRING"),
      column("tags", "STRING_ARRAY"),
    ]);

    const sections = groupRecordSections(metadata, { name: "spike" });

    expect(sections[0]?.fields.map((field) => field.id)).toEqual(["name"]);
  });

  test("keeps a heading whose own columns are carried, even though the row has no value for the heading itself", () => {
    const metadata = table([
      column("about", "SECTION", "About"),
      column("size", "HEADING", "Size"),
      column("weight", "DECIMAL"),
    ]);

    const sections = groupRecordSections(metadata, { weight: 15.7 });

    expect(sections[0]?.headings.map((heading) => heading.id)).toEqual([
      "size",
    ]);
  });

  test("returns nothing when there is no row data", () => {
    const metadata = table([
      column("about", "SECTION", "About"),
      column("name", "STRING"),
    ]);

    expect(groupRecordSections(metadata, undefined)).toEqual([]);
  });

  test("hides mg_ columns unless showMgColumns is set", () => {
    const metadata = table([
      column("about", "SECTION", "About"),
      column("name", "STRING"),
      column("mg_insertedBy", "STRING"),
    ]);
    const rowData: IRow = { name: "spike", mg_insertedBy: "admin" };

    expect(
      groupRecordSections(metadata, rowData)[0]?.fields.map((f) => f.id)
    ).toEqual(["name"]);
    expect(
      groupRecordSections(metadata, rowData, {
        showMgColumns: true,
      })[0]?.fields.map((f) => f.id)
    ).toEqual(["name", "mg_insertedBy"]);
  });

  test("drops a field whose label does not match the filter term", () => {
    const metadata = table([
      column("about", "SECTION", "About"),
      column("name", "STRING", "Name"),
      column("diet", "STRING", "Diet"),
    ]);

    const sections = groupRecordSections(
      metadata,
      { name: "spike", diet: "insects" },
      { filterTerm: "nam" }
    );

    expect(sections[0]?.fields.map((field) => field.id)).toEqual(["name"]);
  });

  test("drops a whole section, and so its menu entry, when the filter term matches none of its fields", () => {
    const metadata = table([
      column("about", "SECTION", "About"),
      column("name", "STRING", "Name"),
      column("care", "SECTION", "Care"),
      column("diet", "STRING", "Diet"),
    ]);

    const sections = groupRecordSections(
      metadata,
      { name: "spike", diet: "insects" },
      { filterTerm: "diet" }
    );

    expect(sections.map((section) => section.id)).toEqual(["care"]);
  });

  test("opens an unnamed section for columns that precede any SECTION", () => {
    const metadata = table([
      column("name", "STRING"),
      column("care", "SECTION", "Care"),
      column("diet", "STRING"),
    ]);

    const sections = groupRecordSections(metadata, {
      name: "spike",
      diet: "insects",
    });

    expect(
      sections.map((section) => [section.id, section.label] as const)
    ).toEqual([
      ["mg_top_of_form", null],
      ["care", "Care"],
    ]);
  });
});
