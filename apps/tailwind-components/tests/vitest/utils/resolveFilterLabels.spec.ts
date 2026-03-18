import { describe, it, expect, vi, beforeEach } from "vitest";
import { resolveFilterLabels } from "../../../app/utils/resolveFilterLabels";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IFilterValue } from "../../../types/filters";

vi.mock("../../../app/composables/fetchTableMetadata", () => ({
  default: vi.fn(),
}));

import fetchTableMetadata from "../../../app/composables/fetchTableMetadata";

const mockColumns: IColumn[] = [
  {
    id: "name",
    label: "Name",
    columnType: "STRING",
    table: "Person",
    key: 1,
    required: false,
  },
  {
    id: "orders",
    label: "Orders",
    columnType: "REF",
    table: "Person",
    key: 1,
    required: false,
    refTableId: "Order",
    refSchemaId: "shop",
  },
];

const orderColumns: IColumn[] = [
  {
    id: "product",
    label: "Product",
    columnType: "REF",
    table: "Order",
    key: 1,
    required: false,
    refTableId: "Product",
    refSchemaId: "shop",
  },
];

const productColumns: IColumn[] = [
  {
    id: "name",
    label: "Name",
    columnType: "STRING",
    table: "Product",
    key: 1,
    required: false,
  },
];

beforeEach(() => {
  vi.clearAllMocks();
});

describe("resolveFilterLabels", () => {
  it("returns empty map for empty filters", async () => {
    const result = await resolveFilterLabels(
      new Map<string, IFilterValue>(),
      mockColumns
    );
    expect(result.size).toBe(0);
  });

  it("resolves simple column id to column label", async () => {
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
    ]);
    const result = await resolveFilterLabels(filters, mockColumns);
    expect(result.get("name")).toBe("Name");
  });

  it("uses displayConfig.label over column label when available", async () => {
    const columns: IColumn[] = [
      {
        id: "name",
        label: "Name",
        columnType: "STRING",
        table: "Person",
        key: 1,
        required: false,
        displayConfig: { label: "Custom Name" },
      },
    ];
    const filters = new Map<string, IFilterValue>([
      ["name", { operator: "like", value: "John" }],
    ]);
    const result = await resolveFilterLabels(filters, columns);
    expect(result.get("name")).toBe("Custom Name");
  });

  it("resolves dotted column id via fetchTableMetadata", async () => {
    vi.mocked(fetchTableMetadata)
      .mockResolvedValueOnce({
        id: "Order",
        label: "Order",
        columns: orderColumns,
      } as any)
      .mockResolvedValueOnce({
        id: "Product",
        label: "Product",
        columns: productColumns,
      } as any);

    const filters = new Map<string, IFilterValue>([
      ["orders.product.name", { operator: "like", value: "Desk" }],
    ]);
    const result = await resolveFilterLabels(filters, mockColumns);
    expect(result.get("orders.product.name")).toBe("Orders → Product → Name");
  });

  it("handles unknown column id gracefully", async () => {
    const filters = new Map<string, IFilterValue>([
      ["unknown", { operator: "like", value: "x" }],
    ]);
    const result = await resolveFilterLabels(filters, mockColumns);
    expect(result.has("unknown")).toBe(false);
  });

  it("does not include dotted entry when fetchTableMetadata fails", async () => {
    vi.mocked(fetchTableMetadata).mockRejectedValueOnce(new Error("not found"));

    const filters = new Map<string, IFilterValue>([
      ["orders.product.name", { operator: "like", value: "Desk" }],
    ]);
    const result = await resolveFilterLabels(filters, mockColumns);
    expect(result.has("orders.product.name")).toBe(false);
  });
});
