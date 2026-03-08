import { describe, it, expect, vi, beforeEach } from "vitest";
import type { ISchemaMetaData } from "../../../metadata-utils/src/types";

const mockFetchMetadata = vi.fn();

vi.mock("../../app/composables/fetchMetadata", () => ({
  default: (...args: any[]) => mockFetchMetadata(...args),
}));

const mockSchema: ISchemaMetaData = {
  id: "test",
  label: "Test Schema",
  tables: [
    {
      id: "Person",
      name: "Person",
      schemaId: "test",
      label: "Person",
      tableType: "DATA",
      columns: [{ id: "name", label: "Name", columnType: "STRING" }],
    },
    {
      id: "Employee",
      name: "Employee",
      schemaId: "test",
      label: "Employee",
      tableType: "DATA",
      inheritId: "Person",
      columns: [
        { id: "name", label: "Name", columnType: "STRING" },
        { id: "salary", label: "Salary", columnType: "INT" },
      ],
    },
  ],
};

describe("fetchTableMetadata", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockFetchMetadata.mockResolvedValue(mockSchema);
  });

  it("returns table metadata for valid tableId", async () => {
    const fetchTableMetadata = (
      await import("../../app/composables/fetchTableMetadata")
    ).default;
    const result = await fetchTableMetadata("test", "Person");
    expect(result.id).toBe("Person");
    expect(result.label).toBe("Person");
  });

  it("rejects when tableId not found", async () => {
    const fetchTableMetadata = (
      await import("../../app/composables/fetchTableMetadata")
    ).default;
    await expect(fetchTableMetadata("test", "Unknown")).rejects.toMatch(
      "Unknown"
    );
  });

  it("without includeSubclassColumns, returns only the table's own columns", async () => {
    const fetchTableMetadata = (
      await import("../../app/composables/fetchTableMetadata")
    ).default;
    const result = await fetchTableMetadata("test", "Person");
    const ids = result.columns.map((c) => c.id);
    expect(ids).toEqual(["name"]);
  });

  it("with includeSubclassColumns: true, returns parent columns plus subclass columns", async () => {
    const fetchTableMetadata = (
      await import("../../app/composables/fetchTableMetadata")
    ).default;
    const result = await fetchTableMetadata("test", "Person", {
      includeSubclassColumns: true,
    });
    const ids = result.columns.map((c) => c.id);
    expect(ids).toContain("name");
    expect(ids).toContain("salary");
  });

  it("subclass columns have visible: 'false' set", async () => {
    const fetchTableMetadata = (
      await import("../../app/composables/fetchTableMetadata")
    ).default;
    const result = await fetchTableMetadata("test", "Person", {
      includeSubclassColumns: true,
    });
    const salary = result.columns.find((c) => c.id === "salary");
    expect(salary?.visible).toBe("false");
  });

  it("subclass columns have sourceTableId set", async () => {
    const fetchTableMetadata = (
      await import("../../app/composables/fetchTableMetadata")
    ).default;
    const result = await fetchTableMetadata("test", "Person", {
      includeSubclassColumns: true,
    });
    const salary = result.columns.find((c) => c.id === "salary");
    expect(salary?.sourceTableId).toBe("Employee");
  });
});
