import { describe, expect, test } from "vitest";
import type { IColumn } from "../../../../metadata-utils/src/types";
import { resolveDisplay } from "../../../app/utils/displayUtils";

function column(overrides: Partial<IColumn> & Pick<IColumn, "id">): IColumn {
  return {
    columnType: "STRING",
    label: overrides.id,
    ...overrides,
  };
}

describe("resolveDisplay", () => {
  test("with no config, builds every slot from structural metadata alone", () => {
    const columns: IColumn[] = [
      column({ id: "lastName", key: 1, position: 2 }),
      column({ id: "firstName", key: 1, position: 1 }),
      column({ id: "bio", columnType: "TEXT", position: 3 }),
      column({ id: "email", position: 4 }),
      column({ id: "phone", position: 5 }),
      column({ id: "otherKey", key: 2, position: 6 }),
    ];

    const result = resolveDisplay(columns);

    expect(result.titleColumn).toBeUndefined();
    expect(result.subtitleColumn).toBeUndefined();
    expect(result.descriptionColumn?.id).toEqual("bio");
    expect(result.detailColumns.map((c) => c.id)).toEqual(["email", "phone"]);
    expect(result.logoColumn).toBeUndefined();
  });

  test("descriptionColumn defaults to the first TEXT column, returned as a column", () => {
    const bioColumn = column({ id: "bio", columnType: "TEXT", position: 2 });
    const columns: IColumn[] = [
      column({ id: "id", key: 1, position: 1 }),
      bioColumn,
    ];

    const result = resolveDisplay(columns);

    expect(result.descriptionColumn).toEqual(bioColumn);
  });

  test("roles place each column in its slot, and DETAIL columns keep column order", () => {
    const columns: IColumn[] = [
      column({ id: "acronym", key: 1, position: 1 }),
      column({ id: "name", role: "TITLE", position: 2 }),
      column({ id: "tagline", role: "SUBTITLE", position: 3 }),
      column({
        id: "about",
        columnType: "TEXT",
        role: "DESCRIPTION",
        position: 4,
      }),
      column({ id: "logo", columnType: "FILE", role: "LOGO", position: 5 }),
      column({ id: "b", role: "DETAIL", position: 6 }),
      column({ id: "a", role: "DETAIL", position: 7 }),
      column({ id: "unroled", position: 8 }),
    ];

    const result = resolveDisplay(columns);

    expect(result.titleColumn?.id).toEqual("name");
    expect(result.subtitleColumn?.id).toEqual("tagline");
    expect(result.descriptionColumn?.id).toEqual("about");
    expect(result.logoColumn?.id).toEqual("logo");
    expect(result.detailColumns.map((c) => c.id)).toEqual(["b", "a"]);
  });

  test("a SUBTITLE role fills the subtitle slot, and a TITLE does not", () => {
    const titleOnly = resolveDisplay([column({ id: "name", role: "TITLE" })]);
    const subtitleOnly = resolveDisplay([
      column({ id: "tagline", role: "SUBTITLE" }),
    ]);

    expect(titleOnly.subtitleColumn).toBeUndefined();
    expect(subtitleOnly.subtitleColumn?.id).toEqual("tagline");
    expect(subtitleOnly.titleColumn).toBeUndefined();
  });

  test("once any column has a role, the structural defaults no longer apply", () => {
    const columns: IColumn[] = [
      column({ id: "id", key: 1, position: 1 }),
      column({ id: "name", role: "TITLE", position: 2 }),
      column({ id: "bio", columnType: "TEXT", position: 3 }),
      column({ id: "email", position: 4 }),
    ];

    const result = resolveDisplay(columns);

    expect(result.titleColumn?.id).toEqual("name");
    expect(result.descriptionColumn).toBeUndefined();
    expect(result.detailColumns).toEqual([]);
  });

  test("an INTERNAL column is in no slot, with roles or without", () => {
    const withoutRoles = resolveDisplay([
      column({ id: "id", key: 1, role: "INTERNAL", position: 1 }),
      column({ id: "bio", columnType: "TEXT", role: "INTERNAL", position: 2 }),
      column({ id: "email", role: "INTERNAL", position: 3 }),
      column({ id: "phone", position: 4 }),
    ]);
    const withRoles = resolveDisplay([
      column({ id: "name", role: "TITLE", position: 1 }),
      column({ id: "secret", role: "INTERNAL", position: 2 }),
    ]);

    expect(withoutRoles.titleColumn).toBeUndefined();
    expect(withoutRoles.descriptionColumn).toBeUndefined();
    expect(withoutRoles.detailColumns.map((c) => c.id)).toEqual(["phone"]);
    expect(withRoles.detailColumns).toEqual([]);
  });

  test("an empty columns array still resolves, with no title and no other slots", () => {
    const result = resolveDisplay([]);

    expect(result.titleColumn).toBeUndefined();
    expect(result.subtitleColumn).toBeUndefined();
    expect(result.descriptionColumn).toBeUndefined();
    expect(result.detailColumns).toEqual([]);
    expect(result.logoColumn).toBeUndefined();
  });

  test("a table whose only column is its key has that key as title and no detail columns", () => {
    const columns: IColumn[] = [column({ id: "id", key: 1, position: 1 })];

    const result = resolveDisplay(columns);

    expect(result.titleColumn?.id).toEqual("id");
    expect(result.detailColumns).toEqual([]);
  });

  test("a table with no TEXT column has no descriptionColumn", () => {
    const columns: IColumn[] = [
      column({ id: "id", key: 1, position: 1 }),
      column({ id: "name", position: 2 }),
    ];

    const result = resolveDisplay(columns);

    expect(result.descriptionColumn).toBeUndefined();
  });

  test("a table with more than five non-key columns caps detailColumns at five, and excludes key, layout and mg_ columns", () => {
    const columns: IColumn[] = [
      column({ id: "id", key: 1, position: 1 }),
      column({ id: "mg_insertedOn", position: 2 }),
      column({ id: "heading1", columnType: "HEADING", position: 3 }),
      column({ id: "section1", columnType: "SECTION", position: 4 }),
      column({ id: "c1", position: 5 }),
      column({ id: "c2", position: 6 }),
      column({ id: "c3", position: 7 }),
      column({ id: "c4", position: 8 }),
      column({ id: "c5", position: 9 }),
      column({ id: "c6", position: 10 }),
    ];

    const result = resolveDisplay(columns);

    expect(result.detailColumns.map((c) => c.id)).toEqual([
      "c1",
      "c2",
      "c3",
      "c4",
      "c5",
    ]);
  });

  test("the default title is the only key=1 column, returned as a column", () => {
    const idColumn = column({ id: "id", key: 1, position: 1 });
    const result = resolveDisplay([
      idColumn,
      column({ id: "name", position: 2 }),
    ]);

    expect(result.titleColumn).toEqual(idColumn);
  });

  test("a composite key has no default title", () => {
    const result = resolveDisplay([
      column({ id: "a", key: 1, position: 1 }),
      column({ id: "b", key: 1, position: 2 }),
    ]);

    expect(result.titleColumn).toBeUndefined();
  });
});
