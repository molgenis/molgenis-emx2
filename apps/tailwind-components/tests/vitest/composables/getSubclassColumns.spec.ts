import { describe, it, expect, vi, beforeEach } from "vitest";
import { getSubclassColumns } from "../../../app/composables/getSubclassColumns";
import type {
  IColumn,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";

const mockFetchMetadata = vi.hoisted(() => vi.fn());
vi.mock("../../../app/composables/fetchMetadata", () => ({
  default: mockFetchMetadata,
}));

function makeTable(
  id: string,
  columns: IColumn[],
  inheritName?: string
): ITableMetaData {
  return {
    id,
    schemaId: "test",
    name: id,
    label: id,
    tableType: "DATA",
    columns,
    ...(inheritName !== undefined ? { inheritName } : {}),
  };
}

function makeColumn(id: string): IColumn {
  return { id, columnType: "STRING" };
}

function mockSchema(tables: ITableMetaData[]) {
  mockFetchMetadata.mockResolvedValue({
    id: "test",
    label: "test",
    tables,
  });
}

describe("getSubclassColumns", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("returns empty array when table has no subclasses", async () => {
    mockSchema([makeTable("Parent", [makeColumn("name")])]);
    expect(await getSubclassColumns("test", "Parent")).toEqual([]);
  });

  it("returns empty array when table is not found in schema", async () => {
    mockSchema([makeTable("Other", [makeColumn("name")])]);
    expect(await getSubclassColumns("test", "Missing")).toEqual([]);
  });

  it("returns columns from direct subclasses, excluding columns already in parent table", async () => {
    const parentColumns = [makeColumn("id"), makeColumn("name")];
    const childColumns = [
      makeColumn("id"),
      makeColumn("name"),
      makeColumn("age"),
    ];
    mockSchema([
      makeTable("Parent", parentColumns),
      makeTable("Child", childColumns, "Parent"),
    ]);

    const result = await getSubclassColumns("test", "Parent");

    expect(result).toHaveLength(1);
    expect(result[0].id).toBe("age");
    expect((result[0] as any).sourceTableId).toBe("Child");
  });

  it("recursively includes columns from multi-level subclass hierarchies", async () => {
    mockSchema([
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
    ]);

    const result = await getSubclassColumns("test", "Grandparent");

    const ids = result.map((c) => c.id);
    expect(ids).toContain("name");
    expect(ids).toContain("age");
    expect(ids).not.toContain("id");
  });

  it("deduplicates columns when multiple subclasses have the same column (first occurrence wins)", async () => {
    mockSchema([
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
    ]);

    const result = await getSubclassColumns("test", "Parent");

    const sharedCols = result.filter((c) => c.id === "shared");
    expect(sharedCols).toHaveLength(1);
    expect((sharedCols[0] as any).sourceTableId).toBe("ChildA");

    const ids = result.map((c) => c.id);
    expect(ids).toContain("onlyA");
    expect(ids).toContain("onlyB");
  });

  it("attaches sourceTableId to each returned column", async () => {
    mockSchema([
      makeTable("Parent", [makeColumn("id")]),
      makeTable("Child", [makeColumn("id"), makeColumn("extra")], "Parent"),
    ]);

    const result = await getSubclassColumns("test", "Parent");

    expect(result).toHaveLength(1);
    expect((result[0] as any).sourceTableId).toBe("Child");
  });

  it("handles empty subclass column arrays", async () => {
    mockSchema([
      makeTable("Parent", [makeColumn("id")]),
      makeTable("Child", [], "Parent"),
    ]);

    const result = await getSubclassColumns("test", "Parent");

    expect(result).toEqual([]);
  });
});
