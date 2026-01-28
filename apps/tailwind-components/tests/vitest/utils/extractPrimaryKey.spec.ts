import { describe, expect, it } from "vitest";
import { extractPrimaryKey } from "../../../app/utils/extractPrimaryKey";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";

describe("extractPrimaryKey", () => {
  it("extracts single PK column", () => {
    const metadata: ITableMetaData = {
      id: "Pet",
      schemaId: "petstore",
      name: "Pet",
      label: "Pet",
      tableType: "DATA",
      columns: [
        { id: "name", columnType: "STRING", label: "Name", key: 1 },
        { id: "species", columnType: "STRING", label: "Species" },
      ],
    };
    const row = { name: "poodle", species: "dog", weight: 10 };
    expect(extractPrimaryKey(row, metadata)).toEqual({ name: "poodle" });
  });

  it("extracts multiple PK columns", () => {
    const metadata: ITableMetaData = {
      id: "Order",
      schemaId: "shop",
      name: "Order",
      label: "Order",
      tableType: "DATA",
      columns: [
        { id: "orderId", columnType: "INT", label: "Order ID", key: 1 },
        { id: "productId", columnType: "INT", label: "Product ID", key: 1 },
        { id: "quantity", columnType: "INT", label: "Quantity" },
      ],
    };
    const row = { orderId: "1", productId: "42", quantity: 5 };
    expect(extractPrimaryKey(row, metadata)).toEqual({
      orderId: "1",
      productId: "42",
    });
  });

  it("returns empty object when no PK columns", () => {
    const metadata: ITableMetaData = {
      id: "Test",
      schemaId: "test",
      name: "Test",
      label: "Test",
      tableType: "DATA",
      columns: [{ id: "name", columnType: "STRING", label: "Name" }],
    };
    const row = { name: "test" };
    expect(extractPrimaryKey(row, metadata)).toEqual({});
  });

  it("ignores undefined PK values", () => {
    const metadata: ITableMetaData = {
      id: "Pet",
      schemaId: "petstore",
      name: "Pet",
      label: "Pet",
      tableType: "DATA",
      columns: [
        { id: "name", columnType: "STRING", label: "Name", key: 1 },
        { id: "id", columnType: "INT", label: "ID", key: 1 },
      ],
    };
    const row = { name: "poodle", id: undefined };
    expect(extractPrimaryKey(row, metadata)).toEqual({ name: "poodle" });
  });
});
