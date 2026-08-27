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
      // out of position order: proves the title sorts by position rather
      // than by array order
      column({ id: "lastName", key: 1, position: 2 }),
      column({ id: "firstName", key: 1, position: 1 }),
      column({ id: "bio", columnType: "TEXT", position: 3 }),
      column({ id: "email", position: 4 }),
      column({ id: "phone", position: 5 }),
      // key of a second, non-primary unique constraint: not key === 1, so
      // must be excluded from both the title (only key === 1 counts) and
      // the details (any key column is excluded, not just key === 1)
      column({ id: "otherKey", key: 2, position: 6 }),
    ];

    const result = resolveDisplay(columns);

    expect(result.layout).toEqual("TABLE");
    expect(result.titleTemplate).toEqual("${firstName} ${lastName}");
    expect(result.titleTemplate).not.toContain("otherKey");
    expect(result.descriptionTemplate).toEqual("${bio}");
    expect(result.detailColumns.map((c) => c.id)).toEqual(["email", "phone"]);
    expect(result.logoColumn).toBeUndefined();
  });

  test("a config that sets one slot keeps the defaults for the rest", () => {
    const columns: IColumn[] = [
      column({ id: "acronym", key: 1, position: 1 }),
      column({ id: "name", position: 2 }),
      column({ id: "description", columnType: "TEXT", position: 3 }),
    ];

    const result = resolveDisplay(columns, {
      layout: "CARDS",
      logoColumn: "name",
    });

    expect(result.layout).toEqual("CARDS");
    expect(result.titleTemplate).toEqual("${acronym}");
    expect(result.descriptionTemplate).toEqual("${description}");
    expect(result.logoColumn?.id).toEqual("name");
  });

  test("an explicit titleTemplate and descriptionTemplate override the defaults", () => {
    const columns: IColumn[] = [
      column({ id: "acronym", key: 1, position: 1 }),
      column({ id: "name", position: 2 }),
      column({ id: "description", columnType: "TEXT", position: 3 }),
    ];

    const result = resolveDisplay(columns, {
      layout: "CARDS",
      titleTemplate: "${name} (${acronym})",
      descriptionTemplate: "custom description",
    });

    expect(result.titleTemplate).toEqual("${name} (${acronym})");
    expect(result.descriptionTemplate).toEqual("custom description");
  });

  test("an explicit detailColumns list is resolved to columns, in the given order", () => {
    const columns: IColumn[] = [
      column({ id: "id", key: 1, position: 1 }),
      column({ id: "a", position: 2 }),
      column({ id: "b", position: 3 }),
    ];

    const result = resolveDisplay(columns, {
      layout: "TABLE",
      detailColumns: ["b", "a"],
    });

    expect(result.detailColumns.map((c) => c.id)).toEqual(["b", "a"]);
  });

  test("an empty columns array still resolves, with an empty title and no other slots", () => {
    const result = resolveDisplay([]);

    expect(result.titleTemplate).toEqual("");
    expect(result.descriptionTemplate).toBeUndefined();
    expect(result.detailColumns).toEqual([]);
    expect(result.logoColumn).toBeUndefined();
  });

  test("a table whose only column is its key has a title and no detail columns", () => {
    const columns: IColumn[] = [column({ id: "id", key: 1, position: 1 })];

    const result = resolveDisplay(columns);

    expect(result.titleTemplate).toEqual("${id}");
    expect(result.detailColumns).toEqual([]);
  });

  test("a table with no TEXT column has no descriptionTemplate", () => {
    const columns: IColumn[] = [
      column({ id: "id", key: 1, position: 1 }),
      column({ id: "name", position: 2 }),
    ];

    const result = resolveDisplay(columns);

    expect(result.descriptionTemplate).toBeUndefined();
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
});
