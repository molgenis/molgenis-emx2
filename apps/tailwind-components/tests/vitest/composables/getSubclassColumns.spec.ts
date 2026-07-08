import { describe, it, expect } from "vitest";
import type {
  IColumn,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import { getSubclassColumns } from "../../../app/composables/getSubclassColumns";

function col(id: string, columnType = "STRING"): IColumn {
  return { id, columnType, label: id } as IColumn;
}

function table(
  id: string,
  columns: IColumn[],
  inheritId?: string
): ITableMetaData {
  return {
    id,
    schemaId: "test",
    name: id,
    label: id,
    tableType: "DATA",
    columns,
    inheritId,
  };
}

describe("getSubclassColumns — unknown tableId", () => {
  it("returns empty array when tableId is not found", () => {
    expect(getSubclassColumns("Unknown", [])).toEqual([]);
  });

  it("returns empty array when tables list is empty", () => {
    expect(getSubclassColumns("Animal", [])).toEqual([]);
  });
});

describe("getSubclassColumns — no subclasses", () => {
  it("returns empty array when the table has no subclasses", () => {
    const tables = [table("Animal", [col("name")])];
    expect(getSubclassColumns("Animal", tables)).toEqual([]);
  });
});

describe("getSubclassColumns — direct subclass", () => {
  const tables = [
    table("Animal", [col("name")]),
    table("Dog", [col("name"), col("breed")], "Animal"),
  ];

  it("returns columns added by the direct subclass", () => {
    const result = getSubclassColumns("Animal", tables);
    expect(result.map((c) => c.id)).toContain("breed");
  });

  it("excludes columns already defined in the parent", () => {
    const result = getSubclassColumns("Animal", tables);
    expect(result.map((c) => c.id)).not.toContain("name");
  });

  it("annotates each column with sourceTableId of the subclass", () => {
    const result = getSubclassColumns("Animal", tables);
    const breedCol = result.find((c) => c.id === "breed");
    expect(breedCol?.sourceTableId).toBe("Dog");
  });
});

describe("getSubclassColumns — multiple direct subclasses", () => {
  const tables = [
    table("Animal", [col("name")]),
    table("Dog", [col("name"), col("breed")], "Animal"),
    table("Cat", [col("name"), col("furLength")], "Animal"),
  ];

  it("collects extra columns from all direct subclasses", () => {
    const ids = getSubclassColumns("Animal", tables).map((c) => c.id);
    expect(ids).toContain("breed");
    expect(ids).toContain("furLength");
  });
});

describe("getSubclassColumns — deduplication", () => {
  it("keeps only the first occurrence when the same column id appears in multiple subclasses", () => {
    const tables = [
      table("Animal", [col("name")]),
      table("Dog", [col("name"), col("color")], "Animal"),
      table("Cat", [col("name"), col("color")], "Animal"),
    ];
    const result = getSubclassColumns("Animal", tables);
    expect(result.filter((c) => c.id === "color")).toHaveLength(1);
  });
});

describe("getSubclassColumns — recursive (sub-subclasses)", () => {
  const tables = [
    table("Animal", [col("name")]),
    table("Dog", [col("name"), col("breed")], "Animal"),
    table("Labrador", [col("name"), col("breed"), col("color")], "Dog"),
  ];

  it("collects columns from a subclass of a subclass", () => {
    const ids = getSubclassColumns("Animal", tables).map((c) => c.id);
    expect(ids).toContain("breed");
    expect(ids).toContain("color");
  });

  it("does not include parent columns from deeper levels", () => {
    const ids = getSubclassColumns("Animal", tables).map((c) => c.id);
    expect(ids).not.toContain("name");
  });

  it("sets sourceTableId to the subclass that introduced the column", () => {
    const result = getSubclassColumns("Animal", tables);
    const colorCol = result.find((c) => c.id === "color");
    expect(colorCol?.sourceTableId).toBe("Labrador");
  });

  it("does not duplicate breed even though Labrador also has it", () => {
    const result = getSubclassColumns("Animal", tables);
    expect(result.filter((c) => c.id === "breed")).toHaveLength(1);
  });
});
