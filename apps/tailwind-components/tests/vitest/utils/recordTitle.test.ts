import { describe, expect, test } from "vitest";
import type {
  IColumn,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import { recordTitle } from "../../../app/utils/recordTitle";

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

describe("recordTitle", () => {
  test("joins every key column's value", () => {
    const metadata = table([
      { id: "name", label: "Name", columnType: "STRING", key: 1 },
      { id: "category", label: "Category", columnType: "STRING", key: 1 },
      { id: "diet", label: "Diet", columnType: "STRING" },
    ]);

    expect(
      recordTitle(metadata, { name: "spike", category: "dog", diet: "insects" })
    ).toBe("spike - dog");
  });

  test("flattens a ref key value into text", () => {
    const metadata = table([
      { id: "category", label: "Category", columnType: "REF", key: 1 },
    ]);

    expect(recordTitle(metadata, { category: { name: "dog" } })).toBe("dog");
  });

  test("is empty when the row carries no key value", () => {
    const metadata = table([
      { id: "name", label: "Name", columnType: "STRING", key: 1 },
    ]);

    expect(recordTitle(metadata, {})).toBe("");
    expect(recordTitle(metadata, null)).toBe("");
  });
});
