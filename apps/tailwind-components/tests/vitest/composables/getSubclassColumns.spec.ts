import { describe, it, expect } from "vitest";
import { getSubclassColumns } from "../../../app/composables/getSubclassColumns";
import type {
  IColumn,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";

function makeTable(
  id: string,
  columns: IColumn[],
  inheritId?: string
): ITableMetaData & { inheritId?: string } {
  return {
    id,
    schemaId: "test",
    name: id,
    label: id,
    tableType: "DATA",
    columns,
    ...(inheritId !== undefined ? { inheritId } : {}),
  };
}

function makeColumn(id: string): IColumn {
  return { id, columnType: "STRING" };
}

describe("getSubclassColumns", () => {
  it("returns empty array when table has no subclasses", () => {
    const tables = [makeTable("Parent", [makeColumn("name")])];
    expect(getSubclassColumns("Parent", tables)).toEqual([]);
  });

  it("returns empty array when table is not found in schema", () => {
    const tables = [makeTable("Other", [makeColumn("name")])];
    expect(getSubclassColumns("Missing", tables)).toEqual([]);
  });

  it("returns columns from direct subclasses, excluding columns already in parent table", () => {
    const parentColumns = [makeColumn("id"), makeColumn("name")];
    const childColumns = [
      makeColumn("id"),
      makeColumn("name"),
      makeColumn("age"),
    ];
    const tables = [
      makeTable("Parent", parentColumns),
      makeTable("Child", childColumns, "Parent"),
    ];

    const result = getSubclassColumns("Parent", tables);

    expect(result).toHaveLength(1);
    expect(result[0].id).toBe("age");
    expect((result[0] as any).sourceTableId).toBe("Child");
  });

  it("recursively includes columns from multi-level subclass hierarchies", () => {
    const tables = [
      makeTable("Grandparent", [makeColumn("id")]),
      makeTable(
        "Parent",
        [makeColumn("id"), makeColumn("name")],
        "Grandparent"
      ),
      makeTable(
        "Child",
        [makeColumn("id"), makeColumn("name"), makeColumn("age")],
        "Parent"
      ),
    ];

    const result = getSubclassColumns("Grandparent", tables);

    const ids = result.map((c) => c.id);
    expect(ids).toContain("name");
    expect(ids).toContain("age");
    expect(ids).not.toContain("id");
  });

  it("deduplicates columns when multiple subclasses have the same column (first occurrence wins)", () => {
    const tables = [
      makeTable("Parent", [makeColumn("id")]),
      makeTable(
        "ChildA",
        [makeColumn("id"), makeColumn("shared"), makeColumn("onlyA")],
        "Parent"
      ),
      makeTable(
        "ChildB",
        [makeColumn("id"), makeColumn("shared"), makeColumn("onlyB")],
        "Parent"
      ),
    ];

    const result = getSubclassColumns("Parent", tables);

    const sharedCols = result.filter((c) => c.id === "shared");
    expect(sharedCols).toHaveLength(1);
    expect((sharedCols[0] as any).sourceTableId).toBe("ChildA");

    const ids = result.map((c) => c.id);
    expect(ids).toContain("onlyA");
    expect(ids).toContain("onlyB");
  });

  it("attaches sourceTableId to each returned column", () => {
    const tables = [
      makeTable("Parent", [makeColumn("id")]),
      makeTable("Child", [makeColumn("id"), makeColumn("extra")], "Parent"),
    ];

    const result = getSubclassColumns("Parent", tables);

    expect(result).toHaveLength(1);
    expect((result[0] as any).sourceTableId).toBe("Child");
  });

  it("handles empty subclass column arrays", () => {
    const tables = [
      makeTable("Parent", [makeColumn("id")]),
      makeTable("Child", [], "Parent"),
    ];

    const result = getSubclassColumns("Parent", tables);

    expect(result).toEqual([]);
  });
});
